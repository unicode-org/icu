// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

/**
 * Exception used for illegal number skeleton strings.
 *
 * @author sffc
 */
public class SkeletonSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = 7733971331648360554L;

    public SkeletonSyntaxException(String message, CharSequence token) {
        super("Syntax error in skeleton string: " + message + ": " + token);
    }

    public SkeletonSyntaxException(String message, CharSequence token, Throwable cause) {
        super("Syntax error in skeleton string: " + message + ": " + token, cause);
    }
}
