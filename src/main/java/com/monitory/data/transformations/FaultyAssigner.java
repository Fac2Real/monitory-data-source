package com.monitory.data.transformations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.MapFunction;

public class FaultyAssigner implements MapFunction<String, String>  {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String map(String value) throws Exception {
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(value);

        if (jsonNode.has("category") && "EQUIPMENT".equalsIgnoreCase(jsonNode.get("category").asText())) {
            String sensorType = "";
            double sensorValue = Double.NaN;
            int faulty = 1;

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

            // 출처: https://facto-real.atlassian.net/wiki/spaces/~557058e02db5ec8a1448f1a1ab0ae5d196e126/pages/26902590
            switch (sensorType.toLowerCase()) {
                case "temp":
                    if ((sensorValue> 61) && (sensorValue < 81)) {
                        faulty = 0;
                    }
                    break;

                case "humid":
                    if ((sensorValue> 38.18) && (sensorValue < 61.86)) {
                        faulty = 0;
                    }
                    break;

                case "vibration":
                    if ((sensorValue> 0.88) && (sensorValue < 2.34)) {
                        faulty = 0;
                    }
                    break;

                case "pressure":
                    if ((sensorValue> 25.36) && (sensorValue < 46.12)) {
                        faulty = 0;
                    }
                    break;

                case "active_power":
                    if ((sensorValue> 14974) && (sensorValue < 91500)) {
                        faulty = 0;
                    }
                    break;

                case "reactive_power":
                    if ((sensorValue> 9771) && (sensorValue < 48265)) {
                        faulty = 0;
                    }
                    break;
                default:
                    return value;
            }
            jsonNode.put("faulty", faulty);
            return mapper.writeValueAsString(jsonNode);
        }
        else {
            return value;
        }
    }
}
