package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.CommentsDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CommentsApiTest extends BaseTest {

    private int commentId;

    @BeforeMethod
    public void createComment() {
        commentId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body("{\"content\": \"Setup comment for testing\"}")
                .when()
                .post("/posts/" + ConfigReader.get("post.id.1") + "/comments")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

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
                .body("id", notNullValue())
                .body("content", notNullValue())
                .body("authorName", notNullValue())
                .body("createdAt", notNullValue());
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

    // ✅ List comments — check response structure
    @Test
    public void testListComments() {
        given()
                .log().all()
                .when()
                .get("/posts/" + ConfigReader.get("post.id.1") + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].content", notNullValue())
                .body("[0].authorName", notNullValue())
                .body("[0].createdAt", notNullValue());
    }

    // ✅ List comments — invalid post → 404
    @Test
    public void testListCommentsInvalidPost() {
        given()
                .log().all()
                .when()
                .get("/posts/99999/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ Delete comment — owner
    @Test
    public void testDeleteCommentAsOwner() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/" + commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    // ✅ Delete comment — non-owner → 403
    @Test
    public void testDeleteCommentAsNonOwner() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .when()
                .delete("/comments/" + commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    // ✅ Delete comment — unauthenticated → 401
    @Test
    public void testDeleteCommentNoAuth() {
        given()
                .log().all()
                .when()
                .delete("/comments/" + commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Delete comment — invalid ID → 404
    @Test
    public void testDeleteCommentInvalidId() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/99999")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }
}