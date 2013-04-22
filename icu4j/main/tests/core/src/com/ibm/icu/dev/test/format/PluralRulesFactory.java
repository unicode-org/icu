/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.NumberInfo;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public abstract class PluralRulesFactory {

    abstract boolean hasOverride(ULocale locale);

    abstract PluralRules forLocale(ULocale locale, PluralType ordinal);

    PluralRules forLocale(ULocale locale) {
        return forLocale(locale, PluralType.CARDINAL);
    }

    abstract ULocale[] getAvailableULocales();

    abstract ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable);

    static final PluralRulesFactory NORMAL = new PluralRulesFactoryVanilla();

    static final PluralRulesFactory ALTERNATE = new PluralRulesFactoryWithOverrides();

    private PluralRulesFactory() {}

    static class PluralRulesFactoryVanilla extends PluralRulesFactory {
        @Override
        boolean hasOverride(ULocale locale) {
            return false;
        }
        @Override
        PluralRules forLocale(ULocale locale, PluralType ordinal) {
            return PluralRules.forLocale(locale, ordinal);
        }
        @Override
        ULocale[] getAvailableULocales() {
            return PluralRules.getAvailableULocales();
        }
        @Override
        ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
            return PluralRules.getFunctionalEquivalent(locale, isAvailable);
        }
    }

    static class PluralRulesFactoryWithOverrides extends PluralRulesFactory {
        static Map<ULocale,PluralRules> OVERRIDES = new HashMap<ULocale,PluralRules>(); 
        static Relation<ULocale,NumberInfo> EXTRA_SAMPLES = Relation.of(new HashMap<ULocale,Set<NumberInfo>>(), HashSet.class); 
        static {
            String[][] overrides = {
                    {"bn", "one: n within 0..1"},
                    {"en,ca,de,et,fi,gl,it,nl,sv,sw,ta,te,ur", "one: j is 1"},
                    {"pt", "one: n is 1 or f is 1"},
                    {"cs,sk", "one: j is 1;  few: j in 2..4; many: v is not 0"},
                    {"cy", "one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
                    //{"el", "one: j is 1 or i is 0 and f is 1"},
                    {"da,is", "one: j is 1 or f is 1"},
                    {"fil", "one: j in 0..1"},
                    {"he", "one: j is 1;  two: j is 2", "10,20"},
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
                    {"sr,hr", "one: j mod 10 is 1 and j mod 100 is not 11" +
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
                    {"ro", "one: j is 1; few: v is not 0 or n is 0 or n is not 1 and n mod 100 in 1..19"},
                    {"ru", "one: j mod 10 is 1 and j mod 100 is not 11;" +
                            " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
//                            + "; many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
                            },
                            {"uk", "one: j mod 10 is 1 and j mod 100 is not 11;  " +
                            		"few: j mod 10 in 2..4 and j mod 100 not in 12..14;  " +
                            		"many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
                                    {"zu", "one: n within 0..1"},
            };
            for (String[] pair : overrides) {
                for (String locale : pair[0].split("\\s*,\\s*")) {
                    ULocale uLocale = new ULocale(locale);
                    if (OVERRIDES.containsKey(uLocale)) {
                        throw new IllegalArgumentException("Duplicate locale: " + uLocale);
                    }
                    try {
                        PluralRules rules = PluralRules.parseDescription(pair[1]);
                        OVERRIDES.put(uLocale, rules);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(locale + "\t" + pair[1], e);
                    }
                    if (pair.length==3) {
                        for (String item : pair[2].split("\\s*,\\s*")) {
                            EXTRA_SAMPLES.put(uLocale, new PluralRules.NumberInfo(item));
                        }
                    }
                }
            }
        }
        @Override
        boolean hasOverride(ULocale locale) {
            return OVERRIDES.containsKey(locale);
        }

        @Override
        PluralRules forLocale(ULocale locale, PluralType ordinal) {
            PluralRules override = ordinal != PluralType.CARDINAL 
                    ? null 
                            : OVERRIDES.get(locale);
            return override != null 
                    ? override
                            : PluralRules.forLocale(locale, ordinal);
        }

        @Override
        ULocale[] getAvailableULocales() {
            return PluralRules.getAvailableULocales(); // TODO fix if we add more locales
        }

        static final Map<String,ULocale> rulesToULocale = new HashMap();

        @Override
        ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
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
