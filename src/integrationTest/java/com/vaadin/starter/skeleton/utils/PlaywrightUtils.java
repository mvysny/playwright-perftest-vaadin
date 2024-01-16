package com.vaadin.starter.skeleton.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class PlaywrightUtils {

    public static void warmupPlaywright(@NotNull String url) {
        final MeasureTime mt = new MeasureTime("Warmup");
        withPlaywrightPage(mt, url, new Semaphore(1), new CountDownLatch(1), (page) -> {});
        System.out.println(mt.format());
    }

    /**
     * Creates Playwright browser, navigates to given URL and runs the block.
     * @param mt logs time measurements here.
     * @param url the URL to navigate to.
     * @param limiter limits the number of concurrent playwright browser creations. Creating a browser is CPU-intensive;
     *                creating 100 browsers at the same time slows down the OS to a crawl. Better to serialize that.
     *                Browser init consumes roughly 2 CPU cores; a good rule of thumb therefore is to pass in Semaphore
     *                with number of permits equal to the number of CPU cores divided by 2.
     * @param commenceLatch await until all Playwrights in all threads have been initialized, then commence all tests at once,
     *                      to start hammering the server at the same time.
     * @param block runs the block with Playwright page.
     */
    public static void withPlaywrightPage(@NotNull MeasureTime mt, @NotNull String url, @NotNull Semaphore limiter, @NotNull CountDownLatch commenceLatch, @NotNull Consumer<Page> block) {
        try {
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
                    limiter.release();
                    commenceLatch.countDown();
                    try {
                        commenceLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mt.log("LatchAwait");
                    block.accept(page);
                    mt.log("Test");
                }
            }
        }
    }
}
