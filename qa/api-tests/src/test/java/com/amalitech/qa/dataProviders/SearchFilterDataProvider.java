package com.amalitech.qa.dataProviders;

import org.testng.annotations.DataProvider;

public class SearchFilterDataProvider {

    // ✅ Valid categories
    @DataProvider(name = "validCategory")
    public Object[][] validCategory() {
        return new Object[][] {
                { "General" },
                { "Events" },
                { "Tech" },
                { "Help" }
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
                { "general" },
                { "GENERAL" },
                { "GeNeRaL" }
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

    // ✅ Page sizes
    @DataProvider(name = "validPageSize")
    public Object[][] validPageSize() {
        return new Object[][] {
                { 1 },
                { 10 },
                { 100 }
        };
    }

    // ✅ Invalid page sizes
    @DataProvider(name = "invalidPageSize")
    public Object[][] invalidPageSize() {
        return new Object[][] {
                { 0 },
                { -1 },
                { 101 }
        };
    }
}