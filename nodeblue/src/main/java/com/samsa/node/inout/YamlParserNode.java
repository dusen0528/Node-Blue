package com.samsa.node.inout;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.samsa.core.InOutNode;
import com.samsa.core.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * YAML 파서 노드 클래스. 메시지의 페이로드를 YAML 문자열과 객체 간 변환합니다.
 */
@Slf4j
public class YamlParserNode extends InOutNode {

    private final Class<?> targetClass;
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    /**
     * YamlParserNode 객체를 생성합니다.
     *
     * @param id 노드의 고유 식별자
     * @param targetClass YAML 문자열을 변환할 대상 클래스
     */
    public YamlParserNode(UUID id, Class<?> targetClass) {
        super(id);
        this.targetClass = targetClass;
    }

    /**
     * 메시지를 수신하고 페이로드를 변환하여 방출합니다.
     *
     * @param message 수신된 메시지
     */
    @Override
    public void onMessage(Message message) {
        try {
            Object result;

            if (message.getPayload() instanceof String yamlString) {
                /**
                 * YAML 문자열을 객체로 변환
                 */
                result = parseYamlToObject(yamlString);
            } else {
                /**
                 * 객체를 YAML 문자열로 변환
                 */
                result = convertObjectToYaml(message.getPayload());
            }

            /**
             * 성공적으로 변환된 메시지 방출
             */
            emit(new Message(result));
        } catch (JsonProcessingException e) {
            log.error("YAML 처리 중 오류 발생: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("메시지 처리 중 예기치 못한 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * YAML 문자열을 지정된 객체로 변환합니다.
     *
     * @param yamlString YAML 문자열
     * @return 변환된 객체
     * @throws JsonProcessingException YAML 파싱 중 오류가 발생한 경우
     */
    private Object parseYamlToObject(String yamlString) throws JsonProcessingException {
        log.debug("YAML 문자열을 객체로 변환 중: {}", yamlString);
        return YAML_MAPPER.readValue(yamlString, targetClass);
    }

    /**
     * 객체를 YAML 문자열로 변환합니다.
     *
     * @param payload 변환할 객체
     * @return YAML 문자열
     * @throws JsonProcessingException YAML 직렬화 중 오류가 발생한 경우
     */
    private String convertObjectToYaml(Object payload) throws JsonProcessingException {
        log.debug("객체를 YAML 문자열로 변환 중: {}", payload);
        return YAML_MAPPER.writeValueAsString(payload);
    }
}
