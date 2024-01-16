package com.vaadin.starter.skeleton;

import com.microsoft.playwright.Locator;
import com.vaadin.starter.skeleton.utils.BetterExecutor;
import com.vaadin.starter.skeleton.utils.MeasureTime;
import com.vaadin.starter.skeleton.utils.PlaywrightUtils;
import com.vaadin.starter.skeleton.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

public class PerformanceIT {
    /**
     * Run the test case in this many browsers in parallel. CAREFUL when increasing this value.
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
        PlaywrightUtils.warmupPlaywright(URL);
    }

    private Semaphore playwrightInitSemaphore;

    @BeforeEach
    public void initSemaphore() {
        final int permits = Runtime.getRuntime().availableProcessors() / 2;
        final int rate = permits < 1 ? 1 : permits;
        playwrightInitSemaphore = new Semaphore(rate);
        System.out.println("Playwright initialization rate limited to " + rate + " concurrent inits");
    }

    @Test
    public void testImplementation() throws Exception {
        final MeasureTime overall = new MeasureTime("Overall");
        final MeasureTime testStats = new MeasureTime("TestStats");
        final CountDownLatch commenceLatch = new CountDownLatch(CONCURRENT_BROWSERS);
        final List<Runnable> jobs = IntStream.range(0, CONCURRENT_BROWSERS).<Runnable>mapToObj(it -> () -> testRun(it == 0, overall, testStats, commenceLatch)).toList();
        executor.submitAllAndWait(jobs);
        System.out.println(overall.format());
        System.out.println(testStats.format());
    }

    private void testRun(boolean log, @NotNull MeasureTime overall, @NotNull MeasureTime testStats, @NotNull CountDownLatch commenceLatch) {
        PlaywrightUtils.withPlaywrightPage(overall, URL, playwrightInitSemaphore, commenceLatch, (page) -> {
            if (log) {
                System.out.println("Playwright is fully initialized in all threads, tests commencing");
            }
            testStats.log("waiting for playwright init");
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
