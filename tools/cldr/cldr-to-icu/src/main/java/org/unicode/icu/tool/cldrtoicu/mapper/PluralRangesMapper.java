// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.annotations.VisibleForTesting;

/**
 * A mapper to collect plural data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data via
 * the paths:
 * <pre>{@code
 *   //supplementalData/plurals/pluralRanges[@locales=*]/...
 * }</pre>
 */
public final class PluralRangesMapper {
    // Note that this mapper only matches when there's no "type" specified on the "plurals" element.
    // This is a bit weird, since the PluralsMapper expects a type (e.g. cardinal or ordinal) to
    // be present. Really this just illustrates that the plural ranges just should not be under the
    // same parent element as plurals.
    private static final PathMatcher RANGES =
        PathMatcher.of("supplementalData/plurals/pluralRanges[@locales=*]");
    private static final AttributeKey RANGES_LOCALES = keyOf("pluralRanges", "locales");

    private static final PathMatcher RANGE = PathMatcher.of("pluralRange[@start=*][@end=*]");
    private static final AttributeKey RANGE_START = keyOf("pluralRange", "start");
    private static final AttributeKey RANGE_END = keyOf("pluralRange", "end");
    private static final AttributeKey RANGE_RESULT = keyOf("pluralRange", "result");

    private static final RbPath RB_RULES = RbPath.of("rules");
    private static final RbPath RB_LOCALES = RbPath.of("locales");

    /**
     * Processes data from the given supplier to generate plural-range ICU data.
     *
     * @param src the CLDR data supplier to process.
     * @return the IcuData instance to be written to a file.
     */
    public static IcuData process(CldrDataSupplier src) {
        CldrData data = src.getDataForType(SUPPLEMENTAL);
        return process(data);
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(CldrData data) {
        PluralRangesVisitor visitor = new PluralRangesVisitor();
        data.accept(ARBITRARY, visitor);
        return visitor.icuData;
    }

    private static final class PluralRangesVisitor implements PrefixVisitor {
        private final IcuData icuData = new IcuData("pluralRanges", false);

        private int setIndex = 0;
        private String ruleLabel = null;

        @Override
        public void visitPrefixStart(CldrPath prefix, Context ctx) {
            if (RANGES.matches(prefix)) {
                ruleLabel = String.format("set%02d", setIndex++);
                RANGES_LOCALES.listOfValuesFrom(prefix)
                    .forEach(l -> icuData.add(RB_LOCALES.extendBy(l), ruleLabel));
                ctx.install(this::visitRange);
            }
        }

        private void visitRange(CldrValue value) {
            checkState(RANGE.matchesSuffixOf(value.getPath()),
                "unexpected path: %s", value.getPath());
            // Note: "range:start" and "range:end" are optional attributes, but the CLDR DTD
            // specifies a default via comments. They should probably be changed to just have a
            // default in the DTD (and possibly converted to use an enum here).
            icuData.add(RB_RULES.extendBy(ruleLabel),
                RbValue.of(
                    RANGE_START.valueFrom(value, "all"),
                    RANGE_END.valueFrom(value, "all"),
                    RANGE_RESULT.valueFrom(value)));
        }
    }

    private PluralRangesMapper() {}
}
