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
                            .setTopicSelector((element) -> {
                                try {
                                    JsonNode json = mapper.readTree(element);

                                    String zoneId = json.path("zoneId").asText(null);
                                    String equipId = json.path("equipId").asText(null);

                                    if (zoneId != null && equipId != null) {
                                        return "EQUIPMENT";
                                    } else if (zoneId != null && equipId == null) {
                                        return "ENVIRONMENT";
                                    } else {
                                        return "sensor.unknown_topic";
                                    }

                                } catch (Exception e) {
                                    return "sensor.error_topic";
                                }
                            })
                        .build()
            )
            .build();
    }
}
