package com.samsa.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * 출력을 생성하는 노드의 추상 클래스입니다.
 * 하나 이상의 출력 파이프를 가질 수 있으며, 생성된 메시지를 다음 노드로 전달합니다.
 */
@Slf4j
public abstract class OutNode extends Node {
    /** 출력 파이프들의 리스트 */
    private final List<Pipe> outputPipes = new ArrayList<>();

    public OutNode() {
        super();
    }
    

    public OutNode(UUID id) {
        super(id);
    }
    
    public OutNode(String uuid) {
        super(uuid);
    }

    /**
     * 메시지를 연결된 모든 출력 파이프로 전송합니다.
     *
     * @param message 전송할 메시지 객체
     */
    public void emit(Message message) {
        log.info("emit 실행");
        for (Pipe pipe : getPipes()) {
            if (pipe.isConnected()) {
                pipe.send(message);
                log.info("메세지를 보냄");
            }
        }
    }

    /**
     * OutNode는 메시지를 받을 수 없으므로 이 메서드를 호출하면 예외가 발생합니다.
     *
     * @throws UnsupportedOperationException 항상 발생
     */
    @Override
    public final void onMessage(Message message) {
        throw new UnsupportedOperationException("Output node cannot receive messages");
    }


    public void addPipe(Pipe pipe) {
        outputPipes.add(pipe);
    }

    public void removePipe(Pipe pipe) {
        outputPipes.remove(pipe);
    }

    protected List<Pipe> getPipes() {
        return Collections.unmodifiableList(outputPipes);
    }
}
