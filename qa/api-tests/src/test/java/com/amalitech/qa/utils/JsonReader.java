package com.amalitech.qa.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class JsonReader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String getTestData(String filePath, String key) {
        try {
            InputStream stream = JsonReader.class.getClassLoader().getResourceAsStream("testdata/" + filePath);
            if (stream == null) throw new RuntimeException("File not found: testdata/" + filePath);
            JsonNode root = mapper.readTree(stream);
            return mapper.writeValueAsString(root.get(key));
        } catch (Exception e) {
            throw new RuntimeException("Error reading JSON: " + filePath + " → " + key);
        }
    }
}
 