/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc. and International Business Machines Corporation and  *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.icu.dev.test.format.PluralRulesTest.StandardPluralCategories;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.NumberInfo;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 */
public class WritePluralRulesData {
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[] {"rules"};
        }
        for (String arg : args) {
            if (arg.equalsIgnoreCase("samples")) {
                generateSamples();
            } else if (arg.equalsIgnoreCase("rules")) {
                showRules();
            } else if (arg.equalsIgnoreCase("oldSnap")) {
                generateLOCALE_SNAPSHOT(PluralRulesFactory.NORMAL);
            } else if (arg.equalsIgnoreCase("newSnap")) {
                generateLOCALE_SNAPSHOT(PluralRulesFactory.ALTERNATE);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public static void generateLOCALE_SNAPSHOT(PluralRulesFactory pluralRulesFactory) {
        StringBuilder builder = new StringBuilder();
        Map<Set<StandardPluralCategories>, Relation<String, ULocale>> keywordsToData = new TreeMap(StandardPluralCategories.SHORTEST_FIRST);
        for (ULocale locale : pluralRulesFactory.getAvailableULocales()) {
            builder.setLength(0);
            PluralRules rules = pluralRulesFactory.forLocale(locale);
            boolean firstKeyword = true;
            EnumSet<StandardPluralCategories> keywords = StandardPluralCategories.getSet(rules.getKeywords());
            Relation<String, ULocale> samplesToLocales = keywordsToData.get(keywords);
            if (samplesToLocales == null) {
                keywordsToData.put(keywords, samplesToLocales = Relation.of(
                        new LinkedHashMap<String,Set<ULocale>>(), LinkedHashSet.class));
            }
            //System.out.println(locale);
            for (StandardPluralCategories keyword : keywords) {
                if (firstKeyword) {
                    firstKeyword = false;
                } else {
                    builder.append(";\t");
                }
                Collection<NumberInfo> samples = rules.getFractionSamples(keyword.toString());
                if (samples.size() == 0) {
                    throw new IllegalArgumentException();
                }
                builder.append(keyword).append(": ");
                boolean first = true;
                for (NumberInfo n : samples) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(n);
                    //                    for (double j : samples) {
                    //                        double sample = i + j/100;
                    //                    }
                }
            }
            samplesToLocales.put(builder.toString(), locale);
        }
        System.out.println("    static final String[] LOCALE_SNAPSHOT = {");
        for (Entry<Set<StandardPluralCategories>, Relation<String, ULocale>> keywordsAndData : keywordsToData.entrySet()) {
            System.out.println("\n        // " + keywordsAndData.getKey());
            for (Entry<String, Set<ULocale>> samplesAndLocales : keywordsAndData.getValue().keyValuesSet()) {
                Set<ULocale> locales = samplesAndLocales.getValue();
                // check functional equivalence
                boolean[] isAvailable = new boolean[1];
                for (ULocale locale : locales) {
                    ULocale base = pluralRulesFactory.getFunctionalEquivalent(locale, isAvailable);
                    if (!locales.contains(base) && base.toString().length() != 0) {
                        System.out.println("**" + locales + " doesn't contain " + base);
                    }
                }

                System.out.println(
                        "        \"" + CollectionUtilities.join(locales, ",")
                        + ";\t" + samplesAndLocales.getKey() + "\",");
            }
        }
        System.out.println("    };");
    }

    private static class OldNewData extends Row.R4<String, String, String, String> {
        public OldNewData(String oldRules, String oldSamples, String newRules, String newSamples) {
            super(oldRules, oldSamples, newRules, newSamples);
        }
    }

    static final String[] FOCUS_LOCALES = ("af,am,ar,az,bg,bn,ca,cs,cy,da,de,el,en,es,et,eu,fa,fi,fil,fr,gl,gu," +
            "hi,hr,hu,hy,id,is,it,he,ja,ka,kk,km,kn,ko,ky,lo,lt,lv,mk,ml,mn,mr,ms,my,ne,nl,nb," +
            "pa,pl,ps,pt,ro,ru,si,sk,sl,sq,sr,sv,sw,ta,te,th,tr,uk,ur,uz,vi,zh,zu").split("\\s*,\\s*");

    public static void showRules() {
        if (true) {
            // for debugging
            PluralRules rules = PluralRulesFactory.ALTERNATE.forLocale(new ULocale("lv"));
            rules.select(2.0d, 2, 0);
        }
        // System.out.println(new TreeSet(Arrays.asList(locales)));
        Relation<Map<String,OldNewData>, String> rulesToLocale = Relation.of(
                new TreeMap<Map<String,OldNewData>, Set<String>>(
                        new CollectionUtilities.MapComparator<String,OldNewData>()), TreeSet.class);
        for (String localeString : FOCUS_LOCALES) {
            ULocale locale = new ULocale(localeString);
            PluralRules oldRules = PluralRulesFactory.NORMAL.forLocale(locale);
            PluralRules newRules = PluralRulesFactory.ALTERNATE.hasOverride(locale) ? PluralRulesFactory.ALTERNATE.forLocale(locale) : null;
            Set<String> keywords = oldRules.getKeywords();
            if (newRules != null) {
                TreeSet<String> temp = new TreeSet<String>(PluralRules.KEYWORD_COMPARATOR);
                temp.addAll(keywords);
                temp.addAll(newRules.getKeywords());
                keywords = temp;
            }
            Map<String,OldNewData> temp = new LinkedHashMap();
            for (String keyword : keywords) {
                Collection<NumberInfo> oldFractionSamples = oldRules.getFractionSamples(keyword);
                Collection<NumberInfo> newFractionSamples = newRules == null ? null : newRules.getFractionSamples(keyword);

                // add extra samples if we have some, or if the rules differ

                if (newRules != null) {
                    oldFractionSamples = oldFractionSamples == null ? new TreeSet()
                    : new TreeSet(oldFractionSamples);
                    newFractionSamples = newFractionSamples == null ? new TreeSet()
                    : new TreeSet(newFractionSamples);
                    //                    if (extraSamples != null) {
                    //                        for (NumberPlus sample : extraSamples) {
                    //                            if (oldRules.select(sample.source, sample.visibleFractionDigitCount, sample.fractionalDigits).equals(keyword)) {
                    //                                oldFractionSamples.add(sample);
                    //                            }
                    //                            if (newRules != null && newRules.select(sample.source, sample.visibleFractionDigitCount, sample.fractionalDigits).equals(keyword)) {
                    //                                newFractionSamples.add(sample);
                    //                            }
                    //                        }
                    //                    }

                    // if the rules differ, then add samples from each to the other
                    if (newRules != null) {
                        for (NumberInfo sample : oldRules.getFractionSamples()) {
                            if (newRules.select(sample.source, sample.visibleFractionDigitCount, sample.fractionalDigits).equals(keyword)) {
                                newFractionSamples.add(sample);
                            }
                        }
                        for (NumberInfo sample : newRules.getFractionSamples()) {
                            if (oldRules.select(sample.source, sample.visibleFractionDigitCount, sample.fractionalDigits).equals(keyword)) {
                                oldFractionSamples.add(sample);
                            }
                        }
                    }
                }
                String oldRulesString = oldRules.getRules(keyword);
                if (oldRulesString == null) {
                    oldRulesString = "";
                }
                String newRulesString = newRules == null ? "" : newRules.getRules(keyword);
                if (newRulesString == null) {
                    newRulesString = "";
                }
                temp.put(keyword, new OldNewData(
                        oldRulesString, 
                        oldFractionSamples == null ? "" : "'" + CollectionUtilities.join(oldFractionSamples, ", "),
                                newRulesString, 
                                newFractionSamples == null ? "" : "'" + CollectionUtilities.join(newFractionSamples, ", ")
                        ));
            }
            rulesToLocale.put(temp, locale.toString());
        }
        System.out.println("Locales\tPC\tOld Rules\tOld Samples\tNew Rules\tNew Samples");
        for (Entry<Map<String, OldNewData>, Set<String>> entry : rulesToLocale.keyValuesSet()) {
            String localeList = CollectionUtilities.join(entry.getValue(), " ");
            for (Entry<String, OldNewData> keywordRulesSamples : entry.getKey().entrySet()) {
                System.out.println(
                        localeList // locale
                        + "\t" + keywordRulesSamples.getKey() // keyword
                        + "\t" + keywordRulesSamples.getValue().get0() // rules
                        + "\t" + keywordRulesSamples.getValue().get1() // samples
                        + "\t" + keywordRulesSamples.getValue().get2() // rules
                        + "\t" + keywordRulesSamples.getValue().get3() // samples
                        );
                localeList = "";
            }
        }

        if (false) {
            System.out.println("\n\nOld Rules for Locales");
            for (String localeString : FOCUS_LOCALES) {
                ULocale locale = new ULocale(localeString);
                PluralRules oldRules = PluralRules.forLocale(locale);
                System.out.println("{\"" + locale.toString() + "\", \"" + oldRules.toString() + "\"},");
            }
        }
    }

    static String[][] SAMPLE_PATTERNS = {
        {"af", "one", "{0} dag"},
        {"af", "other", "{0} dae"},
        {"am", "one", "{0} ቀን"},
        {"am", "other", "{0} ቀናት"}, // fixed to 'other'
        {"ar", "few", "{0} ساعات"},
        {"ar", "many", "{0}  ساعة"},
        {"ar", "one", "ساعة"},
        {"ar", "other", "{0} ساعة"},
        {"ar", "two", "ساعتان"},
        {"ar", "zero", "{0} ساعة"},
        {"bg", "one", "{0} ден"},
        {"bg", "other", "{0} дена"},
        {"bn", "one", "{0} টি আপেল"},
        {"bn", "other", "আমার অনেকগুলি আপেল আছে"},
        {"br", "few", "{0} deiz"},
        {"br", "many", "{0} a zeizioù"},
        {"br", "one", "{0} deiz"},
        {"br", "other", "{0} deiz"},
        {"br", "two", "{0} zeiz"},
        {"ca", "one", "{0} dia"},
        {"ca", "other", "{0} dies"},
        {"cs", "few", "{0} dny"},
        {"cs", "one", "{0} den"},
        {"cs", "other", "{0} dní"},
        {"cs", "many", "{0} dne"}, // added from spreadsheet
        {"cy", "zero", "{0} cadair (f) {0} peint (m)"},
        {"cy", "one", "{0} gadair (f) {0} peint (m)"},
        {"cy", "two", "{0} gadair (f) {0} beint (m)"},
        {"cy", "few", "{0} cadair (f) {0} pheint (m)"},
        {"cy", "many", "{0} chadair (f) {0} pheint (m)"},
        {"cy", "other", "{0} cadair (f) {0} peint (m)"},
        {"da", "one", "{0} dag"},
        {"da", "other", "{0} dage"},
        {"de", "one", "{0} Tag"},
        {"de", "other", "{0} Tage"},
        {"dz", "other", "ཉིནམ་ {0} "},
        {"el", "one", "{0} ημέρα"},
        {"el", "other", "{0} ημέρες"},
        {"es", "one", "{0} día"},
        {"es", "other", "{0} días"},
        {"et", "one", "{0} ööpäev"},
        {"et", "other", "{0} ööpäeva"},
        {"eu", "one", "Nire {0} lagunarekin nago"},
        {"eu", "other", "Nire {0} lagunekin nago"},
        {"fa", "other", "{0} روز"},
        {"fi", "one", "{0} päivä"},
        {"fi", "other", "{0} päivää"},
        {"fil", "one", "sa {0} araw"},
        {"fil", "other", "sa {0} (na) araw"},
        {"fr", "one", "{0} jour"},
        {"fr", "other", "{0} jours"},
        {"gl", "one", "{0} día"},
        {"gl", "other", "{0} días"},
        {"gu", "one", "{0} અઠવાડિયું"},
        {"gu", "other", "{0} અઠવાડિયા"},
        {"he", "many", "{0} ימים"},
        {"he", "one", " יום {0}"},
        {"he", "other", "{0} ימים"},
        {"he", "two", "יומיים"},
        {"hi", "one", "{0} घंटा"},
        {"hi", "other", "{0} घंटे"},
        {"hr", "few", "za {0} mjeseca"},
        {"hr", "many", "za {0} mjeseci"},
        {"hr", "one", "za {0} mjesec"},
        {"hr", "other", "za sljedeći broj mjeseci: {0}"},
        {"hu", "other", "{0} nap"},
        {"hy", "few", "{0} օր"},
        {"hy", "many", "{0} օր"},
        {"hy", "one", "{0} օր"},
        {"hy", "other", "{0} օր"},
        {"hy", "two", "{0} օր"},
        {"hy", "zero", "{0} օր"},
        {"id", "other", "{0} hari"},
        {"is", "one", "{0} dagur"},
        {"is", "other", "{0} dagar"},
        {"it", "one", "{0} giorno"},
        {"it", "other", "{0} giorni"},
        {"ja", "other", "{0}日"},
        {"km", "other", "{0} ថ្ងៃ"},
        {"kn", "other", "{0} ದಿನಗಳು"},
        {"ko", "other", "{0}일"},
        {"lo", "other", "{0} ມື້"},
        {"lt", "few", "{0} dienos"},
        {"lt", "one", "{0} diena"},
        {"lt", "other", "{0} dienų"},
        {"lv", "one", "{0} diennakts"},
        {"lv", "other", "{0} diennaktis"},
        {"lv", "zero", "{0} diennakšu"},
        {"ml", "one", "{0} വ്യക്തി"},
        {"ml", "other", "{0} വ്യക്തികൾ"},
        {"mr", "one", "{0} घर"},
        {"mr", "other", "{0} घरे"},
        {"ms", "other", "{0} hari"},
        {"nb", "one", "{0} dag"},
        {"nb", "other", "{0} dager"},
        {"ne", "one", "तपाईंसँग {0} निमन्त्रणा छ"},
        {"ne", "other", "तपाईँसँग {0} निमन्त्रणाहरू छन्"},
        //        {"ne", "", "{0} दिन बाँकी छ ।"},
        //        {"ne", "", "{0} दिन बाँकी छ ।"},
        //        {"ne", "", "{0} दिन बाँकी छ ।"},
        //        {"ne", "", "{0} जनाहरू पाहुना बाँकी छ ।"},
        {"nl", "one", "{0} dag"},
        {"nl", "other", "{0} dagen"},
        {"pl", "few", "{0} miesiące"},
        {"pl", "many", "{0} miesięcy"},
        {"pl", "one", "{0} miesiąc"},
        {"pl", "other", "{0} miesiąca"},
        {"pt", "one", "{0} dia"},
        {"pt", "other", "{0} dias"},
        {"pt_PT", "one", "{0} dia"},
        {"pt_PT", "other", "{0} dias"},
        {"ro", "few", "{0} zile"},
        {"ro", "one", "{0} zi"},
        {"ro", "other", "{0} de zile"},
        {"ru", "few", "{0} года"},
        {"ru", "many", "{0} лет"},
        {"ru", "one", "{0} год"},
        {"ru", "other", "{0} года"},
        {"si", "other", "දින {0}ක්"},
        {"sk", "few", "{0} dni"},
        {"sk", "one", "{0} deň"},
        {"sk", "other", "{0} dní"},
        {"sk", "many", "{0} dňa"}, // added from spreadsheet
        {"sl", "few", "{0} ure"},
        {"sl", "one", "{0} ura"},
        {"sl", "other", "{0} ur"},
        {"sl", "two", "{0} uri"},
        {"sr", "few", "{0} сата"},
        {"sr", "many", "{0} сати"},
        {"sr", "one", "{0} сат"},
        {"sr", "other", "{0} сати"},
        {"sv", "one", "om {0} dag"},
        {"sv", "other", "om {0} dagar"},
        {"sw", "one", "siku {0} iliyopita"},
        {"sw", "other", "siku {0} zilizopita"},
        {"ta", "one", "{0} நாள்"},
        {"ta", "other", "{0} நாட்கள்"},
        {"te", "one", "{0} రోజు"},
        {"te", "other", "{0} రోజులు"},
        {"th", "other", "{0} วัน"},
        {"tr", "other", "{0} gün"},
        {"uk", "few", "{0} дні"},
        {"uk", "many", "{0} днів"},
        {"uk", "one", "{0} день"},
        {"uk", "other", "{0} дня"},
        {"ur", "one", "{0} گھنٹہ"},
        {"ur", "other", "{0} گھنٹے"},
        {"vi", "other", "{0} ngày"},
        {"zh", "other", "{0} 天"},
        {"zh_Hant", "other", "{0} 日"},     
        {"en", "one", "{0} day"},        // added from spreadsheet  
        {"en", "other", "{0} days"},       // added from spreadsheet   
        {"zu", "one", "{0} usuku"},     // added from spreadsheet
        {"zu", "other", "{0} izinsuku"},          // added from spreadsheet
    };
    static final Set<String> NEW_LOCALES = new HashSet(Arrays.asList("az,ka,kk,ky,mk,mn,my,pa,ps,sq,uz".split("\\s*,\\s*")));

    static class SamplePatterns {
        final Map<String,String> keywordToPattern = new TreeMap(PluralRules.KEYWORD_COMPARATOR);
        final Map<String,String> keywordToErrors = new HashMap();
        public void put(String keyword, String sample) {
            if (keywordToPattern.containsKey(keyword)) {
                throw new IllegalArgumentException("Duplicate keyword <" + keyword + ">");
            } else {
                keywordToPattern.put(keyword, sample);
            }
        }
        public void checkErrors(Set<String> set) {
            final Map<String,String> skeletonToKeyword = new HashMap();
            for (String keyword : set) {
                String error = "";
                String sample = keywordToPattern.get(keyword);
                String skeleton = sample.replace(" ", "").replace("{0}", "");
                String oldSkeletonKeyword = skeletonToKeyword.get(skeleton);
                if (oldSkeletonKeyword != null) {
                    if (error.length() != 0) {
                        error += ", ";
                    }
                    error += "Duplicate keyword skeleton <" + keyword + ", " + skeleton + ">, same as for: <" + oldSkeletonKeyword + ">";
                } else {
                    skeletonToKeyword.put(skeleton, keyword);
                }
                if (error.length() == 0) {
                    keywordToErrors.put(keyword, "");
                } else {
                    keywordToErrors.put(keyword, "\tERROR: " + error);
                }
            }
        }
    }

    static void generateSamples() {
        Map<ULocale, SamplePatterns> localeToSamplePatterns = new LinkedHashMap();
        for (String[] row : SAMPLE_PATTERNS) {
            ULocale locale = new ULocale(row[0]);
            String keyword = row[1];
            String sample = row[2];
            SamplePatterns samplePatterns = localeToSamplePatterns.get(locale);
            if (samplePatterns == null) {
                localeToSamplePatterns.put(locale, samplePatterns = new SamplePatterns());
            }
            samplePatterns.put(keyword, sample);
        }
        LinkedHashSet<ULocale> skippedLocales = new LinkedHashSet<ULocale>();
        System.out.println("Locale\tPC\tPattern\tSample\tErrors");
        for (String localeString : FOCUS_LOCALES) {
            ULocale locale = new ULocale(localeString);
            PluralRules newRules = PluralRulesFactory.ALTERNATE.forLocale(locale);
            SamplePatterns samplePatterns = localeToSamplePatterns.get(locale);
            if (samplePatterns == null && NEW_LOCALES.contains(localeString)) {
                skippedLocales.add(locale);
                continue;
            }
            // check for errors. 
            samplePatterns.checkErrors(newRules.getKeywords());
            // now print.
            for (String keyword : newRules.getKeywords()) {
                String pattern = null;
                String error = null;
                Collection<NumberInfo> samples = newRules.getFractionSamples(keyword);
                NumberInfo first = samples.iterator().next();
                String sample = "??? " + first.toString();
                if (samplePatterns == null) {
                    pattern = "???";
                    error = "\tERROR: Locale data missing";
                } else {
                    pattern = samplePatterns.keywordToPattern.get(keyword);
                    error = samplePatterns.keywordToErrors.get(keyword);
                    if (pattern == null) {
                        pattern = "???";
                        error = "\tERROR: Needed for new rules";
                    } else {
                        sample = pattern.replace("{0}", first.toString());
                    }
                }
                System.out.println(locale + "\t" + keyword
                        + "\t" + pattern
                        + "\t" + sample
                        + error
                        );
            }
        }
        System.out.println("SKIP:\t\t\t" + skippedLocales);
    }


    static String[][] OLDRULES = {
        {"af", "one: n is 1"},
        {"am", "one: n in 0..1"},
        {"ar", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n mod 100 in 3..10;  many: n mod 100 in 11..99"},
        {"az", "other: null"},
        {"bg", "one: n is 1"},
        {"bn", "one: n is 1"},
        {"ca", "one: n is 1"},
        {"cs", "one: n is 1;  few: n in 2..4"},
        {"cy", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
        {"da", "one: n is 1"},
        {"de", "one: n is 1"},
        {"el", "one: n is 1"},
        {"en", "one: n is 1"},
        {"es", "one: n is 1"},
        {"et", "one: n is 1"},
        {"eu", "one: n is 1"},
        {"fa", "other: null"},
        {"fi", "one: n is 1"},
        {"fil", "one: n in 0..1"},
        {"fr", "one: n within 0..2 and n is not 2"},
        {"gl", "one: n is 1"},
        {"gu", "one: n is 1"},
        {"hi", "one: n in 0..1"},
        {"hr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
        {"hu", "other: null"},
        {"hy", "one: n is 1"},
        {"id", "other: null"},
        {"is", "one: n is 1"},
        {"it", "one: n is 1"},
        {"he", "one: n is 1;  two: n is 2;  many: n is not 0 and n mod 10 is 0"},
        {"ja", "other: null"},
        {"ka", "other: null"},
        {"kk", "one: n is 1"},
        {"km", "other: null"},
        {"kn", "other: null"},
        {"ko", "other: null"},
        {"ky", "one: n is 1"},
        {"lo", "other: null"},
        {"lt", "one: n mod 10 is 1 and n mod 100 not in 11..19;  few: n mod 10 in 2..9 and n mod 100 not in 11..19"},
        {"lv", "zero: n is 0;  one: n mod 10 is 1 and n mod 100 is not 11"},
        {"mk", "one: n mod 10 is 1 and n is not 11"},
        {"ml", "one: n is 1"},
        {"mn", "one: n is 1"},
        {"mr", "one: n is 1"},
        {"ms", "other: null"},
        {"my", "other: null"},
        {"ne", "one: n is 1"},
        {"nl", "one: n is 1"},
        {"nb", "one: n is 1"},
        {"pa", "one: n is 1"},
        {"pl", "one: n is 1;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n is not 1 and n mod 10 in 0..1 or n mod 10 in 5..9 or n mod 100 in 12..14"},
        {"ps", "one: n is 1"},
        {"pt", "one: n is 1"},
        {"ro", "one: n is 1;  few: n is 0 or n is not 1 and n mod 100 in 1..19"},
        {"ru", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
        {"si", "other: null"},
        {"sk", "one: n is 1;  few: n in 2..4"},
        {"sl", "one: n mod 100 is 1;  two: n mod 100 is 2;  few: n mod 100 in 3..4"},
        {"sq", "one: n is 1"},
        {"sr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
        {"sv", "one: n is 1"},
        {"sw", "one: n is 1"},
        {"ta", "one: n is 1"},
        {"te", "one: n is 1"},
        {"th", "other: null"},
        {"tr", "other: null"},
        {"uk", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
        {"ur", "one: n is 1"},
        {"uz", "other: null"},
        {"vi", "other: null"},
        {"zh", "other: null"},
        {"zu", "one: n is 1"},
    };

}
