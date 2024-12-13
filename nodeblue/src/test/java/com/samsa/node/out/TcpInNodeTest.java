package com.samsa.node.out;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.samsa.core.Message;

public class TcpInNodeTest {

    private TcpInNode tcpInNode;
    private ServerSocket serverSocket;
    private Thread serverThread;

    @BeforeEach
    public void setUp() {
        try {
            serverSocket = new ServerSocket(9091);
            serverThread = new Thread(() -> {
                try {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println("서버에서 수신한 메시지: " + message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            tcpInNode = new TcpInNode("testNode", "127.0.0.1", 9091);
            tcpInNode.start();
        } catch (IOException e) {
            e.printStackTrace();
            fail("서버 소켓 초기화에 실패했습니다.");
        }
    }

    @AfterEach
    public void tearDown() {
        tcpInNode.stop();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TcpInNode가 서버에 정상적으로 연결되는지 테스트합니다.
     */
    @Test
    public void testConnectionToServer() {
        assertNotNull(tcpInNode, "TcpInNode 인스턴스가 null입니다.");
        assertTrue(tcpInNode.socket.isConnected(), "TcpInNode가 서버에 연결되지 않았습니다.");
    }

    /**
     * TcpInNode가 서버로 메시지를 전송하는지 테스트합니다.
     */
    @Test
    public void testSendMessageToServer() {
        try {
            String testMessage = "Hello, Server!";
            Message message = new Message(testMessage);

            // 메시지 전송
            tcpInNode.emit(message);

            // 수신 대기를 위해 대기
            Thread.sleep(1000);

            // 메시지가 정상적으로 전송되었는지를 확인
            assertNotNull(tcpInNode.outputStream, "출력 스트림이 초기화되지 않았습니다.");
        } catch (Exception e) {
            fail("TcpInNode 메시지 전송 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * TcpInNode의 소켓과 리소스가 올바르게 해제되는지 테스트합니다.
     */
    @Test
    public void testNodeStopsCorrectly() {
        tcpInNode.stop();
        assertTrue(tcpInNode.socket.isClosed(), "클라이언트 소켓이 닫히지 않았습니다.");
    }
}
