// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;

/**
 * This is *not* a modifier; rather, it is an object that can return modifiers
 * based on given parameters.
 *
 * @author sffc
 */
public interface ModifierStore {
    /**
     * Returns a Modifier with the given parameters (best-effort).
     */
    Modifier getModifier(int signum, StandardPlural plural);
}
