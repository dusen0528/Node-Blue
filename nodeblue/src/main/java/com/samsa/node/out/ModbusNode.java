package com.samsa.node.out;

import com.samsa.core.Message;
import com.samsa.core.OutNode;
import com.samsa.core.OutPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modbus 프로토콜을 사용하여 외부 장치와 통신하는 노드 클래스.
 * TCP/IP를 통해 Modbus 슬레이브 장치와 통신하며, 홀딩 레지스터 읽기 기능을 제공합니다.
 */
@Slf4j
public class ModbusNode extends OutNode {

    ModbusMaster master;
    private final String host;
    private final int port;
    private final int slaveId;
    private final int slaveAddress;
    private final int registerCount;
    private String description = "";
    final AtomicBoolean isConnected = new AtomicBoolean(false);
    private static final int RETRY_LIMIT = 3;
    private static final long RETRY_DELAY = 1000L;

    private final OutPort outPort;

    /**
     * ModbusNode 클래스의 생성자.
     *
     * @param id            노드의 고유 식별자
     * @param host          Modbus 슬레이브 장치의 호스트 주소
     * @param outPort       데이터를 전송할 OutPort
     * @param slaveAddress  슬레이브 장치의 시작 레지스터 주소
     * @param slaveId       슬레이브 장치의 ID
     * @param registerCount 읽어올 레지스터의 개수
     */
    public ModbusNode(UUID id, String host, OutPort outPort, int slaveAddress, int slaveId, int registerCount) {
        super(id, outPort);
        validateConstructorParams(host, outPort, slaveAddress, slaveId, registerCount);
        this.host = host;
        this.port = 502;
        this.slaveId = slaveId;
        this.slaveAddress = slaveAddress;
        this.registerCount = registerCount;
        this.outPort = outPort;
    }

    /**
     * 생성자 파라미터의 유효성을 검사합니다.
     */
    private void validateConstructorParams(String host, OutPort outPort, int slaveAddress, int slaveId,
            int registerCount) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (outPort == null) {
            throw new IllegalArgumentException("OutPort cannot be null");
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
        if (master == null) {
            throw new NullPointerException("ModbusMaster가 null입니다.");
        }
    }

    /**
     * 노드를 시작하고 Modbus 마스터를 초기화합니다.
     */
    @Override
    public void start() {
        super.start();
        int retryCount = 0;
        while (retryCount < RETRY_LIMIT && !isConnected.get()) {
            try {
                initModbusMaster();
                isConnected.set(true);
                log.info("ModbusNode[{}] successfully connected after {} attempts", getId(), retryCount + 1);
                break;
            } catch (ModbusInitException e) {
                retryCount++;
                log.error("Attempt {}/{} failed to initialize Modbus master for ModbusNode[{}]: {}",
                        retryCount, RETRY_LIMIT, getId(), e.getMessage());
                if (retryCount < RETRY_LIMIT) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for ModbusNode[{}]", getId());
                        break;
                    }
                }
            }
        }

        if (!isConnected.get()) {
            log.error("Failed to initialize Modbus master after {} attempts", RETRY_LIMIT);
            handleError(new RuntimeException("Modbus initialization failed"));
        }
    }

    /**
     * Modbus 마스터 인스턴스를 초기화합니다.
     */
    private void initModbusMaster() throws ModbusInitException {
        IpParameters params = new IpParameters();
        params.setHost(host);
        params.setPort(port);

        master = new ModbusFactory().createTcpMaster(params, false);
        master.init();
        log.info("ModbusNode[{}] initialized with host: {}, port: {}", getId(), host, port);
    }

    /**
     * 메시지를 수신하여 데이터를 처리합니다.
     */
    public void readMessage(Message message) {
        if (message == null) {
            log.warn("ModbusNode[{}]: Received null message", getId());
            return;
        }

        readSensorData();
    }

    /**
     * 슬레이브 장치로부터 센서 데이터를 읽어옵니다.
     */
    void readSensorData() {
        if (!isConnected.get()) {
            log.error("ModbusNode[{}] is not connected", getId());
            return;
        }

        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, slaveAddress, registerCount);
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

            if (response == null) {
                log.error("ModbusNode[{}]: Null response received", getId());
                return;
            }

            if (response.isException()) {
                log.error("ModbusNode[{}]: Exception response received: {}", getId(), response.getExceptionMessage());
                return;
            }

            short[] data = response.getShortData();
            if (data == null || data.length == 0) {
                log.warn("ModbusNode[{}]: Empty data received", getId());
                return;
            }

            Message responseMessage = new Message(processRegisterData(data));
            outPort.propagate(responseMessage);
            log.info("ModbusNode[{}]: Successfully propagated sensor data", getId());
        } catch (ModbusTransportException e) {
            log.error("Error processing Modbus request in ModbusNode[{}]: {}", getId(), e.getMessage());
            isConnected.set(false);
            handleError(e);
        } catch (Exception e) {
            log.error("Unexpected error in ModbusNode[{}]: {}", getId(), e.getMessage());
            handleError(e);
        }
    }

    /**
     * 레지스터 데이터를 처리하여 Map 형태로 변환합니다.
     */
    private Map<String, Object> processRegisterData(short[] data) {
        Map<String, Object> processedData = new HashMap<>();
        try {
            if (description == null || description.trim().isEmpty()) {
                description = "default";
            }
            processedData.put(description, data);
        } catch (Exception e) {
            log.error("Error processing register data in ModbusNode[{}]: {}", getId(), e.getMessage());
            processedData.put("error", "Data processing failed");
        }
        return processedData;
    }

    /**
     * 노드를 중지하고 리소스를 정리합니다.
     */
    @Override
    public void stop() {
        super.stop();
        if (master != null) {
            try {
                master.destroy();
                isConnected.set(false);
                log.info("ModbusNode[{}] destroyed", getId());
            } catch (Exception e) {
                log.error("Error destroying Modbus master for ModbusNode[{}]: {}", getId(), e.getMessage());
            } finally {
                master = null;
            }
        } else {
            log.warn("ModbusNode[{}]: Master is null, cannot destroy", getId());
        }
    }

    /**
     * 데이터에 대한 설명을 설정합니다.
     */
    public void setDescription(String description) {
        if (description != null) {
            this.description = description.trim();
        }
    }

    /**
     * 현재 설정된 description 값을 반환합니다.
     */
    public String getDescription() {
        return description;
    }
}