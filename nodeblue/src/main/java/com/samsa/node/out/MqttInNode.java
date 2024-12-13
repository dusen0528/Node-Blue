package com.samsa.node.out;

import java.util.Objects;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.samsa.core.Message;
import com.samsa.core.OutNode;

import lombok.extern.slf4j.Slf4j;

/**
 * MQTT 입력 노드 클래스. MQTT 브로커로부터 메시지를 구독하고 처리합니다.
 */
@Slf4j
public class MqttInNode extends OutNode {

    private String broker;
    private String clientId;
    private String[] topics;
    private int[] qos;

    /**
     * 주어진 브로커 URL과 클라이언트 ID로 MqttInNode 객체를 생성합니다.
     *
     * @param broker   MQTT 브로커의 URL
     * @param clientId 연결에 사용할 클라이언트 ID
     * @throws NullPointerException broker 또는 clientId가 null인 경우
     */
    public MqttInNode(String broker, String clientId) {
        super();
        if (Objects.isNull(broker) || Objects.isNull(clientId)) {
            throw new NullPointerException("브로커와 클라이언트 ID는 null일 수 없습니다.");
        }
        this.broker = broker;
        this.clientId = clientId;
    }

    /**
     * 주어진 브로커 URL, 클라이언트 ID 및 토픽으로 MqttInNode 객체를 생성합니다.
     *
     * @param broker   MQTT 브로커의 URL
     * @param clientId 연결에 사용할 클라이언트 ID
     * @param topics   구독할 토픽 목록
     */
    public MqttInNode(String broker, String clientId, String[] topics) {
        this(broker, clientId);
        this.topics = topics;
        this.qos = new int[topics.length];
    }

    /**
     * MQTT 클라이언트를 시작하고 브로커에 연결 및 토픽 구독을 설정합니다.
     */
    @Override
    public void start() {
        super.start();
        try (MqttClient mqttClient = new MqttClient(broker, clientId)) {
            mqttClient.connect();
            mqttClient.setCallback(new MqttCallback() {

                /**
                 * 브로커와의 연결이 끊어졌을 때 호출됩니다.
                 *
                 * @param cause 연결이 끊어진 원인
                 */
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT 브로커와 연결이 끊어졌습니다.", cause);
                }

                /**
                 * 새 메시지가 도착했을 때 호출됩니다.
                 *
                 * @param topic   메시지가 도착한 토픽
                 * @param message 수신된 메시지 객체
                 */
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Message msg = new Message(payload);
                    log.info("수신된 메시지: {}", msg.getPayload());
                    emit(msg);
                }

                /**
                 * 발행된 메시지가 성공적으로 전달되었을 때 호출됩니다. (구독자에서는 거의 사용되지 않음)
                 *
                 * @param token 전달 완료된 메시지의 토큰
                 */
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 구독자에서는 구현 불필요
                }
            });
            mqttClient.subscribe(topics, qos);
            log.info("MQTT 토픽 구독 완료: {}", (Object) topics);
        } catch (Exception e) {
            log.error("MQTT 클라이언트 초기화 중 오류 발생", e);
        }
    }
}
