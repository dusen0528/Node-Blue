package com.samsa.node.out;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.samsa.core.InPort;
import com.samsa.core.Message;
import com.samsa.node.in.TcpOutNode;

/**
 * TcpOutNode 클래스의 TCP 클라이언트 기능을 테스트합니다.
 * 서버 연결 및 메시지 전송 기능을 검증합니다.
 */
class TcpOutNodeTest {

    private TcpOutNode tcpOutNode;
    private ExecutorService testExecutor;
    private ServerSocket mockServer;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tcpOutNode != null)
            tcpOutNode.stop();
        if (mockServer != null)
            mockServer.close();
        testExecutor.shutdown();
    }

    /**
     * TcpOutNode가 서버에 연결하고 메시지를 전송하는지 테스트합니다.
     */
    @Test
    void testTcpOutNodeSendsMessage() throws Exception {
        int port = 12345;
        InPort inPort = mock(InPort.class);
        mockServer = new ServerSocket(port);
        tcpOutNode = new TcpOutNode("localhost", port, inPort);

        // 서버 소켓 리스닝 시작
        Future<String> serverFuture = testExecutor.submit(() -> {
            try (Socket clientSocket = mockServer.accept();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()))) {
                return reader.readLine();
            }
        });

        tcpOutNode.start();
        tcpOutNode.onMessage(new Message("Test Message"));

        String receivedMessage = serverFuture.get(1, TimeUnit.SECONDS);
        assertEquals("Test Message", receivedMessage);
    }
}
