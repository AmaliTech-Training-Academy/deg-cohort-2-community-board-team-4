package com.amalitech.qa.base;

import com.amalitech.qa.utils.ConfigReader;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;

public class BaseTest {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = ConfigReader.get("base.url");
    }
}
