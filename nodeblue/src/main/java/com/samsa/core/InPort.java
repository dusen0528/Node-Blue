package com.samsa.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 노드의 입력 포트를 나타내며, 여러 파이프와 연결되어 메시지를 소비합니다.
 */
public class InPort {

    /**
     * InPort의 고유 식별자입니다.
     */
    private UUID id;

    /**
     * 이 InPort의 소유 노드입니다.
     */
    private Node owner;

    /**
     * 연결된 파이프들의 목록입니다.
     */
    private List<Pipe> pipes;

    /**
     * 랜덤하게 생성된 ID와 지정된 소유 노드를 사용하여 InPort를 생성합니다.
     *
     * @param node 이 InPort의 소유 노드
     * @throws IllegalArgumentException 노드가 null인 경우
     */
    public InPort(Node node) {
        this(UUID.randomUUID(), node);
    }

    /**
     * 지정된 ID와 소유 노드를 사용하여 InPort를 생성합니다.
     *
     * @param id   이 InPort의 고유 ID
     * @param node 이 InPort의 소유 노드
     * @throws IllegalArgumentException 노드가 null인 경우
     */
    public InPort(UUID id, Node node) {
        if (Objects.isNull(node)) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        this.id = id;
        this.owner = node;
        this.pipes = new ArrayList<>();
    }

    /**
     * 연결된 파이프들로부터 데이터를 소비합니다.
     * 여러 파이프에 데이터가 있는 경우 라운드 로빈 방식으로 데이터를 읽습니다.
     *
     * @return 소비된 메시지, 또는 데이터가 없을 경우 {@code null}
     */
    public Message consume() {
        for (Pipe pipe : pipes) {
            if (Objects.isNull(pipe)) {
                continue;
            }
            Message message = pipe.poll();
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    /**
     * 연결된 파이프들 중 데이터가 있는지 확인합니다.
     *
     * @return 데이터가 있으면 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean hasAvailableData() {
        return pipes.stream().anyMatch(pipe -> pipe != null && !pipe.isEmpty());
    }

    /**
     * 이 InPort에 파이프를 추가합니다.
     *
     * @param pipe 추가할 파이프
     * @throws IllegalArgumentException 파이프가 null인 경우
     */
    public void addPipe(Pipe pipe) {
        if (Objects.isNull(pipe)) {
            throw new IllegalArgumentException("Pipe cannot be null");
        }
        pipes.add(pipe);
    }

    /**
     * 이 InPort에서 파이프를 제거합니다.
     *
     * @param pipe 제거할 파이프
     * @throws IllegalArgumentException 파이프가 null인 경우
     */
    public void removePipe(Pipe pipe) {
        if (Objects.isNull(pipe)) {
            throw new IllegalArgumentException("Pipe cannot be null");
        }
        pipes.remove(pipe);
    }

    /**
     * 이 InPort의 고유 ID를 반환합니다.
     *
     * @return 이 InPort의 ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 이 InPort의 소유 노드를 반환합니다.
     *
     * @return 소유 노드
     */
    public Node getOwner() {
        return owner;
    }

    /**
     * 연결된 파이프들의 목록을 반환합니다.
     *
     * @return 파이프들의 목록
     */
    public List<Pipe> getPipes() {
        return new ArrayList<>(pipes);
    }
}
