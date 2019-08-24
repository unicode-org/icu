// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import com.google.common.base.Ascii;

/** Instructions in result specifications (e.g. "values=..." or "fallback=..."). */
enum Instruction {
    /** Defines processing and transformation of CLDR values. */
    VALUES,
    /** Defines fallback values to be used if no result was matched in a resource bundle. */
    FALLBACK,
    /** Defines an xpath used to hack result equality to make deduplication work. */
    BASE_XPATH,
    // TODO: Figure out how to remove this hack (probably by supporting partial matches).
    /**
     * Defines whether result values should be appended one at a time to a resource bundle
     * (default) or grouped into a separate array.
     */
    GROUP;

    /** Returns the instruction enum for its ID as it appears in the configuration file. */
    static Instruction forId(String id) {
        return Instruction.valueOf(Ascii.toUpperCase(id));
    }
}
