// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.SingleUnitImpl;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.MeasureUnit.Complexity;
import com.ibm.icu.util.MeasureUnit.MeasurePrefix;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Takes care of formatting currency and measurement unit names, as well as populating the gender of measure units.
 */
public class LongNameHandler
    implements MicroPropsGenerator, ModifierStore, LongNameMultiplexer.ParentlessMicroPropsGenerator {

    private static int i = 0;
    private static final int DNAM_INDEX = StandardPlural.COUNT + i++;
    private static final int PER_INDEX = StandardPlural.COUNT + i++;
    private static final int GENDER_INDEX = StandardPlural.COUNT + i++;
    static final int ARRAY_LENGTH = StandardPlural.COUNT + i++;

    // Returns the array index that corresponds to the given pluralKeyword.
    private static int getIndex(String pluralKeyword) {
        // pluralKeyword can also be "dnam", "per" or "gender"
        if (pluralKeyword.equals("dnam")) {
            return DNAM_INDEX;
        } else if (pluralKeyword.equals("per")) {
            return PER_INDEX;
        } else if (pluralKeyword.equals("gender")) {
            return GENDER_INDEX;
        } else {
            return StandardPlural.fromString(pluralKeyword).ordinal();
        }
    }

    static String getWithPlural(String[] strings, StandardPlural plural) {
        String result = strings[plural.ordinal()];
        if (result == null) {
            result = strings[StandardPlural.OTHER.ordinal()];
        }
        if (result == null) {
            // There should always be data in the "other" plural variant.
            throw new ICUException("Could not find data in 'other' plural variant");
        }
        return result;
    }

    private enum PlaceholderPosition { NONE, BEGINNING, MIDDLE, END }

    private static class ExtractCorePatternResult {
        String coreUnit;
        PlaceholderPosition placeholderPosition;
        char joinerChar;
    }

    /**
     * Returns three outputs extracted from pattern.
     *
     * @param coreUnit is extracted as per Extract(...) in the spec:
     *   https://unicode.org/reports/tr35/tr35-general.html#compound-units
     * @param PlaceholderPosition indicates where in the string the placeholder
     *   was found.
     * @param joinerChar Iff the placeholder was at the beginning or end,
     *   joinerChar contains the space character (if any) that separated the
     *   placeholder from the rest of the pattern. Otherwise, joinerChar is set
     *   to NUL. Only one space character is considered.
     */
    private static ExtractCorePatternResult extractCorePattern(String pattern) {
        ExtractCorePatternResult result = new ExtractCorePatternResult();
        result.joinerChar = 0;
        int len = pattern.length();
        if (pattern.startsWith("{0}")) {
            result.placeholderPosition = PlaceholderPosition.BEGINNING;
            if (len > 3 && Character.isSpaceChar(pattern.charAt(3))) {
                result.joinerChar = pattern.charAt(3);
                result.coreUnit = pattern.substring(4);
            } else {
                result.coreUnit = pattern.substring(3);
            }
        } else if (pattern.endsWith("{0}")) {
            result.placeholderPosition = PlaceholderPosition.END;
            if (Character.isSpaceChar(pattern.charAt(len - 4))) {
                result.coreUnit = pattern.substring(0, len - 4);
                result.joinerChar = pattern.charAt(len - 4);
            } else {
                result.coreUnit = pattern.substring(0, len - 3);
            }
        } else if (pattern.indexOf("{0}", 1) == -1) {
            result.placeholderPosition = PlaceholderPosition.NONE;
            result.coreUnit = pattern;
        } else {
            result.placeholderPosition = PlaceholderPosition.MIDDLE;
            result.coreUnit = pattern;
        }
        return result;
    }

    //////////////////////////
    /// BEGIN DATA LOADING ///
    //////////////////////////

    // Gets the gender of a built-in unit: unit must be a built-in. Returns an empty
    // string both in case of unknown gender and in case of unknown unit.
    private static String getGenderForBuiltin(ULocale locale, MeasureUnit builtinUnit) {
        ICUResourceBundle unitsBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);

        StringBuilder key = new StringBuilder();
        key.append("units/");
        key.append(builtinUnit.getType());
        key.append("/");

        // Map duration-year-person, duration-week-person, etc. to duration-year, duration-week, ...
        // TODO(ICU-20400): Get duration-*-person data properly with aliases.
        if (builtinUnit.getSubtype() != null && builtinUnit.getSubtype().endsWith("-person")) {
            key.append(builtinUnit.getSubtype(), 0, builtinUnit.getSubtype().length() - 7);
        } else {
            key.append(builtinUnit.getSubtype());
        }
        key.append("/gender");

        try {
            return unitsBundle.getWithFallback(key.toString()).getString();
        } catch (MissingResourceException e) {
            // TODO(icu-units#28): "$unitRes/gender" does not exist. Do we want to
            // check whether the parent "$unitRes" exists? Then we could return
            // U_MISSING_RESOURCE_ERROR for incorrect usage (e.g. builtinUnit not
            // being a builtin).
            return "";
        }
    }

    // Loads data from a resource tree with paths matching
    // $key/$pluralForm/$gender/$case, with lateral inheritance for missing cases
    // and genders.
    //
    // An InflectedPluralSink is configured to load data for a specific gender and
    // case. It loads all plural forms, because selection between plural forms is
    // dependent upon the value being formatted.
    //
    // See data/unit/de.txt and data/unit/fr.txt for examples - take a look at
    // units/compound/power2: German has case, French has differences for
    // gender, but no case.
    //
    // TODO(icu-units#138): Conceptually similar to PluralTableSink, however the
    // tree structures are different. After homogenizing the structures, we may be
    // able to unify the two classes.
    //
    // TODO: Spec violation: expects presence of "count" - does not fallback to an
    // absent "count"! If this fallback were added, getCompoundValue could be
    // superseded?
    private static final class InflectedPluralSink extends UResource.Sink {
        // NOTE: outArray MUST have a length of at least ARRAY_LENGTH. No bounds
        // checking is performed.
        public InflectedPluralSink(String gender, String caseVariant, String[] outArray) {
            this.gender = gender;
            this.caseVariant = caseVariant;
            this.outArray = outArray;
            for (int i = 0; i < ARRAY_LENGTH; i++) {
                outArray[i] = null;
            }
        }

        // See ResourceSink::put().
        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table pluralsTable = value.getTable();
            for (int i = 0; pluralsTable.getKeyAndValue(i, key, value); ++i) {
                String keyString = key.toString();
                int pluralIndex = getIndex(keyString);
                if (outArray[pluralIndex] != null) {
                    // We already have a pattern
                    continue;
                }
                UResource.Table genderTable = value.getTable();
                if (loadForPluralForm(genderTable, value)) {
                    outArray[pluralIndex] = value.getString();
                }
            }
        }

        // Tries to load data for the configured gender from `genderTable`. The
        // returned data will be for the configured gender if found, falling
        // back to "neuter" and no-gender. If none of those are found, null is
        // returned.
        private boolean loadForPluralForm(UResource.Table genderTable, UResource.Value value) {
            if (gender != null && !gender.isEmpty()) {
                if (loadForGender(genderTable, gender, value)) {
                    return true;
                }
                if (gender != "neuter") {
                    if (loadForGender(genderTable, "neuter", value)) {
                        return true;
                    }
                }
            }
            if (loadForGender(genderTable, "_", value)) {
                return true;
            }
            return false;
        }

        // Tries to load data for the given gender from `genderTable`. Returns true
        // if found, returning the data in `value`. The returned data will be for
        // the configured case if found, falling back to "nominative" and no-case if
        // not.
        private boolean
        loadForGender(UResource.Table genderTable, String genderVal, UResource.Value value) {
            if (!genderTable.findValue(genderVal, value)) {
                return false;
            }
            UResource.Table caseTable = value.getTable();
            if (caseVariant != null && !caseVariant.isEmpty()) {
                if (loadForCase(caseTable, caseVariant, value)) {
                    return true;
                }
                if (caseVariant != "nominative") {
                    if (loadForCase(caseTable, "nominative", value)) {
                        return true;
                    }
                }
            }
            if (loadForCase(caseTable, "_", value)) {
                return true;
            }
            return false;
        }

        // Tries to load data for the given case from `caseTable`. Returns null
        // if not found.
        private boolean loadForCase(UResource.Table caseTable, String caseValue, UResource.Value value) {
            if (!caseTable.findValue(caseValue, value)) {
                return false;
            }
            return true;
        }

        String gender;
        String caseVariant;
        String[] outArray;
    }

    // Fetches localised formatting patterns for the given subKey. See
    // documentation for InflectedPluralSink for details.
    //
    // Data is loaded for the appropriate unit width, with missing data filled
    // in from unitsShort.
    static void getInflectedMeasureData(String subKey,
                                        ULocale locale,
                                        UnitWidth width,
                                        String gender,
                                        String caseVariant,
                                        String[] outArray) {
        InflectedPluralSink sink = new InflectedPluralSink(gender, caseVariant, outArray);
        ICUResourceBundle unitsBundle =
            (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);

        StringBuilder key = new StringBuilder();
        key.append("units");
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append("/");
        key.append(subKey);

        try {
            unitsBundle.getAllItemsWithFallback(key.toString(), sink);
            if (width == UnitWidth.SHORT) {
                return;
            }
        } catch (MissingResourceException e) {
            // Continue: fall back to short
        }

        unitsBundle.getAllItemsWithFallback(key.toString(), sink);
    }

    private static final class PluralTableSink extends UResource.Sink {

        String[] outArray;

        // NOTE: outArray MUST have at least ARRAY_LENGTH entries. No bounds
        // checking is performed.
        public PluralTableSink(String[] outArray) {
            this.outArray = outArray;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table pluralsTable = value.getTable();
            for (int i = 0; pluralsTable.getKeyAndValue(i, key, value); ++i) {
                String keyString = key.toString();

                if (keyString.equals("case")) {
                    continue;
                }

                int index = getIndex(keyString);
                if (outArray[index] != null) {
                    continue;
                }

                String formatString = value.getString();
                outArray[index] = formatString;
            }
        }
    }

    // NOTE: outArray MUST have at least ARRAY_LENGTH entries. No bounds checking is performed.
    static void getMeasureData(
            ULocale locale,
            MeasureUnit unit,
            UnitWidth width,
            String unitDisplayCase,
            String[] outArray) {
        PluralTableSink sink = new PluralTableSink(outArray);
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME,
                locale);

        StringBuilder subKey = new StringBuilder();
        subKey.append("/");
        subKey.append(unit.getType());
        subKey.append("/");

        // Map duration-year-person, duration-week-person, etc. to duration-year, duration-week, ...
        // TODO(ICU-20400): Get duration-*-person data properly with aliases.
        if (unit.getSubtype() != null && unit.getSubtype().endsWith("-person")) {
            subKey.append(unit.getSubtype(), 0, unit.getSubtype().length() - 7);
        } else {
            subKey.append(unit.getSubtype());
        }

        if (width != UnitWidth.FULL_NAME) {
            StringBuilder genderKey = new StringBuilder();
            genderKey.append("units");
            genderKey.append(subKey);
            genderKey.append("/gender");
            try {
                outArray[GENDER_INDEX] = resource.getWithFallback(genderKey.toString()).getString();
            } catch (MissingResourceException e) {
                // continue
            }
        }

        StringBuilder key = new StringBuilder();
        key.append("units");
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append(subKey);

        // Grab desired case first, if available. Then grab nominative case to fill
        // in the gaps.
        //
        // TODO(icu-units#138): check that fallback is spec-compliant
        if (width == UnitWidth.FULL_NAME
                        && unitDisplayCase != null
                        && !unitDisplayCase.isEmpty()) {
            StringBuilder caseKey = new StringBuilder();
            caseKey.append(key);
            caseKey.append("/case/");
            caseKey.append(unitDisplayCase);

            try {
                // TODO(icu-units#138): our fallback logic is not spec-compliant:
                // lateral fallback should happen before locale fallback. Switch to
                // getInflectedMeasureData after homogenizing data format? Find a unit
                // test case that demonstrates the incorrect fallback logic (via
                // regional variant of an inflected language?)
                resource.getAllItemsWithFallback(caseKey.toString(), sink);
                // TODO(icu-units#138): our fallback logic is not spec-compliant: we
                // check the given case, then go straight to the no-case data. The spec
                // states we should first look for case="nominative". As part of #138,
                // either get the spec changed, or add unit tests that warn us if
                // case="nominative" data differs from no-case data?
            } catch (MissingResourceException e) {
                // continue.
            }
        }
        try {
            resource.getAllItemsWithFallback(key.toString(), sink);
        } catch (MissingResourceException e) {
            throw new IllegalArgumentException("No data for unit " + unit + ", width " + width, e);
        }
    }

    private static void getCurrencyLongNameData(ULocale locale, Currency currency, String[] outArray) {
        // In ICU4J, this method gets a CurrencyData from CurrencyData.provider.
        // TODO(ICU4J): Implement this without going through CurrencyData, like in ICU4C?
        Map<String, String> data = CurrencyData.provider.getInstance(locale, true).getUnitPatterns();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String pluralKeyword = e.getKey();
            int index = getIndex(pluralKeyword);
            String longName = currency.getName(locale, Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            String simpleFormat = e.getValue();
            // Example pattern from data: "{0} {1}"
            // Example output after find-and-replace: "{0} US dollars"
            simpleFormat = simpleFormat.replace("{1}", longName);
            // String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1,
            // 1);
            // SimpleModifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            outArray[index] = simpleFormat;
        }
    }

    private static String getCompoundValue(String compoundKey, ULocale locale, UnitWidth width) {
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME,
                locale);
        StringBuilder key = new StringBuilder();
        key.append("units");
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append("/compound/");
        key.append(compoundKey);
        try {
            return resource.getStringWithFallback(key.toString());
        } catch (MissingResourceException e) {
            if (width == UnitWidth.SHORT) {
                return "";
            }
        }

        try {
            return resource.getStringWithFallback(key.toString());
        } catch (MissingResourceException e) {
            return "";
        }
    }

    /**
     * Loads and applies deriveComponent rules from CLDR's
     * grammaticalFeatures.xml.
     * <p>
     * Consider a deriveComponent rule that looks like this:
     * <pre>
     *   &lt;deriveComponent feature="case" structure="per" value0="compound" value1="nominative"/&gt;
     * </pre>
     * Instantiating an instance as follows:
     * <pre>
     *   DerivedComponents d(loc, "case", "per");
     * </pre>
     * <p>
     * Applying the rule in the XML element above, <code>d.value0("foo")</code>
     * will be "foo", and <code>d.value1("foo")</code> will be "nominative".
     * <p>
     * In case of any kind of failure, value0() and value1() will simply return
     * "".
     */
    private static class DerivedComponents {
        /**
         * Constructor.
         */
        DerivedComponents(ULocale locale, String feature, String structure) {
            try {
                ICUResourceBundle derivationsBundle =
                    (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME,
                                                                         "grammaticalFeatures");
                derivationsBundle = (ICUResourceBundle)derivationsBundle.get("grammaticalData");
                derivationsBundle = (ICUResourceBundle)derivationsBundle.get("derivations");

                ICUResourceBundle stackBundle;
                try {
                    // TODO: use standard normal locale resolution algorithms rather than just grabbing
                    // language:
                    stackBundle = (ICUResourceBundle)derivationsBundle.get(locale.getLanguage());
                } catch (MissingResourceException e) {
                    stackBundle = (ICUResourceBundle)derivationsBundle.get("root");
                }

                stackBundle = (ICUResourceBundle)stackBundle.get("component");
                stackBundle = (ICUResourceBundle)stackBundle.get(feature);
                stackBundle = (ICUResourceBundle)stackBundle.get(structure);

                String value = stackBundle.getString(0);
                if (value.compareTo("compound") == 0) {
                    this.value0 = null;
                } else {
                    this.value0 = value;
                }

                value = stackBundle.getString(1);
                if (value.compareTo("compound") == 0) {
                    this.value1 = null;
                } else {
                    this.value1 = value;
                }
            } catch (MissingResourceException e) {
                // Fall back to uninflected.
            }
        }

        String value0(String compoundValue) {
            return (this.value0 != null) ? this.value0 : compoundValue;
        }

        String value1(String compoundValue) {
            return (this.value1 != null) ? this.value1 : compoundValue;
        }

        private String value0 = "", value1 = "";
    }

    // TODO(icu-units#28): test somehow? Associate with an ICU ticket for adding
    // testsuite support for testing with synthetic data?
    /**
     * Loads and returns the value in rules that look like these:
     *
     * <deriveCompound feature="gender" structure="per" value="0"/>
     * <deriveCompound feature="gender" structure="times" value="1"/>
     *
     * Currently a fake example, but spec compliant:
     * <deriveCompound feature="gender" structure="power" value="feminine"/>
     *
     * NOTE: If U_FAILURE(status), returns an empty string.
     */
    private static String getDeriveCompoundRule(ULocale locale, String feature, String structure) {
        ICUResourceBundle derivationsBundle =
                (ICUResourceBundle) UResourceBundle
                        .getBundleInstance(ICUData.ICU_BASE_NAME, "grammaticalFeatures");

        derivationsBundle = (ICUResourceBundle) derivationsBundle.get("grammaticalData");
        derivationsBundle = (ICUResourceBundle) derivationsBundle.get("derivations");

        ICUResourceBundle stackBundle;
        try {
            // TODO: use standard normal locale resolution algorithms rather than just grabbing language:
            stackBundle = (ICUResourceBundle) derivationsBundle.get(locale.getLanguage());
        } catch (MissingResourceException e) {
            stackBundle = (ICUResourceBundle) derivationsBundle.get("root");
        }

        stackBundle = (ICUResourceBundle) stackBundle.get("compound");
        stackBundle = (ICUResourceBundle) stackBundle.get(feature);

        return stackBundle.getString(structure);
    }

    // Returns the gender string for structures following these rules:
    //
    // <deriveCompound feature="gender" structure="per" value="0"/>
    // <deriveCompound feature="gender" structure="times" value="1"/>
    //
    // Fake example:
    // <deriveCompound feature="gender" structure="power" value="feminine"/>
    //
    // data0 and data1 should be pattern arrays (UnicodeString[ARRAY_SIZE]) that
    // correspond to value="0" and value="1".
    //
    // Pass a null to data1 if the structure has no concept of value="1" (e.g.
    // "prefix" doesn't).
    private static String
    getDerivedGender(ULocale locale, String structure, String[] data0, String[] data1) {
        String val = getDeriveCompoundRule(locale, "gender", structure);
        if (val.length() == 1) {
            switch (val.charAt(0)) {
            case '0':
                return data0[GENDER_INDEX];
            case '1':
                if (data1 == null) {
                    return null;
                }
                return data1[GENDER_INDEX];
            }
        }
        return val;
    }

    ////////////////////////
    /// END DATA LOADING ///
    ////////////////////////

    /**
     * Calculates the gender of an arbitrary unit: this is the *second*
     * implementation of an algorithm to do this:
     *
     * Gender is also calculated in "processPatternTimes": that code path is
     * "bottom up", loading the gender for every component of a compound unit
     * (at the same time as loading the Long Names formatting patterns), even if
     * the gender is unneeded, then combining the single units' genders into the
     * compound unit's gender, according to the rules. This algorithm does a
     * lazier "top-down" evaluation, starting with the compound unit,
     * calculating which single unit's gender is needed by breaking it down
     * according to the rules, and then loading only the gender of the one
     * single unit who's gender is needed.
     *
     * For future refactorings:
     * 1. we could drop processPatternTimes' gender calculation and just call
     *    this function: for UNUM_UNIT_WIDTH_FULL_NAME, the unit gender is in
     *    the very same table as the formatting patterns, so loading it then may
     *    be efficient. For other unit widths however, it needs to be explicitly
     *    looked up anyway.
     * 2. alternatively, if CLDR is providing all the genders we need such that
     *    we don't need to calculate them in ICU anymore, we could drop this
     *    function and keep only processPatternTimes' calculation. (And optimise
     *    it a bit?)
     *
     * @param locale The desired locale.
     * @param unit The measure unit to calculate the gender for.
     * @return The gender string for the unit, or an empty string if unknown or
     *     ungendered.
     */
    private static String calculateGenderForUnit(ULocale locale, MeasureUnit unit) {
        MeasureUnitImpl mui = unit.getCopyOfMeasureUnitImpl();
        ArrayList<SingleUnitImpl> singleUnits = mui.getSingleUnits();
        int singleUnitIndex = 0;
        if (mui.getComplexity() == MeasureUnit.Complexity.COMPOUND) {
            int startSlice = 0;
            // inclusive
            int endSlice = singleUnits.size() - 1;
            assert endSlice > 0 : "COMPOUND units have more than one single unit";
            if (singleUnits.get(endSlice).getDimensionality() < 0) {
                // We have a -per- construct
                String perRule = getDeriveCompoundRule(locale, "gender", "per");
                if (perRule.length() != 1) {
                    // Fixed gender for -per- units
                    return perRule;
                }
                if (perRule.charAt(0) == '1') {
                    // Find the start of the denominator. We already know there is one.
                    while (singleUnits.get(startSlice).getDimensionality() >= 0) {
                        startSlice++;
                    }
                } else {
                    // Find the end of the numerator
                    while (endSlice >= 0 && singleUnits.get(endSlice).getDimensionality() < 0) {
                        endSlice--;
                    }
                    if (endSlice < 0) {
                        // We have only a denominator, e.g. "per-second".
                        // TODO(icu-units#28): find out what gender to use in the
                        // absence of a first value - mentioned in CLDR-14253.
                        return "";
                    }
                }
            }
            if (endSlice > startSlice) {
                // We have a -times- construct
                String timesRule = getDeriveCompoundRule(locale, "gender", "times");
                if (timesRule.length() != 1) {
                    // Fixed gender for -times- units
                    return timesRule;
                }
                if (timesRule.charAt(0) == '0') {
                    endSlice = startSlice;
                } else {
                    // We assume timesRule[0] == u'1'
                    startSlice = endSlice;
                }
            }
            assert startSlice == endSlice;
            singleUnitIndex = startSlice;
        } else if (mui.getComplexity() == MeasureUnit.Complexity.MIXED) {
            throw new ICUException("calculateGenderForUnit does not support MIXED units");
        } else {
            assert mui.getComplexity() == MeasureUnit.Complexity.SINGLE;
            assert singleUnits.size() == 1;
        }

        // Now we know which singleUnit's gender we want
        SingleUnitImpl singleUnit = singleUnits.get(singleUnitIndex);
        // Check for any power-prefix gender override:
        if (Math.abs(singleUnit.getDimensionality()) != 1) {
            String powerRule = getDeriveCompoundRule(locale, "gender", "power");
            if (powerRule.length() != 1) {
                // Fixed gender for -powN- units
                return powerRule;
            }
            // powerRule[0] == u'0'; u'1' not currently in spec.
        }
        // Check for any SI and binary prefix gender override:
        if (Math.abs(singleUnit.getDimensionality()) != 1) {
            String prefixRule = getDeriveCompoundRule(locale, "gender", "prefix");
            if (prefixRule.length() != 1) {
                // Fixed gender for -powN- units
                return prefixRule;
            }
            // prefixRule[0] == u'0'; u'1' not currently in spec.
        }
        // Now we've boiled it down to the gender of one simple unit identifier:
        return getGenderForBuiltin(locale, MeasureUnit.forIdentifier(singleUnit.getSimpleUnitID()));
    }

    private static void maybeCalculateGender(ULocale locale, MeasureUnit unit, String[] outArray) {
        if (outArray[GENDER_INDEX] == null) {
            String meterGender = getGenderForBuiltin(locale, MeasureUnit.METER);
            if (meterGender.isEmpty()) {
                // No gender for meter: assume ungendered language
                return;
            }
            // We have a gendered language, but are lacking gender for unitRef.
            outArray[GENDER_INDEX] = calculateGenderForUnit(locale, unit);
        }
    }

    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final PluralRules rules;
    private final MicroPropsGenerator parent;
    // Grammatical gender of the formatted result.
    private String gender = "";

    private LongNameHandler(
            Map<StandardPlural, SimpleModifier> modifiers,
            PluralRules rules,
            MicroPropsGenerator parent) {
        this.modifiers = modifiers;
        this.rules = rules;
        this.parent = parent;
    }

    public static String getUnitDisplayName(ULocale locale, MeasureUnit unit, UnitWidth width) {
        String[] measureData = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, "", measureData);
        return measureData[DNAM_INDEX];
    }

    public static LongNameHandler forCurrencyLongNames(
            ULocale locale,
            Currency currency,
            PluralRules rules,
            MicroPropsGenerator parent) {
        String[] simpleFormats = new String[ARRAY_LENGTH];
        getCurrencyLongNameData(locale, currency, simpleFormats);
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(
                StandardPlural.class);
        LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
        result.simpleFormatsToModifiers(simpleFormats, NumberFormat.Field.CURRENCY);
        // TODO(icu-units#28): currency gender?
        return result;
    }

    /**
     * Construct a localized LongNameHandler for the specified MeasureUnit.
     * <p>
     * Mixed units are not supported, use MixedUnitLongNameHandler.forMeasureUnit.
     *
     * @param locale The desired locale.
     * @param unit The measure unit to construct a LongNameHandler for.
     * @param width Specifies the desired unit rendering.
     * @param unitDisplayCase Specifies the desired grammatical case. If the
     *     specified case is not found, we fall back to nominative or no-case.
     * @param rules Plural rules.
     * @param parent Plural rules.
     */
    public static LongNameHandler forMeasureUnit(
            ULocale locale,
            MeasureUnit unit,
            UnitWidth width,
            String unitDisplayCase,
            PluralRules rules,
            MicroPropsGenerator parent) {
        // From https://unicode.org/reports/tr35/tr35-general.html#compound-units -
        // Points 1 and 2 are mostly handled by MeasureUnit:
        //
        // 1. If the unitId is empty or invalid, fail
        // 2. Put the unitId into normalized order
        if (unit.getType() != null) {
            String[] simpleFormats = new String[ARRAY_LENGTH];
            getMeasureData(locale, unit, width, unitDisplayCase, simpleFormats);
            maybeCalculateGender(locale, unit, simpleFormats);
            // TODO(ICU4J): Reduce the number of object creations here?
            Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(StandardPlural.class);
            LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
            result.simpleFormatsToModifiers(simpleFormats, NumberFormat.Field.MEASURE_UNIT);
            if (simpleFormats[GENDER_INDEX] != null) {
                result.gender = simpleFormats[GENDER_INDEX];
            }
            return result;
        } else {
            assert unit.getComplexity() != Complexity.MIXED
                : "Mixed units not supported by LongNameHandler: use MixedUnitLongNameHandler";
            return forArbitraryUnit(locale, unit, width, unitDisplayCase, rules, parent);
        }
    }

    private static LongNameHandler forArbitraryUnit(ULocale loc,
                                                    MeasureUnit unit,
                                                    UnitWidth width,
                                                    String unitDisplayCase,
                                                    PluralRules rules,
                                                    MicroPropsGenerator parent) {
        // Numbered list items are from the algorithms at
        // https://unicode.org/reports/tr35/tr35-general.html#compound-units:
        //
        // 4. Divide the unitId into numerator (the part before the "-per-") and
        //    denominator (the part after the "-per-). If both are empty, fail
        MeasureUnitImpl fullUnit = unit.getCopyOfMeasureUnitImpl();
        unit = null;
        MeasureUnit perUnit = null;
        // TODO(icu-units#28): lots of inefficiency in the handling of
        // MeasureUnit/MeasureUnitImpl:
        for (SingleUnitImpl subUnit : fullUnit.getSingleUnits()) {
            if (subUnit.getDimensionality() > 0) {
                if (unit == null) {
                    unit = subUnit.build();
                } else {
                    unit = unit.product(subUnit.build());
                }
            } else {
                // It's okay to mutate fullUnit, we made a temporary copy:
                subUnit.setDimensionality(subUnit.getDimensionality() * -1);
                if (perUnit == null) {
                    perUnit = subUnit.build();
                } else {
                    perUnit = perUnit.product(subUnit.build());
                }
            }
        }
        MeasureUnitImpl unitImpl = unit == null ? null : unit.getCopyOfMeasureUnitImpl();
        MeasureUnitImpl perUnitImpl = perUnit == null ? null : perUnit.getCopyOfMeasureUnitImpl();

        // TODO(icu-units#28): check placeholder logic, see if it needs to be
        // present here instead of only in processPatternTimes:
        //
        // 5. Set both globalPlaceholder and globalPlaceholderPosition to be empty

        DerivedComponents derivedPerCases = new DerivedComponents(loc, "case", "per");

        // 6. numeratorUnitString
        String[] numeratorUnitData = new String[ARRAY_LENGTH];
        processPatternTimes(unitImpl, loc, width, derivedPerCases.value0(unitDisplayCase),
                            numeratorUnitData);

        // 7. denominatorUnitString
        String[] denominatorUnitData = new String[ARRAY_LENGTH];
        processPatternTimes(perUnitImpl, loc, width, derivedPerCases.value1(unitDisplayCase),
                            denominatorUnitData);

        // TODO(icu-units#139):
        // - implement DerivedComponents for "plural/times" and "plural/power":
        //   French has different rules, we'll be producing the wrong results
        //   currently. (Prove via tests!)
        // - implement DerivedComponents for "plural/per", "plural/prefix",
        //   "case/times", "case/power", and "case/prefix" - although they're
        //   currently hardcoded. Languages with different rules are surely on the
        //   way.
        //
        // Currently we only use "case/per", "plural/times", "case/times", and
        // "case/power".
        //
        // This may have impact on multiSimpleFormatsToModifiers(...) below too?
        // These rules are currently (ICU 69) all the same and hard-coded below.
        String perUnitPattern = null;
        if (denominatorUnitData[PER_INDEX] != null) {
            // If we have no denominator, we obtain the empty string:
            perUnitPattern = denominatorUnitData[PER_INDEX];
        } else {
            StringBuilder sb = new StringBuilder();

            // 8. Set perPattern to be getValue([per], locale, length)
            String rawPerUnitFormat = getCompoundValue("per", loc, width);
            // rawPerUnitFormat is something like "{0} per {1}"; we need to substitute in the secondary
            // unit.
            String perPatternFormatter =
                SimpleFormatterImpl.compileToStringMinMaxArguments(rawPerUnitFormat, sb, 2, 2);
            // Plural and placeholder handling for 7. denominatorUnitString:
            // TODO(icu-units#139): hardcoded:
            // <deriveComponent feature="plural" structure="per" value0="compound" value1="one"/>
            String rawDenominatorFormat = getWithPlural(denominatorUnitData, StandardPlural.ONE);
            // Some "one" pattern may not contain "{0}". For example in "ar" or "ne" locale.
            String denominatorFormatter =
                SimpleFormatterImpl.compileToStringMinMaxArguments(rawDenominatorFormat, sb, 0, 1);
            String denominatorString = PatternProps.trimSpaceChar(
                SimpleFormatterImpl.getTextWithNoArguments(denominatorFormatter));

            // 9. If the denominatorString is empty, set result to
            //    [numeratorString], otherwise set result to format(perPattern,
            //    numeratorString, denominatorString)
            //
            // TODO(icu-units#28): Why does UnicodeString need to be explicit in the
            // following line?
            perUnitPattern =
                SimpleFormatterImpl.formatCompiledPattern(perPatternFormatter, "{0}", denominatorString);
        }
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(
                StandardPlural.class);
        LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
        if (perUnitPattern.length() == 0) {
            result.simpleFormatsToModifiers(numeratorUnitData, NumberFormat.Field.MEASURE_UNIT);
        } else {
            result.multiSimpleFormatsToModifiers(numeratorUnitData, perUnitPattern,
                                                 NumberFormat.Field.MEASURE_UNIT);
        }

        // Gender
        //
        // TODO(icu-units#28): find out what gender to use in the absence of a first
        // value - e.g. what's the gender of "per-second"? Mentioned in CLDR-14253.
        //
        // gender/per deriveCompound rules don't say:
        // <deriveCompound feature="gender" structure="per" value="0"/> <!-- gender(gram-per-meter) ←
        // gender(gram) -->
        result.gender = getDerivedGender(loc, "per", numeratorUnitData, denominatorUnitData);
        return result;
    }

    /**
     * Roughly corresponds to patternTimes(...) in the spec:
     * https://unicode.org/reports/tr35/tr35-general.html#compound-units
     */
    private static void processPatternTimes(MeasureUnitImpl productUnit,
                                            ULocale loc,
                                            UnitWidth width,
                                            String caseVariant,
                                            String[] outArray) {
        assert outArray[StandardPlural.OTHER.ordinal()] == null : "outArray must have only null values!";
        assert outArray[PER_INDEX] == null : "outArray must have only null values!";

        if (productUnit == null) {
            outArray[StandardPlural.OTHER.ordinal()] = "";
            outArray[PER_INDEX] = "";
            return;
        }
        if (productUnit.getComplexity() == Complexity.MIXED) {
            // These are handled by MixedUnitLongNameHandler
            throw new UnsupportedOperationException("Mixed units not supported by LongNameHandler");
        }

        if (productUnit.getIdentifier() == null) {
            // TODO(icu-units#28): consider when serialize should be called.
            // identifier might also be empty for MeasureUnit().
            productUnit.serialize();
        }
        if (productUnit.getIdentifier().length() == 0) {
            // MeasureUnit(): no units: return empty strings.
            return;
        }

        MeasureUnit simpleUnit = MeasureUnit.findBySubType(productUnit.getIdentifier());
        if (simpleUnit != null) {
            // TODO(icu-units#145): spec doesn't cover builtin-per-builtin, it
            // breaks them all down. Do we want to drop this?
            // - findBySubType isn't super efficient, if we skip it and go to basic
            //   singles, we don't have to construct MeasureUnit's anymore.
            // - Check all the existing unit tests that fail without this: is it due
            //   to incorrect fallback via getMeasureData?
            // - Do those unit tests cover this code path representatively?
            getMeasureData(loc, simpleUnit, width, caseVariant, outArray);
            maybeCalculateGender(loc, simpleUnit, outArray);
            return;
        }

        // 2. Set timesPattern to be getValue(times, locale, length)
        String timesPattern = getCompoundValue("times", loc, width);
        StringBuilder sb = new StringBuilder();
        String timesPatternFormatter = SimpleFormatterImpl.compileToStringMinMaxArguments(timesPattern, sb, 2, 2);

        PlaceholderPosition[] globalPlaceholder = new PlaceholderPosition[ARRAY_LENGTH];
        char globalJoinerChar = 0;
        // Numbered list items are from the algorithms at
        // https://unicode.org/reports/tr35/tr35-general.html#compound-units:
        //
        // pattern(...) point 5:
        // - Set both globalPlaceholder and globalPlaceholderPosition to be empty
        //
        // 3. Set result to be empty
        for (StandardPlural plural : StandardPlural.values()) {
            int pluralIndex = plural.ordinal();
            // Initial state: empty string pattern, via all falling back to OTHER:
            if (plural == StandardPlural.OTHER) {
                outArray[pluralIndex] = "";
            } else {
                outArray[pluralIndex] = null;
            }
            globalPlaceholder[pluralIndex] = null;
        }

        // null represents "compound" (propagate the plural form).
        String pluralCategory = null;
        DerivedComponents derivedTimesPlurals = new DerivedComponents(loc, "plural", "times");
        DerivedComponents derivedTimesCases = new DerivedComponents(loc, "case", "times");
        DerivedComponents derivedPowerCases = new DerivedComponents(loc, "case", "power");

        // 4. For each single_unit in product_unit
        ArrayList<SingleUnitImpl> singleUnits = productUnit.getSingleUnits();
        for (int singleUnitIndex = 0; singleUnitIndex < singleUnits.size(); singleUnitIndex++) {
            SingleUnitImpl singleUnit = singleUnits.get(singleUnitIndex);
            String singlePluralCategory;
            String singleCaseVariant;
            // TODO(icu-units#28): ensure we have unit tests that change/fail if we
            // assign incorrect case variants here:
            if (singleUnitIndex < singleUnits.size() - 1) {
                // 4.1. If hasMultiple
                singlePluralCategory = derivedTimesPlurals.value0(pluralCategory);
                singleCaseVariant = derivedTimesCases.value0(caseVariant);
                pluralCategory = derivedTimesPlurals.value1(pluralCategory);
                caseVariant = derivedTimesCases.value1(caseVariant);
            } else {
                singlePluralCategory = derivedTimesPlurals.value1(pluralCategory);
                singleCaseVariant = derivedTimesCases.value1(caseVariant);
            }

            // 4.2. Get the gender of that single_unit
            simpleUnit = MeasureUnit.findBySubType(singleUnit.getSimpleUnitID());
            if (simpleUnit == null) {
                // Ideally all simple units should be known, but they're not:
                // 100-kilometer is internally treated as a simple unit, but it is
                // not a built-in unit and does not have formatting data in CLDR 39.
                //
                // TODO(icu-units#28): test (desirable) invariants in unit tests.
                throw new UnsupportedOperationException("Unsupported sinlgeUnit: " +
                                                        singleUnit.getSimpleUnitID());
            }
            String gender = getGenderForBuiltin(loc, simpleUnit);

            // 4.3. If singleUnit starts with a dimensionality_prefix, such as 'square-'
            assert singleUnit.getDimensionality() > 0;
            int dimensionality = singleUnit.getDimensionality();
            String[] dimensionalityPrefixPatterns = new String[ARRAY_LENGTH];
            if (dimensionality != 1) {
                // 4.3.1. set dimensionalityPrefixPattern to be
                //   getValue(that dimensionality_prefix, locale, length, singlePluralCategory,
                //   singleCaseVariant, gender), such as "{0} kwadratowym"
                StringBuilder dimensionalityKey = new StringBuilder("compound/power");
                dimensionalityKey.append(dimensionality);
                try {
                    getInflectedMeasureData(dimensionalityKey.toString(), loc, width, gender,
                                            singleCaseVariant, dimensionalityPrefixPatterns);
                } catch (MissingResourceException e) {
                    // At the time of writing, only pow2 and pow3 are supported.
                    // Attempting to format other powers results in a
                    // U_RESOURCE_TYPE_MISMATCH. We convert the error if we
                    // understand it:
                    if (dimensionality > 3) {
                        throw new UnsupportedOperationException("powerN not supported for N > 3: " +
                                                                productUnit.getIdentifier());
                    } else {
                        throw e;
                    }
                }

                // TODO(icu-units#139):
                // 4.3.2. set singlePluralCategory to be power0(singlePluralCategory)

                // 4.3.3. set singleCaseVariant to be power0(singleCaseVariant)
                singleCaseVariant = derivedPowerCases.value0(singleCaseVariant);
                // 4.3.4. remove the dimensionality_prefix from singleUnit
                singleUnit.setDimensionality(1);
            }

            // 4.4. if singleUnit starts with an si_prefix, such as 'centi'
            MeasurePrefix prefix = singleUnit.getPrefix();
            String prefixPattern = "";
            if (prefix != MeasurePrefix.ONE) {
                // 4.4.1. set siPrefixPattern to be getValue(that si_prefix, locale,
                //        length), such as "centy{0}"
                StringBuilder prefixKey = new StringBuilder();
                // prefixKey looks like "1024p3" or "10p-2":
                prefixKey.append(prefix.getBase());
                prefixKey.append('p');
                prefixKey.append(prefix.getPower());
                // Contains a pattern like "centy{0}".
                prefixPattern = getCompoundValue(prefixKey.toString(), loc, width);

                // 4.4.2. set singlePluralCategory to be prefix0(singlePluralCategory)
                //
                // TODO(icu-units#139): that refers to these rules:
                // <deriveComponent feature="plural" structure="prefix" value0="one" value1="compound"/>
                // though I'm not sure what other value they might end up having.
                //
                // 4.4.3. set singleCaseVariant to be prefix0(singleCaseVariant)
                //
                // TODO(icu-units#139): that refers to:
                // <deriveComponent feature="case" structure="prefix" value0="nominative"
                // value1="compound"/> but the prefix (value0) doesn't have case, the rest simply
                // propagates.

                // 4.4.4. remove the si_prefix from singleUnit
                singleUnit.setPrefix(MeasurePrefix.ONE);
            }

            // 4.5. Set corePattern to be the getValue(singleUnit, locale, length,
            //      singlePluralCategory, singleCaseVariant), such as "{0} metrem"
            String[] singleUnitArray = new String[ARRAY_LENGTH];
            // At this point we are left with a Simple Unit:
            assert singleUnit.build().getIdentifier().equals(singleUnit.getSimpleUnitID())
                : "Should be equal: singleUnit.build().getIdentifier() produced " +
                singleUnit.build().getIdentifier() + ", singleUnit.getSimpleUnitID() produced " +
                singleUnit.getSimpleUnitID();
            getMeasureData(loc, singleUnit.build(), width, singleCaseVariant, singleUnitArray);

            // Calculate output gender
            if (singleUnitArray[GENDER_INDEX] != null) {
                assert !singleUnitArray[GENDER_INDEX].isEmpty();

                if (prefix != MeasurePrefix.ONE) {
                    singleUnitArray[GENDER_INDEX] =
                        getDerivedGender(loc, "prefix", singleUnitArray, null);
                }

                if (dimensionality != 1) {
                    singleUnitArray[GENDER_INDEX] =
                        getDerivedGender(loc, "power", singleUnitArray, null);
                }

                String timesGenderRule = getDeriveCompoundRule(loc, "gender", "times");
                if (timesGenderRule.length() == 1) {
                    switch (timesGenderRule.charAt(0)) {
                    case '0':
                        if (singleUnitIndex == 0) {
                            assert outArray[GENDER_INDEX] == null;
                            outArray[GENDER_INDEX] = singleUnitArray[GENDER_INDEX];
                        }
                        break;
                    case '1':
                        if (singleUnitIndex == singleUnits.size() - 1) {
                            assert outArray[GENDER_INDEX] == null;
                            outArray[GENDER_INDEX] = singleUnitArray[GENDER_INDEX];
                        }
                    }
                } else {
                    if (outArray[GENDER_INDEX] == null) {
                        outArray[GENDER_INDEX] = timesGenderRule;
                    }
                }
            }

            // Calculate resulting patterns for each plural form
            for (StandardPlural plural_ : StandardPlural.values()) {
                StandardPlural plural = plural_;
                int pluralIndex = plural.ordinal();

                // singleUnitArray[pluralIndex] looks something like "{0} Meter"
                if (outArray[pluralIndex] == null) {
                    if (singleUnitArray[pluralIndex] == null) {
                        // Let the usual plural fallback mechanism take care of this
                        // plural form
                        continue;
                    } else {
                        // Since our singleUnit can have a plural form that outArray
                        // doesn't yet have (relying on fallback to OTHER), we start
                        // by grabbing it with the normal plural fallback mechanism
                        outArray[pluralIndex] = getWithPlural(outArray, plural);
                    }
                }

                if (singlePluralCategory != null) {
                    plural = StandardPlural.fromString(singlePluralCategory);
                }

                // 4.6. Extract(corePattern, coreUnit, placeholder, placeholderPosition) from that
                // pattern.
                ExtractCorePatternResult r = extractCorePattern(getWithPlural(singleUnitArray, plural));

                // 4.7 If the position is middle, then fail
                if (r.placeholderPosition == PlaceholderPosition.MIDDLE) {
                    throw new UnsupportedOperationException();
                }

                // 4.8. If globalPlaceholder is empty
                if (globalPlaceholder[pluralIndex] == null) {
                    globalPlaceholder[pluralIndex] = r.placeholderPosition;
                    globalJoinerChar = r.joinerChar;
                } else {
                    // Expect all units involved to have the same placeholder position
                    assert globalPlaceholder[pluralIndex] == r.placeholderPosition;
                    // TODO(icu-units#28): Do we want to add a unit test that checks
                    // for consistent joiner chars? Probably not, given how
                    // inconsistent they are. File a CLDR ticket with examples?
                }
                // Now coreUnit would be just "Meter"

                // 4.9. If siPrefixPattern is not empty
                if (prefix != MeasurePrefix.ONE) {
                    String prefixCompiled =
                        SimpleFormatterImpl.compileToStringMinMaxArguments(prefixPattern, sb, 1, 1);

                    // 4.9.1. Set coreUnit to be the combineLowercasing(locale, length, siPrefixPattern,
                    //        coreUnit)
                    // combineLowercasing(locale, length, prefixPattern, coreUnit)
                    //
                    // TODO(icu-units#28): run this only if prefixPattern does not
                    // contain space characters - do languages "as", "bn", "hi",
                    // "kk", etc have concepts of upper and lower case?:
                    if (width == UnitWidth.FULL_NAME) {
                        r.coreUnit = UCharacter.toLowerCase(loc, r.coreUnit);
                    }
                    r.coreUnit = SimpleFormatterImpl.formatCompiledPattern(prefixCompiled, r.coreUnit);
                }

                // 4.10. If dimensionalityPrefixPattern is not empty
                if (dimensionality != 1) {
                    String dimensionalityCompiled = SimpleFormatterImpl.compileToStringMinMaxArguments(
                        getWithPlural(dimensionalityPrefixPatterns, plural), sb, 1, 1);

                    // 4.10.1. Set coreUnit to be the combineLowercasing(locale, length,
                    //         dimensionalityPrefixPattern, coreUnit)
                    // combineLowercasing(locale, length, prefixPattern, coreUnit)
                    //
                    // TODO(icu-units#28): run this only if prefixPattern does not
                    // contain space characters - do languages "as", "bn", "hi",
                    // "kk", etc have concepts of upper and lower case?:
                    if (width == UnitWidth.FULL_NAME) {
                        r.coreUnit = UCharacter.toLowerCase(loc, r.coreUnit);
                    }
                    r.coreUnit =
                        SimpleFormatterImpl.formatCompiledPattern(dimensionalityCompiled, r.coreUnit);
                }

                if (outArray[pluralIndex].length() == 0) {
                    // 4.11. If the result is empty, set result to be coreUnit
                    outArray[pluralIndex] = r.coreUnit;
                } else {
                    // 4.12. Otherwise set result to be format(timesPattern, result, coreUnit)
                    outArray[pluralIndex] = SimpleFormatterImpl.formatCompiledPattern(
                        timesPatternFormatter, outArray[pluralIndex], r.coreUnit);
                }
            }
        }
        for (StandardPlural plural : StandardPlural.values()) {
            int pluralIndex = plural.ordinal();
            if (globalPlaceholder[pluralIndex] == PlaceholderPosition.BEGINNING) {
                StringBuilder tmp = new StringBuilder();
                tmp.append("{0}");
                if (globalJoinerChar != 0) {
                    tmp.append(globalJoinerChar);
                }
                tmp.append(outArray[pluralIndex]);
                outArray[pluralIndex] = tmp.toString();
            } else if (globalPlaceholder[pluralIndex] == PlaceholderPosition.END) {
                if (globalJoinerChar != 0) {
                    outArray[pluralIndex] = outArray[pluralIndex] + globalJoinerChar;
                }
                outArray[pluralIndex] = outArray[pluralIndex] + "{0}";
            }
        }
    }

    /** Sets modifiers to use the patterns from simpleFormats. */
    private void simpleFormatsToModifiers(
            String[] simpleFormats,
            NumberFormat.Field field) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = getWithPlural(simpleFormats, plural);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 0, 1);
            Modifier.Parameters parameters = new Modifier.Parameters();
            parameters.obj = this;
            parameters.signum = null;// Signum ignored
            parameters.plural = plural;
            modifiers.put(plural, new SimpleModifier(compiled, field, false, parameters));
        }
    }

    /**
     * Sets modifiers to a combination of `leadFormats` (one per plural form)
     * and `trailFormat` appended to each.
     *
     * With a leadFormat of "{0}m" and a trailFormat of "{0}/s", it produces a
     * pattern of "{0}m/s" by inserting each leadFormat pattern into
     * trailFormat.
     */
    private void multiSimpleFormatsToModifiers(
            String[] leadFormats,
            String trailFormat,
            NumberFormat.Field field) {
        StringBuilder sb = new StringBuilder();
        String trailCompiled = SimpleFormatterImpl.compileToStringMinMaxArguments(trailFormat, sb, 1, 1);
        for (StandardPlural plural : StandardPlural.VALUES) {
            String leadFormat = getWithPlural(leadFormats, plural);
            String compoundFormat;
            if (leadFormat.length() == 0) {
                compoundFormat = trailFormat;
            } else {
                compoundFormat = SimpleFormatterImpl.formatCompiledPattern(trailCompiled, leadFormat);
            }
            String compoundCompiled =
                SimpleFormatterImpl.compileToStringMinMaxArguments(compoundFormat, sb, 0, 1);
            Modifier.Parameters parameters = new Modifier.Parameters();
            parameters.obj = this;
            parameters.signum = null; // Signum ignored
            parameters.plural = plural;
            modifiers.put(plural, new SimpleModifier(compoundCompiled, field, false, parameters));
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
        micros.modOuter = modifiers.get(pluralForm);
        micros.gender = this.gender;
        return micros;
    }

    /**
     * Produces a plural-appropriate Modifier for a unit: `quantity` is taken as
     * the final smallest unit, while the larger unit values must be provided
     * via `micros.mixedMeasures`.
     *
     * Does not call parent.processQuantity, so cannot get a MicroProps instance
     * that way. Instead, the instance is passed in as a parameter.
     */
    @Override
    public MicroProps processQuantityWithMicros(DecimalQuantity quantity, MicroProps micros) {
        StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
        micros.modOuter = modifiers.get(pluralForm);
        return micros;
    }

    @Override
    public Modifier getModifier(Signum signum, StandardPlural plural) {
        // Signum ignored
        return modifiers.get(plural);
    }
}
