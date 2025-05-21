package com.monitory.data;

import com.monitory.data.transformations.DangerLevelAssigner;
import com.monitory.data.utils.KinesisSourceUtil;
import org.apache.flink.connector.kinesis.source.KinesisStreamsSource;
import com.monitory.data.transformations.TimeStampAssigner;
import com.monitory.data.utils.KafkaUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public class FlinkSourceApplication {
    public static void main (String [] args) throws Exception {
        // 1. Flink 환경 설정
        Configuration conf = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);


        // 2. 데이터 소스 설정
        KinesisStreamsSource<String> kinesisStreamSource = KinesisSourceUtil.createKinesisSource();

        DataStream<String> sourceStream = env.fromSource(
                kinesisStreamSource, // 변경된 소스 객체 사용
                WatermarkStrategy.noWatermarks(),
                "Kinesis-Streams-Source" // 이름 변경
        ).returns(TypeInformation.of(String.class));
        //        DataStream<String> sourceStream = env.fromSource(new MqttSource(), WatermarkStrategy.noWatermarks(), "MQTT-Source");

        // 3-1. 데이터 처리: Time Stamp 출력
        DataStream<String> timeTransformedStream = sourceStream
                .map(new TimeStampAssigner());

        // 3-2. 데이터 처리: 이상치 검색
        DataStream<String> labelTransformedStream = timeTransformedStream
                .map(new DangerLevelAssigner());


        // 4. 데이터 싱크: 콘솔에 출력 & kafka publish
        labelTransformedStream.sinkTo(KafkaUtil.createKafkaSink());
        labelTransformedStream.print();

        // 5. 실행
        env.execute("Flink to Kafka Produce");
    }
}
