package com.samsa.node.inout;

import com.samsa.core.InOutNode;
import com.samsa.core.InPort;
import com.samsa.core.Message;
import com.samsa.core.OutPort;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.Map;

/**
 * Modbus 데이터를 MQTT 메시지로 변환하여 전송하는 노드입니다.
 * <p>
 * 이 클래스는 Modbus 장치에서 수집한 데이터를 MQTT 형식으로 변환하여 지정된 토픽(topic)으로 전송합니다.
 * 입력 데이터는 short 배열 형태로 받아야 하며, 변환된 데이터를 OutPort를 통해 전달합니다.
 */
@Slf4j
public class ModbusToMqtt extends InOutNode {
    private final String topic;
    private final MqttClient mqttClient;

    /**
     * MQTT 클라이언트를 생성하는 메서드.
     * 
     * @param broker   MQTT 브로커 주소
     * @param clientId MQTT 클라이언트 ID
     * @return MQTT 클라이언트 객체
     */
    protected MqttClient createMqttClient(String broker, String clientId) throws MqttException {
        return new MqttClient(broker, clientId); // 기본 MqttClient 생성
    }

    /**
     * ModbusToMqtt 생성자.
     *
     * @param id         노드의 고유 식별자
     * @param inPort     입력 포트 (현재 사용되지 않음)
     * @param outPort    출력 포트
     * @param mqttBroker MQTT 브로커 주소 (예: "tcp://localhost:1883")
     * @param topic      MQTT 토픽 (예: "sensor/data")
     * @throws MqttException MQTT 연결 초기화 중 발생할 수 있는 예외
     */
    public ModbusToMqtt(String id, OutPort outPort, InPort inPort, String mqttBroker, String topic)
            throws MqttException {
        super(inPort, outPort);
        this.topic = topic;

        // MQTT 클라이언트 초기화
        this.mqttClient = new MqttClient(mqttBroker, id);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        log.info("ModbusToMqtt[{}]: Connecting to MQTT broker at {}", id, mqttBroker);
        mqttClient.connect(options);
        log.info("ModbusToMqtt[{}]: Connected to MQTT broker", id);
    }

    /**
     * Modbus 데이터를 MQTT 메시지로 변환하고 전송합니다.
     *
     * @param message 입력 메시지로, payload가 short[] 형태여야 합니다.
     *                - payload가 short[]인 경우 데이터를 MQTT 형식으로 변환하고 전송합니다.
     *                - 다른 형식의 payload가 전달되면 경고 로그를 남깁니다.
     */
    @Override
    public void onMessage(Message message) {
        if (message.getPayload() instanceof short[]) {
            try {
                short[] sensorData = (short[]) message.getPayload();

                // 데이터를 MQTT 메시지 형식으로 변환
                Map<String, Object> mqttPayload = new HashMap<>();
                mqttPayload.put("topic", topic);
                mqttPayload.put("timestamp", System.currentTimeMillis());
                mqttPayload.put("values", sensorData);

                // 변환된 메시지를 출력 포트로 전달
                Message mqttMessage = new Message(mqttPayload);
                emit(mqttMessage);
                log.info("ModbusToMqtt[{}]: Successfully converted and forwarded data", getId());

            } catch (Exception e) {
                log.error("ModbusToMqtt[{}]: Error converting data to MQTT format", getId(), e);
            }
        } else {
            log.warn("ModbusToMqtt[{}]: Received unsupported payload type: {}", getId(),
                    message.getPayload().getClass().getSimpleName());
        }
    }

    /**
     * 노드를 중지하고 MQTT 연결을 종료합니다.
     * <p>
     * MQTT 연결이 열려 있는 경우 안전하게 종료하며, 종료 중 오류가 발생하면 예외처리헙니다.
     */
    @Override
    public void stop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("ModbusToMqtt[{}]: Disconnected from MQTT broker", getId());
            }
        } catch (MqttException e) {
            throw new IllegalArgumentException("disconnect 실패");
        }
    }
}