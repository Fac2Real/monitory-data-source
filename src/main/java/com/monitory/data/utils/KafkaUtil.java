package com.monitory.data.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitory.data.config.KafkaConfig;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;

public class KafkaUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static KafkaSink<String> createKafkaSink() {
        return KafkaSink.<String>builder()
            .setBootstrapServers(KafkaConfig.get("KAFKA_SERVER"))
            .setRecordSerializer(
                    KafkaRecordSerializationSchema.<String>builder()
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .setTopicSelector((element) -> { // <--- 바로 이 부분!
                            try {
                                // 1. 입력 문자열(element)을 JSON으로 파싱
                                JsonNode json = mapper.readTree(element);

                                // 2. 필요한 필드 값 추출
                                String zoneId = json.path("zoneId").asText("unknown_zone");
                                String equipId = json.path("equipId").asText("unknown_equip");
                                String sensorId = json.path("sensorId").asText("unknown_sensor");
                                String sensorType = json.path("sensorType").asText("unknown_type");

                                // 3. 추출된 값으로 원하는 형식의 토픽 이름 생성 및 반환
                                return String.format("sensor.%s.%s.%s.%s", zoneId, equipId, sensorId, sensorType);

                            } catch (Exception e) {
                                // 4. 오류 발생 시 대체 토픽 이름 반환
                                return "sensor/error_topic";
                            }
                        })
                        .build()
            )
            .build();
    }
}
