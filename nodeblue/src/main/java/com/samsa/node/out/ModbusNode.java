package com.samsa.node.out;

import com.samsa.core.Message;
import com.samsa.core.OutNode;
import lombok.extern.slf4j.Slf4j;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modbus 프로토콜을 사용하여 외부 장치와 통신하는 출력 노드 클래스
 * TCP/IP를 통해 Modbus 슬레이브 장치와 통신하며, 홀딩 레지스터 읽기 기능을 제공합니다.
 */
@Slf4j
public class ModbusNode extends OutNode {

    private ModbusMaster master;
    private final String host;
    private final int port;
    private final int slaveId;
    private final int slaveAddress;
    private final int registerCount;
    private String description = "";
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private static final int RETRY_LIMIT = 3;
    private static final long RETRY_DELAY = 1000L;

    /**
     * ModbusNode 클래스의 생성자
     * 
     * @param id            노드의 고유 식별자
     * @param host          Modbus 슬레이브 장치의 호스트 주소
     * @param port          Modbus 통신에 사용할 포트 번호
     * @param slaveAddress  슬레이브 장치의 시작 레지스터 주소
     * @param slaveId       슬레이브 장치의 ID
     * @param registerCount 읽어올 레지스터의 개수
     * @throws IllegalArgumentException 입력 파라미터가 유효하지 않을 경우
     */
    public ModbusNode(String id, String host, int port, int slaveAddress, int slaveId, int registerCount) {
        super(id);
        validateConstructorParams(host, port, slaveAddress, slaveId, registerCount);
        this.host = host;
        this.port = port;
        this.slaveId = slaveId;
        this.slaveAddress = slaveAddress;
        this.registerCount = registerCount;
    }

    /**
     * 생성자 파라미터의 유효성을 검사합니다.
     * 
     * @param host          호스트 주소
     * @param port          포트 번호
     * @param slaveAddress  슬레이브 주소
     * @param slaveId       슬레이브 ID
     * @param registerCount 레지스터 개수
     * @throws IllegalArgumentException 파라미터가 유효하지 않을 경우
     */
    private void validateConstructorParams(String host, int port, int slaveAddress, int slaveId, int registerCount) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }
        if (slaveAddress < 0) {
            throw new IllegalArgumentException("Invalid slave address");
        }
        if (slaveId < 0) {
            throw new IllegalArgumentException("Invalid slave ID");
        }
        if (registerCount <= 0) {
            throw new IllegalArgumentException("Register count must be positive");
        }
    }

    /**
     * Modbus 마스터를 초기화하고 연결을 설정합니다.
     * 연결 실패 시 지정된 횟수만큼 재시도합니다.
     */
    @Override
    public void start() {
        int retryCount = 0;
        while (retryCount < RETRY_LIMIT && !isConnected.get()) {
            try {
                initModbusMaster();
                isConnected.set(true);
                log.info("ModbusOutNode[{}] successfully connected after {} attempts", id, retryCount + 1);
                break;
            } catch (ModbusInitException e) {
                retryCount++;
                log.error("Attempt {}/{} failed to initialize Modbus master for ModbusOutNode[{}]: {}",
                        retryCount, RETRY_LIMIT, id, e.getMessage());
                if (retryCount < RETRY_LIMIT) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for ModbusOutNode[{}]", id);
                        break;
                    }
                }
            }
        }

        if (!isConnected.get()) {
            log.error("Failed to initialize Modbus master after {} attempts", RETRY_LIMIT);
        }
    }

    /**
     * Modbus 마스터 인스턴스를 초기화합니다.
     * 
     * @throws ModbusInitException Modbus 초기화 실패 시
     */
    private void initModbusMaster() throws ModbusInitException {
        try {
            IpParameters params = new IpParameters();
            params.setHost(host);
            params.setPort(port);

            master = new ModbusFactory().createTcpMaster(params, false);
            master.init();

            log.info("ModbusOutNode[{}] initialized with host: {}, port: {}", id, host, port);
        } catch (Exception e) {
            throw new ModbusInitException(e);
        }
    }

    /**
     * 슬레이브 장치로부터 센서 데이터를 읽어옵니다.
     * 읽어온 데이터는 처리 후 메시지로 전달됩니다.
     */
    private void readSensorData() {
        if (!isConnected.get()) {
            log.error("ModbusOutNode[{}] is not connected", id);
            return;
        }

        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, slaveAddress, registerCount);
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

            if (response == null) {
                log.error("ModbusOutNode[{}]: Null response received", id);
                return;
            }

            if (response.isException()) {
                log.error("ModbusOutNode[{}]: Exception response received: {}", id, response.getExceptionMessage());
                return;
            }

            short[] data = response.getShortData();
            if (data == null || data.length == 0) {
                log.warn("ModbusOutNode[{}]: Empty data received", id);
                return;
            }

            Message responseMessage = new Message(processRegisterData(data));
            emit(responseMessage);
            log.debug("ModbusNode[{}]: Successfully processed and forwarded sensor data", id);

        } catch (ModbusTransportException e) {
            log.error("Error processing Modbus request in ModbusOutNode[{}]: {}", id, e.getMessage());
            isConnected.set(false);
        } catch (Exception e) {
            log.error("Unexpected error in ModbusOutNode[{}]: {}", id, e.getMessage());
        }
    }

    /**
     * 레지스터 데이터를 처리하여 Map 형태로 변환합니다.
     * 
     * @param data 처리할 레지스터 데이터 배열
     * @return 처리된 데이터를 담은 Map
     */
    Map<String, Object> processRegisterData(short[] data) {
        Map<String, Object> processedData = new HashMap<>();
        try {
            if (description == null || description.trim().isEmpty()) {
                description = "default";
            }
            processedData.put(description, data);
        } catch (Exception e) {
            log.error("Error processing register data in ModbusOutNode[{}]: {}", id, e.getMessage());
            processedData.put("error", "Data processing failed");
        }
        return processedData;
    }

    /**
     * 메시지를 수신하여 처리합니다.
     * 
     * @param message 수신된 메시지
     */
    @Override
    public void onMessage(Message message) {
        if (message == null) {
            log.warn("ModbusOutNode[{}]: Received null message", id);
            return;
        }
        readSensorData();
    }

    /**
     * Modbus 연결을 종료하고 리소스를 정리합니다.
     */
    @Override
    public void stop() {
        if (master != null) {
            try {
                master.destroy();
                isConnected.set(false);
                log.info("ModbusOutNode[{}] destroyed", id);
            } catch (Exception e) {
                log.error("Error destroying Modbus master for ModbusOutNode[{}]: {}", id, e.getMessage());
            } finally {
                master = null;
            }
        } else {
            log.warn("ModbusOutNode[{}]: Master is null, cannot destroy", id);
        }
    }

    /**
     * 데이터에 대한 설명을 설정합니다.
     * 
     * @param description 설정할 설명 문자열
     */
    public void setDescription(String description) {
        if (description != null) {
            this.description = description.trim();
        }
    }

    /**
     * 현재 설정된 description 값을 반환합니다.
     * 
     * @return 설정된 description 문자열
     */
    public String getDescription() {
        return description;
    }

    public void setMaster(ModbusMaster master) {
        this.master = master;
    }

}
