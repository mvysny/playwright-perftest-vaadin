package com.vaadin.starter.skeleton.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class PlaywrightUtils {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightUtils.class);

    public static void warmupPlaywright(@NotNull String url) {
        final MeasureTime mt = new MeasureTime("Playwright Warmup");
        withPlaywrightPage(mt, url, new Semaphore(1), new CountDownLatch(1), (page) -> {});
        log.info(mt.format());
    }

    /**
     * Creates Playwright browser, navigates to given URL and runs the test block.
     * @param mt logs time measurements here.
     * @param url the URL to navigate to.
     * @param limiter limits the number of concurrent playwright browser creations. Creating a browser is CPU-intensive;
     *                creating 100 browsers at the same time slows down the OS to a crawl. Better to serialize that.
     *                Browser init consumes roughly 2 CPU cores; a good rule of thumb therefore is to pass in Semaphore
     *                with number of permits equal to the number of CPU cores divided by 2.
     * @param commenceLatch await until all Playwrights in all threads have been initialized, then commence all tests at once,
     *                      to start hammering the server at the same time.
     * @param block runs the test block with Playwright page.
     */
    public static void withPlaywrightPage(@NotNull MeasureTime mt, @NotNull String url, @NotNull Semaphore limiter, @NotNull CountDownLatch commenceLatch, @NotNull Consumer<Page> block) {
        try {
            // acquire the semaphore permit before initializing Playwright. This way we'll
            // initialize only certain amount of Playwrights at the same time, to avoid hammering on the CPU.
            limiter.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mt.log("waiting");
        try (Playwright playwright = Playwright.create()) {
            mt.log("Playwright init");
            try (Browser browser = playwright.chromium().launch()) {
                mt.log("Browser launch");
                try (Page page = browser.newPage()) {
                    mt.log("New page");
                    page.navigate(url);
                    mt.log("Navigate");
                    // Playwright is fully initialized in this thread. Release the semaphore permit, so that other threads can initialize their Playwright too.
                    limiter.release();

                    // Await for other threads to initialize their Playwrights, before commencing the test suite.
                    commenceLatch.countDown();
                    try {
                        commenceLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mt.log("LatchAwait");

                    // Alrighty, everything is ready, commence the tests!
                    block.accept(page);
                    mt.log("Test");
                }
            }
        }
    }
}
