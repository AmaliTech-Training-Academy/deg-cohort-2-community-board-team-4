package com.amalitech.qa.providers;

import com.amalitech.qa.utils.JsonDataReader;
import org.testng.annotations.DataProvider;

public class PostDetailDataProvider {

    private static final String FILE = "testdata/postdetail-data.json";

    @DataProvider(name = "breadcrumbText")
    public static Object[][] breadcrumbText() {
        return new Object[][]{{JsonDataReader.getString(FILE, "breadcrumbText")}};
    }

    @DataProvider(name = "testComment")
    public static Object[][] testComment() {
        return new Object[][]{{JsonDataReader.getString(FILE, "testComment")}};
    }

    @DataProvider(name = "addCommentPrefix")
    public static Object[][] addCommentPrefix() {
        return new Object[][]{{JsonDataReader.getString(FILE, "addCommentPrefix")}};
    }

    @DataProvider(name = "editCommentPrefix")
    public static Object[][] editCommentPrefix() {
        return new Object[][]{{JsonDataReader.getString(FILE, "editCommentPrefix")}};
    }

    @DataProvider(name = "deleteCommentPrefix")
    public static Object[][] deleteCommentPrefix() {
        return new Object[][]{{JsonDataReader.getString(FILE, "deleteCommentPrefix")}};
    }
}
