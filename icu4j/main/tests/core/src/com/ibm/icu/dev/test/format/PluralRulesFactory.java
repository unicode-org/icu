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
                    {"en,ca,de,et,fi,gl,it,nl,pt,sv,sw,ta,te,ur", "one: j is 1"},
                    {"cs,sk", "one: j is 1;  few: j in 2..4; many: v is not 0"},
                    //{"el", "one: j is 1 or i is 0 and f is 1"},
                    {"da,is", "one: j is 1 or f is 1"},
                    {"fil", "one: j in 0..1"},
                    {"he", "one: j is 1;  two: j is 2", "10,20"},
                    {"hi", "one: n within 0..1"},
                    {"hr", "one: j mod 10 is 1 and j mod 100 is not 11;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
                    {"lv", "zero: n mod 10 is 0" +
                            " or n mod 10 in 11..19" +
                            " or v in 1..6 and f is not 0 and f mod 10 is 0" +
                            " or v in 1..6 and f mod 10 in 11..19;" +
                            "one: n mod 10 is 1 and n mod 100 is not 11" +
                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
                    " or v not in 0..6 and f mod 10 is 1"},
                    {"pl", "one: j is 1;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j is not 1 and j mod 10 in 0..1 or j mod 10 in 5..9 or j mod 100 in 12..14"},
                    {"sl", "one: j mod 100 is 1;  two: j mod 100 is 2;  few: j mod 100 in 3..4 or v is not 0"},
                    {"sr", "one: j mod 10 is 1 and j mod 100 is not 11" +
                            " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
                            " or v not in 0..6 and f mod 10 is 1;" +
                            " few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
                            " or v in 1..6 and f mod 10 in 2..4 and f mod 100 not in 12..14" +
                            " or v not in 0..6 and f mod 10 in 2..4;" +
                            " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14" +
                            " or v in 1..6 and f mod 10 in 5..9" +
                            " or v in 1..6 and f mod 100 in 11..14" +
                    " or v not in 0..6 and f mod 10 in 5..9"},
                    {"ro", "one: j is 1; few: n is 0 or n is not 1 and n mod 100 in 1..19"},
                    {"ru,uk", "one: j mod 10 is 1 and j mod 100 is not 11;" +
                            " few: j mod 10 in 2..4 and j mod 100 not in 12..14;" +
                    " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
            };
            for (String[] pair : overrides) {
                for (String locale : pair[0].split("\\s*,\\s*")) {
                    ULocale uLocale = new ULocale(locale);
                    if (OVERRIDES.containsKey(uLocale)) {
                        throw new IllegalArgumentException("Duplicate locale: " + uLocale);
                    }
                    OVERRIDES.put(uLocale, PluralRules.createRules(pair[1]));
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
            PluralRules override = ordinal != PluralType.CARDINAL ? null : OVERRIDES.get(locale);
            return override != null ? override: PluralRules.forLocale(locale, ordinal);
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
}
