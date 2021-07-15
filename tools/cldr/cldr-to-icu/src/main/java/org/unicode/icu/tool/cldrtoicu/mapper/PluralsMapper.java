// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.cldr.api.FilteredData;
import org.unicode.cldr.api.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * A mapper to collect plural data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data via
 * the paths:
 * <pre>{@code
 *   //supplementalData/plurals[@type=*]/pluralRules[@locales=*]/pluralRule[@count=*]
 * }</pre>
 */
public final class PluralsMapper {

    private static final AttributeKey PLURALS_TYPE = keyOf("plurals", "type");
    private static final AttributeKey RULES_LOCALES = keyOf("pluralRules", "locales");
    private static final AttributeKey RULE_COUNT = keyOf("pluralRule", "count");

    private static final CldrDataProcessor<PluralsMapper> CLDR_PROCESSOR;
    static {
        CldrDataProcessor.Builder<PluralsMapper> processor = CldrDataProcessor.builder();
        processor
            .addAction("//supplementalData/plurals[@type=*]", (m, p) -> m.new Plurals(p))
            .addAction("pluralRules[@locales=*]", Rules::new, Plurals::addRules)
            .addValueAction("pluralRule[@count=*]", Rules::addRule);
        CLDR_PROCESSOR = processor.build();
    }

    private static final ImmutableMap<String, RbPath> ICU_PREFIX_MAP =
        ImmutableMap.of("cardinal", RbPath.of("locales"), "ordinal", RbPath.of("locales_ordinals"));

    /**
     * Processes data from the given supplier to generate plural ICU data.
     *
     * @param src the CLDR data supplier to process.
     * @return the IcuData instance to be written to a file.
     */
    public static IcuData process(CldrDataSupplier src) {
        return process(src.getDataForType(SUPPLEMENTAL));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(CldrData data) {
        PluralsMapper mapper = new PluralsMapper();
        // Note: We explicitly filter by type to mimic the order of the existing code, since this
        // affects the set indices we generate during processing. Ideally this would all be immune
        // to ordering (or just enforce DTD ordering) but right now it's very dependent on
        // mimicking the order of the existing code to get identical output. Once DTD order is
        // everywhere, this can just be a single pass over the original data.
        CLDR_PROCESSOR.process(filterByType(data, "cardinal"), mapper, NESTED_GROUPING);
        CLDR_PROCESSOR.process(filterByType(data, "ordinal"), mapper, NESTED_GROUPING);
        return mapper.icuData;
    }

    // Mutable ICU data collected into during visitation.
    // In a post XML-aware API, is recording the XML file names really a good idea?
    private final IcuData icuData = new IcuData("plurals", false);
    private final List<ImmutableMap<String, String>> previousRules = new ArrayList<>();

    private class Plurals {
        private final RbPath icuPrefix;

        Plurals(CldrPath prefix) {
            // Note: "plurals:type" is an optional attribute but the CLDR DTD specifies a
            // default via comments. It should probably be changed to just have a default in
            // the DTD.
            this.icuPrefix = ICU_PREFIX_MAP.get(PLURALS_TYPE.valueFrom(prefix, "cardinal"));
        }

        private void addRules(Rules r) {
            ImmutableMap<String, String> rules = r.getRules();
            // Note: The original mapper code "sort of" coped with empty rules, but it's not
            // completely well behaved (or documented), so since this doesn't happen in the
            // current CLDR data, I decided to just prohibit it in the new code. Support can
            // easily be added in once the expected semantics are clear.
            checkState(!rules.isEmpty(), "missing rule data for plurals");

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
                previousRules.add(rules);
                idx = newIdx;
            }
            String setName = "set" + idx;
            r.getLocales().forEach(locale -> icuData.add(icuPrefix.extendBy(locale), setName));
        }
    }

    private static class Rules {
        private final ImmutableList<String> locales;
        private final ImmutableMap.Builder<String, String> map = ImmutableMap.builder();

        private Rules(CldrPath prefix) {
            this.locales = ImmutableList.copyOf(RULES_LOCALES.listOfValuesFrom(prefix));
            checkState(!locales.isEmpty(), "missing locale data for plurals: %s", prefix);
        }

        private void addRule(CldrValue value) {
            map.put(RULE_COUNT.valueFrom(value), value.getValue());
        }

        private ImmutableList<String> getLocales() {
            return locales;
        }

        private ImmutableMap<String, String> getRules() {
            return map.build();
        }
    }

    // A hack to allow us to process "cardinal" data before "ordinal" data (even though DTD order
    // is the other way round). Once DTD order is the only ordering used, this can be removed.
    private static CldrData filterByType(CldrData data, String pluralType) {
        PathMatcher matcher =
            PathMatcher.of("//supplementalData/plurals[@type=\"" + pluralType + "\"]");
        return new FilteredData(data) {
            @Override protected CldrValue filter(CldrValue value) {
                return matcher.matchesPrefixOf(value.getPath()) ? value : null;
            }
        };
    }
}
