package com.vaadin.starter.skeleton.utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Adds helpful utility methods on top of {@link ExecutorService}. When closed,
 * it will shut down the underlying executor and will await its termination.
 */
public class BetterExecutor implements AutoCloseable {
    @NotNull
    private final ExecutorService executor;

    /**
     * Run tasks in given executor.
     * @param executor the executor.
     */
    public BetterExecutor(@NotNull ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @NotNull
    private CompletableFuture<Void> submitAll(@NotNull List<Runnable> tasks) {
        final List<CompletableFuture<Void>> futures = tasks.stream().map(task -> CompletableFuture.runAsync(task, executor)).toList();
        final CompletableFuture<Void> catchAllFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return catchAllFuture;
    }

    /**
     * Submits all tasks to the executor and waits until they all complete. If any of the tasks throws
     * an exception, this function terminates immediately and throws the {@link ExecutionException}.
     * @param tasks the tasks to run in parallel.
     * @throws ExecutionException if any of the tasks threw an exception
     * @throws InterruptedException if interrupted during waiting
     */
    public void submitAllAndWait(@NotNull List<Runnable> tasks) throws ExecutionException, InterruptedException {
        submitAll(tasks).get();
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        while (!executor.awaitTermination(1, TimeUnit.DAYS)) {}
    }
}
