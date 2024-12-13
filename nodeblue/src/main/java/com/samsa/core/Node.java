package com.samsa.core;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;


/**
 * 모든 노드의 기본 추상 클래스입니다.
 * 노드의 생명주기와 기본적인 에러 처리를 관리합니다.
 */
@Slf4j
public abstract class Node implements Runnable {

    /**
     * 노드의 고유 식별자
     */
    protected UUID id;

    /**
     * 노드의 현재 상태
     */
    protected NodeStatus status = NodeStatus.CREATED;

    /**
     * 기본 생성자로, 랜덤하게 생성된 ID를 사용하여 노드를 초기화합니다.
     */
    public Node() {
        this(UUID.randomUUID());
    }

    /**
     * 지정된 ID를 사용하여 노드를 초기화합니다.
     *
     * @param id 노드의 고유 식별자
     * @throws IllegalArgumentException ID가 null인 경우
     */
    public Node(UUID id) {
        if (id == null) {
            log.error("Node ID cannot be null");
            throw new IllegalArgumentException("Node ID cannot be null");
        }
        this.id = id;
    }

    /**
     * 메시지를 처리하는 추상 메서드입니다.
     * 각 노드 구현체에서 실제 메시지 처리 로직을 구현해야 합니다.
     *
     * @param message 처리할 메시지 객체
     * @throws IllegalArgumentException 메시지가 null인 경우
     */
    public abstract void onMessage(Message message);

    /**
     * 노드를 시작하고 상태를 RUNNING으로 변경합니다.
     * 상태 변경 중 에러가 발생하면 handleError 메서드가 호출됩니다.
     */
    public void start() {
        try {
            status = NodeStatus.RUNNING;
            log.info("Node[{}] started", id);
        } catch (Exception e) {
            log.error("Failed to start Node[{}]", id, e);
            handleError(e);
        }
    }

    /**
     * Runnable 인터페이스의 run 메서드를 구현하여 노드를 시작합니다.
     * 상태 변경 중 에러가 발생하면 handleError 메서드가 호출됩니다.
     */
    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            log.error("Unexpected error in Node[{}] during run", id, e);
            handleError(e);
        }
    }

    /**
     * 노드를 중지하고 상태를 STOPPED로 변경합니다.
     * 상태 변경 중 에러가 발생하면 handleError 메서드가 호출됩니다.
     */
    public void stop() {
        try {
            status = NodeStatus.STOPPED;
            log.info("Node[{}] stopped", id);
        } catch (Exception e) {
            log.error("Failed to stop Node[{}]", id, e);
            handleError(e);
        }
    }

    /**
     * 노드에서 발생한 에러를 처리합니다.
     * 에러 발생 시 노드의 상태를 ERROR로 변경합니다.
     *
     * @param error 발생한 에러 객체
     * @throws IllegalArgumentException 에러 객체가 null인 경우
     */
    public void handleError(Throwable error) {
        if (error == null) {
            log.error("Error cannot be null in Node[{}]", id);
            throw new IllegalArgumentException("Error cannot be null");
        }
        status = NodeStatus.ERROR;
        log.error("Error in Node[{}]: ", id, error);
    }

    /**
     * 노드의 고유 ID를 반환합니다.
     *
     * @return 노드의 고유 ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 노드의 ID를 문자열로 설정합니다.
     *
     * @param id 설정할 ID 문자열
     * @throws IllegalArgumentException 잘못된 형식의 문자열인 경우
     */
    public void setId(String id) {
        try {
            this.id = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID string: {}", id, e);
            throw new IllegalArgumentException("Invalid UUID string", e);
        }
    }
}

/**
 * 노드의 상태를 나타내는 열거형입니다.
 */
enum NodeStatus {

    /**
     * 노드가 생성된 초기 상태
     */
    CREATED,

    /**
     * 노드가 실행 중인 상태
     */
    RUNNING,

    /**
     * 노드가 중지된 상태
     */
    STOPPED,

    /**
     * 노드에 에러가 발생한 상태
     */
    ERROR
}
