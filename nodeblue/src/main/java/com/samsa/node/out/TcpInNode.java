package com.samsa.node.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.samsa.core.Message;
import com.samsa.core.OutNode;
import com.samsa.core.OutPort;

import lombok.extern.slf4j.Slf4j;

/**
 * TCP 소켓을 통해 외부 클라이언트의 연결을 수락하고 수신한 데이터를
 * 출력 포트를 통해 내보내는 출력 노드입니다.
 * 
 * <p>
 * 이 클래스는 {@link OutNode}를 확장하여 구현되었으며,
 * 서버 소켓을 생성하여 클라이언트의 연결을 수락하고, 클라이언트로부터 수신된 데이터를
 * {@link Message} 객체로 변환한 후 출력 포트({@link OutPort})로 내보냅니다.
 * <p>
 * 주요 기능:
 * 
 * 
 * <li>TCP 서버 소켓 관리: 클라이언트의 연결을 수락하고 데이터를 수신합니다.</li>
 * <li>스레드 기반 연결 처리: 클라이언트의 각 연결을 별도의 스레드로 관리합니다.</li>
 * <li>리소스 관리: 서버 소켓과 클라이언트 소켓을 안전하게 닫고 스레드 풀을 종료합니다.</li>
 * <li>동시성 제어: {@code volatile} 변수와 {@link ExecutorService}를 사용하여 다중 스레드 환경을
 * 관리합니다.</li>
 * </ul>
 * 
 * @author samsa
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class TcpInNode extends OutNode {
    private ServerSocket serverSocket; // TCP 서버 소켓
    private final int port; // 서버가 수신 대기할 포트 번호
    private final ExecutorService executorService; // 클라이언트 연결 처리를 위한 스레드 풀
    private volatile boolean isRunning = false; // 서버의 실행 상태를 나타내는 플래그

    /**
     * 지정된 포트와 출력 포트로 TcpInNode를 생성합니다.
     * 
     * <p>
     * 이 생성자는 서버 소켓을 생성하고 클라이언트 연결을 수락할 준비를 합니다.
     * </p>
     *
     * @param port    클라이언트의 연결을 수락할 TCP 포트 번호 (예: 8080)
     * @param outPort 메시지를 전달할 출력 포트 ({@link OutPort} 인스턴스)
     *                {@code ExecutorService}에 대한 설명은 {@link notion
     *                https://url.kr/a6n85t}
     *                상단 참고
     */
    public TcpInNode(int port, OutPort outPort) {
        super(outPort); // 순환참조?
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 지정된 ID, 포트 및 출력 포트로 TcpInNode를 생성합니다.
     *
     * @param id      노드의 고유 식별자 (UUID)
     * @param port    클라이언트의 연결을 수락할 TCP 포트 번호 (예: 8080)
     * @param outPort 메시지를 전달할 출력 포트 ({@link OutPort} 인스턴스)
     */
    public TcpInNode(UUID id, int port, OutPort outPort) {
        super(id, outPort);// 순환참조?
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 노드의 실행을 시작합니다.
     * 
     * <p>
     * 서버 소켓을 생성하고 클라이언트 연결을 비동기적으로 수락하기 위한
     * 별도의 스레드를 시작합니다. 부모 클래스의 {@code start()} 메서드도 호출됩니다.
     * </p>
     * 
     * @param ServerSocket
     *                     {@code isRunning} - {@link notion 에 2번 참조
     *                     https://url.kr/a6n85t}
     * @see executorService.submit(this::acceptConnections)
     *      //서버 소켓의 연결을 수락하는 메서드(acceptConnections)를 비동기 작업으로 스레드 풀에 제출합니다.
     */
    @Override
    public void start() {
        super.start();
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            executorService.submit(this::acceptConnections);
            log.info("TCP 서버 노드 시작됨. 포트: {}", port);
        } catch (IOException e) {
            handleError(e);
        }
    }

    /**
     * 클라이언트 연결을 지속적으로 수락하고 처리하는 메서드입니다.
     * 
     * <p>
     * 이 메서드는 연결이 들어올 때마다 {@link #handleClientConnection(Socket)} 메서드를
     * 별도의 스레드에서 실행합니다. 서버가 중지될 때까지 계속 실행됩니다.
     * </p>
     */
    private void acceptConnections() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (!isRunning) {
                    log.info("서버가 정상적으로 중지되었습니다.");
                    break;
                }
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                log.error("클라이언트 연결 수락 중 오류 발생", e);
            }
        }
    }

    /**
     * 개별 클라이언트 연결을 처리하는 메서드입니다.
     * 
     * <p>
     * 클라이언트 소켓으로부터 데이터를 수신하고, 수신된 데이터를 {@link Message}로
     * 변환하여 출력 포트를 통해 내보냅니다. 연결 종료 후 클라이언트 소켓을 닫습니다.
     * </p>
     *
     * @param clientSocket 클라이언트 소켓
     */
    private void handleClientConnection(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                Message message = new Message(inputLine);
                emit(message);
            }
        } catch (IOException e) {
            log.error("클라이언트 연결 처리 중 오류 발생", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("소켓 닫기 중 오류 발생", e);
            }
        }
    }

    /**
     * 노드를 중지하고 리소스를 정리합니다.
     * 
     * <p>
     * 서버 소켓을 닫고, 스레드 풀을 종료합니다.
     * 부모 클래스의 {@code stop()} 메서드도 호출됩니다.
     * </p>
     * 
     * @see ExecutorService#shutdown()
     *      // 스레드 풀 종료 (현재 실행 중인 작업은 끝까지 실행)
     */
    @Override
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();
        } catch (IOException e) {
            handleError(e);
        }
        super.stop();
    }

}

/**
 * // 사용예시
 * 
 * OutPort outPort = new OutPort();
 * 
 * // TcpInNode 생성 (포트: 8080)
 * TcpInNode tcpInNode = new TcpInNode(UUID.randomUUID(), 8080, outPort);
 * 
 * // TcpInNode 실행
 * tcpInNode.start();
 * 
 * // 서버를 30초 후에 자동 종료 (테스트용)
 * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
 * System.out.println("서버 종료 중...");
 * tcpInNode.stop();
 * System.out.println("서버가 정상적으로 종료되었습니다.");
 * }));
 * 
 * try {
 * System.out.println("서버가 30초 동안 실행됩니다. 클라이언트를 통해 데이터를 전송해보세요.");
 * Thread.sleep(30_000); // 30초 대기
 * } catch (InterruptedException e) {
 * e.printStackTrace();
 * }
 * 
 * System.exit(0); // 애플리케이션 종료
 */