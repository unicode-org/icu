// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

/**
 * A MicroPropsGenerator that multiplexes between different LongNameHandlers,
 * depending on the outputUnit.
 *
 * See processQuantity() for the input requirements.
 */
public class LongNameMultiplexer implements MicroPropsGenerator {
    /**
     * LongNameMultiplexer calls the parent MicroPropsGenerator itself,
     * receiving the MicroProps instance in use for this formatting pipeline.
     * Next it multiplexes between name handlers (fHandlers) which are not given
     * access to a parent. Consequently LongNameMultiplexer must give these
     * handlers the MicroProps instance.
     */
    public static interface ParentlessMicroPropsGenerator {
        public MicroProps processQuantityWithMicros(DecimalQuantity quantity, MicroProps micros);
    }

    private final MicroPropsGenerator fParent;

    private List<ParentlessMicroPropsGenerator> fHandlers;

    // Each MeasureUnit corresponds to the same-index MicroPropsGenerator
    // pointed to in fHandlers.
    private List<MeasureUnit> fMeasureUnits;

    public LongNameMultiplexer(MicroPropsGenerator fParent) {
        this.fParent = fParent;
    }

    // Produces a multiplexer for LongNameHandlers, one for each unit in
    // `units`. An individual unit might be a mixed unit.
    public static LongNameMultiplexer forMeasureUnits(ULocale locale,
                                                      List<MeasureUnit> units,
                                                      NumberFormatter.UnitWidth width,
                                                      String unitDisplayCase,
                                                      PluralRules rules,
                                                      MicroPropsGenerator parent) {
        LongNameMultiplexer result = new LongNameMultiplexer(parent);

        assert (units.size() > 0);

        result.fMeasureUnits = new ArrayList<>();
        result.fHandlers = new ArrayList<>();


        for (int i = 0; i < units.size(); i++) {
            MeasureUnit unit = units.get(i);
            result.fMeasureUnits.add(unit);
            if (unit.getComplexity() == MeasureUnit.Complexity.MIXED) {
                MixedUnitLongNameHandler mlnh = MixedUnitLongNameHandler
                        .forMeasureUnit(locale, unit, width, unitDisplayCase, rules, null);
                result.fHandlers.add(mlnh);
            } else {
                LongNameHandler lnh = LongNameHandler.forMeasureUnit(locale, unit, width, unitDisplayCase, rules, null);
                result.fHandlers.add(lnh);
            }
        }

        return result;
    }

    // The output unit must be provided via `micros.outputUnit`, it must match
    // one of the units provided to the factory function.
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        // We call parent.processQuantity() from the Multiplexer, instead of
        // letting LongNameHandler handle it: we don't know which LongNameHandler to
        // call until we've called the parent!
        MicroProps micros = this.fParent.processQuantity(quantity);

        // Call the correct LongNameHandler based on outputUnit
        for (int i = 0; i < this.fHandlers.size(); i++) {
            if (fMeasureUnits.get(i).equals(micros.outputUnit)) {
                ParentlessMicroPropsGenerator handler = fHandlers.get(i);
                return handler.processQuantityWithMicros(quantity, micros);
            }
        }
        throw new AssertionError
                (" We shouldn't receive any outputUnit for which we haven't already got a LongNameHandler");
    }
}
