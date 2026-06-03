package com.amalitech.qa.base;

import com.amalitech.qa.utils.ConfigReader;
import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;

public class UIBaseTest {

    protected WebDriver driver;
    protected String baseUrl;

    @BeforeClass(alwaysRun = true)
    public void initDriver() {
        driver = DriverManager.getDriver();
        baseUrl = ConfigReader.get("base.url");
    }

    // Call this before navigating to login/register to avoid auth guard redirects
    protected void clearSession() {
        driver.manage().deleteAllCookies();
        try {
            ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
            ((JavascriptExecutor) driver).executeScript("window.sessionStorage.clear();");
        } catch (Exception ignored) {
            // storage may not be accessible before first page load
        }
    }

    // @AfterSuite so Chrome only launches once for the entire suite, not once per test class
    @AfterSuite(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
