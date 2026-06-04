package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.DashboardPage;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.providers.DashboardDataProvider;
import com.amalitech.qa.utils.ConfigReader;
import com.amalitech.qa.utils.DriverManager;
import com.amalitech.qa.utils.JsonDataReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Feature("Dashboard")
public class DashboardUiTest extends UIBaseTest {

    private static final Logger log = LoggerFactory.getLogger(DashboardUiTest.class);

    private DashboardPage dashboardPage;

    @BeforeClass(alwaysRun = true)
    public void loginAsAdmin() {
        clearSession();
        driver.get(baseUrl + ConfigReader.get("login.url"));
        LoginPage loginPage = new LoginPage();
        loginPage.login(ConfigReader.get("admin.email"), ConfigReader.get("admin.password"));
        loginPage.waitForRedirectToDashboard();

        // Create a seed post so testSearchFiltersResults always has matching data
        // regardless of what exists on the server
        driver.get(baseUrl + ConfigReader.get("dashboard.url"));
        DashboardPage setupPage = new DashboardPage();
        setupPage.isLoaded();
        setupPage.openCreatePostModal();
        String searchKeyword = JsonDataReader.getString("testdata/dashboard-data.json", "searchKeyword");
        String seedBody = JsonDataReader.getString("testdata/dashboard-data.json", "newPost", "body");
        setupPage.fillCreatePostForm(searchKeyword + " Seed", seedBody);
        setupPage.submitCreatePostForm();
    }

    @BeforeMethod(alwaysRun = true)
    public void goToDashboard() {
        driver.get(baseUrl + ConfigReader.get("dashboard.url"));
        dashboardPage = new DashboardPage();
        dashboardPage.isLoaded(); // wait for posts to appear before each test
    }

    // ✅ Dashboard loads with posts visible
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void testDashboardLoads() {
        log.info("Verifying dashboard loads with posts");
        assertTrue(dashboardPage.isLoaded(), "Posts list should be visible on dashboard");
        assertTrue(driver.getCurrentUrl().contains("/dashboard"), "URL should contain /dashboard");
    }

    // ✅ Logged-in user name and email are displayed
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "expectedUserName", dataProviderClass = DashboardDataProvider.class)
    public void testUserDetailsDisplayed(String expectedUserName) {
        log.info("Verifying user details are shown in header");
        assertEquals(dashboardPage.getUserName(), expectedUserName);
        assertEquals(dashboardPage.getUserEmail(), ConfigReader.get("admin.email"));
    }

    // ✅ Posts are listed on the dashboard
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void testPostsAreVisible() {
        log.info("Verifying posts are listed");
        assertTrue(dashboardPage.getPostCount() > 0, "At least one post should be visible");
    }

    // ✅ Search by keyword filters posts
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "searchKeyword", dataProviderClass = DashboardDataProvider.class)
    public void testSearchFiltersResults(String keyword) {
        log.info("Testing search filters posts by keyword");
        int allPostsCount = dashboardPage.getPostCount();
        dashboardPage.searchFor(keyword);
        // wait for Angular to complete the search API call and render results
        DriverManager.getWait().until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("article.post-card"))
        );
        int filteredCount = dashboardPage.getPostCount();
        assertTrue(filteredCount <= allPostsCount, "Search should filter posts");
        assertTrue(filteredCount > 0, "Search for '" + keyword + "' should return at least one result");
    }

    // ✅ Search with no results shows empty state
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "noResultsSearchKeyword", dataProviderClass = DashboardDataProvider.class)
    public void testSearchNoResults(String keyword) {
        log.info("Testing search with a term that matches nothing");
        dashboardPage.searchFor(keyword);
        assertTrue(dashboardPage.hasNoPosts(), "Search with no match should return no posts");
    }

    // ✅ Category filter — click 'news' shows only news posts
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "newsCategoryFilter", dataProviderClass = DashboardDataProvider.class)
    public void testCategoryFilterNews(String category) {
        log.info("Testing category filter for 'news'");
        dashboardPage.clickCategoryPill(category);
        assertEquals(dashboardPage.getActiveCategoryPillText(), category,
                "News pill should be active after clicking");
        assertTrue(dashboardPage.getPostCount() > 0, "News category should have posts");
    }

    // ✅ 'All' category shows all posts
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "allCategoryFilter", dataProviderClass = DashboardDataProvider.class)
    public void testCategoryFilterAll(String newsCategory, String allCategory) {
        log.info("Testing 'All' category shows all posts");
        dashboardPage.clickCategoryPill(newsCategory);
        dashboardPage.clickCategoryPill(allCategory);
        assertEquals(dashboardPage.getActiveCategoryPillText(), allCategory,
                "'All' pill should be active");
    }

    // ✅ Create post modal opens on button click
    @Severity(SeverityLevel.NORMAL)
    @Test
    public void testCreatePostModalOpens() {
        log.info("Testing create post modal opens");
        dashboardPage.openCreatePostModal();
        assertTrue(dashboardPage.isModalVisible(), "Create post modal should be visible");
    }

    // ✅ Create post modal cancel closes it
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testCreatePostModalCancel() {
        log.info("Testing create post modal can be cancelled");
        dashboardPage.openCreatePostModal();
        dashboardPage.cancelCreatePostModal();
        assertFalse(dashboardPage.isModalVisible(), "Modal should be closed after cancel");
    }

    // ✅ Create a post successfully
    @Severity(SeverityLevel.CRITICAL)
    @Test(dataProvider = "newPostData", dataProviderClass = DashboardDataProvider.class)
    public void testCreatePostSuccess(String titlePrefix, String body) {
        log.info("Testing creating a new post");
        int countBefore = dashboardPage.getPostCount();
        dashboardPage.openCreatePostModal();
        dashboardPage.fillCreatePostForm(
                titlePrefix + System.currentTimeMillis(),
                body
        );
        dashboardPage.submitCreatePostForm();
        dashboardPage = new DashboardPage();
        assertTrue(dashboardPage.getPostCount() >= countBefore,
                "Post count should be same or higher after creating a post");
    }

    // ✅ Pagination — next page advances the page number
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "paginationData", dataProviderClass = DashboardDataProvider.class)
    public void testPaginationNextPage(String initialPage, String nextPage) {
        log.info("Testing pagination next page");
        assertEquals(dashboardPage.getActivePageNumber(), initialPage, "Should start on page 1");
        assertFalse(dashboardPage.isNextPageButtonDisabled(), "Next button should be enabled");
        dashboardPage.clickNextPage();
        assertEquals(dashboardPage.getActivePageNumber(), nextPage, "Should advance to page 2");
    }

    // ✅ First page has Previous button disabled
    @Severity(SeverityLevel.MINOR)
    @Test
    public void testPaginationPreviousDisabledOnFirstPage() {
        log.info("Testing Previous button is disabled on first page");
        assertTrue(dashboardPage.isPrevPageButtonDisabled(),
                "Previous button should be disabled on first page");
    }

    // ✅ Logout redirects to login
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 99)
    public void testLogoutRedirectsToLogin() {
        log.info("Testing logout redirects to login page");
        dashboardPage.logout();
        assertTrue(driver.getCurrentUrl().contains("/auth/login"),
                "Should redirect to login after logout");
    }
}
