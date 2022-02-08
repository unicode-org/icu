// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.serializable.FormatHandler;
import com.ibm.icu.dev.test.serializable.SerializableTestUtility;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;

/**
 * See https://sites.google.com/site/icusite/processes/release/tasks/standards?pli=1
 * for information on how to update with each new release.
 * @author markdavis
 */
@RunWith(JUnit4.class)
public class MeasureUnitTest extends TestFmwk {

    static class OrderedPair<F extends Comparable, S extends Comparable> extends Pair<F, S> implements Comparable<OrderedPair<F, S>> {

        OrderedPair(F first, S second) {
            super(first, second);
        }

        public static <F extends Comparable, S extends Comparable> OrderedPair<F, S> of(F first, S second) {
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

    private static final String[] DRAFT_VERSIONS = {"70", "71"};

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
    };

    private static final HashMap<String, String> JAVA_VERSION_MAP = new HashMap<>();

    // modify certain CLDR unit names before generating functions
    // that create/get the corresponding MeasureUnit objects
    private static final Map<String,String> CLDR_NAME_REMAP = new HashMap();

    static {
        TIME_CODES.add("year");
        TIME_CODES.add("month");
        TIME_CODES.add("week");
        TIME_CODES.add("day");
        TIME_CODES.add("hour");
        TIME_CODES.add("minute");
        TIME_CODES.add("second");
        for (String verNum : DRAFT_VERSIONS) {
            DRAFT_VERSION_SET.add(verNum);
        }
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

    @Test
    public void testZZZ() {
        // various generateXXX calls go here, see
        // docs/processes/release/tasks/updating-measure-unit.md
        // use this test to run each of the ollowing in succession
        //generateConstants("71"); // for MeasureUnit.java, update generated MeasureUnit constants
        //generateBackwardCompatibilityTest("71"); // for MeasureUnitTest.java, create TestCompatible70
        //generateCXXHConstants("71"); // for measunit.h, update generated createXXX methods
        //generateCXXConstants(); // for measunit.cpp, update generated code
        //generateCXXBackwardCompatibilityTest("71"); // for measfmttest.cpp, create TestCompatible70
        //updateJAVAVersions("71"); // for MeasureUnitTest.java, JAVA_VERSIONS
    }

    @Test
    public void TestCompatible53() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.DEGREE,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.MILLISECOND,
                MeasureUnit.CENTIMETER,
                MeasureUnit.FOOT,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MILE,
                MeasureUnit.MILLIMETER,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.POUND,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.LITER,
                MeasureUnit.YEAR,
                MeasureUnit.MONTH,
                MeasureUnit.WEEK,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MINUTE,
                MeasureUnit.SECOND,
        };
        assertEquals("", 46, units.length);
    }

    @Test
    public void TestCompatible54() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KARAT,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  121, units.length);
    }

    @Test
    public void TestCompatible55() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KARAT,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  122, units.length);
    }

    @Test
    public void TestCompatible56() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KARAT,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  129, units.length);
    }

    @Test
    public void TestCompatible57() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  134, units.length);
    }

    @Test
    public void TestCompatible58() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                // MeasureUnit.EAST,
                // MeasureUnit.NORTH,
                // MeasureUnit.SOUTH,
                // MeasureUnit.WEST,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  134, units.length);
    }

    @Test
    public void TestCompatible59() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  135, units.length);
    }

    // Note that TestCompatible60(), TestCompatible61(), TestCompatible62()
    // would be the same as TestCompatible59(), no need to add them.

    @Test
    public void TestCompatible63() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.YEAR,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.CALORIE,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.CARAT,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  139, units.length);
    }

    @Test
    public void TestCompatible64() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.DUNAM,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.MOLE,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.PERMYRIAD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.DAY_PERSON,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.MONTH_PERSON,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.WEEK_PERSON,
                MeasureUnit.YEAR,
                MeasureUnit.YEAR_PERSON,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.BRITISH_THERMAL_UNIT,
                MeasureUnit.CALORIE,
                MeasureUnit.ELECTRONVOLT,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.NEWTON,
                MeasureUnit.POUND_FORCE,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.SOLAR_RADIUS,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.SOLAR_LUMINOSITY,
                MeasureUnit.CARAT,
                MeasureUnit.DALTON,
                MeasureUnit.EARTH_MASS,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.SOLAR_MASS,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.KILOPASCAL,
                MeasureUnit.MEGAPASCAL,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.NEWTON_METER,
                MeasureUnit.POUND_FOOT,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BARREL,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.FLUID_OUNCE_IMPERIAL,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  161, units.length);
    }

    @Test
    public void TestCompatible65() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.DUNAM,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.MOLE,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.PERMYRIAD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.DAY_PERSON,
                MeasureUnit.DECADE,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.MONTH_PERSON,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.WEEK_PERSON,
                MeasureUnit.YEAR,
                MeasureUnit.YEAR_PERSON,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.BRITISH_THERMAL_UNIT,
                MeasureUnit.CALORIE,
                MeasureUnit.ELECTRONVOLT,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.THERM_US,
                MeasureUnit.NEWTON,
                MeasureUnit.POUND_FORCE,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.DOT_PER_CENTIMETER,
                MeasureUnit.DOT_PER_INCH,
                MeasureUnit.EM,
                MeasureUnit.MEGAPIXEL,
                MeasureUnit.PIXEL,
                MeasureUnit.PIXEL_PER_CENTIMETER,
                MeasureUnit.PIXEL_PER_INCH,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.SOLAR_RADIUS,
                MeasureUnit.YARD,
                MeasureUnit.LUX,
                MeasureUnit.SOLAR_LUMINOSITY,
                MeasureUnit.CARAT,
                MeasureUnit.DALTON,
                MeasureUnit.EARTH_MASS,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.SOLAR_MASS,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.BAR,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.KILOPASCAL,
                MeasureUnit.MEGAPASCAL,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.PASCAL,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.NEWTON_METER,
                MeasureUnit.POUND_FOOT,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BARREL,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.FLUID_OUNCE_IMPERIAL,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  172, units.length);
    }

    @Test
    public void TestCompatible68() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.DUNAM,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.MOLE,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERMYRIAD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.DAY_PERSON,
                MeasureUnit.DECADE,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.MONTH_PERSON,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.WEEK_PERSON,
                MeasureUnit.YEAR,
                MeasureUnit.YEAR_PERSON,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.BRITISH_THERMAL_UNIT,
                MeasureUnit.CALORIE,
                MeasureUnit.ELECTRONVOLT,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.THERM_US,
                MeasureUnit.NEWTON,
                MeasureUnit.POUND_FORCE,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.DOT,
                MeasureUnit.DOT_PER_CENTIMETER,
                MeasureUnit.DOT_PER_INCH,
                MeasureUnit.EM,
                MeasureUnit.MEGAPIXEL,
                MeasureUnit.PIXEL,
                MeasureUnit.PIXEL_PER_CENTIMETER,
                MeasureUnit.PIXEL_PER_INCH,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.EARTH_RADIUS,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.SOLAR_RADIUS,
                MeasureUnit.YARD,
                MeasureUnit.CANDELA,
                MeasureUnit.LUMEN,
                MeasureUnit.LUX,
                MeasureUnit.SOLAR_LUMINOSITY,
                MeasureUnit.CARAT,
                MeasureUnit.DALTON,
                MeasureUnit.EARTH_MASS,
                MeasureUnit.GRAIN,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.SOLAR_MASS,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.BAR,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.KILOPASCAL,
                MeasureUnit.MEGAPASCAL,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.PASCAL,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.NEWTON_METER,
                MeasureUnit.POUND_FOOT,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BARREL,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.DESSERT_SPOON,
                MeasureUnit.DESSERT_SPOON_IMPERIAL,
                MeasureUnit.DRAM,
                MeasureUnit.DROP,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.FLUID_OUNCE_IMPERIAL,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.JIGGER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINCH,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.QUART_IMPERIAL,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  184, units.length);
    }

    @Test
    public void TestCompatible69() {
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.DUNAM,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_OFGLUCOSE_PER_DECILITER,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.MOLE,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERMYRIAD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.DAY_PERSON,
                MeasureUnit.DECADE,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.MONTH_PERSON,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.WEEK_PERSON,
                MeasureUnit.YEAR,
                MeasureUnit.YEAR_PERSON,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.BRITISH_THERMAL_UNIT,
                MeasureUnit.CALORIE,
                MeasureUnit.ELECTRONVOLT,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.THERM_US,
                MeasureUnit.NEWTON,
                MeasureUnit.POUND_FORCE,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.DOT,
                MeasureUnit.DOT_PER_CENTIMETER,
                MeasureUnit.DOT_PER_INCH,
                MeasureUnit.EM,
                MeasureUnit.MEGAPIXEL,
                MeasureUnit.PIXEL,
                MeasureUnit.PIXEL_PER_CENTIMETER,
                MeasureUnit.PIXEL_PER_INCH,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.EARTH_RADIUS,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.SOLAR_RADIUS,
                MeasureUnit.YARD,
                MeasureUnit.CANDELA,
                MeasureUnit.LUMEN,
                MeasureUnit.LUX,
                MeasureUnit.SOLAR_LUMINOSITY,
                MeasureUnit.CARAT,
                MeasureUnit.DALTON,
                MeasureUnit.EARTH_MASS,
                MeasureUnit.GRAIN,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.SOLAR_MASS,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.BAR,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.KILOPASCAL,
                MeasureUnit.MEGAPASCAL,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.PASCAL,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.NEWTON_METER,
                MeasureUnit.POUND_FOOT,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BARREL,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.DESSERT_SPOON,
                MeasureUnit.DESSERT_SPOON_IMPERIAL,
                MeasureUnit.DRAM,
                MeasureUnit.DROP,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.FLUID_OUNCE_IMPERIAL,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.JIGGER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINCH,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.QUART_IMPERIAL,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  185, units.length);
    }

    @Test
    public void TestCompatible70() { // TestCompatible71 would be identical
        MeasureUnit[] units = {
                MeasureUnit.G_FORCE,
                MeasureUnit.METER_PER_SECOND_SQUARED,
                MeasureUnit.ARC_MINUTE,
                MeasureUnit.ARC_SECOND,
                MeasureUnit.DEGREE,
                MeasureUnit.RADIAN,
                MeasureUnit.REVOLUTION_ANGLE,
                MeasureUnit.ACRE,
                MeasureUnit.DUNAM,
                MeasureUnit.HECTARE,
                MeasureUnit.SQUARE_CENTIMETER,
                MeasureUnit.SQUARE_FOOT,
                MeasureUnit.SQUARE_INCH,
                MeasureUnit.SQUARE_KILOMETER,
                MeasureUnit.SQUARE_METER,
                MeasureUnit.SQUARE_MILE,
                MeasureUnit.SQUARE_YARD,
                MeasureUnit.ITEM,
                MeasureUnit.KARAT,
                MeasureUnit.MILLIGRAM_OFGLUCOSE_PER_DECILITER,
                MeasureUnit.MILLIGRAM_PER_DECILITER,
                MeasureUnit.MILLIMOLE_PER_LITER,
                MeasureUnit.MOLE,
                MeasureUnit.PERCENT,
                MeasureUnit.PERMILLE,
                MeasureUnit.PART_PER_MILLION,
                MeasureUnit.PERMYRIAD,
                MeasureUnit.LITER_PER_100KILOMETERS,
                MeasureUnit.LITER_PER_KILOMETER,
                MeasureUnit.MILE_PER_GALLON,
                MeasureUnit.MILE_PER_GALLON_IMPERIAL,
                MeasureUnit.BIT,
                MeasureUnit.BYTE,
                MeasureUnit.GIGABIT,
                MeasureUnit.GIGABYTE,
                MeasureUnit.KILOBIT,
                MeasureUnit.KILOBYTE,
                MeasureUnit.MEGABIT,
                MeasureUnit.MEGABYTE,
                MeasureUnit.PETABYTE,
                MeasureUnit.TERABIT,
                MeasureUnit.TERABYTE,
                MeasureUnit.CENTURY,
                MeasureUnit.DAY,
                MeasureUnit.DAY_PERSON,
                MeasureUnit.DECADE,
                MeasureUnit.HOUR,
                MeasureUnit.MICROSECOND,
                MeasureUnit.MILLISECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.MONTH,
                MeasureUnit.MONTH_PERSON,
                MeasureUnit.NANOSECOND,
                MeasureUnit.SECOND,
                MeasureUnit.WEEK,
                MeasureUnit.WEEK_PERSON,
                MeasureUnit.YEAR,
                MeasureUnit.YEAR_PERSON,
                MeasureUnit.AMPERE,
                MeasureUnit.MILLIAMPERE,
                MeasureUnit.OHM,
                MeasureUnit.VOLT,
                MeasureUnit.BRITISH_THERMAL_UNIT,
                MeasureUnit.CALORIE,
                MeasureUnit.ELECTRONVOLT,
                MeasureUnit.FOODCALORIE,
                MeasureUnit.JOULE,
                MeasureUnit.KILOCALORIE,
                MeasureUnit.KILOJOULE,
                MeasureUnit.KILOWATT_HOUR,
                MeasureUnit.THERM_US,
                MeasureUnit.KILOWATT_HOUR_PER_100_KILOMETER,
                MeasureUnit.NEWTON,
                MeasureUnit.POUND_FORCE,
                MeasureUnit.GIGAHERTZ,
                MeasureUnit.HERTZ,
                MeasureUnit.KILOHERTZ,
                MeasureUnit.MEGAHERTZ,
                MeasureUnit.DOT,
                MeasureUnit.DOT_PER_CENTIMETER,
                MeasureUnit.DOT_PER_INCH,
                MeasureUnit.EM,
                MeasureUnit.MEGAPIXEL,
                MeasureUnit.PIXEL,
                MeasureUnit.PIXEL_PER_CENTIMETER,
                MeasureUnit.PIXEL_PER_INCH,
                MeasureUnit.ASTRONOMICAL_UNIT,
                MeasureUnit.CENTIMETER,
                MeasureUnit.DECIMETER,
                MeasureUnit.EARTH_RADIUS,
                MeasureUnit.FATHOM,
                MeasureUnit.FOOT,
                MeasureUnit.FURLONG,
                MeasureUnit.INCH,
                MeasureUnit.KILOMETER,
                MeasureUnit.LIGHT_YEAR,
                MeasureUnit.METER,
                MeasureUnit.MICROMETER,
                MeasureUnit.MILE,
                MeasureUnit.MILE_SCANDINAVIAN,
                MeasureUnit.MILLIMETER,
                MeasureUnit.NANOMETER,
                MeasureUnit.NAUTICAL_MILE,
                MeasureUnit.PARSEC,
                MeasureUnit.PICOMETER,
                MeasureUnit.POINT,
                MeasureUnit.SOLAR_RADIUS,
                MeasureUnit.YARD,
                MeasureUnit.CANDELA,
                MeasureUnit.LUMEN,
                MeasureUnit.LUX,
                MeasureUnit.SOLAR_LUMINOSITY,
                MeasureUnit.CARAT,
                MeasureUnit.DALTON,
                MeasureUnit.EARTH_MASS,
                MeasureUnit.GRAIN,
                MeasureUnit.GRAM,
                MeasureUnit.KILOGRAM,
                MeasureUnit.METRIC_TON,
                MeasureUnit.MICROGRAM,
                MeasureUnit.MILLIGRAM,
                MeasureUnit.OUNCE,
                MeasureUnit.OUNCE_TROY,
                MeasureUnit.POUND,
                MeasureUnit.SOLAR_MASS,
                MeasureUnit.STONE,
                MeasureUnit.TON,
                MeasureUnit.GIGAWATT,
                MeasureUnit.HORSEPOWER,
                MeasureUnit.KILOWATT,
                MeasureUnit.MEGAWATT,
                MeasureUnit.MILLIWATT,
                MeasureUnit.WATT,
                MeasureUnit.ATMOSPHERE,
                MeasureUnit.BAR,
                MeasureUnit.HECTOPASCAL,
                MeasureUnit.INCH_HG,
                MeasureUnit.KILOPASCAL,
                MeasureUnit.MEGAPASCAL,
                MeasureUnit.MILLIBAR,
                MeasureUnit.MILLIMETER_OF_MERCURY,
                MeasureUnit.PASCAL,
                MeasureUnit.POUND_PER_SQUARE_INCH,
                MeasureUnit.KILOMETER_PER_HOUR,
                MeasureUnit.KNOT,
                MeasureUnit.METER_PER_SECOND,
                MeasureUnit.MILE_PER_HOUR,
                MeasureUnit.CELSIUS,
                MeasureUnit.FAHRENHEIT,
                MeasureUnit.GENERIC_TEMPERATURE,
                MeasureUnit.KELVIN,
                MeasureUnit.NEWTON_METER,
                MeasureUnit.POUND_FOOT,
                MeasureUnit.ACRE_FOOT,
                MeasureUnit.BARREL,
                MeasureUnit.BUSHEL,
                MeasureUnit.CENTILITER,
                MeasureUnit.CUBIC_CENTIMETER,
                MeasureUnit.CUBIC_FOOT,
                MeasureUnit.CUBIC_INCH,
                MeasureUnit.CUBIC_KILOMETER,
                MeasureUnit.CUBIC_METER,
                MeasureUnit.CUBIC_MILE,
                MeasureUnit.CUBIC_YARD,
                MeasureUnit.CUP,
                MeasureUnit.CUP_METRIC,
                MeasureUnit.DECILITER,
                MeasureUnit.DESSERT_SPOON,
                MeasureUnit.DESSERT_SPOON_IMPERIAL,
                MeasureUnit.DRAM,
                MeasureUnit.DROP,
                MeasureUnit.FLUID_OUNCE,
                MeasureUnit.FLUID_OUNCE_IMPERIAL,
                MeasureUnit.GALLON,
                MeasureUnit.GALLON_IMPERIAL,
                MeasureUnit.HECTOLITER,
                MeasureUnit.JIGGER,
                MeasureUnit.LITER,
                MeasureUnit.MEGALITER,
                MeasureUnit.MILLILITER,
                MeasureUnit.PINCH,
                MeasureUnit.PINT,
                MeasureUnit.PINT_METRIC,
                MeasureUnit.QUART,
                MeasureUnit.QUART_IMPERIAL,
                MeasureUnit.TABLESPOON,
                MeasureUnit.TEASPOON,
        };
        assertEquals("",  187, units.length);
    }

    // TestCompatible71 would be identical to TestCompatible70,
    // no need to add it

    @Test
    public void TestExamplesInDocs() {
        MeasureFormat fmtFr = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.SHORT);
        Measure measure = new Measure(23, MeasureUnit.CELSIUS);
        assertEquals("23\u202F°C", "23\u202F°C", fmtFr.format(measure));
        Measure measureF = new Measure(70, MeasureUnit.FAHRENHEIT);
        assertEquals("70\u202F°F", "70\u202F°F", fmtFr.format(measureF));
        MeasureFormat fmtFrFull = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.WIDE);
        assertEquals(
                "70 pied et 5,3 pouces",
                "70 pieds et 5,3 pouces",
                fmtFrFull.formatMeasures(
                        new Measure(70, MeasureUnit.FOOT),
                        new Measure(5.3, MeasureUnit.INCH)));
        assertEquals(
                "1\u00A0pied et 1\u00A0pouce",
                "1\u00A0pied et 1\u00A0pouce",
                fmtFrFull.formatMeasures(
                        new Measure(1, MeasureUnit.FOOT),
                        new Measure(1, MeasureUnit.INCH)));
        MeasureFormat fmtFrNarrow = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.NARROW);
        assertEquals(
                "1′ 1″",
                "1′ 1″",
                fmtFrNarrow.formatMeasures(
                        new Measure(1, MeasureUnit.FOOT),
                        new Measure(1, MeasureUnit.INCH)));
        MeasureFormat fmtEn = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals(
                "1 inch, 2 feet",
                "1 inch, 2 feet",
                fmtEn.formatMeasures(
                        new Measure(1, MeasureUnit.INCH),
                        new Measure(2, MeasureUnit.FOOT)));
    }

    @Test
    public void TestFormatPeriodEn() {
        TimeUnitAmount[] _19m = {new TimeUnitAmount(19.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_23_5s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.SECOND)};
        TimeUnitAmount[] _1h_23_5m = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_0m_23s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(23.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _2y_5M_3w_4d = {
                new TimeUnitAmount(2.0, TimeUnit.YEAR),
                new TimeUnitAmount(5.0, TimeUnit.MONTH),
                new TimeUnitAmount(3.0, TimeUnit.WEEK),
                new TimeUnitAmount(4.0, TimeUnit.DAY)};
        TimeUnitAmount[] _1m_59_9996s = {
                new TimeUnitAmount(1.0, TimeUnit.MINUTE),
                new TimeUnitAmount(59.9996, TimeUnit.SECOND)};
        TimeUnitAmount[] _5h_17m = {
                new TimeUnitAmount(5.0, TimeUnit.HOUR),
                new TimeUnitAmount(17.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _neg5h_17m = {
                new TimeUnitAmount(-5.0, TimeUnit.HOUR),
                new TimeUnitAmount(17.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _19m_28s = {
                new TimeUnitAmount(19.0, TimeUnit.MINUTE),
                new TimeUnitAmount(28.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _0h_0m_9s = {
                new TimeUnitAmount(0.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(9.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _0h_0m_17s = {
                new TimeUnitAmount(0.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(17.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _6h_56_92m = {
                new TimeUnitAmount(6.0, TimeUnit.HOUR),
                new TimeUnitAmount(56.92, TimeUnit.MINUTE)};
        TimeUnitAmount[] _3h_4s_5m = {
                new TimeUnitAmount(3.0, TimeUnit.HOUR),
                new TimeUnitAmount(4.0, TimeUnit.SECOND),
                new TimeUnitAmount(5.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _6_7h_56_92m = {
                new TimeUnitAmount(6.7, TimeUnit.HOUR),
                new TimeUnitAmount(56.92, TimeUnit.MINUTE)};
        TimeUnitAmount[] _3h_5h = {
                new TimeUnitAmount(3.0, TimeUnit.HOUR),
                new TimeUnitAmount(5.0, TimeUnit.HOUR)};

        Object[][] fullData = {
                {_1m_59_9996s, "1 minute, 59.9996 seconds"},
                {_19m, "19 minutes"},
                {_1h_23_5s, "1 hour, 23.5 seconds"},
                {_1h_23_5m, "1 hour, 23.5 minutes"},
                {_1h_0m_23s, "1 hour, 0 minutes, 23 seconds"},
                {_2y_5M_3w_4d, "2 years, 5 months, 3 weeks, 4 days"}};
        Object[][] abbrevData = {
                {_1m_59_9996s, "1 min, 59.9996 sec"},
                {_19m, "19 min"},
                {_1h_23_5s, "1 hr, 23.5 sec"},
                {_1h_23_5m, "1 hr, 23.5 min"},
                {_1h_0m_23s, "1 hr, 0 min, 23 sec"},
                {_2y_5M_3w_4d, "2 yrs, 5 mths, 3 wks, 4 days"}};
        Object[][] narrowData = {
                {_1m_59_9996s, "1m 59.9996s"},
                {_19m, "19m"},
                {_1h_23_5s, "1h 23.5s"},
                {_1h_23_5m, "1h 23.5m"},
                {_1h_0m_23s, "1h 0m 23s"},
                {_2y_5M_3w_4d, "2y 5m 3w 4d"}};


        Object[][] numericData = {
                {_1m_59_9996s, "1:59.9996"},
                {_19m, "19m"},
                {_1h_23_5s, "1:00:23.5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23.5"},
                {_5h_17m, "5:17"},
                {_neg5h_17m, "-5h 17m"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2y 5m 3w 4d"},
                {_0h_0m_9s, "0:00:09"},
                {_6h_56_92m, "6:56.92"},
                {_6_7h_56_92m, "6:56.92"},
                {_3h_4s_5m, "3h 4s 5m"},
                {_3h_5h, "3h 5h"}};
        Object[][] fullDataDe = {
                {_1m_59_9996s, "1 Minute, 59,9996 Sekunden"},
                {_19m, "19 Minuten"},
                {_1h_23_5s, "1 Stunde, 23,5 Sekunden"},
                {_1h_23_5m, "1 Stunde, 23,5 Minuten"},
                {_1h_0m_23s, "1 Stunde, 0 Minuten und 23 Sekunden"},
                {_2y_5M_3w_4d, "2 Jahre, 5 Monate, 3 Wochen und 4 Tage"}};
        Object[][] numericDataDe = {
                {_1m_59_9996s, "1:59,9996"},
                {_19m, "19 Min."},
                {_1h_23_5s, "1:00:23,5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23,5"},
                {_5h_17m, "5:17"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2 J, 5 M, 3 W und 4 T"},
                {_0h_0m_17s, "0:00:17"},
                {_6h_56_92m, "6:56,92"},
                {_3h_5h, "3 Std., 5 Std."}};
        Object[][] numericDataBn = {
                {_1m_59_9996s, "১:৫৯.৯৯৯৬"},
                {_19m, "১৯ মিঃ"},
                {_1h_23_5s, "১:০০:২৩.৫"},
                {_1h_0m_23s, "১:০০:২৩"},
                {_1h_23_5m, "১:২৩.৫"},
                {_5h_17m, "৫:১৭"},
                {_19m_28s, "১৯:২৮"},
                {_2y_5M_3w_4d, "২ বছর, ৫ মাস, ৩ সপ্তাহ, ৪ দিন"},
                {_0h_0m_17s, "০:০০:১৭"},
                {_6h_56_92m, "৬:৫৬.৯২"},
                {_3h_5h, "৩ ঘঃ, ৫ ঘঃ"}};
        Object[][] numericDataBnLatn = {
                {_1m_59_9996s, "1:59.9996"},
                {_19m, "19 মিঃ"},
                {_1h_23_5s, "1:00:23.5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23.5"},
                {_5h_17m, "5:17"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2 বছর, 5 মাস, 3 সপ্তাহ, 4 দিন"},
                {_0h_0m_17s, "0:00:17"},
                {_6h_56_92m, "6:56.92"},
                {_3h_5h, "3 ঘঃ, 5 ঘঃ"}};

        NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
        nf.setMaximumFractionDigits(4);
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE, nf);
        verifyFormatPeriod("en FULL", mf, fullData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT, nf);
        verifyFormatPeriod("en SHORT", mf, abbrevData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NARROW, nf);
        verifyFormatPeriod("en NARROW", mf, narrowData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("en NUMERIC", mf, numericData);

        nf = NumberFormat.getNumberInstance(ULocale.GERMAN);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.WIDE, nf);
        verifyFormatPeriod("de FULL", mf, fullDataDe);
        mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("de NUMERIC", mf, numericDataDe);

        // Same tests, with Java Locale
        nf = NumberFormat.getNumberInstance(Locale.GERMAN);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.WIDE, nf);
        verifyFormatPeriod("de FULL(Java Locale)", mf, fullDataDe);
        mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("de NUMERIC(Java Locale)", mf, numericDataDe);

        ULocale bengali = ULocale.forLanguageTag("bn");
        nf = NumberFormat.getNumberInstance(bengali);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(bengali, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("bn NUMERIC(Java Locale)", mf, numericDataBn);

        bengali = ULocale.forLanguageTag("bn-u-nu-latn");
        nf = NumberFormat.getNumberInstance(bengali);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(bengali, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("bn NUMERIC(Java Locale)", mf, numericDataBnLatn);
    }

    private void verifyFormatPeriod(String desc, MeasureFormat mf, Object[][] testData) {
        StringBuilder builder = new StringBuilder();
        boolean failure = false;
        for (Object[] testCase : testData) {
            String actual = mf.format(testCase[0]);
            if (!testCase[1].equals(actual)) {
                builder.append(String.format("%s: Expected: '%s', got: '%s'\n", desc, testCase[1], actual));
                failure = true;
            }
        }
        if (failure) {
            errln(builder.toString());
        }
    }

    @Test
    public void Test10219FractionalPlurals() {
        double[] values = {1.588, 1.011};
        String[][] expected = {
                {"1 minute", "1.5 minutes", "1.58 minutes"},
                {"1 minute", "1.0 minutes", "1.01 minutes"}
        };
        for (int j = 0; j < values.length; j++) {
            for (int i = 0; i < expected[j].length; i++) {
                NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
                nf.setRoundingMode(BigDecimal.ROUND_DOWN);
                nf.setMinimumFractionDigits(i);
                nf.setMaximumFractionDigits(i);
                MeasureFormat mf = MeasureFormat.getInstance(
                        ULocale.ENGLISH, FormatWidth.WIDE, nf);
                assertEquals("Test10219", expected[j][i], mf.format(new Measure(values[j], MeasureUnit.MINUTE)));
            }
        }
    }

    @Test
    public void TestGreek() {
        String[] locales = {"el_GR", "el"};
        final MeasureUnit[] units = new MeasureUnit[]{
                MeasureUnit.SECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.HOUR,
                MeasureUnit.DAY,
                MeasureUnit.WEEK,
                MeasureUnit.MONTH,
                MeasureUnit.YEAR};
        FormatWidth[] styles = new FormatWidth[] {FormatWidth.WIDE, FormatWidth.SHORT};
        int[] numbers = new int[] {1, 7};
        String[] expected = {
                // "el_GR" 1 wide
                "1 δευτερόλεπτο",
                "1 λεπτό",
                "1 ώρα",
                "1 ημέρα",
                "1 εβδομάδα",
                "1 μήνας",
                "1 έτος",
                // "el_GR" 1 short
                "1 δευτ.",
                "1 λ.",
                "1 ώ.",
                "1 ημέρα",
                "1 εβδ.",
                "1 μήν.",
                "1 έτ.",	        // year (one)
                // "el_GR" 7 wide
                "7 δευτερόλεπτα",
                "7 λεπτά",
                "7 ώρες",
                "7 ημέρες",
                "7 εβδομάδες",
                "7 μήνες",
                "7 έτη",
                // "el_GR" 7 short
                "7 δευτ.",
                "7 λ.",
                "7 ώ.",		       // hour (other)
                "7 ημέρες",
                "7 εβδ.",
                "7 μήν.",
                "7 έτ.",            // year (other)
                // "el" 1 wide
                "1 δευτερόλεπτο",
                "1 λεπτό",
                "1 ώρα",
                "1 ημέρα",
                "1 εβδομάδα",
                "1 μήνας",
                "1 έτος",
                // "el" 1 short
                "1 δευτ.",
                "1 λ.",
                "1 ώ.",
                "1 ημέρα",
                "1 εβδ.",
                "1 μήν.",
                "1 έτ.",	        // year (one)
                // "el" 7 wide
                "7 δευτερόλεπτα",
                "7 λεπτά",
                "7 ώρες",
                "7 ημέρες",
                "7 εβδομάδες",
                "7 μήνες",
                "7 έτη",
                // "el" 7 short
                "7 δευτ.",
                "7 λ.",
                "7 ώ.",		        // hour (other)
                "7 ημέρες",
                "7 εβδ.",
                "7 μήν.",
                "7 έτ."};           // year (other
        int counter = 0;
        String formatted;
        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            for( int numIndex = 0; numIndex < numbers.length; ++numIndex ) {
                for ( int styleIndex = 0; styleIndex < styles.length; ++styleIndex ) {
                    for ( int unitIndex = 0; unitIndex < units.length; ++unitIndex ) {
                        Measure m = new Measure(numbers[numIndex], units[unitIndex]);
                        MeasureFormat fmt = MeasureFormat.getInstance(new ULocale(locales[locIndex]), styles[styleIndex]);
                        formatted = fmt.format(m);
                        assertEquals(
                                "locale: " + locales[locIndex]
                                        + ", style: " + styles[styleIndex]
                                                + ", units: " + units[unitIndex]
                                                        + ", value: " + numbers[numIndex],
                                                expected[counter], formatted);
                        ++counter;
                    }
                }
            }
        }
    }

    @Test
    public void testAUnit() {
        String lastType = null;
        for (MeasureUnit expected : MeasureUnit.getAvailable()) {
            String type = expected.getType();
            String code = expected.getSubtype();
            if (!type.equals(lastType)) {
                logln(type);
                lastType = type;
            }
            MeasureUnit actual = MeasureUnit.internalGetInstance(type, code);
            assertSame("Identity check", expected, actual);
        }

        // The return value should contain only unique elements
        assertUnique(MeasureUnit.getAvailable());
    }

    static void assertUnique(Collection<?> coll) {
        int expectedSize = new HashSet<>(coll).size();
        int actualSize = coll.size();
        assertEquals("Collection should contain only unique elements", expectedSize, actualSize);
    }

    @Test
    public void testFormatSingleArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.format(new Measure(5, MeasureUnit.METER)));
    }

    @Test
    public void testFormatMeasuresZeroArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "", mf.formatMeasures());
    }

    @Test
    public void testFormatMeasuresOneArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.formatMeasures(new Measure(5, MeasureUnit.METER)));
    }



    @Test
    public void testMultiples() {
        ULocale russia = new ULocale("ru");
        Object[][] data = new Object[][] {
                {ULocale.ENGLISH, FormatWidth.WIDE, "2 miles, 1 foot, 2.3 inches"},
                {ULocale.ENGLISH, FormatWidth.SHORT, "2 mi, 1 ft, 2.3 in"},
                {ULocale.ENGLISH, FormatWidth.NARROW, "2mi 1\u2032 2.3\u2033"},
                {russia, FormatWidth.WIDE,   "2 \u043C\u0438\u043B\u0438 1 \u0444\u0443\u0442 2,3 \u0434\u044E\u0439\u043C\u0430"},
                {russia, FormatWidth.SHORT,  "2 \u043C\u0438 1 \u0444\u0442 2,3 \u0434\u044E\u0439\u043C."},
                {russia, FormatWidth.NARROW, "2 \u043C\u0438 1 \u0444\u0442 2,3 \u0434\u044E\u0439\u043C."},
   };
        for (Object[] row : data) {
            MeasureFormat mf = MeasureFormat.getInstance(
                    (ULocale) row[0], (FormatWidth) row[1]);
            assertEquals(
                    "testMultiples",
                    row[2],
                    mf.formatMeasures(
                            new Measure(2, MeasureUnit.MILE),
                            new Measure(1, MeasureUnit.FOOT),
                            new Measure(2.3, MeasureUnit.INCH)));
        }
    }

    @Test
    public void testManyLocaleDurations() {
        Measure hours   = new Measure(5, MeasureUnit.HOUR);
        Measure minutes = new Measure(37, MeasureUnit.MINUTE);
        ULocale ulocDanish       = new ULocale("da");
        ULocale ulocSpanish      = new ULocale("es");
        ULocale ulocFinnish      = new ULocale("fi");
        ULocale ulocIcelandic    = new ULocale("is");
        ULocale ulocNorwegianBok = new ULocale("nb");
        ULocale ulocNorwegianNyn = new ULocale("nn");
        ULocale ulocDutch        = new ULocale("nl");
        ULocale ulocSwedish      = new ULocale("sv");
        Object[][] data = new Object[][] {
            { ulocDanish,       FormatWidth.NARROW,  "5 t og 37 m" },
            { ulocDanish,       FormatWidth.NUMERIC, "5.37" },
            { ULocale.GERMAN,   FormatWidth.NARROW,  "5 Std., 37 Min." },
            { ULocale.GERMAN,   FormatWidth.NUMERIC, "5:37" },
            { ULocale.ENGLISH,  FormatWidth.NARROW,  "5h 37m" },
            { ULocale.ENGLISH,  FormatWidth.NUMERIC, "5:37" },
            { ulocSpanish,      FormatWidth.NARROW,  "5h 37min" },
            { ulocSpanish,      FormatWidth.NUMERIC, "5:37" },
            { ulocFinnish,      FormatWidth.NARROW,  "5t 37min" },
            { ulocFinnish,      FormatWidth.NUMERIC, "5.37" },
            { ULocale.FRENCH,   FormatWidth.NARROW,  "5h 37min" },
            { ULocale.FRENCH,   FormatWidth.NUMERIC, "5:37" },
            { ulocIcelandic,    FormatWidth.NARROW,  "5 klst. og 37 m\u00EDn." },
            { ulocIcelandic,    FormatWidth.NUMERIC, "5:37" },
            { ULocale.JAPANESE, FormatWidth.NARROW,  "5h37m" },
            { ULocale.JAPANESE, FormatWidth.NUMERIC, "5:37" },
            { ulocNorwegianBok, FormatWidth.NARROW,  "5t, 37m" },
            { ulocNorwegianBok, FormatWidth.NUMERIC, "5:37" },
            { ulocDutch,        FormatWidth.NARROW,  "5 u, 37 m" },
            { ulocDutch,        FormatWidth.NUMERIC, "5:37" },
            { ulocNorwegianNyn, FormatWidth.NARROW,  "5t 37m" },
            { ulocNorwegianNyn, FormatWidth.NUMERIC, "5:37" },
            { ulocSwedish,      FormatWidth.NARROW,  "5h 37m" },
            { ulocSwedish,      FormatWidth.NUMERIC, "5:37" },
            { ULocale.CHINESE,  FormatWidth.NARROW,  "5\u5C0F\u65F637\u5206\u949F" },
            { ULocale.CHINESE,  FormatWidth.NUMERIC, "5:37" },
        };
        for (Object[] row : data) {
            MeasureFormat mf = null;
            try{
                mf = MeasureFormat.getInstance( (ULocale)row[0], (FormatWidth)row[1] );
            } catch(Exception e) {
                errln("Exception creating MeasureFormat for locale " + row[0] + ", width " +
                        row[1] + ": " + e);
                continue;
            }
            String result = mf.formatMeasures(hours, minutes);
            if (!result.equals(row[2])) {
                errln("MeasureFormat.formatMeasures for locale " + row[0] + ", width " +
                        row[1] + ", expected \"" + (String)row[2] + "\", got \"" + result + "\"" );
            }
        }
    }

    @Test
    public void testSimplePer() {
        Object DONT_CARE = null;
        Object[][] data = new Object[][] {
                // per unit pattern
                {FormatWidth.WIDE, 1.0, MeasureUnit.SECOND, "1 pound per second", DONT_CARE, 0, 0},
                {FormatWidth.WIDE, 2.0, MeasureUnit.SECOND, "2 pounds per second", DONT_CARE, 0, 0},
                // compound pattern
                {FormatWidth.WIDE, 1.0, MeasureUnit.MINUTE, "1 pound per minute", DONT_CARE, 0, 0},
                {FormatWidth.WIDE, 2.0, MeasureUnit.MINUTE, "2 pounds per minute", DONT_CARE, 0, 0},
                // per unit
                {FormatWidth.SHORT, 1.0, MeasureUnit.SECOND, "1 lb/s", DONT_CARE, 0, 0},
                {FormatWidth.SHORT, 2.0, MeasureUnit.SECOND, "2 lb/s", DONT_CARE, 0, 0},
                // compound
                {FormatWidth.SHORT, 1.0, MeasureUnit.MINUTE, "1 lb/min", DONT_CARE, 0, 0},
                {FormatWidth.SHORT, 2.0, MeasureUnit.MINUTE, "2 lb/min", DONT_CARE, 0, 0},
                // per unit
                {FormatWidth.NARROW, 1.0, MeasureUnit.SECOND, "1#/s", DONT_CARE, 0, 0},
                {FormatWidth.NARROW, 2.0, MeasureUnit.SECOND, "2#/s", DONT_CARE, 0, 0},
                // compound
                {FormatWidth.NARROW, 1.0, MeasureUnit.MINUTE, "1#/min", DONT_CARE, 0, 0},
                {FormatWidth.NARROW, 2.0, MeasureUnit.MINUTE, "2#/min", DONT_CARE, 0, 0},
                // field positions
                {FormatWidth.SHORT, 23.3, MeasureUnit.SECOND, "23.3 lb/s", NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3},
                {FormatWidth.SHORT, 23.3, MeasureUnit.SECOND, "23.3 lb/s", NumberFormat.Field.INTEGER, 0, 2},
                {FormatWidth.SHORT, 23.3, MeasureUnit.MINUTE, "23.3 lb/min", NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3},
                {FormatWidth.SHORT, 23.3, MeasureUnit.MINUTE, "23.3 lb/min", NumberFormat.Field.INTEGER, 0, 2},

        };

        for (Object[] row : data) {
            FormatWidth formatWidth = (FormatWidth) row[0];
            Number amount = (Number) row[1];
            MeasureUnit perUnit = (MeasureUnit) row[2];
            String expected = row[3].toString();
            NumberFormat.Field field = (NumberFormat.Field) row[4];
            int startOffset = ((Integer) row[5]).intValue();
            int endOffset = ((Integer) row[6]).intValue();
            MeasureFormat mf = MeasureFormat.getInstance(
                    ULocale.ENGLISH, formatWidth);
            FieldPosition pos = field != null ? new FieldPosition(field) : new FieldPosition(0);
            String prefix = "Prefix: ";
            assertEquals(
                    "",
                    prefix + expected,
                    mf.formatMeasurePerUnit(
                            new Measure(amount, MeasureUnit.POUND),
                            perUnit,
                            new StringBuilder(prefix),
                            pos).toString());
            if (field != DONT_CARE) {
                assertEquals("startOffset", startOffset, pos.getBeginIndex() - prefix.length());
                assertEquals("endOffset", endOffset, pos.getEndIndex() - prefix.length());
            }
        }
    }

    @Test
    public void testNumeratorPlurals() {
        ULocale polish = new ULocale("pl");
        Object[][] data = new Object[][] {
                {1, "1 stopa na sekundę"},
                {2, "2 stopy na sekundę"},
                {5, "5 stóp na sekundę"},
                {1.5, "1,5 stopy na sekundę"}};

        for (Object[] row : data) {
            MeasureFormat mf = MeasureFormat.getInstance(polish, FormatWidth.WIDE);
            assertEquals(
                    "",
                    row[1],
                    mf.formatMeasurePerUnit(
                            new Measure((Number) row[0], MeasureUnit.FOOT),
                            MeasureUnit.SECOND,
                            new StringBuilder(),
                            new FieldPosition(0)).toString());
        }
    }

    @Test
    public void testGram() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        assertEquals(
                "testGram",
                "1 g",
                mf.format(new Measure(1, MeasureUnit.GRAM)));
        assertEquals(
                "testGram",
                "1 G",
                mf.format(new Measure(1, MeasureUnit.G_FORCE)));
    }

    @Test
    public void testCurrencies() {
        Measure USD_1 = new Measure(1.0, Currency.getInstance("USD"));
        Measure USD_2 = new Measure(2.0, Currency.getInstance("USD"));
        Measure USD_NEG_1 = new Measure(-1.0, Currency.getInstance("USD"));
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("Wide currency", "-1.00 US dollars", mf.format(USD_NEG_1));
        assertEquals("Wide currency", "1.00 US dollars", mf.format(USD_1));
        assertEquals("Wide currency", "2.00 US dollars", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        assertEquals("short currency", "-USD 1.00", mf.format(USD_NEG_1));
        assertEquals("short currency", "USD 1.00", mf.format(USD_1));
        assertEquals("short currency", "USD 2.00", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NARROW);
        assertEquals("narrow currency", "-$1.00", mf.format(USD_NEG_1));
        assertEquals("narrow currency", "$1.00", mf.format(USD_1));
        assertEquals("narrow currency", "$2.00", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NUMERIC);
        assertEquals("numeric currency", "-$1.00", mf.format(USD_NEG_1));
        assertEquals("numeric currency", "$1.00", mf.format(USD_1));
        assertEquals("numeric currency", "$2.00", mf.format(USD_2));

        mf = MeasureFormat.getInstance(ULocale.JAPAN, FormatWidth.WIDE);
        // Locale jp does NOT put a space between the number and the currency long name:
        // https://unicode.org/cldr/trac/browser/tags/release-32-0-1/common/main/ja.xml?rev=13805#L7046
        assertEquals("Wide currency", "-1.00\u7C73\u30C9\u30EB", mf.format(USD_NEG_1));
        assertEquals("Wide currency", "1.00\u7C73\u30C9\u30EB", mf.format(USD_1));
        assertEquals("Wide currency", "2.00\u7C73\u30C9\u30EB", mf.format(USD_2));

        Measure CAD_1 = new Measure(1.0, Currency.getInstance("CAD"));
        mf = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        assertEquals("short currency", "CAD 1.00", mf.format(CAD_1));
    }

    @Test
    public void testDisplayNames() {
        Object[][] data = new Object[][] {
            // Unit, locale, width, expected result
            { MeasureUnit.YEAR, "en", FormatWidth.WIDE, "years" },
            { MeasureUnit.YEAR, "ja", FormatWidth.WIDE, "年" },
            { MeasureUnit.YEAR, "es", FormatWidth.WIDE, "años" },
            { MeasureUnit.YEAR, "pt", FormatWidth.WIDE, "anos" },
            { MeasureUnit.YEAR, "pt-PT", FormatWidth.WIDE, "anos" },
            { MeasureUnit.AMPERE, "en", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.AMPERE, "ja", FormatWidth.WIDE, "アンペア" },
            { MeasureUnit.AMPERE, "es", FormatWidth.WIDE, "amperios" },
            { MeasureUnit.AMPERE, "pt", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.AMPERE, "pt-PT", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.METER_PER_SECOND_SQUARED, "pt", FormatWidth.WIDE, "metros por segundo ao quadrado" },
            { MeasureUnit.METER_PER_SECOND_SQUARED, "pt-PT", FormatWidth.WIDE, "metros por segundo quadrado" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.NARROW, "km²" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.SHORT, "km²" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.WIDE, "quilômetros quadrados" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.NARROW, "s" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.SHORT, "s" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.WIDE, "segundos" },
            { MeasureUnit.SECOND, "pt", FormatWidth.NARROW, "seg" },
            { MeasureUnit.SECOND, "pt", FormatWidth.SHORT, "seg" },
            { MeasureUnit.SECOND, "pt", FormatWidth.WIDE, "segundos" },
        };

        for (Object[] test : data) {
            MeasureUnit unit = (MeasureUnit) test[0];
            ULocale locale = ULocale.forLanguageTag((String) test[1]);
            FormatWidth formatWidth = (FormatWidth) test[2];
            String expected = (String) test[3];

            MeasureFormat mf = MeasureFormat.getInstance(locale, formatWidth);
            String actual = mf.getUnitDisplayName(unit);
            assertEquals(String.format("Unit Display Name for %s, %s, %s", unit, locale, formatWidth),
                    expected, actual);
        }
    }

    @Test
    public void testFieldPosition() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43.5, MeasureUnit.FOOT), new StringBuffer("123456: "), pos);
        assertEquals("beginIndex", 10, pos.getBeginIndex());
        assertEquals("endIndex", 11, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43, MeasureUnit.FOOT), new StringBuffer(), pos);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());
    }

    @Test
    public void testFieldPositionMultiple() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.INTEGER);
        String result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER)).toString();
        assertEquals("result", "354 m, 23 cm", result);

        // According to javadocs for {@link Format#format} FieldPosition is set to
        // beginning and end of first such field encountered instead of the last
        // such field encountered.
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 3, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 354 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 23, pos.getBeginIndex());
        assertEquals("endIndex", 24, pos.getEndIndex());

        result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "3 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 13, pos.getBeginIndex());
        assertEquals("endIndex", 14, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 3 m, 23 cm, 5 mm", result);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.INTEGER);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(57, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 57 mm", result);
        assertEquals("beginIndex", 8, pos.getBeginIndex());
        assertEquals("endIndex", 10, pos.getEndIndex());

    }

    @Test
    public void testOldFormatWithList() {
        List<Measure> measures = new ArrayList<>(2);
        measures.add(new Measure(5, MeasureUnit.ACRE));
        measures.add(new Measure(3000, MeasureUnit.SQUARE_FOOT));
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
        assertEquals("", "5 acres", fmt.format(measures.subList(0, 1)));
        List<String> badList = new ArrayList<>();
        badList.add("be");
        badList.add("you");
        try {
            fmt.format(badList);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
           // Expected
        }
    }

    @Test
    public void testOldFormatWithArray() {
        Measure[] measures = new Measure[] {
                new Measure(5, MeasureUnit.ACRE),
                new Measure(3000, MeasureUnit.SQUARE_FOOT),
        };
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
    }

    @Test
    public void testOldFormatBadArg() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        try {
            fmt.format("be");
            fail("Expected IllegalArgumentExceptino.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testUnitPerUnitResolution() {
        // Ticket 11274
        MeasureFormat fmt = MeasureFormat.getInstance(Locale.ENGLISH, FormatWidth.SHORT);

        // This fails unless we resolve to MeasureUnit.POUND_PER_SQUARE_INCH
        assertEquals("", "50 psi",
                fmt.formatMeasurePerUnit(
                        new Measure(50, MeasureUnit.POUND_FORCE),
                        MeasureUnit.SQUARE_INCH,
                        new StringBuilder(),
                        new FieldPosition(0)).toString());
    }

    @Test
    public void testEqHashCode() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfeq = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfne = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.WIDE);
        MeasureFormat mfne2 = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        verifyEqualsHashCode(mf, mfeq, mfne);
        verifyEqualsHashCode(mf, mfeq, mfne2);
    }

    @Test
    public void testEqHashCodeOfMeasure() {
        Measure _3feetDouble = new Measure(3.0, MeasureUnit.FOOT);
        Measure _3feetInt = new Measure(3, MeasureUnit.FOOT);
        Measure _4feetInt = new Measure(4, MeasureUnit.FOOT);
        verifyEqualsHashCode(_3feetDouble, _3feetInt, _4feetInt);
    }

    @Test
    public void testGetLocale() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT);
        assertEquals("", ULocale.GERMAN, mf.getLocale(ULocale.VALID_LOCALE));
    }

    @Test
    public void TestSerial() {
        checkStreamingEquality(MeasureUnit.CELSIUS);
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.NARROW));
        checkStreamingEquality(Currency.getInstance("EUR"));
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT));
        checkStreamingEquality(MeasureFormat.getCurrencyFormat(ULocale.ITALIAN));
    }

    @Test
    public void TestSerialFormatWidthEnum() {
        // FormatWidth enum values must map to the same ordinal values for all time in order for
        // serialization to work.
        assertEquals("FormatWidth.WIDE", 0, FormatWidth.WIDE.ordinal());
        assertEquals("FormatWidth.SHORT", 1, FormatWidth.SHORT.ordinal());
        assertEquals("FormatWidth.NARROW", 2, FormatWidth.NARROW.ordinal());
        assertEquals("FormatWidth.NUMERIC", 3, FormatWidth.NUMERIC.ordinal());
    }

    @Test
    public void testCurrencyFormatStandInForMeasureFormat() {
        MeasureFormat mf = MeasureFormat.getCurrencyFormat(ULocale.ENGLISH);
        assertEquals(
                "70 feet, 5.3 inches",
                "70 feet, 5.3 inches",
                mf.formatMeasures(
                        new Measure(70, MeasureUnit.FOOT),
                        new Measure(5.3, MeasureUnit.INCH)));
        assertEquals("getLocale", ULocale.ENGLISH, mf.getLocale());
        assertEquals("getNumberFormat", ULocale.ENGLISH, mf.getNumberFormat().getLocale(ULocale.VALID_LOCALE));
        assertEquals("getWidth", MeasureFormat.FormatWidth.WIDE, mf.getWidth());
    }

    @Test
    public void testCurrencyFormatLocale() {
        MeasureFormat mfu = MeasureFormat.getCurrencyFormat(ULocale.FRANCE);
        MeasureFormat mfj = MeasureFormat.getCurrencyFormat(Locale.FRANCE);

        assertEquals("getCurrencyFormat ULocale/Locale", mfu, mfj);
    }

    @Test
    public void testCurrencyFormatParseIsoCode() throws ParseException {
        MeasureFormat mf = MeasureFormat.getCurrencyFormat(ULocale.ENGLISH);
        CurrencyAmount result = (CurrencyAmount) mf.parseObject("GTQ 34.56");
        assertEquals("Parse should succeed", result.getNumber().doubleValue(), 34.56, 0.0);
        assertEquals("Should parse ISO code GTQ even though the currency is USD",
                "GTQ", result.getCurrency().getCurrencyCode());
    }

    @Test
    public void testDoubleZero() {
        ULocale en = new ULocale("en");
        NumberFormat nf = NumberFormat.getInstance(en);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        MeasureFormat mf = MeasureFormat.getInstance(en, FormatWidth.WIDE, nf);
        assertEquals(
                "Positive Rounding",
                "4 hours, 23 minutes, 16.00 seconds",
                mf.formatMeasures(
                        new Measure(4.7, MeasureUnit.HOUR),
                        new Measure(23, MeasureUnit.MINUTE),
                        new Measure(16, MeasureUnit.SECOND)));
        assertEquals(
                "Negative Rounding",
                "-4 hours, 23 minutes, 16.00 seconds",
                mf.formatMeasures(
                        new Measure(-4.7, MeasureUnit.HOUR),
                        new Measure(23, MeasureUnit.MINUTE),
                        new Measure(16, MeasureUnit.SECOND)));

    }

    @Test
    public void testIndividualPluralFallback() {
        // See ticket #11986 "incomplete fallback in MeasureFormat".
        // In CLDR 28, fr_CA temperature-generic/short has only the "one" form,
        // and falls back to fr for the "other" form.
        MeasureFormat mf = MeasureFormat.getInstance(new ULocale("fr_CA"), FormatWidth.SHORT);
        Measure twoDeg = new Measure(2, MeasureUnit.GENERIC_TEMPERATURE);
        assertEquals("2 deg temp in fr_CA", "2°", mf.format(twoDeg));
    }

    @Test
    public void testPopulateCache() {
        // Quick check that the lazily added additions to the MeasureUnit cache are present.
        assertTrue("MeasureUnit: unexpectedly few currencies defined", MeasureUnit.getAvailable("currency").size() > 50);
    }

    @Test
    public void testParseObject() {
        MeasureFormat mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.NARROW);
        try {
            mf.parseObject("3m", null);
            fail("MeasureFormat.parseObject(String, ParsePosition) " +
                    "should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testCLDRUnitAvailability() {
        Set<MeasureUnit> knownUnits = new HashSet<>();
        Class cMeasureUnit, cTimeUnit;
        try {
            cMeasureUnit = Class.forName("com.ibm.icu.util.MeasureUnit");
            cTimeUnit = Class.forName("com.ibm.icu.util.TimeUnit");
        } catch (ClassNotFoundException e) {
            fail("Count not load MeasureUnit or TimeUnit class: " + e.getMessage());
            return;
        }
        for (Field field : cMeasureUnit.getFields()) {
            if (field.getGenericType() == cMeasureUnit || field.getGenericType() == cTimeUnit) {
                try {
                    MeasureUnit unit = (MeasureUnit) field.get(cMeasureUnit);
                    knownUnits.add(unit);
                } catch (IllegalArgumentException e) {
                    fail(e.getMessage());
                    return;
                } catch (IllegalAccessException e) {
                    fail(e.getMessage());
                    return;
                }
            }
        }
        for (String type : MeasureUnit.getAvailableTypes()) {
            if (type.equals("currency")
                    || type.equals("compound")
                    || type.equals("coordinate")
                    || type.equals("none")) {
                continue;
            }
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                if (!knownUnits.contains(unit)) {
                    fail("Unit present in CLDR but not available via constant in MeasureUnit: " + unit);
                }
            }
        }
    }

    @Test
    public void testBug11966() {
        Locale locale = new Locale("en", "AU");
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE);
        // Should not throw an exception.
    }

    @Test
    public void test20332_PersonUnits() {
        Object[][] cases = new Object[][] {
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.NARROW, "25y"},
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.SHORT, "25 yrs"},
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.WIDE, "25 years"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.NARROW, "25m"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.SHORT, "25 mths"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.WIDE, "25 months"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.NARROW, "25w"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.SHORT, "25 wks"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.WIDE, "25 weeks"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.NARROW, "25d"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.SHORT, "25 days"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.WIDE, "25 days"}
        };
        for (Object[] cas : cases) {
            ULocale locale = (ULocale) cas[0];
            MeasureUnit unit = (MeasureUnit) cas[1];
            MeasureFormat.FormatWidth width = (MeasureFormat.FormatWidth) cas[2];
            String expected = (String) cas[3];

            MeasureFormat fmt = MeasureFormat.getInstance(locale, width);
            String result = fmt.formatMeasures(new Measure(25, unit));
            assertEquals("" + locale + " " + unit + " " + width, expected, result);
        }
    }

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static Map<MeasureUnit, Pair<MeasureUnit, MeasureUnit>> getUnitsToPerParts() {
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

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void generateCXXHConstants(String thisVersion) {
        Map<String, MeasureUnit> seen = new HashMap<>();
        System.out.println("// Start generated createXXX methods");
        System.out.println();
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
                    System.out.println("#ifndef U_HIDE_DRAFT_API");
                }
                System.out.println("    /**");
                System.out.println("     * Returns by pointer, unit of " + type + ": " + code + ".");
                System.out.println("     * Caller owns returned value and must free it.");
                System.out.printf("     * Also see {@link #get%s()}.\n", name);
                System.out.println("     * @param status ICU error code.");
                if (isDraft(javaName)) {
                    System.out.println("     * @draft ICU " + getVersion(javaName, thisVersion));
                } else {
                    System.out.println("     * @stable ICU " + getVersion(javaName, thisVersion));
                }
                System.out.println("     */");
                System.out.printf("    static MeasureUnit *create%s(UErrorCode &status);\n", name);
                System.out.println();
                System.out.println("    /**");
                System.out.println("     * Returns by value, unit of " + type + ": " + code + ".");
                System.out.printf("     * Also see {@link #create%s()}.\n", name);
                String getterVersion = getVersion(javaName, thisVersion);
                if (Integer.valueOf(getterVersion) < 64) {
                    getterVersion = "64";
                }
                if (isDraft(javaName)) {
                    System.out.println("     * @draft ICU " + getterVersion);
                } else {
                    System.out.println("     * @stable ICU " + getterVersion);
                }
                System.out.println("     */");
                System.out.printf("    static MeasureUnit get%s();\n", name);
                if (isDraft(javaName)) {
                    System.out.println("#endif /* U_HIDE_DRAFT_API */");
                }
                System.out.println("");
            }
        }
        System.out.println("// End generated createXXX methods");
    }

    private static void checkForDup(
            Map<String, MeasureUnit> seen, String name, MeasureUnit unit) {
        if (seen.containsKey(name)) {
            throw new RuntimeException("\nCollision!!" + unit + ", " + seen.get(name));
        } else {
            seen.put(name, unit);
        }
    }

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void updateJAVAVersions(String thisVersion) {
        System.out.println();
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
                    System.out.printf("        {\"%s\", \"%s\"},\n", javaName, thisVersion);
                }
            }
        }
    }

    static TreeMap<String, List<MeasureUnit>> getAllUnits() {
        TreeMap<String, List<MeasureUnit>> allUnits = new TreeMap<>();
        for (String type : MeasureUnit.getAvailableTypes()) {
            ArrayList<MeasureUnit> units = new ArrayList<>(MeasureUnit.getAvailable(type));
            Collections.sort(
                    units,
                    new Comparator<MeasureUnit>() {

                        @Override
                        public int compare(MeasureUnit o1, MeasureUnit o2) {
                            return o1.getSubtype().compareTo(o2.getSubtype());
                        }

                    });
            allUnits.put(type, units);
        }
        return allUnits;
    }

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void generateCXXConstants() {
        System.out.println("// Start generated code for measunit.cpp");
        System.out.println("");
        TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();

        // Hack: for C++, add base unit here, but ignore them when printing the create methods.
        // Also keep track of the base unit offset to make the C++ default constructor faster.
        allUnits.put("none", Arrays.asList(new MeasureUnit[] {NoUnit.BASE}));
        int baseTypeIdx = -1;
        int baseSubTypeIdx = -1;

        System.out.println("// Maps from Type ID to offset in gSubTypes.");
        System.out.println("static const int32_t gOffsets[] = {");
        int index = 0;
        int typeCount = 0;
        int currencyIndex = -1;
        for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
            System.out.printf("    %d,\n", index);
            if (entry.getKey() == "currency") {
                currencyIndex = typeCount;
            }
            typeCount++;
            index += entry.getValue().size();
        }
        assertTrue("currency present", currencyIndex >= 0);
        System.out.printf("    %d\n", index);
        System.out.println("};");
        System.out.println();
        System.out.println("static const int32_t kCurrencyOffset = " + currencyIndex + ";");
        System.out.println();
        System.out.println("// Must be sorted alphabetically.");
        System.out.println("static const char * const gTypes[] = {");
        boolean first = true;
        for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
            if (!first) {
                System.out.println(",");
            }
            System.out.print("    \"" + entry.getKey() + "\"");
            first = false;
        }
        System.out.println();
        System.out.println("};");
        System.out.println();
        System.out.println("// Must be grouped by type and sorted alphabetically within each type.");
        System.out.println("static const char * const gSubTypes[] = {");
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
                    System.out.println(",");
                }
                if (unit != null) {
                    System.out.print("    \"" + unit.getSubtype() + "\"");
                } else {
                    assertEquals("unit only null for \"none\" type", "none", entry.getKey());
                    System.out.print("    \"\"");
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
        System.out.println();
        System.out.println("};");
        System.out.println();

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
        System.out.println("// Shortcuts to the base unit in order to make the default constructor fast");
        System.out.println("static const int32_t kBaseTypeIdx = " + baseTypeIdx + ";");
        System.out.println("static const int32_t kBaseSubTypeIdx = " + baseSubTypeIdx + ";");
        System.out.println();

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
                System.out.printf("MeasureUnit *MeasureUnit::create%s(UErrorCode &status) {\n", name);
                System.out.printf("    return MeasureUnit::create(%d, %d, status);\n",
                        typeSubType.first, typeSubType.second);
                System.out.println("}");
                System.out.println();
                System.out.printf("MeasureUnit MeasureUnit::get%s() {\n", name);
                System.out.printf("    return MeasureUnit(%d, %d);\n",
                        typeSubType.first, typeSubType.second);
                System.out.println("}");
                System.out.println();
            }
        }
        System.out.println("// End generated code for measunit.cpp");
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

    static boolean isTypeHidden(String type) {
        return "currency".equals(type);
    }

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void generateBackwardCompatibilityTest(String version) {
        Map<String, MeasureUnit> seen = new HashMap<>();
        System.out.println();
        System.out.printf("    public void TestCompatible%s() {\n", version.replace(".", "_"));
        System.out.println("        MeasureUnit[] units = {");
        TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
        int count = 0;
        for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
            if (isTypeHidden(entry.getKey())) {
                continue;
            }
            for (MeasureUnit unit : entry.getValue()) {
                String javaName = toJAVAName(unit);
                checkForDup(seen, javaName, unit);
                System.out.printf("                MeasureUnit.%s,\n", javaName);
                count++;
            }
        }
        System.out.println("        };");
        System.out.printf("        assertEquals(\"\",  %d, units.length);\n", count);
        System.out.println("    }");
    }

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void generateCXXBackwardCompatibilityTest(String version) {
        System.out.println();
        Map<String, MeasureUnit> seen = new HashMap<>();
        System.out.printf("void MeasureFormatTest::TestCompatible%s() {\n", version.replace(".", "_"));
        System.out.println("    UErrorCode status = U_ZERO_ERROR;");
        System.out.println("    LocalPointer<MeasureUnit> measureUnit;");
        System.out.println("    MeasureUnit measureUnitValue;");
        TreeMap<String, List<MeasureUnit>> allUnits = getAllUnits();
        for (Map.Entry<String, List<MeasureUnit>> entry : allUnits.entrySet()) {
            if (isTypeHidden(entry.getKey())) {
                continue;
            }
            for (MeasureUnit unit : entry.getValue()) {
                String camelCase = toCamelCase(unit);
                checkForDup(seen, camelCase, unit);
                System.out.printf("    measureUnit.adoptInstead(MeasureUnit::create%s(status));\n", camelCase);
                System.out.printf("    measureUnitValue = MeasureUnit::get%s();\n", camelCase);
            }
        }
        System.out.println("    assertSuccess(\"\", status);");
        System.out.println("}");
    }

    static String toJAVAName(MeasureUnit unit) {
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

    // DO NOT DELETE THIS FUNCTION! It may appear as dead code, but we use this to generate code
    // for MeasureFormat during the release process.
    static void generateConstants(String thisVersion) {
        System.out.println("    // Start generated MeasureUnit constants");
        System.out.println();
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
                System.out.println("    /**");
                System.out.println("     * Constant for unit of " + type +
                        ": " +
                        code);
                // Special case JAVA had old constants for time from before.
                if ("duration".equals(type) && TIME_CODES.contains(code)) {
                    System.out.println("     * @stable ICU 4.0");
                }
                else if (isDraft(name)) {
                    System.out.println("     * @draft ICU " + getVersion(name, thisVersion));
                } else {
                    System.out.println("     * @stable ICU " + getVersion(name, thisVersion));
                }
                System.out.println("     */");
                if ("duration".equals(type) && TIME_CODES.contains(code)) {
                    System.out.println("    public static final TimeUnit " + name + " = (TimeUnit) MeasureUnit.internalGetInstance(\"" +
                            type +
                            "\", \"" +
                            code +
                            "\");");
                } else {
                    System.out.println("    public static final MeasureUnit " + name + " = MeasureUnit.internalGetInstance(\"" +
                            type +
                            "\", \"" +
                            code +
                            "\");");
                }
                System.out.println();
            }
        }
        System.out.println("    // End generated MeasureUnit constants");
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

    public <T extends Serializable> void checkStreamingEquality(T item) {
        try {
          ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
          objectOutputStream.writeObject(item);
          objectOutputStream.close();
          byte[] contents = byteOut.toByteArray();
          logln("bytes: " + contents.length + "; " + item.getClass() + ": " + showBytes(contents));
          ByteArrayInputStream byteIn = new ByteArrayInputStream(contents);
          ObjectInputStream objectInputStream = new ObjectInputStream(byteIn);
          Object obj = objectInputStream.readObject();
          assertEquals("Streamed Object equals ", item, obj);
        } catch (IOException e) {
          e.printStackTrace();
          assertNull("Test Serialization " + item.getClass(), e);
        } catch (ClassNotFoundException e) {
          assertNull("Test Serialization " + item.getClass(), e);
        }
      }

    /**
     * @param contents
     * @return
     */
    private String showBytes(byte[] contents) {
      StringBuilder b = new StringBuilder("[");
      for (int i = 0; i < contents.length; ++i) {
        int item = contents[i] & 0xFF;
        if (item >= 0x20 && item <= 0x7F) {
          b.append((char) item);
        } else {
          b.append('(').append(Utility.hex(item, 2)).append(')');
        }
      }
      return b.append(']').toString();
    }

    private void verifyEqualsHashCode(Object o, Object eq, Object ne) {
        assertEquals("verifyEqualsHashCodeSame", o, o);
        assertEquals("verifyEqualsHashCodeEq", o, eq);
        assertNotEquals("verifyEqualsHashCodeNe", o, ne);
        assertNotEquals("verifyEqualsHashCodeEqTrans", eq, ne);
        assertEquals("verifyEqualsHashCodeHashEq", o.hashCode(), eq.hashCode());

        // May be a flaky test, but generally should be true.
        // May need to comment this out later.
        assertNotEquals("verifyEqualsHashCodeHashNe", o.hashCode(), ne.hashCode());
    }

    public static class MeasureUnitHandler implements SerializableTestUtility.Handler
    {
        @Override
        public Object[] getTestObjects()
        {
            MeasureUnit items[] = {
                    MeasureUnit.CELSIUS,
                    Currency.getInstance("EUR")
            };
            return items;
        }
        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureUnit a1 = (MeasureUnit) a;
            MeasureUnit b1 = (MeasureUnit) b;
            return a1.getType().equals(b1.getType())
                    && a1.getSubtype().equals(b1.getSubtype());
        }
    }

    public static class MeasureFormatHandler  implements SerializableTestUtility.Handler
    {
        FormatHandler.NumberFormatHandler nfh = new FormatHandler.NumberFormatHandler();

        @Override
        public Object[] getTestObjects()
        {
            MeasureFormat items[] = {
                    MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.SHORT),
                    MeasureFormat.getInstance(
                            ULocale.FRANCE,
                            FormatWidth.WIDE,
                            NumberFormat.getIntegerInstance(ULocale.CANADA_FRENCH)),
            };
            return items;
        }
        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureFormat a1 = (MeasureFormat) a;
            MeasureFormat b1 = (MeasureFormat) b;
            boolean getLocaleEqual = a1.getLocale().equals(b1.getLocale());
            boolean getWidthEqual = a1.getWidth().equals(b1.getWidth());
            boolean numFmtHasSameBehavior = nfh.hasSameBehavior(a1.getNumberFormat(), b1.getNumberFormat());
            if (getLocaleEqual && getWidthEqual && numFmtHasSameBehavior) {
                return true;
            }
            System.out.println("MeasureFormatHandler.hasSameBehavior fails:");
            if (!getLocaleEqual) {
                System.out.println("- getLocale equality fails: old a1: " + a1.getLocale().getName() + "; test b1: " + b1.getLocale().getName());
            }
            if (!getWidthEqual) {
                System.out.println("- getWidth equality fails: old a1: " + a1.getWidth().name() + "; test b1: " + b1.getWidth().name());
            }
            if (!numFmtHasSameBehavior) {
                System.out.println("- getNumberFormat hasSameBehavior fails");
            }
            return false;
        }
    }

    @Test
    public void TestNumericTimeNonLatin() {
        ULocale ulocale = ULocale.forLanguageTag("bn");
        MeasureFormat fmt = MeasureFormat.getInstance(ulocale, FormatWidth.NUMERIC);
        String actual = fmt.formatMeasures(new Measure(12, MeasureUnit.MINUTE), new Measure(39.12345, MeasureUnit.SECOND));
        assertEquals("Incorrect digits", "১২:৩৯.১২৩", actual);
    }

    @Test
    public void TestNumericTime() {
        MeasureFormat fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("en"), FormatWidth.NUMERIC);

        Measure hours = new Measure(112, MeasureUnit.HOUR);
        Measure minutes = new Measure(113, MeasureUnit.MINUTE);
        Measure seconds = new Measure(114, MeasureUnit.SECOND);
        Measure fhours = new Measure(112.8765, MeasureUnit.HOUR);
        Measure fminutes = new Measure(113.8765, MeasureUnit.MINUTE);
        Measure fseconds = new Measure(114.8765, MeasureUnit.SECOND);

        Assert.assertEquals("112h", fmt.formatMeasures(hours));
        Assert.assertEquals("113m", fmt.formatMeasures(minutes));
        Assert.assertEquals("114s", fmt.formatMeasures(seconds));

        Assert.assertEquals("112.876h", fmt.formatMeasures(fhours));
        Assert.assertEquals("113.876m", fmt.formatMeasures(fminutes));
        Assert.assertEquals("114.876s", fmt.formatMeasures(fseconds));

        Assert.assertEquals("112:113", fmt.formatMeasures(hours, minutes));
        Assert.assertEquals("112:00:114", fmt.formatMeasures(hours, seconds));
        Assert.assertEquals("113:114", fmt.formatMeasures(minutes, seconds));

        Assert.assertEquals("112:113.876", fmt.formatMeasures(hours, fminutes));
        Assert.assertEquals("112:00:114.876", fmt.formatMeasures(hours, fseconds));
        Assert.assertEquals("113:114.876", fmt.formatMeasures(minutes, fseconds));

        Assert.assertEquals("112:113", fmt.formatMeasures(fhours, minutes));
        Assert.assertEquals("112:00:114", fmt.formatMeasures(fhours, seconds));
        Assert.assertEquals("113:114", fmt.formatMeasures(fminutes, seconds));

        Assert.assertEquals("112:113.876", fmt.formatMeasures(fhours, fminutes));
        Assert.assertEquals("112:00:114.876", fmt.formatMeasures(fhours, fseconds));
        Assert.assertEquals("113:114.876", fmt.formatMeasures(fminutes, fseconds));

        Assert.assertEquals("112:113:114", fmt.formatMeasures(hours, minutes, seconds));
        Assert.assertEquals("112:113:114.876", fmt.formatMeasures(fhours, fminutes, fseconds));
    }

    @Test
    public void TestNumericTimeSomeSpecialFormats() {
        Measure fhours = new Measure(2.8765432, MeasureUnit.HOUR);
        Measure fminutes = new Measure(3.8765432, MeasureUnit.MINUTE);

        // Latvian is one of the very few locales 0-padding the hour
        MeasureFormat fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("lt"), FormatWidth.NUMERIC);
        Assert.assertEquals("02:03,877", fmt.formatMeasures(fhours, fminutes));

        // Danish is one of the very few locales using '.' as separator
        fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("da"), FormatWidth.NUMERIC);
        Assert.assertEquals("2.03,877", fmt.formatMeasures(fhours, fminutes));
    }

    @Test
    public void TestIdentifiers() {
        class TestCase {
            final String id;
            final String normalized;

            TestCase(String id, String normalized) {
                this.id = id;
                this.normalized = normalized;
            }
        }

        TestCase cases[] = {
            // Correctly normalized identifiers should not change
            new TestCase("square-meter-per-square-meter", "square-meter-per-square-meter"),
            new TestCase("kilogram-meter-per-square-meter-square-second",
                         "kilogram-meter-per-square-meter-square-second"),
            new TestCase("square-mile-and-square-foot", "square-mile-and-square-foot"),
            new TestCase("square-foot-and-square-mile", "square-foot-and-square-mile"),
            new TestCase("per-cubic-centimeter", "per-cubic-centimeter"),
            new TestCase("per-kilometer", "per-kilometer"),

            // Normalization of power and per
            new TestCase("pow2-foot-and-pow2-mile", "square-foot-and-square-mile"),
            new TestCase("gram-square-gram-per-dekagram", "cubic-gram-per-dekagram"),
            new TestCase("kilogram-per-meter-per-second", "kilogram-per-meter-second"),
            new TestCase("kilometer-per-second-per-megaparsec", "kilometer-per-megaparsec-second"),

            // Correct order of units, as per unitQuantities in CLDR's units.xml
            new TestCase("newton-meter", "newton-meter"),
            new TestCase("meter-newton", "newton-meter"),
            new TestCase("pound-force-foot", "pound-force-foot"),
            new TestCase("foot-pound-force", "pound-force-foot"),
            new TestCase("kilowatt-hour", "kilowatt-hour"),
            new TestCase("hour-kilowatt", "kilowatt-hour"),

            // Testing prefixes are parsed and produced correctly (ensures no
            // collisions in the enum values)
            new TestCase("yoctofoot", "yoctofoot"),
            new TestCase("zeptofoot", "zeptofoot"),
            new TestCase("attofoot", "attofoot"),
            new TestCase("femtofoot", "femtofoot"),
            new TestCase("picofoot", "picofoot"),
            new TestCase("nanofoot", "nanofoot"),
            new TestCase("microfoot", "microfoot"),
            new TestCase("millifoot", "millifoot"),
            new TestCase("centifoot", "centifoot"),
            new TestCase("decifoot", "decifoot"),
            new TestCase("foot", "foot"),
            new TestCase("dekafoot", "dekafoot"),
            new TestCase("hectofoot", "hectofoot"),
            new TestCase("kilofoot", "kilofoot"),
            new TestCase("megafoot", "megafoot"),
            new TestCase("gigafoot", "gigafoot"),
            new TestCase("terafoot", "terafoot"),
            new TestCase("petafoot", "petafoot"),
            new TestCase("exafoot", "exafoot"),
            new TestCase("zettafoot", "zettafoot"),
            new TestCase("yottafoot", "yottafoot"),
            new TestCase("kibibyte", "kibibyte"),
            new TestCase("mebibyte", "mebibyte"),
            new TestCase("gibibyte", "gibibyte"),
            new TestCase("tebibyte", "tebibyte"),
            new TestCase("pebibyte", "pebibyte"),
            new TestCase("exbibyte", "exbibyte"),
            new TestCase("zebibyte", "zebibyte"),
            new TestCase("yobibyte", "yobibyte"),

            // Testing aliases
            new TestCase("foodcalorie", "foodcalorie"),
            new TestCase("dot-per-centimeter", "dot-per-centimeter"),
            new TestCase("dot-per-inch", "dot-per-inch"),
            new TestCase("dot", "dot"),

            // Testing sort order of prefixes.
            new TestCase("megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("per-megafoot-mebifoot-kibifoot-kilofoot", "per-mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("megafoot-mebifoot-kibifoot-kilofoot-per-megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot-per-mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("microfoot-millifoot-megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot-millifoot-microfoot"),
            new TestCase("per-microfoot-millifoot-megafoot-mebifoot-kibifoot-kilofoot", "per-mebifoot-megafoot-kibifoot-kilofoot-millifoot-microfoot"),
        };

        for (TestCase testCase : cases) {
            MeasureUnit unit = MeasureUnit.forIdentifier(testCase.id);

            final String actual = unit.getIdentifier();
            assertEquals(testCase.id, testCase.normalized, actual);
        }

        assertEquals("for empty identifiers, the MeasureUnit will be null",
                null, MeasureUnit.forIdentifier(""));
    }

    @Test
    public void TestInvalidIdentifiers() {
        final String inputs[] = {
                "kilo",
                "kilokilo",
                "onekilo",
                "meterkilo",
                "meter-kilo",
                "k",
                "meter-",
                "meter+",
                "-meter",
                "+meter",
                "-kilometer",
                "+kilometer",
                "-pow2-meter",
                "+pow2-meter",
                "p2-meter",
                "p4-meter",
                "+",
                "-",
                "-mile",
                "-and-mile",
                "-per-mile",
                "one",
                "one-one",
                "one-per-mile",
                "one-per-cubic-centimeter",
                "square--per-meter",
                "metersecond", // Must have compound part in between single units

                // Negative powers not supported in mixed units yet. TODO(CLDR-13701).
                "per-hour-and-hertz",
                "hertz-and-per-hour",

                // Compound units not supported in mixed units yet. TODO(CLDR-13700).
                "kilonewton-meter-and-newton-meter",
        };

        for (String input : inputs) {
            try {
                MeasureUnit.forIdentifier(input);
                Assert.fail("An IllegalArgumentException must be thrown");
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
    }

    @Test
    public void TestIdentifierDetails() {
        MeasureUnit joule = MeasureUnit.forIdentifier("joule");
        assertEquals("Initial joule", "joule", joule.getIdentifier());

        // "Invalid prefix" test not needed: in Java we cannot pass a
        // non-existent enum instance. (In C++ an int can be typecast.)

        MeasureUnit unit = joule.withPrefix(MeasureUnit.MeasurePrefix.HECTO);
        assertEquals("Joule with hecto prefix", "hectojoule", unit.getIdentifier());

        unit = unit.withPrefix(MeasureUnit.MeasurePrefix.EXBI);
        assertEquals("Joule with exbi prefix", "exbijoule", unit.getIdentifier());
    }

    @Test
    public void TestPrefixes() {
        class TestCase {
            final MeasureUnit.MeasurePrefix prefix;
            final int expectedBase;
            final int expectedPower;

            TestCase(MeasureUnit.MeasurePrefix prefix, int expectedBase, int expectedPower) {
                this.prefix = prefix;
                this.expectedBase = expectedBase;
                this.expectedPower = expectedPower;
            }
        }

        TestCase cases[] = {
            new TestCase(MeasureUnit.MeasurePrefix.YOCTO, 10, -24),
            new TestCase(MeasureUnit.MeasurePrefix.ZEPTO, 10, -21),
            new TestCase(MeasureUnit.MeasurePrefix.ATTO, 10, -18),
            new TestCase(MeasureUnit.MeasurePrefix.FEMTO, 10, -15),
            new TestCase(MeasureUnit.MeasurePrefix.PICO, 10, -12),
            new TestCase(MeasureUnit.MeasurePrefix.NANO, 10, -9),
            new TestCase(MeasureUnit.MeasurePrefix.MICRO, 10, -6),
            new TestCase(MeasureUnit.MeasurePrefix.MILLI, 10, -3),
            new TestCase(MeasureUnit.MeasurePrefix.CENTI, 10, -2),
            new TestCase(MeasureUnit.MeasurePrefix.DECI, 10, -1),
            new TestCase(MeasureUnit.MeasurePrefix.ONE, 10, 0),
            new TestCase(MeasureUnit.MeasurePrefix.DEKA, 10, 1),
            new TestCase(MeasureUnit.MeasurePrefix.HECTO, 10, 2),
            new TestCase(MeasureUnit.MeasurePrefix.KILO, 10, 3),
            new TestCase(MeasureUnit.MeasurePrefix.MEGA, 10, 6),
            new TestCase(MeasureUnit.MeasurePrefix.GIGA, 10, 9),
            new TestCase(MeasureUnit.MeasurePrefix.TERA, 10, 12),
            new TestCase(MeasureUnit.MeasurePrefix.PETA, 10, 15),
            new TestCase(MeasureUnit.MeasurePrefix.EXA, 10, 18),
            new TestCase(MeasureUnit.MeasurePrefix.ZETTA, 10, 21),
            new TestCase(MeasureUnit.MeasurePrefix.YOTTA, 10, 24),
            new TestCase(MeasureUnit.MeasurePrefix.KIBI, 1024, 1),
            new TestCase(MeasureUnit.MeasurePrefix.MEBI, 1024, 2),
            new TestCase(MeasureUnit.MeasurePrefix.GIBI, 1024, 3),
            new TestCase(MeasureUnit.MeasurePrefix.TEBI, 1024, 4),
            new TestCase(MeasureUnit.MeasurePrefix.PEBI, 1024, 5),
            new TestCase(MeasureUnit.MeasurePrefix.EXBI, 1024, 6),
            new TestCase(MeasureUnit.MeasurePrefix.ZEBI, 1024, 7),
            new TestCase(MeasureUnit.MeasurePrefix.YOBI, 1024, 8),
        };

        for (TestCase testCase : cases) {
            MeasureUnit m = MeasureUnit.AMPERE.withPrefix(testCase.prefix);
            assertEquals("getPrefixPower()", testCase.expectedPower, m.getPrefix().getPower());
            assertEquals("getPrefixBase()", testCase.expectedBase, m.getPrefix().getBase());
        }
    }

    @Test
    public void TestParseBuiltIns() {
        for (MeasureUnit unit : MeasureUnit.getAvailable()) {
            System.out.println("unit ident: " + unit.getIdentifier() + ", type: " + unit.getType());
            if (unit.getType() == "currency") {
                continue;
            }

            // Prove that all built-in units are parseable, except "generic" temperature:
            if (unit == MeasureUnit.GENERIC_TEMPERATURE) {
                try {
                    MeasureUnit.forIdentifier(unit.getIdentifier());
                    Assert.fail("GENERIC_TEMPERATURE should not be parseable");
                } catch (IllegalArgumentException e) {
                    continue;
                }
            } else {
                MeasureUnit parsed = MeasureUnit.forIdentifier(unit.getIdentifier());
                assertTrue("parsed MeasureUnit '" + parsed + "'' should equal built-in '" + unit + "'",
                           unit.equals(parsed));
            }
        }
    }

    @Test
    public void TestParseToBuiltIn() {
        class TestCase {
            final String identifier;
            MeasureUnit expectedBuiltin;

            TestCase(String identifier, MeasureUnit expectedBuiltin) {
                this.identifier = identifier;
                this.expectedBuiltin = expectedBuiltin;
            }
        }

        TestCase cases[] = {
            new TestCase("meter-per-second-per-second", MeasureUnit.METER_PER_SECOND_SQUARED),
            new TestCase("meter-per-second-second", MeasureUnit.METER_PER_SECOND_SQUARED),
            new TestCase("centimeter-centimeter", MeasureUnit.SQUARE_CENTIMETER),
            new TestCase("square-foot", MeasureUnit.SQUARE_FOOT),
            new TestCase("pow2-inch", MeasureUnit.SQUARE_INCH),
            new TestCase("milligram-per-deciliter", MeasureUnit.MILLIGRAM_PER_DECILITER),
            new TestCase("pound-force-per-pow2-inch", MeasureUnit.POUND_PER_SQUARE_INCH),
            new TestCase("yard-pow2-yard", MeasureUnit.CUBIC_YARD),
            new TestCase("square-yard-yard", MeasureUnit.CUBIC_YARD),
        };

        for (TestCase testCase : cases) {
            MeasureUnit m = MeasureUnit.forIdentifier(testCase.identifier);
            assertTrue(testCase.identifier + " parsed to builtin", m.equals(testCase.expectedBuiltin));
        }
    }

    @Test
    public void TestCompoundUnitOperations() {
        MeasureUnit.forIdentifier("kilometer-per-second-joule");

        MeasureUnit kilometer = MeasureUnit.KILOMETER;
        MeasureUnit cubicMeter = MeasureUnit.CUBIC_METER;
        MeasureUnit meter = kilometer.withPrefix(MeasureUnit.MeasurePrefix.ONE);
        MeasureUnit centimeter1 = kilometer.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
        MeasureUnit centimeter2 = meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
        MeasureUnit cubicDecimeter = cubicMeter.withPrefix(MeasureUnit.MeasurePrefix.DECI);

        verifySingleUnit(kilometer, MeasureUnit.MeasurePrefix.KILO, 1, "kilometer");
        verifySingleUnit(meter, MeasureUnit.MeasurePrefix.ONE, 1, "meter");
        verifySingleUnit(centimeter1, MeasureUnit.MeasurePrefix.CENTI, 1, "centimeter");
        verifySingleUnit(centimeter2, MeasureUnit.MeasurePrefix.CENTI, 1, "centimeter");
        verifySingleUnit(cubicDecimeter, MeasureUnit.MeasurePrefix.DECI, 3, "cubic-decimeter");

        assertTrue("centimeter equality", centimeter1.equals( centimeter2));
        assertTrue("kilometer inequality", !centimeter1.equals( kilometer));

        MeasureUnit squareMeter = meter.withDimensionality(2);
        MeasureUnit overCubicCentimeter = centimeter1.withDimensionality(-3);
        MeasureUnit quarticKilometer = kilometer.withDimensionality(4);
        MeasureUnit overQuarticKilometer1 = kilometer.withDimensionality(-4);

        verifySingleUnit(squareMeter, MeasureUnit.MeasurePrefix.ONE, 2, "square-meter");
        verifySingleUnit(overCubicCentimeter, MeasureUnit.MeasurePrefix.CENTI, -3, "per-cubic-centimeter");
        verifySingleUnit(quarticKilometer, MeasureUnit.MeasurePrefix.KILO, 4, "pow4-kilometer");
        verifySingleUnit(overQuarticKilometer1, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");

        assertTrue("power inequality", quarticKilometer != overQuarticKilometer1);

        MeasureUnit overQuarticKilometer2 = quarticKilometer.reciprocal();
        MeasureUnit overQuarticKilometer3 = kilometer.product(kilometer)
                .product(kilometer)
                .product(kilometer)
                .reciprocal();
        MeasureUnit overQuarticKilometer4 = meter.withDimensionality(4)
                .reciprocal()
                .withPrefix(MeasureUnit.MeasurePrefix.KILO);

        verifySingleUnit(overQuarticKilometer2, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");
        verifySingleUnit(overQuarticKilometer3, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");
        verifySingleUnit(overQuarticKilometer4, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");

        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer2));
        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer3));
        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer4));

        MeasureUnit kiloSquareSecond = MeasureUnit.SECOND
                .withDimensionality(2).withPrefix(MeasureUnit.MeasurePrefix.KILO);
        MeasureUnit meterSecond = meter.product(kiloSquareSecond);
        MeasureUnit cubicMeterSecond1 = meter.withDimensionality(3).product(kiloSquareSecond);
        MeasureUnit centimeterSecond1 = meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI).product(kiloSquareSecond);
        MeasureUnit secondCubicMeter = kiloSquareSecond.product(meter.withDimensionality(3));
        MeasureUnit secondCentimeter = kiloSquareSecond.product(meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI));
        MeasureUnit secondCentimeterPerKilometer = secondCentimeter.product(kilometer.reciprocal());

        verifySingleUnit(kiloSquareSecond, MeasureUnit.MeasurePrefix.KILO, 2, "square-kilosecond");
        String meterSecondSub[] = {
                "meter", "square-kilosecond"
        };
        verifyCompoundUnit(meterSecond, "meter-square-kilosecond",
                meterSecondSub, meterSecondSub.length);
        String cubicMeterSecond1Sub[] = {
                "cubic-meter", "square-kilosecond"
        };
        verifyCompoundUnit(cubicMeterSecond1, "cubic-meter-square-kilosecond",
                cubicMeterSecond1Sub, cubicMeterSecond1Sub.length);
        String centimeterSecond1Sub[] = {
                "centimeter", "square-kilosecond"
        };
        verifyCompoundUnit(centimeterSecond1, "centimeter-square-kilosecond",
                centimeterSecond1Sub, centimeterSecond1Sub.length);
        String secondCubicMeterSub[] = {
                "cubic-meter", "square-kilosecond"
        };
        verifyCompoundUnit(secondCubicMeter, "cubic-meter-square-kilosecond",
                secondCubicMeterSub, secondCubicMeterSub.length);
        String secondCentimeterSub[] = {
                "centimeter", "square-kilosecond"
        };
        verifyCompoundUnit(secondCentimeter, "centimeter-square-kilosecond",
                secondCentimeterSub, secondCentimeterSub.length);
        String secondCentimeterPerKilometerSub[] = {
                "centimeter", "square-kilosecond", "per-kilometer"
        };
        verifyCompoundUnit(secondCentimeterPerKilometer, "centimeter-square-kilosecond-per-kilometer",
                secondCentimeterPerKilometerSub, secondCentimeterPerKilometerSub.length);

        assertTrue("reordering equality", cubicMeterSecond1.equals(secondCubicMeter));
        assertTrue("additional simple units inequality", !secondCubicMeter.equals(secondCentimeter));

        // Don't allow get/set power or SI or binary prefix on compound units
        try {
            meterSecond.getDimensionality();
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.withDimensionality(3);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.getPrefix();
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        MeasureUnit footInch = MeasureUnit.forIdentifier("foot-and-inch");
        MeasureUnit inchFoot = MeasureUnit.forIdentifier("inch-and-foot");

        String footInchSub[] = {
                "foot", "inch"
        };
        verifyMixedUnit(footInch, "foot-and-inch",
                footInchSub, footInchSub.length);
        String inchFootSub[] = {
                "inch", "foot"
        };
        verifyMixedUnit(inchFoot, "inch-and-foot",
                inchFootSub, inchFootSub.length);

        assertTrue("order matters inequality", !footInch.equals(inchFoot));


        MeasureUnit dimensionless  = NoUnit.BASE;
        MeasureUnit dimensionless2 = MeasureUnit.forIdentifier("");
        assertEquals("dimensionless equality", dimensionless, dimensionless2);

        // We support starting from an "identity" MeasureUnit and then combining it
        // with others via product:
        MeasureUnit kilometer2 = kilometer.product(dimensionless);

        verifySingleUnit(kilometer2, MeasureUnit.MeasurePrefix.KILO, 1, "kilometer");
        assertTrue("kilometer equality", kilometer.equals(kilometer2));

        // Test out-of-range powers
        MeasureUnit power15 = MeasureUnit.forIdentifier("pow15-kilometer");
        verifySingleUnit(power15, MeasureUnit.MeasurePrefix.KILO, 15, "pow15-kilometer");

        try {
            MeasureUnit.forIdentifier("pow16-kilometer");
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        try {
            power15.product(kilometer);
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        MeasureUnit powerN15 = MeasureUnit.forIdentifier("per-pow15-kilometer");
        verifySingleUnit(powerN15, MeasureUnit.MeasurePrefix.KILO, -15, "per-pow15-kilometer");

        try {
            MeasureUnit.forIdentifier("per-pow16-kilometer");
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        try {
            powerN15.product(overQuarticKilometer1);
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }
    }

    @Test
    public void TestDimensionlessBehaviour() {
        MeasureUnit dimensionless = MeasureUnit.forIdentifier("");
        MeasureUnit dimensionless2 = NoUnit.BASE;
        MeasureUnit dimensionless3 = null;
        MeasureUnit dimensionless4 = MeasureUnit.forIdentifier(null);

        assertEquals("dimensionless must be equals", dimensionless, dimensionless2);
        assertEquals("dimensionless must be equals", dimensionless2, dimensionless3);
        assertEquals("dimensionless must be equals", dimensionless3, dimensionless4);

        // product(dimensionless)
        MeasureUnit mile = MeasureUnit.MILE;
        mile = mile.product(dimensionless);
        verifySingleUnit(mile, MeasureUnit.MeasurePrefix.ONE, 1, "mile");
    }

    private void verifySingleUnit(MeasureUnit singleMeasureUnit, MeasureUnit.MeasurePrefix prefix, int power, String identifier) {
        assertEquals(identifier + ": SI or binary prefix", prefix, singleMeasureUnit.getPrefix());

        assertEquals(identifier + ": Power", power, singleMeasureUnit.getDimensionality());

        assertEquals(identifier + ": Identifier", identifier, singleMeasureUnit.getIdentifier());

        assertTrue(identifier + ": Constructor", singleMeasureUnit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity", MeasureUnit.Complexity.SINGLE, singleMeasureUnit.getComplexity());
    }


    // Kilogram is a "base unit", although it's also "gram" with a kilo- prefix.
    // This tests that it is handled in the preferred manner.
    @Test
    public void TestKilogramIdentifier() {
        // SI unit of mass
        MeasureUnit kilogram = MeasureUnit.forIdentifier("kilogram");
        // Metric mass unit
        MeasureUnit gram = MeasureUnit.forIdentifier("gram");
        // Microgram: still a built-in type
        MeasureUnit microgram = MeasureUnit.forIdentifier("microgram");
        // Nanogram: not a built-in type at this time
        MeasureUnit nanogram = MeasureUnit.forIdentifier("nanogram");

        assertEquals("parsed kilogram equals built-in kilogram", MeasureUnit.KILOGRAM.getType(),
                kilogram.getType());
        assertEquals("parsed kilogram equals built-in kilogram", MeasureUnit.KILOGRAM.getSubtype(),
                kilogram.getSubtype());
        assertEquals("parsed gram equals built-in gram", MeasureUnit.GRAM.getType(), gram.getType());
        assertEquals("parsed gram equals built-in gram", MeasureUnit.GRAM.getSubtype(),
                gram.getSubtype());
        assertEquals("parsed microgram equals built-in microgram", MeasureUnit.MICROGRAM.getType(),
                microgram.getType());
        assertEquals("parsed microgram equals built-in microgram", MeasureUnit.MICROGRAM.getSubtype(),
                microgram.getSubtype());
        assertEquals("nanogram", null, nanogram.getType());
        assertEquals("nanogram", "nanogram", nanogram.getIdentifier());

        assertEquals("prefix of kilogram", MeasureUnit.MeasurePrefix.KILO, kilogram.getPrefix());
        assertEquals("prefix of gram", MeasureUnit.MeasurePrefix.ONE, gram.getPrefix());
        assertEquals("prefix of microgram", MeasureUnit.MeasurePrefix.MICRO, microgram.getPrefix());
        assertEquals("prefix of nanogram", MeasureUnit.MeasurePrefix.NANO, nanogram.getPrefix());

        MeasureUnit tmp = kilogram.withPrefix(MeasureUnit.MeasurePrefix.MILLI);
        assertEquals("Kilogram + milli should be milligram, got: " + tmp.getIdentifier(),
                MeasureUnit.MILLIGRAM.getIdentifier(), tmp.getIdentifier());
    }

    @Test
    public void TestInternalMeasureUnitImpl() {
        MeasureUnitImpl mu1 = MeasureUnitImpl.forIdentifier("meter");
        assertEquals("mu1 initial identifier", null, mu1.getIdentifier());
        assertEquals("mu1 initial complexity", MeasureUnit.Complexity.SINGLE, mu1.getComplexity());
        assertEquals("mu1 initial units length", 1, mu1.getSingleUnits().size());
        assertEquals("mu1 initial units[0]", "meter", mu1.getSingleUnits().get(0).getSimpleUnitID());

        // Producing identifier via build(): the MeasureUnitImpl instance gets modified
        // while it also gets assigned to tmp's internal measureUnitImpl.
        MeasureUnit tmp = mu1.build();
        assertEquals("mu1 post-build identifier", "meter", mu1.getIdentifier());
        assertEquals("mu1 post-build complexity", MeasureUnit.Complexity.SINGLE, mu1.getComplexity());
        assertEquals("mu1 post-build units length", 1, mu1.getSingleUnits().size());
        assertEquals("mu1 post-build units[0]", "meter", mu1.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("MeasureUnit tmp identifier", "meter", tmp.getIdentifier());

        mu1 = MeasureUnitImpl.forIdentifier("hour-and-minute-and-second");
        assertEquals("mu1 = HMS: identifier", null, mu1.getIdentifier());
        assertEquals("mu1 = HMS: complexity", MeasureUnit.Complexity.MIXED, mu1.getComplexity());
        assertEquals("mu1 = HMS: units length", 3, mu1.getSingleUnits().size());
        assertEquals("mu1 = HMS: units[0]", "hour", mu1.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("mu1 = HMS: units[1]", "minute", mu1.getSingleUnits().get(1).getSimpleUnitID());
        assertEquals("mu1 = HMS: units[2]", "second", mu1.getSingleUnits().get(2).getSimpleUnitID());

        MeasureUnitImpl m2 = MeasureUnitImpl.forIdentifier("meter");
        m2.appendSingleUnit(MeasureUnit.METER.getCopyOfMeasureUnitImpl().getSingleUnitImpl());
        assertEquals("append meter twice: complexity", MeasureUnit.Complexity.SINGLE, m2.getComplexity());
        assertEquals("append meter twice: units length", 1, m2.getSingleUnits().size());
        assertEquals("append meter twice: units[0]", "meter", m2.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("append meter twice: identifier", "square-meter", m2.build().getIdentifier());

        MeasureUnitImpl mcm = MeasureUnitImpl.forIdentifier("meter");
        mcm.appendSingleUnit(MeasureUnit.CENTIMETER.getCopyOfMeasureUnitImpl().getSingleUnitImpl());
        assertEquals("append meter & centimeter: complexity", MeasureUnit.Complexity.COMPOUND, mcm.getComplexity());
        assertEquals("append meter & centimeter: units length", 2, mcm.getSingleUnits().size());
        assertEquals("append meter & centimeter: units[0]", "meter", mcm.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("append meter & centimeter: units[1]", "meter", mcm.getSingleUnits().get(1).getSimpleUnitID());
        assertEquals("append meter & centimeter: identifier", "meter-centimeter", mcm.build().getIdentifier());

        MeasureUnitImpl m2m = MeasureUnitImpl.forIdentifier("meter-square-meter");
        assertEquals("meter-square-meter: complexity", MeasureUnit.Complexity.SINGLE, m2m.getComplexity());
        assertEquals("meter-square-meter: units length", 1, m2m.getSingleUnits().size());
        assertEquals("meter-square-meter: units[0]", "meter", m2m.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("meter-square-meter: identifier", "cubic-meter", m2m.build().getIdentifier());

    }

    private void verifyCompoundUnit(
            MeasureUnit unit,
            String identifier,
            String subIdentifiers[],
            int subIdentifierCount) {
        assertEquals(identifier + ": Identifier",
                identifier,
                unit.getIdentifier());

        assertTrue(identifier + ": Constructor",
                unit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity",
                MeasureUnit.Complexity.COMPOUND,
                unit.getComplexity());

        List<MeasureUnit> subUnits = unit.splitToSingleUnits();
        assertEquals(identifier + ": Length", subIdentifierCount, subUnits.size());
        for (int i = 0; ; i++) {
            if (i >= subIdentifierCount || i >= subUnits.size()) break;
            assertEquals(identifier + ": Sub-unit #" + i,
                    subIdentifiers[i],
                    subUnits.get(i).getIdentifier());
            assertEquals(identifier + ": Sub-unit Complexity",
                    MeasureUnit.Complexity.SINGLE,
                    subUnits.get(i).getComplexity());
        }
    }

    private void verifyMixedUnit(
            MeasureUnit unit,
            String identifier,
            String subIdentifiers[],
            int subIdentifierCount) {
        assertEquals(identifier + ": Identifier",
                identifier,
                unit.getIdentifier());
        assertTrue(identifier + ": Constructor",
                unit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity",
                MeasureUnit.Complexity.MIXED,
                unit.getComplexity());

        List<MeasureUnit> subUnits = unit.splitToSingleUnits();
        assertEquals(identifier + ": Length", subIdentifierCount, subUnits.size());
        for (int i = 0; ; i++) {
            if (i >= subIdentifierCount || i >= subUnits.size()) break;
            assertEquals(identifier + ": Sub-unit #" + i,
                    subIdentifiers[i],
                    subUnits.get(i).getIdentifier());
        }
    }
}
