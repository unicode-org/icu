// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsRouter;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class UsagePrefsHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private UnitsRouter fUnitsRouter;

    public UsagePrefsHandler(ULocale locale, MeasureUnit inputUnit, String usage, MicroPropsGenerator parent) {
        assert parent != null;

        this.fParent = parent;
        this.fUnitsRouter =
                new UnitsRouter(MeasureUnitImpl.forIdentifier(inputUnit.getIdentifier()), locale.getCountry(), usage);
    }

    /**
     * Populates micros.mixedMeasures and modifies quantity, based on the values
     * in measures.
     */
    protected static void
    mixedMeasuresToMicros(List<Measure> measures, DecimalQuantity outQuantity, MicroProps outMicros) {
        outMicros.mixedMeasures = new ArrayList<>();
        if (measures.size() > 1) {
            // For debugging
            assert (outMicros.outputUnit.getComplexity() == MeasureUnit.Complexity.MIXED);

            // Check that we received the expected number of measurements:
            assert measures.size() == outMicros.outputUnit.splitToSingleUnits().size();

            // Mixed units: except for the last value, we pass all values to the
            // LongNameHandler via micros->mixedMeasures.
            for (int i = 0, n = measures.size() - 1; i < n; i++) {
                outMicros.mixedMeasures.add(measures.get(i));
            }
        }

        // The last value (potentially the only value) gets passed on via quantity.
        outQuantity.setToBigDecimal((BigDecimal) measures.get(measures.size()- 1).getNumber());
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
        final UnitsRouter.RouteResult routed = fUnitsRouter.route(quantity.toBigDecimal(), micros);

        final List<Measure> routedMeasures = routed.measures;
        micros.outputUnit = routed.outputUnit.build();

        UsagePrefsHandler.mixedMeasuresToMicros(routedMeasures, quantity, micros);
        return micros;
    }
}
