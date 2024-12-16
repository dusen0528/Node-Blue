package com.samsa.node.in;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samsa.core.InPort;
import com.samsa.core.Message;

/**
 * {@code TcpOutNodeTest} 클래스는 {@code TcpOutNode}의 단위 및 통합 테스트를 수행합니다.
 * 
 * <p>
 * 이 클래스는 다음의 주요 기능을 검증합니다:
 * </p>
 * <ul>
 * <li>TCP 연결 설정 및 메시지 전송 테스트</li>
 * <li>TCP 연결이 끊어졌을 때의 재연결 메커니즘 테스트</li>
 * <li>리소스 해제 및 연결 종료 테스트</li>
 * </ul>
 * 
 * <p>
 * 모든 테스트는 {@code JUnit 5}와 {@code Mockito}를 사용하여 구현되었습니다.
 * </p>
 * 
 * @author samsa
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TcpOutNodeTest {

    /** 테스트용 TCP 서버의 호스트 주소 */
    private static final String TEST_HOST = "localhost";

    /** 테스트용 TCP 서버의 포트 번호 */
    private static final int TEST_PORT = 1234;

    /** {@code InPort} 모의(Mock) 객체 */
    @Mock
    private InPort mockInPort;

    /** 테스트 대상인 {@code TcpOutNode} 인스턴스 */
    private TcpOutNode tcpOutNode;

    /**
     * 각 테스트 메서드 실행 전에 {@code TcpOutNode} 인스턴스를 초기화합니다.
     * 
     * <p>
     * Mock 객체를 사용하여 {@code InPort}를 초기화합니다.
     * </p>
     */
    @BeforeEach
    public void setUp() {
        tcpOutNode = new TcpOutNode(TEST_HOST, TEST_PORT, mockInPort);
    }

    /**
     * TCP 소켓을 통한 메시지 전송 기능을 테스트합니다.
     * 
     * <p>
     * 테스트 절차는 다음과 같습니다:
     * </p>
     * <ol>
     * <li>서버 소켓을 생성하여 메시지를 수신합니다.</li>
     * <li>클라이언트(테스트 대상)가 서버에 연결합니다.</li>
     * <li>테스트 메시지를 전송한 후, 서버에서 해당 메시지를 수신합니다.</li>
     * <li>수신된 메시지가 예상한 메시지와 일치하는지 검증합니다.</li>
     * </ol>
     * 
     * @throws IOException          소켓 연결 중 발생할 수 있는 예외
     * @throws InterruptedException 스레드 동기화 중 발생할 수 있는 예외
     */
    @Test
    public void testMessageSending() throws IOException, InterruptedException {
        try (ServerSocket serverSocket = new ServerSocket(TEST_PORT)) {
            CountDownLatch latch = new CountDownLatch(1);

            // 서버 스레드 - 메시지를 수신하고 검증
            Thread serverThread = new Thread(() -> {
                try (Socket clientSocket = serverSocket.accept();
                        var reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(clientSocket.getInputStream()))) {

                    String receivedMessage = reader.readLine();
                    assertEquals("Test Message", receivedMessage, "서버가 예상한 메시지를 수신하지 못했습니다.");
                    latch.countDown();
                } catch (IOException e) {
                    fail("서버 소켓 처리 중 예외 발생: " + e.getMessage());
                }
            });
            serverThread.start();

            // TcpOutNode 시작
            tcpOutNode.start();

            // 메시지 전송
            Message testMessage = new Message("Test Message\n");
            Mockito.when(mockInPort.consume()).thenReturn(testMessage);
            tcpOutNode.receive();

            // 메시지 수신 대기
            assertFalse(latch.await(5, TimeUnit.SECONDS), "메시지 수신 타임아웃");

            // 리소스 정리
            tcpOutNode.stop();
        }
    }

    /**
     * TCP 연결이 끊어진 후 소켓 재연결을 테스트합니다.
     * 
     * <p>
     * 테스트 절차는 다음과 같습니다:
     * </p>
     * <ol>
     * <li>서버 소켓을 닫아 연결을 끊습니다.</li>
     * <li>새로운 서버 소켓을 생성하여 클라이언트의 재연결을 기다립니다.</li>
     * <li>테스트 메시지를 전송하고, 서버에서 수신합니다.</li>
     * <li>수신된 메시지가 예상한 메시지와 일치하는지 검증합니다.</li>
     * </ol>
     * 
     * @throws IOException          소켓 연결 중 발생할 수 있는 예외
     * @throws InterruptedException 스레드 동기화 중 발생할 수 있는 예외
     */
    @Test
    public void testSocketReconnection() throws IOException, InterruptedException {
        // 첫 번째 서버 소켓 생성 후 연결 종료
        try (ServerSocket firstServerSocket = new ServerSocket(TEST_PORT)) {
            Socket firstClientSocket = firstServerSocket.accept();
            firstClientSocket.close();
        }

        // 두 번째 서버 소켓 생성
        try (ServerSocket secondServerSocket = new ServerSocket(TEST_PORT)) {
            CountDownLatch latch = new CountDownLatch(1);

            // 서버 스레드 - 메시지를 수신하고 검증
            Thread serverThread = new Thread(() -> {
                try (Socket clientSocket = secondServerSocket.accept();
                        var reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(clientSocket.getInputStream()))) {

                    String receivedMessage = reader.readLine();
                    assertEquals("Reconnection Test", receivedMessage, "재연결 후 서버가 예상한 메시지를 수신하지 못했습니다.");
                    latch.countDown();
                } catch (IOException e) {
                    fail("서버 소켓 처리 중 예외 발생: " + e.getMessage());
                }
            });
            serverThread.start();

            // TcpOutNode 시작
            tcpOutNode.start();

            // 메시지 전송
            Message reconnectionMessage = new Message("Reconnection Test\n");
            Mockito.when(mockInPort.consume()).thenReturn(reconnectionMessage);
            tcpOutNode.receive();

            // 메시지 수신 대기
            assertFalse(latch.await(5, TimeUnit.SECONDS), "재연결 후 메시지 수신 타임아웃");

            // 리소스 정리
            tcpOutNode.stop();
            secondServerSocket.close();
        }
    }
}