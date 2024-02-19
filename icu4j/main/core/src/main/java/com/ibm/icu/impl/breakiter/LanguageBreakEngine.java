// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.breakiter;

import java.text.CharacterIterator;

/**
 * The LanguageBreakEngine interface is to be used to implement any
 * language-specific logic for break iteration.
 */
public interface LanguageBreakEngine {
    /**
     * @param c A Unicode codepoint value
     * @return true if the engine can handle this character, false otherwise
     */
    boolean handles(int c);

    /**
     * Implements the actual breaking logic. Find any breaks within a run in the supplied text.
     * @param text The text to break over. The iterator is left at
     * the end of the run of characters which the engine has handled.
     * @param startPos The index of the beginning of the range
     * @param endPos The index of the possible end of our range. It is possible,
     *  however, that the range ends earlier
     * @param foundBreaks A data structure to receive the break positions.
     * @return the number of breaks found
     */
    int findBreaks(CharacterIterator text, int startPos, int endPos,
            DictionaryBreakEngine.DequeI foundBreaks, boolean isPhraseBreaking);
}



