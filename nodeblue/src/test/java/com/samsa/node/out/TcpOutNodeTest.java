package com.samsa.node.out;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samsa.core.InPort;
import com.samsa.core.Message;
import com.samsa.node.in.TcpOutNode;

/**
 * TcpOutNode의 단위 및 통합 테스트를 위한 JUnit 테스트 클래스입니다.
 * 
 * <p>
 * 이 테스트 클래스는 TcpOutNode의 다양한 시나리오와 동작을 검증합니다:
 * <ul>
 * <li>소켓 연결 및 메시지 전송</li>
 * <li>예외 처리 및 재연결 메커니즘</li>
 * <li>리소스 관리 및 노드 시작/중지</li>
 * </ul>
 * </p>
 * 
 * @author samsa
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TcpOutNodeTest {

    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 54321;

    @Mock
    private InPort mockInPort;

    private TcpOutNode tcpOutNode;

    /**
     * 각 테스트 메서드 실행 전 TcpOutNode 인스턴스를 설정합니다.
     * 
     * <p>
     * 모의(Mock) 입력 포트를 사용하여 TcpOutNode를 초기화합니다.
     * </p>
     */
    @BeforeEach
    public void setUp() {
        tcpOutNode = new TcpOutNode(TEST_HOST, TEST_PORT, mockInPort);
    }

    /**
     * TcpOutNode의 생성자 및 초기화를 테스트합니다.
     * 
     * <p>
     * 두 가지 생성자가 제대로 작동하는지 확인하고,
     * 기본 및 UUID 기반 생성자의 동작을 검증합니다.
     * </p>
     */
    @Test
    public void testConstructors() {
        // 기본 생성자 테스트
        TcpOutNode defaultNode = new TcpOutNode(TEST_HOST, TEST_PORT, mockInPort);
        assertNotNull(defaultNode);

        // UUID 기반 생성자 테스트
        UUID testId = UUID.randomUUID();
        TcpOutNode uuidNode = new TcpOutNode(testId, TEST_HOST, TEST_PORT, mockInPort);
        assertNotNull(uuidNode);
        assertEquals(testId, uuidNode.getId());
    }

    /**
     * 메시지 전송 시나리오를 테스트합니다.
     * 
     * <p>
     * 실제 TCP 서버를 생성하여 TcpOutNode가 메시지를 성공적으로 전송하는지 검증합니다.
     * </p>
     * 
     * @throws IOException          소켓 연결 중 발생할 수 있는 예외
     * @throws InterruptedException 스레드 동기화 중 발생할 수 있는 예외
     */
    @Test
    public void testMessageSending() throws IOException, InterruptedException {
        // 테스트용 서버 소켓 생성
        ServerSocket serverSocket = new ServerSocket(TEST_PORT);
        CountDownLatch latch = new CountDownLatch(1);

        // 서버 스레드 생성
        Thread serverThread = new Thread(() -> {
            try {
                Socket clientSocket = serverSocket.accept();
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
                String receivedMessage = reader.readLine();
                assertEquals("Test Message", receivedMessage);
                latch.countDown();
                clientSocket.close();
            } catch (IOException e) {
                fail("서버 소켓 처리 중 예외 발생: " + e.getMessage());
            }
        });
        serverThread.start();

        // TcpOutNode 시작
        tcpOutNode.start();

        // 메시지 전송
        Message testMessage = new Message("Test Message");
        mockInPort.send(testMessage);

        // 메시지 전송 대기
        assertTrue(latch.await(5, TimeUnit.SECONDS), "메시지 수신 타임아웃");

        // 리소스 정리
        tcpOutNode.stop();
        serverSocket.close();
    }

    /**
     * 소켓 재연결 시나리오를 테스트합니다.
     * 
     * <p>
     * 소켓 연결이 끊어졌을 때 자동으로 재연결되는 메커니즘을 검증합니다.
     * </p>
     * 
     * @throws IOException          소켓 연결 중 발생할 수 있는 예외
     * @throws InterruptedException 스레드 동기화 중 발생할 수 있는 예외
     */
    @Test
    public void testSocketReconnection() throws IOException, InterruptedException {
        // 첫 번째 서버 소켓
        ServerSocket firstServerSocket = new ServerSocket(TEST_PORT);
        Socket firstClientSocket = firstServerSocket.accept();
        firstClientSocket.close();
        firstServerSocket.close();

        // 두 번째 서버 소켓
        ServerSocket secondServerSocket = new ServerSocket(TEST_PORT);
        CountDownLatch latch = new CountDownLatch(1);

        Thread serverThread = new Thread(() -> {
            try {
                Socket clientSocket = secondServerSocket.accept();
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
                String receivedMessage = reader.readLine();
                assertEquals("Reconnection Test", receivedMessage);
                latch.countDown();
                clientSocket.close();
            } catch (IOException e) {
                fail("서버 소켓 처리 중 예외 발생: " + e.getMessage());
            }
        });
        serverThread.start();

        // TcpOutNode 시작
        tcpOutNode.start();

        // 메시지 전송 (내부적으로 재연결 시도)
        Message testMessage = new Message("Reconnection Test");
        mockInPort.send(testMessage);

        // 메시지 전송 대기
        assertTrue(latch.await(5, TimeUnit.SECONDS), "재연결 후 메시지 수신 타임아웃");

        // 리소스 정리
        tcpOutNode.stop();
        secondServerSocket.close();
    }

    /**
     * 노드 중지 시 리소스 해제를 테스트합니다.
     * 
     * <p>
     * stop() 메서드 호출 시 소켓과 출력 스트림이 정상적으로 닫히는지 검증합니다.
     * </p>
     */
    @Test
    public void testNodeStopping() {
        tcpOutNode.start();
        tcpOutNode.stop();

        // 추가 어설션이 필요할 경우 여기에 구현
        // 예: 소켓이 닫혔는지, 리소스가 해제되었는지 등
    }

    /**
     * 예외 처리 시나리오를 테스트합니다.
     * 
     * <p>
     * 잘못된 호스트나 포트로 인한 연결 실패 상황을 시뮬레이션합니다.
     * </p>
     */
    @Test
    public void testConnectionFailure() {
        // 존재하지 않는 포트로 노드 생성
        TcpOutNode failNode = new TcpOutNode("nonexistent.host", 99999, mockInPort);

        // start() 호출 시 예외 처리 검증
        assertDoesNotThrow(() -> failNode.start());
        // 로그 확인 등 추가 검증 로직 필요
    }
}
