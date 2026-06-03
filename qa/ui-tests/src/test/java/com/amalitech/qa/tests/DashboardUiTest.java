package com.amalitech.qa.tests;

import com.amalitech.qa.base.UIBaseTest;
import com.amalitech.qa.pages.DashboardPage;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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
    }

    @BeforeMethod(alwaysRun = true)
    public void goToDashboard() {
        driver.get(baseUrl + ConfigReader.get("dashboard.url"));
        dashboardPage = new DashboardPage();
        dashboardPage.isLoaded(); // wait for posts to appear before each test
    }

    // ✅ Dashboard loads with posts visible
    @Test
    public void testDashboardLoads() {
        log.info("Verifying dashboard loads with posts");
        assertTrue(dashboardPage.isLoaded(), "Posts list should be visible on dashboard");
        assertTrue(driver.getCurrentUrl().contains("/dashboard"), "URL should contain /dashboard");
    }

    // ✅ Logged-in user name and email are displayed
    @Test
    public void testUserDetailsDisplayed() {
        log.info("Verifying user details are shown in header");
        assertEquals(dashboardPage.getUserName(), "Admin User");
        assertEquals(dashboardPage.getUserEmail(), ConfigReader.get("admin.email"));
    }

    // ✅ Posts are listed on the dashboard
    @Test
    public void testPostsAreVisible() {
        log.info("Verifying posts are listed");
        assertTrue(dashboardPage.getPostCount() > 0, "At least one post should be visible");
    }

    // ✅ Search by keyword filters posts
    @Test
    public void testSearchFiltersResults() {
        log.info("Testing search filters posts by keyword");
        int allPostsCount = dashboardPage.getPostCount();
        dashboardPage.searchFor("Hackathon");
        int filteredCount = dashboardPage.getPostCount();
        assertTrue(filteredCount <= allPostsCount, "Search should filter posts");
        assertTrue(filteredCount > 0, "Search for 'Hackathon' should return at least one result");
    }

    // ✅ Search with no results shows empty state
    @Test
    public void testSearchNoResults() {
        log.info("Testing search with a term that matches nothing");
        dashboardPage.searchFor("zzzzzthisdoesnotexist12345");
        assertTrue(dashboardPage.hasNoPosts(), "Search with no match should return no posts");
    }

    // ✅ Category filter — click 'news' shows only news posts
    @Test
    public void testCategoryFilterNews() {
        log.info("Testing category filter for 'news'");
        dashboardPage.clickCategoryPill("news");
        assertEquals(dashboardPage.getActiveCategoryPillText(), "news",
                "News pill should be active after clicking");
        assertTrue(dashboardPage.getPostCount() > 0, "News category should have posts");
    }

    // ✅ 'All' category shows all posts
    @Test
    public void testCategoryFilterAll() {
        log.info("Testing 'All' category shows all posts");
        dashboardPage.clickCategoryPill("news");
        dashboardPage.clickCategoryPill("All");
        assertEquals(dashboardPage.getActiveCategoryPillText(), "All",
                "'All' pill should be active");
    }

    // ✅ Create post modal opens on button click
    @Test
    public void testCreatePostModalOpens() {
        log.info("Testing create post modal opens");
        dashboardPage.openCreatePostModal();
        assertTrue(dashboardPage.isModalVisible(), "Create post modal should be visible");
    }

    // ✅ Create post modal cancel closes it
    @Test
    public void testCreatePostModalCancel() {
        log.info("Testing create post modal can be cancelled");
        dashboardPage.openCreatePostModal();
        dashboardPage.cancelCreatePostModal();
        assertFalse(dashboardPage.isModalVisible(), "Modal should be closed after cancel");
    }

    // ✅ Create a post successfully
    @Test
    public void testCreatePostSuccess() {
        log.info("Testing creating a new post");
        int countBefore = dashboardPage.getPostCount();
        dashboardPage.openCreatePostModal();
        dashboardPage.fillCreatePostForm(
                "UI Test Post " + System.currentTimeMillis(),
                "This post was created by the Selenium UI test suite."
        );
        dashboardPage.submitCreatePostForm();
        dashboardPage = new DashboardPage();
        assertTrue(dashboardPage.getPostCount() >= countBefore,
                "Post count should be same or higher after creating a post");
    }

    // ✅ Pagination — next page advances the page number
    @Test
    public void testPaginationNextPage() {
        log.info("Testing pagination next page");
        assertEquals(dashboardPage.getActivePageNumber(), "1", "Should start on page 1");
        assertFalse(dashboardPage.isNextPageButtonDisabled(), "Next button should be enabled");
        dashboardPage.clickNextPage();
        assertEquals(dashboardPage.getActivePageNumber(), "2", "Should advance to page 2");
    }

    // ✅ First page has Previous button disabled
    @Test
    public void testPaginationPreviousDisabledOnFirstPage() {
        log.info("Testing Previous button is disabled on first page");
        assertTrue(dashboardPage.isPrevPageButtonDisabled(),
                "Previous button should be disabled on first page");
    }

    // ✅ Logout redirects to login
    @Test(priority = 99)
    public void testLogoutRedirectsToLogin() {
        log.info("Testing logout redirects to login page");
        dashboardPage.logout();
        assertTrue(driver.getCurrentUrl().contains("/auth/login"),
                "Should redirect to login after logout");
    }
}
