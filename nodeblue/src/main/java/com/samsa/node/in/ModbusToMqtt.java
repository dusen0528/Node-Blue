package com.samsa.node.in;

import java.util.HashMap;
import java.util.Map;

import com.samsa.core.InNode;
import com.samsa.core.Message;
import com.samsa.core.Pipe;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModbusToMqtt extends InNode {
    private final String topic;

    /**
     * @param id
     * @param topic
     */
    public ModbusToMqtt(String id, String topic) {
        super(id);
        this.topic = topic;
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
                for (Pipe pipe : getPipes()) {
                    pipe.send(mqttmessage);
                }
                log.info("ModbusInNode[{}]: Converted and Forwarded data", id);
            } catch (Exception e) {
                log.error("Error converting data to MQTT format: ", e);
            }
        }
    }
}
