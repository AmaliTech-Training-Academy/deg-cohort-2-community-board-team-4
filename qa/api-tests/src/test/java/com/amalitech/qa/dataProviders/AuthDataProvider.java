package com.amalitech.qa.dataProviders;


import com.amalitech.qa.utils.JsonReader;
import org.testng.annotations.DataProvider;

public class AuthDataProvider {

    @DataProvider(name = "registerSuccess")
    public Object[][] registerSuccessData() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return new Object[][] {
                { "{\"email\": \"first_" + timestamp + "@amalitech.com\", \"password\": \"passcode123!\", \"name\": \"Lion\"}" },
                { "{\"email\": \"second_" + timestamp + "@amalitech.com\", \"password\": \"passd123%\", \"name\": \"kagozi\"}" }
        };
    }

    @DataProvider(name = "registerDuplicate")
    public Object[][] registerDuplicateData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/register.json", "duplicate_email_1") },
                { JsonReader.getTestData("auth/register.json", "duplicate_email_2") }
        };
    }

    @DataProvider(name = "loginValid")
    public Object[][] loginValidData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/login.json", "valid_1") },
                { JsonReader.getTestData("auth/login.json", "valid_2") }
        };
    }

    @DataProvider(name = "registerInvalid")
    public Object[][] registerInvalidData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/register.json", "invalid_register_1") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_2") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_3") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_4") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_5") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_6") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_7") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_8") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_9") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_10") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_11") },
                { JsonReader.getTestData("auth/register.json", "invalid_register_12") },
                {JsonReader.getTestData("auth/register.json", "invalid_register_13")}
        };
    }
    @DataProvider(name = "loginInvalid")
    public Object[][] loginInvalidData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/login.json", "invalid_1") },
                { JsonReader.getTestData("auth/login.json", "invalid_2") }
        };
    }
}