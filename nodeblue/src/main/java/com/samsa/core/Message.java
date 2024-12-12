package com.samsa.core;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 노드 간에 전달되는 메시지를 나타내는 클래스입니다.
 * 메시지는 고유 ID, 페이로드, 그리고 메타데이터를 포함합니다.
 */
public class Message {
    /** 메시지의 고유 식별자 */
    private final String id;
    /** 메시지의 실제 내용 */
    private final Object payload;
    /** 메시지의 부가 정보 */
    private final Map<String, Object> metadata;

    /**
     * 기본 메시지를 생성합니다.
     *
     * @param payload 메시지 내용
     */
    public Message(Object payload) {
        this.id = UUID.randomUUID().toString();
        this.payload = payload;
        this.metadata = new HashMap<>();
    }

    // 메타데이터 포함 생성자
    public Message(Object payload, Map<String, Object> metadata) {
        this.id = UUID.randomUUID().toString();
        this.payload = payload;
        this.metadata = new HashMap<>(metadata); // 메타데이터 복사
    }

    // 전체 지정 생성자
    public Message(String id, Object payload, Map<String, Object> metadata) {
        this.id = id;
        this.payload = payload;
        this.metadata = new HashMap<>(metadata);
    }

    public String getId() {
        return id;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 
     * payload를 binary 형식으로 변환하는 메소드
     */
    public byte[] getPayloadAsBinary() {
        if (payload == null) {
            throw new UnsupportedOperationException("payload가 null입니다. 바이너리로 변환할 수 없습니다.");
        }

        if (payload instanceof byte[]) {
            // 이미 바이너리 데이터인 경우 그대로 반환
            return (byte[]) payload;
        } else if (payload instanceof String) {
            // 문자열인 경우 UTF-8 인코딩으로 변환
            return ((String) payload).getBytes(StandardCharsets.UTF_8);
        } else {
            // 그 외의 객체는 toString() 후 변환
            return payload.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}