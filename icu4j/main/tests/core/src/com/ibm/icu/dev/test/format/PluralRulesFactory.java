/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.NumberInfo;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public abstract class PluralRulesFactory extends PluralRules.Factory {

    abstract boolean hasOverride(ULocale locale);

    public abstract PluralRules forLocale(ULocale locale, PluralType ordinal);

    public abstract ULocale[] getAvailableULocales();

    public abstract ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable);

    static final PluralRulesFactory NORMAL = new PluralRulesFactoryVanilla();

    static final PluralRulesFactory ALTERNATE = new PluralRulesFactoryWithOverrides();

    private PluralRulesFactory() {}

    static class PluralRulesFactoryVanilla extends PluralRulesFactory {
        @Override
        boolean hasOverride(ULocale locale) {
            return false;
        }
        @Override
        public PluralRules forLocale(ULocale locale, PluralType ordinal) {
            return PluralRules.forLocale(locale, ordinal);
        }
        @Override
        public ULocale[] getAvailableULocales() {
            return PluralRules.getAvailableULocales();
        }
        @Override
        public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
            return PluralRules.getFunctionalEquivalent(locale, isAvailable);
        }
    }

    static class PluralRulesFactoryWithOverrides extends PluralRulesFactory {
        @Override
        boolean hasOverride(ULocale locale) {
            return getPluralOverrides().containsKey(locale);
        }

        @Override
        public PluralRules forLocale(ULocale locale, PluralType ordinal) {
            PluralRules override = ordinal != PluralType.CARDINAL 
                    ? null 
                            : getPluralOverrides().get(locale);
            return override != null 
                    ? override
                            : PluralRules.forLocale(locale, ordinal);
        }

        @Override
        public ULocale[] getAvailableULocales() {
            return PluralRules.getAvailableULocales(); // TODO fix if we add more locales
        }

        static final Map<String,ULocale> rulesToULocale = new HashMap();

        @Override
        public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
            if (rulesToULocale.isEmpty()) {
                for (ULocale locale2 : getAvailableULocales()) {
                    String rules = forLocale(locale2).toString();
                    ULocale old = rulesToULocale.get(rules);
                    if (old == null) {
                        rulesToULocale.put(rules, locale2);
                    }
                }
            }
            String rules = forLocale(locale).toString();
            ULocale result = rulesToULocale.get(rules);
            return result == null ? ULocale.ROOT : result;
        }
    };

    static class SamplePatterns {
        final Map<String,String> keywordToPattern = new TreeMap(PluralRules.KEYWORD_COMPARATOR);
        final Map<String,String> keywordToErrors = new HashMap();
        public void put(ULocale locale, String keyword, String sample) {
            if (keywordToPattern.containsKey(keyword)) {
                throw new IllegalArgumentException("Duplicate keyword <" + keyword + "> for " + locale);
            } else {
                keywordToPattern.put(keyword, sample.replace(" ", "\u00A0"));
            }
        }
        public void checkErrors(Set<String> set) {
            final Map<String,String> skeletonToKeyword = new HashMap();
            for (String keyword : set) {
                String error = "";
                String sample = keywordToPattern.get(keyword);
                String skeleton = sample.replace(" ", "").replaceAll("\\s*\\{0\\}\\s*", "");
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


    public static Map<ULocale, SamplePatterns> getLocaleToSamplePatterns() {
        if (LOCALE_TO_SAMPLE_PATTERNS == null) {
            loadData();
        }
        return LOCALE_TO_SAMPLE_PATTERNS;
    }
    public static Map<ULocale, PluralRules> getPluralOverrides() {
        if (OVERRIDES == null) {
            loadData();
        }
        return OVERRIDES;
    }
    public static Relation<ULocale, NumberInfo> getExtraSamples() {
        if (EXTRA_SAMPLES == null) {
            loadData();
        }
        return EXTRA_SAMPLES;
    }

    private static Map<ULocale, SamplePatterns> LOCALE_TO_SAMPLE_PATTERNS = null;    
    private static Map<ULocale,PluralRules> OVERRIDES = null; 
    private static Relation<ULocale,NumberInfo> EXTRA_SAMPLES = null; 

    private static void loadData() {
        LinkedHashMap<ULocale, SamplePatterns> temp = new LinkedHashMap<ULocale, SamplePatterns>();
        HashMap<ULocale, PluralRules> tempOverrides = new HashMap<ULocale,PluralRules>();
        Relation<ULocale, NumberInfo> tempSamples = Relation.of(new HashMap<ULocale,Set<NumberInfo>>(), HashSet.class);
        for (String[] row : SAMPLE_PATTERNS) {
            ULocale locale = new ULocale(row[0]);
            String keyword = row[1];
            String sample = row[2];
            SamplePatterns samplePatterns = temp.get(locale);
            if (samplePatterns == null) {
                temp.put(locale, samplePatterns = new SamplePatterns());
            }
            samplePatterns.put(locale, keyword, sample);
        }
        for (String[] pair : overrides) {
            for (String locale : pair[0].split("\\s*,\\s*")) {
                ULocale uLocale = new ULocale(locale);
                if (tempOverrides.containsKey(uLocale)) {
                    throw new IllegalArgumentException("Duplicate locale: " + uLocale);
                }
                try {
                    PluralRules rules = PluralRules.parseDescription(pair[1]);
                    tempOverrides.put(uLocale, rules);
                } catch (Exception e) {
                    throw new IllegalArgumentException(locale + "\t" + pair[1], e);
                }
            }
        }
        for (String[] pair : EXTRA_SAMPLE_SOURCE) {
            for (String locale : pair[0].split("\\s*,\\s*")) {
                ULocale uLocale = new ULocale(locale);
                if (tempSamples.containsKey(uLocale)) {
                    throw new IllegalArgumentException("Duplicate locale: " + uLocale);
                }
                for (String item : pair[1].split("\\s*,\\s*")) {
                    tempSamples.put(uLocale, new PluralRules.NumberInfo(item));
                }
            }
        }
        LOCALE_TO_SAMPLE_PATTERNS = Collections.unmodifiableMap(temp);
        OVERRIDES = Collections.unmodifiableMap(tempOverrides);
        EXTRA_SAMPLES = (Relation<ULocale, NumberInfo>) tempSamples.freeze();
    }


//    static String[][] OLDRULES = {
//        {"af", "one: n is 1"},
//        {"am", "one: n in 0..1"},
//        {"ar", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n mod 100 in 3..10;  many: n mod 100 in 11..99"},
//        {"az", "other: null"},
//        {"bg", "one: n is 1"},
//        {"bn", "one: n is 1"},
//        {"ca", "one: n is 1"},
//        {"cs", "one: n is 1;  few: n in 2..4"},
//        {"cy", "zero: n is 0;  one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
//        {"da", "one: n is 1"},
//        {"de", "one: n is 1"},
//        {"el", "one: n is 1"},
//        {"en", "one: n is 1"},
//        {"es", "one: n is 1"},
//        {"et", "one: n is 1"},
//        {"eu", "one: n is 1"},
//        {"fa", "other: null"},
//        {"fi", "one: n is 1"},
//        {"fil", "one: n in 0..1"},
//        {"fr", "one: n within 0..2 and n is not 2"},
//        {"gl", "one: n is 1"},
//        {"gu", "one: n is 1"},
//        {"hi", "one: n in 0..1"},
//        {"hr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
//        {"hu", "other: null"},
//        {"hy", "one: n is 1"},
//        {"id", "other: null"},
//        {"is", "one: n is 1"},
//        {"it", "one: n is 1"},
//        {"he", "one: n is 1;  two: n is 2;  many: n is not 0 and n mod 10 is 0"},
//        {"ja", "other: null"},
//        {"ka", "other: null"},
//        {"kk", "one: n is 1"},
//        {"km", "other: null"},
//        {"kn", "other: null"},
//        {"ko", "other: null"},
//        {"ky", "one: n is 1"},
//        {"lo", "other: null"},
//        {"lt", "one: n mod 10 is 1 and n mod 100 not in 11..19;  few: n mod 10 in 2..9 and n mod 100 not in 11..19"},
//        {"lv", "zero: n is 0;  one: n mod 10 is 1 and n mod 100 is not 11"},
//        {"mk", "one: n mod 10 is 1 and n is not 11"},
//        {"ml", "one: n is 1"},
//        {"mn", "one: n is 1"},
//        {"mr", "one: n is 1"},
//        {"ms", "other: null"},
//        {"my", "other: null"},
//        {"ne", "one: n is 1"},
//        {"nl", "one: n is 1"},
//        {"nb", "one: n is 1"},
//        {"pa", "one: n is 1"},
//        {"pl", "one: n is 1;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n is not 1 and n mod 10 in 0..1 or n mod 10 in 5..9 or n mod 100 in 12..14"},
//        {"ps", "one: n is 1"},
//        {"pt", "one: n is 1"},
//        {"ro", "one: n is 1;  few: n is 0 or n is not 1 and n mod 100 in 1..19"},
//        {"ru", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
//        {"si", "other: null"},
//        {"sk", "one: n is 1;  few: n in 2..4"},
//        {"sl", "one: n mod 100 is 1;  two: n mod 100 is 2;  few: n mod 100 in 3..4"},
//        {"sq", "one: n is 1"},
//        {"sr", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
//        {"sv", "one: n is 1"},
//        {"sw", "one: n is 1"},
//        {"ta", "one: n is 1"},
//        {"te", "one: n is 1"},
//        {"th", "other: null"},
//        {"tr", "other: null"},
//        {"uk", "one: n mod 10 is 1 and n mod 100 is not 11;  few: n mod 10 in 2..4 and n mod 100 not in 12..14;  many: n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14"},
//        {"ur", "one: n is 1"},
//        {"uz", "other: null"},
//        {"vi", "other: null"},
//        {"zh", "other: null"},
//        {"zu", "one: n is 1"},
//    };

    static String[][] SAMPLE_PATTERNS = {
        {"und", "zero", "{0} ADD-SAMPLE-ZERO"},
        {"und", "one", "{0} ADD-SAMPLE-ONE"},
        {"und", "two", "{0} ADD-SAMPLE-TWO"},
        {"und", "few", "{0} ADD-SAMPLE-FEW"},
        {"und", "many", "{0} ADD-SAMPLE-MANY"},
        {"und", "other", "{0} ADD-SAMPLE-OTHER"},
        {"af", "one", "{0} dag"},
        {"af", "other", "{0} dae"},
        {"am", "one", "{0} ቀን"},
        {"am", "other", "{0} ቀናት"}, // fixed to 'other'
        {"ar", "few", "{0} ساعات"},
        {"ar", "many", "{0} ساعة"},
        {"ar", "one", "ساعة"},
        {"ar", "other", "{0} ساعة"},
        {"ar", "two", "ساعتان"},
        {"ar", "zero", "{0} ساعة"},
        {"az", "one",   "Sizin alış-veriş katınızda {0} X var. Onu almaq istəyirsiniz mi?"},
        {"az", "other", "Sizin alış-veriş kartınızda {0} X var. Onları almaq istəyirsiniz mi?"},
        {"bg", "one", "{0} ден"},
        {"bg", "other", "{0} дена"},
        {"bn", "one", "সসে {0}টি আপেল নিয়ে সেটা খেল"},
        {"bn", "other", "সসে {0}টি আপেল নিয়ে সেগুলি খেল"},
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
        {"cy", "zero", "{0} cŵn, {0} cathod"},
        {"cy", "one", "{0} ci, {0} gath"},
        {"cy", "two", "{0} gi, {0} gath"},
        {"cy", "few", "{0} chi, {0} cath"},
        {"cy", "many", "{0} chi, {0} chath"},
        {"cy", "other", "{0} ci, {0} cath"},
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
        {"fa", "one", "او {0} فیلم در هفته می‌بیند که کمدی است."},
        {"fa", "other", "او {0} فیلم در هفته می‌بیند که کمدی هستند."},
        {"fi", "one", "{0} päivä"},
        {"fi", "other", "{0} päivää"},
        {"fil", "one", "sa {0} araw"},
        {"fil", "other", "sa {0} (na) araw"},
        {"fr", "one", "{0} jour"},
        {"fr", "other", "{0} jours"},
        {"gl", "one", "{0} día"},
        {"gl", "other", "{0} días"},
        {"gu", "one", "{0} કિલોગ્રામ"},
        {"gu", "other", "{0} કિલોગ્રામ્સ"},
        {"he", "many", "{0} שנה"},
        {"he", "one", "שנה"},
        {"he", "other", "{0} שנים"},
        {"he", "two", "שנתיים"},
        {"hi", "one", "{0} घंटा"},
        {"hi", "other", "{0} घंटे"},
        {"hr", "few", "za {0} mjeseca"},
        {"hr", "many", "za {0} mjeseci"},
        {"hr", "one", "za {0} mjesec"},
        {"hr", "other", "za {0} mjeseci"},
        {"hu", "one", "A kosárban van {0} X. Meg akarja venni?"},
        {"hu", "other", "A kosárban van {0} X. Meg akarja venni őket?"},
        {"hy", "one", "այդ {0} ժամը"},
        {"hy", "other", "այդ {0} ժամերը"},
        {"id", "other", "{0} hari"},
        {"is", "one", "{0} dagur"},
        {"is", "other", "{0} dagar"},
        {"it", "one", "{0} giorno"},
        {"it", "other", "{0} giorni"},
        {"ja", "other", "{0}日"},
        {"ka", "one",   "თქვენი კალათა შეიცავს {0} X-ს. გსურთ მისი შეძენა?"}, // 
        {"ka", "other", "თქვენი კალათა შეიცავს {0} X-ს. გსურთ მათი შეძენა?"}, // 
        {"kk", "one",   "Кәрзеңкеде {0} X бар. Оны сатып алғыңыз келе ме?"}, // 
        {"kk", "other", "Кәрзеңкеде {0} X бар. Оларды сатып алғыңыз келе ме?"}, // 
        {"km", "other", "{0} ថ្ងៃ"},
        {"kn", "one", "{0} ದಿನ"},
        {"kn", "other", "{0} ದಿನಗಳು"},
        {"ko", "other", "{0}일"},
        {"ky", "one", "Сиздин дүкөнчүлөө картыңызда {0} X бар. Аны сатып алайын дейсизби?"},
        {"ky", "other", "Сиздин дүкөнчүлөө картыңызда {0} X бар. Аларды сатып алайын дейсизби?"},
        {"lo", "other", "{0} ມື້"},
        {"lt", "few", "{0} dienos"},
        {"lt", "one", "{0} diena"},
        {"lt", "other", "{0} dienų"},
        {"lv", "one", "{0} diennakts"},
        {"lv", "other", "{0} diennaktis"},
        {"lv", "zero", "{0} diennakšu"},
        {"mk", "one", "{0} ден"},
        {"mk", "other", "{0} дена"},
        {"ml", "one", "{0} വ്യക്തി"},
        {"ml", "other", "{0} വ്യക്തികൾ"},
        {"mn", "one",   "Таны картанд {0} X байна. Та энийг худалдаж авмаар байна уу?"},
        {"mn", "other", "Таны картанд {0} Х байна. Та эднийг худалдаж авмаар байна уу?"},
        {"mr", "one", "{0} घर"},
        {"mr", "other", "{0} घरे"},
        {"ms", "other", "{0} hari"},
        {"my", "other", "{0}ရက္"},
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
        {"pa", "one", "{0} ਘੰਟਾ"},
        {"pa", "other", "{0} ਘੰਟੇ"},
        {"pl", "few", "{0} miesiące"},
        {"pl", "many", "{0} miesięcy"},
        {"pl", "one", "{0} miesiąc"},
        {"pl", "other", "{0} miesiąca"},
        {"pt", "one", "{0} ponto"},
        {"pt", "other", "{0} pontos"},
        //        {"pt_PT", "one", "{0} dia"},
        //        {"pt_PT", "other", "{0} dias"},
        {"ro", "few", "{0} zile"},
        {"ro", "one", "{0} zi"},
        {"ro", "other", "{0} de zile"},
        {"ru", "few", "{0} года"},
        {"ru", "many", "{0} лет"},
        {"ru", "one", "{0} год"},
        {"ru", "other", "{0} года"},
        {"si", "one", "මට පොත් {0}ක් තිබේ. මම එය කියවීමි.්"},
        {"si", "other", "මට පොත් {0}ක් තිබේ. මම ඒවා කියවීමි."},
        {"sk", "few", "{0} dni"},
        {"sk", "one", "{0} deň"},
        {"sk", "other", "{0} dní"},
        {"sk", "many", "{0} dňa"}, // added from spreadsheet
        {"sl", "few", "{0} ure"},
        {"sl", "one", "{0} ura"},
        {"sl", "other", "{0} ur"},
        {"sl", "two", "{0} uri"},
        {"sq", "one", "{0} libër"},
        {"sq", "other", "{0} libra"},
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
        {"tr", "one",   "Sizin alış-veriş kartınızda {0} X var. Onu almak istiyor musunuz?"},
        {"tr", "other", "Sizin alış-veriş kartınızda {0} X var. Onları almak istiyor musunuz?"},
        {"uk", "few", "{0} дні"},
        {"uk", "many", "{0} днів"},
        {"uk", "one", "{0} день"},
        {"uk", "other", "{0} дня"},
        {"ur", "one", "{0} گھنٹہ"},
        {"ur", "other", "{0} گھنٹے"},
        {"uz", "one",   "Xarid savatingizda {0} X bor. Uni sotib olishni xohlaysizmi?"},
        {"uz", "other", "Xaridingiz savatida {0} X bor. Ularni sotib olishni xohlaysizmi?"},
        {"vi", "other", "{0} ngày"},
        {"zh", "other", "{0} 天"},
        {"zh_Hant", "other", "{0} 日"},     
        {"en", "one", "{0} day"},        // added from spreadsheet  
        {"en", "other", "{0} days"},       // added from spreadsheet   
        {"zu", "one", "{0} usuku"},     // added from spreadsheet
        {"zu", "other", "{0} izinsuku"},          // added from spreadsheet
    };

    static String[][] EXTRA_SAMPLE_SOURCE = {
        {"he,iw","10,20"},
        {"und,az,ka,kk,ky,mk,mn,my,pa,sq,uz","0,0.0,0.1,1,1.0,1.1,2.0,2.1,3,4,5,10,11,1.2,1.121"},
    };

    static String[][] overrides = {
        {"gu,mr,kn,am,fa", "one: n within 0..1"},
        {"ta,te,kk,uz,ky,hu,az,ka,mn,tr", "one: n is 1"},
        {"bn", "one: n within 0..1"},
        {"en,ca,de,et,fi,gl,it,nl,sw,ur", "one: j is 1"},
        {"sv", "one: j is 1 or f is 1"},
        {"pt", "one: n is 1 or f is 1"},
        {"si", "one: n in 0,1 or i is 0 and f is 1"},
        {"cs,sk", "one: j is 1;  few: j in 2..4; many: v is not 0"},
        //{"cy", "one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
        //{"el", "one: j is 1 or i is 0 and f is 1"},
        {"da,is", "one: j is 1 or f is 1"},
        {"fil,tl", "one: j in 0..1"},
        {"he,iw", "one: j is 1;  two: j is 2; many: j not in 0..10 and j mod 10 is 0", "10,20"},
        {"hi", "one: n within 0..1"},
        {"hy", "one: n within 0..2 and n is not 2"},
        //                    {"hr", "one: j mod 10 is 1 and j mod 100 is not 11;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
        {"lv", "zero: n mod 10 is 0" +
                " or n mod 10 in 11..19" +
                " or v is 2 and f mod 10 in 11..19;" +
                "one: n mod 10 is 1 and n mod 100 is not 11" +
                " or v is 2 and f mod 10 is 1 and f mod 100 is not 11" +
        " or v is not 2 and f mod 10 is 1"},
        //                    {"lv", "zero: n mod 10 is 0" +
        //                            " or n mod 10 in 11..19" +
        //                            " or v in 1..6 and f is not 0 and f mod 10 is 0" +
        //                            " or v in 1..6 and f mod 10 in 11..19;" +
        //                            "one: n mod 10 is 1 and n mod 100 is not 11" +
        //                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
        //                            " or v not in 0..6 and f mod 10 is 1"},
        {"pl", "one: j is 1;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j is not 1 and j mod 10 in 0..1 or j mod 10 in 5..9 or j mod 100 in 12..14"},
        {"sl", "one: j mod 100 is 1;  two: j mod 100 is 2;  few: j mod 100 in 3..4 or v is not 0"},
        //                    {"sr", "one: j mod 10 is 1 and j mod 100 is not 11" +
        //                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
        //                            " or v not in 0..6 and f mod 10 is 1;" +
        //                            "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
        //                            " or v in 1..6 and f mod 10 in 2..4 and f mod 100 not in 12..14" +
        //                            " or v not in 0..6 and f mod 10 in 2..4"
        //                    },
        {"sr,hr,sh,bs", "one: j mod 10 is 1 and j mod 100 is not 11" +
                " or f mod 10 is 1 and f mod 100 is not 11;" +
                "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
                " or f mod 10 in 2..4 and f mod 100 not in 12..14"
        },
        // +
        //                            " ; many: j mod 10 is 0 " +
        //                            " or j mod 10 in 5..9 " +
        //                            " or j mod 100 in 11..14" +
        //                            " or v in 1..6 and f mod 10 is 0" +
        //                            " or v in 1..6 and f mod 10 in 5..9" +
        //                            " or v in 1..6 and f mod 100 in 11..14" +
        //                    " or v not in 0..6 and f mod 10 in 5..9"
        {"mo,ro", "one: j is 1; few: v is not 0 or n is 0 or n is not 1 and n mod 100 in 1..19"},
        {"ru", "one: j mod 10 is 1 and j mod 100 is not 11;" +
                " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
                //                            + "; many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
        },
        {"uk", "one: j mod 10 is 1 and j mod 100 is not 11;  " +
                "few: j mod 10 in 2..4 and j mod 100 not in 12..14;  " +
        "many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
        {"zu", "one: n within 0..1"},
        {"mk", "one: n mod 10 in 0..1 and n mod 100 not in 0,11 or f mod 10 is 1 and f mod 10 is not 11"},
        {"pa", "one: n in 0..1"},
    };

}
