// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.Modifier.Signum;

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
    Modifier getModifier(Signum signum, StandardPlural plural);
}
