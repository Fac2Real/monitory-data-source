package com.monitory.data.transformations;

import org.apache.flink.api.common.functions.MapFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDateTime;

public class TimeStampAssigner implements MapFunction<String, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String map(String value) throws Exception {
        ObjectNode jsonNode = (ObjectNode) mapper.readTree(value);
        jsonNode.put("time", LocalDateTime.now().toString());
        return mapper.writeValueAsString(jsonNode);
    }
}
