// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import java.util.Optional;

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

import com.google.common.annotations.VisibleForTesting;

/**
 * A mapper to collect day-period data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL}
 * data via the paths:
 * <pre>{@code
 *   //supplementalData/dayPeriodRuleSet/*
 * }</pre>
 */
public final class DayPeriodsMapper {
    private static final PathMatcher RULESET =
        PathMatcher.of("supplementalData/dayPeriodRuleSet");
    private static final AttributeKey RULESET_TYPE = keyOf("dayPeriodRuleSet", "type");

    private static final PathMatcher RULES = PathMatcher.of("dayPeriodRules[@locales=*]");
    private static final AttributeKey RULES_LOCALES = keyOf("dayPeriodRules", "locales");

    private static final PathMatcher RULE = PathMatcher.of("dayPeriodRule[@type=*]");
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
        RuleSetVisitor mapper = new RuleSetVisitor();
        data.accept(ARBITRARY, mapper);
        return mapper.icuData;
    }

    private static final class RuleSetVisitor implements PrefixVisitor {
        // Mutable ICU data collected into during visitation.
        private final IcuData icuData = new IcuData("dayPeriods", false);
        private int setNum = 0;

        @Override
        public void visitPrefixStart(CldrPath prefix, Context ctx) {
            if (RULESET.matches(prefix)) {
                ctx.install(new RuleVisitor(RULESET_TYPE.optionalValueFrom(prefix)));
            }
        }

        private final class RuleVisitor implements PrefixVisitor {
            private final RbPath localePrefix;

            private RuleVisitor(Optional<String> type) {
                // If there's a given type, add it to the prefix path.
                this.localePrefix = type.map(t -> RbPath.of("locales_" + t)).orElse(RB_LOCALES);
            }

            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                if (RULES.matchesSuffixOf(prefix)) {
                    // Sets are arbitrarily identified by the string "setNN".
                    String setName = "set" + (++setNum);
                    RULES_LOCALES.listOfValuesFrom(prefix)
                        .forEach(locale -> icuData.add(localePrefix.extendBy(locale), setName));
                    ctx.install(this::visitRule);
                }
            }

            private void visitRule(CldrValue value) {
                if (RULE.matchesSuffixOf(value.getPath())) {
                    RbPath prefix = RbPath.of("rules", "set" + setNum, RULE_TYPE.valueFrom(value));
                    value.getValueAttributes()
                        .forEach((k, v) -> icuData.add(prefix.extendBy(k.getAttributeName()), v));
                }
            }
        }
    }

    private DayPeriodsMapper() {}
}
