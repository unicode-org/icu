/*
 * (C) Copyright IBM Corp. 1999-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package com.ibm.richtext.demo;

import com.ibm.richtext.awtui.TextFrame;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.textpanel.TextPanelListener;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import java.awt.Color;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.CollationKey;
import java.text.Collator;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * SyntaxColorer is a TextPanelListener that applies a style
 * to a set of words in the TextPanel.
 */
public final class SyntaxColorer implements TextPanelListener {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    private static final class Colorer {
        
        static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
        private int fStart;
        private Hashtable fStyleMap;
        private Collator fCollator = Collator.getInstance();
        private BreakIterator fBreakIter = BreakIterator.getWordInstance();

        private String fText;
        private int fCurrentStart;
        private int fCurrentLimit;
        private AttributeMap fCurrentStyle;
        
        Colorer(Hashtable styles) {

            fStyleMap = new Hashtable(styles.size());

            Enumeration e = styles.keys();
            while (e.hasMoreElements()) {
                String k = (String) e.nextElement();
                fStyleMap.put(fCollator.getCollationKey(k), styles.get(k));
            }
        }
        
        void set(CharacterIterator text, int start, int limit) {
            
            fStart = start;

            StringBuffer sb = new StringBuffer(limit-start);
            for (char c=text.setIndex(start); text.getIndex() != limit; c=text.next()) {
                sb.append(c);
            }
            fText = sb.toString();
            fCurrentStart = fCurrentLimit = 0;
            fCurrentStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;
            
            fBreakIter.setText(fText);
            fBreakIter.first();
        }
            
        boolean next() {
            
            if (fCurrentLimit == fText.length()) {
                fText = null;
                return false;
            }

            fCurrentStart = fCurrentLimit;
            fCurrentLimit = fBreakIter.next();
            
            String word = fText.substring(fCurrentStart, fCurrentLimit);
            CollationKey ck = fCollator.getCollationKey(word);
            fCurrentStyle = (AttributeMap) fStyleMap.get(ck);
            if (fCurrentStyle == null) {
                fCurrentStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;
            }
            
            return true;
        }
            
        int currentStart() {
            return fCurrentStart + fStart;
        }
        
        int currentLimit() {
            return fCurrentLimit + fStart;
        }
        
        AttributeMap currentStyle() {
            return fCurrentStyle;
        }
    }

    private BreakIterator fBreakIter = BreakIterator.getWordInstance();
    private Colorer fColorer;
    private boolean fModifying = false;
    private AttributeMap fDefaultKeywordStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;
    private Hashtable fModifierCache;
    
    public SyntaxColorer() {
        
        this(null);
    }
    
    public SyntaxColorer(MTextPanel panel) {
        
        Hashtable ht = new Hashtable();

        //Uncomment this to make keywords appear right-to-left!
        //fDefaultKeywordStyle = fDefaultKeywordStyle.addAttribute(TextAttribute.BIDI_EMBEDDING, 
        //                                                         new Integer(-1));
        
        fDefaultKeywordStyle = fDefaultKeywordStyle.addAttribute(TextAttribute.UNDERLINE,
                                                                 TextAttribute.UNDERLINE_ON);
        fDefaultKeywordStyle = fDefaultKeywordStyle.addAttribute(TextAttribute.FOREGROUND, 
                                                                 Color.blue);
        
        String[] javaWords = {"abstract" , "boolean", "break", "byte",
                              "byvalue", "case", "cast", "default",
                              "do", "double", "else", "extends", 
                              "false", "final", "goto", "if",
                              "implements", "import", "inner", "instanceof",
                              "int", "operator", "outer", "package",
                              "private", "protected", "public", "rest",
                              "synchronized", "this", "throw", "throws",
                              "transient", "true", "try",
                              "catch", "char", "const", "continue",
                              "finally", "float", "for", "future",
                              "generic", "interface", "long", "native",
                              "new", "null", "return", "short",
                              "static", "super", "switch", "var",
                              "void", "volatile", "while", "class"};

        for (int i=0; i < javaWords.length; i++) {
            ht.put(javaWords[i], fDefaultKeywordStyle);
        }
        
        fColorer = new Colorer(ht);
        
        if (panel != null) {
            MConstText text = panel.getText();
            colorRange(0, text.length(), text.createCharacterIterator(), panel);
        }
        
        fModifierCache = new Hashtable(2);
        fModifierCache.put(fDefaultKeywordStyle, 
                           StyleModifier.createReplaceModifier(fDefaultKeywordStyle));
        fModifierCache.put(AttributeMap.EMPTY_ATTRIBUTE_MAP, 
                           StyleModifier.createReplaceModifier(AttributeMap.EMPTY_ATTRIBUTE_MAP));
    }
    
    public boolean respondsToEventType(int type) {
        
        return type == TextPanelEvent.TEXT_CHANGED;
    }
    
    public void textEventOccurred(TextPanelEvent e) {

        if (fModifying) {
            return;
        }
        
        MTextPanel panel = (MTextPanel) e.getSource();
        
        final MConstText text = panel.getText();
        int start = text.damagedRangeStart();
        int limit = text.damagedRangeLimit();
        if (start > limit) {
            return;
        }
        
        CharacterIterator textIter = text.createCharacterIterator();
        
        fBreakIter.setText(textIter);
        if (start > 0) {
            if (start == text.length()) {
                fBreakIter.last();
            }
            else {
                fBreakIter.following(start-1);
            }
            start = fBreakIter.previous();
        }
        if (limit < text.length()) {
            fBreakIter.following(limit);
            //int l;
            if ((fBreakIter.previous()) <= limit) {
                limit = fBreakIter.next();
            }
        }
        
        fModifying = true;
        colorRange(start, limit, textIter, panel);
        fModifying = false;
    }
    
    private void colorRange(final int start, 
                            final int limit, 
                            CharacterIterator textIter,
                            MTextPanel panel) {
        
        fColorer.set(textIter, start, limit);

        MConstText oldText = panel.getText();
        MText newText = null;
        
        while (fColorer.next()) {

            int rangeStart = fColorer.currentStart();
            int rangeLimit = fColorer.currentLimit();
            
            AttributeMap style = fColorer.currentStyle();
            
            if (oldText.characterStyleLimit(rangeStart) < rangeLimit ||
                    oldText.characterStyleAt(rangeStart) != style) {
            
                int cstart = rangeStart-start;
                int climit = rangeLimit-start;
                if (newText == null) {
                    newText = new StyledText(oldText, start, limit);
                }
                StyleModifier mod = (StyleModifier) fModifierCache.get(style);
                newText.modifyCharacterStyles(cstart, climit, mod);
            }
        }
        
        if (newText != null) {
        
            int oldStart = panel.getSelectionStart();
            int oldLimit = panel.getSelectionEnd();
            
            panel.replaceRange(newText, start, limit);            
    
            panel.select(oldStart, oldLimit);
            if (oldStart == oldLimit) {
                StyleModifier mod = (StyleModifier) fModifierCache.get(AttributeMap.EMPTY_ATTRIBUTE_MAP);                
                panel.modifyCharacterStyleOnSelection(mod);
            }
        }
    }
    
    public static void main(String[] args) {
        
        TextFrame f = new TextFrame();
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setSize(400, 300);
        MTextPanel panel = f.getTextPanel();
        panel.addListener(new SyntaxColorer(panel));
        f.show();
    }
}