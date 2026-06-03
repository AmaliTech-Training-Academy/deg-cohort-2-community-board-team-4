package com.amalitech.qa.helpers;

import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class PageHelper {

    public WebElement find(WebElement element) {
        return DriverManager.getWait().until(ExpectedConditions.visibilityOf(element));
    }

    public void click(WebElement element) {
        DriverManager.getWait().until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    public void type(WebElement element, String value) {
        WebElement target = DriverManager.getWait().until(ExpectedConditions.elementToBeClickable(element));
        target.clear();
        target.sendKeys(value);
    }

    public String getText(WebElement element) {
        return find(element).getText();
    }

    public boolean isVisible(WebElement element) {
        return DriverManager.getWait().until(ExpectedConditions.visibilityOf(element)).isDisplayed();
    }

    public void waitForUrlToContain(String fragment) {
        DriverManager.getWait().until(ExpectedConditions.urlContains(fragment));
    }
}
