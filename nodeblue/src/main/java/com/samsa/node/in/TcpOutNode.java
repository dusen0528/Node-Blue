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
 * TCP 소켓을 통해 데이터를 전송하는 노드입니다.
 * 입력 포트로부터 메시지를 수신하고 지정된 호스트와 포트로 데이터를 전송합니다.
 * 
 * @author samsa
 * @since 1.0
 */
@Slf4j
public class TcpOutNode extends InNode {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;

    /**
     * 지정된 호스트, 포트, 입력 포트로 TcpOutNode를 생성합니다.
     *
     * @param host   데이터를 전송할 대상 호스트
     * @param port   데이터를 전송할 대상 포트 번호
     * @param inPort 메시지를 수신할 입력 포트
     */
    public TcpOutNode(String host, int port, InPort inPort) {
        super(inPort);
        this.host = host;
        this.port = port;
    }

    /**
     * 지정된 ID, 호스트, 포트, 입력 포트로 TcpOutNode를 생성합니다.
     *
     * @param id     노드의 고유 식별자
     * @param host   데이터를 전송할 대상 호스트
     * @param port   데이터를 전송할 대상 포트 번호
     * @param inPort 메시지를 수신할 입력 포트
     */
    public TcpOutNode(UUID id, String host, int port, InPort inPort) {
        super(id, inPort);
        this.host = host;
        this.port = port;
    }

    /**
     * 노드의 실행을 시작하고 메시지 처리 루프를 실행합니다.
     * 입력 포트로부터 메시지를 수신하여 TCP 소켓을 통해 전송합니다.
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
     * 부모 클래스의 start() 메소드를 호출하고 TCP 소켓을 생성합니다.
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
     * 메시지를 TCP 소켓을 통해 전송합니다.
     * 소켓이 닫혀있거나 null인 경우 새로운 연결을 생성합니다.
     *
     * @param message 전송할 메시지
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
     * TcpOutNode는 receive() 메서드를 통해 메시지를 처리하므로,
     * 이 메서드가 호출되면 경고 로그를 남깁니다.
     *
     * @param message 수신된 메시지
     */
    @Override
    public void onMessage(Message message) {
        // TcpOutNode는 receive() 메서드를 통해 메시지를 처리합니다.
        log.warn("TcpOutNode에서 예기치 않은 메시지 수신");
    }

    /**
     * 노드를 중지하고 리소스를 정리합니다.
     * TCP 소켓과 출력 스트림을 닫고 부모 클래스의 stop() 메소드를 호출합니다.
     */
    @Override
    public void stop() {
        try {
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            handleError(e);
        }
        super.stop();
    }

    public void onMessage(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onMessage'");
    }
}