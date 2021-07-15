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
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;

import com.google.common.annotations.VisibleForTesting;

/**
 * A mapper to collect day-period data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL}
 * data via the paths:
 * <pre>{@code
 *   //supplementalData/dayPeriodRuleSet/*
 * }</pre>
 */
public final class DayPeriodsMapper {

    private static final CldrDataProcessor<DayPeriodsMapper> CLDR_PROCESSOR;
    static {
        CldrDataProcessor.Builder<DayPeriodsMapper> processor = CldrDataProcessor.builder();
        processor.addAction("//supplementalData/dayPeriodRuleSet", (m, p) -> m.new Ruleset(p))
            .addSubprocessor("dayPeriodRules[@locales=*]", Ruleset::prefixStart)
            .addValueAction("dayPeriodRule[@type=*]", Ruleset::visitRule);
        CLDR_PROCESSOR = processor.build();
    }

    private static final AttributeKey RULESET_TYPE = keyOf("dayPeriodRuleSet", "type");
    private static final AttributeKey RULES_LOCALES = keyOf("dayPeriodRules", "locales");
    private static final AttributeKey RULE_TYPE = keyOf("dayPeriodRule", "type");

    private static final RbPath RB_LOCALES = RbPath.of("locales");

    /**
     * Processes data from the given supplier to generate day-period ICU data.
     *
     * @param src the CLDR data supplier to process.
     * @return the IcuData instance to be written to a file.
     */
    public static IcuData process(CldrDataSupplier src) {
        return process(src.getDataForType(SUPPLEMENTAL));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(CldrData data) {
        return CLDR_PROCESSOR.process(data, new DayPeriodsMapper(), NESTED_GROUPING).icuData;
    }

    // Mutable ICU data collected into during visitation.
    private final IcuData icuData = new IcuData("dayPeriods", false);
    private int setNum = 0;

    private final class Ruleset {
        private RbPath localePrefix;

        Ruleset(CldrPath prefix) {
            this.localePrefix = RULESET_TYPE.optionalValueFrom(prefix)
                .map(t -> RbPath.of("locales_" + t))
                .orElse(RB_LOCALES);
        }

        private void prefixStart(CldrPath prefix) {
            // Sets are arbitrarily identified by the string "setNN".
            String setName = "set" + (++setNum);
            RULES_LOCALES.listOfValuesFrom(prefix)
                .forEach(locale -> icuData.add(localePrefix.extendBy(locale), setName));
        }

        private void visitRule(CldrValue value) {
            RbPath prefix = RbPath.of("rules", "set" + setNum, RULE_TYPE.valueFrom(value));
            value.getValueAttributes()
                .forEach((k, v) -> icuData.add(prefix.extendBy(k.getAttributeName()), v));
        }
    }
}
