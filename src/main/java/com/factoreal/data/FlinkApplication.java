package com.factoreal.data;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.legacy.SourceFunction;

import java.util.Arrays;
import java.util.List;

public class FlinkApplication {
    public static void main (String [] args) throws Exception {
        // 1. Flink 환경 설정
        Configuration conf = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

        // 2. 커스텀 소스 사용 (예시로 간단한 데이터 소스)
        DataStream<String> sourceStream = env.addSource(new SourceFunction<String>() {
            private boolean running = true;

            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                List<String> data = Arrays.asList("Hello", "Flink", "Stream");
                for (String item : data) {
                    if (!running) {
                        return;
                    }
                    ctx.collect(item);  // 데이터 한 항목씩 전달
                }
            }

            @Override
            public void cancel() {
                running = false;  // 소스 중지
            }
        });
        // 3. 데이터 처리: 단순하게 문자열을 대문자로 변환하는 예시
        DataStream<String> transformedStream = sourceStream
                .map(new MapFunction<String, String>() {
                    @Override
                    public String map(String value) throws Exception {
                        Thread.sleep(2000000);
                        return value.toUpperCase();
                    }
                });

        // 4. 데이터 싱크: 콘솔에 출력
        transformedStream.print();

        // 5. 실행
        env.execute("Flink DataStream Example");
    }
}
