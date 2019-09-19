// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Comparator;
import java.util.Objects;

import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableList;

/**
 * A fake result, primarily for testing mappers. This implementation does not "play well" with
 * other result implementations.
 */
public final class FakeResult extends Result {
    private static final Comparator<FakeResult> ORDERING =
        Comparator.comparing(FakeResult::getKey)
            .thenComparing(r -> r.index)
            .thenComparing(r -> r.isFallback);

    /**
     * Returns a primary result. Care must be taken to ensure that multiple "equal()" results are
     * not used in the same test (results are equal if their path and index are equal, and they
     * share the same fallback state).
     *
     * @param path the path of the result
     * @param index the sort index of the result (to distinguish paths with the same path).
     * @param isGrouped whether values in the result should be grouped into a separate sub-array.
     * @param values the result values.
     */
    public static Result of(String path, int index, boolean isGrouped, String... values) {
        return new FakeResult(
            RbPath.parse(path), ImmutableList.copyOf(values), isGrouped, index, false);
    }

    /**
     * Returns a fallback result. Note that currently fallbacks are never "grouped".
     *
     * @param path the path of the result
     * @param index the sort index of the result (to distinguish paths with the same path).
     * @param values the result values.
     */
    public static Result fallback(String path, int index, String... values) {
        return new FakeResult(RbPath.parse(path), ImmutableList.copyOf(values), false, index, true);
    }

    private final boolean grouped;
    private final ImmutableList<String> values;
    private final boolean isFallback;
    private final int index;

    private FakeResult(
        RbPath path, ImmutableList<String> values, boolean grouped, int index, boolean isFallback) {
        super(path);
        this.grouped = grouped;
        this.values = values;
        this.isFallback = isFallback;
        this.index = index;
    }

    boolean isFallback() {
        return isFallback;
    }

    @Override public boolean isGrouped() {
        return grouped;
    }

    @Override public ImmutableList<String> getValues() {
        return values;
    }

    @Override public boolean isFallbackFor(Result r) {
        FakeResult other = (FakeResult) r;
        return isFallback && !other.isFallback
            && getKey().equals(r.getKey())
            && index == (other).index;
    }

    @Override public int compareTo(Result other) {
        int signum = ORDERING.compare(this, (FakeResult) other);
        checkState(signum != 0 || this == other,
            "equivalent but non-identical results found in test data: %s / %s", this, other);
        return signum;
    }

    // We really don't want to pretend to support mixing implementations of Result in tests.
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override public boolean equals(Object obj) {
        FakeResult other = (FakeResult) obj;
        boolean isEqual = getKey().equals(other.getKey())
            && index == other.index
            && isFallback == other.isFallback;
        checkState(!isEqual || this == other,
            "equivalent but non-identical results found in test data: %s / %s", this, other);
        return isEqual;
    }

    @Override public int hashCode() {
        return Objects.hash(getKey(), index, isFallback);
    }
}
