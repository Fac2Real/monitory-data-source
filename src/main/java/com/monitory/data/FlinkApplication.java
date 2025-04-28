package com.monitory.data;

import com.monitory.data.sources.MqttSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
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

        // 3. 데이터 처리: 단순하게 문자열을 대문자로 변환하는 예시
        DataStream<String> transformedStream = sourceStream
                .map(new MapFunction<String, String>() {
                    @Override
                    public String map(String value) throws Exception {
//                        Thread.sleep(2000000);
                        System.out.println("💡 received: " + value);
                        return value.toUpperCase();
                    }
                });

        // 4. 데이터 싱크: 콘솔에 출력
        transformedStream.print();

        // 5. 실행
        env.execute("Flink DataStream Example");
    }
}
