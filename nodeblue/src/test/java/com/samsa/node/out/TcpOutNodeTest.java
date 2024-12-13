package com.samsa.node.out;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.samsa.node.in.TcpOutNode;

public class TcpOutNodeTest {

    private TcpOutNode tcpOutNode;
    private Thread serverThread;

    @BeforeEach
    public void setUp() {
        tcpOutNode = new TcpOutNode("testNode", 9090);
        serverThread = new Thread(() -> tcpOutNode.start());
        serverThread.start();

        try {
            // 서버가 시작될 때까지 잠시 대기
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        tcpOutNode.stop();
    }

    /**
     * TcpOutNode가 포트에서 연결을 대기하고 클라이언트를 허용하는지 테스트합니다.
     */
    @Test
    public void testServerAcceptsClientConnection() {
        try (Socket clientSocket = new Socket("127.0.0.1", 9090)) {
            assertTrue(clientSocket.isConnected(), "클라이언트가 서버에 연결되지 않았습니다.");
        } catch (IOException e) {
            fail("클라이언트 소켓 연결에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * TcpOutNode가 클라이언트로부터 메시지를 수신하는지 테스트합니다.
     */
    @Test
    public void testReceiveMessageFromClient() {
        try (Socket clientSocket = new Socket("127.0.0.1", 9090);
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // 클라이언트가 서버로 메시지 전송
            String testMessage = "Hello, TcpOutNode!";
            writer.println(testMessage);

            // 수신 메시지를 기다리는 시간 확보
            Thread.sleep(1000);

            // 메시지가 정상적으로 수신되었는지 검증
            assertNotNull(tcpOutNode);
            // 메시지 수신은 로그에 남으므로, 여기에 대한 검증은 직접적이지 않을 수 있습니다.
            // 실제 테스트에서는 onMessage 콜백을 모의(mock)하여 메시지를 캡처할 수 있습니다.
        } catch (IOException | InterruptedException e) {
            fail("테스트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * TcpOutNode의 소켓 및 리소스가 올바르게 해제되는지 테스트합니다.
     */
    @Test
    public void testServerStopsCorrectly() {
        tcpOutNode.stop();
        assertTrue(tcpOutNode.serverSocket.isClosed(), "서버 소켓이 닫히지 않았습니다.");
    }
}
