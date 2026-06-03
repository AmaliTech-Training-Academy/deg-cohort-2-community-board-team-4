package com.amalitech.qa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;
import static org.testng.Assert.*;

import java.time.Duration;

public class LoginPageTest {

    private WebDriver driver;

    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
    }

    @Test
    public void testLoginPageLoads() {
        driver.get("http://localhost:3000/login");
        assertTrue(driver.getTitle().contains("CommunityBoard") || driver.getTitle().contains("Ping"));
        assertNotNull(driver.findElement(By.id("email")));
        assertNotNull(driver.findElement(By.id("password")));
    }

    @Test
    public void testSuccessfulLogin() {
        driver.get("http://localhost:3000/login");
        
        // Locate inputs and fill credentials
        driver.findElement(By.id("email")).sendKeys("admin@amalitech.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        
        // Submit the form
        driver.findElement(By.className("btn-submit")).click();
        
        // Wait up to 5 seconds for the URL to change to dashboard
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        
        // Assert redirected URL contains '/dashboard'
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/dashboard"), "Expected redirection to /dashboard, but got: " + currentUrl);
    }

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }
}

