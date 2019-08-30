// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * A mapper to collect plural data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data via
 * the paths:
 * <pre>{@code
 *   //supplementalData/plurals[@type=*]/pluralRules[@locales=*]/pluralRule[@count=*]
 * }</pre>
 */
public final class PluralsMapper {
    private static final PathMatcher PLURALS = PathMatcher.of("supplementalData/plurals[@type=*]");
    private static final AttributeKey PLURALS_TYPE = keyOf("plurals", "type");

    private static final PathMatcher RULES = PathMatcher.of("pluralRules[@locales=*]");
    private static final AttributeKey RULES_LOCALES = keyOf("pluralRules", "locales");

    private static final PathMatcher RULE = PathMatcher.of("pluralRule[@count=*]");
    private static final AttributeKey RULE_COUNT = keyOf("pluralRule", "count");

    private static final ImmutableMap<String, RbPath> ICU_PREFIX_MAP =
        ImmutableMap.of("cardinal", RbPath.of("locales"), "ordinal", RbPath.of("locales_ordinals"));

    /**
     * Processes data from the given supplier to generate plural ICU data.
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
        PluralsVisitor visitor = new PluralsVisitor();
        // Note: We explicitly reset the type to mimic the order of the existing code, since this
        // affects the set indices we generate during processing. Ideally this would all be immune
        // to ordering (or just enforce DTD ordering) but right now it's very dependent on
        // mimicking the order of the existing code to get identical output.
        data.accept(ARBITRARY, visitor.setType("cardinal"));
        data.accept(ARBITRARY, visitor.setType("ordinal"));
        return visitor.icuData;
    }

    private static final class PluralsVisitor implements PrefixVisitor {
        // Mutable ICU data collected into during visitation.
        // In a post XML-aware API, is recording the XML file names really a good idea?
        private final IcuData icuData = new IcuData("plurals", false);
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
                // Note: "plurals:type" is an optional attribute but the CLDR DTD specifies a
                // default via comments. It should probably be changed to just have a default in
                // the DTD.
                if (PLURALS_TYPE.valueFrom(prefix, "cardinal").equals(type)) {
                    ctx.install(new RulesVisitor(ICU_PREFIX_MAP.get(type)));
                }
            }
        }

        private final class RulesVisitor implements PrefixVisitor {
            private final RbPath icuPrefix;
            private final List<String> locales = new ArrayList<>();
            private final Map<String, String> rules = new LinkedHashMap<>();

            RulesVisitor(RbPath icuPrefix) {
                this.icuPrefix = checkNotNull(icuPrefix);
            }

            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                if (RULES.matchesSuffixOf(prefix)) {
                    Iterables.addAll(locales, RULES_LOCALES.listOfValuesFrom(prefix));
                    ctx.install(value -> {
                        if (RULE.matchesSuffixOf(value.getPath())) {
                            rules.put(RULE_COUNT.valueFrom(value), value.getValue());
                        }
                    });
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

                // Have we seen this set of rules before? If so, reuse the existing index. Note
                // that an IDE might report this call as suspicious because the key is not yet an
                // immutable map (saves creating immutable maps just to check for inclusion) but
                // this is fine because collection equality is based only on contents, not
                // collection type.
                int idx = previousRules.indexOf(rules);
                if (idx == -1) {
                    int newIdx = previousRules.size();
                    rules.forEach((k, v) -> icuData.add(RbPath.of("rules", "set" + newIdx, k), v));
                    // Since "rules" is mutable and reused, we must take an immutable copy here.
                    previousRules.add(ImmutableMap.copyOf(rules));
                    idx = newIdx;
                }
                String setName = "set" + idx;
                locales.forEach(locale -> icuData.add(icuPrefix.extendBy(locale), setName));
                rules.clear();
                locales.clear();
            }
        }
    }

    private PluralsMapper() {}
}
