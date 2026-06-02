package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.CommentsDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CommentsApiTest extends BaseTest {

    // ✅ Add comment — valid (runs twice)
    @Test(dataProvider = "createCommentValid", dataProviderClass = CommentsDataProvider.class)
    public void testAddCommentValid(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts/" + ConfigReader.get("post.id.1") + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("id", notNullValue());
    }

    // ✅ Add comment — unauthenticated → 401
    @Test
    public void testAddCommentNoAuth() {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body("{\"content\": \"Unauthenticated comment\"}")
                .when()
                .post("/posts/" + ConfigReader.get("post.id.1") + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Add comment — invalid post → 404
    @Test
    public void testAddCommentInvalidPost() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body("{\"content\": \"Comment on non-existent post\"}")
                .when()
                .post("/posts/99999/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ List comments — valid post
    @Test
    public void testListComments() {
        given()
                .log().all()
                .when()
                .get("/posts/" + ConfigReader.get("post.id.1") + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("$", notNullValue());
    }

    // ✅ Delete comment — unauthenticated → 401
    @Test
    public void testDeleteCommentNoAuth() {
        given()
                .log().all()
                .when()
                .delete("/comments/1")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Delete comment — non-owner → 403
    @Test
    public void testDeleteCommentNonOwner() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/1")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }
}