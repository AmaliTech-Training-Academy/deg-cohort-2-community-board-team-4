package com.amalitech.qa.providers;

import com.amalitech.qa.utils.JsonDataReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.DataProvider;

public class RegisterDataProvider {

    private static final String FILE = "testdata/register-data.json";

    @DataProvider(name = "registerPageTitle")
    public static Object[][] registerPageTitle() {
        return new Object[][]{{JsonDataReader.getString(FILE, "pageTitle")}};
    }

    @DataProvider(name = "successfulRegistration")
    public static Object[][] successfulRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("successfulRegistration");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "emptyFullNameRegistration")
    public static Object[][] emptyFullNameRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("emptyFullNameTest");
        return new Object[][]{{
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "emptyEmailRegistration")
    public static Object[][] emptyEmailRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("emptyEmailTest");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "invalidEmailRegistration")
    public static Object[][] invalidEmailRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("invalidEmailTest");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("email").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "shortPasswordRegistration")
    public static Object[][] shortPasswordRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("shortPasswordTest");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "mismatchedPasswordsRegistration")
    public static Object[][] mismatchedPasswordsRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("mismatchedPasswordsTest");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }

    @DataProvider(name = "duplicateEmailRegistration")
    public static Object[][] duplicateEmailRegistration() {
        JsonNode data = JsonDataReader.load(FILE).get("duplicateEmailTest");
        return new Object[][]{{
            data.get("fullName").asText(),
            data.get("password").asText(),
            data.get("confirmPassword").asText()
        }};
    }
}
