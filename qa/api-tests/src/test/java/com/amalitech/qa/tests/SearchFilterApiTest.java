package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.dataProviders.SearchFilterDataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SearchFilterApiTest extends BaseTest {

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
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].categoryName", equalToIgnoringCase(category));
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
                .log().ifValidationFails()
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
                .queryParam("category", "General")
                .queryParam("keyword", "Welcome")
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].categoryName", equalToIgnoringCase("General"));
    }

    // ✅ Combined filters — category + date range
    @Test
    public void testCombinedCategoryAndDateRange() {
        given()
                .queryParam("category", "General")
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

    // ✅ Pagination — invalid page size (runs 3 times)
    @Test(dataProvider = "invalidPageSize", dataProviderClass = SearchFilterDataProvider.class)
    public void testPaginationInvalidPageSize(int size) {
        given()
                .queryParam("page", 0)
                .queryParam("size", size)
                .log().all()
                .when()
                .get("/posts")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
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