package com.vaadin.starter.skeleton;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.vaadin.starter.skeleton.utils.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceIT {
    private static final Logger log = LoggerFactory.getLogger(PerformanceIT.class);
    /**
     * Run the test case in this many browsers in parallel. CAREFUL when increasing this value: read README.md for further details.
     */
    private static final int CONCURRENT_BROWSERS = 10;
    /**
     * Repeat a test case this many times in a browser.
     */
    private static final int TEST_REPEATS = 10;
    /**
     * Where the Vaadin app is running.
     */
    @NotNull
    private static final String URL = "http://localhost:8080";
    private static ParallelPlaywright executor;

    @BeforeAll
    public static void setupParallelPlaywright() {
        executor = new ParallelPlaywright(CONCURRENT_BROWSERS, URL);
        executor.initialize();
    }

    @AfterAll
    public static void shutdownExecutor() throws Exception {
        executor.close();
    }

    @Test
    public void testImplementation() {
        final MeasureTime testStats = new MeasureTime("Detailed Test Stats");
        executor.runInAllBrowsersAndWait(page -> testRun(page, testStats));
        log.info(testStats.format());
    }

    private void testRun(@NotNull Page page, @NotNull MeasureTime testStats) {
        for (int i = 0; i < TEST_REPEATS; i++) {
            Locator nameField = page.locator("vaadin-text-field#nameField input");
            testStats.log("TextField lookup");
            nameField.fill("Martin");
            testStats.log("Fill TextField");
            Locator button = page.locator("vaadin-button#sayHelloButton");
            testStats.log("Button lookup");
            button.click();
            testStats.log("Button click");
            Locator card =
                    page.locator("vaadin-notification-container > vaadin-notification-card").first();
            testStats.log("Card lookup");
            Assertions.assertEquals("Hello Martin", card.textContent());
            testStats.log("Text content retrieval");
            Utils.sleep(1000L);
            testStats.log("Sleep");
        }
    }
}
