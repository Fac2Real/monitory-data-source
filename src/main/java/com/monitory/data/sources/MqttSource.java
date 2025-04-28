package com.monitory.data.sources;

import org.apache.flink.api.connector.source.*;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.nio.charset.StandardCharsets;
/**
 * Flink에서 사용하는 커스텀 MQTT Source
 * Source<T, SplitT, CheckpointT>
 *  - T: 읽어올 데이터 타입 (여기선 String)
 *  - SplitT: 데이터를 나누는 단위 (MqttSplit)
 *  - CheckpointT: 체크포인트에 저장할 상태 (여기선 Void)
 */
public class MqttSource implements Source<String, MqttSplit, Void> {
    /**
     * 이 소스는 실시간 데이터 스트림이기 때문에 "Unbounded" (끝이 없음)
     */
    @Override
    public Boundedness getBoundedness() {
        return Boundedness.CONTINUOUS_UNBOUNDED;
    }

    /**
     * SplitEnumerator 생성: 각 parallel task에 split을 분배하는 역할
     */
    @Override
    public SplitEnumerator<MqttSplit, Void> createEnumerator(SplitEnumeratorContext<MqttSplit> splitEnumeratorContext) throws Exception {
        return new MqttSplitEnumerator(splitEnumeratorContext);
    }

    /**
     * 장애 복구 시 사용: 이전 상태에서 SplitEnumerator 복구
     */
    @Override
    public SplitEnumerator<MqttSplit, Void> restoreEnumerator(SplitEnumeratorContext<MqttSplit> splitEnumeratorContext, Void checkpoint) throws Exception {
        return createEnumerator(splitEnumeratorContext);
    }

    /**
     * Split을 직렬화/역직렬화하는 방법 정의 (필수)
     */
    @Override
    public SimpleVersionedSerializer<MqttSplit> getSplitSerializer() {
        return new SimpleVersionedSerializer<>() {
            @Override public int getVersion() { return 1; }
            @Override public byte[] serialize(MqttSplit split) {
                return split.splitId().getBytes(StandardCharsets.UTF_8);
            }

            @Override public MqttSplit deserialize(int version, byte[] serialized) {
                return new MqttSplit(new String(serialized, StandardCharsets.UTF_8));
            }
        };
    }
    /**
     * Enumerator의 체크포인트 상태 직렬화 (우린 상태 없음)
     */
    @Override
    public SimpleVersionedSerializer<Void> getEnumeratorCheckpointSerializer() {
        return new SimpleVersionedSerializer<>() {
            @Override public int getVersion() { return 1; }
            @Override public byte[] serialize(Void obj) { return new byte[0]; }
            @Override public Void deserialize(int version, byte[] serialized) { return null; }
        };
    }
    /**
     * SourceReader 생성: 실제 MQTT 데이터를 읽어오는 곳
     */
    @Override
    public SourceReader<String, MqttSplit> createReader(SourceReaderContext sourceReaderContext) throws Exception {
        return new MqttReader(sourceReaderContext);
    }
}
