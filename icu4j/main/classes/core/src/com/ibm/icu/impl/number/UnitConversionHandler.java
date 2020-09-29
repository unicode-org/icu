// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.List;

import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsData;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/**
 * A MicroPropsGenerator which converts a measurement from one MeasureUnit to
 * another. In particular, the output MeasureUnit may be a mixed unit. (The
 * input unit may not be a mixed unit.)
 */
public class UnitConversionHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private MeasureUnit fOutputUnit;
    private ComplexUnitsConverter fComplexUnitConverter;

    /**
     * Constructor.
     *
     * @param inputUnit Specifies the input MeasureUnit. Mixed units are not
     *     supported as input (because input is just a single decimal quantity).
     * @param outputUnit Specifies the output MeasureUnit.
     * @param parent The parent MicroPropsGenerator.
     */
    public UnitConversionHandler(MeasureUnit inputUnit,
                                 MeasureUnit outputUnit,
                                 MicroPropsGenerator parent) {
        this.fOutputUnit = outputUnit;
        this.fParent = parent;
        MeasureUnitImpl inputUnitImpl = MeasureUnitImpl.forIdentifier(inputUnit.getIdentifier());
        MeasureUnitImpl outputUnitImpl = MeasureUnitImpl.forIdentifier(outputUnit.getIdentifier());
        this.fComplexUnitConverter = new ComplexUnitsConverter(inputUnitImpl, outputUnitImpl,
                                                               new UnitsData().getConversionRates());
    }

    /**
     * Obtains the appropriate output values from the Unit Converter.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps result = this.fParent.processQuantity(quantity);

        quantity.roundToInfinity(); // Enables toDouble
        List<Measure> measures = this.fComplexUnitConverter.convert(quantity.toBigDecimal(), result.rounder);

        result.outputUnit = this.fOutputUnit;
        UsagePrefsHandler.mixedMeasuresToMicros(measures, quantity, result);

        return result;
    }
}
