// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.locale.XCldrStub.HashMultimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimaps;
import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.BytesTrieBuilder;
import com.ibm.icu.util.ICUException;

/**
 * Builds data for XLikelySubtags.
 * Reads source data from ICU resource bundles.
 */
class LikelySubtagsBuilder {
    private static final boolean DEBUG_OUTPUT = false;

    private static ICUResourceBundle getSupplementalDataBundle(String name) {
        return ICUResourceBundle.getBundleInstance(
            ICUData.ICU_BASE_NAME, name,
            ICUResourceBundle.ICU_DATA_CLASS_LOADER, ICUResourceBundle.OpenType.DIRECT);
    }

    private static final class AliasesBuilder {
        final Map<String, String> toCanonical = new HashMap<>();
        final Multimap<String, String> toAliases;

        public Set<String> getAliases(String canonical) {
            Set<String> aliases = toAliases.get(canonical);
            return aliases == null ? Collections.singleton(canonical) : aliases;
        }

        public AliasesBuilder(String type) {
            ICUResourceBundle metadata = getSupplementalDataBundle("metadata");
            UResource.Value value = metadata.getValueWithFallback("alias/" + type);
            UResource.Table aliases = value.getTable();
            UResource.Key key = new UResource.Key();
            for (int i = 0; aliases.getKeyAndValue(i, key, value); ++i) {
                String aliasFrom = key.toString();
                if (aliasFrom.contains("_")) {
                    continue; // only simple aliasing
                }
                UResource.Table table = value.getTable();
                if (table.findValue("reason", value) && value.getString().equals("overlong")) {
                    continue;
                }
                if (!table.findValue("replacement", value)) {
                    continue;
                }
                String aliasTo = value.getString();
                int spacePos = aliasTo.indexOf(' ');
                String aliasFirst = spacePos < 0 ? aliasTo : aliasTo.substring(0, spacePos);
                if (aliasFirst.contains("_")) {
                    continue; // only simple aliasing
                }
                toCanonical.put(aliasFrom, aliasFirst);
            }
            if (type.equals("language")) {
                toCanonical.put("mo", "ro"); // special case
            }
            toAliases = Multimaps.invertFrom(toCanonical, HashMultimap.<String, String>create());

            if (DEBUG_OUTPUT) {
                System.out.println("*** " + type + " aliases");
                for (Map.Entry<String, String> mapping : new TreeMap<>(toCanonical).entrySet()) {
                    System.out.println(mapping);
                }
            }
        }
    }

    private static final class TrieBuilder {
        byte[] bytes = new byte[24];
        BytesTrieBuilder tb = new BytesTrieBuilder();

        void addMapping(String s, int value) {
            // s contains only ASCII characters.
            s.getBytes(0, s.length(), bytes, 0);
            tb.add(bytes, s.length(), value);
        }

        BytesTrie build() {
            ByteBuffer buffer = tb.buildByteBuffer(BytesTrieBuilder.Option.SMALL);
            // Allocate an array with just the necessary capacity,
            // so that we do not hold on to a larger array for a long time.
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            if (DEBUG_OUTPUT) {
                System.out.println("likely subtags trie size: " + bytes.length + " bytes");
            }
            return new BytesTrie(bytes, 0);
        }
    }

    static XLikelySubtags.Data build() {
        AliasesBuilder languageAliasesBuilder = new AliasesBuilder("language");
        AliasesBuilder regionAliasesBuilder = new AliasesBuilder("territory");

        Map<String, Map<String, Map<String, LSR>>> langTable =
                makeTable(languageAliasesBuilder, regionAliasesBuilder);

        TrieBuilder trieBuilder = new TrieBuilder();
        Map<LSR, Integer> lsrIndexes = new LinkedHashMap<>();
        // Bogus LSR at index 0 for some code to easily distinguish between
        // intermediate match points and real result values.
        LSR bogus = new LSR("", "", "");
        lsrIndexes.put(bogus, 0);
        // We could prefill the lsrList with common locales to give them small indexes,
        // and see if that improves performance a little.
        for (Map.Entry<String, Map<String, Map<String, LSR>>> ls :  langTable.entrySet()) {
            String lang = ls.getKey();
            if (lang.equals("und")) {
                lang = "*";
            }
            // Create a match point for the language.
            trieBuilder.addMapping(lang, 0);
            Map<String, Map<String, LSR>> scriptTable = ls.getValue();
            for (Map.Entry<String, Map<String, LSR>> sr :  scriptTable.entrySet()) {
                String script = sr.getKey();
                if (script.isEmpty()) {
                    script = "*";
                }
                // Match point for lang+script.
                trieBuilder.addMapping(lang + script, 0);
                Map<String, LSR> regionTable = sr.getValue();
                for (Map.Entry<String, LSR> r2lsr :  regionTable.entrySet()) {
                    String region = r2lsr.getKey();
                    if (region.isEmpty()) {
                        region = "*";
                    }
                    // Map the whole lang+script+region to a unique, dense index of the LSR.
                    LSR lsr = r2lsr.getValue();
                    Integer index = lsrIndexes.get(lsr);
                    int i;
                    if (index != null) {
                        i = index.intValue();
                    } else {
                        i = lsrIndexes.size();
                        lsrIndexes.put(lsr, i);
                    }
                    trieBuilder.addMapping(lang + script + region, i);
                }
            }
        }
        BytesTrie trie = trieBuilder.build();
        LSR[] lsrs = lsrIndexes.keySet().toArray(new LSR[lsrIndexes.size()]);
        return new XLikelySubtags.Data(
                languageAliasesBuilder.toCanonical, regionAliasesBuilder.toCanonical, trie, lsrs);
    }

    private static Map<String, Map<String, Map<String, LSR>>> makeTable(
            AliasesBuilder languageAliasesBuilder, AliasesBuilder regionAliasesBuilder) {
        Map<String, Map<String, Map<String, LSR>>> result = new TreeMap<>();
        // set the base data
        ICUResourceBundle likelySubtags = getSupplementalDataBundle("likelySubtags");
        UResource.Value value = likelySubtags.getValueWithFallback("");
        UResource.Table table = value.getTable();
        UResource.Key key = new UResource.Key();
        for (int i = 0; table.getKeyAndValue(i, key, value); ++i) {
            LSR ltp = lsrFromLocaleID(key.toString());  // source
            final String language = ltp.language;
            final String script = ltp.script;
            final String region = ltp.region;

            ltp = lsrFromLocaleID(value.getString());  // target
            String languageTarget = ltp.language;
            final String scriptTarget = ltp.script;
            final String regionTarget = ltp.region;

            set(result, language, script, region, languageTarget, scriptTarget, regionTarget);
            // now add aliases
            Collection<String> languageAliases = languageAliasesBuilder.getAliases(language);
            Collection<String> regionAliases = regionAliasesBuilder.getAliases(region);
            for (String languageAlias : languageAliases) {
                for (String regionAlias : regionAliases) {
                    if (languageAlias.equals(language) && regionAlias.equals(region)) {
                        continue;
                    }
                    set(result, languageAlias, script, regionAlias,
                            languageTarget, scriptTarget, regionTarget);
                }
            }
        }
        // hack
        set(result, "und", "Latn", "", "en", "Latn", "US");

        // hack, ensure that if und-YY => und-Xxxx-YY, then we add Xxxx=>YY to the table
        // <likelySubtag from="und_GH" to="ak_Latn_GH"/>

        // so und-Latn-GH   =>  ak-Latn-GH
        Map<String, Map<String, LSR>> undScriptMap = result.get("und");
        Map<String, LSR> undEmptyRegionMap = undScriptMap.get("");
        for (Map.Entry<String, LSR> regionEntry : undEmptyRegionMap.entrySet()) {
            final LSR lsr = regionEntry.getValue();
            set(result, "und", lsr.script, lsr.region, lsr);
        }
        //
        // check that every level has "" (or "und")
        if (!result.containsKey("und")) {
            throw new IllegalArgumentException("failure: base");
        }
        for (Map.Entry<String, Map<String, Map<String, LSR>>> langEntry : result.entrySet()) {
            String lang = langEntry.getKey();
            final Map<String, Map<String, LSR>> scriptMap = langEntry.getValue();
            if (!scriptMap.containsKey("")) {
                throw new IllegalArgumentException("failure: " + lang);
            }
            for (Map.Entry<String, Map<String, LSR>> scriptEntry : scriptMap.entrySet()) {
                String script = scriptEntry.getKey();
                final Map<String, LSR> regionMap = scriptEntry.getValue();
                if (!regionMap.containsKey("")) {
                    throw new IllegalArgumentException("failure: " + lang + "-" + script);
                }
            }
        }
        return result;
    }

    // Parses locale IDs in the likelySubtags data, not arbitrary language tags.
    private static LSR lsrFromLocaleID(String languageIdentifier) {
        String[] parts = languageIdentifier.split("[-_]");
        if (parts.length < 1 || parts.length > 3) {
            throw new ICUException("too many subtags");
        }
        String lang = parts[0];
        String p2 = parts.length < 2 ? "" : parts[1];
        String p3 = parts.length < 3 ? "" : parts[2];
        return p2.length() < 4 ? new LSR(lang, "", p2) : new LSR(lang, p2, p3);
    }

    private static void set(Map<String, Map<String, Map<String, LSR>>> langTable,
            final String language, final String script, final String region,
            final String languageTarget, final String scriptTarget, final String regionTarget) {
        LSR target = new LSR(languageTarget, scriptTarget, regionTarget);
        set(langTable, language, script, region, target);
    }

    private static void set(Map<String, Map<String, Map<String, LSR>>> langTable,
            final String language, final String script, final String region, LSR newValue) {
        Map<String, Map<String, LSR>> scriptTable = getSubtable(langTable, language);
        Map<String, LSR> regionTable = getSubtable(scriptTable, script);
        regionTable.put(region, newValue);
    }

    private static <K, V, T> Map<V, T> getSubtable(Map<K, Map<V, T>> table, final K language) {
        Map<V, T> subTable = table.get(language);
        if (subTable == null) {
            table.put(language, subTable = new TreeMap<>());
        }
        return subTable;
    }
}
