/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import static com.ibm.icu.impl.CharacterIteration.DONE32;

import java.text.CharacterIterator;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

final class UnhandledBreakEngine implements LanguageBreakEngine {
    // TODO: Use two arrays of UnicodeSet, one with all frozen sets, one with unfrozen.
    // in handleChar(), update the unfrozen version, clone, freeze, replace the frozen one.
    private final UnicodeSet[] fHandled = new UnicodeSet[BreakIterator.KIND_TITLE + 1];
    public UnhandledBreakEngine() {
        for (int i = 0; i < fHandled.length; i++) {
            fHandled[i] = new UnicodeSet();
        }
    }
    
    public boolean handles(int c, int breakType) {
        return (breakType >= 0 && breakType < fHandled.length) && 
                (fHandled[breakType].contains(c));
    }

    public int findBreaks(CharacterIterator text, int startPos, int endPos,
            boolean reverse, int breakType, DictionaryBreakEngine.DequeI foundBreaks) {
        text.setIndex(endPos);
        return 0;
    }

    public synchronized void handleChar(int c, int breakType) {
        if (breakType >= 0 && breakType < fHandled.length && c != DONE32) {
            if (!fHandled[breakType].contains(c)) {
                int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
                fHandled[breakType].applyIntPropertyValue(UProperty.SCRIPT, script);
            }
        }
    }
}
