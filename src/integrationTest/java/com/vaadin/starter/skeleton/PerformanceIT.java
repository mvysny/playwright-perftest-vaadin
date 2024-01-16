package com.vaadin.starter.skeleton;

import com.microsoft.playwright.Locator;
import com.vaadin.starter.skeleton.utils.BetterExecutor;
import com.vaadin.starter.skeleton.utils.MeasureTime;
import com.vaadin.starter.skeleton.utils.PlaywrightUtils;
import com.vaadin.starter.skeleton.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class PerformanceIT {
    /**
     * Run the test case in this many browsers in parallel. CAREFUL when increasing this value.
     */
    private static final int CONCURRENT_BROWSERS = 2;
    /**
     * Repeat a test case this many times in a browser.
     */
    private static final int TEST_REPEATS = 10;
    /**
     * Where the Vaadin app is running.
     */
    @NotNull
    private static final String URL = "http://localhost:8080";
    private static BetterExecutor executor;

    @BeforeAll
    public static void setupExecutor() {
        executor = new BetterExecutor(Executors.newFixedThreadPool(CONCURRENT_BROWSERS));
    }
    @AfterAll
    public static void shutdownExecutor() throws Exception {
        executor.close();
    }

    @BeforeAll
    public static void warmupPlaywright() throws Exception {
        final MeasureTime mt = new MeasureTime("Warmup");
        PlaywrightUtils.withPlaywrightPage(mt, URL, (page) -> {});
        System.out.println(mt.format());
    }

    @Test
    public void testImplementation() throws Exception {
        final MeasureTime overall = new MeasureTime("Overall");
        final MeasureTime testStats = new MeasureTime("TestStats");
        final List<Runnable> jobs = IntStream.range(0, CONCURRENT_BROWSERS).<Runnable>mapToObj(it -> () -> testRun(overall, testStats)).toList();
        executor.submitAllAndWait(jobs);
        System.out.println(overall.format());
        System.out.println(testStats.format());
    }

    private void testRun(@NotNull MeasureTime overall, @NotNull MeasureTime testStats) {
        PlaywrightUtils.withPlaywrightPage(overall, URL, (page) -> {
            testStats.log("waiting");
            for (int i = 0; i < TEST_REPEATS; i++) {
                Locator nameField = page.locator("vaadin-text-field#nameField input");
                testStats.log("TextField lookup");
                nameField.fill("Martin");
                testStats.log("Fill");
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
        });
    }
}
