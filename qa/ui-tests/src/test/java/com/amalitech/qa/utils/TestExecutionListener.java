package com.amalitech.qa.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestExecutionListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        log.info("STARTED  : {}", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("PASSED   : {}", result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("FAILED   : {} — {}", result.getName(), result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("SKIPPED  : {}", result.getName());
    }
}
