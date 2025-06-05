package com.monitory.data.sources;

import org.apache.flink.api.connector.source.SourceSplit;

public class MqttSplit implements SourceSplit {
    private final String splitId;

    public MqttSplit(String splitId) {
        this.splitId = splitId;
    }
    @Override
    public String splitId() {
        return splitId;
    }
}
