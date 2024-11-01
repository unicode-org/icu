// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.VersionInfo;

/**
 * This is not a real test class. It is only used to
 * generate updated unit tests code based on new CLDR data.
 * Do not add any other tests here.
 *
 * See https://unicode-org.github.io/icu/processes/release/tasks/updating-measure-unit.html
 * for information on how to update with each new release.
 * @author markdavis
 */
@RunWith(JUnit4.class)
public class MeasureUnitGeneratorTest extends CoreTestFmwk {

    private static class OrderedPair<F extends Comparable<F>, S extends Comparable<S>> extends Pair<F, S> implements Comparable<OrderedPair<F, S>> {

        private OrderedPair(F first, S second) {
            super(first, second);
        }

        private static <F extends Comparable<F>, S extends Comparable<S>> OrderedPair<F, S> of(F first, S second) {
            if (first == null || second == null) {
                throw new IllegalArgumentException("OrderedPair.of requires non null values.");
            }
            return new OrderedPair<>(first, second);
        }

        @Override
        public int compareTo(OrderedPair<F, S> other) {
            int result = first.compareTo(other.first);
            if (result != 0) {
                return result;
            }
            return second.compareTo(other.second);
        }
    }

    private static final HashSet<String> DRAFT_VERSION_SET = new HashSet<>();

    private static final HashSet<String> TIME_CODES = new HashSet<>();

    private static final String[][] JAVA_VERSIONS = {
        {"G_FORCE", "53"},
        {"DEGREE", "53"},
        {"ARC_MINUTE", "53"},
        {"ARC_SECOND", "53"},
        {"ACRE", "53"},
        {"HECTARE", "53"},
        {"SQUARE_FOOT", "53"},
        {"SQUARE_KILOMETER", "53"},
        {"SQUARE_METER", "53"},
        {"SQUARE_MILE", "53"},
        {"MILLISECOND", "53"},
        {"CENTIMETER", "53"},
        {"FOOT", "53"},
        {"INCH", "53"},
        {"KILOMETER", "53"},
        {"LIGHT_YEAR", "53"},
        {"METER", "53"},
        {"MILE", "53"},
        {"MILLIMETER", "53"},
        {"PICOMETER", "53"},
        {"YARD", "53"},
        {"GRAM", "53"},
        {"KILOGRAM", "53"},
        {"OUNCE", "53"},
        {"POUND", "53"},
        {"HORSEPOWER", "53"},
        {"KILOWATT", "53"},
        {"WATT", "53"},
        {"HECTOPASCAL", "53"},
        {"INCH_HG", "53"},
        {"MILLIBAR", "53"},
        {"KILOMETER_PER_HOUR", "53"},
        {"METER_PER_SECOND", "53"},
        {"MILE_PER_HOUR", "53"},
        {"CELSIUS", "53"},
        {"FAHRENHEIT", "53"},
        {"CUBIC_KILOMETER", "53"},
        {"CUBIC_MILE", "53"},
        {"LITER", "53"},
        {"YEAR", "53"},
        {"MONTH", "53"},
        {"WEEK", "53"},
        {"DAY", "53"},
        {"HOUR", "53"},
        {"MINUTE", "53"},
        {"SECOND", "53"},
        {"METER_PER_SECOND_SQUARED", "54"},
        {"RADIAN", "54"},
        {"SQUARE_CENTIMETER", "54"},
        {"SQUARE_INCH", "54"},
        {"SQUARE_YARD", "54"},
        {"LITER_PER_KILOMETER", "54"},
        {"MILE_PER_GALLON", "54"},
        {"BIT", "54"},
        {"BYTE", "54"},
        {"GIGABIT", "54"},
        {"GIGABYTE", "54"},
        {"KILOBIT", "54"},
        {"KILOBYTE", "54"},
        {"MEGABIT", "54"},
        {"MEGABYTE", "54"},
        {"TERABIT", "54"},
        {"TERABYTE", "54"},
        {"MICROSECOND", "54"},
        {"NANOSECOND", "54"},
        {"AMPERE", "54"},
        {"MILLIAMPERE", "54"},
        {"OHM", "54"},
        {"VOLT", "54"},
        {"CALORIE", "54"},
        {"FOODCALORIE", "54"},
        {"JOULE", "54"},
        {"KILOCALORIE", "54"},
        {"KILOJOULE", "54"},
        {"KILOWATT_HOUR", "54"},
        {"GIGAHERTZ", "54"},
        {"HERTZ", "54"},
        {"KILOHERTZ", "54"},
        {"MEGAHERTZ", "54"},
        {"ASTRONOMICAL_UNIT", "54"},
        {"DECIMETER", "54"},
        {"FATHOM", "54"},
        {"FURLONG", "54"},
        {"MICROMETER", "54"},
        {"NANOMETER", "54"},
        {"NAUTICAL_MILE", "54"},
        {"PARSEC", "54"},
        {"LUX", "54"},
        {"CARAT", "54"},
        {"METRIC_TON", "54"},
        {"MICROGRAM", "54"},
        {"MILLIGRAM", "54"},
        {"OUNCE_TROY", "54"},
        {"STONE", "54"},
        {"TON", "54"},
        {"GIGAWATT", "54"},
        {"MEGAWATT", "54"},
        {"MILLIWATT", "54"},
        {"MILLIMETER_OF_MERCURY", "54"},
        {"POUND_PER_SQUARE_INCH", "54"},
        {"KARAT", "54"},
        {"KELVIN", "54"},
        {"ACRE_FOOT", "54"},
        {"BUSHEL", "54"},
        {"CENTILITER", "54"},
        {"CUBIC_CENTIMETER", "54"},
        {"CUBIC_FOOT", "54"},
        {"CUBIC_INCH", "54"},
        {"CUBIC_METER", "54"},
        {"CUBIC_YARD", "54"},
        {"CUP", "54"},
        {"DECILITER", "54"},
        {"FLUID_OUNCE", "54"},
        {"GALLON", "54"},
        {"HECTOLITER", "54"},
        {"MEGALITER", "54"},
        {"MILLILITER", "54"},
        {"PINT", "54"},
        {"QUART", "54"},
        {"TABLESPOON", "54"},
        {"TEASPOON", "54"},
        {"GENERIC_TEMPERATURE", "56"},
        {"REVOLUTION_ANGLE", "56"},
        {"LITER_PER_100KILOMETERS", "56"},
        {"CENTURY", "56"},
        {"MILE_SCANDINAVIAN", "56"},
        {"KNOT", "56"},
        {"CUP_METRIC", "56"},
        {"PINT_METRIC", "56"},
        {"MILLIGRAM_PER_DECILITER", "57"},
        {"MILLIMOLE_PER_LITER", "57"},
        {"PART_PER_MILLION", "57"},
        {"MILE_PER_GALLON_IMPERIAL", "57"},
        {"GALLON_IMPERIAL", "57"},
        {"POINT", "59"},
        {"PERCENT", "63"},
        {"PERMILLE", "63"},
        {"PETABYTE", "63"},
        {"ATMOSPHERE", "63"},
        {"DUNAM", "64"},
        {"MOLE", "64"},
        {"PERMYRIAD", "64"},
        {"DAY_PERSON", "64"},
        {"MONTH_PERSON", "64"},
        {"WEEK_PERSON", "64"},
        {"YEAR_PERSON", "64"},
        {"BRITISH_THERMAL_UNIT", "64"},
        {"ELECTRONVOLT", "64"},
        {"NEWTON", "64"},
        {"POUND_FORCE", "64"},
        {"SOLAR_RADIUS", "64"},
        {"SOLAR_LUMINOSITY", "64"},
        {"DALTON", "64"},
        {"EARTH_MASS", "64"},
        {"SOLAR_MASS", "64"},
        {"KILOPASCAL", "64"},
        {"MEGAPASCAL", "64"},
        {"NEWTON_METER", "64"},
        {"POUND_FOOT", "64"},
        {"BARREL", "64"},
        {"FLUID_OUNCE_IMPERIAL", "64"},
        {"DECADE", "65"},
        {"THERM_US", "65"},
        {"DOT_PER_CENTIMETER", "65"},
        {"DOT_PER_INCH", "65"},
        {"EM", "65"},
        {"MEGAPIXEL", "65"},
        {"PIXEL", "65"},
        {"PIXEL_PER_CENTIMETER", "65"},
        {"PIXEL_PER_INCH", "65"},
        {"BAR", "65"},
        {"PASCAL", "65"},
        {"DOT", "68"},
        {"EARTH_RADIUS", "68"},
        {"CANDELA", "68"},
        {"LUMEN", "68"},
        {"GRAIN", "68"},
        {"DESSERT_SPOON", "68"},
        {"DESSERT_SPOON_IMPERIAL", "68"},
        {"DRAM", "68"},
        {"DROP", "68"},
        {"JIGGER", "68"},
        {"PINCH", "68"},
        {"QUART_IMPERIAL", "68"},
        {"MILLIGRAM_OFGLUCOSE_PER_DECILITER", "69"},
        {"ITEM", "70"},
        {"KILOWATT_HOUR_PER_100_KILOMETER", "70"},
        {"QUARTER", "72"},
        {"TONNE", "72"},
        {"BEAUFORT", "73"},
        {"GASOLINE_ENERGY_DENSITY", "74"},
    };

    private static final HashMap<String, String> JAVA_VERSION_MAP = new HashMap<>();

    // modify certain CLDR unit names before generating functions
    // that create/get the corresponding MeasureUnit objects
    private static final Map<String,String> CLDR_NAME_REMAP = new HashMap<>();

    static {
        TIME_CODES.add("year");
        TIME_CODES.add("month");
        TIME_CODES.add("week");
        TIME_CODES.add("day");
        TIME_CODES.add("hour");
        TIME_CODES.add("minute");
        TIME_CODES.add("second");
        for (String[] funcNameAndVersion : JAVA_VERSIONS) {
            JAVA_VERSION_MAP.put(funcNameAndVersion[0], funcNameAndVersion[1]);
        }

        // CLDR_NAME_REMAP entries
        // The first two fix overly-generic CLDR unit names
        CLDR_NAME_REMAP.put("revolution", "revolution-angle");
        CLDR_NAME_REMAP.put("generic",    "generic-temperature");
        // The next seven map updated CLDR 37 names back to their
        // old form in order to preserve the old function names
        CLDR_NAME_REMAP.put("meter-per-square-second",     "meter-per-second-squared");
        CLDR_NAME_REMAP.put("permillion",                  "part-per-million");
        CLDR_NAME_REMAP.put("liter-per-100-kilometer",     "liter-per-100kilometers");
        CLDR_NAME_REMAP.put("inch-ofhg",                   "inch-hg");
        CLDR_NAME_REMAP.put("millimeter-ofhg",             "millimeter-of-mercury");
        CLDR_NAME_REMAP.put("pound-force-per-square-inch", "pound-per-square-inch");
        CLDR_NAME_REMAP.put("pound-force-foot",            "pound-foot");
    }

    private static final String ICU_ROOT = findIcuRoot();

    private static String findIcuRoot() {
        URL x = MeasureUnitGeneratorTest.class.getResource(".");
        String classFile = x.getFile();
        int idx = classFile.indexOf("/icu4j/main/common_tests/target/");
        if (idx != -1) {
            return classFile.substring(0, idx);
        } else {
            return "${icuroot}";
        }
    }

    @Test
    public void generateUnitTestsUpdate() throws IOException {
        // various generateXXX calls go here, see
        // docs/processes/release/tasks/updating-measure-unit.md
        // use this test to run each of the following in succession
        if (System.getProperty("generateMeasureUnitUpdate") != null) {
            final String icuVersion = Integer.toString(VersionInfo.ICU_VERSION.getMajor());
            System.out.println();
            System.out.println("WARNING: open the pairs of files listed below and copy code fragments, not full files!");
            System.out.println("Some kind of diff tool / editor would work best.");

            generateConstants(icuVersion); // update generated MeasureUnit constants
            generateBackwardCompatibilityTest(icuVersion); // create TestCompatible<icu_ver>
            generateCXXHConstants(icuVersion); // update generated createXXX methods
            generateCXXConstants(); // update generated code
            generateCXXBackwardCompatibilityTest(icuVersion); // create TestCompatible<icu_ver>
            updateJAVAVersions(icuVersion); // JAVA_VERSIONS
        }
    }

    private static Map<MeasureUnit, Pair<MeasureUnit, MeasureUnit>> getUnitsToPerParts() {
        TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
        Map<MeasureUnit, Pair<String, String>> unitsToPerStrings =
                new HashMap<>();
        Map<String, MeasureUnit> namesToUnits = new HashMap<>();
        for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
            String type = entry.getKey();
            // Currency types are always atomic units, so we can skip these
            if (type.equals("currency")) {
                continue;
            }
            for (MeasureUnit unit : entry.getValue()) {
                String javaName = toJAVAName(unit);
                String[] nameParts = javaName.split("_PER_");
                if (nameParts.length == 1) {
                    namesToUnits.put(nameParts[0], unit);
                } else if (nameParts.length == 2) {
                    unitsToPerStrings.put(unit, Pair.of(nameParts[0], nameParts[1]));
                }
            }
        }
        Map<MeasureUnit, Pair<MeasureUnit, MeasureUnit>> unitsToPerUnits =
                new HashMap<>();
        for (Map.Entry<MeasureUnit, Pair<String, String>> entry : unitsToPerStrings.entrySet()) {
            Pair<String, String> perStrings = entry.getValue();
            MeasureUnit unit = namesToUnits.get(perStrings.first);
            MeasureUnit perUnit = namesToUnits.get(perStrings.second);
            if (unit != null && perUnit != null) {
                unitsToPerUnits.put(entry.getKey(), Pair.of(unit, perUnit));
            }
        }
        return unitsToPerUnits;
    }

    private static void generateCXXHConstants(String thisVersion) throws IOException {
        String fullOutputPath = "${icuroot}/icu4c/source/i18n/unicode/measunit.h";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            Map<String, MeasureUnit> seen = new HashMap<>();
            out.println("// Start generated createXXX methods");
            out.println();
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                String type = entry.getKey();
                if (type.equals("currency")) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String code = unit.getSubtype();
                    String name = toCamelCase(unit);
                    String javaName = toJAVAName(unit);
                    checkForDup(seen, name, unit);
                    if (isDraft(javaName)) {
                        out.println("#ifndef U_HIDE_DRAFT_API");
                    }
                    out.println("    /**");
                    out.println("     * Returns by pointer, unit of " + type + ": " + code + ".");
                    out.println("     * Caller owns returned value and must free it.");
                    out.printf("     * Also see {@link #get%s()}.\n", name);
                    out.println("     * @param status ICU error code.");
                    if (isDraft(javaName)) {
                        out.println("     * @draft ICU " + getVersion(javaName, thisVersion));
                    } else {
                        out.println("     * @stable ICU " + getVersion(javaName, thisVersion));
                    }
                    out.println("     */");
                    out.printf("    static MeasureUnit *create%s(UErrorCode &status);\n", name);
                    out.println();
                    out.println("    /**");
                    out.println("     * Returns by value, unit of " + type + ": " + code + ".");
                    out.printf("     * Also see {@link #create%s()}.\n", name);
                    String getterVersion = getVersion(javaName, thisVersion);
                    if (Integer.parseInt(getterVersion) < 64) {
                        getterVersion = "64";
                    }
                    if (isDraft(javaName)) {
                        out.println("     * @draft ICU " + getterVersion);
                    } else {
                        out.println("     * @stable ICU " + getterVersion);
                    }
                    out.println("     */");
                    out.printf("    static MeasureUnit get%s();\n", name);
                    if (isDraft(javaName)) {
                        out.println("#endif /* U_HIDE_DRAFT_API */");
                    }
                    out.println("");
                    // Hack: METRIC-TON unit changed its name from "metric-ton" to "tonne"
                    // In order to preserve the existing APIs for "metric-ton" we need to
                    // add those APIs manually
                    if (name.equals("Tonne")) {
                        addCXXHForMetricTon(out);
                    }
                }
            }
            out.println("// End generated createXXX methods");
        }
    }

    // Add the headers for "metric-ton"
    // The tool won't create them any more
    private static void addCXXHForMetricTon(PrintStream out) {
        out.println("    /**");
        out.println("     * Returns by pointer, unit of mass: metric-ton");
        out.println("     * (renamed to tonne in CLDR 42 / ICU 72).");
        out.println("     * Caller owns returned value and must free it.");
        out.println("     * Note: In ICU 74 this will be deprecated in favor of");
        out.println("     * createTonne(), which is currently draft but will");
        out.println("     * become stable in ICU 74, and which uses the preferred naming.");
        out.println("     * Also see {@link #getMetricTon()} and {@link #createTonne()}.");
        out.println("     * @param status ICU error code.");
        out.println("     * @stable ICU 54");
        out.println("     */");
        out.println("    static MeasureUnit *createMetricTon(UErrorCode &status);");
        out.println("");
        out.println("    /**");
        out.println("     * Returns by value, unit of mass: metric-ton");
        out.println("     * (renamed to tonne in CLDR 42 / ICU 72).");
        out.println("     * Note: In ICU 74 this will be deprecated in favor of");
        out.println("     * getTonne(), which is currently draft but will");
        out.println("     * become stable in ICU 74, and which uses the preferred naming.");
        out.println("     * Also see {@link #createMetricTon()} and {@link #getTonne()}.");
        out.println("     * @stable ICU 64");
        out.println("     */");
        out.println("    static MeasureUnit getMetricTon();");
        out.println("");
    }

    private static void checkForDup(
            Map<String, MeasureUnit> seen, String name, MeasureUnit unit) {
        if (seen.containsKey(name)) {
            throw new RuntimeException("\nCollision!!" + unit + ", " + seen.get(name));
        } else {
            seen.put(name, unit);
        }
    }

    private static void updateJAVAVersions(String thisVersion) throws IOException {
        String fullOutputPath = "${icuroot}/icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitGeneratorTest.java";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            out.println();
            Map<String, MeasureUnit> seen = new HashMap<>();
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                String type = entry.getKey();
                if (type.equals("currency")) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String javaName = toJAVAName(unit);
                    checkForDup(seen, javaName, unit);
                    if (!JAVA_VERSION_MAP.containsKey(javaName)) {
                        out.printf("        {\"%s\", \"%s\"},\n", javaName, thisVersion);
                    }
                }
            }
        }
    }


    private static TreeMap<String, List<MeasureUnit>> getAllUnits() {
        final Comparator<MeasureUnit> measureUnitComparator =
                (MeasureUnit o1, MeasureUnit o2) -> o1.getSubtype().compareTo(o2.getSubtype());
        TreeMap<String, List<MeasureUnit>> allUnits = new TreeMap<>();
        for (String type : MeasureUnit.getAvailableTypes()) {
            ArrayList<MeasureUnit> units = new ArrayList<>(MeasureUnit.getAvailable(type));
            Collections.sort(units, measureUnitComparator);
            allUnits.put(type, units);
        }
        return allUnits;
    }

    private static void generateCXXConstants() throws IOException {
        String fullOutputPath = "${icuroot}/icu4c/source/i18n/measunit.cpp";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            out.println("// Start generated code for measunit.cpp");
            out.println("");
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();

            // Hack: for C++, add base unit here, but ignore them when printing the create methods.
            // Also keep track of the base unit offset to make the C++ default constructor faster.
            allUnits.put("none", Arrays.asList(new MeasureUnit[] {NoUnit.BASE}));
            int baseTypeIdx = -1;
            int baseSubTypeIdx = -1;

            out.println("// Maps from Type ID to offset in gSubTypes.");
            out.println("static const int32_t gOffsets[] = {");
            int index = 0;
            int typeCount = 0;
            int currencyIndex = -1;
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                out.printf("    %d,\n", index);
                if (entry.getKey() == "currency") {
                    currencyIndex = typeCount;
                }
                typeCount++;
                index += entry.getValue().size();
            }
            assertTrue("currency present", currencyIndex >= 0);
            out.printf("    %d\n", index);
            out.println("};");
            out.println();
            out.println("static const int32_t kCurrencyOffset = " + currencyIndex + ";");
            out.println();
            out.println("// Must be sorted alphabetically.");
            out.println("static const char * const gTypes[] = {");
            boolean first = true;
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                if (!first) {
                    out.println(",");
                }
                out.print("    \"" + entry.getKey() + "\"");
                first = false;
            }
            out.println();
            out.println("};");
            out.println();
            out.println("// Must be grouped by type and sorted alphabetically within each type.");
            out.println("static const char * const gSubTypes[] = {");
            first = true;
            int offset = 0;
            int typeIdx = 0;
            Map<MeasureUnit, Integer> measureUnitToOffset = new HashMap<>();
            Map<MeasureUnit, Pair<Integer, Integer>> measureUnitToTypeSubType =
                    new HashMap<>();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                int subTypeIdx = 0;
                for (MeasureUnit unit : entry.getValue()) {
                    if (!first) {
                        out.println(",");
                    }
                    if (unit != null) {
                        out.print("    \"" + unit.getSubtype() + "\"");
                    } else {
                        assertEquals("unit only null for \"none\" type", "none", entry.getKey());
                        out.print("    \"\"");
                    }
                    first = false;
                    measureUnitToOffset.put(unit, offset);
                    measureUnitToTypeSubType.put(unit, Pair.of(typeIdx, subTypeIdx));
                    if (unit == NoUnit.BASE) {
                        baseTypeIdx = typeIdx;
                        baseSubTypeIdx = subTypeIdx;
                    }
                    offset++;
                    subTypeIdx++;
                }
                typeIdx++;
            }
            out.println();
            out.println("};");
            out.println();

            // Build unit per unit offsets to corresponding type sub types sorted by
            // unit first and then per unit.
            TreeMap<OrderedPair<Integer, Integer>, Pair<Integer, Integer>> unitPerUnitOffsetsToTypeSubType
                    = new TreeMap<>();
            for (Map.Entry<MeasureUnit, Pair<MeasureUnit, MeasureUnit>> entry
                    : getUnitsToPerParts().entrySet()) {
                Pair<MeasureUnit, MeasureUnit> unitPerUnit = entry.getValue();
                unitPerUnitOffsetsToTypeSubType.put(
                        OrderedPair.of(
                                measureUnitToOffset.get(unitPerUnit.first),
                                measureUnitToOffset.get(unitPerUnit.second)),
                        measureUnitToTypeSubType.get(entry.getKey()));
            }

            // Print out the fast-path for the default constructor
            out.println("// Shortcuts to the base unit in order to make the default constructor fast");
            out.println("static const int32_t kBaseTypeIdx = " + baseTypeIdx + ";");
            out.println("static const int32_t kBaseSubTypeIdx = " + baseSubTypeIdx + ";");
            out.println();

            Map<String, MeasureUnit> seen = new HashMap<>();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {

                String type = entry.getKey();
                if (type.equals("currency") || type.equals("none")) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String name = toCamelCase(unit);
                    Pair<Integer, Integer> typeSubType = measureUnitToTypeSubType.get(unit);
                    if (typeSubType == null) {
                        throw new IllegalStateException();
                    }
                    checkForDup(seen, name, unit);
                    out.printf("MeasureUnit *MeasureUnit::create%s(UErrorCode &status) {\n", name);
                    out.printf("    return MeasureUnit::create(%d, %d, status);\n",
                            typeSubType.first, typeSubType.second);
                    out.println("}");
                    out.println();
                    out.printf("MeasureUnit MeasureUnit::get%s() {\n", name);
                    out.printf("    return MeasureUnit(%d, %d);\n",
                            typeSubType.first, typeSubType.second);
                    out.println("}");
                    out.println();
                    // Hack: METRIC-TON unit changed its name from "metric-ton" to "tonne"
                    // In order to preserve the existing APIs for "metric-ton" we need to
                    // add those APIs manually
                    if (name.equals("Tonne")) {
                        addCXXForMetricTon(typeSubType, out);
                    }
                }
            }
            out.println("// End generated code for measunit.cpp");
        }
    }

    // Add the API skeletons for "metric-ton"
    // The tool won't create them any more
    private static void addCXXForMetricTon(Pair<Integer, Integer> typeSubType, PrintStream out) {
        String name = "MetricTon";
        out.printf("MeasureUnit *MeasureUnit::create%s(UErrorCode &status) {\n", name);
        out.printf("    return MeasureUnit::create(%d, %d, status);\n",
                        typeSubType.first, typeSubType.second);
        out.println("}");
        out.println();
        out.printf("MeasureUnit MeasureUnit::get%s() {\n", name);
        out.printf("    return MeasureUnit(%d, %d);\n",
                        typeSubType.first, typeSubType.second);
        out.println("}");
        out.println();
    }

    private static String toCamelCase(MeasureUnit unit) {
        StringBuilder result = new StringBuilder();
        boolean caps = true;
        String code = unit.getSubtype();

        String replacement = CLDR_NAME_REMAP.get(code);
        if (replacement != null) {
            code = replacement;
        }

        int len = code.length();
        for (int i = 0; i < len; i++) {
            char ch = code.charAt(i);
            if (ch == '-') {
                caps = true;
            } else if (Character.isDigit(ch)) {
                caps = true;
                result.append(ch);
            } else if (caps) {
                result.append(Character.toUpperCase(ch));
                caps = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static boolean isTypeHidden(String type) {
        return "currency".equals(type);
    }

    private static void generateBackwardCompatibilityTest(String version) throws IOException {
        String fullOutputPath = "${icuroot}/icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitCompatibilityTest.java";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            Map<String, MeasureUnit> seen = new HashMap<>();
            out.println();
            out.printf("    @Test\n");
            out.printf("    public void TestCompatible%s() {\n", version.replace(".", "_"));
            out.println("        MeasureUnit[] units = {");
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
            int count = 0;
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                if (isTypeHidden(entry.getKey())) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String javaName = toJAVAName(unit);
                    checkForDup(seen, javaName, unit);
                    out.printf("                MeasureUnit.%s,\n", javaName);
                    count++;
                }
            }
            out.println("        };");
            out.printf("        assertEquals(\"\",  %d, units.length);\n", count);
            out.println("    }");
        }
    }

    private static void generateCXXBackwardCompatibilityTest(String version) throws IOException {
        String fullOutputPath = "${icuroot}/icu4c/source/test/intltest/measfmttest.cpp";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            out.println();
            Map<String, MeasureUnit> seen = new HashMap<>();
            out.printf("void MeasureFormatTest::TestCompatible%s() {\n", version.replace(".", "_"));
            out.println("    UErrorCode status = U_ZERO_ERROR;");
            out.println("    LocalPointer<MeasureUnit> measureUnit;");
            out.println("    MeasureUnit measureUnitValue;");
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                if (isTypeHidden(entry.getKey())) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String camelCase = toCamelCase(unit);
                    checkForDup(seen, camelCase, unit);
                    out.printf("    measureUnit.adoptInstead(MeasureUnit::create%s(status));\n", camelCase);
                    out.printf("    measureUnitValue = MeasureUnit::get%s();\n", camelCase);
                }
            }
            out.println("    assertSuccess(\"\", status);");
            out.println("}");
        }
    }

    private static String toJAVAName(MeasureUnit unit) {
        String code = unit.getSubtype();
        String type = unit.getType();

        String replacement = CLDR_NAME_REMAP.get(code);
         if (replacement != null) {
            code = replacement;
        }

        String name = code.toUpperCase(Locale.ENGLISH).replace("-", "_");
        if (type.equals("angle")) {
            if (code.equals("minute") || code.equals("second")) {
                name = "ARC_" + name;
            }
        }
        return name;
    }

    private static void generateConstants(String thisVersion) throws IOException {
        String fullOutputPath = "${icuroot}/icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java";
        try (PrintStream out = createAndStartOutputFile(fullOutputPath)) {
            out.println("    // Start generated MeasureUnit constants");
            out.println();
            Map<String, MeasureUnit> seen = new HashMap<>();
            TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
            for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
                String type = entry.getKey();
                if (isTypeHidden(type)) {
                    continue;
                }
                for (MeasureUnit unit : entry.getValue()) {
                    String name = toJAVAName(unit);
                    String code = unit.getSubtype();
                    checkForDup(seen, name, unit);
                    out.println("    /**");
                    out.println("     * Constant for unit of " + type +
                            ": " +
                            code);
                    // Special case JAVA had old constants for time from before.
                    if ("duration".equals(type) && TIME_CODES.contains(code)) {
                        out.println("     * @stable ICU 4.0");
                    }
                    else if (isDraft(name)) {
                        out.println("     * @draft ICU " + getVersion(name, thisVersion));
                    } else {
                        out.println("     * @stable ICU " + getVersion(name, thisVersion));
                    }
                    out.println("     */");
                    if ("duration".equals(type) && TIME_CODES.contains(code)) {
                        out.println("    public static final TimeUnit " + name + " = (TimeUnit) MeasureUnit.internalGetInstance(\"" +
                                type +
                                "\", \"" +
                                code +
                                "\");");
                    } else {
                        out.println("    public static final MeasureUnit " + name + " = MeasureUnit.internalGetInstance(\"" +
                                type +
                                "\", \"" +
                                code +
                                "\");");
                    }
                    out.println();
                }
            }
            out.println("    // End generated MeasureUnit constants");
        }
    }

    private static String getVersion(String javaName, String thisVersion) {
        String version = JAVA_VERSION_MAP.get(javaName);
        if (version == null) {
            return thisVersion;
        }
        return version;
    }

    private static boolean isDraft(String javaName) {
        String version = JAVA_VERSION_MAP.get(javaName);
        if (version == null) {
            return true;
        }
        return DRAFT_VERSION_SET.contains(version);
    }

    private static PrintStream createAndStartOutputFile(String fullOutputFileName) throws IOException {
        if (fullOutputFileName.startsWith("${icuroot}")) {
            fullOutputFileName = fullOutputFileName.replace("${icuroot}", ICU_ROOT);
        }
        File outputFile = new File("target", new File(fullOutputFileName).getName());
        System.out.printf("%nCopy the generated code fragments from / to\n    %s \\\n    %s%n",
                outputFile.getAbsoluteFile(), fullOutputFileName);

        return new PrintStream(outputFile, "utf-8");
    }

    /*
     * This is not a real test class. It is only used to
     *generate updated unit tests code based on new CLDR data.
     * Do not add any other tests here.
     */
}
