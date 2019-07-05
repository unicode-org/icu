package com.ibm.icu.dev.tool.cldr;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.util.List;

import static org.unicode.cldr.api.AttributeKey.keyOf;

/**
 * A visitor to collect plural data from {@code SUPPLEMENTAL} data via the paths:
 * <pre>{@code
 *   //supplementalData/plurals[@type=*]/pluralRules[@locales=*]/pluralRule[@count=*]
 * }</pre>
 */
final class PluralRangesVisitor implements PrefixVisitor {
    private static final PathMatcher RANGES =
        PathMatcher.of("supplementalData/plurals/pluralRanges[@locales=*]");
    private static final AttributeKey RANGES_LOCALES = keyOf("pluralRanges", "locales");

    private static final PathMatcher RANGE = PathMatcher.of("pluralRange[@start=*][@end=*]");
    private static final AttributeKey RANGE_START = keyOf("pluralRange", "start");
    private static final AttributeKey RANGE_END = keyOf("pluralRange", "end");
    private static final AttributeKey RANGE_RESULT = keyOf("pluralRange", "result");

    private enum PluralRanges { zero, one, two, few, many, other }

    /** Processes data from the given supplier to generate plural range data. */
    public static IcuData process(CldrDataSupplier supplier) {
        PluralRangesVisitor v = new PluralRangesVisitor();
        CldrData data = supplier.getDataForType(CldrDataType.SUPPLEMENTAL);
        data.accept(CldrData.PathOrder.ARBITRARY, v);
        return v.icuData;
    }

    // Mutable ICU data collected into during visitation.
    // In a post XML-aware API, is recording the XML file names really a good idea?
    private final IcuData icuData = new IcuData("pluralRanges.xml", "pluralRanges", false);

    private List<String> locales = null;
    private int setIndex = 0;
    private String ruleLabel = null;

    @Override
    public void visitPrefixStart(CldrPath prefix, Context ctx) {
        // Captured type is either "cardinal" or "ordinal" (and will cause exception otherwise).
        if (RANGES.matches(prefix)) {
            ruleLabel = String.format("set%02d", setIndex++);
            RANGES_LOCALES.listOfValuesFrom(prefix)
                .forEach(locale -> icuData.add("/locales/" + locale, ruleLabel));
            ctx.install(this::visitRange);
        }
    }

    private void visitRange(CldrValue value) {
        if (RANGE.matchesSuffixOf(value.getPath())) {
            // Note: "range:start" and "range:end" are optional attributes, but the CLDR DTD
            // specifies a default via comments. They should probably be changed to just have a
            // default in the DTD (and possibly converted to use an enum here).
            icuData.add(
                "/rules/" + ruleLabel,
                RANGE_START.valueFrom(value, "all"),
                RANGE_END.valueFrom(value, "all"),
                RANGE_RESULT.valueFrom(value));
        }
    }

    private PluralRangesVisitor() {}
}
