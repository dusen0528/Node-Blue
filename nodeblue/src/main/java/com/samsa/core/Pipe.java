package com.samsa.core;

/**
 * 노드 간의 연결을 담당하는 파이프 클래스입니다.
 * 메시지를 한 노드에서 다른 노드로 전달하는 역할을 합니다.
 */
public class Pipe {
    /** 파이프의 고유 식별자 */
    private final String id;

    /** 이 파이프가 연결된 소스 노드 */
    private final Node node;

    /** 이 파이프와 연결된 대상 파이프 */
    private Pipe connectedPipe;

    /**
     * 새로운 파이프를 생성합니다.
     *
     * @param id   파이프의 고유 식별자
     * @param node 이 파이프가 연결될 소스 노드
     */
    public Pipe(String id, Node node) {
        this.id = id;
        this.node = node;
    }

    /**
     * 파이프의 고유 식별자를 반환합니다.
     *
     * @return 파이프 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 이 파이프가 연결된 소스 노드를 반환합니다.
     *
     * @return 소스 노드
     */
    public Node getNode() {
        return node;
    }

    /**
     * 이 파이프를 다른 파이프와 연결합니다.
     *
     * @param pipe 연결할 대상 파이프
     */
    public void connect(Pipe pipe) {
        this.connectedPipe = pipe;
    }

    /**
     * 현재 연결된 파이프와의 연결을 해제합니다.
     */
    public void disconnect() {
        this.connectedPipe = null;
    }

    /**
     * 파이프가 다른 파이프와 연결되어 있는지 확인합니다.
     *
     * @return 연결되어 있으면 true, 아니면 false
     */
    public boolean isConnected() {
        return connectedPipe != null;
    }

    /**
     * 메시지를 연결된 파이프를 통해 다음 노드로 전송합니다.
     * 파이프가 연결되어 있지 않으면 메시지는 전송되지 않습니다.
     *
     * @param message 전송할 메시지
     */

    public void send(Message message) {
        if (isConnected()) {
            connectedPipe.getNode().onMessage(message);
        }
    }
}