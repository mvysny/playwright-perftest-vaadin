package com.vaadin.starter.skeleton.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Maintains a thread pool, each thread having a Playwright instance available.
 * You can submit a test tasks as Runnables - the runnable will be run in parallel
 * in all threads.
 * <p></p>
 * When closed, this object will close all Playwrights and shutdown all threads.
 */
public class ParallelPlaywright implements AutoCloseable {
    @NotNull
    private final BetterExecutor executor;
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(ParallelPlaywright.class);
    /**
     * Each thread will contain its own playwright
     */
    @NotNull
    private final ThreadLocal<PlaywrightAndPage> playwrightThreadLocal = new ThreadLocal<>();

    /**
     * The number of threads as given in the constructor.
     */
    private final int concurrentBrowsers;
    @NotNull
    private final String url;

    public ParallelPlaywright(int concurrentBrowsers, @NotNull String url) {
        this.concurrentBrowsers = concurrentBrowsers;
        this.url = Objects.requireNonNull(url);
        if (concurrentBrowsers < 1) {
            throw new IllegalArgumentException("Parameter concurrentBrowsers: invalid value " + concurrentBrowsers + ": must be 1 or higher");
        }
        executor = new BetterExecutor(Executors.newFixedThreadPool(concurrentBrowsers));
    }

    /**
     * Creates the browsers. Might take a long time to execute.
     */
    public void initialize() {
        PlaywrightUtils.warmupPlaywright(url);
        final int cpuCores = Runtime.getRuntime().availableProcessors();
        final int permits = cpuCores / 2;
        final int rate = permits < 1 ? 1 : permits;
        final Semaphore playwrightInitSemaphore = new Semaphore(rate);
        log.info("CPU cores: " + cpuCores + "; Playwright initialization rate limited to " + rate + " concurrent inits");
        final MeasureTime mt = new MeasureTime("Overall Playwright stats");
        runInAllThreadsAndWait(() -> {
            try {
                playwrightInitSemaphore.acquire();
                try {
                    playwrightThreadLocal.set(PlaywrightAndPage.create(mt, url));
                } finally {
                    playwrightInitSemaphore.release();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        log.info(concurrentBrowsers + " Playwright browsers initialized: " + mt.format());
    }

    public void runInAllBrowsersAndWait(@NotNull Consumer<Page> testBlock) {
        runInAllThreadsAndWait(() -> {
            final Page page = Objects.requireNonNull(playwrightThreadLocal.get().page);
            testBlock.accept(page);
        });
    }

    /**
     * Runs given Runnable in all threads in parallel. Awaits until all runnables have
     * finished their execution.
     * @param runnable the runnable to run in parallel, not null.
     */
    private void runInAllThreadsAndWait(@NotNull Runnable runnable) {
        Objects.requireNonNull(runnable);
        // we'll submit "parallelThreads" amount of runnables and then wait on a latch.
        // That way we are sure that each Thread has executed the runnable: it cannot happen
        // that a single thread is executing a bunch of runnables serially.
        final CountDownLatch allRunnablesExecutedLatch = new CountDownLatch(concurrentBrowsers);
        final List<Runnable> tasks = Collections.nCopies(concurrentBrowsers, () -> {
            try {
                runnable.run();
            } finally {
                allRunnablesExecutedLatch.countDown();
                try {
                    allRunnablesExecutedLatch.await();
                } catch (InterruptedException e) {
                    log.error("Unexpected", e);
                }
            }
        });
        try {
            executor.submitAllAndWait(tasks);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        runInAllThreadsAndWait(() -> {
            final PlaywrightAndPage playwright = playwrightThreadLocal.get();
            playwright.close();
        });
        executor.close();
    }

    private static class PlaywrightAndPage implements AutoCloseable {
        @NotNull
        public final Playwright playwright;
        @NotNull
        public final Page page;

        private PlaywrightAndPage(@NotNull Playwright playwright, @NotNull Page page) {
            this.playwright = playwright;
            this.page = page;
        }

        @NotNull
        public static PlaywrightAndPage create(@NotNull MeasureTime mt, @NotNull String url) {
            mt.log("waiting");
            final Playwright pw = Playwright.create();
            mt.log("Playwright Init");
            boolean initialized = false;
            try {
                final Browser browser = pw.chromium().launch();
                mt.log("Browser Launch");
                final Page page = browser.newPage();
                mt.log("New Page");
                page.navigate(url);
                mt.log("Navigate");
                initialized = true;
                return new PlaywrightAndPage(pw, page);
            } finally {
                if (!initialized) {
                    try {
                        pw.close();
                    } catch (Exception ex) {
                        log.error("Failed to close Playwright", ex);
                    }
                }
            }
        }

        @Override
        public void close() {
            playwright.close();
        }
    }
}
