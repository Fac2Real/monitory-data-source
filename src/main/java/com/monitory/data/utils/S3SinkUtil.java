package com.monitory.data.utils;

import com.monitory.data.sinks.S3BucketAssigner;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;

import java.time.Duration;

public class S3SinkUtil {
    private static final BucketAssigner<String, String> s3BucketAssigner = new S3BucketAssigner();
    public static FileSink<String> createS3Sink(String s3Bucket) {
        return FileSink
                .forRowFormat(
                        new Path("s3a://" + s3Bucket + "/"),
                        new SimpleStringEncoder<String>("UTF-8")
                )
                .withBucketAssigner(s3BucketAssigner)
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(Duration.ofMinutes(5))
                                .withInactivityInterval(Duration.ofMinutes(1))
                                .withMaxPartSize(MemorySize.ofMebiBytes(128))
                                .build()
                )
                .build();
    }
}
