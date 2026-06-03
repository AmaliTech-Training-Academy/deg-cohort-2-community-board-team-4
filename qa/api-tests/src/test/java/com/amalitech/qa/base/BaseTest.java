package com.amalitech.qa.base;

import com.amalitech.qa.utils.ConfigReader;
import com.amalitech.qa.utils.JsonReader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;

public class BaseTest {

    protected static String adminToken;
    protected static String userToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = ConfigReader.get("base.url");

        // Extract admin token
        adminToken = given()
                .contentType(ContentType.JSON)
                .body(JsonReader.getTestData("auth/login.json", "valid_1"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        // Extract user token
        userToken = given()
                .contentType(ContentType.JSON)
                .body(JsonReader.getTestData("auth/login.json", "valid_2"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

}