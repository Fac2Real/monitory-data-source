package com.monitory.data.utils;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kinesis.source.KinesisStreamsSource;
import org.apache.flink.connector.kinesis.source.config.KinesisSourceConfigOptions;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;

public class KinesisSourceUtil {

    private static final String KINESIS_STREAM_ARN = "arn:aws:kinesis:ap-northeast-2:853660505909:stream/IoTtoFlink";
    private static final String AWS_REGION = "ap-northeast-2";

    /**
     * KinesisStreamsSource<String> 객체를 생성하여 반환합니다.
     * 이 메소드는 Kinesis 스트림 연결에 필요한 설정을 담당합니다.
     *
     * @return 구성된 KinesisStreamsSource 인스턴스
     */
    public static KinesisStreamsSource<String> createKinesisSource() {
        Configuration sourceConfig = new Configuration();
        sourceConfig.setString("aws.region", AWS_REGION);
        sourceConfig.set(KinesisSourceConfigOptions.STREAM_INITIAL_POSITION, KinesisSourceConfigOptions.InitialPosition.LATEST);

        // 최대 레코드 수 (기본값 10,000 → 500으로 낮춤)
        sourceConfig.setString(ConsumerConfigConstants.SHARD_GETRECORDS_MAX, "500");

        // GetRecords 호출 간격 (기본 200ms → 3000ms로 증가)
        sourceConfig.setString(ConsumerConfigConstants.SHARD_GETRECORDS_INTERVAL_MILLIS, "3000");

        // 재시도 백오프 설정 추가
        sourceConfig.setString(ConsumerConfigConstants.SHARD_GETRECORDS_BACKOFF_BASE, "1000");
        sourceConfig.setString(ConsumerConfigConstants.SHARD_GETRECORDS_BACKOFF_MAX, "5000");

        // KinesisSource 빌더 사용
        KinesisStreamsSource<String> kinesisStreamsSource = KinesisStreamsSource.<String>builder()
                .setStreamArn(KINESIS_STREAM_ARN)
                .setDeserializationSchema(new SimpleStringSchema())
                .setSourceConfig(sourceConfig)
                .build();

        return kinesisStreamsSource;
    }
}