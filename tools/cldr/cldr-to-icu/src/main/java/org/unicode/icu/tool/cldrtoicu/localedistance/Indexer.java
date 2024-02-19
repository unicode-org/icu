// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Returns a canonicalized value for each unique value encountered, the memoized value is
 * created using the zero-based index of the value and the given transformation function.
 */
final class Indexer<T, R> implements Function<T, R> {
    /** Returns a plain indexer which returns the index directly. */
    public static <T> Indexer<T, Integer> create() {
        return create(Function.identity());
    }

    /** Returns an indexer which transforms the returned index by the given function. */
    public static <T, R> Indexer<T, R> create(Function<Integer, R> convertIndexFn) {
        return new Indexer<>(convertIndexFn);
    }

    private final Map<T, Integer> indexMap = new LinkedHashMap<>();
    private final Function<Integer, R> convertIndexFn;

    private Indexer(Function<Integer, R> convertIndexFn) {
        this.convertIndexFn = checkNotNull(convertIndexFn);
    }

    /** Memoizes the given value and returns the derived value. */
    @Override
    public R apply(T value) {
        indexMap.putIfAbsent(checkNotNull(value), indexMap.size());
        return convertIndexFn.apply(indexMap.get(value));
    }

    /** Returns a set of the indexed values, in the order they were first encountered. */
    public Set<T> getValues() {
        return Collections.unmodifiableSet(indexMap.keySet());
    }
}
