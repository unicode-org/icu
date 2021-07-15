// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

/**
 * This interface is used when all number formatting settings, including the locale, are known, except
 * for the quantity itself. The {@link #processQuantity} method performs the final step in the number
 * processing pipeline: it uses the quantity to generate a finalized {@link MicroProps}, which can be
 * used to render the number to output.
 *
 * <p>
 * In other words, this interface is used for the parts of number processing that are
 * <em>quantity-dependent</em>.
 *
 * <p>
 * In order to allow for multiple different objects to all mutate the same MicroProps, a "chain" of
 * MicroPropsGenerators are linked together, and each one is responsible for manipulating a certain
 * quantity-dependent part of the MicroProps. At the top of the linked list is a base instance of
 * {@link MicroProps} with properties that are not quantity-dependent. Each element in the linked list
 * calls {@link #processQuantity} on its "parent", then does its work, and then returns the result.
 *
 * <p>
 * This chain of MicroPropsGenerators is typically constructed by NumberFormatterImpl::macrosToMicroGenerator() when
 * constructing a NumberFormatter.
 *
 * <p>
 * A class implementing MicroPropsGenerator looks something like this:
 *
 * <pre>
 * class Foo implements MicroPropsGenerator {
 *     private final MicroPropsGenerator parent;
 *
 *     public Foo(MicroPropsGenerator parent) {
 *         this.parent = parent;
 *     }
 *
 *     &#64;Override
 *     public MicroProps processQuantity(DecimalQuantity quantity) {
 *         MicroProps micros = this.parent.processQuantity(quantity);
 *         // Perform manipulations on micros and/or quantity
 *         return micros;
 *     }
 * }
 * </pre>
 *
 * @author sffc
 *
 */
public interface MicroPropsGenerator {
    /**
     * Considers the given {@link DecimalQuantity}, optionally mutates it, and returns a
     * {@link MicroProps}.
     *
     * @param quantity
     *            The quantity for consideration and optional mutation.
     * @return A MicroProps instance resolved for the quantity.
     */
    public MicroProps processQuantity(DecimalQuantity quantity);
}
