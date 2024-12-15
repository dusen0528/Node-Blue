package com.samsa.node.out;

import com.samsa.core.OutPort;
import com.serotonin.modbus4j.ModbusMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ModbusNode 클래스에 대한 단위 테스트입니다. 이 클래스는 Modbus 프로토콜을 사용하여 외부 장치와 통신합니다.
 */
class ModbusNodeTest {

    private ModbusNode modbusNode;
    private OutPort mockOutPort;
    private ModbusMaster mockMaster;
    private UUID nodeId;

    /**
     * 테스트 환경을 설정합니다. 모의 객체와 ModbusNode 인스턴스를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        nodeId = UUID.randomUUID();
        mockOutPort = mock(OutPort.class);

        modbusNode = new ModbusNode(nodeId, "localhost", mockOutPort, 0, 1, 5);
        modbusNode.master = mockMaster;
    }

    /**
     * ModbusNode의 OutPort가 null일 때 IllegalArgumentException이 발생하는지 테스트합니다.
     */
    @Test
    void testOutPortNull() {
        // Expect IllegalArgumentException to be thrown when OutPort is null
        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "localhost", null, 0, 1, 5); // Pass null for OutPort
        }, "OutPort cannot be null");
    }

    /**
     * host가 null 또는 빈 값일 때 IllegalArgumentException이 발생하는지 확인하는 테스트입니다.
     */
    @Test
    void testHostNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), null, mockOutPort, 0, 1, 5);
        }, "Host cannot be null or empty");

        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "", mockOutPort, 0, 1, 5);
        }, "Host cannot be null or empty");
    }

    /**
     * slaveAddress가 0보다 작은 값일 때 IllegalArgumentException이 발생하는지 확인하는 테스트입니다.
     */
    @Test
    void testInvalidSlaveAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "localhost", mockOutPort, -1, 1, 5);
        }, "Invalid slave address");
    }

    /**
     * slaveId가 0보다 작은 값일 때 IllegalArgumentException이 발생하는지 확인하는 테스트입니다.
     */
    @Test
    void testInvalidSlaveId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "localhost", mockOutPort, 0, -1, 5);
        }, "Invalid slave ID");
    }

    /**
     * registerCount가 0이거나 음수일 때 IllegalArgumentException이 발생하는지 확인하는 테스트입니다.
     */
    @Test
    void testInvalidRegisterCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "localhost", mockOutPort, 0, 1, 0);
        }, "Register count must be positive");

        assertThrows(IllegalArgumentException.class, () -> {
            new ModbusNode(UUID.randomUUID(), "localhost", mockOutPort, 0, 1, -1);
        }, "Register count must be positive");
    }

    /**
     * master가 null일 때 NullPointerException이 발생하는지 확인하는 테스트입니다.
     */
    @Test
    void testMasterNull() {
        // 명시적으로 master를 null로 설정하여 NullPointerException이 발생하도록 합니다.
        modbusNode.master = null;

        assertThrows(NullPointerException.class, () -> {
            modbusNode.readSensorData();
        }, "ModbusMaster가 null입니다.");
    }

    /**
     * ModbusNode의 설명 설정 및 가져오기를 테스트합니다.
     */
    @Test
    void testSetDescription() {
        modbusNode.setDescription("Test Description");
        assertEquals("Test Description", modbusNode.getDescription(), "설명은 올바르게 설정되어야 합니다.");

        modbusNode.setDescription(null);
        assertEquals("Test Description", modbusNode.getDescription(), "설명을 null로 설정해도 기존 값이 유지되어야 합니다.");
    }

    /**
     * ModbusNode가 중지 후 리소스를 해제하는지 테스트합니다.
     */
    @Test
    void testStop() {
        modbusNode.isConnected.set(false);

        modbusNode.stop();

        assertFalse(modbusNode.isConnected.get(), "ModbusNode는 중지 후 연결되지 않은 상태여야 합니다.");
        verify(mockMaster, times(1)).destroy();
    }
}
