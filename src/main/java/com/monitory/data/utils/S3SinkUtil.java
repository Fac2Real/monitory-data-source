package com.monitory.data.utils;

import com.monitory.data.sinks.BucketJson;
import com.monitory.data.sinks.S3BucketAssigner;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class S3SinkUtil {
    private static final BucketAssigner<BucketJson, String> s3BucketAssigner = new S3BucketAssigner();
    public static FileSink<BucketJson> createS3Sink(String s3Bucket) {
        OutputFileConfig outputFileConfig = OutputFileConfig.builder()
                .withPartPrefix("equip")
                .withPartSuffix(".json")
                .build();

        return FileSink
                .forRowFormat(
                        new Path("s3a://" + s3Bucket + "/EQUIPMENT"),
                        new SimpleStringEncoder<BucketJson>("UTF-8"){
                            @Override
                            public void encode(BucketJson record, OutputStream stream) throws IOException {
                                stream.write((record.json() + "\n").getBytes(StandardCharsets.UTF_8));
                            }
                        }
                )
                .withBucketAssigner(s3BucketAssigner)
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(Duration.ofHours(1))
                                .withInactivityInterval(Duration.ofMinutes(5))
                                .withMaxPartSize(MemorySize.ofMebiBytes(512))
                                .build()
                )
                .withOutputFileConfig(outputFileConfig)
                .build();
    }
}
