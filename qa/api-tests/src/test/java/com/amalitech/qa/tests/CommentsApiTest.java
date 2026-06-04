package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.CommentsDataProvider;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Feature("Comments")
public class CommentsApiTest extends BaseTest {

    private int commentId;
    private int commentPostId;
    private int commentId2;

    @BeforeClass(alwaysRun = true)
    public void createPostForComments() {
        commentPostId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"title\": \"Comment Test Post " + System.currentTimeMillis() + "\", \"content\": \"Post for comment testing\", \"categoryId\": 1}")
                .when()
                .post("/posts")
                .then()
                .extract().path("id");
    }

    @BeforeMethod(alwaysRun = true)
    public void createComment() {
        commentId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"content\": \"Setup comment for testing\"}")
                .when()
                .post("/posts/" + commentPostId + "/comments")
                .then()
                .extract().path("id");

        commentId2 = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .body("{\"content\": \"Setup comment for testing\"}")
                .when()
                .post("/posts/" + commentPostId + "/comments")
                .then()
                .extract().path("id");
    }

    // ✅ Add comment — valid (runs twice)
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "createCommentValid", dataProviderClass = CommentsDataProvider.class)
    public void testAddCommentValid(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts/" + commentPostId + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", notNullValue())
                .body("authorName", notNullValue())
                .body("createdAt", notNullValue());
    }

    // ✅ Add comment — invalid (runs 5 times)
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "createCommentInvalid", dataProviderClass = CommentsDataProvider.class)
    public void testAddCommentInvalid(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts/" + commentPostId + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Add comment — unauthenticated → 401
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void testAddCommentNoAuth() {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body("{\"content\": \"Unauthenticated comment\"}")
                .when()
                .post("/posts/" + commentPostId + "/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Add comment — invalid post → 404
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testAddCommentInvalidPost() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body("{\"content\": \"Comment on non-existent post\"}")
                .when()
                .post("/posts/-1/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ List comments — check response structure
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void testListComments() {
        given()
                .log().all()
                .when()
                .get("/posts/" + commentPostId + "/comments")
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
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testListCommentsInvalidPost() {
        given()
                .log().all()
                .when()
                .get("/posts/-1/comments")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ Delete comment — non-owner → 403
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 1)
    public void testDeleteCommentAsNonOwner() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/" + commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    // ✅ Delete comment — owner
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 2)
    public void testDeleteCommentAsOwner() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/" + commentId2)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    // ✅ Delete comment — unauthenticated → 401
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 3)
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
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testDeleteCommentInvalidId() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/comments/-1")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }
}
