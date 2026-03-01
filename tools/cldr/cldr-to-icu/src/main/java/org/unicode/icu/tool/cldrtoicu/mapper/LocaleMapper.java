// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

/**
 * Generate locale {@link IcuData} by transforming {@link CldrDataType#LDML LDML} data using a
 * {@link PathValueTransformer}.
 *
 * <p>This is currently driven by the {@code ldml2icu_locale.txt} configuration file via a
 * {@code RegexTransformer}, but could use any {@link PathValueTransformer} implementation.
 */
public final class LocaleMapper extends AbstractPathValueMapper {
    // The default calendar (only set is different from inherited parent value).
    private static final RbPath RB_CALENDAR = RbPath.of("calendar", "default");

    /**
     * Processes data from the given supplier to generate general locale data for the given locale.
     *
     * @param icuData the ICU data to be filled in.
     * @param unresolved the unresolved CLDR source data (to determine which paths to add).
     * @param resolved the resolved CLDR source data (from which the actual data is processed).
     * @param icuSpecialData additional ICU data (in the "icu:" namespace)
     * @param transformer the transformer to match and transform each CLDR path/value pair.
     * @param defaultCalendar the default calendar (obtained separately from supplemental data).
     */
    public static void process(
        IcuData icuData,
        CldrData unresolved,
        CldrData resolved,
        Optional<CldrData> icuSpecialData,
        PathValueTransformer transformer,
        Optional<String> defaultCalendar) {

        new LocaleMapper(unresolved, resolved, icuSpecialData, transformer).addIcuData(icuData);
        doDateTimeHack(icuData);
        defaultCalendar.ifPresent(c -> icuData.add(RB_CALENDAR, c));
    }

    private final CldrData unresolved;
    private final Optional<CldrData> icuSpecialData;

    private LocaleMapper(
        CldrData unresolved,
        CldrData resolved,
        Optional<CldrData> icuSpecialData,
        PathValueTransformer transformer) {

        super(resolved, transformer);
        this.unresolved = checkNotNull(unresolved);
        this.icuSpecialData = checkNotNull(icuSpecialData);
    }

    @Override
    void addResults() {
        collectResults(collectPaths());
        icuSpecialData.ifPresent(this::collectSpecials);
    }

    private Set<RbPath> collectPaths() {
        Set<RbPath> validRbPaths = new HashSet<>();
        unresolved
            .accept(DTD, v -> transformValue(v).forEach(r -> collectResultPath(r, validRbPaths)));
        return validRbPaths;
    }

    private static void collectResultPath(Result result, Set<RbPath> validRbPaths) {
        RbPath rbPath = result.getKey();
        validRbPaths.add(rbPath);
        if (rbPath.isAnonymous()) {
            RbPath parent = rbPath.getParent();
            checkState(!parent.isAnonymous(), "anonymous paths must not be nested: %s", rbPath);
            validRbPaths.add(parent);
        }
    }

    private void collectResults(Set<RbPath> validRbPaths) {
        getCldrData().accept(DTD,
            v -> transformValue(v)
                .filter(r -> validRbPaths.contains(r.getKey()))
                .forEach(result -> addResult(result.getKey(), result)));
    }

    private void collectSpecials(CldrData specials) {
        specials.accept(DTD,
            v -> transformValue(v).forEach(result -> addResult(result.getKey(), result)));
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
                checkState(valuesToHack.size() == 12,
                    "unexpected number of date/time patterns for '/%s': %s", rbPath, valuesToHack);
                valuesToHack.add(8, valuesToHack.get(10));
            }
        }
    }
}
