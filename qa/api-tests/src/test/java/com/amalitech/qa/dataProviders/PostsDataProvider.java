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

    // ✅ Static JSON — missing fields
    @DataProvider(name = "createPostInvalid")
    public Object[][] createPostInvalid() {
        return new Object[][] {
                { JsonReader.getTestData("posts/create_post.json", "missing_title") },
                { JsonReader.getTestData("posts/create_post.json", "missing_content") }
        };
    }
}