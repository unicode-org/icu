package com.ibm.icu.dev.tool.cldr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.AttributeKey.keyOf;

/**
 * A visitor to collect plural data from {@code SUPPLEMENTAL} data via the paths:
 * <pre>{@code
 *   //supplementalData/plurals[@type=*]/pluralRules[@locales=*]/pluralRule[@count=*]
 * }</pre>
 */
final class PluralsVisitor implements PrefixVisitor {
    private static final PathMatcher PLURALS = PathMatcher.of("supplementalData/plurals[@type=*]");
    private static final AttributeKey PLURALS_TYPE = keyOf("plurals", "type");

    private static final PathMatcher RULES = PathMatcher.of("pluralRules[@locales=*]");
    private static final AttributeKey RULES_LOCALES = keyOf("pluralRules", "locales");

    private static final PathMatcher RULE = PathMatcher.of("pluralRule[@count=*]");
    private static final AttributeKey RULE_COUNT = keyOf("pluralRule", "count");

    private static final ImmutableMap<String, String> ICU_PREFIX_MAP =
        ImmutableMap.of("cardinal", "/locales/", "ordinal", "/locales_ordinals/");

    /** Processes data from the given supplier to generate plurals data. */
    public static IcuData process(CldrDataSupplier supplier) {
        PluralsVisitor v = new PluralsVisitor();
        CldrData data = supplier.getDataForType(CldrDataType.SUPPLEMENTAL);
        // Note: We explicitly reset the type to mimic the order of the existing code, since this
        // affects the set indices we generate during processing. Ideally this would all be immune
        // to ordering (or just enforce DTD ordering) but right now it's very dependent on
        // mimicking the order of the existing code to get identical output.
        data.accept(CldrData.PathOrder.ARBITRARY, v.setType("cardinal"));
        data.accept(CldrData.PathOrder.ARBITRARY, v.setType("ordinal"));
        return v.icuData;
    }

    // Mutable ICU data collected into during visitation.
    // In a post XML-aware API, is recording the XML file names really a good idea?
    private final IcuData icuData = new IcuData("plurals.xml, ordinals.xml", "plurals", false);
    // Filter for the type we are processing now (this could be removed if we don't mind which
    // order the types are processed, and switching to DTD ordering would make it stable).
    private String type = null;
    private final List<ImmutableMap<String, String>> previousRules = new ArrayList<>();

    // Hack method to allow a single type to be processed at a time (the visitor would otherwise
    // happily handle both types in a single pass). We can't do this as two different visitors
    // (one for each type) because the current behaviour relies on carrying over the calculated
    // set numbers from one pass to the next. Once migration is complete we should revisit this
    // and allow this visitor to work in a single pass (probably with DTD order for stability).
    PluralsVisitor setType(String type) {
        this.type = checkNotNull(type);
        return this;
    }

    @Override
    public void visitPrefixStart(CldrPath prefix, Context ctx) {
        if (PLURALS.matches(prefix)) {
            // Note: "plurals:type" is an optional attribute but the CLDR DTD specifies a default
            // via comments. It should probably be changed to just have a default in the DTD.
            if (PLURALS_TYPE.valueFrom(prefix, "cardinal").equals(type)) {
                ctx.install(new RulesVisitor(ICU_PREFIX_MAP.get(type)));
            }
        }
    }

    private final class RulesVisitor implements PrefixVisitor {
        private final String icuPrefix;
        private final List<String> locales = new ArrayList<>();

        private final Map<String, String> rules = new LinkedHashMap<>();
        private final ValueVisitor ruleCollector =
            value -> {
                if (RULE.matchesSuffixOf(value.getPath())) {
                    rules.put(RULE_COUNT.valueFrom(value), value.getValue());
                }
            };

        RulesVisitor(String icuPrefix) {
            this.icuPrefix = checkNotNull(icuPrefix);
        }

        @Override
        public void visitPrefixStart(CldrPath prefix, Context ctx) {
            if (RULES.matchesSuffixOf(prefix)) {
                Iterables.addAll(locales, RULES_LOCALES.listOfValuesFrom(prefix));
                ctx.install(ruleCollector);
            }
        }

        @Override
        public void visitPrefixEnd(CldrPath prefix) {
            checkState(!locales.isEmpty(), "missing locale data for plurals: %s", prefix);
            // Note: The original mapper code "sort of" coped with empty rules, but it's not
            // completely well behaved (or documented), so since this doesn't happen in the
            // current CLDR data, I decided to just prohibit it in the new code. Support can
            // easily be added in once the expected semantics are clear.
            checkState(!rules.isEmpty(), "missing rule data for plurals: %s", prefix);
            // Have we seen this set of rules before? If so, reuse the existing index.
            int idx = previousRules.indexOf(rules);
            if (idx == -1) {
                int newIdx = previousRules.size();
                rules.forEach((k, v) -> icuData.add("/rules/set" + newIdx + '/' + k, v));
                // Since "rules" is mutable and reused, we must take an immutable copy here.
                previousRules.add(ImmutableMap.copyOf(rules));
                idx = newIdx;
            }
            String setName = "set" + idx;
            locales.forEach(locale -> icuData.add(icuPrefix + locale, setName));
            rules.clear();
            locales.clear();
        }
    }

    private PluralsVisitor() {}
}
