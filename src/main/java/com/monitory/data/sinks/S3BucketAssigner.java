package com.monitory.data.sinks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;

public class S3BucketAssigner implements BucketAssigner<String, String > {
    @Override
    public String getBucketId(String s, Context context) {
        System.out.println("⭐️url: "+ s.split("\\|", 2)[0]);
        return s.split("\\|", 2)[0];
    }

    @Override
    public SimpleVersionedSerializer<String> getSerializer() {
        return SimpleVersionedStringSerializer.INSTANCE;
    }
}
