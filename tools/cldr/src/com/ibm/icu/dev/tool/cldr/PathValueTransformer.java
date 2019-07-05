package com.ibm.icu.dev.tool.cldr;

import com.google.common.collect.ImmutableList;
import org.unicode.cldr.api.CldrValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * API for transforming CLDR path/value pairs. Transformed results support grouping by their key
 * and the ability to generate default "fallback" values to account for missing values in a group.
 *
 * <p>To transform some set of CLDR path/values:
 * <ol>
 * <li>Transform all desired path/value pairs into a set of matched results, discarding duplicates
 * (see {@link #transform(CldrValue)}.
 * <li>Group the results by key (e.g. into a {@code ListMultimap}).
 * <li>For each group, add any fallback values which don't yet exist for that key (see
 * {@link #getFallbackResultsFor(String)} and {@link Result#isFallbackFor(Result)}).
 * <li>Sort elements within each group and flatten result values (see {@link Result#isGrouped()}).
 * </ol>
 *
 * <p>For each unique key, this should yield correctly ordered sequence of values (according to the
 * semantics of the chosen transformer implementation).
 */
abstract class PathValueTransformer {
    /**
     * A result either obtained by transforming a path/value pair, or as a potential fallback for
     * some known key (see {@link PathValueTransformer#transform(CldrValue)} and
     * {@link PathValueTransformer#getFallbackResultsFor(String)}).
     */
    public static abstract class Result implements Comparable<Result> {
        private final String key;

        protected Result(String key) {
            this.key = checkNotNull(key);
        }

        /**
         * Returns the key of this result, used to group results and determine fallback values
         * according to the semantics of the chosen transformer.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns whether the values in this result should be grouped or not. Un-grouped values
         * should be considered as individual values in a sequence and might be joined with values
         * from other results in the same group. Grouped values cannot be split and must appear
         * as a single value.
         *
         * <p>For example for the ordered results:
         * <pre>
         * Result X = { key=K, values=[ "a", "b" ], grouped=false }
         * Result Y = { key=K, values=[ "c", "d" ], grouped=false }
         * Result Z = { key=K, values=[ "e" ], grouped=false }
         * </pre>
         * the values for key {@code K} are conceptually {@code [ "a", "b", "c", "d", "e" ]}.
         *
         * <p>However if result {@code Y} has {@code grouped=true} then there are now 4 values
         * {@code [ "a", "b", ["c", "d"], "e" ]}, and if {@code X} is also grouped, then it is
         * {@code [ ["a", "b"], ["c", "d"], "e" ]}, producing only 3 values.
         */
        public abstract boolean isGrouped();

        /**
         * Returns the transformed values of this result, which may or may not be grouped
         * according to {@link #isGrouped()}.
         */
        public abstract ImmutableList<String> getValues();

        /**
         * Returns whether this result is a fallback for some existing matched result. Fallback
         * results should only be used when it is not a fallback for any existing result.
         */
        public abstract boolean isFallbackFor(Result r);

        /** Debug only string representation. */
        @Override
        public String toString() {
            return String.format(
                "Result{ key='%s', grouped=%s, values=%s }",
                getKey(), isGrouped(), getValues());
        }
    }

    /**
     * Transforms a single CLDR path/value pair into a possibly empty sequence of results (not
     * necessarily with the same key). See class-level documentation for typical usage.
     */
    public abstract ImmutableList<Result> transform(CldrValue v);

    /**
     * Returns a possibly empty sequence of fallback results for a given key. A fallback result for
     * a key should be used only if it is not a fallback for any other result with that key.
     *
     * <p>Fallback results should only be determined after all matched results have been obtained
     * via {@link #transform(CldrValue)}.
     */
    public abstract ImmutableList<Result> getFallbackResultsFor(String key);
}
