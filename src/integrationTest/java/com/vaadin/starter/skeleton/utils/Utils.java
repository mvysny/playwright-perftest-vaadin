package com.vaadin.starter.skeleton.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class Utils {
    @NotNull
    public static <E extends Comparable<? super E>> E calculateMedian(@NotNull Collection<E> items) {
        final ArrayList<E> list = new ArrayList<>(items);
        list.sort(Comparator.naturalOrder());
        return list.get(list.size() / 2);
    }
}
