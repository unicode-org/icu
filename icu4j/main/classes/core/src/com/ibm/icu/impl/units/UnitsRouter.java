/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * `UnitsRouter` responsible for converting from a single unit (such as `meter` or `meter-per-second`) to
 * one of the complex units based on the limits.
 * For example:
 * if the input is `meter` and the output as following
 * {`foot+inch`, limit: 3.0}
 * {`inch`     , limit: no value (-inf)}
 * Thus means if the input in `meter` is greater than or equal to `3.0 feet`, the output will be in
 * `foot+inch`, otherwise, the output will be in `inch`.
 * <p>
 * NOTE:
 * the output units and the their limits MUST BE in order, for example, if the output units, from the
 * previous example, are the following:
 * {`inch`     , limit: no value (-inf)}
 * {`foot+inch`, limit: 3.0}
 * IN THIS CASE THE OUTPUT WILL BE ALWAYS IN `inch`.
 * <p>
 * NOTE:
 * the output units  and their limits will be extracted from the units preferences database by knowing
 * the followings:
 * - input unit
 * - locale
 * - usage
 * <p>
 * DESIGN:
 * `UnitRouter` uses internally `ComplexUnitConverter` in order to convert the input units to the
 * desired complex units and to check the limit too.
 */
public class UnitsRouter {
    public class RouteResult {
        public ArrayList<Measure> measures;
        public String precision;
        public ArrayList<Pair<MeasureUnitImpl, BigDecimal>> tempResults; // TODO: remove this after implementing build function.

        RouteResult(Pair<ArrayList<Measure>, ArrayList<Pair<MeasureUnitImpl, BigDecimal>>> results, String precision) {
            this.measures = results.first;
            ;
            this.precision = precision;
            this.tempResults = results.second;
        }
    }

    public UnitsRouter(MeasureUnitImpl inputUnitImpl, String region, String usage) {
        // TODO: do we want to pass in ConversionRates and UnitPreferences instead?
        // of loading in each UnitsRouter instance? (Or make global?)
        UnitsData data = new UnitsData();

        //MeasureUnitImpl inputUnitImpl = MeasureUnitImpl.forMeasureUnitMaybeCopy(inputUnit);
        String category = data.getCategory(inputUnitImpl);
        UnitPreferences.UnitPreference[] unitPreferences = data.getPreferencesFor(category,  usage, region);

        for (int i = 0; i < unitPreferences.length; ++i) {
            UnitPreferences.UnitPreference preference = unitPreferences[i];

            MeasureUnitImpl complexTargetUnitImpl =
                    MeasureUnitImpl.UnitsParser.parseForIdentifier(preference.getUnit());

            String precision = preference.getSkeleton();

            // For now, we only have "precision-increment" in Units Preferences skeleton.
            // Therefore, we check if the skeleton starts with "precision-increment" and force the program to
            // fail otherwise.
            // NOTE:
            //  It is allowed to have an empty precision.
            if (!precision.isEmpty() && !precision.startsWith("precision-increment")) {
                Assert.fail("Only `precision-increment` is allowed");
            }

            outputUnits_.add(complexTargetUnitImpl.build());
            converterPreferences_.add(new ConverterPreference(inputUnitImpl, complexTargetUnitImpl,
                    preference.getGeq(), precision,
                    data.getConversionRates()));
        }
    }

    public RouteResult route(BigDecimal quantity) {
        for (ConverterPreference converterPreference :
                converterPreferences_) {
            if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
                return new RouteResult(converterPreference.converter.convert(quantity), converterPreference.precision);
            }
        }

        // In case of the `quantity` does not fit in any converter limit, use the last converter.
        ConverterPreference lastConverterPreference = converterPreferences_.get(converterPreferences_.size() - 1);
        return new RouteResult(lastConverterPreference.converter.convert(quantity), lastConverterPreference.precision);
    }

    /**
     * Returns the list of possible output units, i.e. the full set of
     * preferences, for the localized, usage-specific unit preferences.
     * <p>
     * The returned pointer should be valid for the lifetime of the
     * UnitsRouter instance.
     */
    public ArrayList<MeasureUnit> getOutputUnits() {
        return this.outputUnits_;
    }


    // List of possible output units. TODO: converterPreferences_ now also has
    // this data available. Maybe drop outputUnits_ and have getOutputUnits
    // construct a the list from data in converterPreferences_ instead?
    private ArrayList<MeasureUnit> outputUnits_ = new ArrayList<>();
    private ArrayList<ConverterPreference> converterPreferences_ = new ArrayList<>();

    /**
     * Contains the complex unit converter and the limit which representing the smallest value that the
     * converter should accept. For example, if the converter is converting to `foot+inch` and the limit
     * equals 3.0, thus means the converter should not convert to a value less than `3.0 feet`.
     *
     * NOTE:
     *    if the limit doest not has a value `i.e. (std::numeric_limits<double>::lowest())`, this mean there
     *    is no limit for the converter.
     */
    public static class ConverterPreference {
        ComplexUnitsConverter converter;
        BigDecimal limit;
        String precision;

        // In case there is no limit, the limit will be -inf.
        public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl outputUnits,
                                   String precision, ConversionRates conversionRates) {
            this(source, outputUnits, BigDecimal.valueOf(Double.MIN_VALUE), precision,
                    conversionRates);
        }

        public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl outputUnits,
                                   BigDecimal limit, String precision, ConversionRates conversionRates) {
            this.converter = new ComplexUnitsConverter(source, outputUnits, conversionRates);
            this.limit = limit;
            this.precision = precision;
        }
    }
}


