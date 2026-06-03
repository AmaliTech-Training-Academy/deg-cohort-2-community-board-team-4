package com.amalitech.qa.pages;

import com.amalitech.qa.helpers.PageHelper;
import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class DashboardPage {

    private final PageHelper helper;

    // Header
    @FindBy(css = "button[aria-label='Toggle Menu']")
    private WebElement hamburgerButton;

    @FindBy(css = "span.user-name")
    private WebElement userName;

    @FindBy(css = "span.user-email")
    private WebElement userEmail;

    @FindBy(xpath = "//button[contains(text(),'Log out')]")
    private WebElement logoutButton;

    // Search
    @FindBy(css = "input.search-input")
    private WebElement searchInput;

    @FindBy(css = "button.btn-search")
    private WebElement searchButton;

    // Create post
    @FindBy(xpath = "//button[contains(text(),'Create post')]")
    private WebElement createPostButton;

    // Category pills
    @FindBy(css = "button.pill-button")
    private List<WebElement> categoryPills;

    // Posts
    @FindBy(css = "article.post-card")
    private List<WebElement> postCards;

    @FindBy(css = "div.posts-list")
    private WebElement postsList;

    // Pagination
    @FindBy(xpath = "//button[contains(@class,'pagination-btn') and contains(text(),'Next')]")
    private WebElement nextPageButton;

    @FindBy(xpath = "//button[contains(@class,'pagination-btn') and contains(text(),'Previous')]")
    private WebElement prevPageButton;

    @FindBy(css = "button.pagination-num-btn.active")
    private WebElement activePageNumber;

    // Create post modal
    @FindBy(xpath = "//div[contains(@class,'modal-form')]//input[contains(@class,'form-input')]")
    private WebElement modalTitleInput;

    @FindBy(xpath = "//textarea[contains(@class,'form-textarea')]")
    private WebElement modalContentTextarea;

    @FindBy(css = "button.custom-select-trigger")
    private WebElement modalCategoryTrigger;

    @FindBy(xpath = "//button[contains(@class,'btn-modal-submit')]")
    private WebElement modalSubmitButton;

    @FindBy(xpath = "//button[contains(@class,'btn-modal-cancel')]")
    private WebElement modalCancelButton;

    @FindBy(xpath = "//button[contains(@class,'btn-close-modal')]")
    private WebElement modalCloseButton;

    @FindBy(css = "div.modal-overlay")
    private WebElement modalOverlay;

    public DashboardPage() {
        this.helper = new PageHelper();
        PageFactory.initElements(DriverManager.getDriver(), this);
    }

    public boolean isLoaded() {
        // wait for actual post cards to appear, not just the container
        helper.waitForElementLocated(By.cssSelector("article.post-card"));
        return !postCards.isEmpty();
    }

    public String getUserName() {
        return helper.getText(userName);
    }

    public String getUserEmail() {
        return helper.getText(userEmail);
    }

    public void logout() {
        helper.click(logoutButton);
        helper.waitForUrlToContain("/auth/login");
    }

    public void searchFor(String keyword) {
        helper.type(searchInput, keyword);
        helper.click(searchButton);
    }

    public void clearSearch() {
        helper.type(searchInput, "");
        helper.click(searchButton);
    }

    public int getPostCount() {
        return postCards.size();
    }

    public boolean hasNoPosts() {
        try {
            DriverManager.getWait().until(
                    ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("article.post-card"))
            );
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void clickCategoryPill(String categoryName) {
        categoryPills.stream()
                .filter(pill -> pill.getText().trim().equalsIgnoreCase(categoryName))
                .findFirst()
                .ifPresent(pill -> helper.click(pill));
        // wait for the specific pill to become active
        DriverManager.getWait().until(driver ->
                driver.findElements(By.cssSelector("button.pill-button.active")).stream()
                        .anyMatch(e -> e.getText().trim().equalsIgnoreCase(categoryName))
        );
        // wait for posts to refresh after Angular applies the filter
        helper.waitForElementLocated(By.cssSelector("article.post-card"));
    }

    public String getActiveCategoryPillText() {
        return DriverManager.getDriver()
                .findElements(By.cssSelector("button.pill-button")).stream()
                .filter(pill -> pill.getAttribute("class").contains("active"))
                .findFirst()
                .map(pill -> pill.getText().trim())
                .orElse("");
    }

    public void clickFirstPost() {
        helper.click(postCards.get(0));
    }

    public void openCreatePostModal() {
        helper.click(createPostButton);
        // use * not div — Angular renders modal-form on a <form> element
        helper.waitForElementLocated(
                By.xpath("//*[contains(@class,'modal-form')]//input[contains(@class,'form-input')]")
        );
    }

    public boolean isModalVisible() {
        return helper.isElementPresent(By.cssSelector("div.modal-overlay"));
    }

    public void fillCreatePostForm(String title, String content) {
        WebElement titleInput = helper.waitForElementLocated(
                By.xpath("//*[contains(@class,'modal-form')]//input[contains(@class,'form-input')]"));
        helper.type(titleInput, title);

        WebElement textarea = helper.waitForElementLocated(By.cssSelector("textarea.form-textarea"));
        helper.type(textarea, content);

        // open category dropdown and select first option
        WebElement categoryTrigger = helper.waitForElementLocated(By.cssSelector("button.custom-select-trigger"));
        helper.click(categoryTrigger);
        WebElement firstOption = helper.waitForElementLocated(By.cssSelector("button.custom-select-option"));
        helper.click(firstOption);
    }

    public void submitCreatePostForm() {
        helper.click(modalSubmitButton);
    }

    public void cancelCreatePostModal() {
        helper.click(modalCancelButton);
        // wait for Angular to remove the modal before returning
        DriverManager.getWait().until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.modal-overlay"))
        );
    }

    public void clickNextPage() {
        String currentPage = getActivePageNumber();
        helper.click(nextPageButton);
        // wait for Angular to update the active page button after the API call
        DriverManager.getWait().until(driver -> {
            List<WebElement> active = driver.findElements(By.cssSelector("button.pagination-num-btn.active"));
            return !active.isEmpty() && !active.get(0).getText().trim().equals(currentPage);
        });
    }

    public String getActivePageNumber() {
        return helper.getText(activePageNumber).trim();
    }

    public boolean isNextPageButtonDisabled() {
        return nextPageButton.getAttribute("disabled") != null;
    }

    public boolean isPrevPageButtonDisabled() {
        return prevPageButton.getAttribute("disabled") != null;
    }
}
