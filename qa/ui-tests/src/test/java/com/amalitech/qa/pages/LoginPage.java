package com.amalitech.qa.pages;

import com.amalitech.qa.helpers.PageHelper;
import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage {

    private final PageHelper helper;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "button[type='button'].toggle-password")
    private WebElement togglePasswordButton;

    // Anchored to stable input id — not fragile class-based nth-child
    @FindBy(xpath = "//input[@id='email']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement emailErrorMessage;

    @FindBy(xpath = "//input[@id='password']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement passwordErrorMessage;

    @FindBy(css = "a[href='/auth/register']")
    private WebElement registerLink;

    @FindBy(tagName = "h1")
    private WebElement pageHeading;

    public LoginPage() {
        this.helper = new PageHelper();
        PageFactory.initElements(DriverManager.getDriver(), this);
    }

    public boolean isLoaded() {
        return helper.isVisible(pageHeading);
    }

    public void enterEmail(String email) {
        helper.type(emailInput, email);
    }

    public void enterPassword(String password) {
        helper.type(passwordInput, password);
    }

    public void clickSubmit() {
        helper.click(submitButton);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSubmit();
    }

    public void togglePasswordVisibility() {
        helper.click(togglePasswordButton);
    }

    public String getEmailError() {
        return helper.getText(emailErrorMessage);
    }

    public String getPasswordError() {
        return helper.getText(passwordErrorMessage);
    }

    public void clickRegisterLink() {
        helper.click(registerLink);
        helper.waitForUrlToContain("/auth/register");
    }

    public void waitForRedirectToDashboard() {
        helper.waitForUrlToContain("/dashboard");
    }
}
