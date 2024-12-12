package com.samsa.node.in;

import java.util.Objects;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.samsa.core.InNode;
import com.samsa.core.Message;

public class MqttOutNode extends InNode{
    // 퍼블리셔

    private String broker;
    private String clientId;
    private String topic;
    MqttClient mqttClient;

    public MqttOutNode(String broker, String clientId) {
        super();
        if(Objects.isNull(broker) || Objects.isNull(clientId)) {
            throw new NullPointerException();
        }
        this.broker = broker;
        this.clientId = clientId;
    }

    public MqttOutNode(String broker, String clientId, String topic) {
        this(broker, clientId);
        this.topic = topic;
    }

    @Override
    public void start() {
        super.start();
        try{
            mqttClient = new MqttClient(broker, clientId); // mqtt 클라이언트가 해당 브로커와 연결할 것이라고 알려줘야함.
            System.out.println("mqttClient create");
            mqttClient.connect(); // 지정된 브로커에 연결을 시도합니다.

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message message) {
        // 브로커한테 날리는 메시지 메서드
        try {
            mqttClient.publish(topic, new MqttMessage(message.getPayload().toString().getBytes()));
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        } // 지정된 주제와 메시지를 MQTT 브로커에 발행합니다. 브로커로 토픽과 메시지(바이트로 변환)를 날림.
    }
    
}
