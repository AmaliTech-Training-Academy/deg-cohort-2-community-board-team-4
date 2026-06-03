package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.RegisterPage;
import com.amalitech.qa.utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RegisterUiTest extends UIBaseTest {

    private static final Logger log = LoggerFactory.getLogger(RegisterUiTest.class);

    private RegisterPage registerPage;

    // unique email per test run to avoid duplicate registration conflicts
    private String uniqueEmail() {
        return "testuser_" + System.currentTimeMillis() + "@example.com";
    }

    @BeforeMethod(alwaysRun = true)
    public void openRegisterPage() {
        clearSession();
        driver.get(baseUrl + ConfigReader.get("register.url"));
        registerPage = new RegisterPage();
    }

    // ✅ Register page loads with correct heading
    @Test
    public void testRegisterPageLoads() {
        log.info("Verifying register page loads correctly");
        assertTrue(registerPage.isLoaded(), "Register page heading should be visible");
        assertEquals(driver.getTitle(), "CommunityBoardUi");
    }

    // ✅ Successful registration auto-logs in and redirects to dashboard
    @Test
    public void testSuccessfulRegistration() {
        log.info("Testing successful registration with valid data");
        registerPage.register("Test User", uniqueEmail(), "Test@1234!", "Test@1234!");
        registerPage.waitForRedirectAfterRegistration();
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Should redirect to dashboard after successful registration");
    }

    // ✅ Empty full name shows error
    @Test
    public void testEmptyFullNameShowsError() {
        log.info("Testing registration with empty full name");
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword("Test@1234!");
        registerPage.enterConfirmPassword("Test@1234!");
        registerPage.clickSubmit();
        String error = registerPage.getFullNameError();
        assertFalse(error.isEmpty(), "Full name error should be shown when name is empty");
    }

    // ✅ Empty email shows error
    @Test
    public void testEmptyEmailShowsError() {
        log.info("Testing registration with empty email");
        registerPage.enterFullName("Test User");
        registerPage.enterPassword("Test@1234!");
        registerPage.enterConfirmPassword("Test@1234!");
        registerPage.clickSubmit();
        String error = registerPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error should be shown when email is empty");
    }

    // ✅ Invalid email format shows error
    @Test
    public void testInvalidEmailShowsError() {
        log.info("Testing registration with invalid email format");
        registerPage.enterFullName("Test User");
        registerPage.enterEmail("not-an-email");
        registerPage.enterPassword("Test@1234!");
        registerPage.enterConfirmPassword("Test@1234!");
        registerPage.clickSubmit();
        String error = registerPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error should be shown for invalid format");
    }

    // ✅ Short password (under 6 chars) shows error
    @Test
    public void testShortPasswordShowsError() {
        log.info("Testing registration with password under 6 characters");
        registerPage.enterFullName("Test User");
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword("abc");
        registerPage.enterConfirmPassword("abc");
        registerPage.clickSubmit();
        String error = registerPage.getPasswordError();
        assertFalse(error.isEmpty(), "Password error should be shown for short password");
    }

    // ✅ Mismatched passwords show confirm password error
    @Test
    public void testMismatchedPasswordsShowsError() {
        log.info("Testing registration with mismatched passwords");
        registerPage.enterFullName("Test User");
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword("Test@1234!");
        registerPage.enterConfirmPassword("Different@1234!");
        registerPage.clickSubmit();
        String error = registerPage.getConfirmPasswordError();
        assertFalse(error.isEmpty(), "Confirm password error should be shown when passwords don't match");
    }

    // ✅ Duplicate email — stays on register page
    @Test
    public void testDuplicateEmailStaysOnRegisterPage() {
        log.info("Testing registration with already-used email");
        // admin@amalitech.com already exists in the system
        registerPage.register(
                "Admin User",
                ConfigReader.get("admin.email"),
                "Test@1234!",
                "Test@1234!"
        );
        assertFalse(driver.getCurrentUrl().contains("/auth/login"),
                "Should not redirect to login when email is already registered");
    }

    // ✅ Login link navigates back to login page
    @Test
    public void testLoginLinkNavigatesToLogin() {
        log.info("Testing login link on register page");
        registerPage.clickLoginLink();
        assertTrue(driver.getCurrentUrl().contains(ConfigReader.get("login.url")),
                "Should navigate to login page");
    }
}
