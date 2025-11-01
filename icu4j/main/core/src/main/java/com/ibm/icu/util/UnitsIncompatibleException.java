package com.ibm.icu.util;

/**
 * Exception thrown when an attempt is made to convert between units
 * that are not compatible with each other.
 *
 * Examples of unit compatibility:
 * - Converting "meter" to "mile" is linearly convertible (compatible).
 * - Converting "liter-per-100km" to "mile-per-gallon" is reciprocally
 *   convertible (compatible).
 * - Converting "meter" to "kilogram" is not convertible (incompatible).
 *
 * @draft ICU 78
 */
public class UnitsIncompatibleException extends IllegalArgumentException {

    // TODO: is the number correct?
    private static final long serialVersionUID = 1286569061095434541L;

    /**
     * Constructs a new UnitsIncompatibleException with no detail message.
     * 
     * @draft ICU 78
     */
    public UnitsIncompatibleException() {
        super();
    }

    /**
     * Constructs a new UnitsIncompatibleException with the specified detail
     * message.
     * 
     * @param message the detail message
     * @draft ICU 78
     */
    public UnitsIncompatibleException(String message) {
        super(message);
    }

    /**
     * Constructs a new UnitsIncompatibleException with the specified cause.
     *
     * @param cause original exception (normally a {@link IllegalArgumentException})
     * @draft ICU 78
     */

    public UnitsIncompatibleException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new UnitsIncompatibleException with the specified detail
     * message and cause.
     *
     * @param message the detail message
     * @param cause   the cause (normally an {@link IllegalArgumentException})
     * @draft ICU 78
     */
    public UnitsIncompatibleException(String message, Throwable cause) {
        super(message, cause);
    }

}
