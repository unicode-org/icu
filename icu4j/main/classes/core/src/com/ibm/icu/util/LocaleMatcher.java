/*
 ****************************************************************************************
 * Copyright (C) 2009, Google, Inc.; International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                         *
 ****************************************************************************************
 */
package com.ibm.icu.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;

/**
 * Provides a way to match the languages (locales) supported by a product to the
 * languages (locales) acceptable to a user, and get the best match. For
 * example:
 * 
 * <pre>
 * LanguageMatcher matcher = new StandardLanguageMatcher(&quot;fr, en-GB, en&quot;);
 * 
 * // afterwards:
 * matcher.getBestMatch(LanguageCode.US).first == LanguageCode.ENGLISH
 * </pre>
 * 
 * It takes into account when languages are close to one another, such as fil
 * and tl, and when language regional variants are close, like en-GB and en-AU.
 * It also handles scripts, like zh-Hant vs zh-TW. For examples, see the test
 * file.
 * <p>All classes implementing this interface should be immutable. Often a
 * product will just need one static instance, built with the languages
 * that it supports. However, it may want multiple instances with different
 * default languages based on additional information, such as the domain.
 * 
 * @author markdavis@google.com
 * @draft ICU 4.4
 */
public class LocaleMatcher {
    private static final boolean DEBUG = false;

    /**
     * Threshold for falling back to the default (first) language. May make this
     * a parameter in the future.
     */
    private static final double DEFAULT_THRESHOLD = 0.5;

    /**
     * The default language, in case the threshold is not met.
     */
    private final ULocale defaultLanguage;

    /**
     * Create a new language matcher. The highest-weighted language is the
     * default. That means that if no other language is matches closer than a given
     * threshold, that default language is chosen. Typically the default is English,
     * but it could be different based on additional information, such as the domain
     * of the page.
     * 
     * @param languagePriorityList weighted list
     */
    public LocaleMatcher(LocalePriorityList languagePriorityList) {
        this(languagePriorityList, defaultWritten);
    }

    /**
     * Create a new language matcher from a String form. The highest-weighted
     * language is the default.
     * 
     * @param languagePriorityListString String form of LanguagePriorityList
     */
    public LocaleMatcher(String languagePriorityListString) {
        this(LocalePriorityList.add(languagePriorityListString).build());
    }

    /**
     * Internal testing function; may expose API later.
     * @internal
     * @param languagePriorityList LocalePriorityList to match
     * @param matcherData Internal matching data
     */
    public LocaleMatcher(LocalePriorityList languagePriorityList, LanguageMatcherData matcherData) {
        this.matcherData = matcherData;
        for (final ULocale language : languagePriorityList) {
            add(language, languagePriorityList.getWeight(language));
        }
        Iterator<ULocale> it = languagePriorityList.iterator();
        defaultLanguage = it.hasNext() ? it.next() : null;
    }


    /**
     * Returns a fraction between 0 and 1, where 1 means that the languages are a
     * perfect match, and 0 means that they are completely different. Note that
     * the precise values may change over time; no code should be made dependent
     * on the values remaining constant.
     * @param desired Desired locale
     * @param desiredMax Maximized locale (using likely subtags)
     * @param supported Supported locale
     * @param supportedMax Maximized locale (using likely subtags)
     * @return value between 0 and 1, inclusive.
     */
    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        return matcherData.match(desired, desiredMax, supported, supportedMax);
    }


    /**
     * Canonicalize a locale (language). Note that for now, it is canonicalizing according to CLDR conventions (he vs iw, etc), since that is what is needed for likelySubtags.
     * TODO Get the data from CLDR, use Java conventions.
     * @param ulocale language/locale code
     * @return ULocale with remapped subtags.
     */
    public ULocale canonicalize(ULocale ulocale) {
        String lang = ulocale.getLanguage();
        String lang2 = canonicalMap.get(lang);
        String script = ulocale.getScript();
        String script2 = canonicalMap.get(script);
        String region = ulocale.getCountry();
        String region2 = canonicalMap.get(region);
        if (lang2 != null || script2 != null || region2 != null) {
            return new ULocale(
                    lang2 == null ? lang : lang2,
                            script2 == null ? script : script2,
                                    region2 == null ? region : region2
            );
        }
        return ulocale;
    }

    /**
     * Get the best match for a LanguagePriorityList
     * 
     * @param languageList list to match
     * @return best matching language code
     */
    public ULocale getBestMatch(LocalePriorityList languageList) {
        double bestWeight = 0;
        ULocale bestTableMatch = null;
        for (final ULocale language : languageList) {
            final Row.R2<ULocale, Double> matchRow = getBestMatchInternal(language);
            final double weight = matchRow.get1() * languageList.getWeight(language);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestTableMatch = matchRow.get0();
            }
        }
        if (bestWeight < DEFAULT_THRESHOLD) {
            bestTableMatch = defaultLanguage;
        }
        return bestTableMatch;
    }

    /**
     * Convenience method: Get the best match for a LanguagePriorityList
     * 
     * @param languageList String form of language priority list
     * @return best matching language code
     */
    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build());
    }

    /**
     * Get the best match for an individual language code.
     * 
     * @param ulocale locale/language code to match
     * @return best matching language code
     */
    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatchInternal(ulocale).get0();
    }

    @Override
    public String toString() {
        return "{" + defaultLanguage + ", " 
        + maximizedLanguageToWeight + "}";
    }
    // ================= Privates =====================

    /**
     * Get the best match for an individual language code.
     * 
     * @param languageCode
     * @return best matching language code and weight (as per
     *         {@link #match(ULocale, ULocale)})
     */
    private Row.R2<ULocale, Double> getBestMatchInternal(ULocale languageCode) {
        languageCode = canonicalize(languageCode);
        final ULocale maximized = addLikelySubtags(languageCode);
        if (DEBUG) {
            System.out.println("\n" + languageCode + ";\t" + maximized);
        }
        double bestWeight = 0;
        ULocale bestTableMatch = null;
        for (final ULocale tableKey : maximizedLanguageToWeight.keySet()) {
            R2<ULocale, Double> row = maximizedLanguageToWeight.get(tableKey);
            final double match = match(languageCode, maximized, tableKey, row.get0());
            if (DEBUG) {
                System.out.println("\t" + tableKey + ";\t" + row.toString() + ";\t" + match);
            }
            final double weight = match * row.get1();
            if (weight > bestWeight) {
                bestWeight = weight;
                bestTableMatch = tableKey;
            }
        }
        if (bestWeight < DEFAULT_THRESHOLD) {
            bestTableMatch = defaultLanguage;
        }
        return Row.R2.of(bestTableMatch, bestWeight);
    }

    private void add(ULocale language, Double weight) {
        language = canonicalize(language);
        R2<ULocale, Double> row = Row.of(addLikelySubtags(language), weight);
        maximizedLanguageToWeight.put(language, row);
    }

    Map<ULocale,Row.R2<ULocale, Double>> maximizedLanguageToWeight = new LinkedHashMap<ULocale, R2<ULocale, Double>>();


    // =============== Special Mapping Information ==============

    /**
     * We need to add another method to addLikelySubtags that doesn't return
     * null, but instead substitutes Zzzz and ZZ if unknown. There are also
     * a few cases where addLikelySubtags needs to have expanded data, to handle
     * all deprecated codes, and to update to CLDR 1.6.
     * @param languageCode
     * @return "fixed" addLikelySubtags
     */
    // TODO(markdavis): update the above when CLDR 1.6 is final.
    private ULocale addLikelySubtags(ULocale languageCode) {
        final ULocale result = ULocale.addLikelySubtags(languageCode);
        // should have method on getLikelySubtags for this
        if (result == null || result.equals(languageCode)) {
            final String language = languageCode.getLanguage();
            final String script = languageCode.getScript();
            final String region = languageCode.getCountry();
            return new ULocale((language.length()==0 ? "und"
                    : language)
                    + "_"
                    + (script.length()==0 ? "Zzzz" : script)
                    + "_"
                    + (region.length()==0 ? "ZZ" : region));
        }
        return result;
    }

    private static class LocalePatternMatcher {
        // a value of null means a wildcard; matches any.
        private String lang;
        private String script;
        private String region;
        private Level level;
        static Pattern pattern = Pattern.compile(
                "([a-zA-Z]{1,8}|\\*)" +
                "(?:-([a-zA-Z]{4}|\\*))?" +
        "(?:-([a-zA-Z]{2}|[0-9]{3}|\\*))?");

        public LocalePatternMatcher(String toMatch) {
            Matcher matcher = pattern.matcher(toMatch);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Bad pattern: " + toMatch);
            }
            lang = matcher.group(1);
            script = matcher.group(2);
            region = matcher.group(3);
            level = region != null ? Level.region : script != null ? Level.script : Level.language;

            if (lang.equals("*")) {
                lang = null;
            }
            if (script != null && script.equals("*")) {
                script = null;
            }
            if (region != null && region.equals("*")) {
                region = null;
            }
        }

        boolean matches(ULocale ulocale) {
            if (lang != null && !lang.equals(ulocale.getLanguage())) {
                return false;
            }
            if (script != null && !script.equals(ulocale.getScript())) {
                return false;
            }
            if (region != null && !region.equals(ulocale.getCountry())) {
                return false;
            }
            return true;
        }

        public Level getLevel() {
            return level;
        }

        public String getLanguage() {
            return (lang == null ? "*" : lang);
        }

        public String getScript() {
            return (script == null ? "*" : script);
        }

        public String getRegion() {
            return (region == null ? "*" : region);
        }

        public String toString() {
            String result = getLanguage();
            if (level != Level.language) {
                result += "-" + getScript();
                if (level != Level.script) {
                    result += "-" + getRegion();
                }
            }
            return result;
        }
    }

    enum Level {language, script, region}

    private static class ScoreData implements Freezable {
        LinkedHashSet<Row.R3<LocalePatternMatcher,LocalePatternMatcher,Double>> scores = new LinkedHashSet<R3<LocalePatternMatcher, LocalePatternMatcher, Double>>();
        final double worst;
        final Level level;

        public ScoreData(Level level) {
            this.level = level;
            this.worst = (1-(level == Level.language ? 90 : level == Level.script ? 20 : 4))/100.0;
        }

        void addDataToScores(String desired, String supported, R3<LocalePatternMatcher,LocalePatternMatcher,Double> data) {
            //            Map<String, Set<R3<LocalePatternMatcher,LocalePatternMatcher,Double>>> lang_result = scores.get(desired);
            //            if (lang_result == null) {
            //                scores.put(desired, lang_result = new HashMap());
            //            }
            //            Set<R3<LocalePatternMatcher,LocalePatternMatcher,Double>> result = lang_result.get(supported);
            //            if (result == null) {
            //                lang_result.put(supported, result = new LinkedHashSet());
            //            }
            //            result.add(data);
            scores.add(data);
        }

        double getScore(ULocale desiredLocale, ULocale dMax, String desiredRaw, String desiredMax, 
                ULocale supportedLocale, ULocale sMax, String supportedRaw, String supportedMax) {

            /*
             * d, dm, s, sm
             * dc = d != dm
             * sc = s != sm
             * if dm != sm
             *   rd = rd(dm,sm) // line 4
             *   if dc != sc
             *     rd *= 0.75 // lines 3,8
             *   ef dc
             *     rd *= 0.5 // lines 7
             *   end
             *  ef dc == sc
             *   rd = 0 // line 6
             *  else
             *   rd = 0.25*StdRDiff // lines 2,5
             */

            boolean desiredChange = desiredRaw.equals(desiredMax);
            boolean supportedChange = supportedRaw.equals(supportedMax);
            double distance;
            if (!desiredMax.equals(supportedMax)) {
                //                Map<String, Set<R3<LocalePatternMatcher,LocalePatternMatcher,Double>>> lang_result = scores.get(desiredMax);
                //                if (lang_result == null) {
                //                    distance = worst;
                //                } else {
                //                    Set<R3<LocalePatternMatcher,LocalePatternMatcher,Double>> result = lang_result.get(supportedMax);
                //                    skip:
                //                    if (result == null) {
                //                        distance = worst;
                //                    } else {
                distance = getRawScore(dMax, sMax);
                //                }
                if (desiredChange == supportedChange) {
                    distance *= 0.75;
                } else if (desiredChange) {
                    distance *= 0.5;
                }
            } else if (desiredChange == supportedChange) { // maxes are equal, changes are equal
                distance = 0;
            } else { // maxes are equal, changes are different
                distance = 0.25*worst;
            }
            return distance;
        }

        private double getRawScore(ULocale desiredLocale, ULocale supportedLocale) {
            if (DEBUG) {
                System.out.println("\t\t\tRaw Score:\t" + desiredLocale + ";\t" + supportedLocale);
            }
            for (R3<LocalePatternMatcher,LocalePatternMatcher,Double> datum : scores) { // : result
                if (datum.get0().matches(desiredLocale) 
                        && datum.get1().matches(supportedLocale)) {
                    if (DEBUG) {
                        System.out.println("\t\t\tFOUND\t" + datum);
                    }
                    return datum.get2();
                }
            }
            if (DEBUG) {
                System.out.println("\t\t\tNOTFOUND\t" + worst);
            }
            return worst;
        }

        public String toString() {
            return level + ", " + scores;
        }

        @SuppressWarnings("unchecked")
        public ScoreData cloneAsThawed() {
            try {
                ScoreData result = (ScoreData) clone();
                result.scores = (LinkedHashSet<R3<LocalePatternMatcher, LocalePatternMatcher, Double>>) result.scores.clone();
                result.frozen = false;
                return result;
            } catch (CloneNotSupportedException e) {
                throw new IllegalArgumentException(e); // will never happen
            }

        }

        private boolean frozen = false;

        public ScoreData freeze() {
            return this;
        }

        public boolean isFrozen() {
            return frozen;
        }
    }

    /**
     * Only for testing and use by tools. Interface may change!!
     * @internal
     */
    public static class LanguageMatcherData implements Freezable {
        ScoreData languageScores = new ScoreData(Level.language);
        ScoreData scriptScores = new ScoreData(Level.script);
        ScoreData regionScores = new ScoreData(Level.region);

        public double match(ULocale a, ULocale aMax, ULocale b, ULocale bMax) {
            double diff = 0;
            diff += languageScores.getScore(a, aMax, a.getLanguage(), aMax.getLanguage(), b, bMax, b.getLanguage(), bMax.getLanguage());
            diff += scriptScores.getScore(a, aMax, a.getScript(), aMax.getScript(), b, bMax, b.getScript(), bMax.getScript());
            diff += regionScores.getScore(a, aMax, a.getCountry(), aMax.getCountry(), b, bMax, b.getCountry(), bMax.getCountry());

            if (!a.getVariant().equals(b.getVariant())) {
                diff += 1;
            }
            if (diff < 0.0d) {
                diff = 0.0d;
            } else if (diff > 1.0d) {
                diff = 1.0d;
            }
            return 1.0 - diff;
        }


        /**
         * Add an exceptional distance between languages, typically because regional
         * dialects were given their own language codes. At this point the code is
         * symmetric. We don't bother producing an equivalence class because there are
         * so few cases; this function depends on the other permutations being
         * added specifically.
         * @param desired
         * @param supported
         * @param distance
         * @param bidirectional TODO
         * @return 
         */
        private LanguageMatcherData addDistance(String desired, String supported, int percent) {
            return addDistance(desired, supported, percent, false, null);
        }
        public LanguageMatcherData addDistance(String desired, String supported, int percent, String comment) {
            return addDistance(desired, supported, percent, false, comment);
        }

        public LanguageMatcherData addDistance(String desired, String supported, int percent, boolean oneway) {
            return addDistance(desired, supported, percent, oneway, null);
        }

        private LanguageMatcherData addDistance(String desired, String supported, int percent, boolean oneway, String comment) {
            if (DEBUG) {
                System.out.println("\t<languageMatch desired=\"" + desired + "\"" +
                        " supported=\"" + supported + "\"" +
                        " percent=\"" + percent + "\""
                        + (oneway ? " oneway=\"true\"" : "")
                        + "/>"
                        + (comment == null ? "" : "\t<!-- " + comment + " -->"));
                //                    //     .addDistance("nn", "nb", 4, true)
                //                        System.out.println(".addDistance(\"" + desired + "\"" +
                //                                ", \"" + supported + "\"" +
                //                                ", " + percent + ""
                //                                + (oneway ? "" : ", true")
                //                                + (comment == null ? "" : ", \"" + comment + "\"")
                //                                + ")"
                //                        );

            }
            double score = 1-percent/100.0; // convert from percentage
            LocalePatternMatcher desiredMatcher = new LocalePatternMatcher(desired);
            Level desiredLen = desiredMatcher.getLevel();
            LocalePatternMatcher supportedMatcher = new LocalePatternMatcher(supported);
            Level supportedLen = supportedMatcher.getLevel();
            if (desiredLen != supportedLen) {
                throw new IllegalArgumentException();
            }
            R3<LocalePatternMatcher,LocalePatternMatcher,Double> data = Row.of(desiredMatcher, supportedMatcher, score);
            R3<LocalePatternMatcher,LocalePatternMatcher,Double> data2 = oneway ? null : Row.of(supportedMatcher, desiredMatcher, score);
            switch (desiredLen) {
            case language:
                String dlanguage = desiredMatcher.getLanguage();
                String slanguage = supportedMatcher.getLanguage();
                languageScores.addDataToScores(dlanguage, slanguage, data);
                if (!oneway) {
                    languageScores.addDataToScores(slanguage, dlanguage, data2);
                }
                break;
            case script:
                String dscript = desiredMatcher.getScript();
                String sscript = supportedMatcher.getScript();
                scriptScores.addDataToScores(dscript, sscript, data);
                if (!oneway) {
                    scriptScores.addDataToScores(sscript, dscript, data2);
                }
                break;
            case region:
                String dregion = desiredMatcher.getRegion();
                String sregion = supportedMatcher.getRegion();
                regionScores.addDataToScores(dregion, sregion, data);
                if (!oneway) {
                    regionScores.addDataToScores(sregion, dregion, data2);
                }
                break;
            }
            return this;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.util.Freezable#cloneAsThawed()
         */
        public Object cloneAsThawed() {
            LanguageMatcherData result;
            try {
                result = (LanguageMatcherData) clone();
                result.languageScores = languageScores.cloneAsThawed();
                result.scriptScores = scriptScores.cloneAsThawed();
                result.regionScores = regionScores.cloneAsThawed();
                result.frozen = false;
                return result;
            } catch (CloneNotSupportedException e) {
                throw new IllegalArgumentException(e); // will never happen
            }
        }

        private boolean frozen = false;

        public LanguageMatcherData freeze() {
            return this;
        }

        public boolean isFrozen() {
            return frozen;
        }
    }

    LanguageMatcherData matcherData;

    private static LanguageMatcherData defaultWritten = new LanguageMatcherData()
    // TODO get data from CLDR
    .addDistance("no", "nb", 100, "The language no is normally taken as nb in content; we might alias this for lookup.")
    .addDistance("nn", "nb", 96)
    .addDistance("nn", "no", 96)
    .addDistance("da", "no", 90, "Danish and norwegian are reasonably close.")
    .addDistance("da", "nb", 90)
    .addDistance("hr", "br", 96, "Serbo-croatian variants are all very close.")
    .addDistance("sh", "br", 96)
    .addDistance("sr", "br", 96)
    .addDistance("sh", "hr", 96)
    .addDistance("sr", "hr", 96)
    .addDistance("sh", "sr", 96)
    .addDistance("sr-Latn", "sr-Cyrl", 90, "Most serbs can read either script.")
    .addDistance("*-Hans", "*-Hant", 85, true, "Readers of simplified can read traditional much better than reverse.")
    .addDistance("*-Hant", "*-Hans", 75, true)
    .addDistance("en-*-US", "en-*-CA", 98, "US is different than others, and Canadian is inbetween.")
    .addDistance("en-*-US", "en-*-*", 97)
    .addDistance("en-*-CA", "en-*-*", 98)
    .addDistance("en-*-*", "en-*-*", 99)
    .addDistance("es-*-ES", "es-*-ES", 100, "Latin American Spanishes are closer to each other. Approximate by having es-ES be further from everything else.")
    .addDistance("es-*-ES", "es-*-*", 93)
    .addDistance("*", "*", 1, "[Default value -- must be at end!] Normally there is no comprehension of different languages.")
    .addDistance("*-*", "*-*", 20, "[Default value -- must be at end!] Normally there is little comprehension of different scripts.")
    .addDistance("*-*-*", "*-*-*", 96, "[Default value -- must be at end!] Normally there are small differences across regions.")
    .freeze();

    private static HashMap<String,String> canonicalMap = new HashMap<String, String>();

    static {
        // TODO get data from CLDR
        canonicalMap.put("iw", "he");
        canonicalMap.put("mo", "ro");
        canonicalMap.put("tl", "fil");
    }
}
