// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ********************************************************************************
 * Copyright (C) 2003-2016, Google, International Business Machines Corporation
 * and others. All Rights Reserved.
 ********************************************************************************
*/
package com.ibm.icu.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;

/**
 * Provide information about gender in locales based on data in CLDR. Currently supplies gender of lists.
 * @author markdavis
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class GenderInfo {

    private final ListGenderStyle style; // set based on locale

    /**
     * Gender: OTHER means either the information is unavailable, or the person has declined to state MALE or FEMALE.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public enum Gender {
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        MALE,
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        FEMALE,
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        OTHER
    }

    /**
     * Create GenderInfo from a ULocale.
     * @param uLocale desired locale
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static GenderInfo getInstance(ULocale uLocale) {
        return genderInfoCache.get(uLocale);
    }

    /**
     * Create GenderInfo from a Locale.
     * @param locale desired locale
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static GenderInfo getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Enum only meant for use in CLDR and in testing. Indicates the category for the locale.
     * This only affects gender for lists more than one. For lists of 1 item, the gender
     * of the list always equals the gender of that sole item.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public enum ListGenderStyle {
        /**
         * For an empty list, returns OTHER;
         * For a single item, returns its gender;
         * Otherwise always OTHER.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        NEUTRAL,
        /**
         * For an empty list, returns OTHER;
         * For a single item, returns its gender;
         * Otherwise gender(all male) = male, gender(all female) = female, otherwise gender(list) = other.
         * So any 'other' value makes the overall gender be 'other'.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        MIXED_NEUTRAL,
        /**
         * For an empty list, returns OTHER;
         * For a single item, returns its gender;
         * Otherwise, gender(all female) = female, otherwise gender(list) = male.
         * So for more than one item, any 'other' value makes the overall gender be 'male'.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        MALE_TAINTS;

        private static Map<String, ListGenderStyle> fromNameMap =
            new HashMap<String, ListGenderStyle>(3);

        static {
            fromNameMap.put("neutral", NEUTRAL);
            fromNameMap.put("maleTaints", MALE_TAINTS);
            fromNameMap.put("mixedNeutral", MIXED_NEUTRAL);
        }

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public static ListGenderStyle fromName(String name) {
            ListGenderStyle result = fromNameMap.get(name);
            if (result == null) {
                throw new IllegalArgumentException("Unknown gender style name: " + name);
            }
            return result;
        }
    }

    /**
     * Get the gender of a list, based on locale usage.
     * @param genders a list of genders.
     * @return the gender of the list.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Gender getListGender(Gender... genders) {
        return getListGender(Arrays.asList(genders));
    }

    /**
     * Get the gender of a list, based on locale usage.
     * @param genders a list of genders.
     * @return the gender of the list.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Gender getListGender(List<Gender> genders) {
        if (genders.size() == 0) {
            return Gender.OTHER; // degenerate case
        }
        if (genders.size() == 1) {
            return genders.get(0); // degenerate case
        }
        switch(style) {
        case NEUTRAL:
            return Gender.OTHER;
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
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public GenderInfo(ListGenderStyle genderStyle) {
        style = genderStyle;
    }

    private static GenderInfo neutral = new GenderInfo(ListGenderStyle.NEUTRAL);

    private static class Cache {
        private final ICUCache<ULocale, GenderInfo> cache =
            new SimpleCache<ULocale, GenderInfo>();

        public GenderInfo get(ULocale locale) {
            GenderInfo result = cache.get(locale);
            if (result == null) {
                result = load(locale);
                if (result == null) {
                    ULocale fallback = locale.getFallback();

                    // We call get() recursively so that we can leverage the cache
                    // for all fallback locales too. If we get to the root locale,
                    // and find no resource assume that list gender style is NEUTRAL.
                    result = fallback == null ? neutral : get(fallback);
                }
                cache.put(locale, result);
            }
            return result;
        }

        private static GenderInfo load(ULocale ulocale) {
            UResourceBundle rb = UResourceBundle.getBundleInstance(
                    ICUData.ICU_BASE_NAME,
                    "genderList",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER, true);
            UResourceBundle genderList = rb.get("genderList");
            try {
                return new GenderInfo(
                        ListGenderStyle.fromName(genderList.getString(ulocale.toString())));
            } catch (MissingResourceException mre) {
                return null;
            }
        }
    }

    private static Cache genderInfoCache = new Cache();
}
