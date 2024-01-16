package com.vaadin.starter.skeleton;

import com.vaadin.starter.skeleton.utils.BetterExecutor;
import com.vaadin.starter.skeleton.utils.MeasureTime;
import com.vaadin.starter.skeleton.utils.PlaywrightUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class PerformanceIT {
    private static final int CONCURRENT_BROWSERS = 10;
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
    public void haha() throws Exception {
        System.out.println("haha!!!!");
        Thread.sleep(1000L);
        System.out.println("haha!!!!");
    }
}
