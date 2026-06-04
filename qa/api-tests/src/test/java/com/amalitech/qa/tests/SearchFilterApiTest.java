package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.SearchFilterDataProvider;
import io.restassured.http.ContentType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SearchFilterApiTest extends BaseTest {

    private String keyword1;
    private String keyword2;
    private int postId1;
    private int postId2;

    @BeforeClass(alwaysRun = true)
    public void createPostsForSearch() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        // create post 1 and store unique keyword
        keyword1 = "SearchKeyword_" + timestamp + "_1";
        postId1 = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"title\": \"" + keyword1 + "\", \"content\": \"Test content for search\", \"categoryId\": 1}")
                .when()
                .post("/posts")
                .then()
                .statusCode(201).extract().path("id");

        // create post 2 and store unique keyword
        keyword2 = "SearchKeyword_" + timestamp + "_2";
        postId2  = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"title\": \"" + keyword2 + "\", \"content\": \"Another content for search\", \"categoryId\": 2}")
                .when()
                .post("/posts")
                .then()
                .statusCode(201).extract().path("id");
    }

    @AfterClass(alwaysRun = true)
    public void deletePostsForSearch() {
        // delete post 1
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/posts/" + postId1)
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        // delete post 2
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/posts/" + postId2)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
        System.out.println("This is the end");
    }


    // ✅ Get all posts — no filters
    @Test
    public void testGetAllPostsNoFilter() {
        given()
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("totalElements", greaterThan(0))
                .body("empty", equalTo(false));
    }

    // ✅ Filter by valid category (runs 4 times)
    @Test(dataProvider = "validCategory", dataProviderClass = SearchFilterDataProvider.class)
    public void testFilterByCategory(String category) {
        given()
                .queryParam("category", category)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().all()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content.categoryName", everyItem(equalToIgnoringCase(category)));
    }

    // ✅ Filter by category — case insensitive (runs 3 times)
    @Test(dataProvider = "caseInsensitiveCategory", dataProviderClass = SearchFilterDataProvider.class)
    public void testFilterByCategoryCaseInsensitive(String category) {
        given()
                .queryParam("category", category)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()));
    }

    // ✅ Filter by invalid category — empty results
    @Test
    public void testFilterByInvalidCategory() {
        given()
                .queryParam("category", "INVALID")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().all()
                .statusCode(200)
                .body("content", empty())
                .body("totalElements", equalTo(0))
                .body("empty", equalTo(true));
    }

    // ✅ Search by valid keyword (runs twice)
    @Test(dataProvider = "validKeyword", dataProviderClass = SearchFilterDataProvider.class)
    public void testSearchByKeyword(String keyword) {
        given()
                .queryParam("keyword", keyword)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("totalElements", greaterThan(0));
    }

    // ✅ Search by keyword — case insensitive (runs 3 times)
    @Test(dataProvider = "caseInsensitiveKeyword", dataProviderClass = SearchFilterDataProvider.class)
    public void testSearchByKeywordCaseInsensitive(String keyword) {
        given()
                .queryParam("keyword", keyword)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()));
    }

    // ✅ Search by keyword — no results
    @Test
    public void testSearchByKeywordNoResults() {
        given()
                .queryParam("keyword", "xyznotfound123")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", empty())
                .body("totalElements", equalTo(0))
                .body("empty", equalTo(true));
    }

    // ✅ Filter by date range — valid
    @Test
    public void testFilterByDateRange() {
        given()
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2099-12-31")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("totalElements", greaterThan(0));
    }

    // ✅ Filter by date range — no results
    @Test
    public void testFilterByDateRangeNoResults() {
        given()
                .queryParam("from", "2000-01-01")
                .queryParam("to", "2000-12-31")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", empty())
                .body("totalElements", equalTo(0))
                .body("empty", equalTo(true));
    }

    // ✅ Filter by invalid date format → 400
    @Test
    public void testFilterByInvalidDateFormat() {
        given()
                .queryParam("from", "01-01-2024")
                .queryParam("to", "31-12-2024")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    // ✅ Combined filters — category + keyword
    @Test
    public void testCombinedCategoryAndKeyword() {
        given()
                .queryParam("category", "news")
                .queryParam("keyword", keyword1)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].categoryName", equalToIgnoringCase("news"));
    }

    // ✅ Combined filters — category + date range
    @Test
    public void testCombinedCategoryAndDateRange() {
        given()
                .queryParam("category", "discussion")
                .queryParam("from", "2024-01-01")
                .queryParam("to", "2099-12-31")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()));
    }

    // ✅ Pagination — valid page size (runs 3 times)
    @Test(dataProvider = "validPageSize", dataProviderClass = SearchFilterDataProvider.class)
    public void testPaginationValidPageSize(int size) {
        given()
                .queryParam("page", 0)
                .queryParam("size", size)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("size", equalTo(size))
                .body("number", equalTo(0));
    }


    // ✅ Pagination — second page
    @Test
    public void testPaginationSecondPage() {
        given()
                .queryParam("page", 1)
                .queryParam("size", 1)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("number", equalTo(1))
                .body("size", equalTo(1));
    }
}