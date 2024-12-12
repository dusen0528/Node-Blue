package com.samsa.node.out;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samsa.core.Message;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ModbusNodeTest {

    @Mock
    private ModbusMaster modbusMaster;

    @Mock
    private ModbusFactory modbusFactory;

    @Mock
    private ReadHoldingRegistersResponse response;

    private ModbusNode modbusNode;
    private static final String TEST_SLAVE_ADDRESS = "localhost";
    private static final int TEST_PORT = 502;

    /**
     * 인스턴스 생성 테스트
     * 
     * @throws ModbusInitException
     */
    @BeforeEach
    void setUp() throws ModbusInitException {

        String testId = UUID.randomUUID().toString();
        // ModbusNode 인스턴스 생성
        modbusNode = new ModbusNode(testId, TEST_SLAVE_ADDRESS, TEST_PORT);

        // ModbusMaster mock 객체 주입
        ReflectionTestUtils.setField(modbusNode, "master", modbusMaster);
    }

    /**
     * 요청 테스트
     * 
     * @throws ModbusTransportException
     */
    @Test
    void testValidModbusRequest() throws ModbusTransportException {

        // 유효한 payload를 포함한 메세지 객체 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("slaveId", 1);
        payload.put("startAddress", 0);
        payload.put("quantity", 10);
        Message message = new Message(payload);

        // ModbusMaster의 send()에 대한 Mock 응답 설정
        ReadHoldingRegistersResponse response = mock(ReadHoldingRegistersResponse.class);
        when(response.isException()).thenReturn(false);
        when(response.getShortData()).thenReturn(new short[] { 100, 200, 300 });
        when(modbusMaster.send(any(ReadHoldingRegistersRequest.class))).thenReturn(response);

        modbusNode.onMessage(message);

        // ModbusMaster.send() 메서드가 한번 호출되었는지 검증
        verify(modbusMaster, times(1)).send(any(ReadHoldingRegistersRequest.class));
    }

    /**
     * 유효하지 않은 메세지 Payload인지 테스트
     */
    @Test
    void testInvalidMessagePayload() {

        // 유효하지 않은 메세지 생성
        Message invalidMessage = new Message("invalid payload");
        modbusNode.onMessage(invalidMessage); // 실행

        // 호출되지 않았는지 검증
        try {
            verify(modbusMaster, never()).send(any(ReadHoldingRegistersRequest.class));
        } catch (ModbusTransportException e) {
            e.printStackTrace();
        }
    }

    /**
     * 예외가 발생하는지 테스트
     * 
     * @throws ModbusTransportException
     */
    @Test
    void testModbusException() throws ModbusTransportException {

        // 유효한 payload 객체 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("slaveId", 1);
        payload.put("startAddress", 0);
        payload.put("quantity", 10);
        Message message = new Message(payload);

        // modbusMaster.send() 메서드가 예외 던지도록 설정
        when(modbusMaster.send(any(ReadHoldingRegistersRequest.class)))
                .thenThrow(new ModbusTransportException("Test exception"));

        modbusNode.onMessage(message);

        // 호출되었는지 검증
        verify(modbusMaster, times(1)).send(any(ReadHoldingRegistersRequest.class));
    }

    /**
     * stop() 테스트
     */
    @Test
    void testStop() {

        modbusNode.stop();
        verify(modbusMaster).destroy();
    }

    /**
     * start() 테스트
     * 
     * @throws ModbusInitException
     */
    @Test
    void testStart() throws ModbusInitException {

        // ModbusFactory와 ModbusMaster에 대한 mock 객체 생성
        ModbusFactory mockFactory = mock(ModbusFactory.class);
        ModbusMaster mockMaster = mock(ModbusMaster.class);

        // Modbus TCP 통신을 테스트할때, 실제 네트워크 연결 없이 동작을 검증하기 위한 부분
        mockFactory.createTcpMaster(any(IpParameters.class), anyBoolean());

        // // mock 객체를 ModbusNode에 주입
        ReflectionTestUtils.setField(modbusNode, "master", mockMaster);
        ReflectionTestUtils.setField(modbusNode, "master", mockMaster);

        modbusNode.start();

        // 검증
        verify(modbusMaster, never()).init();
    }
}
