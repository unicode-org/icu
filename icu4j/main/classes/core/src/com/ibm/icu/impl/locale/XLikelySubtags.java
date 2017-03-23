// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.locale.XCldrStub.HashMultimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimaps;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Minimize;
import com.ibm.icu.util.UResourceBundle;

public class XLikelySubtags {

    private static final XLikelySubtags DEFAULT = new XLikelySubtags();

    public static final XLikelySubtags getDefault() {
        return DEFAULT;
    }

    @SuppressWarnings("unchecked")
    static abstract class Maker {
        abstract <V> V make();

        public <K,V> V getSubtable(Map<K, V> langTable, final K language) {
            V scriptTable = langTable.get(language);
            if (scriptTable == null) {
                langTable.put(language, scriptTable = (V) make());
            }
            return scriptTable;
        }

        static final Maker HASHMAP = new Maker() {
            @Override
            public Map<Object,Object> make() {
                return new HashMap<Object,Object>();
            }
        };

        static final Maker TREEMAP = new Maker() {
            @Override
            public Map<Object,Object> make() {
                return new TreeMap<Object,Object>();
            }
        };
    }

    public static class Aliases {
        final Map<String, String> toCanonical;
        final Multimap<String, String> toAliases;
        public String getCanonical(String alias) {
            String canonical = toCanonical.get(alias);
            return canonical == null ? alias : canonical;
        }
        public Set<String> getAliases(String canonical) {
            Set<String> aliases = toAliases.get(canonical);
            return aliases == null ? Collections.singleton(canonical) : aliases;
        }
        public Aliases(String key) {
            UResourceBundle metadata = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME,"metadata",ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle metadataAlias = metadata.get("alias");
            UResourceBundle territoryAlias = metadataAlias.get(key);
            Map<String, String> toCanonical1 = new HashMap<String, String>();
            for ( int i = 0 ; i < territoryAlias.getSize(); i++ ) {
                UResourceBundle res = territoryAlias.get(i);
                String aliasFrom = res.getKey();
                if (aliasFrom.contains("_")) {
                    continue; // only simple aliasing
                }
                String aliasReason = res.get("reason").getString();
                if (aliasReason.equals("overlong")) {
                    continue;
                }
                String aliasTo = res.get("replacement").getString();
                int spacePos = aliasTo.indexOf(' ');
                String aliasFirst = spacePos < 0 ? aliasTo : aliasTo.substring(0, spacePos);
                if (aliasFirst.contains("_")) {
                    continue; // only simple aliasing
                }
                toCanonical1.put(aliasFrom, aliasFirst);
            }
            if (key.equals("language")) {
                toCanonical1.put("mo", "ro"); // special case
            }
            toCanonical = Collections.unmodifiableMap(toCanonical1);
            toAliases = Multimaps.invertFrom(toCanonical1, HashMultimap.<String,String>create());
        }
    }

    public static class LSR {
        public final String language;
        public final String script;
        public final String region;

        public static Aliases LANGUAGE_ALIASES = new Aliases("language");
        public static Aliases REGION_ALIASES = new Aliases("territory");

        public static LSR from(String language, String script, String region) {
            return new LSR(language, script, region);
        }

        // from http://unicode.org/reports/tr35/#Unicode_language_identifier
        // but simplified to requiring language subtag, and nothing beyond region
        // #1 is language
        // #2 is script
        // #3 is region
        //        static final String pat =
        //                "language_id = (unicode_language_subtag)"
        //                        + "(?:sep(unicode_script_subtag))?"
        //                        + "(?:sep(unicode_region_subtag))?;\n"
        //                        + "unicode_language_subtag = alpha{2,3}|alpha{5,8};\n"
        //                        + "unicode_script_subtag = alpha{4};\n"
        //                        + "unicode_region_subtag  = alpha{2}|digit{3};\n"
        //                        + "sep    = [-_];\n"
        //                        + "digit  = [0-9];\n"
        //                        + "alpha   = [A-Za-z];\n"
        //                        ;
        //        static {
        //            System.out.println(pat);
        //            System.out.println(new UnicodeRegex().compileBnf(pat));
        //        }
        //        static final Pattern LANGUAGE_PATTERN = Pattern.compile(
        //                "([a-zA-Z0-9]+)" // (?:[-_]([a-zA-Z0-9]+))?(?:[-_]([a-zA-Z0-9]+))?"
        //                //new UnicodeRegex().compileBnf(pat)
        //                );
        //
        // TODO: fix this to check for format. Not required, since this is only called internally, but safer for the future.
        static LSR from(String languageIdentifier) {
            String[] parts = languageIdentifier.split("[-_]");
            if (parts.length < 1 || parts.length > 3) {
                throw new ICUException("too many subtags");
            }
            String lang = parts[0].toLowerCase();
            String p2 = parts.length < 2 ? "": parts[1];
            String p3 = parts.length < 3 ? "": parts[2];
            return p2.length() < 4 ? new LSR(lang, "", p2) : new LSR(lang, p2, p3);

            //            Matcher matcher = LANGUAGE_PATTERN.matcher(languageIdentifier);
            //            if (!matcher.matches()) {
            //                return new LSR(matcher.group(1), matcher.group(2), matcher.group(3));
            //            }
            //            System.out.println(RegexUtilities.showMismatch(matcher, languageIdentifier));
            //            throw new ICUException("invalid language id");
        }

        public static LSR from(ULocale locale) {
            return new LSR(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(ULocale locale) {
            return fromMaximalized(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(String language, String script, String region) {
            String canonicalLanguage = LANGUAGE_ALIASES.getCanonical(language);
            // script is ok
            String canonicalRegion = REGION_ALIASES.getCanonical(region); // getCanonical(REGION_ALIASES.get(region));

            return DEFAULT.maximize(canonicalLanguage, script, canonicalRegion);
        }

        public LSR(String language, String script, String region) {
            this.language = language;
            this.script = script;
            this.region = region;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(language);
            if (!script.isEmpty()) {
                result.append('-').append(script);
            }
            if (!region.isEmpty()) {
                result.append('-').append(region);
            }
            return result.toString();
        }
        public LSR replace(String language2, String script2, String region2) {
            if (language2 == null && script2 == null && region2 == null) return this;
            return new LSR(
                    language2 == null ? language: language2,
                            script2 == null ? script : script2,
                                    region2 == null ? region : region2);
        }
        @Override
        public boolean equals(Object obj) {
            LSR other;
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && language.equals((other = (LSR) obj).language)
                    && script.equals(other.script)
                    && region.equals(other.region));
        }
        @Override
        public int hashCode() {
            return Utility.hash(language, script, region);
        }
    }

    final Map<String, Map<String, Map<String, LSR>>> langTable;

    public XLikelySubtags() {
        this(getDefaultRawData(), true);
    }

    private static Map<String, String> getDefaultRawData() {
        Map<String, String> rawData = new TreeMap<String, String>();
        UResourceBundle bundle = UResourceBundle.getBundleInstance( ICUData.ICU_BASE_NAME, "likelySubtags");
        for (Enumeration<String> enumer = bundle.getKeys(); enumer.hasMoreElements();) {
            String key = enumer.nextElement();
            rawData.put(key, bundle.getString(key));
        }
        return rawData;
    }

    public XLikelySubtags(Map<String, String> rawData, boolean skipNoncanonical) {
        this.langTable = init(rawData, skipNoncanonical);
    }

    private Map<String, Map<String, Map<String, LSR>>> init(final Map<String, String> rawData, boolean skipNoncanonical) {
        // prepare alias info. We want a mapping from the canonical form to all aliases

        //Multimap<String,String> canonicalToAliasLanguage = HashMultimap.create();
        //        getAliasInfo(LANGUAGE_ALIASES, canonicalToAliasLanguage);

        // Don't bother with script; there are none

        //Multimap<String,String> canonicalToAliasRegion = HashMultimap.create();
        //        getAliasInfo(REGION_ALIASES, canonicalToAliasRegion);

        Maker maker = Maker.TREEMAP;
        Map<String, Map<String, Map<String, LSR>>> result = maker.make();
        //        Splitter bar = Splitter.on('_');
        //        int last = -1;
        // set the base data
        Map<LSR,LSR> internCache = new HashMap<LSR,LSR>();
        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
            LSR ltp = LSR.from(sourceTarget.getKey());
            final String language = ltp.language;
            final String script = ltp.script;
            final String region = ltp.region;

            ltp = LSR.from(sourceTarget.getValue());
            String languageTarget = ltp.language;
            final String scriptTarget = ltp.script;
            final String regionTarget = ltp.region;

            set(result, language, script, region, languageTarget, scriptTarget, regionTarget, internCache);
            // now add aliases
            Collection<String> languageAliases = LSR.LANGUAGE_ALIASES.getAliases(language);
            //            if (languageAliases.isEmpty()) {
            //                languageAliases = Collections.singleton(language);
            //            }
            Collection<String> regionAliases = LSR.REGION_ALIASES.getAliases(region);
            //            if (regionAliases.isEmpty()) {
            //                regionAliases = Collections.singleton(region);
            //            }
            for (String languageAlias : languageAliases) {
                for (String regionAlias : regionAliases) {
                    if (languageAlias.equals(language) && regionAlias.equals(region)) {
                        continue;
                    }
                    set(result, languageAlias, script, regionAlias, languageTarget, scriptTarget, regionTarget, internCache);
                }
            }
        }
        // hack
        set(result, "und", "Latn", "", "en", "Latn", "US", internCache);

        // hack, ensure that if und-YY => und-Xxxx-YY, then we add Xxxx=>YY to the table
        // <likelySubtag from="und_GH" to="ak_Latn_GH"/>

        // so und-Latn-GH   =>  ak-Latn-GH
        Map<String, Map<String, LSR>> undScriptMap = result.get("und");
        Map<String, LSR> undEmptyRegionMap = undScriptMap.get("");
        for (Entry<String, LSR> regionEntry : undEmptyRegionMap.entrySet()) {
            final LSR value = regionEntry.getValue();
            set(result, "und", value.script, value.region, value);
        }
        //
        // check that every level has "" (or "und")
        if (!result.containsKey("und")) {
            throw new IllegalArgumentException("failure: base");
        }
        for (Entry<String, Map<String, Map<String, LSR>>> langEntry : result.entrySet()) {
            String lang = langEntry.getKey();
            final Map<String, Map<String, LSR>> scriptMap = langEntry.getValue();
            if (!scriptMap.containsKey("")) {
                throw new IllegalArgumentException("failure: " + lang);
            }
            for (Entry<String, Map<String, LSR>> scriptEntry : scriptMap.entrySet()) {
                String script = scriptEntry.getKey();
                final Map<String, LSR> regionMap = scriptEntry.getValue();
                if (!regionMap.containsKey("")) {
                    throw new IllegalArgumentException("failure: " + lang + "-" + script);
                }
                //                for (Entry<String, LSR> regionEntry : regionMap.entrySet()) {
                //                    String region = regionEntry.getKey();
                //                    LSR value = regionEntry.getValue();
                //                }
            }
        }
        return result;
    }

    //    private void getAliasInfo(Map<String, R2<List<String>, String>> aliasInfo, Multimap<String, String> canonicalToAlias) {
    //        for (Entry<String, R2<List<String>, String>> e : aliasInfo.entrySet()) {
    //            final String alias = e.getKey();
    //            if (alias.contains("_")) {
    //                continue; // only do simple aliasing
    //            }
    //            String canonical = getCanonical(e.getValue());
    //            canonicalToAlias.put(canonical, alias);
    //        }
    //    }

    //    private static String getCanonical(R2<List<String>, String> aliasAndReason) {
    //        if (aliasAndReason == null) {
    //            return null;
    //        }
    //        if (aliasAndReason.get1().equals("overlong")) {
    //            return null;
    //        }
    //        List<String> value = aliasAndReason.get0();
    //        if (value.size() != 1) {
    //            return null;
    //        }
    //        final String canonical = value.iterator().next();
    //        if (canonical.contains("_")) {
    //            return null; // only do simple aliasing
    //        }
    //        return canonical;
    //    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, final String language, final String script, final String region,
            final String languageTarget, final String scriptTarget, final String regionTarget, Map<LSR, LSR> internCache) {
        LSR newValue = new LSR(languageTarget, scriptTarget, regionTarget);
        LSR oldValue = internCache.get(newValue);
        if (oldValue == null) {
            internCache.put(newValue, newValue);
            oldValue = newValue;
        }
        set(langTable, language, script, region, oldValue);
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, final String language, final String script, final String region, LSR newValue) {
        Map<String, Map<String, LSR>> scriptTable = Maker.TREEMAP.getSubtable(langTable, language);
        Map<String, LSR> regionTable = Maker.TREEMAP.getSubtable(scriptTable, script);
        //        LSR oldValue = regionTable.get(region);
        //        if (oldValue != null) {
        //            int debug = 0;
        //        }
        regionTable.put(region, newValue);
    }

    /**
     * Convenience methods
     */
    public LSR maximize(String source) {
        return maximize(ULocale.forLanguageTag(source));
    }

    public LSR maximize(ULocale source) {
        return maximize(source.getLanguage(), source.getScript(), source.getCountry());
    }

    public LSR maximize(LSR source) {
        return maximize(source.language, source.script, source.region);
    }

    //    public static ULocale addLikelySubtags(ULocale loc) {
    //
    //    }

    /**
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    public LSR maximize(String language, String script, String region) {
        int retainOldMask = 0;
        Map<String, Map<String, LSR>> scriptTable = langTable.get(language);
        if (scriptTable == null) { // cannot happen if language == "und"
            retainOldMask |= 4;
            scriptTable = langTable.get("und");
        } else if (!language.equals("und")) {
            retainOldMask |= 4;
        }

        if (script.equals("Zzzz")) {
            script = "";
        }
        Map<String, LSR> regionTable = scriptTable.get(script);
        if (regionTable == null) { // cannot happen if script == ""
            retainOldMask |= 2;
            regionTable = scriptTable.get("");
        } else if (!script.isEmpty()) {
            retainOldMask |= 2;
        }

        if (region.equals("ZZ")) {
            region = "";
        }
        LSR result = regionTable.get(region);
        if (result == null) { // cannot happen if region == ""
            retainOldMask |= 1;
            result = regionTable.get("");
            if (result == null) {
                return null;
            }
        } else if (!region.isEmpty()) {
            retainOldMask |= 1;
        }

        switch (retainOldMask) {
        default:
        case 0: return result;
        case 1: return result.replace(null, null, region);
        case 2: return result.replace(null, script, null);
        case 3: return result.replace(null, script, region);
        case 4: return result.replace(language, null, null);
        case 5: return result.replace(language, null, region);
        case 6: return result.replace(language, script, null);
        case 7: return result.replace(language, script, region);
        }
    }

    @SuppressWarnings("unused")
    private LSR minimizeSubtags(String languageIn, String scriptIn, String regionIn, Minimize fieldToFavor) {
        LSR result = maximize(languageIn, scriptIn, regionIn);

        // We could try just a series of checks, like:
        // LSR result2 = addLikelySubtags(languageIn, "", "");
        // if result.equals(result2) return result2;
        // However, we can optimize 2 of the cases:
        //   (languageIn, "", "")
        //   (languageIn, "", regionIn)

        Map<String, Map<String, LSR>> scriptTable = langTable.get(result.language);

        Map<String, LSR> regionTable0 = scriptTable.get("");
        LSR value00 = regionTable0.get("");
        boolean favorRegionOk = false;
        if (result.script.equals(value00.script)) { //script is default
            if (result.region.equals(value00.region)) {
                return result.replace(null, "", "");
            } else if (fieldToFavor == Minimize.FAVOR_REGION) {
                return result.replace(null, "", null);
            } else {
                favorRegionOk = true;
            }
        }

        // The last case is not as easy to optimize.
        // Maybe do later, but for now use the straightforward code.
        LSR result2 = maximize(languageIn, scriptIn, "");
        if (result2.equals(result)) {
            return result.replace(null, null, "");
        } else if (favorRegionOk) {
            return result.replace(null, "", null);
        }
        return result;
    }

    private static StringBuilder show(Map<?,?> map, String indent, StringBuilder output) {
        String first = indent.isEmpty() ? "" : "\t";
        for (Entry<?,?> e : map.entrySet()) {
            String key = e.getKey().toString();
            Object value = e.getValue();
            output.append(first + (key.isEmpty() ? "∅" : key));
            if (value instanceof Map) {
                show((Map<?,?>)value, indent+"\t", output);
            } else {
                output.append("\t" + Utility.toString(value)).append("\n");
            }
            first = indent;
        }
        return output;
    }

    @Override
    public String toString() {
        return show(langTable, "", new StringBuilder()).toString();
    }

    //    public static void main(String[] args) {
    //        System.out.println(LSR.fromMaximalized(ULocale.ENGLISH));
    //
    //        final Map<String, String> rawData = sdi.getLikelySubtags();
    //        XLikelySubtags ls = XLikelySubtags.getDefault();
    //        System.out.println(ls);
    //        ls.maximize(new ULocale("iw"));
    //        if (true) return;
    //
    //        LanguageTagParser ltp = new LanguageTagParser();
    //
    //        // get all the languages, scripts, and regions
    //        Set<String> languages = new TreeSet<String>();
    //        Set<String> scripts = new TreeSet<String>();
    //        Set<String> regions = new TreeSet<String>();
    //        Counter<String> languageCounter = new Counter<String>();
    //        Counter<String> scriptCounter = new Counter<String>();
    //        Counter<String> regionCounter = new Counter<String>();
    //
    //        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
    //            final String source = sourceTarget.getKey();
    //            ltp.set(source);
    //            languages.add(ltp.getLanguage());
    //            scripts.add(ltp.getScript());
    //            regions.add(ltp.getRegion());
    //            final String target = sourceTarget.getValue();
    //            ltp.set(target);
    //            add(target, languageCounter, ltp.getLanguage(), 1);
    //            add(target, scriptCounter, ltp.getScript(), 1);
    //            add(target, regionCounter, ltp.getRegion(), 1);
    //        }
    //        ltp.set("und-Zzzz-ZZ");
    //        languageCounter.add(ltp.getLanguage(), 1);
    //        scriptCounter.add(ltp.getScript(), 1);
    //        regionCounter.add(ltp.getRegion(), 1);
    //
    //        if (SHORT) {
    //            removeSingletons(languages, languageCounter);
    //            removeSingletons(scripts, scriptCounter);
    //            removeSingletons(regions, regionCounter);
    //        }
    //
    //        System.out.println("languages: " + languages.size() + "\n\t" + languages + "\n\t" + languageCounter);
    //        System.out.println("scripts: " + scripts.size() + "\n\t" + scripts + "\n\t" + scriptCounter);
    //        System.out.println("regions: " + regions.size() + "\n\t" + regions + "\n\t" + regionCounter);
    //
    //        int maxCount = Integer.MAX_VALUE;
    //
    //        int counter = maxCount;
    //        long tempTime = System.nanoTime();
    //        newMax:
    //            for (String language : languages) {
    //                for (String script : scripts) {
    //                    for (String region : regions) {
    //                        if (--counter < 0) break newMax;
    //                        LSR result = ls.maximize(language, script, region);
    //                    }
    //                }
    //            }
    //        long newMaxTime = System.nanoTime() - tempTime;
    //        System.out.println("newMaxTime: " + newMaxTime);
    //
    //        counter = maxCount;
    //        tempTime = System.nanoTime();
    //        newMin:
    //            for (String language : languages) {
    //                for (String script : scripts) {
    //                    for (String region : regions) {
    //                        if (--counter < 0) break newMin;
    //                        LSR minNewS = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_SCRIPT);
    //                    }
    //                }
    //            }
    //        long newMinTime = System.nanoTime() - tempTime;
    //        System.out.println("newMinTime: " + newMinTime);
    //
    //        // *****
    //
    //        tempTime = System.nanoTime();
    //        counter = maxCount;
    //        oldMax:
    //            for (String language : languages) {
    //                for (String script : scripts) {
    //                    for (String region : regions) {
    //                        if (--counter < 0) break oldMax;
    //                        ULocale tempLocale = new ULocale(language, script, region);
    //                        ULocale max = ULocale.addLikelySubtags(tempLocale);
    //                    }
    //                }
    //            }
    //        long oldMaxTime = System.nanoTime() - tempTime;
    //        System.out.println("oldMaxTime: " + oldMaxTime + "\t" + oldMaxTime/newMaxTime + "x");
    //
    //        counter = maxCount;
    //        tempTime = System.nanoTime();
    //        oldMin:
    //            for (String language : languages) {
    //                for (String script : scripts) {
    //                    for (String region : regions) {
    //                        if (--counter < 0) break oldMin;
    //                        ULocale tempLocale = new ULocale(language, script, region);
    //                        ULocale minOldS = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_SCRIPT);
    //                    }
    //                }
    //            }
    //        long oldMinTime = System.nanoTime() - tempTime;
    //        System.out.println("oldMinTime: " + oldMinTime + "\t" + oldMinTime/newMinTime + "x");
    //
    //        counter = maxCount;
    //        testMain:
    //            for (String language : languages) {
    //                System.out.println(language);
    //                int tests = 0;
    //                for (String script : scripts) {
    //                    for (String region : regions) {
    //                        ++tests;
    //                        if (--counter < 0) break testMain;
    //                        LSR maxNew = ls.maximize(language, script, region);
    //                        LSR minNewS = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_SCRIPT);
    //                        LSR minNewR = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_REGION);
    //
    //                        ULocale tempLocale = new ULocale(language, script, region);
    //                        ULocale maxOld = ULocale.addLikelySubtags(tempLocale);
    //                        ULocale minOldS = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_SCRIPT);
    //                        ULocale minOldR = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_REGION);
    //
    //                        // check values
    //                        final String maxNewS = String.valueOf(maxNew);
    //                        final String maxOldS = maxOld.toLanguageTag();
    //                        boolean sameMax = maxOldS.equals(maxNewS);
    //
    //                        final String minNewSS = String.valueOf(minNewS);
    //                        final String minOldSS = minOldS.toLanguageTag();
    //                        boolean sameMinS = minNewSS.equals(minOldSS);
    //
    //                        final String minNewRS = String.valueOf(minNewR);
    //                        final String minOldRS = minOldS.toLanguageTag();
    //                        boolean sameMinR = minNewRS.equals(minOldRS);
    //
    //                        if (sameMax && sameMinS && sameMinR) continue;
    //                        System.out.println(new LSR(language, script, region)
    //                                + "\tmax: " + maxNew
    //                                + (sameMax ? "" : "≠" + maxOldS)
    //                                + "\tminS: " + minNewS
    //                                + (sameMinS ? "" : "≠" + minOldS)
    //                                + "\tminR: " + minNewR
    //                                + (sameMinR ? "" : "≠" + minOldR)
    //                                );
    //                    }
    //                }
    //                System.out.println(language + ": " + tests);
    //            }
    //    }
    //
    //    private static void add(String target, Counter<String> languageCounter, String language, int count) {
    //        if (language.equals("aa")) {
    //            int debug = 0;
    //        }
    //        languageCounter.add(language, count);
    //    }
    //
    //    private static void removeSingletons(Set<String> languages, Counter<String> languageCounter) {
    //        for (String s : languageCounter) {
    //            final long count = languageCounter.get(s);
    //            if (count <= 1) {
    //                languages.remove(s);
    //            }
    //        }
    //    }
}
