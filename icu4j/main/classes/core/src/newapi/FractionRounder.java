// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

/**
 * A rounding strategy based on a minimum and/or maximum number of fraction digits. Allows for a minimum or maximum
 * number of significant digits to be specified.
 */
public abstract class FractionRounder extends Rounder {

    /* package-private */ FractionRounder() {
    }

    /**
     * Ensures that no less than this number of significant figures are retained when rounding according to fraction
     * rules.
     *
     * <p>
     * For example, with integer rounding, the number 3.141 becomes "3". However, with minimum figures set to 2, 3.141
     * becomes "3.1" instead.
     *
     * <p>
     * This setting does not affect the number of trailing zeros. For example, 3.01 would print as "3", not "3.0".
     *
     * @param minFigures
     *            The number of significant figures to guarantee.
     * @return An immutable object for chaining.
     */
    public Rounder withMinFigures(int minFigures) {
        if (minFigures > 0 && minFigures <= MAX_VALUE) {
            return constructFractionSignificant(this, minFigures, -1);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }

    /**
     * Ensures that no more than this number of significant figures are retained when rounding according to fraction
     * rules.
     *
     * <p>
     * For example, with integer rounding, the number 123.4 becomes "123". However, with maximum figures set to 2, 123.4
     * becomes "120" instead.
     *
     * <p>
     * This setting does not affect the number of trailing zeros. For example, with fixed fraction of 2, 123.4 would
     * become "120.00".
     *
     * @param maxFigures
     *            Round the number to no more than this number of significant figures.
     * @return An immutable object for chaining.
     */
    public Rounder withMaxFigures(int maxFigures) {
        if (maxFigures > 0 && maxFigures <= MAX_VALUE) {
            return constructFractionSignificant(this, -1, maxFigures);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }
}