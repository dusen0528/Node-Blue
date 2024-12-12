package com.samsa.node.out;

import java.util.Objects;

import javax.management.RuntimeErrorException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.samsa.core.Message;
import com.samsa.core.OutNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttInNode extends OutNode{    
    // 브로커한테 받으니까 sub

    private String broker;
    private String clientId;
    private String[] topics;
    private int[] qos;
    MqttClient mqttClient;

    public MqttInNode(String broker, String clientId) {
        super();
        if(Objects.isNull(broker) || Objects.isNull(clientId)) {
            throw new NullPointerException();
        }
        this.broker = broker;
        this.clientId = clientId;
    }

    public MqttInNode(String broker, String clientId, String[] topics) {
        this(broker, clientId);
        this.topics = topics;
        this.qos = new int[topics.length];
    }

    @Override
    public void start() {
        super.start();
            try{
            mqttClient = new MqttClient(broker, clientId); // mqtt 클라이언트가 해당 브로커와 연결할 것이라고 알려줘야함.
            System.out.println("mqttClient create");
            mqttClient.connect(); // 지정된 브로커(서버)에 연결을 시도합니다.
            mqttClient.setCallback(new MqttCallback() { // MQTT 클라이언트가 수신한 메시지를 처리하기 위한 콜백 메서드를 정의합니다.
                // 구독자가 mqtt를 받으면 3가지로 분류해서 응답함.

                @Override
                public void connectionLost(Throwable cause) { // 브로커와의 연결이 끊어졌을 때 호출됩니다.
                    System.out.println("Disconnected");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception { // 새 메시지가 도착했을 때 호출됩니다.
                    // MQTT 메시지 페이로드를 JSON으로 파싱
                    String payload = new String(message.getPayload());
                    // JsonNode jsonNode = new ObjectMapper().readTree(payload);
                    // System.out.println(jsonNode.toString());


                    Message msg = new Message(payload);
                    log.info(msg.getPayload().toString());
                    emit(msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 발행된 메시지가 성공적으로 전달되었을 때 호출됩니다(구독자에서는 거의 사용되지 않음).
                }
            });
            mqttClient.subscribe(topics, qos); // Sub이 topics을 구독
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}