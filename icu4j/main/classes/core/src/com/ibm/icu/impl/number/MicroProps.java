// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.List;

import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/**
 * MicroProps is the first MicroPropsGenerator that should be should be called,
 * producing an initialized MicroProps instance that will be passed on and
 * modified throughout the rest of the chain of MicroPropsGenerator instances.
 */
public class MicroProps implements Cloneable, MicroPropsGenerator {
    // Populated globally:
    public SignDisplay sign;
    public DecimalFormatSymbols symbols;
    public String nsName;
    public Padder padding;
    public DecimalSeparatorDisplay decimal;
    public IntegerWidth integerWidth;

    // Modifiers provided by the number formatting pipeline (when the value is known):

    // A Modifier provided by LongNameHandler, used for currency long names and
    // units. If there is no LongNameHandler needed, this should be an
    // null. (This is typically the third modifier applied.)
    public Modifier modOuter;

    // A Modifier for short currencies and compact notation. (This is typically
    // the second modifier applied.)
    public Modifier modMiddle;

    // A Modifier provided by ScientificHandler, used for scientific notation.
    // This is typically the first modifier applied.
    public Modifier modInner;

    public Precision rounder;
    public Grouper grouping;
    public boolean useCurrency;

    // Internal fields:
    private final boolean immutable;

    // The MeasureUnit with which the output is represented. May also have
    // MeasureUnit.Complexity.MIXED complexity, in which case mixedMeasures comes into
    // play.
    public MeasureUnit outputUnit;

    // In the case of mixed units, this is the set of integer-only units
    // *preceding* the final unit.
    public List<Measure> mixedMeasures;

    private volatile boolean exhausted;

    /**
     * @param immutable
     *            Whether this MicroProps should behave as an immutable after construction with respect
     *            to the quantity chain.
     */
    public MicroProps(boolean immutable) {
        this.immutable = immutable;
    }

    /**
     * As MicroProps is the "base instance", this implementation of
     * MircoPropsGenerator.processQuantity() just ensures that the output
     * `micros` is correctly initialized.
     * <p>
     * For the "safe" invocation of this function, micros must not be *this,
     * such that a copy of the base instance is made. For the "unsafe" path,
     * this function can be used only once, because the base MicroProps instance
     * will be modified and thus not be available for re-use.
     *
     * @param quantity The quantity for consideration and optional mutation.
     * @return an initialized MicroProps instance.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        if (immutable) {
            return (MicroProps) this.clone();
        } else if (exhausted) {
            // Safety check
            throw new AssertionError("Cannot re-use a mutable MicroProps in the quantity chain");
        } else {
            exhausted = true;
            return this;
        }
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
