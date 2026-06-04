package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.RegisterPage;
import com.amalitech.qa.providers.RegisterDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Feature("Registration")
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
    @Severity(SeverityLevel.MINOR)
    @Test(dataProvider = "registerPageTitle", dataProviderClass = RegisterDataProvider.class)
    public void testRegisterPageLoads(String expectedTitle) {
        log.info("Verifying register page loads correctly");
        assertTrue(registerPage.isLoaded(), "Register page heading should be visible");
        assertEquals(driver.getTitle(), expectedTitle);
    }

    // ✅ Successful registration auto-logs in and redirects to dashboard
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "successfulRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testSuccessfulRegistration(String fullName, String password, String confirmPassword) {
        log.info("Testing successful registration with valid data");
        registerPage.register(fullName, uniqueEmail(), password, confirmPassword);
        registerPage.waitForRedirectAfterRegistration();
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Should redirect to dashboard after successful registration");
    }

    // ✅ Empty full name shows error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "emptyFullNameRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testEmptyFullNameShowsError(String password, String confirmPassword) {
        log.info("Testing registration with empty full name");
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword(password);
        registerPage.enterConfirmPassword(confirmPassword);
        registerPage.clickSubmit();
        String error = registerPage.getFullNameError();
        assertFalse(error.isEmpty(), "Full name error should be shown when name is empty");
    }

    // ✅ Empty email shows error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "emptyEmailRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testEmptyEmailShowsError(String fullName, String password, String confirmPassword) {
        log.info("Testing registration with empty email");
        registerPage.enterFullName(fullName);
        registerPage.enterPassword(password);
        registerPage.enterConfirmPassword(confirmPassword);
        registerPage.clickSubmit();
        String error = registerPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error should be shown when email is empty");
    }

    // ✅ Invalid email format shows error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "invalidEmailRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testInvalidEmailShowsError(String fullName, String email, String password, String confirmPassword) {
        log.info("Testing registration with invalid email format");
        registerPage.enterFullName(fullName);
        registerPage.enterEmail(email);
        registerPage.enterPassword(password);
        registerPage.enterConfirmPassword(confirmPassword);
        registerPage.clickSubmit();
        String error = registerPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error should be shown for invalid format");
    }

    // ✅ Short password (under 6 chars) shows error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "shortPasswordRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testShortPasswordShowsError(String fullName, String password, String confirmPassword) {
        log.info("Testing registration with password under 6 characters");
        registerPage.enterFullName(fullName);
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword(password);
        registerPage.enterConfirmPassword(confirmPassword);
        registerPage.clickSubmit();
        String error = registerPage.getPasswordError();
        assertFalse(error.isEmpty(), "Password error should be shown for short password");
    }

    // ✅ Mismatched passwords show confirm password error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "mismatchedPasswordsRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testMismatchedPasswordsShowsError(String fullName, String password, String confirmPassword) {
        log.info("Testing registration with mismatched passwords");
        registerPage.enterFullName(fullName);
        registerPage.enterEmail(uniqueEmail());
        registerPage.enterPassword(password);
        registerPage.enterConfirmPassword(confirmPassword);
        registerPage.clickSubmit();
        String error = registerPage.getConfirmPasswordError();
        assertFalse(error.isEmpty(), "Confirm password error should be shown when passwords don't match");
    }

    // ✅ Duplicate email — stays on register page
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "duplicateEmailRegistration", dataProviderClass = RegisterDataProvider.class)
    public void testDuplicateEmailStaysOnRegisterPage(String fullName, String password, String confirmPassword) {
        log.info("Testing registration with already-used email");
        // admin@amalitech.com already exists in the system
        registerPage.register(
                fullName,
                ConfigReader.get("admin.email"),
                password,
                confirmPassword
        );
        assertFalse(driver.getCurrentUrl().contains("/auth/login"),
                "Should not redirect to login when email is already registered");
    }

    // ✅ Login link navigates back to login page
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testLoginLinkNavigatesToLogin() {
        log.info("Testing login link on register page");
        registerPage.clickLoginLink();
        assertTrue(driver.getCurrentUrl().contains(ConfigReader.get("login.url")),
                "Should navigate to login page");
    }
}
