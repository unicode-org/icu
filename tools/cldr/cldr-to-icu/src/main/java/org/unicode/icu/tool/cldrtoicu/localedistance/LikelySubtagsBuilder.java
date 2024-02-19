// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.impl.locale.LikelySubtags;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.PathMatcher;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.nullToEmpty;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;

/**
 * Generates likely subtag information from CLDR supplemental data.
 *
 * <p>Likely subtag information and language aliases are combined to produce a
 * Trie table of lookup data to canonicalize any incoming language ID to its
 * most likely fully qualified form.
 */
final class LikelySubtagsBuilder {
    private static final Logger logger = Logger.getLogger(LikelySubtagsBuilder.class.getName());

    private static final PathMatcher ALIAS =
        PathMatcher.of("//supplementalData/metadata/alias/*[@type=*]");

    private static final PathMatcher LIKELY_SUBTAG =
        PathMatcher.of("//supplementalData/likelySubtags/likelySubtag[@from=*]");
    private static final AttributeKey SUBTAG_FROM = AttributeKey.keyOf("likelySubtag", "from");
    private static final AttributeKey SUBTAG_TO = AttributeKey.keyOf("likelySubtag", "to");

    // NOTE: You must omit empty strings, since otherwise " foo " becomes ("", "foo", "").
    private static final Splitter LIST_SPLITTER =
            Splitter.on(' ').trimResults().omitEmptyStrings();

    // A language identifier is "xx", "xx_Yyyy", "xx_ZZ" or "xx_Yyyy_ZZ".
    private static final Pattern LOCALE_ID =
            Pattern.compile("([a-z]{2,3})(?:_([A-Z][a-z]{3}))?(?:_([A-Z]{2}|[0-9]{3}))?");

    // While likely subtags are only separated by '_', language aliases can use '-' for
    // legacy values. E.g.:
    //     <languageAlias type="zh-min" replacement="nan-x-zh-min" reason="legacy"/>
    // Territory aliases never have a separator, so are always "simple".
    private static final CharMatcher ALIAS_SEPARATOR = CharMatcher.anyOf("-_");

    // This is a bit of a hack to let this newer implementation behave exactly like the original
    // ICU4J version of the code. In particular, this version of the code normalizes the keys of
    // the LSR table to "*" earlier than before (previously the "special" keys were "und" for
    // the top-level language subtags and "" for script or region). By normalizing earlier,
    // there's no longer any reason to have special case code in the Trie logic, but if we just
    // do that, the table keys are now sorted differently.
    //
    // Normally sort order wouldn't matter, when writing the Trie, but in order to demonstrate
    // that this code produces the same binary output as before, the old ordering is replicated.
    //
    // TODO: When the dust settles, consider moving this to a star-first or star-last ordering??
    private static Comparator<String> sortingStarLike(String t) {
        return Comparator.comparing(x -> x.equals("*") ? t : x);
    }

    private static final Comparator<String> LSR_TABLE_ORDER = sortingStarLike("und");
    private static final Comparator<String> SUBTABLE_ORDER = sortingStarLike("");

    /** Possible alias types. */
    private enum AliasType {
        LANGUAGE("languageAlias"),
        TERRITORY("territoryAlias");

        private final String elementName;
        private final AttributeKey typeKey;
        private final AttributeKey reasonKey;
        private final AttributeKey replacementKey;

        AliasType(String elementName) {
            this.elementName = elementName;
            this.typeKey = AttributeKey.keyOf(elementName, "type");
            this.reasonKey = AttributeKey.keyOf(elementName, "reason");
            this.replacementKey = AttributeKey.keyOf(elementName, "replacement");
        }
    }

    /** Alias mappings for base languages and territories. */
    private static final class Aliases {
        /**
         * Returns the alias mapping for the given type. Note that for language aliases,
         * only "simple" aliases (between base languages) are mapped.
         */
        public static Aliases getAliases(CldrData supplementalData, AliasType type) {
            ImmutableSortedMap.Builder<String, String> canonicalMap =
                    ImmutableSortedMap.naturalOrder();
            supplementalData.accept(DTD, v -> {
                CldrPath path = v.getPath();
                if (ALIAS.matches(path) && path.getName().equals(type.elementName)) {
                    // TODO: Find out why we ignore "overlong" aliases?
                    String aliasFrom = v.get(type.typeKey);
                    if (isSimpleAlias(aliasFrom) && !v.get(type.reasonKey).equals("overlong")) {
                        // Replacement locale IDs must be non-empty (but can be a list) and we
                        // use only the first (default) mapping.
                        String aliasTo = LIST_SPLITTER.splitToList(v.get(type.replacementKey)).get(0);
                        if (isSimpleAlias(aliasTo)) {
                            canonicalMap.put(aliasFrom, aliasTo);
                        }
                    }
                }
            });
            return new Aliases(canonicalMap.build());
        }

        // A simple language alias references only a base language (territory alias are
        // always "simple" so this check is harmless).
        private static boolean isSimpleAlias(String localeId) {
            return ALIAS_SEPARATOR.matchesNoneOf(localeId);
        }

        private final ImmutableSortedMap<String, String> toCanonical;
        private final ImmutableSetMultimap<String, String> toAliases;

        private Aliases(ImmutableSortedMap<String, String> toCanonical) {
            this.toCanonical = checkNotNull(toCanonical);
            this.toAliases = toCanonical.asMultimap().inverse();
        }

        /** Returns the alias-to-canonical-value mapping. */
        public ImmutableSortedMap<String, String> getCanonicalMap() {
            return toCanonical;
        }

        /**
         * Returns the aliases for a given canonical value (if there are no aliases
         * then a singleton set containing the given canonical value is returned).
         */
        public ImmutableSet<String> getAliases(String canonical) {
            ImmutableSet<String> aliases = toAliases.get(canonical);
            return aliases.isEmpty() ? ImmutableSet.of(canonical) : aliases;
        }
    }

    public static LikelySubtags.Data build(CldrData supplementalData) {
        // Build the table of LSR data from CLDR aliases and likely subtag information.
        Aliases languageAliases = Aliases.getAliases(supplementalData, AliasType.LANGUAGE);
        Aliases regionAliases = Aliases.getAliases(supplementalData, AliasType.TERRITORY);
        Map<String, Map<String, Map<String, LSR>>> lsrTable =
            makeTable(languageAliases, regionAliases, supplementalData);

        // In the output Trie we must reference LSR instance by their special index
        // (which is calculated by client code in order to lookup values).
        //
        // Note: We could pre-load this indexer with common locales to give them small
        // indices, and see if that improves performance a little.
        Indexer<LSR, Integer> lsrToIndex = Indexer.create();

        // Reserve index 0 as "no value":
        // The runtime lookup returns 0 for an intermediate match with no value, so we
        // need that index to be reserved by something (but the value is arbitrary).
        lsrToIndex.apply(lsr("", "", ""));
        // Reserve index 1 for SKIP_SCRIPT:
        // The runtime lookup returns 1 for an intermediate match with a value.
        // This value is also arbitrary so use a value that is easy to debug.
        lsrToIndex.apply(lsr("skip", "script", ""));

        // Build the Trie of the LSR table data.
        Trie trie = writeLsrTable(lsrTable, lsrToIndex);

        // Note: Using LikelySubtags as a fairly "dumb" container for the return values
        // requires us to do slightly awkward things like passing mutable arrays around, but
        // the advantage it has is that this data structure is also what's used in client code,
        // so if the likely subtags data changes, it will be a forcing function to change this
        // code.
        return new LikelySubtags.Data(
                languageAliases.getCanonicalMap(),
                regionAliases.getCanonicalMap(),
                trie.toByteArray(),
                lsrToIndex.getValues().toArray(new LSR[0]));
    }

    private static Trie writeLsrTable(
            Map<String, Map<String, Map<String, LSR>>> languages,
            Indexer<LSR, Integer> lsrToIndex) {

        Trie trie = new Trie();
        Trie.Span rootSpan = trie.root();
        languages.forEach(
                (language, scripts) -> rootSpan.with(
                        language,
                        span -> writeScripts(span, scripts, lsrToIndex)));
        return trie;
    }

    private static void writeScripts(
            Trie.Span languageSpan, Map<String, Map<String, LSR>> scripts, Indexer<LSR, Integer> lsrToIndex) {
        checkArgument(!scripts.isEmpty(), "invalid script table: %s", scripts);
        // If we only have '*' for scripts, but there is more than one region then we can prune
        // the Trie at the script level and just write "<language><region>:<value>". However in
        // order to let the lookup code know that it should not expect a script prefix for the
        // following entries, we must add the special "skip" value before writing the regions.
        //
        // However if there is also only one region, we can just write "<language>:<value>" and
        // must avoid adding the "skip" value.
        if (scripts.size() == 1) {
            // We already checked '*' is in every scripts table.
            Map<String, LSR> regions = scripts.get("*");
            if (regions.size() > 1) {
                languageSpan.putPrefixAndValue(LikelySubtags.SKIP_SCRIPT);
            }
            writeRegions(languageSpan, regions, lsrToIndex);
        } else {
            scripts.forEach(
                    (script, regions) -> languageSpan.with(
                            script,
                            span -> writeRegions(span, regions, lsrToIndex)));
        }
    }

    private static void writeRegions(
            Trie.Span languageOrScriptSpan, Map<String, LSR> regions, Indexer<LSR, Integer> lsrToIndex) {
        checkArgument(!regions.isEmpty(), "invalid region table: %s", regions);
        // Prune anything ending with '*' (either <language-*-*> or <language-script-*>)
        // by writing the value immediately and omitting the '*' from the Trie.
        if (regions.size() == 1) {
            // We already checked '*' is in every region table.
            languageOrScriptSpan.putPrefixAndValue(lsrToIndex.apply(regions.get("*")));
        } else {
            regions.forEach(
                    (region, lsr) -> languageOrScriptSpan.with(
                            region,
                            span -> span.putPrefixAndValue(lsrToIndex.apply(lsr))));
        }
    }

    private static Map<String, Map<String, Map<String, LSR>>> makeTable(
        Aliases languageAliases, Aliases regionAliases, CldrData supplementalData) {

        Map<String, Map<String, Map<String, LSR>>> lsrTable = new TreeMap<>(LSR_TABLE_ORDER);

        // set the base data
        supplementalData.accept(DTD, v -> {
            CldrPath path = v.getPath();
            if (LIKELY_SUBTAG.matches(path)) {
                // Add the canonical subtag mapping.
                LSR source = lsrFromLocaleID(v.get(SUBTAG_FROM));
                LSR target = lsrFromLocaleID(v.get(SUBTAG_TO));
                set(lsrTable, source, target);

                // Add all combinations of language and region aliases. This lets the
                // matcher process aliases in locales in a single step.
                for (String languageAlias : languageAliases.getAliases(source.language)) {
                    for (String regionAlias : regionAliases.getAliases(source.region)) {
                        if (languageAlias.equals(source.language) && regionAlias.equals(source.region)) {
                            continue;
                        }
                        set(lsrTable, languageAlias, source.script, regionAlias, target);
                    }
                }
            }
        });

        // Add the special case for "und-Latn" => "en-Latn-US" (which is a bit of a
        // hack for language matching).
        // TODO: Find out the history of this line and document it better.
        set(lsrTable, "und", "Latn", "", lsr("en", "Latn", "US"));
        logger.fine(lsrTable::toString);

        // Ensure that if "und-RR" => "ll-Ssss-RR", then we also add "Ssss" => "RR".
        // For example, given:
        //     <likelySubtag from="und_GH" to="ak_Latn_GH"/>
        // we add an additional mapping for "und-Latn-GH" => "ak-Latn-GH" since there
        // will be cases where the language subtag is just missing in data, but given
        // the script and region we can at least make a best guess.
        //
        // Note: We can't move this code after the checks below because it might add
        // more mappings which then need to be checked. However realistically, the only
        // time the mapping "*" -> "*" would not appear is if the likely subtag data was
        // completely broken (since it implies no region-only mappings).
        checkState(lsrTable.containsKey("*") && lsrTable.get("*").containsKey("*"),
                "missing likely subtag data (no default region mappings): %s", lsrTable);
        lsrTable.get("*").get("*").forEach((key, lsr) -> set(lsrTable, "und", lsr.script, lsr.region, lsr));

        // Check that every level has "*" (mapped from "und" or "").
        lsrTable.forEach((lang, scripts) -> {
            checkArgument(scripts.containsKey("*"), "missing likely subtag mapping for: %s", asLocale(lang));
            scripts.forEach(
                    (script, regions) -> checkArgument(regions.containsKey("*"),
                            "missing likely subtag mapping for: %s", asLocale(lang, script)));
        });
        return lsrTable;
    }

    // Converts subtable key sequence into original locale ID (for debugging).
    // asLocale("*", *", "GB") -> "und_GB"
    private static String asLocale(String... parts) {
        return String.format("%s%s%s",
                !parts[0].equals("*") ? parts[0] : "und",
                parts.length > 1 && !parts[1].equals("*") ? "_" + parts[1] : "",
                parts.length > 2 && !parts[2].equals("*") ? "_" + parts[2] : "");
    }

    private static void set(
            Map<String, Map<String, Map<String, LSR>>> langTable, LSR key, LSR newValue) {
        set(langTable, key.language, key.script, key.region, newValue);
    }

    private static void set(Map<String, Map<String, Map<String, LSR>>> langTable,
            String language, String script, String region, LSR lsr) {
        Map<String, Map<String, LSR>> scriptTable = getSubtable(langTable, subtagOrStar(language));
        Map<String, LSR> regionTable = getSubtable(scriptTable, subtagOrStar(script));
        regionTable.put(subtagOrStar(region), lsr);
    }

    private static <T> Map<String, T> getSubtable(Map<String, Map<String, T>> table, String subtag) {
        return table.computeIfAbsent(subtag, k -> new TreeMap<>(SUBTABLE_ORDER));
    }

    private static String subtagOrStar(String s) {
        checkArgument(!s.equals("*"), "language subtags should not be '*'");
        return s.equals("und") || s.isEmpty() ? "*" : s;
    }

    // Parses simple locale IDs in the <likelySubtags> data, not arbitrary language tags.
    private static LSR lsrFromLocaleID(String languageIdentifier) {
        Matcher m = LOCALE_ID.matcher(languageIdentifier);
        checkArgument(m.matches(), "invalid language identifier: %s", languageIdentifier);
        return lsr(m.group(1), m.group(2), m.group(3));
    }

    // Lenient factory method which accepts null for missing script or region (but not language).
    private static LSR lsr(String language, String script, String region) {
        return new LSR(checkNotNull(language), nullToEmpty(script), nullToEmpty(region), LSR.DONT_CARE_FLAGS);
    }
}
