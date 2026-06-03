package com.amalitech.qa.helpers;

import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

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

    // Use this for *ngIf elements that may not be in the DOM yet — polls until located
    public WebElement waitForElementLocated(By locator) {
        return DriverManager.getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // Safe presence check without throwing — returns true if element is in the DOM and visible
    public boolean isElementPresent(By locator) {
        try {
            List<WebElement> elements = DriverManager.getDriver().findElements(locator);
            return !elements.isEmpty() && elements.get(0).isDisplayed();
        } catch (StaleElementReferenceException e) {
            // element was removed from DOM between findElements and isDisplayed — treat as absent
            return false;
        }
    }

    // Wait for an element to have a specific class value
    public void waitForElementClass(By locator, String className) {
        DriverManager.getWait().until(driver -> {
            List<WebElement> els = driver.findElements(locator);
            return !els.isEmpty() && els.stream().anyMatch(e -> e.getAttribute("class").contains(className));
        });
    }

    public void waitForUrlToContain(String fragment) {
        DriverManager.getWait().until(ExpectedConditions.urlContains(fragment));
    }
}
