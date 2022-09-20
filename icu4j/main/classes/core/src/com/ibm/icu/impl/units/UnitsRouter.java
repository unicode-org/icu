// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

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
 * the output units and their limits MUST BE in order, for example, if the output units, from the
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
    // List of possible output units. TODO: converterPreferences_ now also has
    // this data available. Maybe drop outputUnits_ and have getOutputUnits
    // construct a the list from data in converterPreferences_ instead?
    private ArrayList<MeasureUnit> outputUnits_ = new ArrayList<>();
    private ArrayList<ConverterPreference> converterPreferences_ = new ArrayList<>();

    public UnitsRouter(String inputUnitIdentifier, ULocale locale, String usage) {
        this(MeasureUnitImpl.forIdentifier(inputUnitIdentifier), locale, usage);
    }

    public UnitsRouter(MeasureUnitImpl inputUnit, ULocale locale, String usage) {
        // TODO: do we want to pass in ConversionRates and UnitPreferences instead?
        // of loading in each UnitsRouter instance? (Or make global?)
        UnitsData data = new UnitsData();

        String category = data.getCategory(inputUnit);
        UnitPreferences.UnitPreference[] unitPreferences = data.getPreferencesFor(category, usage, locale);

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
                throw new AssertionError("Only `precision-increment` is allowed");
            }

            outputUnits_.add(complexTargetUnitImpl.build());
            converterPreferences_.add(new ConverterPreference(inputUnit, complexTargetUnitImpl,
                    preference.getGeq(), precision,
                    data.getConversionRates()));
        }
    }

    /** If micros.rounder is a BogusRounder, this function replaces it with a valid one. */
    public RouteResult route(BigDecimal quantity, MicroProps micros) {
        Precision rounder = micros == null ? null : micros.rounder;
        ConverterPreference converterPreference = null;
        for (ConverterPreference itr : converterPreferences_) {
            converterPreference = itr;
            if (converterPreference.converter.greaterThanOrEqual(quantity.abs(),
                                                                 converterPreference.limit)) {
                break;
            }
        }
        assert converterPreference != null;
        assert converterPreference.precision != null;

        // Set up the rounder for this preference's precision
        if (rounder != null && rounder instanceof Precision.BogusRounder) {
            Precision.BogusRounder bogus = (Precision.BogusRounder)rounder;
            if (converterPreference.precision.length() > 0) {
                rounder = bogus.into(parseSkeletonToPrecision(converterPreference.precision));
            } else {
                // We use the same rounding mode as COMPACT notation: known to be a
                // human-friendly rounding mode: integers, but add a decimal digit
                // as needed to ensure we have at least 2 significant digits.
                rounder = bogus.into(Precision.integer().withMinDigits(2));
            }
        }

        if (micros != null) {
            micros.rounder = rounder;
        }
        return new RouteResult(
                converterPreference.converter.convert(quantity, rounder),
                converterPreference.targetUnit
        );
    }

    private static Precision parseSkeletonToPrecision(String precisionSkeleton) {
        final String kSkeletonPrefix = "precision-increment/";
        if (!precisionSkeleton.startsWith(kSkeletonPrefix)) {
            throw new IllegalIcuArgumentException("precisionSkeleton is only precision-increment");
        }

        // TODO(icu-units#104): the C++ code uses a more sophisticated
        // parseIncrementOption which supports "withMinFraction" - e.g.
        // "precision-increment/0.5". Test with a unit preference that uses
        // this, and fix Java.
        String incrementValue = precisionSkeleton.substring(kSkeletonPrefix.length());
        return Precision.increment(new BigDecimal(incrementValue));
    }

    /**
     * Returns the list of possible output units, i.e. the full set of
     * preferences, for the localized, usage-specific unit preferences.
     * <p>
     * The returned pointer should be valid for the lifetime of the
     * UnitsRouter instance.
     */
    public List<MeasureUnit> getOutputUnits() {
        return this.outputUnits_;
    }

    /**
     * Contains the complex unit converter and the limit which representing the smallest value that the
     * converter should accept. For example, if the converter is converting to `foot+inch` and the limit
     * equals 3.0, thus means the converter should not convert to a value less than `3.0 feet`.
     * <p>
     * NOTE:
     * if the limit doest not has a value `i.e. (std::numeric_limits<double>::lowest())`, this mean there
     * is no limit for the converter.
     */
    public static class ConverterPreference {
        // The output unit for this ConverterPreference. This may be a MIXED unit -
        // for example: "yard-and-foot-and-inch".
        final MeasureUnitImpl targetUnit;
        final ComplexUnitsConverter converter;
        final BigDecimal limit;
        final String precision;

        // In case there is no limit, the limit will be -inf.
        public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl targetUnit,
                                   String precision, ConversionRates conversionRates) {
            this(source, targetUnit, BigDecimal.valueOf(Double.MIN_VALUE), precision,
                    conversionRates);
        }

        public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl targetUnit,
                                   BigDecimal limit, String precision, ConversionRates conversionRates) {
            this.converter = new ComplexUnitsConverter(source, targetUnit, conversionRates);
            this.limit = limit;
            this.precision = precision;
            this.targetUnit = targetUnit;

        }
    }

    public class RouteResult {
        public final ComplexUnitsConverter.ComplexConverterResult complexConverterResult;

        // The output unit for this RouteResult. This may be a MIXED unit - for
        // example: "yard-and-foot-and-inch", for which `measures` will have three
        // elements.
        public final MeasureUnitImpl outputUnit;

        RouteResult(ComplexUnitsConverter.ComplexConverterResult complexConverterResult, MeasureUnitImpl outputUnit) {
            this.complexConverterResult = complexConverterResult;
            this.outputUnit = outputUnit;
        }
    }
}
