package com.amalitech.qa.pages;

import com.amalitech.qa.helpers.PageHelper;
import com.amalitech.qa.utils.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class PostDetailPage {

    private final PageHelper helper;

    // Breadcrumb
    @FindBy(css = "a.breadcrumb-item[href='/dashboard']")
    private WebElement breadcrumbHomeLink;

    @FindBy(css = "span.breadcrumb-current")
    private WebElement breadcrumbCurrent;

    // Post content
    @FindBy(css = "h1.detail-title")
    private WebElement postTitle;

    @FindBy(css = "span.category-badge")
    private WebElement categoryBadge;

    @FindBy(css = "div.detail-body p")
    private WebElement postBody;

    @FindBy(css = "span.author-name")
    private WebElement authorName;

    // Comment form — scoped to the add-comment section to avoid selecting the edit-mode textarea
    @FindBy(css = "div.add-comment-section textarea")
    private WebElement commentTextarea;

    @FindBy(css = "button.btn-submit-comment")
    private WebElement submitCommentButton;

    // Comments list
    @FindBy(css = "h2.comments-title")
    private WebElement commentsTitle;

    @FindBy(css = "div.comment-item")
    private List<WebElement> commentItems;

    // Delete confirmation modal
    @FindBy(css = "button.btn-modal-cancel")
    private WebElement modalCancelButton;

    @FindBy(css = "button.btn-modal-delete")
    private WebElement modalDeleteButton;

    public PostDetailPage() {
        this.helper = new PageHelper();
        PageFactory.initElements(DriverManager.getDriver(), this);
    }

    public boolean isLoaded() {
        helper.waitForElementLocated(By.cssSelector("h1.detail-title"));
        return postTitle.isDisplayed();
    }

    // --- Post content ---

    public String getPostTitle() {
        return helper.getText(postTitle).trim();
    }

    public boolean isCategoryBadgeVisible() {
        return helper.isVisible(categoryBadge);
    }

    public String getPostBody() {
        return helper.getText(postBody).trim();
    }

    public String getAuthorName() {
        return helper.getText(authorName).trim();
    }

    // --- Breadcrumb ---

    public String getBreadcrumbCurrentText() {
        return helper.getText(breadcrumbCurrent).trim();
    }

    public void clickBreadcrumbHome() {
        helper.click(breadcrumbHomeLink);
        helper.waitForUrlToContain("/dashboard");
    }

    // --- Comment form ---

    public boolean isCommentSubmitDisabled() {
        return submitCommentButton.getAttribute("disabled") != null;
    }

    public void typeComment(String text) {
        helper.type(commentTextarea, text);
    }

    public void submitComment() {
        int countBefore = DriverManager.getDriver().findElements(By.cssSelector("div.comment-item")).size();
        helper.click(submitCommentButton);
        // wait for Angular to append the new comment to the list
        DriverManager.getWait().until(driver ->
                driver.findElements(By.cssSelector("div.comment-item")).size() > countBefore
        );
    }

    // --- Comments list ---

    public int getCommentCount() {
        return commentItems.size();
    }

    public String getCommentsTitle() {
        return helper.getText(commentsTitle).trim();
    }

    public String getFirstCommentText() {
        WebElement body = helper.waitForElementLocated(By.cssSelector("div.comment-body p"));
        return body.getText().trim();
    }

    public String getLastCommentText() {
        List<WebElement> bodies = DriverManager.getDriver().findElements(By.cssSelector("div.comment-body p"));
        return bodies.isEmpty() ? "" : bodies.get(bodies.size() - 1).getText().trim();
    }

    // --- Edit comment ---

    public void clickEditOnFirstComment() {
        WebElement editBtn = helper.waitForElementLocated(By.cssSelector("button[aria-label='Edit Comment']"));
        helper.click(editBtn);
        helper.waitForElementLocated(By.cssSelector("textarea.comment-textarea.edit-mode"));
    }

    public void typeInEditTextarea(String text) {
        WebElement editTextarea = helper.waitForElementLocated(By.cssSelector("textarea.comment-textarea.edit-mode"));
        editTextarea.clear();
        editTextarea.sendKeys(text);
    }

    public void clickCancelEdit() {
        WebElement cancelBtn = helper.waitForElementLocated(By.cssSelector("button.btn-edit-cancel"));
        helper.click(cancelBtn);
        DriverManager.getWait().until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("textarea.comment-textarea.edit-mode"))
        );
    }

    public void clickSaveEdit() {
        WebElement saveBtn = helper.waitForElementLocated(By.cssSelector("button.btn-edit-save"));
        helper.click(saveBtn);
        DriverManager.getWait().until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("textarea.comment-textarea.edit-mode"))
        );
    }

    public boolean isEditEditorVisible() {
        return helper.isElementPresent(By.cssSelector("textarea.comment-textarea.edit-mode"));
    }

    // --- Delete comment ---

    public void clickDeleteOnLastComment() {
        List<WebElement> deleteButtons = DriverManager.getDriver()
                .findElements(By.cssSelector("button[aria-label='Delete Comment']"));
        helper.click(deleteButtons.get(deleteButtons.size() - 1));
        helper.waitForElementLocated(By.cssSelector("div.modal-overlay"));
    }

    public boolean isDeleteModalVisible() {
        return helper.isElementPresent(By.cssSelector("div.modal-overlay"));
    }

    public void cancelDeleteComment() {
        helper.click(modalCancelButton);
        DriverManager.getWait().until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.modal-overlay"))
        );
    }

    public void confirmDeleteComment() {
        int countBefore = DriverManager.getDriver().findElements(By.cssSelector("div.comment-item")).size();
        helper.click(modalDeleteButton);
        // wait for Angular to remove the comment and close the modal
        DriverManager.getWait().until(driver ->
                driver.findElements(By.cssSelector("div.comment-item")).size() < countBefore
        );
    }
}
