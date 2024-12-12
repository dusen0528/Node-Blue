package com.samsa.node.out;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.samsa.core.Message;
import com.samsa.core.OutNode;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code TcpInNode} 클래스는 TCP connect을 통해 데이터를 전송하는 출력을 생성하는 노드입니다.
 * 이 노드는 {@link OutNode}를 확장하여 TCP connect을 통한 데이터 전송 기능을 구현합니다.
 */
@Slf4j
public class TcpInNode extends OutNode {
    /** TCP 서버의 호스트 주소 */
    private String host;
    /** TCP 서버의 포트 번호 */
    private int port;
    /** connect 종류 ("대기" 또는 "connect") */
    private String connectionType;
    /** 출력 형식 ("stream" 또는 "바이너리 버퍼") */
    private String outputFormat;
    /** 메시지의 토픽 */
    private String topic;
    /** 사용자 정의 이름 */
    private String name;
    /** TCP 소켓 인스턴스 */
    private Socket socket;
    /** TCP 데이터를 전송하는 출력 stream */
    private OutputStream outputStream;

    /**
     * {@code TcpInNode}의 생성자.
     * 
     * @param id   노드의 고유 식별자
     * @param host connect할 TCP 서버의 호스트 주소 (예: 127.0.0.1)
     * @param port connect할 TCP 서버의 포트 번호 (예: 8080)
     */
    public TcpInNode(String id, String host, int port) {
        super(id); // OutNode의 기본 생성자 호출
        this.host = host;
        this.port = port;

    }

    /**
     * 노드를 시작합니다.
     * <p>
     * TCP 서버와 연결을 설정하고, 소켓과 출력 stream을 초기화합니다.
     * </p>
     */
    @Override
    public void start() {
        super.start();
        try {
            // 소켓이 연결된 후 출력 스트림을 얻습니다.
            outputStream = socket.getOutputStream();
            log.info("TcpInNode[{}] 연결됨 - {}:{}", getId(), host, port);
        } catch (IOException e) {
            handleError(e);
        }
    }

    /**
     * 노드를 중지합니다.
     * <p>
     * 연결된 소켓과 출력 stream을 닫아 연결을 해제합니다.
     * </p>
     */
    @Override
    public void stop() {
        super.stop();
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
            log.info("TcpInNode[{}] connect 종료됨", getId());
        } catch (IOException e) {
            handleError(e);
        }
    }

    /**
     * 메시지를 TCP connect을 통해 전송합니다.
     * 
     * @param message 전송할 메시지 객체
     */
    @Override
    public void emit(Message message) {
        super.emit(message);
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log.warn("TcpInNode[{}] 소켓이 연결되지 않음", getId());
            return;
        }
        try {
            byte[] messageBytes;
            if ("binary buffer".equals(outputFormat)) {
                // 바이너리 형식으로 메시지를 변환
                messageBytes = message.getPayloadAsBinary();
            } else {
                // 문자열(stream) 형식으로 메시지를 변환
                messageBytes = message.getPayload().toString().getBytes();
            }

            if (messageBytes.length > 0) {
                outputStream.write(messageBytes);
                outputStream.flush();
                log.info("TcpInNode[{}] 메시지 전송: {}", getId(), new String(messageBytes));
            } else {
                throw new IndexOutOfBoundsException("메시지는 최소 1바이트 이상이어야 합니다.");
            }
        } catch (IOException e) {
            handleError(e);
        }
    }

    // Getter 및 Setter 추가

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}