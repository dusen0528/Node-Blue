package com.samsa.node.in;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.samsa.core.InNode;
import com.samsa.core.Message;

/**
 * MQTT 출력 노드 클래스. MQTT 브로커에 연결하고 메시지를 발행합니다.
 */
@Slf4j
public class MqttOutNode extends InNode {

    private String broker;
    private String clientId;
    private String topic;
    private MqttClient mqttClient;

    /**
     * 주어진 브로커 URL과 클라이언트 ID로 MqttOutNode 객체를 생성합니다.
     *
     * @param broker   MQTT 브로커의 URL
     * @param clientId 연결에 사용할 클라이언트 ID
     * @throws NullPointerException broker 또는 clientId가 null인 경우
     */
    public MqttOutNode(String broker, String clientId) {
        super();
        if (Objects.isNull(broker) || Objects.isNull(clientId)) {
            throw new NullPointerException("브로커와 클라이언트 ID는 null일 수 없습니다.");
        }
        this.broker = broker;
        this.clientId = clientId;
    }

    /**
     * 주어진 브로커 URL, 클라이언트 ID, 토픽으로 MqttOutNode 객체를 생성합니다.
     *
     * @param broker   MQTT 브로커의 URL
     * @param clientId 연결에 사용할 클라이언트 ID
     * @param topic    메시지를 발행할 토픽
     */
    public MqttOutNode(String broker, String clientId, String topic) {
        this(broker, clientId);
        this.topic = topic;
    }

    /**
     * MQTT 클라이언트를 시작하고 브로커에 연결합니다.
     */
    @Override
    public void start() {
        super.start();
        try {
            mqttClient = new MqttClient(broker, clientId);
            log.info("MQTT 클라이언트 생성 완료");
            mqttClient.connect();
            log.info("MQTT 브로커 연결 성공: {}", broker);
        } catch (MqttException e) {
            log.error("MQTT 브로커 연결 실패", e);
        }
    }

    /**
     * 지정된 토픽으로 MQTT 브로커에 메시지를 발행합니다.
     *
     * @param message 이전 노드로부터 전달받은 메세지
     */
    @Override
    public void onMessage(Message message) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT 클라이언트가 연결되지 않았습니다.");
            return;
        }

        try {
            MqttMessage mqttMessage = new MqttMessage(message.getPayload().toString().getBytes());
            mqttClient.publish(topic, mqttMessage);
            log.info("메시지가 토픽 '{}'에 발행되었습니다.", topic);
        } catch (MqttException e) {
            log.error("토픽 '{}'에 메시지 발행 실패", topic, e);
        }
    }
}
