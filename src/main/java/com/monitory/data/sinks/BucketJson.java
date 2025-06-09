package com.monitory.data.sinks;

public class BucketJson {
    private final String bucketId;
    private final String json;

    public BucketJson(String bucketId, String json) {
        this.bucketId = bucketId;
        this.json = json;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getJson() {
        return json;
    }
}
