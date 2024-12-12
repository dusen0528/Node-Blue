package com.samsa.node.out;

import org.junit.jupiter.api.*;


class MqttIntNodeTest {

    MqttInNode mqttInNode;

    @BeforeEach
    void setUp() {
        String [] topics = {"application/#","123"};
        mqttInNode = new MqttInNode("tcp://192.168.70.203:1883", "123", topics);
    }

    @Test
    void constructorTest() {
        Assertions.assertThrows(NullPointerException.class,
        () -> new MqttInNode(null, "123"));

        Assertions.assertThrows(NullPointerException.class,
        () -> new MqttInNode("123", null));
    }

    @Test
    void start() {
        
        mqttInNode.start();
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Assertions.assertDoesNotThrow(()-> mqttInNode.start());
    }
}
