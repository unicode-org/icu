// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.translit;

import java.awt.event.KeyEvent;

import com.ibm.icu.dev.demo.impl.DumbTextComponent;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.Transliterator;

/**
 * A subclass of {@link DumbTextComponent} that passes key events through
 * a {@link com.ibm.icu.text.Transliterator}.
 *
 * @author Alan Liu
 */
public class TransliteratingTextComponent extends DumbTextComponent {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -8672128213174154047L;

    private static boolean DEBUG = false;

    private Transliterator translit = null;
    
    // NOTE: DISABLE THE START AND CURSOR UNTIL WE CAN GET IT TO WORK AT ALL

    // Index into getText() where the start of transliteration is.
    // As we commit text during transliteration, we advance
    // this.
    //private int start = 0;

    // Index into getText() where the cursor is; cursor >= start
    //private int cursor = 0;

//    private static final String COPYRIGHT =
//        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Constructor.
     */
    public TransliteratingTextComponent() {
        super();
        /*
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // We get an ActionEvent only when the selection changes
                resetTransliterationStart();
            }
        });
        */
    }

    /**
     * {@link DumbTextComponent} API.  Framework method that is called
     * when a <code>KeyEvent</code> is received.  This implementation
     * runs the new character through the current
     * <code>Transliterator</code>, if one is set, and inserts the
     * transliterated text into the buffer.
     */
    protected void handleKeyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        
        if (translit == null) {
            setKeyStart(-1);
            super.handleKeyTyped(e);
            return;
        }

        transliterate(ch, false);
    }
    
    public void flush() {
        if (translit != null) transliterate('\uFFFF', true);
    }
    
    
    protected void transliterate(char ch, boolean flush) {

        // ------------------------------------------------------------
        // The following case motivates the two lines that recompute
        // start and cursor below.

        //      "     "   
        // a b c q r|s t u m m
        // 0 1 2 3 4 5 6 7 8 9
        //       0 1 2

        // start 3, cursor 5, sel 6 -> { 0, 3, 2 }
        // : new int[] { 0, sel - start, cursor - start };
        
        // sz>99|9

        //      "     {   "
        // a b c q r 9 9|9 t u m m
        // 0 1 2 3 4 5 6 7 8 9 a b
        //       0 1 2 3 4

        // { 3, 5, 4 } -> start 6, cursor 7, sel 8
        // : start += index[0];
        // : cursor = start + index[2] - index[0];
        // ------------------------------------------------------------

        // Need to save start because calls to replaceRange will update
        // start and cursor.
        //int saveStart = start;

        int end = flush ? getSelectionEnd() : getSelectionStart();
        String sourceText = getText().substring(0,end);
        ReplaceableString buf = new ReplaceableString(sourceText);
        /*buf.replace(0, 1, getText().substring(start,
                                              getSelectionStart()));*/

        Transliterator.Position index = new Transliterator.Position();
        index.contextLimit = buf.length();
        index.contextStart = 0;
        index.start = getKeyStart();
        if (index.start == -1) index.start = getSelectionStart();
        index.limit = buf.length();

      //  StringBuffer log = null;
        if (DEBUG) {
            System.out.println("Transliterator: " + translit.getID());
            System.out.println("From:\t" + '"' + buf.toString() + '"'
                + "; {cs: " + index.contextStart
                + ", s: " + index.start
                + ", l: " + index.limit
                + ", cl: " + index.contextLimit 
                + "}" + "; '" + ch + "'"
                + " " + getKeyStart()
            );
        }

        if (flush) {
            translit.finishTransliteration(buf, index);
        } else {
            translit.transliterate(buf, index, ch);
        }
        
        if (DEBUG) {
            System.out.println("To:\t" + '"' + buf.toString() + '"'
                + "; {cs: " + index.contextStart
                + ", s: " + index.start
                + ", l: " + index.limit
                + ", cl: " + index.contextLimit 
                + "}"
                );
            System.out.println();
        }
        /*
        buf.replace(buf.length(), buf.length(), String.valueOf(ch));
        translit.transliterate(buf);
        */
        
        String result = buf.toString();
        //if (result.equals(sourceText + ch)) return;
        
        replaceRange(result, 0, getSelectionEnd());
        setKeyStart(index.start);
        
        // At this point start has been changed by the callback to
        // resetTransliteratorStart() via replaceRange() -- so use our
        // local copy, saveStart.

        // The START index is zero-based.  On entry to transliterate(),
        // it was zero.  We can therefore just add it to our original
        // getText()-based index value of start (in saveStart) to get
        // the new getText()-based start.
//        start = saveStart + index.contextStart;

        // Make the cursor getText()-based.  The CURSOR index is zero-based.
//        cursor = start + index.start - index.contextStart;

/*
        if (DEBUG) {
            String out = buf.toString();
            log.append(out.substring(0, index.contextStart)).
                append('{').
                append(out.substring(index.contextStart, index.start)).
                append('|').
                append(out.substring(index.start)).
                append('"');
            log.append(", {" + index.contextStart + ", " + index.contextLimit + ", " + index.start + "}, ");
//            log.append("start " + start + ", cursor " + cursor);
            log.append(", sel " + getSelectionStart());
            System.out.println(escape(log.toString()));
        }
        */
    }
    
    /**
     * Set the {@link com.ibm.icu.text.Transliterator} and direction to
     * use to process incoming <code>KeyEvent</code>s.
     * @param t the {@link com.ibm.icu.text.Transliterator} to use
     */
    public void setTransliterator(Transliterator t) {
        /*
        if (translit != t) { // [sic] pointer compare ok; singletons
            resetTransliterationStart();
        }
        */
        translit = t;
    }

    public Transliterator getTransliterator() {
        return translit;
    }

    /**
     * Reset the start point at which transliteration begins.  This
     * needs to be done when the user moves the cursor or when the
     * current {@link com.ibm.icu.text.Transliterator} is changed. 
     */
     /*
    private void resetTransliterationStart() {
        start = getSelectionStart();
        cursor = start;
    }
    */

    /**
     * Escape non-ASCII characters as Unicode.
     * JUST FOR DEBUGGING OUTPUT.
     */
    public static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                if (c == '\\') {
                    buf.append("\\\\"); // That is, "\\"
                } else {
                    buf.append(c);
                }
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }
}
