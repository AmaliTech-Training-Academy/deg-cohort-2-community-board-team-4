package com.amalitech.qa.providers;

import com.amalitech.qa.utils.JsonDataReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.DataProvider;

public class DashboardDataProvider {

    private static final String FILE = "testdata/dashboard-data.json";

    @DataProvider(name = "expectedUserName")
    public static Object[][] expectedUserName() {
        return new Object[][]{{JsonDataReader.getString(FILE, "expectedUserName")}};
    }

    @DataProvider(name = "searchKeyword")
    public static Object[][] searchKeyword() {
        return new Object[][]{{JsonDataReader.getString(FILE, "searchKeyword")}};
    }

    @DataProvider(name = "noResultsSearchKeyword")
    public static Object[][] noResultsSearchKeyword() {
        return new Object[][]{{JsonDataReader.getString(FILE, "noResultsSearchKeyword")}};
    }

    @DataProvider(name = "newsCategoryFilter")
    public static Object[][] newsCategoryFilter() {
        return new Object[][]{{JsonDataReader.getString(FILE, "newsCategory")}};
    }

    @DataProvider(name = "allCategoryFilter")
    public static Object[][] allCategoryFilter() {
        JsonNode root = JsonDataReader.load(FILE);
        return new Object[][]{{
            root.get("newsCategory").asText(),
            root.get("allCategory").asText()
        }};
    }

    @DataProvider(name = "newPostData")
    public static Object[][] newPostData() {
        JsonNode data = JsonDataReader.load(FILE).get("newPost");
        return new Object[][]{{
            data.get("titlePrefix").asText(),
            data.get("body").asText()
        }};
    }

    @DataProvider(name = "paginationData")
    public static Object[][] paginationData() {
        JsonNode data = JsonDataReader.load(FILE).get("pagination");
        return new Object[][]{{
            data.get("initialPage").asText(),
            data.get("nextPage").asText()
        }};
    }
}
