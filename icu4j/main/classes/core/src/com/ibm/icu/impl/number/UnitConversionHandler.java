// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.number;

import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsData;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * A MicroPropsGenerator which converts a measurement from a simple MeasureUnit
 * to a Mixed MeasureUnit.
 */
public class UnitConversionHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private MeasureUnit fOutputUnit;
    private ComplexUnitsConverter fComplexUnitConverter;

    public UnitConversionHandler(MeasureUnit outputUnit, MicroPropsGenerator parent) {
        this.fOutputUnit = outputUnit;
        this.fParent = parent;

        List<MeasureUnit> singleUnits = outputUnit.splitToSingleUnits();

        assert outputUnit.getComplexity() == MeasureUnit.Complexity.MIXED;
        assert singleUnits.size() > 1;

        MeasureUnitImpl outputUnitImpl = MeasureUnitImpl.forIdentifier(outputUnit.getIdentifier());
       // TODO(icu-units#97): The input unit should be the largest unit, not the first unit, in the identifier.
        this.fComplexUnitConverter =
                new ComplexUnitsConverter(
                        new MeasureUnitImpl(outputUnitImpl.getSingleUnits().get(0)),
                        outputUnitImpl,
                        new UnitsData().getConversionRates());
    }

    /**
     * Obtains the appropriate output values from the Unit Converter.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        /*TODO: Questions : shall we check the parent if it is equals null */
        MicroProps result = this.fParent == null?
                this.fParent.processQuantity(quantity):
                new MicroProps(false);

        quantity.roundToInfinity(); // Enables toDouble
        List<Measure> measures = this.fComplexUnitConverter.convert(quantity.toBigDecimal());

        result.outputUnit = this.fOutputUnit;
        result.mixedMeasures = new ArrayList<>();
        UsagePrefsHandler.mixedMeasuresToMicros(measures, quantity, result);

        return result;
    }
}
