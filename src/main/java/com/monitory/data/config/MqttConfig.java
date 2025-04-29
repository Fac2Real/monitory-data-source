package com.monitory.data.config;

import java.io.InputStream;
import java.util.Properties;

public class MqttConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = MqttConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("❌ application.properties 파일을 찾을 수 없습니다.");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("❌ properties 파일 로딩 실패", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
