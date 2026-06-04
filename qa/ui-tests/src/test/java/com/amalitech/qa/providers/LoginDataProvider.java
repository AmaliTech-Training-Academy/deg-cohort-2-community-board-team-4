package com.amalitech.qa.providers;

import com.amalitech.qa.utils.JsonDataReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.DataProvider;

public class LoginDataProvider {

    private static final String FILE = "testdata/login-data.json";

    @DataProvider(name = "loginPageTitle")
    public static Object[][] loginPageTitle() {
        return new Object[][]{{JsonDataReader.getString(FILE, "pageTitle")}};
    }

    @DataProvider(name = "invalidEmailLogin")
    public static Object[][] invalidEmailLogin() {
        JsonNode data = JsonDataReader.load(FILE).get("invalidEmailTest");
        return new Object[][]{{data.get("email").asText(), data.get("password").asText()}};
    }

    @DataProvider(name = "emptyEmailLogin")
    public static Object[][] emptyEmailLogin() {
        JsonNode data = JsonDataReader.load(FILE).get("emptyEmailTest");
        return new Object[][]{{data.get("password").asText()}};
    }

    @DataProvider(name = "wrongCredentialsLogin")
    public static Object[][] wrongCredentialsLogin() {
        JsonNode data = JsonDataReader.load(FILE).get("wrongCredentialsTest");
        return new Object[][]{{data.get("email").asText(), data.get("password").asText()}};
    }
}
