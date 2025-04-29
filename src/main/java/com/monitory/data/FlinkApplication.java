package com.monitory.data;

import com.monitory.data.sources.MqttSource;
import com.monitory.data.transformations.TimeStampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkApplication {
    public static void main (String [] args) throws Exception {
        // 1. Flink 환경 설정
        Configuration conf = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

        // 2. 데이터 소스
        DataStream<String> sourceStream = env.fromSource(new MqttSource(), WatermarkStrategy.noWatermarks(), "MQTT-Source");

        // 3. 데이터 처리: Time Stamp 출력과 Anomaly 감지
        DataStream<String> transformedStream = sourceStream
                .map(new TimeStampAssigner());

        // 4. 데이터 싱크: 콘솔에 출력
        transformedStream.print();

        // 5. 실행
        env.execute("Flink DataStream Example");
    }
}
