package com.amalitech.qa.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static Properties properties = new Properties();

    static {
        try {
            InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");
            if (stream == null) throw new RuntimeException("config.properties not found");
            properties.load(stream);
        } catch (Exception e) {
            throw new RuntimeException("config.properties not found");
        }
    }

    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property '" + key + "' not found in config.properties");
        }
        return value;
    }
}