// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.number;

import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsRouter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public class UsagePrefsHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private UnitsRouter fUnitsRouter;

    public UsagePrefsHandler(ULocale locale, MeasureUnit inputUnit, String usage, MicroPropsGenerator parent) {
        assert parent != null;

        this.fParent = parent;
        this.fUnitsRouter =
                new UnitsRouter(MeasureUnitImpl.forIdentifier(inputUnit.getIdentifier()), locale.getCountry(), usage);
    }

    private static Precision parseSkeletonToPrecision(String precisionSkeleton) {
        final String kSuffixPrefix = "precision-increment/";
        if (!precisionSkeleton.startsWith(kSuffixPrefix)) {
            throw new IllegalIcuArgumentException("precisionSkeleton is only precision-increment");
        }

        String skeleton = precisionSkeleton.substring(kSuffixPrefix.length());
        String skeletons[] = skeleton.split("/");
        BigDecimal num = new BigDecimal(skeletons[0]);
        BigDecimal den =
                skeletons.length == 2 ?
                        new BigDecimal(skeletons[1]) :
                        new BigDecimal("1");


        return Precision.increment(num.divide(den, MathContext.DECIMAL128));
    }

    protected static void mixedMeasuresToMicros(List<Measure> measures, DecimalQuantity quantity, MicroProps micros) {
        if (measures.size() > 1) {
            // For debugging
            assert (micros.outputUnit.getComplexity() == MeasureUnit.Complexity.MIXED);

            // Check that we received measurements with the expected MeasureUnits:
            List<MeasureUnit> singleUnits = micros.outputUnit.splitToSingleUnits();

            assert measures.size() == singleUnits.size();

            // Mixed units: except for the last value, we pass all values to the
            // LongNameHandler via micros->mixedMeasures.
            for (int i = 0, n = measures.size() - 1; i < n; i++) {
                micros.mixedMeasures.add(measures.get(i));
            }
        }

        // The last value (potentially the only value) gets passed on via quantity.
        quantity.setToBigDecimal((BigDecimal) measures.get(measures.size()- 1).getNumber());
    }

    /**
     * Returns the list of possible output units, i.e. the full set of
     * preferences, for the localized, usage-specific unit preferences.
     * <p>
     * The returned pointer should be valid for the lifetime of the
     * UsagePrefsHandler instance.
     */
    public List<MeasureUnit> getOutputUnits() {
        return fUnitsRouter.getOutputUnits();
    }

    /**
     * Obtains the appropriate output value, MeasureUnit and
     * rounding/precision behaviour from the UnitsRouter.
     * <p>
     * The output unit is passed on to the LongNameHandler via
     * micros.outputUnit.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = this.fParent.processQuantity(quantity);

        quantity.roundToInfinity(); // Enables toDouble
        final UnitsRouter.RouteResult routed = fUnitsRouter.route(quantity.toBigDecimal());

        final List<Measure> routedMeasures = routed.measures;
        micros.outputUnit = routed.outputUnit.build();
        micros.mixedMeasures = new ArrayList<>();

        UsagePrefsHandler.mixedMeasuresToMicros(routedMeasures, quantity, micros);

        String precisionSkeleton = routed.precision;

        assert micros.rounder != null;

        // TODO: use the user precision if the user already set precision.
        if (precisionSkeleton != null && precisionSkeleton.length() > 0) {
            micros.rounder = parseSkeletonToPrecision(precisionSkeleton);
        } else {
            // We use the same rounding mode as COMPACT notation: known to be a
            // human-friendly rounding mode: integers, but add a decimal digit
            // as needed to ensure we have at least 2 significant digits.
            micros.rounder = Precision.integer().withMinDigits(2);
        }

        return micros;
    }
}
