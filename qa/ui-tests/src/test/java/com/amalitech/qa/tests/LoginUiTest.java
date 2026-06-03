package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LoginUiTest extends UIBaseTest {

    private static final Logger log = LoggerFactory.getLogger(LoginUiTest.class);

    private LoginPage loginPage;

    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        clearSession();
        driver.get(baseUrl + ConfigReader.get("login.url"));
        loginPage = new LoginPage();
    }

    // ✅ Login page loads with correct heading
    @Test
    public void testLoginPageLoads() {
        log.info("Verifying login page loads correctly");
        assertTrue(loginPage.isLoaded(), "Login page heading should be visible");
        assertEquals(driver.getTitle(), "CommunityBoardUi");
    }

    // ✅ Successful login redirects to dashboard
    @Test
    public void testSuccessfulLogin() {
        log.info("Testing successful login with admin credentials");
        loginPage.login(
                ConfigReader.get("admin.email"),
                ConfigReader.get("admin.password")
        );
        loginPage.waitForRedirectToDashboard();
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Should redirect to dashboard after login");
    }

    // ✅ Invalid email format shows email error
    @Test
    public void testInvalidEmailShowsError() {
        log.info("Testing login with invalid email format");
        loginPage.enterEmail("not-an-email");
        loginPage.enterPassword("password123");
        loginPage.clickSubmit();
        String error = loginPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error message should be visible");
    }

    // ✅ Empty email shows email required error
    @Test
    public void testEmptyEmailShowsError() {
        log.info("Testing login with empty email");
        loginPage.enterPassword("password123");
        loginPage.clickSubmit();
        String error = loginPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error message should be visible for empty email");
    }

    // ✅ Empty password shows password required error
    @Test
    public void testEmptyPasswordShowsError() {
        log.info("Testing login with empty password");
        loginPage.enterEmail(ConfigReader.get("admin.email"));
        loginPage.clickSubmit();
        String error = loginPage.getPasswordError();
        assertFalse(error.isEmpty(), "Password error message should be visible for empty password");
    }

    // ✅ Wrong credentials — stays on login page
    @Test
    public void testWrongCredentialsStaysOnLoginPage() {
        log.info("Testing login with wrong credentials");
        loginPage.login("wrong@example.com", "wrongpassword");
        assertFalse(driver.getCurrentUrl().contains("/dashboard"),
                "Should not redirect to dashboard with wrong credentials");
    }

    // ✅ Register link navigates to register page
    @Test
    public void testRegisterLinkNavigatesToRegister() {
        log.info("Testing register link on login page");
        loginPage.clickRegisterLink();
        assertTrue(driver.getCurrentUrl().contains(ConfigReader.get("register.url")),
                "Should navigate to register page");
    }
}
