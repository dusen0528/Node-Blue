package com.samsa.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 입력을 받아 처리하는 노드의 추상 클래스입니다.
 * 하나 이상의 입력 파이프를 가질 수 있으며, 입력된 메시지를 처리합니다.
 */
public abstract class InNode extends Node {
    /** 입력 파이프들의 리스트 */
    private final List<Pipe> inputPipes = new ArrayList<>();

    public InNode() {
        super();
    }
    
    public InNode(UUID id) {
        super(id);
    }
    
    public InNode(String uuid) {
        super(uuid);
    }

    /**
     * 입력 파이프를 추가합니다.
     *
     * @param pipe 추가할 파이프 객체
     */
    public void addPipe(Pipe pipe) {
        inputPipes.add(pipe);
    }

    /**
     * 입력 파이프를 제거합니다.
     *
     * @param pipe 제거할 파이프 객체
     */
    public void removePipe(Pipe pipe) {
        inputPipes.remove(pipe);
    }

    /**
     * 현재 연결된 모든 입력 파이프의 불변 리스트를 반환합니다.
     *
     * @return 입력 파이프들의 불변 리스트
     */
    protected List<Pipe> getPipes() {
        return Collections.unmodifiableList(inputPipes);
    }
}