/*
 *******************************************************************************
 * Copyright (C) 2003-2012, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provide information about gender in locales based on data in CLDR. Currently supplies gender of lists.
 * @author markdavis
 * @internal
 */
public class GenderInfo {

    private final ListGenderStyle style; // set based on locale

    /**
     * Gender: OTHER means either the information is unavailable, or the person has declined to state MALE or FEMALE. 
     * @internal
     */
    public enum Gender {MALE, FEMALE, OTHER}

    /**
     * Create GenderInfo from a ULocale.
     * @param uLocale desired locale
     * @internal
     */
    public static GenderInfo getInstance(ULocale uLocale) {
        // These can be cached, since they are read-only
        // poor-man's locale lookup, for hardcoded data
        while (true) {
            GenderInfo data = localeToListGender.get(uLocale);
            if (data != null) {
                return data;
            }
            uLocale = uLocale.getFallback();
            if (uLocale == null) {
                return neutral;
            }
        }
    }

    /**
     * Create GenderInfo from a Locale.
     * @param locale desired locale
     * @internal
     */
    public static GenderInfo getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Enum only meant for use in CLDR and in testing. Indicates the category for the locale.
     * @internal
     */
    public enum ListGenderStyle {
        /**
         * Always OTHER (if more than one)
         */
        NEUTRAL, 
        /**
         * gender(all male) = male, gender(all female) = female, otherwise gender(list) = other.
         * In particular, any 'other' value makes the overall gender be 'other'.
         */
        MIXED_NEUTRAL, 
        /**
         * gender(all female) = female, otherwise gender(list) = male.
         * In particular, any 'other' value makes the overall gender be 'male'.
         */
        MALE_TAINTS
    }

    /**
     * Reset the data used for mapping locales to styles. Only for use in CLDR and in testing!
     * @param newULocaleToListGender replacement data, copied internally for safety.
     * @internal
     */
    public static void setLocaleMapping(Map<ULocale,GenderInfo> newULocaleToListGender) {
        localeToListGender.clear();
        localeToListGender.putAll(newULocaleToListGender);
    }

    /**
     * Get the gender of a list, based on locale usage.
     * @param genders a list of genders.
     * @return the gender of the list.
     * @internal
     */
    public Gender getListGender(Gender... genders) {
        return getListGender(Arrays.asList(genders));
    }

    /**
     * Get the gender of a list, based on locale usage.
     * @param genders a list of genders.
     * @return the gender of the list.
     * @internal
     */
    public Gender getListGender(List<Gender> genders) {
        if (genders.size() == 0 || style == ListGenderStyle.NEUTRAL) {
            return Gender.OTHER; // degenerate case
        }
        if (genders.size() == 1) {
            return genders.get(0); // degenerate case
        }
        switch(style) {
        case MIXED_NEUTRAL:
            boolean hasFemale = false;
            boolean hasMale = false;
            for (Gender gender : genders) {
                switch (gender) {
                case FEMALE:
                    if (hasMale) {
                        return Gender.OTHER;
                    }
                    hasFemale = true;
                    break;
                case MALE: 
                    if (hasFemale) {
                        return Gender.OTHER;
                    }
                    hasMale = true;
                    break;
                case OTHER:
                    return Gender.OTHER;
                }
            }
            return hasMale ? Gender.MALE : Gender.FEMALE;
            // Note: any OTHER would have caused a return in the loop, which always happens.
        case MALE_TAINTS:
            for (Gender gender : genders) {
                if (gender != Gender.FEMALE) {
                    return Gender.MALE;
                }
            }
            return Gender.FEMALE;
        default:
            return Gender.OTHER; 
        }
    }
    
    /**
     * Only for testing and use with CLDR.
     * @param genderStyle gender style
     * @internal
     */
    public GenderInfo(ListGenderStyle genderStyle) {
        style = genderStyle;
    }
    
    private static GenderInfo neutral = new GenderInfo(ListGenderStyle.NEUTRAL);

    // TODO Get this data from a resource bundle generated from CLDR. 
    // For now, hard coded.

    private static Map<ULocale,GenderInfo> localeToListGender = new HashMap<ULocale,GenderInfo>();
    static {
        GenderInfo taints = new GenderInfo(ListGenderStyle.MALE_TAINTS);
        for (String locale : Arrays.asList("ar", "ca", "cs", "hr", "es", "fr", "he", "hi", "it", "lt", "lv", "mr", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sr", "uk", "ur", "zh")) {
            localeToListGender.put(new ULocale(locale), taints);
        }
        GenderInfo mixed = new GenderInfo(ListGenderStyle.MIXED_NEUTRAL);
        for (String locale : Arrays.asList("el", "is")) {
            localeToListGender.put(new ULocale(locale), mixed);
        }
    }
}
