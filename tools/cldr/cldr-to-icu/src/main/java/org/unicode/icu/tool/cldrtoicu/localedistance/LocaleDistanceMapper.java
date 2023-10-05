// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.cldr.api.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.DebugWriter;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.impl.locale.LikelySubtags;
import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.lang.UScript;

import com.ibm.icu.util.ULocale;

/**
 * Mapper for generating locale distance tables from CLDR language data.
 *
 * <p>Note that this is an atypical mapper which does a lot more processing than other
 * ICU mapper classes and relies on several auxilliary classes (which is why it's in a
 * different package). Conceptually it's still a "mapper" though, just not a simple one.
 *
 * <p>This mapper was converted from the LocaleDistanceBuilder code in the ICU4J project.
 */
public final class LocaleDistanceMapper {
    private static final Logger logger = Logger.getLogger(LocaleDistanceMapper.class.getName());

    // All the language matching data comes from the "written_new" language data in
    // "common/supplemental/languageInfo.xml".
    private static final PathMatcher WRITTEN_LANGUAGE_PREFIX =
        PathMatcher.of("//supplementalData/languageMatching/languageMatches[@type=\"written_new\"]");

    // Definitions of region containment variables used when expressing match distances. E.g.:
    // <matchVariable id="$maghreb" value="MA+DZ+TN+LY+MR+EH"/>
    private static final PathMatcher VARIABLE_PATH =
        WRITTEN_LANGUAGE_PREFIX.withSuffix("matchVariable[@id=*]");
    private static final AttributeKey VARIABLE_ID = AttributeKey.keyOf("matchVariable", "id");
    private static final AttributeKey VARIABLE_VALUE = AttributeKey.keyOf("matchVariable", "value");

    // Language distance data, including wildcards and variable references (possibly negated). E.g.:
    // <languageMatch desired="ja_Latn"       supported="ja_Jpan"       distance="5" oneway="true"/>
    // <languageMatch desired="ar_*_$maghreb" supported="ar_*_$maghreb" distance="4"/>
    // <languageMatch desired="en_*_$!enUS"   supported="en_*_GB"       distance="3"/>
    private static final PathMatcher LANGUAGE_MATCH_PATH =
        WRITTEN_LANGUAGE_PREFIX.withSuffix("languageMatch[@desired=*][@supported=*]");
    private static final AttributeKey MATCH_DESIRED =
        AttributeKey.keyOf("languageMatch", "desired");
    private static final AttributeKey MATCH_SUPPORTED =
        AttributeKey.keyOf("languageMatch", "supported");
    private static final AttributeKey MATCH_DISTANCE =
        AttributeKey.keyOf("languageMatch", "distance");
    // Optional, assume false if not present.
    private static final AttributeKey MATCH_ONEWAY =
        AttributeKey.keyOf("languageMatch", "oneway");

    // Singleton element containing the list of special case "paradigm" locales, which should
    // always be preferred if there is a tie. E.g.:
    // <paradigmLocales locales="en en_GB es es_419 pt_BR pt_PT"/>
    //
    // Since there are no distinguishing attributes for this path, there can only be one
    // instance which we can just lookup directly.
    private static final CldrPath PARADIGM_LOCALES_PATH = CldrPath.parseDistinguishingPath(
        "//supplementalData/languageMatching/languageMatches[@type=\"written_new\"]/paradigmLocales");
    private static final AttributeKey PARADIGM_LOCALES =
        AttributeKey.keyOf("paradigmLocales", "locales");

    // NOTE: You must omit empty strings, since otherwise " foo " becomes ("", "foo", "").
    private static final Splitter LIST_SPLITTER =
            Splitter.on(' ').trimResults().omitEmptyStrings();

    // Output resource bundle paths, split into two basic groups for likely locale mappings
    // and match data.
    private static final RbPath LIKELY_LANGUAGES = RbPath.of("likely", "languageAliases");
    private static final RbPath LIKELY_M49 = RbPath.of("likely", "m49");
    private static final RbPath LIKELY_REGIONS = RbPath.of("likely", "regionAliases");
    private static final RbPath LIKELY_TRIE = RbPath.of("likely", "trie:bin");
    private static final RbPath LIKELY_LSRNUM = RbPath.of("likely", "lsrnum:intvector");

    private static final RbPath MATCH_TRIE = RbPath.of("match", "trie:bin");
    private static final RbPath MATCH_REGION_TO_PARTITIONS = RbPath.of("match", "regionToPartitions:bin");
    private static final RbPath MATCH_PARTITIONS = RbPath.of("match", "partitions");
    private static final RbPath MATCH_PARADIGMNUM = RbPath.of("match", "paradigmnum:intvector");
    private static final RbPath MATCH_DISTANCES = RbPath.of("match", "distances:intvector");

    // To split locale specifications (e.g. "ja_Latn" or "en_*_$!enUS").
    private static final Splitter UNDERSCORE = Splitter.on('_');

    // The encoding scheme allow us to only encode up to 27 M.49 code below.
    // The size is later check while reading the M49 List.
    private static final List<String> M49 = Arrays.asList("001", "143", "419");

    /**
     * Processes data from the given supplier to generate locale matcher ICU data.
     *
     * @param src the CLDR data supplier to process.
     * @return the IcuData instance to be written to a file.
     */
    public static IcuData process(CldrDataSupplier src) {
        return process(src.getDataForType(SUPPLEMENTAL));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(CldrData data) {
        IcuData icuData = new IcuData("langInfo", false);

        if (M49.size() > 27) {
            throw new IllegalStateException(
                "The M49 list is too long. We can only encode up to 27 M49 codes.");
        }
        LikelySubtags.Data likelyData = LikelySubtagsBuilder.build(data);
        icuData.add(LIKELY_LANGUAGES, ofMapEntries(likelyData.languageAliases));
        icuData.add(LIKELY_M49, RbValue.of(M49));
        icuData.add(LIKELY_REGIONS, ofMapEntries(likelyData.regionAliases));
        icuData.add(LIKELY_TRIE, ofBytes(likelyData.trie));
        icuData.add(LIKELY_LSRNUM, ofLsrNum(asList(likelyData.lsrs)));

        LocaleDistance.Data distanceData = buildDistanceData(data);
        icuData.add(MATCH_TRIE, ofBytes(distanceData.trie));
        icuData.add(MATCH_REGION_TO_PARTITIONS, ofBytes(distanceData.regionToPartitionsIndex));
        icuData.add(MATCH_PARTITIONS, RbValue.of(distanceData.partitionArrays));
        icuData.add(MATCH_PARADIGMNUM, ofLsrNum(distanceData.paradigmLSRs));
        icuData.add(MATCH_DISTANCES, RbValue.of(Arrays.stream(distanceData.distances).mapToObj(Integer::toString)));
        return icuData;
    }

    /**
     * A simple holder for language, script and region which allows for wildcards (i.e. "*")
     * and variables to represent partitions of regions (e.g. "$enUS"). Minimal additional
     * validation is done on incoming fields as data is assumed to be correct.
     */
    private static final class LsrSpec {
        /**
         * Parse a raw specification string (e.g. "en", "ja_Latn", "*_*_*", "ar_*_$maghreb"
         * or "en_*_GB") into a structured spec. Note that if the specification string
         * contains a "bare" region (e.g. "en_*_GB") then it is registered as a variable in
         * the given RegionMapper builder, so the returned {@code LsrSpec} will be
         * {@code "en_*_$GB"}.
         */
        public static LsrSpec parse(String rawSpec, PartitionInfo.Builder rmb) {
            List<String> parts = UNDERSCORE.splitToList(rawSpec);
            checkArgument(parts.size() <= 3, "invalid raw LSR specification: %s", rawSpec);
            String language = parts.get(0);
            Optional<String> script = parts.size() > 1 ? Optional.of(parts.get(1)) : Optional.empty();
            // While parsing the region part, ensure any "bare" region subtags are converted
            // to variables (e.g. "GB" -> "$GB") and registered with the parition map.
            Optional<String> region =
                    parts.size() > 2 ? Optional.of(rmb.ensureVariable(parts.get(2))) : Optional.empty();
            return new LsrSpec(language, script, region);
        }

        // A language subtag (e.g. "en") or "*".
        private final String language;
        // If present, a script subtag (e.g. "Latn") or "*".
        private final Optional<String> script;
        // If present, a registered variable with '$' prefix (e.g. "$foo" or "$GB") or "*".
        private final Optional<String> regionVariable;

        private LsrSpec(String language, Optional<String> script, Optional<String> regionVariable) {
            this.language = language;
            this.script = script;
            this.regionVariable = regionVariable;
            // Implementation shortcuts assume:
            // - If the language subtags are '*', the other-level subtags must also be '*' (if present).
            // If there are rules that do not fit these constraints, we need to revise the implementation.
            if (isAny(language)) {
                script.ifPresent(
                        s -> checkArgument(isAny(s), "expected wildcard script, got: %s", script));
                regionVariable.ifPresent(
                        r -> checkArgument(isAny(r), "expected wildcard region, got: %s", regionVariable));
            }
        }

        public String getLanguage() {
            return language;
        }

        public String getScript() {
            return script.orElseThrow(() -> new IllegalArgumentException("no script available: " + this));
        }

        public String getRegionVariable() {
            return regionVariable.orElseThrow(() -> new IllegalArgumentException("no region available: " + this));
        }

        public int size() {
            return regionVariable.isPresent() ? 3 : script.isPresent() ? 2 : 1;
        }

        @Override
        public String toString() {
            return language + script.map(s -> "_" + s).orElse("") + regionVariable.map(r -> "_" + r).orElse("");
        }
    }

    /**
     * Represents a {@code <languageMatch>} rule derived from supplemental data, such as:
     * <pre>{@code
     *   <languageMatch desired="zh_Hans" supported="zh_Hant" distance="15" oneway="true"/>
     * }</pre>
     * or:
     * <pre>{@code
     *   <languageMatch desired="ar_*_$maghreb" supported="ar_*_$maghreb" distance="4"/>
     * }</pre>
     *
     * <p>The job of a {@code Rule} is to provide a mechanism for capturing the data in
     * {@code <languageMatch>} elements and subsequently adding that information to a
     * {@link DistanceTable.Builder} in a structured way.
     */
    private static final class LanguageMatchRule {
        private final LsrSpec desired;
        private final LsrSpec supported;
        private final int distance;
        private final boolean oneway;

        public LanguageMatchRule(LsrSpec desired, LsrSpec supported, int distance, boolean oneway) {
            this.desired = checkNotNull(desired);
            this.supported = checkNotNull(supported);
            this.distance = distance;
            this.oneway = oneway;
            // Implementation shortcuts assume:
            // - At any level, either both or neither spec subtags are *.
            // If there are rules that do not fit these constraints, we need to revise the implementation.
            checkArgument(desired.size() == supported.size(),
                    "mismatched rule specifications in: %s, %s", desired, supported);
            checkArgument(isAny(desired.language) == isAny(supported.language),
                    "wildcard mismatch for languages in: %s, %s", desired, supported);
            checkArgument(isAny(desired.script) == isAny(supported.script),
                    "wildcard mismatch for scripts in: %s, %s", desired, supported);
            checkArgument(isAny(desired.regionVariable) == isAny(supported.regionVariable),
                    "wildcard mismatch for languages in: %s, %s", desired, supported);
        }

        int size() {
            return desired.size();
        }

        boolean isDefaultRule() {
            // We already know that in LsrSpec, if the language is "*" then all subtags are too.
            return isAny(desired.language);
        }

        /**
         * Adds this rule to the given distance table, using the given partition map to
         * resolve any region variables present in the desired or supported specs.
         */
        void addTo(DistanceTable.Builder distanceTable, PartitionInfo partitions) {
            // Note that rather than using the rule's "size" to mediate the different
            // cases, we could have had 3 distinct sub-types of a common rule API (e.g.
            // "LanguageRule", "ScriptRule" and "RegionRule"), each with a different
            // addTo() callback. However this would have been quite a lot more code
            // for not much real gain.
            switch (size()) {
            case 1:  // Language only.
                distanceTable.addDistance(distance, oneway,
                        desired.getLanguage(), supported.getLanguage());
                break;

            case 2:  // Language and script present.
                distanceTable.addDistance(distance, oneway,
                        desired.getLanguage(), supported.getLanguage(),
                        desired.getScript(), supported.getScript());
                break;

            case 3:  // Language, script and region variable present.
                // Add the rule distance for every combination of desired/supported
                // partition IDs for the region variables. This is important for
                // variables like "$americas" which overlap with multiple paritions.
                //
                // Note that in this case (because region variables map to sets of
                // partition IDs) we can get situations where "shouldReverse" is true,
                // but the desired/supported pairs being passed in are identical (e.g.
                // different region variables map to distinct partition groups which
                // share some common elements).
                //
                // This is fine, providing that the distance table is going to ignore
                // identical mappings (which it does). Alternatively we could just
                // re-calculate "shouldReverse" inside this loop to account for partition
                // IDs rather than region variables.
                ImmutableSet<String> desiredPartitionIds =
                        partitions.getPartitionIds(desired.getRegionVariable());
                ImmutableSet<String> supportedPartitionIds =
                        partitions.getPartitionIds(supported.getRegionVariable());
                for (String desiredPartitionId : desiredPartitionIds) {
                    for (String supportedPartitionId : supportedPartitionIds) {
                        distanceTable.addDistance(distance, oneway,
                                desired.getLanguage(), supported.getLanguage(),
                                desired.getScript(), supported.getScript(),
                                desiredPartitionId, supportedPartitionId);
                    }
                }
                break;

            default:
                throw new IllegalStateException("invalid size for LsrSpec: " + this);
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "Rule{ desired=%s, supported=%s, distance=%d, oneway=%b }",
                    desired, supported, distance, oneway);
        }
    }

    private static LocaleDistance.Data buildDistanceData(CldrData supplementalData) {
        // Resolve any explicitly declared region variables into the partition map.
        // Territory containment information is used to recursively resolve region
        // variables (e.g. "$enUS") into a collection of non-macro regions.
        PartitionInfo.Builder partitionBuilder =
                PartitionInfo.builder(TerritoryContainment.getContainment(supplementalData));
        supplementalData.accept(DTD, v -> {
            CldrPath path = v.getPath();
            if (VARIABLE_PATH.matches(path)) {
                partitionBuilder.addVariableExpression(v.get(VARIABLE_ID), v.get(VARIABLE_VALUE));
            }
        });

        // Parse the rules from <languageMatch> elements. Note that the <languageMatch>
        // element is marked as "ORDERED" in the DTD, which means the elements always
        // appear in the same order is in the CLDR XML file (even when using DTD order).
        //
        // This is one of the relatively rare situations in which using DTD order will
        // not isolate the ICU data from reordering of the CLDR data. In particular this
        // matters when specifying language matcher preferences (such as "en_*_GB" vs
        // "en_*_!enUS").
        //
        // We could almost process the rules while reading them from the source data, but
        // rules may contain region codes rather than variables, and we need to create a
        // variable for each such region code before the RegionMapper is built, and
        // before processing the rules (this happens when the LsrSpec is parsed).
        List<LanguageMatchRule> rules = new ArrayList<>();
        supplementalData.accept(DTD, v -> {
            CldrPath path = v.getPath();
            if (LANGUAGE_MATCH_PATH.matches(path)) {
                int distance = Integer.parseInt(v.get(MATCH_DISTANCE));
                // Lenient against there being no "oneway" attribute.
                boolean oneway = "true".equalsIgnoreCase(v.get(MATCH_ONEWAY));
                LsrSpec desired = LsrSpec.parse(v.get(MATCH_DESIRED), partitionBuilder);
                LsrSpec supported = LsrSpec.parse(v.get(MATCH_SUPPORTED), partitionBuilder);
                LanguageMatchRule rule = new LanguageMatchRule(desired, supported, distance, oneway);
                logger.fine(() -> String.format("rule: %s", rule));
                rules.add(rule);
            }
        });
        // Check that the rules are in the expected order. Rule order is important in ensuring
        // data correctness and incorrect order may violate business logic assumptions later.
        // TODO: Consider what other ordering/sanity checks make sense here.
        for (int n = 0, prevSize = 1; n < rules.size(); n++) {
            LanguageMatchRule rule = rules.get(n);
            checkArgument(rule.size() >= prevSize, "<languageMatch> elements out of order at: %s", rule);
            checkArgument(rule.size() == prevSize || (n > 0 && rules.get(n - 1).isDefaultRule()),
               "missing default rule before: %s", rule);
            prevSize = rule.size();
        }
        checkState(rules.stream().distinct().count() == rules.size(), "duplicated rule in: %s", rules);

        // Build region partition data after all the variables have been accounted for
        // (including the implicit variables found while processing LsrSpecs).
        PartitionInfo partitions = partitionBuilder.build();

        // Add all the rules (in order) to the distance table.
        DistanceTable.Builder distanceTableBuilder = DistanceTable.builder();
        rules.forEach(r -> r.addTo(distanceTableBuilder, partitions));
        DistanceTable distanceTable = distanceTableBuilder.build();

        // Note: Using LocaleDistance.Data as a fairly "dumb" container for the return values
        // requires us to do slightly awkward things, like passing mutable arrays and LSR
        // instances around, but the advantage it has is that this data structure is also what's
        // used in client code, so if the likely subtags data changes, it will be a forcing
        // function to change this code.
        return new LocaleDistance.Data(
                distanceTable.getTrie().toByteArray(),
                partitions.getPartitionLookupArray(),
                partitions.getPartitionStrings(),
                getParadigmLsrs(supplementalData),
                distanceTable.getDefaultDistances());
    }

    private static Set<LSR> getParadigmLsrs(CldrData supplementalData) {
        // LinkedHashSet for stable order; otherwise a unit test is flaky.
        CldrValue cldrValue = supplementalData.get(PARADIGM_LOCALES_PATH);
        checkState(cldrValue != null,
                "<paradigmLocales> element was missing: %s", PARADIGM_LOCALES_PATH);
        String localesList = cldrValue.get(PARADIGM_LOCALES);
        checkState(localesList != null,
                "<paradigmLocales> 'locales' attribute was missing: %s", cldrValue);

        Set<LSR> paradigmLSRs = new LinkedHashSet<>();
        for (String paradigm : LIST_SPLITTER.split(localesList)) {
            LSR max = LikelySubtags.INSTANCE.makeMaximizedLsrFrom(new ULocale(paradigm), false);
            // Clear the LSR flags to make the data equality test in LocaleDistanceTest happy.
            paradigmLSRs.add(new LSR(max.language, max.script, max.region, LSR.DONT_CARE_FLAGS));
        }
        checkArgument(paradigmLSRs.size() % 2 == 0, "unpaired paradigm locales: %s", paradigmLSRs);
        return paradigmLSRs;
    }

    // Returns an RbValue serialized from a map as a sequence of alternating (key, value)
    // pairs (formatted as one pair per line in the IcuData file).
    //
    // E.g.
    // foo{
    //     key1, value1,
    //     ...
    //     keyN, valueN,
    // }
    private static RbValue ofMapEntries(Map<String, String> map) {
        return RbValue.of(
                map.entrySet().stream()
                        .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()))
                .elementsPerLine(2);
    }

    // Returns an RbValue serialized from a sequence of LSR instance as a sequence of number
    // represent (language, region, script) tuples (formatted as one number per line in the IcuData file).
    private static RbValue ofLsrNum(Collection<LSR> lsrs) {
        return RbValue.of(
                lsrs.stream()
                        .flatMapToInt(lsr -> IntStream.of(LSRToNum(lsr)))
                        .mapToObj(Integer::toString));
    }

    // This method is added only to support encodeToIntForResource()
    // It only support [a-z]{2,3} and will not work for other cases.
    // TODO(ftang) Remove after LSR.encodeToIntForResource is available to the tool.
    static private int encodeLanguageToInt(String language) {
        assert language.length() >= 2;
        assert language.length() <= 3;
        assert language.charAt(0) >= 'a';
        assert language.charAt(0) <= 'z';
        assert language.charAt(1) >= 'a';
        assert language.charAt(1) <= 'z';
        assert language.length() == 2 || language.charAt(2) >= 'a';
        assert language.length() == 2 || language.charAt(2) <= 'z';
        return language.charAt(0) - 'a' + 1 +
               27 * (language.charAt(1) - 'a' + 1) +
               ((language.length() == 2) ? 0 : 27 * 27 * (language.charAt(2) - 'a' + 1));
    }
    // This method is added only to support encodeToIntForResource()
    // It only support [A-Z][a-z]{3} which defined in UScript and does not work for other cases.
    // TODO(ftang) Remove after LSR.encodeToIntForResource is available to the tool.
    static private int encodeScriptToInt(String script) {
        int ret = UScript.getCodeFromName(script);
        assert ret != UScript.INVALID_CODE;
        return ret;
    }
    // This method is added only to support encodeToIntForResource()
    // It only support [A-Z]{2}|001|143|419 and does not work for other cases.
    // TODO(ftang) Remove after LSR.encodeToIntForResource is available to the tool.
    static private int encodeRegionToInt(String region, List<String> m49) {
        assert region.length() >= 2;
        assert region.length() <= 3;
        // Do not have enough bits to store the all 1000 possible combination of \d{3}
        // Only support what is in M49.
        if (region.length() == 3) {
            int index = m49.indexOf(region);
            assert index >= 0;
            if (index < 0) {
                throw new IllegalStateException(
                    "Please add '" + region + "' to M49 in LocaleDistanceMapper.java");
            }
            return index;
        }
        assert region.charAt(0) >= 'A';
        assert region.charAt(0) <= 'Z';
        assert region.charAt(1) >= 'A';
        assert region.charAt(1) <= 'Z';
        // 'AA' => 1+27*1  = 28
        // ...
        // 'AZ' => 1+27*26 = 703
        // 'BA' => 2+27*1  = 29
        // ...
        // 'IN' => 9+27*14 = 387
        // 'ZZ' => 26+27*26 = 728
        return (region.charAt(0) - 'A' + 1) + 27 * (region.charAt(1) - 'A' + 1);
    }
    // This is designed to only support encoding some LSR into resources but not for other cases.
    // TODO(ftang) Remove after LSR.encodeToIntForResource is available to the tool.
    static int encodeToIntForResource(LSR lsr) {
        return (encodeLanguageToInt(lsr.language) + (27*27*27) * encodeRegionToInt(lsr.region, M49)) |
            (encodeScriptToInt(lsr.script) << 24);
    }

    private static int LSRToNum(LSR lsr) {
        // Special number for "", "", "" return 0
        if (lsr.language.isEmpty() && lsr.script.isEmpty() && lsr.region.isEmpty()) {
            return 0;
        }
        // Special number for "skip", "script", "" return 1
        if (lsr.language.equals("skip") && lsr.script.equals("script") && lsr.region.isEmpty()) {
            return 1;
        }
        // TODO(ftang) Change to the following line after LSR.encodeToIntForResource is available to the tool.
        // return lsr.encodeToIntForResource();
        return encodeToIntForResource(lsr);
    }

    // Returns an RbValue serialized from a byte array, as a concatenated sequence of rows of
    // hex values. This is intended only for RbPaths using the ":bin" suffix.
    //
    // E.g.
    // foo{
    // 0123456789abcdef0123456789abcdef
    //     ...
    // 1c0de4c0ffee
    // }
    //
    // Note that typically no indentation is used when writting this binary "blob".
    private static RbValue ofBytes(byte[] data) {
        ImmutableList.Builder<String> hexValues = ImmutableList.builder();
        List<Byte> bytes = Bytes.asList(data);
        for (List<Byte> line : Iterables.partition(bytes, 16)) {
            hexValues.add(line.stream().map(b -> String.format("%02x", b)).collect(Collectors.joining()));
        }
        return RbValue.of(hexValues.build());
    }

    // Returns if the subtag is the '*' wildcard. This is not to be confused with the
    // "ANY" character used in DistanceTable.
    private static boolean isAny(String subtag) {
        return subtag.equals("*");
    }

    // Returns if the subtag exists and is the '*' wildcard.
    private static boolean isAny(Optional<String> subtag) {
        return subtag.map(LocaleDistanceMapper::isAny).orElse(false);
    }

    // Main method for running this mapper directly with logging enabled.
    // CLDR_DIR is picked up from system properties or envirnment variables.
    // Arguments: <output-file> [<log-level>]
    public static void main(String[] args) throws IOException {
        DebugWriter.writeForDebugging(args, LocaleDistanceMapper::process);
    }
}
