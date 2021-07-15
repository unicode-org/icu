// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.util;

/**
 * The input is impractically long for an operation.
 * It is rejected because it may lead to problems such as excessive
 * processing time, stack depth, or heap memory requirements.
 *
 * @draft ICU 68
 */
public class ICUInputTooLongException extends ICUException {
    private static final long serialVersionUID = -2602876786689338226L;

    /**
     * Default constructor.
     *
     * @draft ICU 68
     */
    public ICUInputTooLongException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @draft ICU 68
     */
    public ICUInputTooLongException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception
     * @draft ICU 68
     */
    public ICUInputTooLongException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception
     * @draft ICU 68
     */
    public ICUInputTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
