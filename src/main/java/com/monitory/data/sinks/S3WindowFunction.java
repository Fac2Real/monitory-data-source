package com.monitory.data.sinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class S3WindowFunction implements WindowFunction<String, String, String, TimeWindow> {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Seoul"));
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void apply(String key, TimeWindow window, Iterable<String> input, Collector<String> out) throws Exception {
        System.out.println("S3WindowFunction - IN: Key=" + key + ", WindowStart=" + window.getStart());
        // 1. JSON 배열 생성
        ArrayNode jsonArray = mapper.createArrayNode();
        int count = 0;
        for (String record : input) {
            jsonArray.add(mapper.readTree(record));
            count++;
        }
        System.out.println("S3WindowFunction - PROCESSED: Input count=" + count + " for Key=" + key);

        if (count > 0) { // 실제로 처리된 레코드가 있을 때만 출력

            // 2. S3 경로 생성 (date=2025-05-23/zone_id=Z001/equip_id=E001)
            String[] keys = key.split("\\|");
            String zoneId = keys[0];
            String equipId = keys[1];
            String date = dateFormatter.format(Instant.ofEpochMilli(window.getStart()));

            // 3. S3 경로와 JSON 데이터를 "|"로 구분하여 출력
            String s3Path = String.format("date=%s/zone_id=%s/equip_id=%s", date, zoneId, equipId);
            String stringToCollect = s3Path + "|" + jsonArray;
            System.out.println("S3WindowFunction - COLLECTING: " + stringToCollect.substring(0, Math.min(stringToCollect.length(), 200))); // 로그 확인!!!
            out.collect(stringToCollect);

            System.out.println("S3WindowFunction - OUT: Collecting data for Key=" + key + ", DataLength=" + jsonArray.toString().length());
        } else {
            System.out.println("S3WindowFunction - WARN: No records to collect for Key=" + key); // 아무것도 출력하지 않는 경우 확인
        }
    }
}
