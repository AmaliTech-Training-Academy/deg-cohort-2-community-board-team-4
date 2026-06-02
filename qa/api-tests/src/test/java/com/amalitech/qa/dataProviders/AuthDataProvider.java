package com.amalitech.qa.dataProviders;


import com.amalitech.qa.utils.JsonReader;
import org.testng.annotations.DataProvider;

public class AuthDataProvider {

    @DataProvider(name = "registerSuccess")
    public Object[][] registerSuccessData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/register.json", "success_1") },
                { JsonReader.getTestData("auth/register.json", "success_2") }
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

    @DataProvider(name = "loginInvalid")
    public Object[][] loginInvalidData() {
        return new Object[][] {
                { JsonReader.getTestData("auth/login.json", "invalid_1") },
                { JsonReader.getTestData("auth/login.json", "invalid_2") }
        };
    }
}