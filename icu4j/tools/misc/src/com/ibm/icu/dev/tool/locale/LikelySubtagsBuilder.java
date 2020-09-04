// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.tool.locale;

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
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.impl.locale.XCldrStub.HashMultimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimaps;
import com.ibm.icu.impl.locale.XLikelySubtags;
import com.ibm.icu.util.BytesTrieBuilder;
import com.ibm.icu.util.ICUException;

/**
 * Builds data for XLikelySubtags.
 * Reads source data from ICU resource bundles.
 */
public class LikelySubtagsBuilder {
    private static final boolean DEBUG_OUTPUT = LSR.DEBUG_OUTPUT;

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
                if (aliasFrom.contains("_") || aliasFrom.contains("-")) {
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
        int length = 0;
        BytesTrieBuilder tb = new BytesTrieBuilder();

        void addValue(int value) {
            assert value >= 0;
            tb.add(bytes, length, value);
        }

        void addStar() {
            bytes[length++] = '*';
        }

        void addSubtag(String s) {
            assert !s.isEmpty();
            assert !s.equals("*");
            int end = s.length() - 1;
            for (int i = 0;; ++i) {
                char c = s.charAt(i);
                assert c <= 0x7f;
                if (i < end) {
                    bytes[length++] = (byte) c;
                } else {
                    // Mark the last character as a terminator to avoid overlap matches.
                    bytes[length++] = (byte) (c | 0x80);
                    break;
                }
            }
        }

        byte[] build() {
            ByteBuffer buffer = tb.buildByteBuffer(BytesTrieBuilder.Option.SMALL);
            // Allocate an array with just the necessary capacity,
            // so that we do not hold on to a larger array for a long time.
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            if (DEBUG_OUTPUT) {
                System.out.println("likely subtags trie size: " + bytes.length + " bytes");
            }
            return bytes;
        }
    }

    // VisibleForTesting
    public static XLikelySubtags.Data build() {
        AliasesBuilder languageAliasesBuilder = new AliasesBuilder("language");
        AliasesBuilder regionAliasesBuilder = new AliasesBuilder("territory");

        Map<String, Map<String, Map<String, LSR>>> langTable =
                makeTable(languageAliasesBuilder, regionAliasesBuilder);

        TrieBuilder trieBuilder = new TrieBuilder();
        Map<LSR, Integer> lsrIndexes = new LinkedHashMap<>();
        // Reserve index 0 as "no value":
        // The runtime lookup returns 0 for an intermediate match with no value.
        lsrIndexes.put(new LSR("", "", "", LSR.DONT_CARE_FLAGS), 0);  // arbitrary LSR
        // Reserve index 1 for SKIP_SCRIPT:
        // The runtime lookup returns 1 for an intermediate match with a value.
        // This LSR looks good when printing the data.
        lsrIndexes.put(new LSR("skip", "script", "", LSR.DONT_CARE_FLAGS), 1);
        // We could prefill the lsrList with common locales to give them small indexes,
        // and see if that improves performance a little.
        for (Map.Entry<String, Map<String, Map<String, LSR>>> ls :  langTable.entrySet()) {
            trieBuilder.length = 0;
            String lang = ls.getKey();
            if (lang.equals("und")) {
                trieBuilder.addStar();
            } else {
                trieBuilder.addSubtag(lang);
            }
            Map<String, Map<String, LSR>> scriptTable = ls.getValue();
            boolean skipScript = false;
            if (scriptTable.size() == 1) {
                Map<String, LSR> regionTable = scriptTable.get("");
                if (regionTable.size() == 1) {
                    // Prune the script and region levels from language with
                    // only * for scripts and regions.
                    int i = uniqueIdForLsr(lsrIndexes, regionTable.get(""));
                    trieBuilder.addValue(i);
                    continue;
                } else {
                    // Prune the script level from language with only * for scripts
                    // but with real regions.
                    // Set an intermediate value as a signal to the lookup code.
                    trieBuilder.addValue(XLikelySubtags.SKIP_SCRIPT);
                    skipScript = true;
                }
            }
            int scriptStartLength = trieBuilder.length;
            for (Map.Entry<String, Map<String, LSR>> sr :  scriptTable.entrySet()) {
                trieBuilder.length = scriptStartLength;
                if (!skipScript) {
                    String script = sr.getKey();
                    if (script.isEmpty()) {
                        trieBuilder.addStar();
                    } else {
                        trieBuilder.addSubtag(script);
                    }
                }
                Map<String, LSR> regionTable = sr.getValue();
                if (regionTable.size() == 1) {
                    // Prune the region level from language+script with only * for regions.
                    int i = uniqueIdForLsr(lsrIndexes, regionTable.get(""));
                    trieBuilder.addValue(i);
                    continue;
                }
                int regionStartLength = trieBuilder.length;
                for (Map.Entry<String, LSR> r2lsr :  regionTable.entrySet()) {
                    trieBuilder.length = regionStartLength;
                    String region = r2lsr.getKey();
                    // Map the whole lang+script+region to a unique, dense index of the LSR.
                    if (region.isEmpty()) {
                        trieBuilder.addStar();
                    } else {
                        trieBuilder.addSubtag(region);
                    }
                    int i = uniqueIdForLsr(lsrIndexes, r2lsr.getValue());
                    trieBuilder.addValue(i);
                }
            }
        }
        byte[] trie = trieBuilder.build();
        LSR[] lsrs = lsrIndexes.keySet().toArray(new LSR[lsrIndexes.size()]);
        return new XLikelySubtags.Data(
                languageAliasesBuilder.toCanonical, regionAliasesBuilder.toCanonical, trie, lsrs);
    }

    private static int uniqueIdForLsr(Map<LSR, Integer> lsrIndexes, LSR lsr) {
        Integer index = lsrIndexes.get(lsr);
        if (index != null) {
            return index.intValue();
        } else {
            int i = lsrIndexes.size();
            lsrIndexes.put(lsr, i);
            return i;
        }
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
            set(result, language, script, region, ltp);

            // now add aliases
            Collection<String> languageAliases = languageAliasesBuilder.getAliases(language);
            Collection<String> regionAliases = regionAliasesBuilder.getAliases(region);
            for (String languageAlias : languageAliases) {
                for (String regionAlias : regionAliases) {
                    if (languageAlias.equals(language) && regionAlias.equals(region)) {
                        continue;
                    }
                    set(result, languageAlias, script, regionAlias, ltp);
                }
            }
        }
        // hack
        set(result, "und", "Latn", "", new LSR("en", "Latn", "US", LSR.DONT_CARE_FLAGS));

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
        return p2.length() < 4 ?
                new LSR(lang, "", p2, LSR.DONT_CARE_FLAGS) :
                new LSR(lang, p2, p3, LSR.DONT_CARE_FLAGS);
    }

    private static void set(Map<String, Map<String, Map<String, LSR>>> langTable,
            final String language, final String script, final String region, LSR newValue) {
        Map<String, Map<String, LSR>> scriptTable = getSubtable(langTable, language);
        Map<String, LSR> regionTable = getSubtable(scriptTable, script);
        regionTable.put(region, newValue);
    }

    private static <K, V, T> Map<V, T> getSubtable(Map<K, Map<V, T>> table, final K subtag) {
        Map<V, T> subTable = table.get(subtag);
        if (subTable == null) {
            table.put(subtag, subTable = new TreeMap<>());
        }
        return subTable;
    }
}
