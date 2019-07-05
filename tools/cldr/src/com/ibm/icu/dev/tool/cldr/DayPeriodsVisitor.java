package com.ibm.icu.dev.tool.cldr;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.util.Optional;

import static org.unicode.cldr.api.AttributeKey.keyOf;

/**
 * A visitor to collect BCP47 data from {@code BCP47} data via the paths:
 * <pre>{@code
 *   //ldmlBCP47/keyword/key[@name=*]/type[@name=*]
 * }</pre>
 */
final class DayPeriodsVisitor implements PrefixVisitor {
    private static final PathMatcher RULESET =
        PathMatcher.of("supplementalData/dayPeriodRuleSet");
    private static final AttributeKey RULESET_TYPE = keyOf("dayPeriodRuleSet", "type");

    private static final PathMatcher RULES = PathMatcher.of("dayPeriodRules[@locales=*]");
    private static final AttributeKey RULES_LOCALES = keyOf("dayPeriodRules", "locales");

    private static final PathMatcher RULE = PathMatcher.of("dayPeriodRule[@type=*]");
    private static final AttributeKey RULE_TYPE = keyOf("dayPeriodRule", "type");

    /** Processes data from the given supplier to generate day period data. */
    public static IcuData process(CldrDataSupplier supplier) {
        DayPeriodsVisitor v = new DayPeriodsVisitor();
        CldrData data = supplier.getDataForType(CldrDataType.SUPPLEMENTAL);
        data.accept(CldrData.PathOrder.ARBITRARY, v);
        return v.icuData;
    }

    // Mutable ICU data collected into during visitation.
    private final IcuData icuData = new IcuData("dayPeriods.xml", "dayPeriods", false);
    private int setNum = 0;

    @Override
    public void visitPrefixStart(CldrPath prefix, Context ctx) {
        if (RULESET.matches(prefix)) {
            ctx.install(new RuleVisitor(RULESET_TYPE.optionalValueFrom(prefix)));
        }
    }

    private final class RuleVisitor implements PrefixVisitor {
        private final String localePrefix;

        private RuleVisitor(Optional<String> type) {
            this.localePrefix = type.map(t -> "/locales_" + t + "/").orElse("/locales/");
        }

        @Override
        public void visitPrefixStart(CldrPath prefix, Context ctx) {
            if (RULES.matchesSuffixOf(prefix)) {
                setNum++;
                RULES_LOCALES.listOfValuesFrom(prefix)
                    .forEach(locale -> icuData.add(localePrefix + locale, "set" + setNum));
                ctx.install(this::visitRule);
            }
        }

        private void visitRule(CldrValue value) {
            if (RULE.matchesSuffixOf(value.getPath())) {
                String rulePrefix = "/rules/set" + setNum + "/" + RULE_TYPE.valueFrom(value) + "/";
                value.getValueAttributes()
                    .forEach((k, v) -> icuData.add(rulePrefix + k.getAttributeName(), v));
            }
        }
    }
}
