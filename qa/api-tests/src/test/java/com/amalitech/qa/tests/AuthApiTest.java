package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.AuthDataProvider;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuthApiTest extends BaseTest {

    // ✅ Register - happy path (runs twice with success_1 and success_2)
    @Test(dataProvider = "registerSuccess", dataProviderClass = AuthDataProvider.class)
    public void testRegisterSuccess(String body) {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body(body)
                .when()
                .post("/auth/register")   // ← update when endpoint is known
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    // register invalid data
    @Test(dataProvider = "registerInvalid", dataProviderClass = AuthDataProvider.class)
    public void testRegisterInvalid(String body) {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body(body)
                .when()
                .post("/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Register - duplicate email → 400 (runs twice)
    @Test(dataProvider = "registerDuplicate", dataProviderClass = AuthDataProvider.class)
    public void testRegisterDuplicateEmail(String body) {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body(body)
                .when()
                .post("/auth/register")   // ← update when endpoint is known
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Login - valid credentials → JWT (runs twice)
    @Test(dataProvider = "loginValid", dataProviderClass = AuthDataProvider.class)
    public void testLoginSuccess(String body) {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body(body)
                .when()
                .post("/auth/login")      // ← update when endpoint is known
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("token", notNullValue());
    }

    // ✅ Login - invalid credentials → 401 (runs twice)
    @Test(dataProvider = "loginInvalid", dataProviderClass = AuthDataProvider.class)
    public void testLoginInvalidCredentials(String body) {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body(body)
                .when()
                .post("/auth/login")      // ← update when endpoint is known
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }
}