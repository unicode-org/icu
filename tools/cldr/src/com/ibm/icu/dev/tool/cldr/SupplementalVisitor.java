package com.ibm.icu.dev.tool.cldr;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.ibm.icu.dev.tool.cldr.PathValueTransformer.Result;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.natural;

/** Converts supplemental LDML data from CLDR to the ICU data structure. */
final class SupplementalVisitor {
    /**
     * Transforms supplemental CLDR data according to the specified regular expression based
     * transformer configuration file.
     */
    public static IcuData process(CldrDataSupplier supplier, Path configFile)
        throws IOException {
        CldrData supplementalData = supplier.getDataForType(CldrDataType.SUPPLEMENTAL);
        ResultsCollector collector = new ResultsCollector(
            RegexTransformer.fromConfig(configFile,
                IcuFunctions.ALGORITHM_FN,
                IcuFunctions.DATE_FN,
                IcuFunctions.EXP_FN,
                IcuFunctions.YMD_FN));

        // DTD and NESTED_GROUPING order differ because of how the magic <FIFO> label works (it
        // basically enforces "encounter order" onto things in unlabeled sequences, which matches
        // the old behaviour). If it wouldn't break anything, it might be worth moving to DTD order
        // to remove any lingering implicit dependencies on the CLDR data behaviour.
        supplementalData.accept(CldrData.PathOrder.NESTED_GROUPING, collector);
        ImmutableListMultimap<String, Result> resultsByRbPath = collector.getResults();

        // Write out the results into the IcuData class, preserving result grouping and expanding
        // path references as necessary.
        IcuData icuData = new IcuData("test.xml", "supplementalData", false, ORDINAL_MAP);
        for (String rbPath : resultsByRbPath.keySet()) {
            for (Result r : resultsByRbPath.get(rbPath)) {
                if (r.isGrouped()) {
                    // Grouped results have all the values in a single value entry.
                    icuData.add(rbPath, r.getValues().toArray(new String[0]));
                } else {
                    // Ungrouped results are one value per entry, but might be expanded into
                    // grouped results if they are a path referencing a grouped entry.
                    r.getValues().forEach(v -> icuData.add(rbPath, replacePathValues(v, icuData)));
                }
            }
        }
        // Finally add the CLDR version.
        icuData.add("/cldrVersion", CldrDataSupplier.getCldrVersionString());
        return icuData;
    }

    private static final Pattern ARRAY_INDEX = Pattern.compile("(/[^\\[]++)(?:\\[(\\d++)\\])?$");

    /**
     * Replaces an ungrouped CLDR value for the form "/foo/bar" or "/foo/bar[N]" which is assumed
     * to be a reference to an existing value in a resource bundle. Note that the referenced bundle
     * might be grouped (i.e. an array with more than one element).
     */
    private static String[] replacePathValues(String value, IcuData icuData) {
        Matcher m = ARRAY_INDEX.matcher(value);
        if (!m.matches()) {
            return new String[] { value };
        }
        // The only constraint is that the "path" value starts with a leading '/'.
        String replacePath = m.group(1);
        List<String[]> replaceValues = icuData.get(replacePath);
        checkArgument(replaceValues != null, "Path %s is missing from IcuData", replacePath);
        // If no index is given (e.g. "/foo/bar" then treat it as "/foo/bar[0]"
        int replaceIndex = m.groupCount() > 1 ? Integer.parseInt(m.group(2)) : 0;
        return replaceValues.get(replaceIndex);
    }

    private static final class ResultsCollector implements ValueVisitor {
        // Maybe have a version that can sort the matchers and do a log(N) match?
        private static final PathMatcher ALLOWED_PATHS = PathMatcher.inOrder(
            PathMatcher.of("supplementalData/calendarData"),
            PathMatcher.of("supplementalData/calendarPreferenceData"),
            PathMatcher.of("supplementalData/codeMappings"),
            PathMatcher.of("supplementalData/codeMappingsCurrency"),
            PathMatcher.of("supplementalData/currencyData"),
            PathMatcher.of("supplementalData/idValidity"),
            PathMatcher.of("supplementalData/languageData"),
            PathMatcher.of("supplementalData/languageMatching"),
            PathMatcher.of("supplementalData/measurementData"),
            PathMatcher.of("supplementalData/parentLocales"),
            PathMatcher.of("supplementalData/subdivisionContainment"),
            PathMatcher.of("supplementalData/territoryContainment"),
            PathMatcher.of("supplementalData/territoryInfo"),
            PathMatcher.of("supplementalData/timeData"),
            PathMatcher.of("supplementalData/unitPreferenceData"),
            PathMatcher.of("supplementalData/weekData"),
            PathMatcher.of("supplementalData/weekOfPreference"));

        private final PathValueTransformer transformer;

        // WARNING: TreeMultimap() is NOT suitable here, even though it would sort the values for
        // each key. The reason is that result comparison is not "consistent with equals", and
        // TreeMultimap uses the comparator to decide if two elements are equal (not the equals()
        // method), and it does this even if using the add() method of the sorted set (this is in
        // fact in violation of the stated behaviour of Set#add).
        private final SetMultimap<String, Result> resultsByRbPath = LinkedHashMultimap.create();
        private int fifoCounter = 0;

        ResultsCollector(PathValueTransformer transformer) {
            this.transformer = checkNotNull(transformer);
        }

        @Override
        public void visit(CldrValue value) {
            CldrPath path = value.getPath();
            if (ALLOWED_PATHS.matchesPrefixOf(path)) {
                for (Result r : transformer.transform(value)) {
                    String key = r.getKey();
                    // The fifo counter needs to be formatted with leading zeros for sorting.
                    String rbPath = key.contains("<FIFO>")
                        ? key.replace("<FIFO>", '<' + String.format("%04d", fifoCounter) + '>')
                        : key;
                    resultsByRbPath.put(rbPath, r);
                }
                fifoCounter++;
            }
        }

        ImmutableListMultimap<String, Result> getResults() {
            ImmutableListMultimap.Builder<String, Result> out = ImmutableListMultimap.builder();
            out.orderValuesBy(natural());
            for (String rbPath : resultsByRbPath.keySet()) {
                Set<Result> existingResults = resultsByRbPath.get(rbPath);
                out.putAll(rbPath, existingResults);
                for (Result fallback : transformer.getFallbackResultsFor(rbPath)) {
                    if (existingResults.stream().noneMatch(fallback::isFallbackFor)) {
                        out.put(rbPath, fallback);
                    }
                }
            }
            return out.build();
        }
    }

    // This can contain more than just days of the week if necessary - see IcuData for details.
    // Note that this should almost certainly be replaced by a function in the transformer instead
    // of assuming that any value that happens to match a day of the week should be turned into an
    // integer.
    private static final ImmutableMap<String, String> ORDINAL_MAP =
        ImmutableMap.<String, String>builder()
            .put("sun", "1")
            .put("mon", "2")
            .put("tues", "3")
            .put("wed", "4")
            .put("thu", "5")
            .put("fri", "6")
            .put("sat", "7")
            .build();
}
