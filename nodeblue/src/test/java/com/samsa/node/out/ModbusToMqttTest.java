package com.samsa.node.out;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samsa.core.Message;
import com.samsa.core.Pipe;
import com.samsa.node.in.ModbusToMqtt;

@ExtendWith(MockitoExtension.class)
public class ModbusToMqttTest {

    @Mock
    private MqttClient mqttClient;

    @Mock
    private Pipe outputPipe;

    private ModbusToMqtt modbusToMqtt;
    private static final String TEST_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_TOPIC = "test/topic";
    private static final String TEST_BROKER = "tcp://localhost:1883";

    /**
     * 설정이 적용되는지 확인 테스트
     * 
     * @throws MqttException
     */
    @BeforeEach
    void setUp() throws MqttException {
        doNothing().when(mqttClient).connect(any(MqttConnectOptions.class));

        // ModbusToMqtt 생성
        modbusToMqtt = new ModbusToMqtt(TEST_ID, TEST_BROKER, TEST_TOPIC);
        // pipe 추가
        modbusToMqtt.addOutputPipe(outputPipe);
    }

    /**
     * 데이터 변환되는지 테스트
     */
    @Test
    void testValidSensorDataConversion() {

        short[] testData = new short[] { 100, 200, 300 };
        Message inputMessage = new Message(testData);

        modbusToMqtt.onMessage(inputMessage);

        // Assert
        verify(outputPipe, times(1)).send(argThat(message -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            return TEST_TOPIC.equals(payload.get("topic")) &&
                    payload.get("timestamp") instanceof Long &&
                    payload.get("values") instanceof short[] &&
                    ((short[]) payload.get("values")).length == testData.length;
        }));
    }

    /**
     * 유효한 payload인지 확인하는 테스트
     */
    @Test
    void testInvalidMessagePayload() {

        Message invalidMessage = new Message("invalid payload");
        modbusToMqtt.onMessage(invalidMessage);
        verify(outputPipe, never()).send(any());
    }

    /**
     * stop() 테스트
     * 
     * @throws MqttException
     */
    @Test
    void testStop() throws MqttException {

        when(mqttClient.isConnected()).thenReturn(true);
        modbusToMqtt.stop();
        verify(mqttClient).disconnect();
    }

    /**
     * disconnect() 와 stop()이 작동하는지 테스트
     * 
     * @throws MqttException
     */
    @Test
    void testStopWithDisconnectedClient() throws MqttException {

        when(mqttClient.isConnected()).thenReturn(false);
        modbusToMqtt.stop();
        verify(mqttClient, never()).disconnect();
    }
}
