package com.samsa.node.in;

import com.samsa.core.InNode;
import com.samsa.core.Message;
import lombok.extern.slf4j.Slf4j;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.intelligt.modbus.jlibmodbus.msg.base.ModbusRequest;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.utils.ModbusFunctionCode;

@Slf4j
public class ModbusOutNode extends InNode {

    private final String slaveAddress;
    private final int port;
    private ModbusMaster master;

    public ModbusOutNode(String id, String slaveAddress, int port) {
        super(id);
        this.slaveAddress = slaveAddress;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            IpParameters params = new IpParameters();
            params.setHost(slaveAddress);
            params.setPort(port);

            ModbusFactory factory = new ModbusFactory();
            master = factory.createTcpMaster(params, false);
            master.init();
            log.info("ModbusInNode[{}] initialized with slave address: {}, port: {}", id, slaveAddress, port);
        } catch (ModbusInitException e) {
            log.error("Failed to initialize Modbus master for ModbusInNode[{}]: ", id, e);
        }
    }

    @Override
    public void onMessage(Message message) {
        // 메시지가 ModbusReadRequest 인스턴스인지 확인
        if (message.getPayload() instanceof ReadHoldingRegistersRequest) {
            ReadHoldingRegistersRequest request = (ReadHoldingRegistersRequest) message.getPayload();
            int functionCode = request.getFunction();
            int startingAddress = request.getStartAddress();
            int quantity = request.getQuantity();

            try {
                switch (functionCode) {
                    case ModbusFunctionCodes.READ_HOLDING_REGISTERS:
                        short[] data = master.readHoldingRegisters(Integer.parseInt(slaveAddress), startingAddress,
                                quantity);
                        Message response = new ModbusReadResponse(message.getId(), data); // 응답 메시지 생성
                        log.info("ModbusInNode[{}] received response: {}", id, response);
                        // 응답을 처리하는 로직 추가 가능
                        break;

                    default:
                        log.warn("ModbusInNode[{}] Unsupported Modbus function code: {}", id, functionCode);
                        break;
                }
            } catch (Exception e) {
                log.error("Error processing message in ModbusInNode[{}]: ", id, e);
            }
        } else {
            log.warn("ModbusInNode[{}] received unsupported message type: {}", id, message.getClass().getSimpleName());
        }
    }

    @Override
    public void stop() {
        if (master != null) {
            try {
                master.destroy();
                log.info("ModbusInNode[{}] Modbus master destroyed", id);
            } catch (Exception e) {
                log.error("Error destroying Modbus master for ModbusInNode[{}]: ", id, e);
            }
        }
    }
}
