package com.samsa.node.out;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class ModbusNodeTest {

    private ModbusNode modbusNode;
    private static final String NODE_ID = UUID.randomUUID().toString();
    private static final String HOST = "192.168.70.203";
    private static final int PORT = 502;
    private static final int SLAVE_ADDRESS = 0;
    private static final int SLAVE_ID = 1;
    private static final int REGISTER_COUNT = 10;

    @Mock
    private ModbusMaster modbusMaster;
    @Mock
    private ModbusFactory modbusFactory;
    @Mock
    private ReadHoldingRegistersResponse response;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        modbusNode = new ModbusNode(NODE_ID, HOST, PORT, SLAVE_ADDRESS, SLAVE_ID, REGISTER_COUNT);
        // ModbusFactory 모킹 추가
        when(modbusFactory.createTcpMaster(any(), anyBoolean())).thenReturn(modbusMaster);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (modbusNode != null) {
            modbusNode.stop();
        }
        closeable.close();
    }

    @Test
    void constructor_WithValidParameters_ShouldCreateInstance() {
        assertNotNull(modbusNode);
    }

    @Test
    void constructor_WithInvalidHost_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModbusNode(NODE_ID, "", PORT, SLAVE_ADDRESS, SLAVE_ID, REGISTER_COUNT));
    }

    @Test
    void constructor_WithInvalidPort_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModbusNode(NODE_ID, HOST, -1, SLAVE_ADDRESS, SLAVE_ID, REGISTER_COUNT));
    }

    @Test
    void constructor_WithInvalidSlaveAddress_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModbusNode(NODE_ID, HOST, PORT, -1, SLAVE_ID, REGISTER_COUNT));
    }

    @Test
    void start_Success_ShouldInitializeConnection() throws ModbusInitException {
        when(modbusFactory.createTcpMaster(any(), anyBoolean())).thenReturn(modbusMaster);
        doNothing().when(modbusMaster).init();

        modbusNode.start();

        verify(modbusMaster, times(1));
    }

    @Test
    void start_WithRetry_ShouldAttemptReconnection() throws ModbusInitException {
        when(modbusFactory.createTcpMaster(any(), anyBoolean())).thenReturn(modbusMaster);
        doThrow(ModbusInitException.class)
                .doThrow(ModbusInitException.class)
                .doNothing()
                .when(modbusMaster).init();

        modbusNode.start();

        verify(modbusMaster, times(3));
    }

    @Test
    void onMessage_WithNullMessage_ShouldNotProcess() throws ModbusTransportException {
        modbusNode.onMessage(null);

        verify(modbusMaster, never()).send(any(ReadHoldingRegistersRequest.class));
    }

    @Test
    public void stop_ShouldDestroyMaster() {
        ModbusMaster modbusMasterMock = mock(ModbusMaster.class); // 모의 객체
        ModbusNode modbusNode = new ModbusNode(NODE_ID, "localhost", 502, 0, 1, 10);
        modbusNode.setMaster(modbusMasterMock); // master 설정

        // stop() 호출
        modbusNode.stop();

        // destroy() 메서드가 호출되었는지 확인
        verify(modbusMasterMock, times(1)).destroy();
    }

    @Test
    void setDescription_WithValidString_ShouldSetDescription() {
        String testDescription = "Test Description";

        modbusNode.setDescription(testDescription);

        assertEquals(testDescription, (modbusNode).getDescription());
    }

    @Test
    void setDescription_WithNull_ShouldNotChangeDescription() {
        String initialDescription = modbusNode.getDescription();

        modbusNode.setDescription(null);

        assertEquals(initialDescription, modbusNode.getDescription());
    }

    @Test
    void processRegisterData_WithValidData_ShouldReturnMap() {
        short[] testData = new short[] { 100, 200 };
        modbusNode.setDescription("test");

        Map<String, Object> result = modbusNode.processRegisterData(testData);

        assertNotNull(result);
        assertTrue(result.containsKey("test"));
        assertArrayEquals(testData, (short[]) result.get("test"));
    }

    @Test
    void processRegisterData_WithEmptyDescription_ShouldUseDefault() {
        short[] testData = new short[] { 100, 200 };

        Map<String, Object> result = modbusNode.processRegisterData(testData);

        assertNotNull(result);
        assertTrue(result.containsKey("default"));
    }
}
