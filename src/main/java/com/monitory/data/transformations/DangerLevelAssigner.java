package com.monitory.data.transformations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.MapFunction;

public class DangerLevelAssigner implements MapFunction<String, String>  {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String map(String value) throws Exception {
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(value);

        if (jsonNode.has("category") && "ENVIRONMENT".equalsIgnoreCase(jsonNode.get("category").asText())) {
            String sensorType = "";
            double sensorValue = Double.NaN;
            int dangerLevel = 0;

            if (jsonNode.has("sensorType") && jsonNode.get("sensorType").isTextual()) {
                sensorType = jsonNode.get("sensorType").asText();
            } else {
                jsonNode.put("dangerLevel", "unknown_type");
                return mapper.writeValueAsString(jsonNode);
            }

            if (jsonNode.has("val") && jsonNode.get("val").isNumber()) {
                sensorValue = jsonNode.get("val").asDouble();
            } else {
                jsonNode.put("dangerLevel", "invalid_value");
                return mapper.writeValueAsString(jsonNode);
            }

            // 출처: https://facto-real.atlassian.net/wiki/spaces/~557058e02db5ec8a1448f1a1ab0ae5d196e126/pages/22806599
            switch (sensorType.toLowerCase()) {
                case "temp":
                    if (sensorValue > 40.0) {
                        dangerLevel = 2;
                    } else if (sensorValue < -35) {
                        dangerLevel = 2;
                    } else if (sensorValue < 25 || sensorValue > 30) {
                        dangerLevel = 1;
                    }
                    break;

                case "humid":
                    if (sensorValue >= 80.0) {
                        dangerLevel = 2;
                    } else if (sensorValue > 60 && sensorValue < 80.0) {
                        dangerLevel = 1;
                    }
                    break;

                case "vibration":
                    if (sensorValue > 7.1) {
                        dangerLevel = 2;
                    } else if (sensorValue > 2.8 && sensorValue <= 7.1) {
                        dangerLevel = 1;
                    }
                    break;

                case "current":
                    if (sensorValue > 30) { // 예시 임계값 (ppm)
                        dangerLevel = 2;
                    } else if (sensorValue >= 7 && sensorValue <= 30) {
                        dangerLevel = 1;
                    }
                    break;

                case "dust":
                    if (sensorValue > 150) {
                        dangerLevel = 2;
                    } else if (sensorValue > 75 && sensorValue <= 150) {
                        dangerLevel = 1;
                    }
                    break;

                case "voc":
                    if (sensorValue > 1000) {
                        dangerLevel = 2;
                    } else if (sensorValue >= 300 && sensorValue <= 1000) {
                        dangerLevel = 1;
                    }
                    break;

                default:
                    dangerLevel = 0;
                    break;
            }
            jsonNode.put("dangerLevel", dangerLevel);
            return mapper.writeValueAsString(jsonNode);
        }
        else {
            return value;
        }
    }
}
