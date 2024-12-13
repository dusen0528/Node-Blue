package com.samsa.core;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 출력을 생성하는 노드의 추상 클래스입니다.
 * 하나 이상의 출력 파이프를 가질 수 있으며, 생성된 메시지를 다음 노드로 전달합니다.
 * 
 * @author samsa
 * @since 1.0
 */
@Slf4j
public abstract class OutNode extends Node {
    private final OutPort port;

    public OutNode(OutPort port) {
        super();
        this.port = port;
    }

    public OutNode(UUID id, OutPort port) {
        super(id);
        this.port = port;
    }

    /**
     * 메시지를 연결된 모든 출력 파이프로 전송합니다.
     * 출력 포트가 null이거나 메시지가 null인 경우 예외가 발생합니다.
     *
     * @param message 전송할 메시지 객체
     * @throws IllegalStateException    출력 포트가 초기화되지 않은 경우
     * @throws IllegalArgumentException 메시지가 null인 경우
     */
    public void emit(Message message) {
        if (port == null) {
            log.error("출력 포트가 초기화되지 않았습니다. NodeId: {}", getId());
            throw new IllegalStateException("출력 포트가 초기화되지 않았습니다");
        }
        if (message == null) {
            log.error("전송할 메시지가 null입니다. NodeId: {}", getId());
            throw new IllegalArgumentException("메시지는 null일 수 없습니다");
        }

        try {
            log.debug("메시지 전송 시작. NodeId: {}, MessageId: {}", getId(), message.getId());
            port.propagate(message);
            log.debug("메시지 전송 완료. NodeId: {}, MessageId: {}", getId(), message.getId());
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생. NodeId: {}, MessageId: {}", getId(), message.getId(), e);
            throw new RuntimeException("메시지 전송 중 오류가 발생했습니다", e);
        }
    }

    /**
     * OutNode는 메시지를 받을 수 없으므로 이 메서드를 호출하면 예외가 발생합니다.
     * 출력 전용 노드이므로 메시지 수신 기능은 지원하지 않습니다.
     *
     * @param message 수신된 메시지 객체
     * @throws UnsupportedOperationException 이 메서드가 호출될 경우 항상 발생
     */
    @Override
    public final void onMessage(Message message) {
        log.warn("출력 노드에서 메시지 수신 시도됨. NodeId: {}", getId());
        throw new UnsupportedOperationException("출력 노드는 메시지를 수신할 수 없습니다");
    }
}
