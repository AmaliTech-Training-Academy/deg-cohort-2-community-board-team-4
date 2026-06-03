package com.amalitech.qa.dataProviders;

import com.amalitech.qa.utils.JsonReader;
import org.testng.annotations.DataProvider;

public class CommentsDataProvider {

    @DataProvider(name = "createCommentValid")
    public Object[][] createCommentValid() {
        return new Object[][] {
                { JsonReader.getTestData("comments/create_comment.json", "valid_1") },
                { JsonReader.getTestData("comments/create_comment.json", "valid_2") }
        };
    }

    @DataProvider(name = "createCommentInvalid")
    public Object[][] createCommentInvalid() {
        return new Object[][] {
                { JsonReader.getTestData("comments/create_comment.json", "empty_content") },
                { JsonReader.getTestData("comments/create_comment.json", "whitespace_content") },
                { JsonReader.getTestData("comments/create_comment.json", "missing_content") },
        };
    }
}