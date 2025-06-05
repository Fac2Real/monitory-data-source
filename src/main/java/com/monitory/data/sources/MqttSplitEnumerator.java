package com.monitory.data.sources;

import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * MQTT용 SplitEnumerator
 * - Flink에서 parallel reader에게 데이터를 분배할 "split"을 지정하는 역할
 */
public class MqttSplitEnumerator implements SplitEnumerator<MqttSplit, Void> {
    private final SplitEnumeratorContext<MqttSplit> context;

    // Flink에서 이 enumerator와 상호작용하는 데 필요한 context
    public MqttSplitEnumerator(SplitEnumeratorContext<MqttSplit> context) {
        this.context = context;
    }

    /**
     * Flink가 enumerator를 시작할 때 호출됨
     * 여기서는 단일 split("mqtt-split")을 0번 서브태스크에게 할당하고,
     * 더 이상 split이 없다고 알림
     */
    @Override
    public void start() {
//        context.assignSplit(new MqttSplit("mqtt-split"), 0);
//        context.signalNoMoreSplits(0);
    }

    @Override public void handleSplitRequest(int subtaskId, @Nullable String requesterHostname) {}
    @Override public void addSplitsBack(List<MqttSplit> list, int i) {}
    @Override public void addReader(int subtaskId) {
        System.out.println("Subtask " + subtaskId + " is ready, assigning split...");
        MqttSplit split = new MqttSplit("mqtt-split-"+subtaskId); // 네가 설계한 split 객체
        context.assignSplit(split, subtaskId);
    }
    @Override public Void snapshotState(long checkpointId) { return null; }
    @Override public void close() {}
}
