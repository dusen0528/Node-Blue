package com.samsa.node.in;

import static com.samsa.core.Node.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.samsa.core.InNode;
import com.samsa.core.Message;

/**
 * TcpOutNode 클래스
 * 
 * 이 클래스는 InNode를 확장하여 TCP 연결을 통해 데이터를 수신하는 노드의 동작을 정의합니다.
 * 일반적으로 Node-RED의 TcpOut은 출력 노드로 간주되지만,
 * 이 구현에서는 Input 노드로 취급됩니다.
 */
public class TcpOutNode extends InNode {
    private int port; // 수신 대기할 포트 번호
    private ServerSocket serverSocket; // 서버 소켓 객체
    private Socket clientSocket; // 클라이언트와의 연결을 나타내는 소켓
    private BufferedReader inputReader; // 입력 스트림을 읽기 위한 리더 객체

    /**
     * 생성자
     * 
     * @param id   노드의 고유 식별자 (Node ID)
     * @param port TCP 서버의 포트 번호 (수신 대기할 포트)
     * 
     *             InNode의 생성자를 호출하여 id를 설정하고, port를 초기화합니다.
     */
    public TcpOutNode(String id, int port) {
        super(id); // 부모 클래스 InNode의 생성자를 호출
        this.port = port; // TCP 수신 대기할 포트 번호를 초기화
    }

    /**
     * 노드 시작 메소드
     * 
     * InNode의 start() 메소드를 호출하여 노드를 시작한 후,
     * TCP 서버 소켓을 열고 클라이언트의 연결을 대기합니다.
     */
    @Override
    public void start() {
        super.start(); // 부모 클래스의 start 메소드 호출
        try {
            // 지정된 포트로 서버 소켓을 생성합니다.
            serverSocket = new ServerSocket(port);
            log.info("TcpOutNode[{}]가 포트 {}에서 연결을 기다리고 있습니다.", getId(), port);

            // 클라이언트의 연결을 기다리고 연결되면 클라이언트 소켓을 생성합니다.
            clientSocket = serverSocket.accept();
            inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            log.info("TcpOutNode[{}] 클라이언트 연결 수락: {}", getId(), clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            // 연결 오류 발생 시 에러 상태로 전환하고 로그에 기록합니다.
            handleError(e);
        }
    }

    /**
     * 노드 중지 메소드
     * 
     * InNode의 stop() 메소드를 호출한 후,
     * 소켓과 입력 스트림을 닫아 연결을 해제합니다.
     */
    @Override
    public void stop() {
        super.stop(); // 부모 클래스의 stop 메소드 호출
        try {
            if (inputReader != null) {
                inputReader.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }

            log.info("TcpOutNode[{}] 연결이 종료되었습니다.", getId());
        } catch (IOException e) {
            // 소켓이나 스트림을 닫는 중에 발생한 예외를 처리합니다.
            handleError(e);
        }
    }

    /**
     * TCP 메시지를 수신하는 메소드
     * 
     * 클라이언트로부터 데이터를 수신할 때마다 onMessage 메소드를 호출합니다.
     */
    @Override
    public void onMessage(Message message) {
        try {
            // 입력 스트림에서 메시지를 읽어옵니다.
            String receivedData;
            while ((receivedData = inputReader.readLine()) != null) {
                log.info("TcpOutNode[{}] 수신된 메시지: {}", getId(), receivedData);

                // 메시지를 생성하고 연결된 다음 노드로 전달합니다.
                Message newMessage = new Message(receivedData);

                // 연결된 파이프를 통해 메시지를 전송합니다.
                for (var pipe : getPipes()) {
                    pipe.send(newMessage);
                }
            }
        } catch (IOException e) {
            // 메시지 수신 중 예외가 발생하면 에러 상태로 전환하고 로그에 기록합니다.
            handleError(e);
        }
    }

    /**
     * 포트 번호를 반환하는 getter 메소드
     * 
     * @return 수신 대기할 포트 번호
     */
    public int getPort() {
        return port;
    }

    /**
     * 포트 번호를 설정하는 setter 메소드
     * 
     * @param port 수신 대기할 포트 번호
     */
    public void setPort(int port) {
        this.port = port;
    }
}// teste
