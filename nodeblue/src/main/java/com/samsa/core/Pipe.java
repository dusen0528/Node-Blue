package com.samsa.core;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Pipe {
    private final UUID id;
    private final BlockingQueue<Message> queue;
    private static final int DEFAULT_CAPACITY = 1024;

    /**
     * 기본 용량의 파이프를 생성합니다.
     */
    public Pipe() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * 지정된 용량의 파이프를 생성합니다.
     */
    public Pipe(int capacity) {
        this.id = UUID.randomUUID();
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * 메시지를 파이프에 넣습니다. 큐가 가득 찬 경우 false를 반환합니다.
     */
    public boolean offer(Message message) {
        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        return queue.offer(message);
    }

    /**
     * 파이프에서 메시지를 가져옵니다. 큐가 비어있는 경우 null을 반환합니다.
     */
    public Message poll() {
        return queue.poll();
    }

    /**
     * 파이프가 비어있는지 확인합니다.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 파이프가 가득 찼는지 확인합니다.
     */
    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }

    /**
     * 현재 파이프에 있는 메시지 수를 반환합니다.
     */
    public int size() {
        return queue.size();
    }

    /**
     * 파이프의 고유 식별자를 반환합니다.
     */
    public UUID getId() {
        return id;
    }

    /**
     * 파이프를 비웁니다.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * 현재 파이프의 상태 정보를 문자열로 반환합니다.
     */
    @Override
    public String toString() {
        return String.format("Pipe[id=%s, size=%d, capacity=%d]",
                id, queue.size(), queue.size() + queue.remainingCapacity());
    }
}