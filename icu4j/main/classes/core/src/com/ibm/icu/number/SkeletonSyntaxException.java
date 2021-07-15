// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

/**
 * Exception used for illegal number skeleton strings.
 *
 * @stable ICU 62
 * @see NumberFormatter
 */
public class SkeletonSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = 7733971331648360554L;

    /**
     * Construct a new SkeletonSyntaxException with information about the token at the point of failure.
     *
     * @stable ICU 62
     * @see NumberFormatter
     */
    public SkeletonSyntaxException(String message, CharSequence token) {
        super("Syntax error in skeleton string: " + message + ": " + token);
    }

    /**
     * Construct a new SkeletonSyntaxException with information about the token at the point of failure.
     *
     * @stable ICU 62
     * @see NumberFormatter
     */
    public SkeletonSyntaxException(String message, CharSequence token, Throwable cause) {
        super("Syntax error in skeleton string: " + message + ": " + token, cause);
    }
}
