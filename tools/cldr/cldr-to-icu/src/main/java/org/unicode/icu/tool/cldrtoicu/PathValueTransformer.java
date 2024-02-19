// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.ImmutableList;

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
 * {@link #getFallbackResultsFor(RbPath, DynamicVars)} and {@link Result#isFallbackFor(Result)}).
 * <li>Sort elements within each group and flatten result values (see {@link Result#isGrouped()}).
 * </ol>
 *
 * <p>For each unique key, this should yield correctly ordered sequence of values (according to the
 * semantics of the chosen transformer implementation).
 */
public abstract class PathValueTransformer {
    /**
     * A result either obtained by transforming a path/value pair, or as a potential fallback for
     * some known key (see {@link PathValueTransformer#transform(CldrValue)} and
     * {@link PathValueTransformer#getFallbackResultsFor(RbPath, DynamicVars)}).
     */
    public static abstract class Result implements Comparable<Result> {
        private final RbPath key;

        protected Result(RbPath key) {
            this.key = checkNotNull(key);
        }

        /**
         * Returns the key of this result, used to group results and determine fallback values
         * according to the semantics of the chosen transformer.
         */
        public RbPath getKey() {
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
         * {@code [ ["a", "b"], ["c", "d"], "e" ]}, producing only 3 top-level values.
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
        public final String toString() {
            return String.format(
                "Result{ key='%s', grouped=%s, values=%s }",
                getKey(), isGrouped(), getValues());
        }
    }

    /**
     * A "typedef" for the function to do late binding of dynamic variables. This is used for edge
     * cases where a %N variable in the rules config is bound to a CLDR path (e.g. "//foo/bar")
     * which cannot be resolved until the rule is evaluated. Unfortunately the need to support late
     * binding of variables incurs significant additional complexity in the code, despite being
     * used in exactly one situation so far (the '%D' variable to represent the default numbering
     * scheme.
     */
    // TODO: Figure out how to get rid of all of this mess.
    public interface DynamicVars extends Function<CldrPath, String> {}

    /**
     * Transforms a CLDR value into a sequence of results (empty if the value was not matched by
     * any rule).
     *
     * @param cldrValue the value to transform.
     * @return the transformed result(s).
     */
    public abstract ImmutableList<Result> transform(CldrValue cldrValue);

    /**
     * Transforms a CLDR value into a sequence of results (empty if the value was not matched by
     * any rule). The dynamic variable function provides any "late bound" CLDR path variables to be
     * resolved from CLDR data during processing (e.g "%D=//ldml/numbers/defaultNumberingSystem").
     *
     * @param cldrValue the value to transform.
     * @param varFn a function for resolving "late bound" variables.
     * @return the transformed result(s).
     */
    public abstract ImmutableList<Result> transform(CldrValue cldrValue, DynamicVars varFn);

    /**
     * Returns a possibly empty sequence of fallback results for a given key. A fallback result for
     * a key should be used only if it is not a fallback for any other result with that key; see
     * also {@link Result#isFallbackFor(Result)}.
     */
    public abstract ImmutableList<Result> getFallbackResultsFor(RbPath key, DynamicVars varFn);
}
