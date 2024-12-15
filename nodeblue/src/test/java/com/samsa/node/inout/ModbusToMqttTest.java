package com.samsa.node.inout;

import com.samsa.core.InPort;
import com.samsa.core.Message;
import com.samsa.core.OutPort;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModbusToMqtt 클래스에 대한 테스트 클래스입니다.
 * 이 클래스는 Modbus 데이터를 MQTT 메시지로 변환하고 전송하는 기능을 검증합니다.
 */
@Slf4j
public class ModbusToMqttTest {

    private ModbusToMqtt modbusToMqtt;
    private OutPort mockOutPort;
    private InPort mockInPort;
    private MqttClient mockMqttClient;

    /**
     * 각 테스트 전에 필요한 객체를 초기화합니다.
     */
    @BeforeEach
    void setUp() throws MqttException {
        mockOutPort = mock(OutPort.class);
        mockMqttClient = mock(MqttClient.class);
        mockInPort = mock(InPort.class);

        // ModbusToMqtt 인스턴스 생성
        modbusToMqtt = new ModbusToMqtt("test-node", mockOutPort, mockInPort, "tcp://localhost:1883", "test/topic") {
            @Override
            protected MqttClient createMqttClient(String broker, String clientId) {
                return mockMqttClient; // 테스트용 Mock 객체 반환
            }
        };
    }

    /**
     * ModbusToMqtt가 short[]가 아닌 payload를 처리할 때 적절히 경고 로그를 남기는지 테스트합니다.
     */
    @Test
    void testOnMessageWithInvalidPayload() {
        String invalidPayload = "invalid data";
        Message inputMessage = new Message(invalidPayload);

        modbusToMqtt.onMessage(inputMessage);

        verify(mockOutPort, never()).propagate(any(Message.class));
    }

    /**
     * ModbusToMqtt가 stop()이 작동하는지 확인
     */
    @Test
    void testStop() throws MqttException {
        // mockMqttClient이 연결되어 있다고 가정
        when(mockMqttClient.isConnected()).thenReturn(true);

        // stop 메서드 호출
        modbusToMqtt.stop();

        // disconnect가 호출되었는지 확인
        verify(mockMqttClient, times(1)).disconnect();
    }

    /**
     * ModbusToMqtt가 stop()에 실패할 때 예외처리하는지 확인인
     * 
     * @throws MqttException
     */
    @Test
    void testStopError() throws MqttException {

        when(mockMqttClient.isConnected()).thenReturn(true);

        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mockMqttClient).disconnect();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> modbusToMqtt.stop());
        assertEquals("disconnect 실패", exception.getMessage());
    }
}