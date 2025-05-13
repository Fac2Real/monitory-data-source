package com.monitory.data.sources;

import com.monitory.data.config.MqttConfig;
import com.monitory.data.utils.SslUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.core.io.InputStatus;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MQTT 메시지를 읽어오는 SourceReader
 * - Flink에서 데이터를 가져오는 역할
 */
@Slf4j
public class MqttReader implements SourceReader <String, MqttSplit> {
    // Flink에서 SourceReader와 상호작용하는 데 필요한 context
    private final SourceReaderContext sourceReaderContext;
    // MQTT로 읽은 메시지를 임시로 저장할 큐
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private CompletableFuture<Void> availabilityFuture = new CompletableFuture<>();
    // 메시지를 계속 읽어오게 할지 여부
    private volatile boolean running = true;
    private MqttClient client;

    private final IMqttMessageListener mqttMessageListener = (topic, message) -> {
        messageQueue.add(new String(message.getPayload()));
        synchronized (messageQueue) {
            if (availabilityFuture.isDone()) {
                availabilityFuture = new CompletableFuture<>();
            }
            availabilityFuture.complete(null); // 메시지 도착 시 신호 전송
        }
    };

    public MqttReader(SourceReaderContext sourceReaderContext) {
        this.sourceReaderContext = sourceReaderContext;
    }
    /**
     * SourceReader 시작 시 호출됨
     * MQTT 클라이언트 연결 및 구독 등을 처리할 수 있음
     * (현재는 비어 있음)
     */
    @Override
    public void start() {
        try {
            String broker = MqttConfig.get("AWS_IOT_BROKER");
            String topic = MqttConfig.get("AWS_IOT_TOPIC");
            String clientId = MqttConfig.get("AWS_IOT_CLIENT_ID");

            SslUtil sslUtil = new SslUtil();
            SSLSocketFactory temp = sslUtil.getSocketFactory(
                    MqttConfig.get("AWS_IOT_CA_PEM_PATH"),
                    MqttConfig.get("AWS_IOT_CERT_PATH"),
                    MqttConfig.get("AWS_IOT_PRIVATE_KEY_PATH")
                    );
            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(temp);
            options.setCleanSession(true);
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(60);

            client = new MqttClient(broker, clientId);
            log.info("⭐️ Connecting to MQTT broker: {}", broker);
            client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("✅ MQTT 연결 완료: reconnect={}, serverURI={}", reconnect, serverURI);
                    try {
                        if (client.isConnected()) {
                            client.subscribe(topic, mqttMessageListener);
                            log.info("📡 MQTT subscribe 완료: topic = {}", topic);
                        } else {
                            log.warn("⚠️ MQTT 클라이언트가 연결되지 않았습니다.");
                        }
                    } catch (Exception e) {
                        log.error("❌ MQTT subscribe 실패", e);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("⚠️ MQTT 연결 끊김", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    messageQueue.add(new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {}
            });

            client.connect(options);
            if (!client.isConnected()) {
                log.error("❌ MQTT 연결 실패: 연결이 되지 않았습니다.");
                throw new RuntimeException("MQTT 연결 실패");
            } else {
                log.info("✅ MQTT 연결 성공");
            }

        } catch (Exception e) {
            log.error("❌ MQTT Connection setup 중 예외 발생", e);
        }
    }
    /**
     * 큐에서 메시지를 하나씩 꺼내서 Flink에 전달
     * - 메시지가 있으면 데이터를 전달하고, 계속 받을 수 있음을 알림
     * - 메시지가 없으면 더 이상 데이터를 받을 수 없다고 알림
     * - pollNext() 수정: 메시지 처리 후 availabilityFuture 관리
     */
    @Override
    public InputStatus pollNext(ReaderOutput<String> output) throws InterruptedException{
        String msg = messageQueue.poll();
        if (msg != null) {
            output.collect(msg);
            return InputStatus.MORE_AVAILABLE;
        } else {
            // 메시지 없을 때 availabilityFuture 리셋
            synchronized (messageQueue) {
                availabilityFuture = new CompletableFuture<>();
            }
            return InputStatus.NOTHING_AVAILABLE;
        }
    }
    /**
     * 체크포인트 상태를 스냅샷으로 저장하는 메소드
     * - 현재 상태가 없으므로 빈 리스트 반환
     */
    @Override
    public List<MqttSplit> snapshotState(long l) {
        return List.of();
    }
    /**
     * 메시지가 수신될 때까지 대기
     * - 여기서는 구현되지 않음
     */
    @Override
    public CompletableFuture<Void> isAvailable() {
        synchronized (messageQueue) {
            return availabilityFuture;
        }
    }

    @Override
    public void addSplits(List<MqttSplit> list) {

    }

    @Override
    public void notifyNoMoreSplits() {

    }

    @Override
    public void close() throws Exception {
        running = false;
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
            log.info("🛑 MQTT Client disconnected and closed.");
        }
    }
}
