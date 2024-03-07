// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.breakiter;

import java.text.CharacterIterator;

import com.ibm.icu.impl.CharacterIteration;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;

public final class UnhandledBreakEngine implements LanguageBreakEngine {
    // TODO: Use two UnicodeSets, one with all frozen sets, one with unfrozen.
    // in handleChar(), update the unfrozen version, clone, freeze, replace the frozen one.

    // Note on concurrency: A single instance of UnhandledBreakEngine is shared across all
    // RuleBasedBreakIterators in a process. They may make arbitrary concurrent calls.
    // If handleChar() is updating the set of unhandled characters at the same time
    // findBreaks() or handles() is referencing it, the referencing functions must see
    // a consistent set. It doesn't matter whether they see it before or after the update,
    // but they should not see an inconsistent, changing set.
    //
    // To do this, an update is made by cloning the old set, updating the clone, then
    // replacing the old with the new. Once made visible, each set remains constant.

    // TODO: it's odd that findBreaks() can produce different results, depending
    // on which scripts have been previously seen by handleChar(). (This is not a
    // threading specific issue). Possibly stop on script boundaries?

    volatile UnicodeSet fHandled = new UnicodeSet();
    public UnhandledBreakEngine() {
    }

    @Override
    public boolean handles(int c) {
        return fHandled.contains(c);
    }

    @Override
    public int findBreaks(CharacterIterator text, int startPos, int endPos,
            DictionaryBreakEngine.DequeI foundBreaks, boolean isPhraseBreaking) {

        UnicodeSet uniset = fHandled;
        int c = CharacterIteration.current32(text);
        while (text.getIndex() < endPos && uniset.contains(c)) {
            CharacterIteration.next32(text);
            c = CharacterIteration.current32(text);
        }
        return 0;
    }

    /**
     * Update the set of unhandled characters to include
     * all that have the same script as c.
     * May be called concurrently with handles() or findBreaks().
     * Must not be called concurrently with itself.
     */
    public void handleChar(int c) {
        UnicodeSet originalSet = fHandled;
        if (!originalSet.contains(c)) {
            int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
            UnicodeSet newSet = new UnicodeSet();
            newSet.applyIntPropertyValue(UProperty.SCRIPT, script);
            newSet.addAll(originalSet);
            fHandled = newSet;
        }
    }
}
