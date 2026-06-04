package com.amalitech.qa.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode load(String resourcePath) {
        try {
            InputStream stream = JsonDataReader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) throw new RuntimeException("Test data file not found: " + resourcePath);
            return MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data from: " + resourcePath, e);
        }
    }

    public static String getString(String resourcePath, String... keys) {
        JsonNode node = load(resourcePath);
        for (String key : keys) {
            node = node.get(key);
            if (node == null) throw new RuntimeException("Key '" + key + "' not found in: " + resourcePath);
        }
        return node.asText();
    }
}
