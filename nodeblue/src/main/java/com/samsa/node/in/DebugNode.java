package com.samsa.node.in;

import com.samsa.core.InNode;
import com.samsa.core.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * 메시지의 내용을 로그로 출력하는 디버그용 노드입니다.
 * 로깅을 활성화/비활성화할 수 있으며, 메시지의 페이로드를 문자열로 변환하여 출력합니다.
 */
@Slf4j
public class DebugNode extends InNode {


    /**
     * 새로운 DebugNode를 생성합니다.
     *
     * @param id 노드의 고유 식별자
     */
    public DebugNode(String id) {
        super(id);
    }

    /**
     * 수신된 메시지를 처리합니다.
     * 로깅이 활성화된 경우, 메시지의 페이로드를 로그로 출력합니다.
     *
     * @param message 처리할 메시지
     */
    @Override
    public void onMessage(Message message) {
            String payload = message.getPayload().toString();
            log.info("Node[{}] - Payload: {}", getId(), payload);
    }

    /**
     * 노드에서 발생한 에러를 처리합니다.
     * 로깅이 활성화된 경우, 기본 에러 처리 외에 추가적인 에러 상세 정보를 로그로 출력합니다.
     *
     * @param error 발생한 에러
     */
    @Override
    public void handleError(Throwable error) {
        super.handleError(error);
        log.error("Node[{}] - Debug error details: {}", getId(), error.getMessage());
    }
}