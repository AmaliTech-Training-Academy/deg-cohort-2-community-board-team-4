package com.amalitech.qa.dataProviders;

import org.testng.annotations.DataProvider;

public class SearchFilterDataProvider {

    // ✅ Valid categories
    @DataProvider(name = "validCategory")
    public Object[][] validCategory() {
        return new Object[][] {
                { "NEWS" },
                { "EVENT" },
                { "DISCUSSION" },
                { "ALERT" }
        };
    }

    // ✅ Valid keywords
    @DataProvider(name = "validKeyword")
    public Object[][] validKeyword() {
        return new Object[][] {
                { "Welcome" },
                { "Team" }
        };
    }

    // ✅ Case insensitive categories
    @DataProvider(name = "caseInsensitiveCategory")
    public Object[][] caseInsensitiveCategory() {
        return new Object[][] {
                { "news" },
                { "NEWS" },
                { "nEwS" }
        };
    }

    // ✅ Case insensitive keywords
    @DataProvider(name = "caseInsensitiveKeyword")
    public Object[][] caseInsensitiveKeyword() {
        return new Object[][] {
                { "welcome" },
                { "WELCOME" },
                { "WeLcOmE" }
        };
    }

    // ✅ Valid page sizes
    @DataProvider(name = "validPageSize")
    public Object[][] validPageSize() {
        return new Object[][] {
                { 1 },
                { 10 },
                { 100 }
        };
    }

}