package com.monitory.data.transformations;

import org.apache.flink.api.common.functions.MapFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeStampAssigner implements MapFunction<String, String> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Override
    public String map(String value) throws Exception {
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(value);

        if (jsonNode.has("utc_ingestion_time")) {
            long utcMillis = jsonNode.get("utc_ingestion_time").asLong();

            Instant utcInstant = Instant.ofEpochMilli(utcMillis);

            ZonedDateTime kstZonedDateTime = utcInstant.atZone(KST_ZONE_ID);
            String kstTimeString = kstZonedDateTime.format(KST_FORMATTER);

            // 기존 필드 제거 및 새로운 'time' 필드 추가
            jsonNode.remove("utc_ingestion_time");
            jsonNode.remove("timezone");
            jsonNode.put("time", kstTimeString);
        } else {
            // utc_ingestion_time 필드가 없는 경우, 현재 KST 시간을 기본값으로 사용하거나 오류 처리
            ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE_ID);
            jsonNode.put("time", nowKst.format(KST_FORMATTER));
        }
        return mapper.writeValueAsString(jsonNode);
    }
}
