package com.samsa.core;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 입력을 받아서 처리하고 출력을 생성하는 노드의 추상 클래스입니다.
 * 입력과 출력 파이프를 모두 가질 수 있으며, 메시지를 변환하거나 처리할 수 있습니다.
 * 
 * @author samsa
 * @since 1.0
 */
@Slf4j
public abstract class InOutNode extends Node {
    private final InPort inPort;
    private final OutPort outPort;

    /**
     * 지정된 입출력 포트로 InOutNode를 생성합니다.
     *
     * @param inPort  입력 포트
     * @param outPort 출력 포트
     * @throws IllegalArgumentException 입력 또는 출력 포트가 null인 경우
     */
    public InOutNode(InPort inPort, OutPort outPort) {
        super();
        if (inPort == null || outPort == null) {
            log.error("입력 또는 출력 포트가 null입니다");
            throw new IllegalArgumentException("포트는 null일 수 없습니다");
        }
        this.inPort = inPort;
        this.outPort = outPort;
    }

    /**
     * 지정된 ID와 입출력 포트로 InOutNode를 생성합니다.
     *
     * @param id      노드의 고유 식별자
     * @param inPort  입력 포트
     * @param outPort 출력 포트
     * @throws IllegalArgumentException ID, 입력 또는 출력 포트가 null인 경우
     */
    public InOutNode(UUID id, InPort inPort, OutPort outPort) {
        super(id);
        if (inPort == null || outPort == null) {
            log.error("입력 또는 출력 포트가 null입니다. NodeId: {}", id);
            throw new IllegalArgumentException("포트는 null일 수 없습니다");
        }
        this.inPort = inPort;
        this.outPort = outPort;
    }

    /**
     * 메시지를 모든 출력 파이프로 전송합니다.
     *
     * @param message 전송할 메시지 객체
     * @throws IllegalArgumentException 메시지가 null인 경우
     */
    public void emit(Message message) {
        if (message == null) {
            log.error("전송할 메시지가 null입니다. NodeId: {}", getId());
            throw new IllegalArgumentException("메시지는 null일 수 없습니다");
        }
        try {
            log.debug("메시지 전송 시작. NodeId: {}, MessageId: {}", getId(), message.getId());
            outPort.propagate(message);
            log.debug("메시지 전송 완료. NodeId: {}, MessageId: {}", getId(), message.getId());
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생. NodeId: {}", getId(), e);
            throw new RuntimeException("메시지 전송 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 입력 포트로부터 메시지를 수신합니다.
     *
     * @return 수신된 메시지, 수신할 메시지가 없으면 null
     */
    public Message receive() {
        try {
            log.debug("메시지 수신 시도. NodeId: {}", getId());
            Message message = inPort.consume();
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
