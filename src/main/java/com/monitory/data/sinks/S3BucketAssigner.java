package com.monitory.data.sinks;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;

public class S3BucketAssigner implements BucketAssigner<BucketJson, String> {
    @Override
    public String getBucketId(BucketJson s, Context context) {
        return s.getBucketId();
    }

    @Override
    public SimpleVersionedSerializer<String> getSerializer() {
        return SimpleVersionedStringSerializer.INSTANCE;
    }
}
