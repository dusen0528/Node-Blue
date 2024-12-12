package com.samsa.core;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 노드의 기본 추상 클래스입니다.
 * 노드의 생명주기와 기본적인 에러 처리를 관리합니다.
 */
@Slf4j
public abstract class Node {
    /** 노드의 고유 식별자 */
    protected UUID id;
    
    /** 노드의 현재 상태 */
    protected NodeStatus status = NodeStatus.CREATED;
    

    public Node() {
        this.id = UUID.randomUUID();
    }
    

    public Node(UUID id) {
        this.id = id;
    }
    
    public Node(String uuid) {
        try{
            this.id = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
        
        throw e;
    }
    }

    /**
     * 메시지를 처리하는 추상 메서드입니다.
     * 각 노드 구현체에서 실제 메시지 처리 로직을 구현해야 합니다.
     *
     * @param message 처리할 메시지 객체
     */
    public abstract void onMessage(Message message);
    
    /**
     * 노드를 시작하고 상태를 RUNNING으로 변경합니다.
     */
    public void start() {
        status = NodeStatus.RUNNING;
        log.info("Node[{}] started", id);
    }

    /**
     * 노드를 중지하고 상태를 STOPPED로 변경합니다.
     */
    public void stop() {
        status = NodeStatus.STOPPED;
        log.info("Node[{}] stopped", id);
    }

    /**
     * 노드에서 발생한 에러를 처리합니다.
     * 에러 발생 시 노드의 상태를 ERROR로 변경합니다.
     *
     * @param error 발생한 에러 객체
     */
    public void handleError(Throwable error) {
        status = NodeStatus.ERROR;
        log.error("Error in Node[{}]: ", id, error);
    }

    
    public UUID getId() {
        return id;
    }

    public void setId(String id) {
        this.id = UUID.fromString(id);
    }   
}

/**
 * 노드의 상태를 나타내는 열거형입니다.
 */
enum NodeStatus {
    /** 노드가 생성된 초기 상태 */
    CREATED,
    /** 노드가 실행 중인 상태 */
    RUNNING,
    /** 노드가 중지된 상태 */
    STOPPED,
    /** 노드에 에러가 발생한 상태 */
    ERROR
}
