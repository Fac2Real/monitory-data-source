package com.monitory.data.utils;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;

import java.util.Properties;

public class KinesisSourceUtil {

    private static final String KINESIS_STREAM_ARN = "arn:aws:kinesis:ap-northeast-2:853660505909:stream/IoTtoFlink";
    private static final String AWS_REGION = "ap-northeast-2";

    /**
     * KinesisStreamsSource<String> 객체를 생성하여 반환합니다.
     * 이 메소드는 Kinesis 스트림 연결에 필요한 설정을 담당합니다.
     *
     * @return 구성된 KinesisStreamsSource 인스턴스
     */
    public static FlinkKinesisConsumer<String> createKinesisSource() {
        Properties kinesisConsumerConfig = new Properties();
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.AWS_REGION, AWS_REGION);
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, "LATEST");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_MAX, "500");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_INTERVAL_MILLIS, "3000");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_BACKOFF_BASE, "1000");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_BACKOFF_MAX, "5000");
        // KINESIS_STREAM_ARN이 아닌, 스트림 이름("IoTtoFlink")을 명시해야 함
        return new FlinkKinesisConsumer<>(
                "IoTtoFlink",
                new SimpleStringSchema(),
                kinesisConsumerConfig
        );
    }
}