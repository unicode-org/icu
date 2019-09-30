// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.RESOLVED;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.SupplementalData;

import com.google.common.annotations.VisibleForTesting;

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

        return process(
            localeId,
            src,
            icuSpecialData,
            transformer,
            supplementalData.getDefaultCalendar(localeId));
    }

    @VisibleForTesting  // Avoids needing to pass a complete SupplementalData instance in tests.
    public static IcuData process(
        String localeId,
        CldrDataSupplier src,
        Optional<CldrData> icuSpecialData,
        PathValueTransformer transformer,
        Optional<String> defaultCalendar) {

        IcuData icuData =
            new LocaleMapper(localeId, src, icuSpecialData, transformer)
                .generateIcuData(localeId, true);
        doDateTimeHack(icuData);
        defaultCalendar.ifPresent(c -> icuData.add(RB_CALENDAR, c));
        return icuData;
    }

    private final String localeId;
    private final CldrDataSupplier src;
    private final Optional<CldrData> icuSpecialData;

    private LocaleMapper(
        String localeId,
        CldrDataSupplier src,
        Optional<CldrData> icuSpecialData,
        PathValueTransformer transformer) {

        super(src.getDataForLocale(localeId, RESOLVED), transformer);
        this.localeId = localeId;
        this.src = checkNotNull(src);
        this.icuSpecialData = checkNotNull(icuSpecialData);
    }

    @Override
    void addResults() {
        collectResults(collectPaths());
        icuSpecialData.ifPresent(this::collectSpecials);
    }

    private Set<RbPath> collectPaths() {
        Set<RbPath> validRbPaths = new HashSet<>();
        src.getDataForLocale(localeId, UNRESOLVED)
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
