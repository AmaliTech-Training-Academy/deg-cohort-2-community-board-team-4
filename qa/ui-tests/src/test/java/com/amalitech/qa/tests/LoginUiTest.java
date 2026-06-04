package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.providers.LoginDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Feature("Login")
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
    @Severity(SeverityLevel.MINOR)
    @Test(dataProvider = "loginPageTitle", dataProviderClass = LoginDataProvider.class)
    public void testLoginPageLoads(String expectedTitle) {
        log.info("Verifying login page loads correctly");
        assertTrue(loginPage.isLoaded(), "Login page heading should be visible");
        assertEquals(driver.getTitle(), expectedTitle);
    }

    // ✅ Successful login redirects to dashboard
    @Severity(SeverityLevel.BLOCKER)
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
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "invalidEmailLogin", dataProviderClass = LoginDataProvider.class)
    public void testInvalidEmailShowsError(String email, String password) {
        log.info("Testing login with invalid email format");
        loginPage.enterEmail(email);
        loginPage.enterPassword(password);
        loginPage.clickSubmit();
        String error = loginPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error message should be visible");
    }

    // ✅ Empty email shows email required error
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "emptyEmailLogin", dataProviderClass = LoginDataProvider.class)
    public void testEmptyEmailShowsError(String password) {
        log.info("Testing login with empty email");
        loginPage.enterPassword(password);
        loginPage.clickSubmit();
        String error = loginPage.getEmailError();
        assertFalse(error.isEmpty(), "Email error message should be visible for empty email");
    }

    // ✅ Empty password shows password required error
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testEmptyPasswordShowsError() {
        log.info("Testing login with empty password");
        loginPage.enterEmail(ConfigReader.get("admin.email"));
        loginPage.clickSubmit();
        String error = loginPage.getPasswordError();
        assertFalse(error.isEmpty(), "Password error message should be visible for empty password");
    }

    // ✅ Wrong credentials — stays on login page
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "wrongCredentialsLogin", dataProviderClass = LoginDataProvider.class)
    public void testWrongCredentialsStaysOnLoginPage(String email, String password) {
        log.info("Testing login with wrong credentials");
        loginPage.login(email, password);
        assertFalse(driver.getCurrentUrl().contains("/dashboard"),
                "Should not redirect to dashboard with wrong credentials");
    }

    // ✅ Register link navigates to register page
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testRegisterLinkNavigatesToRegister() {
        log.info("Testing register link on login page");
        loginPage.clickRegisterLink();
        assertTrue(driver.getCurrentUrl().contains(ConfigReader.get("register.url")),
                "Should navigate to register page");
    }
}
