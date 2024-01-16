package com.vaadin.starter.skeleton.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PlaywrightUtils {
    public static void withPlaywrightPage(@NotNull MeasureTime mt, @NotNull String url, @NotNull Consumer<Page> block) {
        mt.log("waiting");
        try (Playwright playwright = Playwright.create()) {
            mt.log("Playwright init");
            try (Browser browser = playwright.chromium().launch()) {
                mt.log("Browser launch");
                try (Page page = browser.newPage()) {
                    mt.log("New page");
                    page.navigate(url);
                    mt.log("Navigate");
                    block.accept(page);
                    mt.log("Test");
                }
            }
        }
    }
}
