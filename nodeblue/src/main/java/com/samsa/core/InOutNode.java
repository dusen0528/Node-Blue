package com.samsa.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 입력을 받아서 처리하고 출력을 생성하는 노드의 추상 클래스입니다.
 * 입력과 출력 파이프를 모두 가질 수 있으며, 메시지를 변환하거나 처리할 수 있습니다.
 */
public abstract class InOutNode extends Node {
    /** 입력 파이프들의 리스트 */
    private final List<Pipe> inputPipes = new ArrayList<>();
    /** 출력 파이프들의 리스트 */
    private final List<Pipe> outputPipes = new ArrayList<>();

    /**
     * InOutNode를 생성합니다.
     *
     * @param id 노드의 고유 식별자
     */
    protected InOutNode(String id) {
        this.id = id;
    }

    /**
     * 메시지를 모든 출력 파이프로 전송합니다.
     *
     * @param message 전송할 메시지 객체
     */
    public void emit(Message message) {
        for (Pipe pipe : getOutputPipes()) {
            if (pipe.isConnected()) {
                pipe.send(message);
            }
        }
    }

    public void addInputPipe(Pipe pipe) {
        inputPipes.add(pipe);
    }

    public void addOutputPipe(Pipe pipe) {
        outputPipes.add(pipe);
    }

    public void removeInputPipe(Pipe pipe) {
        inputPipes.remove(pipe);
    }

    public void removeOutputPipe(Pipe pipe) {
        outputPipes.remove(pipe);
    }

    protected List<Pipe> getInputPipes() {
        return Collections.unmodifiableList(inputPipes);
    }

    protected List<Pipe> getOutputPipes() {
        return Collections.unmodifiableList(outputPipes);
    }


}