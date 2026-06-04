package com.amalitech.qa.dataProviders;

import com.amalitech.qa.utils.JsonReader;
import org.testng.annotations.DataProvider;

public class PostsDataProvider {

    // ✅ Dynamic titles — unique every run (avoids slug conflict)
    @DataProvider(name = "createPostAllCategories")
    public Object[][] createPostAllCategories() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return new Object[][] {
                { "{\"title\": \"General Post " + timestamp + "_1\", \"content\": \"General content\", \"categoryId\": 1}" },
                { "{\"title\": \"Events Post " + timestamp + "_2\", \"content\": \"Events content\", \"categoryId\": 2}" },
                { "{\"title\": \"Tech Post " + timestamp + "_3\", \"content\": \"Tech content\", \"categoryId\": 3}" },
                { "{\"title\": \"Help Post " + timestamp + "_4\", \"content\": \"Help content\", \"categoryId\": 4}" }
        };
    }

    // ✅ Static JSON — all invalid cases
    @DataProvider(name = "createPostMissingData")
    public Object[][] createPostInvalid() {
        return new Object[][] {
                { JsonReader.getTestData("posts/create_post_invalid.json", "missing_title") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "missing_content") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "missing_category") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "empty_title") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "empty_content") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "whitespace_title") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "whitespace_content") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "empty_body") }
        };
    }

    @DataProvider(name = "createPostWrongData")
    public Object[][] createPostWrong() {
        return new Object[][] {

                { JsonReader.getTestData("posts/create_post_invalid.json", "invalid_category_id") },
                { JsonReader.getTestData("posts/create_post_invalid.json", "negative_category_id") },

        };
    }

}
