package com.samsa.core;

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
       this.metadata = new HashMap<>(metadata);  // 메타데이터 복사
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
}