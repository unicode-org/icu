// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.util.List;

import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsRouter;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class UsagePrefsHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private UnitsRouter fUnitsRouter;

    public UsagePrefsHandler(ULocale locale, MeasureUnit inputUnit, String usage, MicroPropsGenerator parent) {
        assert parent != null;

        this.fParent = parent;
        this.fUnitsRouter = new UnitsRouter(MeasureUnitImpl.forIdentifier(inputUnit.getIdentifier()), locale, usage);
    }

    /**
     * Populates micros.mixedMeasures and modifies quantity, based on the values
     * in measures.
     */
    protected static void
    mixedMeasuresToMicros(ComplexUnitsConverter.ComplexConverterResult complexConverterResult, DecimalQuantity quantity, MicroProps outMicros) {
        outMicros.mixedMeasures = complexConverterResult.measures;
        outMicros.indexOfQuantity = complexConverterResult.indexOfQuantity;
        quantity.setToBigDecimal((BigDecimal) outMicros.mixedMeasures.get(outMicros.indexOfQuantity).getNumber());
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
        micros.outputUnit = routed.outputUnit.build();
        UsagePrefsHandler.mixedMeasuresToMicros(routed.complexConverterResult, quantity, micros);
        return micros;
    }
}
