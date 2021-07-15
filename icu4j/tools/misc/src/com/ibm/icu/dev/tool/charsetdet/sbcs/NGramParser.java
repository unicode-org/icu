// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ***********************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and *
 * others. All Rights Reserved.                                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.charsetdet.sbcs;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NGramParser
{

    public interface NGramParserClient
    {
        char nextChar();
        void handleNGram(String key);
    }
    
    private static final int A_NULL = 0;
    private static final int A_ADDC = 1;
    private static final int A_ADDS = 2;
    
    /*
     * Character classes
     */
    public static final int C_IGNORE = 0;
    public static final int C_LETTER = 1;
    public static final int C_PUNCT  = 2;
    
    private static final int S_START  = 0;
    private static final int S_LETTER = 1;
    private static final int S_PUNCT  = 2;

    static final class StateEntry
    {
        private int newState;
        private int action;
        
        StateEntry(int theState, int theAction)
        {
            newState = theState;
            action   = theAction;
        }
        
        public int getNewState()
        {
            return newState;
        }
        
        public int getAction()
        {
            return action;
        }
    }
    
    private StateEntry[][] stateTable = {
            {new StateEntry(S_START,  A_NULL), new StateEntry(S_LETTER, A_ADDC), new StateEntry(S_PUNCT,  A_ADDS)},
            {new StateEntry(S_LETTER, A_NULL), new StateEntry(S_LETTER, A_ADDC), new StateEntry(S_PUNCT,  A_ADDS)},
            {new StateEntry(S_PUNCT,  A_NULL), new StateEntry(S_LETTER, A_ADDC), new StateEntry(S_PUNCT,  A_NULL)}
    };

    protected final int N_GRAM_SIZE = 3;
    
    private char[] letters = new char[N_GRAM_SIZE];
    private int letterCount;
    
    private static UnicodeSet letterSet = new UnicodeSet("[:letter:]");

    private NGramParserClient client;

    /**
     * 
     */
    public NGramParser(NGramParserClient theClient)
    {
        client = theClient;
        letterCount = 0;
    }
    
    public void setClient(NGramParserClient theClient)
    {
        client = theClient;
    }
    
    // TODO Is this good enough, or are there other C_IGNORE characters?
    // TODO Could this make Latin letters C_PUNCT for non-Latin scripts?
    public static int getCharClass(char ch)
    {
        if (ch == '\'' || ch == '\uFEFF') {
            return C_IGNORE;
        }
        
        if (letterSet.contains(ch)) {
            return C_LETTER;
        }
        
        return C_PUNCT;
    }
    
    public void reset()
    {
        letterCount = 0;
    }
    
    public void addLetter(char letter)
    {
        // somewhat clever stuff goes here...        
        letters[letterCount++] = letter;
        
        if (letterCount >= N_GRAM_SIZE) {
            String key = new String(letters);
            
            client.handleNGram(key);
            
            letterCount = N_GRAM_SIZE - 1;
            for (int i = 0; i < letterCount; i += 1) {
                letters[i] = letters[i + 1];
            }
        }
    }
    
    public void parse()
    {
        char ch;
        int state = 0;

        // this is where the clever stuff goes...
        while ((ch = client.nextChar()) != 0) {
            int charClass = getCharClass(ch);
            StateEntry entry = stateTable[state][charClass];
            
            state = entry.getNewState();
            
            switch (entry.getAction())
            {
            case A_ADDC:
                addLetter(Character.toLowerCase(ch));
                break;
                
            case A_ADDS:
                addLetter(' ');
                break;
 
            case A_NULL:
            default:
                break;
            }
        }
        
        addLetter(' ');
    }
}
