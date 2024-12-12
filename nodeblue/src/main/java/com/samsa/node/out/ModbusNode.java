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

import java.util.Map;

/***
 * Modbus 출력 노드 클래스
 * Modbus 프로토콜을 사용하여 외부 장치와 통신하는 기능을 제공
 */
@Slf4j
public class ModbusNode extends OutNode {

    private ModbusMaster master;
    private final String slaveAddress;
    private final int port;

    /**
     * ModbusNode를 생성
     * Modbus 마스터 연결을 전달받은 주소와 포트로 연결
     * 
     * @param id
     */
    public ModbusNode(String id, String slaveAddress, int port) {
        super(id);
        this.slaveAddress = slaveAddress;
        this.port = port;
    }

    /**
     * Modbus 마스터 연결을 초기화하고 시작합니다.
     */
    @Override
    public void start() {
        try {
            IpParameters params = new IpParameters();
            params.setHost(slaveAddress);
            params.setPort(port);

            ModbusFactory factory = new ModbusFactory();
            master = factory.createTcpMaster(params, false);
            master.init();
            log.info("ModbusOutNode[{}] initialized with slave address: {}, port: {}", id, slaveAddress, port);
        } catch (ModbusInitException e) {
            log.error("Failed to initialize Modbus master for ModbusOutNode[{}]: ", id, e);
        }
    }

    /**
     * 수신된 메세지를 Map<String,Object> 형태로 처리해서 payload를 만듬
     * payload에서 modbus통신에 필요한 정보를 추출함.
     * 
     * @param message 처리할 메세지 객체
     */
    @Override
    public void onMessage(Message message) {
        if (message.getPayload() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();

            try {
                // slave로부터 데이터 요청(Request)
                int slaveId = (int) payload.get("slaveId");
                int startAddress = (int) payload.get("startAddress");
                int quantity = (int) payload.get("quantity");

                // Modbus 슬레이브 장치의 특정 홀딩 레지스터 값들을 읽음.
                // 응답 객체(response)에는 요청한 레지스터의 값들이 포함되어 있어, 이후 이 값들을 처리하거나 사용할 수 있습니다.
                ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startAddress, quantity);
                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);

                // 예외처리
                if (response.isException()) {
                } else {
                    // 응답 데이터를 다음 노드로 전달
                    short[] data = response.getShortData();
                    log.info("ModbusOutNode[{}]: Received data: {}", id, data);

                    // 추가적으로 응답하는 메세지
                    Message responseMessage = new Message(data);
                    log.info("ModbusOutNode[{}]: Response message created: {}", id, responseMessage);

                    emit(responseMessage);
                    log.info("ModbusNode[{}]: Received and forwarded sensor data", id);
                }
            } catch (ModbusTransportException e) {
                log.error("Error processing Modbus request in ModbusOutNode[{}]: ", id, e);
            }
        }
    }

    /**
     * Modbus 마스터 연결을 종료하고 리소스를 정리합니다.
     */
    @Override
    public void stop() {
        if (master != null) {
            try {
                master.destroy();
                log.info("ModbusOutNode[{}] Modbus master destroyed", id);
            } catch (Exception e) {
                log.error("Error destroying Modbus master for ModbusOutNode[{}]: ", id, e);
            }
        }
    }
}
