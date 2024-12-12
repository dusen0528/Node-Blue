package com.samsa.node.out;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.samsa.core.Message;
import com.samsa.core.OutNode;

import lombok.extern.slf4j.Slf4j;

/**
 * TcpInNode 클래스
 * 
 * 이 클래스는 OutNode를 확장하여 TCP 연결을 통해 데이터를 전송하는 노드의 동작을 정의합니다.
 * 일반적으로 Node-RED의 TcpIn은 입력 노드이지만,
 * 이 구현에서는 Output 노드로 간주됩니다.
 */
@Slf4j
public class TcpInNode extends OutNode {
    private String host; // 연결할 TCP 서버의 호스트 주소 (예: 127.0.0.1)
    private int port; // 연결할 포트 번호 (예: 8080)
    private Socket socket; // TCP 소켓 객체로 서버와의 연결을 나타냅니다.
    private OutputStream outputStream; // TCP 데이터를 보내기 위한 출력 스트림

    /**
     * 생성자
     * 
     * @param id   노드의 고유 식별자 (Node ID)
     * @param host TCP 서버의 호스트 주소
     * @param port TCP 서버의 포트 번호
     * 
     *             OutNode의 생성자를 호출하여 id를 설정하고,
     *             host 및 port를 초기화합니다.
     */
    public TcpInNode(String id, String host, int port) {
        super(id); // 부모 클래스 OutNode의 생성자를 호출
        this.host = host; // TCP 연결할 호스트 주소를 초기화
        this.port = port; // TCP 연결할 포트 번호를 초기화
    }

    private void reconnect() {
        stop();
        start();
    }

    /**
     * 노드 시작 메소드
     * 
     * OutNode의 start() 메소드를 호출하여 노드를 시작한 후,
     * TCP 서버에 연결. 연결이 성공하면 소켓과 출력 스트림을 초기화.
     */
    @Override
    public void start() {
        super.start(); // 부모 클래스의 start 메소드 호출
        try {
            // 호스트와 포트를 사용하여 TCP 서버에 연결
            socket = new Socket(host, port);
            outputStream = socket.getOutputStream(); // 소켓의 출력 스트림을 가져옵니다.
            log.info("TcpInNode[{}]와 연결되었습니다. {}:{}", getId(), host, port);
        } catch (IOException e) {
            // 연결에 실패한 경우 에러 상태로 전환하고 로그에 기록.
            handleError(e);
            reconnect();
        }
    }

    /**
     * 노드 중지 메소드
     * 
     * OutNode의 stop() 메소드를 호출한 후,
     * 소켓과 출력 스트림을 닫아 연결을 해제.
     */
    @Override
    public void stop() {
        super.stop(); // 부모 클래스의 stop 메소드 호출
        try {
            // 출력 스트림을 닫습니다.
            if (outputStream != null) {
                outputStream.close();
            }
            // TCP 소켓을 닫습니다.
            if (socket != null) {
                socket.close();
            }
            log.info("TcpInNode[{}] 연결이 끊겼습니다", getId());
        } catch (IOException e) {
            // 소켓이나 스트림을 닫는 중에 발생한 예외를 처리.
            handleError(e);
        }
    }

    /**
     * 메시지를 TCP 연결을 통해 전송하는 메소드
     * 
     * @param message 전송할 메시지 객체
     * 
     *                메세지의 본문을 바이트 배열로 변환하여 소켓의 출력 스트림으로 전송합니다.
     */
    @Override
    public void emit(Message message) {
        super.emit(message); // 부모 클래스의 emit 메소드 호출
        // 소켓 연결이 닫혀 있거나 연결되지 않은 경우 경고 메시지를 기록합니다.
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log.warn("TcpInNode[{}] 소켓이 연결되지 않았습니다.", getId());
            reconnect();

        }
        try {
            // 메시지의 본문을 바이트 배열로 변환합니다.
            byte[] messageBytes = new byte[2048];
            /**
             * message.getBody() 그러나 Message 클래스 선언필요
             * 이유는 바이트 단위로 한정지으면 원하는 메시지가 전송이 안되고 바이트 단위로만 갈수도 있기때문
             */
            // test
            // 바이트 데이터를 출력 스트림으로 전송합니다.
            if (messageBytes != null && messageBytes.length > 0) {
                outputStream.write(messageBytes);
                outputStream.flush(); // 출력 버퍼에 남아 있는 데이터를 강제로 전송합니다.
                log.info("TcpInNode[{}] 메시지 전송: {}", getId(), messageBytes);
            }
        } catch (IOException e) {
            // 전송 중에 오류가 발생하면 에러 상태로 전환하고 로그에 기록합니다.
            handleError(e);
            reconnect();
        }
    }

    // getter setter 설정
    /**
     * 호스트 주소를 반환하는 getter 메소드
     * 
     * @return TCP 서버의 호스트 주소
     */
    public String getHost() {
        return host;
    }

    /**
     * 호스트 주소를 설정하는 setter 메소드
     * 
     * @param host TCP 서버의 호스트 주소
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 포트 번호를 반환하는 getter 메소드
     * 
     * @return TCP 서버의 포트 번호
     */
    public int getPort() {
        return port;
    }

    /**
     * 포트 번호를 설정하는 setter 메소드
     * 
     * @param port TCP 서버의 포트 번호
     */
    public void setPort(int port) {
        this.port = port;
    }
}
