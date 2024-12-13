package com.samsa.core;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 입력을 받아 처리하는 노드의 추상 클래스입니다.
 * 하나 이상의 입력 파이프를 가질 수 있으며, 입력된 메시지를 처리합니다.
 * 
 * @author samsa
 * @since 1.0
 */
@Slf4j
public abstract class InNode extends Node {
    private final InPort port;

    /**
     * 지정된 입력 포트로 InNode를 생성합니다.
     *
     * @param port 입력 포트
     * @throws IllegalArgumentException 포트가 null인 경우
     */
    public InNode(InPort port) {
        super();
        if (port == null) {
            log.error("입력 포트가 null입니다");
            throw new IllegalArgumentException("입력 포트는 null일 수 없습니다");
        }
        this.port = port;
    }

    /**
     * 지정된 ID와 입력 포트로 InNode를 생성합니다.
     *
     * @param id   노드의 고유 식별자
     * @param port 입력 포트
     * @throws IllegalArgumentException ID 또는 포트가 null인 경우
     */
    public InNode(UUID id, InPort port) {
        super(id);
        if (port == null) {
            log.error("입력 포트가 null입니다. NodeId: {}", id);
            throw new IllegalArgumentException("입력 포트는 null일 수 없습니다");
        }
        this.port = port;
    }

    /**
     * 입력 포트로부터 메시지를 수신합니다.
     * 포트가 초기화되지 않았거나 메시지 수신 중 오류가 발생하면 예외가 발생합니다.
     *
     * @return 수신된 메시지, 수신할 메시지가 없으면 null
     * @throws IllegalStateException 입력 포트가 초기화되지 않은 경우
     */
    public Message receive() {
        if (port == null) {
            log.error("입력 포트가 초기화되지 않았습니다. NodeId: {}", getId());
            throw new IllegalStateException("입력 포트가 초기화되지 않았습니다");
        }

        try {
            log.debug("메시지 수신 시도. NodeId: {}", getId());
            Message message = port.consume();
            if (message != null) {
                log.debug("메시지 수신 완료. NodeId: {}, MessageId: {}", getId(), message.getId());
            }
            return message;
        } catch (Exception e) {
            log.error("메시지 수신 중 오류 발생. NodeId: {}", getId(), e);
            throw new RuntimeException("메시지 수신 중 오류가 발생했습니다", e);
        }
    }
}