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
 * TCP 소켓을 통해 데이터를 수신하고 출력 포트로 메시지를 내보내는 노드입니다.
 * 서버 소켓을 생성하고 클라이언트 연결을 수락하여 데이터를 읽습니다.
 * 
 * @author samsa
 * @since 1.0
 *        {@link ExecutorService} - ExecutorService는 Java에서 멀티스레딩과 동시성 작업을 관리하기
 *        위한 고수준 API를 제공하는 중요한 인터페이스
 */
@Slf4j
public class TcpInNode extends OutNode {
    private ServerSocket serverSocket;
    private final int port;
    private final ExecutorService executorService;
    private volatile boolean isRunning = false;

    /**
     * 지정된 포트와 출력 포트로 TcpInNode를 생성합니다.
     *
     * @param port                 수신 대기할 TCP 포트 번호
     * @param outPort              메시지를 전달할 출력 포트
     * @param id                   노드의 고유 식별자
     * @param newChachedThreadPool 새로운 캐시 풀 스레드 풀을 생성
     * 
     * @since 1.0
     * @see Executors.newCachedThreadPool()은 필요에 따라 스레드를 동적으로 생성하고 재사용하며,
     *      60초 동안 사용되지 않은 스레드는 풀에서 제거됩니다.
     */
    public TcpInNode(int port, OutPort outPort) {
        super(outPort);
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 지정된 ID, 포트, 출력 포트로 TcpInNode를 생성합니다.
     *
     * @param id      노드의 고유 식별자
     * @param port    수신 대기할 TCP 포트 번호
     * @param outPort 메시지를 전달할 출력 포트
     */
    public TcpInNode(UUID id, int port, OutPort outPort) {
        super(id, outPort);
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 노드의 실행을 시작합니다.
     * 서버 소켓을 생성하고 클라이언트 연결을 수락하기 위한 스레드를 시작합니다.
     * 부모 클래스의 start() 메소드도 호출합니다.
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
     */
    private void acceptConnections() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                if (isRunning) {
                    log.error("클라이언트 연결 수락 중 오류 발생", e);
                }
            }
        }
    }

    /**
     * 개별 클라이언트 연결을 처리하는 메서드입니다.
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
     * 노드의 실행을 중지합니다.
     * 서버 소켓을 닫고 실행 중인 스레드 풀을 종료시킵니다.
     * 부모 클래스의 stop() 메소드도 호출합니다.
     */
    @Override
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            executorService.shutdown();
        } catch (IOException e) {
            handleError(e);
        }
        super.stop();
    }

}