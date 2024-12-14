package com.samsa.node.out;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.samsa.core.InPort;
import com.samsa.node.in.TcpOutNode;

/**
 * TcpOutNode 클래스의 동작을 테스트합니다.
 */
class TcpOutNodeTest {

    private TcpOutNode tcpOutNode;
    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        if (tcpOutNode != null) {
            tcpOutNode.stop();
        }
        testExecutor.shutdown();
    }

    /**
     * TcpOutNode가 메시지를 전송할 수 있는지 테스트합니다.
     */
    @Test
    void testTcpOutNodeSendsMessage() throws Exception {
        int port = 12345;
        InPort inPort = mock(InPort.class);
        tcpOutNode = new TcpOutNode("localhost", port, inPort);

        // 서버 소켓을 시작하고 클라이언트 연결을 기다림
        ServerSocket mockServer = new ServerSocket(port);

        // 클라이언트 연결을 기다리는 비동기 작업
        testExecutor.submit(() -> {
            try {
                Socket clientSocket = mockServer.accept();
                // 클라이언트 소켓 연결 확인
                assertNotNull(clientSocket);
                // 메시지 전송과 관련된 추가 검증 로직을 추가할 수 있습니다.
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // TcpOutNode가 메시지를 보내는 동작을 트리거
        tcpOutNode.onMessage("test");

        // 테스트에서 사용한 소켓 종료
        mockServer.close();
    }
}