package com.vaadin.starter.skeleton.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Measures work. The work consists of several work items, each logged via [log].
 * Assumes that a work item starts right away. Thread-safe.
 */
public final class MeasureTime {
    /**
     * @param name the work name.
     */
    public MeasureTime(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * The work name.
     */
    @NotNull
    private final String name;
    /**
     * The start of the work.
     */
    private final long start = System.currentTimeMillis();
    /**
     * The start of the last work item.
     */
    private final ThreadLocal<Long> startOfLastWorkItem = new ThreadLocal<>();

    private long getStartOfLastWorkItem() {
        final Long last = startOfLastWorkItem.get();
        return last == null ? start : last;
    }

    /**
     * Maps work item name to the total activity duration.
     */
    @NotNull
    private final ConcurrentHashMap<String, Measurements> log = new ConcurrentHashMap<>();

    /**
     * A work item is done. Log the duration of the work item (calculated from the end of the previous work item).
     * @param workItemName the work item name, not null.
     */
    public void log(@NotNull String workItemName) {
        Objects.requireNonNull(workItemName);
        final long now = System.currentTimeMillis();
        final long duration = now - getStartOfLastWorkItem();
        log.computeIfAbsent(workItemName, (key) -> new Measurements()).log(duration);
        startOfLastWorkItem.set(now);
    }

    @NotNull
    public String format() {
        return name + log + " TOTAL=" + (System.currentTimeMillis() - start);
    }

    @Override
    public String toString() {
        return name + log.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().getTotalDuration()) + " TOTAL=" + (System.currentTimeMillis() - start);
    }

    /**
     * Thread-safe. Remembers a bunch of measurements and provides min/max/avg/median. Call {@link #format()} to obtain a nicely formatted string.
     */
    private static class Measurements {
        // thread-safe.
        private final BlockingQueue<Long> measurements = new LinkedBlockingQueue<>();

        public void log(long durationMs) {
            measurements.add(durationMs);
        }

        public long getTotalDuration() {
            return measurements.stream().reduce(0L, Long::sum);
        }

        public long getAvgDuration() {
            return getTotalDuration() / measurements.size();
        }

        public long getMinDuration() {
            return measurements.stream().min(Long::compareTo).get();
        }

        public long getMaxDuration() {
            return measurements.stream().max(Long::compareTo).get();
        }

        public long getMedianDuration() {
            return Utils.calculateMedian(measurements);
        }

        @Override
        public String toString() {
            return format();
        }

        public int getCount() {
            return measurements.size();
        }

        @NotNull
        public String format() {
            if (measurements.isEmpty()) {
                return "N/A";
            }
            if (measurements.size() == 1) {
                return measurements.toString();
            }
            return "min=" + getMinDuration() + "/max=" + getMaxDuration() + "/avg=" + getAvgDuration() + "/median=" + getMedianDuration() + "/total=" + getTotalDuration() + "/count=" + getCount();
        }
    }
}
