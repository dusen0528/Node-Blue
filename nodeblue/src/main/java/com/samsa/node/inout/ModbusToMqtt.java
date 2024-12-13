package com.samsa.node.inout;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.samsa.core.InOutNode;
import com.samsa.core.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModbusToMqtt extends InOutNode {
    private final String topic;
    private final MqttClient mqttClient;

    /**
     * @param id
     * @param topic
     */
    public ModbusToMqtt(String id, String mqttBroker, String topic) throws MqttException {
        super(id);
        this.topic = topic;
        this.mqttClient = new MqttClient(mqttBroker, id);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        mqttClient.connect(options);
    }

    /**
     * Modbus에서 받은 센서 데이터(short[] 형태)를 mqtt형식으로 바꾼다.
     * 바꾼 데이터를 다음 노드로 전달한다.
     */
    @Override
    public void onMessage(Message message) {
        if (message.getPayload() instanceof short[]) {
            try {
                short[] sensorData = (short[]) message.getPayload();

                // 데이터를 MQTT메세지 형식으로 변환
                Map<String, Object> mqttData = new HashMap<>();
                mqttData.put("topic", topic);
                mqttData.put("timestamp", System.currentTimeMillis());
                mqttData.put("values", sensorData);

                // 변환된 데이터를 다음 노드로 전달
                Message mqttmessage = new Message(mqttData);
                emit(mqttmessage);
                log.info("ModbusInNode[{}]: Converted and Forwarded data", id);
            } catch (Exception e) {
                log.error("Error converting data to MQTT format: ", e);
            }
        }
    }

    @Override
    public void stop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("ModbusInNode[{}]: Disconnected from MQTT broker", id);
            }
        } catch (MqttException e) {
            log.error("Error disconnecting from MQTT broker: ", e);
        }
    }
}
