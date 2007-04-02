/*
 *******************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.awt.im.spi.InputMethodContext;

import java.awt.event.KeyEvent;
import java.awt.event.InputMethodEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class IndicInputMethodImpl {
    
    protected char[] KBD_MAP;
    
    private static final char SUBSTITUTION_BASE = '\uff00';

    // Indexed by map value - SUBSTITUTION_BASE
    protected char[][] SUBSTITUTION_TABLE;

    // Invalid character.
    private static final char INVALID_CHAR              = '\uffff';

    // Unmapped versions of some interesting characters.
    private static final char KEY_SIGN_VIRAMA           = '\u0064'; // or just 'd'??
    private static final char KEY_SIGN_NUKTA            = '\u005d';  // or just ']'??

    // Two succeeding viramas are replaced by one virama and one ZWNJ.
    // Viram followed by Nukta is replaced by one VIRAMA and one ZWJ
    private static final char ZWJ                       = '\u200d';
    private static final char ZWNJ                      = '\u200c';
    
    // Backspace
    private static final char BACKSPACE                 = '\u0008';

    // Sorted list of characters which can be followed by Nukta
    protected char[] JOIN_WITH_NUKTA;
    
    // Nukta form of the above characters
    protected char[] NUKTA_FORM;
        
    //private int log2;
    private int power;
    private int extra;

    // cached TextHitInfo. Only one type of TextHitInfo is required.
    private static final TextHitInfo ZERO_TRAILING_HIT_INFO = TextHitInfo.trailing(0);

    /**
     * Returns the index of the given character in the JOIN_WITH_NUKTA array.
     * If character is not found, -1 is returned.
     */
    private int nuktaIndex(char ch) {
        if (JOIN_WITH_NUKTA == null) {
            return -1;
        }
        
        int probe = power;
        int index = 0;

        if (JOIN_WITH_NUKTA[extra] <= ch) {
            index = extra;
        }
         
        while (probe > (1 << 0)) {
            probe >>= 1;

            if (JOIN_WITH_NUKTA[index + probe] <= ch) {
                index += probe;
            }
        }

        if (JOIN_WITH_NUKTA[index] != ch) {
            index = -1;
        }

        return index;
    }
    
    /**
     * Returns the equivalent character for hindi locale.
     * @param originalChar The original character.
     */
    private char getMappedChar(char originalChar) {
        if (originalChar <= KBD_MAP.length) {
            return KBD_MAP[originalChar];
        }
        
        return originalChar;
    }
    
    // Array used to hold the text to be sent.
    // If the last character was not committed it is stored in text[0].
    // The variable totalChars give an indication of whether the last
    // character was committed or not. If at any time ( but not within a
    // a call to dispatchEvent ) totalChars is not equal to 0 ( it can
    // only be 1 otherwise ) the last character was not committed.
    private char [] text = new char[4];

    // this is always 0 before and after call to dispatchEvent. This character assumes
    // significance only within a call to dispatchEvent.
    private int committedChars = 0;// number of committed characters

    // the total valid characters in variable text currently.
    private int totalChars = 0;//number of total characters ( committed + composed )

    private boolean lastCharWasVirama = false;
    
    private InputMethodContext context;

    //
    // Finds the high bit by binary searching
    // through the bits in n.
    //
    private static byte highBit(int n) {
        if (n <= 0) {
            return -32;
        }

        byte bit = 0;

        if (n >= 1 << 16) {
            n >>= 16;
            bit += 16;
        }

        if (n >= 1 << 8) {
            n >>= 8;
            bit += 8;
        }

        if (n >= 1 << 4) {
            n >>= 4;
            bit += 4;
        }

        if (n >= 1 << 2) {
            n >>= 2;
            bit += 2;
        }

        if (n >= 1 << 1) {
            n >>= 1;
            bit += 1;
        }

        return bit;
    }

    IndicInputMethodImpl(char[] keyboardMap, char[] joinWithNukta, char[] nuktaForm, 
                         char[][] substitutionTable) {
        KBD_MAP = keyboardMap;
        JOIN_WITH_NUKTA = joinWithNukta;
        NUKTA_FORM = nuktaForm;
        SUBSTITUTION_TABLE = substitutionTable;
        
        if (JOIN_WITH_NUKTA != null) {
            int log2 = highBit(JOIN_WITH_NUKTA.length);
            
            power = 1 << log2;
            extra = JOIN_WITH_NUKTA.length - power;
        } else {
            power = extra = 0;
        }
        
    }
    
    void setInputMethodContext(InputMethodContext context) {
        this.context = context;
    }
    
    void handleKeyTyped(KeyEvent kevent) {
        char keyChar = kevent.getKeyChar();
        char currentChar = getMappedChar(keyChar);
        
        // The Explicit and Soft Halanta case.
        if ( lastCharWasVirama ) {
            switch (keyChar) {
            case KEY_SIGN_NUKTA:
                currentChar = ZWJ;
                break;
            case KEY_SIGN_VIRAMA:
                currentChar = ZWNJ;
                break;
            default:
            }//endSwitch
        }//endif

        if (currentChar == INVALID_CHAR) {
            kevent.consume();
            return;
        }

        if (currentChar == BACKSPACE) {
            lastCharWasVirama = false;
            
            if (totalChars > 0) {
                totalChars = committedChars = 0;
            } else {
                return;
            }                            
        }
        else if (keyChar == KEY_SIGN_NUKTA) {
            int nuktaIndex = nuktaIndex(text[0]);
            
            if (nuktaIndex != -1) {
                text[0] = NUKTA_FORM[nuktaIndex];
            } else {
                // the last character was committed, commit just Nukta.
                // Note : the lastChar must have been committed if it is not one of
                // the characters which combine with nukta.
                // the state must be totalChars = committedChars = 0;
                text[totalChars++] = currentChar;
            }
            
            committedChars += 1;
        }
        else {
            int nuktaIndex = nuktaIndex(currentChar);
            
            if (nuktaIndex != -1) {
                // Commit everything but currentChar
                text[totalChars++] = currentChar;
                committedChars = totalChars-1;
            } else {
                if (currentChar >= SUBSTITUTION_BASE) {
                    char[] sub = SUBSTITUTION_TABLE[currentChar - SUBSTITUTION_BASE];
                    
                    System.arraycopy(sub, 0, text, totalChars, sub.length);
                    totalChars += sub.length;
                } else {
                    text[totalChars++] = currentChar;
                }
                
                committedChars = totalChars;
            }
        }
        
        ACIText aText = new ACIText( text, 0, totalChars, committedChars );
        int composedCharLength = totalChars - committedChars;
        TextHitInfo caret=null,visiblePosition=null;
        switch( composedCharLength ) {
            case 0:
                break;
            case 1:
                visiblePosition = caret = ZERO_TRAILING_HIT_INFO;
                break;
            default:
                // The code should not reach here. There is no case where there can be
                // more than one character pending.
        }
                        
        context.dispatchInputMethodEvent(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                                         aText,
                                         committedChars,
                                         caret,
                                         visiblePosition);
                                        
        if (totalChars == 0) {
            text[0] = INVALID_CHAR;
        } else {
            text[0] = text[totalChars - 1];// make text[0] hold the last character
        }
        
        lastCharWasVirama =  keyChar == KEY_SIGN_VIRAMA && !lastCharWasVirama;
                    
        totalChars -= committedChars;
        committedChars = 0;
        // state now text[0] = last character
        // totalChars = ( last character committed )? 0 : 1;
        // committedChars = 0;

        kevent.consume();// prevent client from getting this event.
    }

    void endComposition() {
        if( totalChars != 0 ) {// if some character is not committed.
            ACIText aText = new ACIText( text, 0, totalChars, totalChars );
            context.dispatchInputMethodEvent( InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                                aText, totalChars, null, null );
            totalChars = committedChars = 0;
            text[0] = INVALID_CHAR;
            lastCharWasVirama = false;
        }
    }
    
    // custom AttributedCharacterIterator -- much lightweight since currently there is no
    // attribute defined on the text being generated by the input method.
    private class ACIText implements AttributedCharacterIterator {
        private char [] text = null;
        private int committed = 0;
        private int index = 0;

        ACIText( char [] chArray, int offset, int length, int committed ) {
            this.text = new char[length];
            this.committed = committed;
            System.arraycopy( chArray, offset, text, 0, length );
        }

        // CharacterIterator methods.
        public char first() {
            return _setIndex( 0 );
        }

        public char last() {
            if( text.length == 0 ) {
                return _setIndex( text.length );
            }
            return _setIndex( text.length - 1 );
        }

        public char current() {
            if( index == text.length )
                return DONE;
            return text[index];
        }

        public char next() {
            if( index == text.length ) {
                return DONE;
            }
            return _setIndex( index + 1 );
        }

        public char previous() {
            if( index == 0 )
                return DONE;
            return _setIndex( index - 1 );
        }

        public char setIndex(int position) {
            if( position < 0 || position > text.length ) {
                throw new IllegalArgumentException();
            }
            return _setIndex( position );
        }

        public int getBeginIndex() {
            return 0;
        }

        public int getEndIndex() {
            return text.length;
        }

        public int getIndex() {
            return index;
        }

        public Object clone() {
            try {
                ACIText clone = (ACIText) super.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException();
            }
        }

        // AttributedCharacterIterator methods.
        public int getRunStart() {
            return index >= committed ? committed : 0;
        }

        public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
            return (index >= committed &&
                attribute == TextAttribute.INPUT_METHOD_UNDERLINE) ? committed : 0;
        }

        public int getRunStart(Set attributes) {
            return (index >= committed &&
                    attributes.contains(TextAttribute.INPUT_METHOD_UNDERLINE)) ? committed : 0;
        }

        public int getRunLimit() {
            return index < committed ? committed : text.length;
        }

        public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
            return (index < committed && 
                    attribute == TextAttribute.INPUT_METHOD_UNDERLINE) ? committed : text.length;
        }

        public int getRunLimit(Set attributes) {
            return (index < committed &&
                    attributes.contains(TextAttribute.INPUT_METHOD_UNDERLINE)) ? committed : text.length;
        }

        public Map getAttributes() {
            Hashtable result = new Hashtable();
            if (index >= committed && committed < text.length) {
                result.put(TextAttribute.INPUT_METHOD_UNDERLINE, 
                           TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
            }
            return result;
        }

        public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
            if (index >= committed && 
                committed < text.length &&
                attribute == TextAttribute.INPUT_METHOD_UNDERLINE) {

                return TextAttribute.UNDERLINE_LOW_ONE_PIXEL;
            }
            return null;
        }

        public Set getAllAttributeKeys() {
            HashSet result = new HashSet();
            if (committed < text.length) {
                result.add(TextAttribute.INPUT_METHOD_UNDERLINE);
            }
            return result;
        }

        // private methods

        /**
         * This is always called with valid i ( 0 < i <= text.length )
         */
        private char _setIndex( int i ) {
            index = i;
            if( i == text.length ) {
                return DONE;
            }
            return text[i];
        }

    }
}
