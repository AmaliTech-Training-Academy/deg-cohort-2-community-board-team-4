package com.amalitech.qa.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class JsonReader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String getTestData(String filePath, String key) {
        try {
            JsonNode root = mapper.readTree(
                    new File("src/test/resources/testdata/" + filePath)
            );
            return mapper.writeValueAsString(root.get(key));
        } catch (Exception e) {
            throw new RuntimeException("Error reading JSON: " + filePath + " → " + key);
        }
    }
}
 