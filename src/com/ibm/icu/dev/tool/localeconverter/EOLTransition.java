/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;


/**
 * This transition parses an end-of-line sequence.  The comment character
 * can be set to an arbitrary character, but it is shared globally.
 * A comment may only occur after an end-of-line.
 * EOL := <EOF> | <EOL_CHARS> [ <EOL_SEGMENT> ]*
 * EOL_SEGMENT := <EOL_CHARS> | <SPACE_CHARS> | <COMMENT>
 * SPACE_CHARS := " \t";
 * EOL_CHARS = "\r\n\u2028\u2029";
 * COMMENT_STRING := <COMMENT_CHAR> <COMMENT_BODY>
 * COMMENT_CHAR = "#";
 * COMMENT_BODY = [ ~<EOF> & ~<EOL_CHARS> ]*
 */
public class EOLTransition extends ComplexTransition {
    public static final String EOL_CHARS = "\f\r\n\u2028\u2029";
    public static final char DEFAULT_COMMENT_CHAR = '#';
    public static char COMMENT_CHAR = DEFAULT_COMMENT_CHAR;
    public static final EOLTransition GLOBAL = new EOLTransition(SUCCESS);
    
        /** Restore the comment character to the default value */
    public static synchronized char setDefaultCommentChar() {
        return setCommentChar(DEFAULT_COMMENT_CHAR);
    }
    
        /** Set a new comment character */
    public static synchronized char setCommentChar(char c) {
        char result = COMMENT_CHAR;
        COMMENT_CHAR = c;
        states = null;  //flush states
        return result;
    }
    
    public EOLTransition(int success) {
        super(success);
            //{{INIT_CONTROLS
        //}}
}
    public boolean accepts(int c) {
        return (EOL_CHARS.indexOf((char)c) >= 0 || COMMENT_CHAR==c);
    }
    protected Lex.Transition[][] getStates() {
        synchronized (getClass()) {
            if (states == null) {
                //cache the states so they can be shared.  This states
                //need to be flushed and rebuilt when the comment
                //character changes.
                states = new Lex.Transition[][] {
                    { //state 0: 
                        new Lex.StringTransition(EOL_CHARS, Lex.IGNORE_CONSUME, -1),
                        new Lex.EOFTransition(SUCCESS),
                    },
                    { //state 1:
                        new Lex.CharTransition(COMMENT_CHAR, Lex.IGNORE_CONSUME, -2),
                        new Lex.StringTransition(EOL_CHARS, Lex.IGNORE_CONSUME, -1),
                        new Lex.StringTransition(SpaceTransition.SPACE_CHARS, Lex.IGNORE_CONSUME, -1),
                        new Lex.EOFTransition(SUCCESS),
                        new Lex.DefaultTransition(Lex.IGNORE_PUTBACK, SUCCESS)
                    },
                    { //state 2:
                        new Lex.StringTransition(EOL_CHARS, Lex.IGNORE_CONSUME, -1),
                        new Lex.EOFTransition(SUCCESS),
                        new Lex.DefaultTransition(Lex.IGNORE_CONSUME, -2)
                    }
                };
            }
        }
        return states;
    }
    private static Lex.Transition[][] states;

    public static void main(String args[]) {
        try {
            
            final int TOKEN = 1;
            final int EOF = 2;
            final int EOL = 3;
            final int RANGE = 4;
            Lex.Transition[][] states = {{ 
                    new Lex.EOFTransition(EOF),
                    new EOLTransition(EOL),
                    new SymbolTransition(TOKEN),
                    new SpaceTransition(0),
                    new RangeTransition(RANGE),
                    new EOLTransition(EOL),
                    new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, 0)
            }};
            String text = "\n<NULL> 0x1243 #hello  kdsj\n";
            StringReader sr = new StringReader(text);
            PushbackReader pr = new PushbackReader(sr);
            Lex parser = new Lex(states, pr);
            parser.debug(true);
            parser.debug(true);
            int s = parser.nextToken();
            while (s == EOL) {
                System.out.println(parser.getData());
                s = parser.nextToken();
                while(s==TOKEN){
                    System.out.println(parser.getData());
                    s = parser.nextToken();   
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    //{{DECLARE_CONTROLS
    //}}
}
