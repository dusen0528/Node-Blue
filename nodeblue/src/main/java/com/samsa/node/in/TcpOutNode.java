package com.samsa.node.in;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import com.samsa.core.InNode;
import com.samsa.core.InPort;
import com.samsa.core.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * TCP 소켓을 통해 데이터를 전송하는 입력 노드입니다.
 * 
 * <p>
 * 이 클래스는 {@link InNode}를 확장하여 구현되었으며,
 * 입력 포트({@link InPort})로부터 수신한 메시지를 지정된 TCP 소켓을 통해 외부 서버로 전송합니다.
 * TCP 소켓 연결이 끊어지거나 닫힌 경우 자동으로 재연결을 시도합니다.
 * </p>
 * 
 * <p>
 * 주요 기능:
 * </p>
 * <ul>
 * <li>**TCP 연결 관리**: 소켓이 닫히거나 끊어지면 재연결을 수행합니다.</li>
 * <li>**스레드 기반 메시지 전송**: 입력 포트로부터 메시지를 비동기 방식으로 수신하고 전송합니다.</li>
 * <li>**리소스 관리**: stop() 호출 시 소켓과 출력 스트림을 안전하게 닫고 자원을 해제합니다.</li>
 * <li>**로깅 및 예외 처리**: TCP 연결 및 메시지 전송 과정에서 발생한 예외를 로그로 기록합니다.</li>
 * </ul>
 * 
 * @author samsa
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class TcpOutNode extends InNode {
    private final String host; // 데이터 전송 대상 호스트
    private final int port; // 데이터 전송 대상 포트 번호
    private Socket socket; // TCP 소켓
    private PrintWriter out; // 소켓의 출력 스트림

    /**
     * 지정된 호스트, 포트 및 입력 포트로 TcpOutNode를 생성합니다.
     *
     * @param host   데이터를 전송할 대상 호스트 (예: "localhost")
     * @param port   데이터를 전송할 대상 포트 번호 (예: 8080)
     * @param inPort 메시지를 수신할 입력 포트 ({@link InPort} 인스턴스)
     */
    public TcpOutNode(String host, int port, InPort inPort) {
        super(inPort);
        this.host = host;
        this.port = port;
    }

    /**
     * 지정된 ID, 호스트, 포트 및 입력 포트로 TcpOutNode를 생성합니다.
     *
     * @param id     노드의 고유 식별자 (UUID)
     * @param host   데이터를 전송할 대상 호스트 (예: "localhost")
     * @param port   데이터를 전송할 대상 포트 번호 (예: 8080)
     * @param inPort 메시지를 수신할 입력 포트 ({@link InPort} 인스턴스)
     */
    public TcpOutNode(UUID id, String host, int port, InPort inPort) {
        super(id, inPort);
        this.host = host;
        this.port = port;
    }

    /**
     * 노드의 실행을 시작하고 메시지 처리 루프를 실행합니다.
     * 
     * <p>
     * 입력 포트로부터 메시지를 비동기적으로 수신하여 TCP 소켓을 통해 전송합니다.
     * 스레드가 인터럽트되면 루프가 종료됩니다.
     * </p>
     */
    @Override
    public void run() {
        super.run();
        while (!Thread.currentThread().isInterrupted()) {
            Message message = receive();
            if (message != null) {
                sendMessage(message);
            }
        }
    }

    /**
     * 노드를 시작하고 TCP 소켓 연결을 초기화합니다.
     * 
     * <p>
     * 부모 클래스의 {@code start()} 메서드를 호출한 후 TCP 소켓을 생성합니다.
     * 소켓이 생성되지 않거나 연결이 실패하면 {@link IOException} 예외를 처리합니다.
     * </p>
     */
    @Override
    public void start() {
        super.start();
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            handleError(e);
        }
    }

    /**
     * 입력 포트로부터 수신한 메시지를 TCP 소켓을 통해 전송합니다.
     * 
     * <p>
     * 만약 소켓이 닫혀 있거나 null인 경우 자동으로 재연결을 시도합니다.
     * 재연결에 실패할 경우 예외가 발생하며, 예외 정보는 로그에 기록됩니다.
     * </p>
     *
     * @param message 전송할 메시지 ({@link Message} 인스턴스)
     */
    private void sendMessage(Message message) {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
            }
            out.println(message.getPayload().toString());
            log.info("메시지 전송 완료. 호스트: {}, 포트: {}, 메시지: {}", host, port, message);
        } catch (IOException e) {
            handleError(e);
        }
    }

    /**
     * 예기치 않은 메시지 수신을 처리합니다.
     * 
     * <p>
     * 이 메서드는 TcpOutNode에서 일반적으로 호출되지 않습니다.
     * 메시지는 수신 후 {@link #sendMessage(Message)} 메서드를 통해 처리됩니다.
     * 이 메서드가 호출되면 경고 로그를 남깁니다.
     * </p>
     *
     * @param message 수신된 메시지 ({@link Message} 인스턴스)
     */
    @Override
    public void onMessage(Message message) {
        log.warn("TcpOutNode에서 예기치 않은 메시지 수신. 메시지: {}", message);
    }

    /**
     * 노드를 중지하고 리소스를 정리합니다.
     * 
     * <p>
     * 이 메서드는 {@code stop()} 메서드가 호출될 때 실행됩니다.
     * TCP 소켓과 출력 스트림을 안전하게 닫고 자원을 해제합니다.
     * 소켓이나 스트림을 닫는 중 예외가 발생하면 예외 정보를 로그에 기록합니다.
     * </p>
     */
    @Override
    public void stop() {
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            handleError(e);
        } finally {
            super.stop();
        }
    }

}

/**
 * //사용예시
 * 
 * InPort inPort = new InPort(UUID.randomUUID());
 * 
 * // TcpOutNode 생성 (서버 호스트와 포트 번호 설정)
 * String host = "localhost"; // 대상 호스트 (예: "localhost")
 * int port = 8080; // 대상 포트 번호 (예: 8080)
 * 
 * // 입력 포트와 호스트, 포트 번호로 TcpOutNode 생성
 * TcpOutNode tcpOutNode = new TcpOutNode(host, port, inPort);
 * 
 * // 노드를 시작하여 TCP 연결을 설정
 * tcpOutNode.start();
 * 
 * // 메시지 생성 (전송할 메시지 예시)
 * Message message = new Message("Hello, TCP Server!");
 * 
 * // 입력 포트에 메시지 수신 (예시로, 해당 메시지를 수동으로 수신)
 * inPort.send(message);
 * 
 * // TcpOutNode가 메시지를 전송할 때 자동으로 처리됩니다.
 * 
 * // 노드 종료 시 리소스 정리
 * tcpOutNode.stop();
 * }
 */