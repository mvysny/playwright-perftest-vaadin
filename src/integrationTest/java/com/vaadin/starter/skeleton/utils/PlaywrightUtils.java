package com.vaadin.starter.skeleton.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class PlaywrightUtils {
    public static void withPlaywrightPage(@NotNull Consumer<Page> block) {
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch()) {
                try (Page page = browser.newPage()) {
                    block.accept(page);
                }
            }
        }
    }
}
