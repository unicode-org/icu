// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.impl.number.MicroPropsGenerator;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.MultiplierProducer;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.Precision.SignificantRounderImpl;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;

/**
 * A class that defines the scientific notation style to be used when formatting numbers in
 * NumberFormatter.
 *
 * <p>
 * To create a ScientificNotation, use one of the factory methods in {@link Notation}.
 *
 * @draft ICU 60
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public class ScientificNotation extends Notation implements Cloneable {

    int engineeringInterval;
    boolean requireMinInt;
    int minExponentDigits;
    SignDisplay exponentSignDisplay;

    /* package-private */ ScientificNotation(
            int engineeringInterval,
            boolean requireMinInt,
            int minExponentDigits,
            SignDisplay exponentSignDisplay) {
        this.engineeringInterval = engineeringInterval;
        this.requireMinInt = requireMinInt;
        this.minExponentDigits = minExponentDigits;
        this.exponentSignDisplay = exponentSignDisplay;
    }

    /**
     * Sets the minimum number of digits to show in the exponent of scientific notation, padding with
     * zeros if necessary. Useful for fixed-width display.
     *
     * <p>
     * For example, with minExponentDigits=2, the number 123 will be printed as "1.23E02" in
     * <em>en-US</em> instead of the default "1.23E2".
     *
     * @param minExponentDigits
     *            The minimum number of digits to show in the exponent.
     * @return A ScientificNotation, for chaining.
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public ScientificNotation withMinExponentDigits(int minExponentDigits) {
        if (minExponentDigits >= 1 && minExponentDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            ScientificNotation other = (ScientificNotation) this.clone();
            other.minExponentDigits = minExponentDigits;
            return other;
        } else {
            throw new IllegalArgumentException("Integer digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Sets whether to show the sign on positive and negative exponents in scientific notation. The
     * default is AUTO, showing the minus sign but not the plus sign.
     *
     * <p>
     * For example, with exponentSignDisplay=ALWAYS, the number 123 will be printed as "1.23E+2" in
     * <em>en-US</em> instead of the default "1.23E2".
     *
     * @param exponentSignDisplay
     *            The strategy for displaying the sign in the exponent.
     * @return A ScientificNotation, for chaining.
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public ScientificNotation withExponentSignDisplay(SignDisplay exponentSignDisplay) {
        ScientificNotation other = (ScientificNotation) this.clone();
        other.exponentSignDisplay = exponentSignDisplay;
        return other;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Should not happen since parent is Object
            throw new AssertionError(e);
        }
    }

    /* package-private */ MicroPropsGenerator withLocaleData(
            DecimalFormatSymbols symbols,
            boolean build,
            MicroPropsGenerator parent) {
        return new ScientificHandler(this, symbols, build, parent);
    }

    // NOTE: The object lifecycle of ScientificModifier and ScientificHandler differ greatly in Java and
    // C++.
    //
    // During formatting, we need to provide an object with state (the exponent) as the inner modifier.
    //
    // In Java, where the priority is put on reducing object creations, the unsafe code path re-uses the
    // ScientificHandler as a ScientificModifier, and the safe code path pre-computes 25
    // ScientificModifier
    // instances. This scheme reduces the number of object creations by 1 in both safe and unsafe.
    //
    // In C++, MicroProps provides a pre-allocated ScientificModifier, and ScientificHandler simply
    // populates
    // the state (the exponent) into that ScientificModifier. There is no difference between safe and
    // unsafe.

    private static class ScientificHandler implements MicroPropsGenerator, MultiplierProducer, Modifier {

        final ScientificNotation notation;
        final DecimalFormatSymbols symbols;
        final ScientificModifier[] precomputedMods;
        final MicroPropsGenerator parent;
        /* unsafe */ int exponent;

        private ScientificHandler(
                ScientificNotation notation,
                DecimalFormatSymbols symbols,
                boolean safe,
                MicroPropsGenerator parent) {
            this.notation = notation;
            this.symbols = symbols;
            this.parent = parent;

            if (safe) {
                // Pre-build the modifiers for exponents -12 through 12
                precomputedMods = new ScientificModifier[25];
                for (int i = -12; i <= 12; i++) {
                    precomputedMods[i + 12] = new ScientificModifier(i, this);
                }
            } else {
                precomputedMods = null;
            }
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            assert micros.rounder != null;

            // Treat zero as if it had magnitude 0
            int exponent;
            if (quantity.isZero()) {
                if (notation.requireMinInt && micros.rounder instanceof SignificantRounderImpl) {
                    // Show "00.000E0" on pattern "00.000E0"
                    ((SignificantRounderImpl) micros.rounder).apply(quantity,
                            notation.engineeringInterval);
                    exponent = 0;
                } else {
                    micros.rounder.apply(quantity);
                    exponent = 0;
                }
            } else {
                exponent = -micros.rounder.chooseMultiplierAndApply(quantity, this);
            }

            // Add the Modifier for the scientific format.
            if (precomputedMods != null && exponent >= -12 && exponent <= 12) {
                // Safe code path A
                micros.modInner = precomputedMods[exponent + 12];
            } else if (precomputedMods != null) {
                // Safe code path B
                micros.modInner = new ScientificModifier(exponent, this);
            } else {
                // Unsafe code path: mutates the object and re-uses it as a Modifier!
                this.exponent = exponent;
                micros.modInner = this;
            }

            // We already performed rounding. Do not perform it again.
            micros.rounder = Precision.constructPassThrough();

            return micros;
        }

        @Override
        public int getMultiplier(int magnitude) {
            int interval = notation.engineeringInterval;
            int digitsShown;
            if (notation.requireMinInt) {
                // For patterns like "000.00E0" and ".00E0"
                digitsShown = interval;
            } else if (interval <= 1) {
                // For patterns like "0.00E0" and "@@@E0"
                digitsShown = 1;
            } else {
                // For patterns like "##0.00"
                digitsShown = ((magnitude % interval + interval) % interval) + 1;
            }
            return digitsShown - magnitude - 1;
        }

        @Override
        public int getPrefixLength() {
            // TODO: Localized exponent separator location.
            return 0;
        }

        @Override
        public int getCodePointCount() {
            // This method is not used for strong modifiers.
            throw new AssertionError();
        }

        @Override
        public boolean isStrong() {
            // Scientific is always strong
            return true;
        }

        @Override
        public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
            return doApply(exponent, output, rightIndex);
        }

        private int doApply(int exponent, NumberStringBuilder output, int rightIndex) {
            // FIXME: Localized exponent separator location.
            int i = rightIndex;
            // Append the exponent separator and sign
            i += output.insert(i, symbols.getExponentSeparator(), NumberFormat.Field.EXPONENT_SYMBOL);
            if (exponent < 0 && notation.exponentSignDisplay != SignDisplay.NEVER) {
                i += output.insert(i, symbols.getMinusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            } else if (exponent >= 0 && notation.exponentSignDisplay == SignDisplay.ALWAYS) {
                i += output.insert(i, symbols.getPlusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            }
            // Append the exponent digits (using a simple inline algorithm)
            int disp = Math.abs(exponent);
            for (int j = 0; j < notation.minExponentDigits || disp > 0; j++, disp /= 10) {
                int d = disp % 10;
                String digitString = symbols.getDigitStringsLocal()[d];
                i += output.insert(i - j, digitString, NumberFormat.Field.EXPONENT);
            }
            return i - rightIndex;
        }
    }

    private static class ScientificModifier implements Modifier {
        final int exponent;
        final ScientificHandler handler;

        ScientificModifier(int exponent, ScientificHandler handler) {
            this.exponent = exponent;
            this.handler = handler;
        }

        @Override
        public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
            return handler.doApply(exponent, output, rightIndex);
        }

        @Override
        public int getPrefixLength() {
            // TODO: Localized exponent separator location.
            return 0;
        }

        @Override
        public int getCodePointCount() {
            // This method is not used for strong modifiers.
            throw new AssertionError();
        }

        @Override
        public boolean isStrong() {
            // Scientific is always strong
            return true;
        }
    }
}