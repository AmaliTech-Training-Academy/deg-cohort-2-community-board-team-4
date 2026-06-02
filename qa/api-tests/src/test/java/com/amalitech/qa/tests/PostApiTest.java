package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.PostsDataProvider;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PostApiTest extends BaseTest {

    // ✅ Create post — all 4 categories (runs 4 times)
    @Test(dataProvider = "createPostAllCategories", dataProviderClass = PostsDataProvider.class)
    public void testCreatePostAllCategories(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts")           // ← update when endpoint confirmed
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", notNullValue());
    }

    // ✅ Create post — missing fields → 400 (runs twice)
    @Test(dataProvider = "createPostInvalid", dataProviderClass = PostsDataProvider.class)
    public void testCreatePostMissingFields(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts")           // ← update when endpoint confirmed
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Create post — no auth → 401
    @Test
    public void testCreatePostNoAuth() {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body("{\"title\": \"Unauth Post\", \"content\": \"Content\", \"categoryId\": 1}")
                .when()
                .post("/posts")           // ← update when endpoint confirmed
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Get all posts
    @Test
    public void testGetAllPosts() {
        given()
                .log().all()
                .when()
                .get("/posts")            // ← update when endpoint confirmed
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("$", not(empty()));
    }

    // ✅ Get single post — valid ID
    @Test
    public void testGetSinglePost() {
        given()
                .log().all()
                .when()
                .get("/posts/1")          // uses seeded post ID 1
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("id", equalTo(1));
    }

    // ✅ Get single post — invalid ID → 404
    @Test
    public void testGetPostNotFound() {
        given()
                .log().all()
                .when()
                .get("/posts/99999")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ Update post — owner
    @Test
    public void testUpdatePostAsOwner() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body("{\"title\": \"Updated Title\", \"content\": \"Updated content\", \"categoryId\": 1}")
                .when()
                .put("/posts/1")          // seeded post owned by admin
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    // ✅ Update post — non-owner → 403
    @Test
    public void testUpdatePostAsNonOwner() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .body("{\"title\": \"Hacked Title\", \"content\": \"Hacked content\", \"categoryId\": 1}")
                .when()
                .put("/posts/1")          // post owned by admin, user tries to update
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    // ✅ Delete post — non-owner → 403
    @Test
    public void testDeletePostAsNonOwner() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .log().all()
                .when()
                .delete("/posts/1")       // post owned by admin
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    // ✅ ADMIN can delete any post
    @Test
    public void testDeletePostAsAdmin() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .when()
                .delete("/posts/2")       // seeded post ID 2
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }
}