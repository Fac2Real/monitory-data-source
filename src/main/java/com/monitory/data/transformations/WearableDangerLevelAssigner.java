package com.monitory.data.transformations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.MapFunction;

public class WearableDangerLevelAssigner implements MapFunction<String, String>  {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String map(String value) throws Exception {
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(value);

        if (jsonNode.has("category") && "WEARABLE".equalsIgnoreCase(jsonNode.get("category").asText())) {
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
                case "heartRate":
                    if (sensorValue < 60.0 ||  sensorValue > 140.0) {
                        dangerLevel = 2;
                    }
                    break;
                default:
                    return value;
            }
            jsonNode.put("dangerLevel", dangerLevel);
            return mapper.writeValueAsString(jsonNode);
        }
        else {
            return value;
        }
    }
}
