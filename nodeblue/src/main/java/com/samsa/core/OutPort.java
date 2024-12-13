package com.samsa.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 노드의 출력 포트를 나타내며, 여러 파이프와 연결되어 메시지를 전파합니다.
 */
@Slf4j
public class OutPort {
    /** 출력 포트의 고유 식별자 */
    private final UUID id;
    /** 이 출력 포트의 소유 노드 */
    private final Node owner;
    /** 연결된 파이프들의 목록 */
    private final List<Pipe> pipes;

    /**
     * 랜덤하게 생성된 ID와 지정된 소유 노드를 사용하여 OutPort를 생성합니다.
     *
     * @param node 이 OutPort의 소유 노드
     * @throws IllegalArgumentException 노드가 null인 경우
     */
    public OutPort(Node node) {
        this(UUID.randomUUID(), node);
    }

    /**
     * 지정된 ID와 소유 노드를 사용하여 OutPort를 생성합니다.
     *
     * @param id   이 OutPort의 고유 ID
     * @param node 이 OutPort의 소유 노드
     * @throws IllegalArgumentException 노드가 null인 경우
     */
    public OutPort(UUID id, Node node) {
        if (Objects.isNull(node)) {
            log.error("소유 노드가 null입니다");
            throw new IllegalArgumentException("Node cannot be null");
        }
        this.id = id;
        this.owner = node;
        this.pipes = new ArrayList<>();
    }

    /**
     * 메시지를 연결된 모든 파이프로 전파합니다.
     *
     * @param message 전파할 메시지
     * @throws IllegalArgumentException 메시지가 null인 경우
     */
    public void propagate(Message message) {
        if (Objects.isNull(message)) {
            log.error("전파할 메시지가 null입니다. OutPortId: {}", id);
            throw new IllegalArgumentException("Message cannot be null");
        }

        log.debug("메시지 전파 시작. OutPortId: {}, MessageId: {}", id, message.getId());
        pipes.forEach(pipe -> {
            try {
                pipe.offer(message);
                log.debug("파이프로 메시지 전송 완료. PipeId: {}", pipe.getId());
            } catch (Exception e) {
                log.error("파이프로 메시지 전송 실패. PipeId: {}", pipe.getId(), e);
            }
        });
    }

    /**
     * 연결된 파이프들이 데이터를 수용할 수 있는지 확인합니다.
     *
     * @return 하나 이상의 파이프가 데이터를 수용할 수 있으면 true
     */
    public boolean canAcceptData() {
        return pipes.stream().anyMatch(pipe -> !pipe.isFull());
    }

    /**
     * 이 OutPort에 파이프를 추가합니다.
     *
     * @param pipe 추가할 파이프
     * @throws IllegalArgumentException 파이프가 null인 경우
     */
    public void addPipe(Pipe pipe) {
        if (Objects.isNull(pipe)) {
            log.error("추가할 파이프가 null입니다. OutPortId: {}", id);
            throw new IllegalArgumentException("Pipe cannot be null");
        }
        pipes.add(pipe);
        log.debug("파이프 추가됨. OutPortId: {}, PipeId: {}", id, pipe.getId());
    }

    /**
     * 이 OutPort에서 파이프를 제거합니다.
     *
     * @param pipe 제거할 파이프
     */
    public void removePipe(Pipe pipe) {
        if (Objects.nonNull(pipe)) {
            pipes.remove(pipe);
            log.debug("파이프 제거됨. OutPortId: {}, PipeId: {}", id, pipe.getId());
        }
    }

    /**
     * 이 OutPort의 고유 ID를 반환합니다.
     *
     * @return 이 OutPort의 ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 이 OutPort의 소유 노드를 반환합니다.
     *
     * @return 소유 노드
     */
    public Node getOwner() {
        return owner;
    }

    /**
     * 연결된 파이프들의 목록을 반환합니다.
     *
     * @return 파이프들의 목록의 복사본
     */
    public List<Pipe> getPipes() {
        return new ArrayList<>(pipes);
    }
}