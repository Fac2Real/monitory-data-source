package com.monitory.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitory.data.sinks.BucketJson;
import com.monitory.data.sinks.S3WindowFunction;
import com.monitory.data.transformations.EnvironmentDangerLevelAssigner;
import com.monitory.data.transformations.FaultyAssigner;
import com.monitory.data.transformations.WearableDangerLevelAssigner;
import com.monitory.data.utils.KinesisSourceUtil;
import com.monitory.data.utils.S3SinkUtil;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kinesis.source.KinesisStreamsSource;
import com.monitory.data.transformations.TimeStampAssigner;
import com.monitory.data.utils.KafkaUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;

import java.time.Duration;

public class FlinkSourceApplication {
    public static void main (String [] args) throws Exception {
        // 1. Flink 환경 설정
        Configuration conf = new Configuration();

        conf.setString("metrics.reporters", "prom");
        conf.setString("metrics.reporter.prom.factory.class", "org.apache.flink.metrics.prometheus.PrometheusReporterFactory");
        conf.setString("metrics.reporter.prom.port", "9249");

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);
        final ObjectMapper staticMapper = new ObjectMapper();

        // 1-1. 체크포인트 설정
        env.enableCheckpointing(60000); // 60초(1분)마다 체크포인트 생성
        env.getCheckpointConfig().setCheckpointTimeout(30_000); // 체크포인트 타임아웃 30초
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500); // 체크포인트 간 최소 간격 500ms

        env.setParallelism(16);

        // 2. 데이터 소스 설정
        KinesisStreamsSource<String> kinesisStreamSource = KinesisSourceUtil.createKinesisSource();

        DataStream<String> sourceStream = env.fromSource(
                kinesisStreamSource,
//                WatermarkStrategy.noWatermarks(),
                WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5)) // json의 timestamp 기준 5초 지연 허용, 이후 데이터 삭제
                        .withTimestampAssigner((SerializableTimestampAssigner<String>) (element, recordTimestamp) -> {
                            try {
                                JsonNode node = staticMapper.readTree(element);
                                return node.get("utc_ingestion_time").asLong();
                            } catch (Exception e) {
                                return recordTimestamp;
                            }
                        }).withIdleness(Duration.ofMinutes(1)),
                "Kinesis-Streams-Source"
        ).returns(TypeInformation.of(String.class));
//        DataStream<String> sourceStream = env.fromSource(new MqttSource(), WatermarkStrategy.noWatermarks(), "MQTT-Source");

        // 3-1. 데이터 처리: Time Stamp 출력
        DataStream<String> timeTransformedStream = sourceStream
                .map(new TimeStampAssigner());

        // 3-2. 데이터 처리: 이상치 검색
        DataStream<String> labelTransformedStream = timeTransformedStream
                .map(new EnvironmentDangerLevelAssigner());
        labelTransformedStream = labelTransformedStream
                .map(new FaultyAssigner());
        labelTransformedStream = labelTransformedStream
                .map(new WearableDangerLevelAssigner());


        // 4-1. 데이터 싱크: 콘솔에 출력 & kafka publish
        labelTransformedStream.sinkTo(KafkaUtil.createKafkaSink());
        labelTransformedStream.print();

        // 4-2. S3로 보낼 데이터만 filter
        ObjectMapper mapper = new ObjectMapper();
        DataStream<String> equipmentStream = labelTransformedStream
                .filter(json -> {
                    try {
                        JsonNode node = mapper.readTree(json);
                        return node.has("category") && "EQUIPMENT".equalsIgnoreCase(node.get("category").asText());
                    } catch (Exception e) {
                        return false;
                    }
                });

        // 4-3. zoneId, equipId별로 1시간 단위 집계 by window 함수 (데이터 집계), keyBy (커스텀 파티셔닝)
        DataStream<String> aggregatedStream = equipmentStream
                .keyBy(json -> {
                    JsonNode node = mapper.readTree(json);
                    String zoneId = node.get("zoneId").asText("unknown");
                    String equipId = node.get("equipId").asText("unknown");
                    return zoneId + "|" + equipId;
                })
                .window(TumblingProcessingTimeWindows.of(Duration.ofHours(1)))
                .apply(new S3WindowFunction());

        // 4-4. S3 Sink 설정 (S3SinkUtil로 분리)
        FileSink<BucketJson> s3Sink = S3SinkUtil.createS3Sink("monitory-bucket");

        // 4-5. S3에 저장 (경로 제외하고 데이터만 저장)
        DataStream<BucketJson> bucketJsonStream = aggregatedStream
                .map(element -> {
                    String[] parts = element.split("\\|", 2);
                    return new BucketJson(parts[0], parts[1]);
                });

        // 4-6.
        bucketJsonStream.sinkTo(s3Sink);

        // 5. 실행
        env.execute("Flink to Kafka Produce");
    }
}
