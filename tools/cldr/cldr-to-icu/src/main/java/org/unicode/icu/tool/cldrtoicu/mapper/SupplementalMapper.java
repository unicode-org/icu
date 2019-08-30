// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.natural;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;

import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Generate supplemental {@link IcuData} by transforming {@link CldrDataType#SUPPLEMENTAL
 * SUPPLEMENTAL} data using a {@link PathValueTransformer}.
 *
 * <p>This is currently driven by the {@code ldml2icu_supplemental.txt} configuration file via a
 * {@code RegexTransformer}, but could use any {@link PathValueTransformer} implementation.
 */
public final class SupplementalMapper {
    private static final RbPath RB_FIFO = RbPath.of("<FIFO>");

    /**
     * Processes a subset of supplemental data from the given supplier.
     *
     * @param src the CLDR data supplier to process.
     * @param transformer the transformer to match and transform each CLDR path/value pair.
     * @param icuName the name for the generated IcuData.
     * @param includePaths a matcher to select the CLDR paths to be transformed.
     * @return An IcuData instance containing the specified subset of supplemental data with the
     *     given ICU name.
     */
    // TODO: Improve external data splitting and remove need for a PathMatcher here.
    public static IcuData process(
        CldrDataSupplier src, PathValueTransformer transformer, String icuName,
        PathMatcher includePaths) {
        ResultsCollector collector = new ResultsCollector(includePaths, transformer);
        // Write out the results into the IcuData class, preserving result grouping and expanding
        // path references as necessary.
        IcuData icuData = new IcuData(icuName, false);
        icuData.addResults(collector.getResults(src));
        return icuData;
    }

    private static final class ResultsCollector {
        private final PathMatcher pathMatcher;
        private final PathValueTransformer transformer;

        // WARNING: TreeMultimap() is NOT suitable here, even though it would sort the values for
        // each key. The reason is that result comparison is not "consistent with equals", and
        // TreeMultimap uses the comparator to decide if two elements are equal (not the equals()
        // method), and it does this even if using the add() method of the sorted set (this is in
        // fact in violation of the stated behaviour of Set#add).
        private final SetMultimap<RbPath, Result> resultsByRbPath = LinkedHashMultimap.create();
        private int fifoCounter = 0;

        ResultsCollector(PathMatcher pathMatcher, PathValueTransformer transformer) {
            this.pathMatcher = checkNotNull(pathMatcher);
            this.transformer = checkNotNull(transformer);
        }

        private void visit(CldrValue value) {
            if (pathMatcher.matchesPrefixOf(value.getPath())) {
                for (Result r : transformer.transform(value)) {
                    RbPath rbPath = r.getKey();
                    if (rbPath.contains(RB_FIFO)) {
                        // The fifo counter needs to be formatted with leading zeros for sorting.
                        rbPath = rbPath.mapSegments(
                            s -> s.equals("<FIFO>") ? String.format("<%04d>", fifoCounter) : s);
                    }
                    resultsByRbPath.put(rbPath, r);
                }
                fifoCounter++;
            }
        }

        ImmutableListMultimap<RbPath, Result> getResults(CldrDataSupplier supplier) {
            // DTD and NESTED_GROUPING order differ because of how the magic <FIFO> label works (it
            // basically enforces "encounter order" onto things in unlabeled sequences, which matches
            // the old behaviour). If it wouldn't break anything, it might be worth moving to DTD order
            // to remove any lingering implicit dependencies on the CLDR data behaviour.
            CldrData supplementalData = supplier.getDataForType(CldrDataType.SUPPLEMENTAL);
            PathValueTransformer.DynamicVars varFn = p -> {
                CldrValue cldrValue = supplementalData.get(p);
                return cldrValue != null ? cldrValue.getValue() : null;
            };

            supplementalData.accept(NESTED_GROUPING, this::visit);

            ImmutableListMultimap.Builder<RbPath, Result> out = ImmutableListMultimap.builder();
            out.orderValuesBy(natural());
            for (RbPath rbPath : resultsByRbPath.keySet()) {
                Set<Result> existingResults = resultsByRbPath.get(rbPath);
                out.putAll(rbPath, existingResults);
                for (Result fallback : transformer.getFallbackResultsFor(rbPath, varFn)) {
                    if (existingResults.stream().noneMatch(fallback::isFallbackFor)) {
                        out.put(rbPath, fallback);
                    }
                }
            }
            return out.build();
        }
    }

    private SupplementalMapper() {}
}
