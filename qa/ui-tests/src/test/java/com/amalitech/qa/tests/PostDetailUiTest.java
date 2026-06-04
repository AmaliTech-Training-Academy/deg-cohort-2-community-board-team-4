package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.DashboardPage;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.pages.PostDetailPage;
import com.amalitech.qa.providers.PostDetailDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import com.amalitech.qa.utils.JsonDataReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Feature("Post Detail")
public class PostDetailUiTest extends UIBaseTest {

    private static final Logger log = LoggerFactory.getLogger(PostDetailUiTest.class);

    private PostDetailPage postDetailPage;
    private String postDetailUrl;

    @BeforeClass(alwaysRun = true)
    public void loginAndSetup() {
        clearSession();
        driver.get(baseUrl + ConfigReader.get("login.url"));
        LoginPage loginPage = new LoginPage();
        loginPage.login(ConfigReader.get("admin.email"), ConfigReader.get("admin.password"));
        loginPage.waitForRedirectToDashboard();

        // Navigate from dashboard (Option A) and capture the URL so every
        // @BeforeMethod goes to the same post regardless of dashboard sort order
        driver.get(baseUrl + ConfigReader.get("dashboard.url"));
        DashboardPage dashboardPage = new DashboardPage();
        dashboardPage.isLoaded();
        dashboardPage.clickFirstPost();

        // Capture URL after the page has fully loaded — Angular updates the URL
        // asynchronously, so waiting for the page element guarantees the URL is final
        PostDetailPage setup = new PostDetailPage();
        setup.isLoaded();
        postDetailUrl = driver.getCurrentUrl();
        if (setup.getCommentCount() == 0) {
            setup.typeComment(JsonDataReader.getString("testdata/postdetail-data.json", "baselineComment"));
            setup.submitComment();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void goToPostDetail() {
        driver.get(postDetailUrl);
        postDetailPage = new PostDetailPage();
        postDetailPage.isLoaded();
        // re-seed a comment if all were deleted during a previous test or retry
        if (postDetailPage.getCommentCount() == 0) {
            postDetailPage.typeComment(JsonDataReader.getString("testdata/postdetail-data.json", "baselineComment"));
            postDetailPage.submitComment();
        }
    }

    // ✅ Post detail page loads with all content visible
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void testPostDetailLoads() {
        log.info("Verifying post detail page loads with content");
        assertFalse(postDetailPage.getPostTitle().isEmpty(), "Post title should be visible");
        assertTrue(postDetailPage.isCategoryBadgeVisible(), "Category badge should be visible");
        assertFalse(postDetailPage.getPostBody().isEmpty(), "Post body should be visible");
        assertFalse(postDetailPage.getAuthorName().isEmpty(), "Author name should be visible");
        assertTrue(driver.getCurrentUrl().contains("/dashboard/posts/"), "URL should contain /dashboard/posts/");
    }

    // ✅ Breadcrumb shows "Post Details" as the current page
    @Severity(SeverityLevel.MINOR)
    @Test(dataProvider = "breadcrumbText", dataProviderClass = PostDetailDataProvider.class)
    public void testBreadcrumbShowsPostDetails(String expectedText) {
        log.info("Verifying breadcrumb shows 'Post Details'");
        assertEquals(postDetailPage.getBreadcrumbCurrentText(), expectedText,
                "Breadcrumb current item should read 'Post Details'");
    }

    // ✅ Home breadcrumb navigates back to the dashboard
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testBreadcrumbHomeNavigatesToDashboard() {
        log.info("Verifying home breadcrumb navigates to dashboard");
        postDetailPage.clickBreadcrumbHome();
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Clicking Home breadcrumb should navigate to /dashboard");
    }

    // ✅ Submit button is disabled when the comment textarea is empty
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testCommentSubmitDisabledWhenEmpty() {
        log.info("Verifying submit is disabled with empty comment textarea");
        assertTrue(postDetailPage.isCommentSubmitDisabled(),
                "Submit button should be disabled when the comment textarea is empty");
    }

    // ✅ Submit button becomes enabled after typing a comment
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "testComment", dataProviderClass = PostDetailDataProvider.class)
    public void testCommentSubmitEnabledWhenFilled(String comment) {
        log.info("Verifying submit enables after typing a comment");
        postDetailPage.typeComment(comment);
        assertFalse(postDetailPage.isCommentSubmitDisabled(),
                "Submit button should be enabled after typing in the textarea");
    }

    // ✅ Submitting a comment increases the comment count and shows the new comment
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "addCommentPrefix", dataProviderClass = PostDetailDataProvider.class)
    public void testAddComment(String commentPrefix) {
        log.info("Testing that submitting a comment adds it to the list");
        int countBefore = postDetailPage.getCommentCount();
        String commentText = commentPrefix + System.currentTimeMillis();
        postDetailPage.typeComment(commentText);
        postDetailPage.submitComment();
        assertTrue(postDetailPage.getCommentCount() > countBefore,
                "Comment count should increase after submitting");
        assertEquals(postDetailPage.getLastCommentText(), commentText,
                "The submitted comment should appear at the bottom of the list");
    }

    // ✅ Clicking the edit icon shows the inline editor with the comment's current text
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testEditCommentShowsEditor() {
        log.info("Verifying clicking edit shows the inline comment editor");
        postDetailPage.clickEditOnFirstComment();
        assertTrue(postDetailPage.isEditEditorVisible(),
                "Inline edit textarea should be visible after clicking edit");
    }

    // ✅ Clicking cancel on the inline editor hides it without changes
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testEditCommentCancel() {
        log.info("Verifying cancel closes the inline edit editor");
        postDetailPage.clickEditOnFirstComment();
        postDetailPage.clickCancelEdit();
        assertFalse(postDetailPage.isEditEditorVisible(),
                "Inline edit textarea should be hidden after clicking cancel");
    }

    // ✅ Editing and saving a comment updates the displayed text
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "editCommentPrefix", dataProviderClass = PostDetailDataProvider.class)
    public void testEditCommentSave(String editPrefix) {
        log.info("Verifying edit and save updates the comment text");
        String updatedText = editPrefix + System.currentTimeMillis();
        postDetailPage.clickEditOnFirstComment();
        postDetailPage.typeInEditTextarea(updatedText);
        postDetailPage.clickSaveEdit();
        assertFalse(postDetailPage.isEditEditorVisible(),
                "Inline editor should be hidden after saving");
        assertEquals(postDetailPage.getFirstCommentText(), updatedText,
                "First comment body should reflect the saved edit");
    }

    // ✅ Clicking delete shows the confirmation modal
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testDeleteCommentModalAppears() {
        log.info("Verifying delete button opens the confirmation modal");
        postDetailPage.clickDeleteOnLastComment();
        assertTrue(postDetailPage.isDeleteModalVisible(),
                "Delete confirmation modal should appear after clicking delete");
        postDetailPage.cancelDeleteComment();
    }

    // ✅ Cancelling the delete modal leaves the comment intact
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testDeleteCommentModalCancel() {
        log.info("Verifying cancel in delete modal keeps the comment count unchanged");
        int countBefore = postDetailPage.getCommentCount();
        postDetailPage.clickDeleteOnLastComment();
        postDetailPage.cancelDeleteComment();
        assertEquals(postDetailPage.getCommentCount(), countBefore,
                "Comment count should be unchanged after cancelling delete");
    }

    // ✅ Confirming delete removes the comment from the list (runs last)
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 99, dataProvider = "deleteCommentPrefix", dataProviderClass = PostDetailDataProvider.class)
    public void testDeleteCommentConfirm(String commentPrefix) {
        log.info("Verifying confirming delete removes the comment");
        String tempComment = commentPrefix + System.currentTimeMillis();
        postDetailPage.typeComment(tempComment);
        postDetailPage.submitComment();
        int countAfterAdd = postDetailPage.getCommentCount();
        postDetailPage.clickDeleteOnLastComment();
        postDetailPage.confirmDeleteComment();
        assertEquals(postDetailPage.getCommentCount(), countAfterAdd - 1,
                "Comment count should decrease by one after confirming delete");
    }
}
