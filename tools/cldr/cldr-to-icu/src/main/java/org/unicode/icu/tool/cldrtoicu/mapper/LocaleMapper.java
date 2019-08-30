// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Ordering.natural;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.RESOLVED;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.DynamicVars;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.SupplementalData;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Generate locale {@link IcuData} by transforming {@link CldrDataType#LDML LDML} data using a
 * {@link PathValueTransformer}.
 *
 * <p>This is currently driven by the {@code ldml2icu_locale.txt} configuration file via a
 * {@code RegexTransformer}, but could use any {@link PathValueTransformer} implementation.
 */
public final class LocaleMapper {
    // The default calendar (only set is different from inherited parent value).
    private static final RbPath RB_CALENDAR = RbPath.of("calendar", "default");

    /**
     * Processes data from the given supplier to generate general locale data for the given locale
     * ID.
     *
     * @param localeId the locale ID to generate data for.
     * @param src the CLDR data supplier to process.
     * @param icuSpecialData additional ICU data (in the "icu:" namespace)
     * @param transformer the transformer to match and transform each CLDR path/value pair.
     * @param supplementalData additional necessary data derived from
     *        {@link org.unicode.cldr.api.CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data.
     * @return IcuData containing locale data for the given locale ID.
     */
    public static IcuData process(
        String localeId,
        CldrDataSupplier src,
        Optional<CldrData> icuSpecialData,
        PathValueTransformer transformer,
        SupplementalData supplementalData) {

        IcuData icuData = new IcuData(localeId, true);
        // Write out the results into the IcuData class, preserving result grouping and expanding
        // path references as necessary.
        ResultsCollector collector = new ResultsCollector(transformer);
        icuData.addResults(collector.collectResultsFor(localeId, src, icuSpecialData));
        doDateTimeHack(icuData);
        supplementalData.getDefaultCalendar(icuData.getName())
            .ifPresent(c -> icuData.add(RB_CALENDAR, c));
        return icuData;
    }

    // This is an awful hack for post-processing the date-time format patterns to inject a 13th
    // pattern at index 8, which is just a duplicate of the "medium" date-time pattern. The reasons
    // for this are lost in the midst of time, but essentially there's ICU library code that just
    // expects the value at index 8 to be this "default" value, and reads the date-time values
    // starting at index 9.
    //
    // Before the hack would be at index 10, since there are 3 groups:
    //   "time" -> "date" -> "date-time"
    // with 4 patterns each:
    //   "full" -> "long" -> "medium" -> "short"
    private static void doDateTimeHack(IcuData icuData) {
        for (RbPath rbPath : icuData.getPaths()) {
            if (rbPath.length() == 3
                && rbPath.getSegment(0).equals("calendar")
                && rbPath.getSegment(2).equals("DateTimePatterns")) {
                // This cannot be null and should not be empty, since the path is in this data.
                List<RbValue> valuesToHack = icuData.get(rbPath);
                checkArgument(valuesToHack.size() == 12,
                    "unexpected number of date/time patterns for '%s': %s", rbPath, valuesToHack);
                valuesToHack.add(8, valuesToHack.get(10));
            }
        }
    }

    private static final class ResultsCollector {
        private final PathValueTransformer transformer;
        private final Set<RbPath> validRbPaths = new HashSet<>();

        // WARNING: TreeMultimap() is NOT suitable here, even though it would sort the values for
        // each key. The reason is that result comparison is not "consistent with equals", and
        // TreeMultimap uses the comparator to decide if two elements are equal (not the equals()
        // method), and it does this even if using the add() method of the sorted set (this is in
        // fact in violation of the stated behaviour of Set#add).
        private final SetMultimap<RbPath, Result> resultsByRbPath = LinkedHashMultimap.create();

        ResultsCollector(PathValueTransformer transformer) {
            this.transformer = checkNotNull(transformer);
        }

        ImmutableListMultimap<RbPath, Result> collectResultsFor(
            String localeId, CldrDataSupplier src, Optional<CldrData> icuSpecialData) {

            CldrData unresolved = src.getDataForLocale(localeId, UNRESOLVED);
            CldrData resolved = src.getDataForLocale(localeId, RESOLVED);
            DynamicVars varFn = p -> {
                CldrValue cldrValue = resolved.get(p);
                return cldrValue != null ? cldrValue.getValue() : null;
            };

            collectPaths(unresolved, varFn);
            collectResults(resolved, varFn);
            icuSpecialData.ifPresent(s -> collectSpecials(s, varFn));

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

        private void collectPaths(CldrData unresolved, DynamicVars varFn) {
            ValueVisitor collectPaths =
                v -> transformer.transform(v, varFn).forEach(this::collectResultPath);
            unresolved.accept(DTD, collectPaths);
        }

        private void collectResultPath(Result result) {
            RbPath rbPath = result.getKey();
            validRbPaths.add(rbPath);
            if (rbPath.isAnonymous()) {
                RbPath parent = rbPath.getParent();
                checkState(!parent.isAnonymous(),
                    "anonymous paths should not be nested: %s", rbPath);
                validRbPaths.add(parent);
            }
        }

        void collectResults(CldrData resolved, DynamicVars varFn) {
            ValueVisitor collectResults =
                v -> transformer.transform(v, varFn).stream()
                    .filter(r -> validRbPaths.contains(r.getKey()))
                    .forEach(r -> resultsByRbPath.put(r.getKey(), r));
            resolved.accept(DTD, collectResults);
        }

        private void collectSpecials(CldrData cldrData, DynamicVars varFn) {
            cldrData.accept(DTD, v ->
                transformer.transform(v, varFn).forEach(r -> resultsByRbPath.put(r.getKey(), r)));
        }
    }

    private LocaleMapper() {}
}
