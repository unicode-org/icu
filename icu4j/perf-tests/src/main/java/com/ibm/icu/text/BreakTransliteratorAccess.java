// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

/**
 * This provides access for testing to the non-public {@code BreakTransliterator}.
 *
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class BreakTransliteratorAccess {

    /**
     * The private constructor.
     *
     * <p>The internal} and deprecated tags prevent the otherwise public default constuctor from
     * being collected for the various API reports and checks.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    private BreakTransliteratorAccess() {
        // Prevent creation, and prevent the otherwise public default constuctor
        // from being collected for the various API reports and checks.
    }

    /**
     * The accessor method. Public only for testing.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Transliterator newInstance() {
        return new BreakTransliterator("Any-Break", UnicodeSet.ALL_CODE_POINTS);
    }
}
