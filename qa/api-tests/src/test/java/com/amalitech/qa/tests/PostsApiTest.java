package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.PostsDataProvider;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PostsApiTest extends BaseTest {

    private int postToDeleteId;
    private int postToGet;

    private void createPostToGet(){
        postToGet = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"title\": \"Post To Get Comments " + System.currentTimeMillis() + "\", \"content\": \"Post for listing comments\", \"categoryId\": 2}")
                .when()
                .post("/posts")
                .then()
                .statusCode(201)
                .extract().path("id");
    }
    // ✅ Create post — all 4 categories (runs 4 times)
    @Test(dataProvider = "createPostAllCategories", dataProviderClass = PostsDataProvider.class)
    public void testCreatePostAllCategories(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", notNullValue())
                .body("slug", notNullValue())
                .body("categoryId", notNullValue())
                .body("authorEmail", equalTo("admin@amalitech.com"));
    }

    // ✅ Create post — missing fields → 400 (runs twice)
    @Test(dataProvider = "createPostMissingData", dataProviderClass = PostsDataProvider.class)
    public void testCreatePostMissingFields(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Create post — missing fields → 400 (runs twice)
    @Test(dataProvider = "createPostWrongData", dataProviderClass = PostsDataProvider.class)
    public void testCreatePostWrongCategory(String body) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .body(body)
                .when()
                .post("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    // ✅ Create post — no auth → 401
    @Test
    public void testCreatePostNoAuth() {
        given()
                .contentType(ContentType.JSON)
                .log().all()
                .body("{\"title\": \"Unauth Post\", \"content\": \"Content\", \"categoryId\": 1}")
                .when()
                .post("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    // ✅ Get all posts — check pagination structure
    @Test
    public void testGetAllPosts() {
        given()
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].id", notNullValue())
                .body("content[0].title", notNullValue())
                .body("content[0].slug", notNullValue())
//                .body("content[0].categoryName", notNullValue())
                .body("content[0].authorName", notNullValue())
                .body("content[0].authorEmail", notNullValue())
                .body("totalElements", greaterThan(0))
                .body("totalPages", greaterThan(0))
                .body("empty", equalTo(false));
    }

    // ✅ Get all posts — with page and size params
    @Test
    public void testGetAllPostsWithPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 1)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("size", equalTo(1))
                .body("number", equalTo(0));
    }

    // ✅ Get single post
    @Test
    public void testGetSinglePost() {
        createPostToGet();

        given()
                .log().all()
                .when()
                .get("/posts/" + postToGet)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("id", equalTo(postToGet))
                .body("title", notNullValue())
                .body("slug", notNullValue())
                .body("categoryName", notNullValue())
                .body("authorName", notNullValue())
                .body("authorEmail", notNullValue())
                .body("commentCount", notNullValue());
    }
    // ✅ Get single post — invalid identifier → 404
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
                .put("/posts/1")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("title", equalTo("Updated Title"))
                .body("content", equalTo("Updated content"));
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
                .put("/posts/1")
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
                .delete("/posts/1")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }




    private void createPostForDeletion() {
        postToDeleteId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"title\": \"Post to delete " + System.currentTimeMillis() + "\", \"content\": \"Will be deleted\", \"categoryId\": 1}")
                .when()
                .post("/posts")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    // ✅ ADMIN can delete any post
    @Test(priority = 99)
    public void testDeletePostAsAdmin() {
        createPostForDeletion();  // ← call it directly inside the test

        given()
                .header("Authorization", "Bearer " + adminToken)
                .log().all()
                .when()
                .delete("/posts/" + postToDeleteId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

}