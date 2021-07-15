// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;

import com.google.common.annotations.VisibleForTesting;

/**
 * A mapper to collect plural data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data via
 * the paths:
 * <pre>{@code
 *   //supplementalData/plurals/pluralRanges[@locales=*]/...
 * }</pre>
 */
public final class PluralRangesMapper {

    private static final CldrDataProcessor<PluralRangesMapper> CLDR_PROCESSOR;
    static {
        CldrDataProcessor.Builder<PluralRangesMapper> processor = CldrDataProcessor.builder();
        processor
            .addAction(
                "//supplementalData/plurals/pluralRanges[@locales=*]", (m, p) -> m.new Ranges(p))
            .addValueAction("pluralRange[@start=*][@end=*]", Ranges::visitRange);
        CLDR_PROCESSOR = processor.build();
    }

    private static final AttributeKey RANGES_LOCALES = keyOf("pluralRanges", "locales");
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
        return process(src.getDataForType(SUPPLEMENTAL));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(CldrData data) {
        return CLDR_PROCESSOR.process(data, new PluralRangesMapper(), NESTED_GROUPING).icuData;
    }

    private final IcuData icuData = new IcuData("pluralRanges", false);
    private int setIndex = 0;

    private PluralRangesMapper() { }

    private final class Ranges {
        private final String label;

        Ranges(CldrPath prefix) {
            this.label = String.format("set%02d", setIndex++);
            RANGES_LOCALES.listOfValuesFrom(prefix)
                .forEach(l -> icuData.add(RB_LOCALES.extendBy(l), label));
        }

        private void visitRange(CldrValue value) {
            // Note: "range:start" and "range:end" are optional attributes, but the CLDR DTD
            // specifies a default via comments. They should probably be changed to just have a
            // default in the DTD (and possibly converted to use an enum here).
            icuData.add(RB_RULES.extendBy(label),
                RbValue.of(
                    RANGE_START.valueFrom(value, "all"),
                    RANGE_END.valueFrom(value, "all"),
                    RANGE_RESULT.valueFrom(value)));
        }
    }
}