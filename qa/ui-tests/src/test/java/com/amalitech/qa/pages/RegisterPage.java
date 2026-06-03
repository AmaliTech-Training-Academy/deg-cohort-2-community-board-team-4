package com.amalitech.qa.pages;

import com.amalitech.qa.helpers.PageHelper;
import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class RegisterPage {

    private final PageHelper helper;

    @FindBy(id = "fullName")
    private WebElement fullNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPasswordInput;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "a[href='/auth/login']")
    private WebElement loginLink;

    @FindBy(tagName = "h1")
    private WebElement pageHeading;

    // Anchored to stable input id — survives CSS class renames
    @FindBy(xpath = "//input[@id='fullName']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement fullNameError;

    @FindBy(xpath = "//input[@id='email']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement emailError;

    @FindBy(xpath = "//input[@id='password']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement passwordError;

    @FindBy(xpath = "//input[@id='confirmPassword']/ancestor::div[contains(@class,'form-group')]//div[contains(@class,'error-message')]")
    private WebElement confirmPasswordError;

    public RegisterPage() {
        this.helper = new PageHelper();
        PageFactory.initElements(DriverManager.getDriver(), this);
    }

    public boolean isLoaded() {
        return helper.isVisible(pageHeading);
    }

    public void enterFullName(String name) {
        helper.type(fullNameInput, name);
    }

    public void enterEmail(String email) {
        helper.type(emailInput, email);
    }

    public void enterPassword(String password) {
        helper.type(passwordInput, password);
    }

    public void enterConfirmPassword(String password) {
        helper.type(confirmPasswordInput, password);
    }

    public void clickSubmit() {
        helper.click(submitButton);
    }

    public void register(String fullName, String email, String password, String confirmPassword) {
        enterFullName(fullName);
        enterEmail(email);
        enterPassword(password);
        enterConfirmPassword(confirmPassword);
        clickSubmit();
    }

    public void clickLoginLink() {
        helper.click(loginLink);
        helper.waitForUrlToContain("/auth/login");
    }

    public String getFullNameError() {
        return helper.getText(fullNameError);
    }

    public String getEmailError() {
        return helper.getText(emailError);
    }

    public String getPasswordError() {
        return helper.getText(passwordError);
    }

    public String getConfirmPasswordError() {
        return helper.getText(confirmPasswordError);
    }

    public void waitForRedirectAfterRegistration() {
        helper.waitForUrlToContain("/dashboard");
    }
}
