/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;

public class QuoteTransition extends ComplexTransition {
    public static final QuoteTransition GLOBAL = new QuoteTransition(SUCCESS);

    public static final char STRING_CHAR = '"';
    
    public QuoteTransition(int success) {
        super(success);
            //{{INIT_CONTROLS
        //}}
    }
    public boolean accepts(int c) {
        return STRING_CHAR == (char)c;
    }
    protected Lex.Transition[][] getStates() {
        return states;
    }
    private static final Lex.Transition[][] states = {
        { //state 0: 
            new Lex.CharTransition(STRING_CHAR, Lex.IGNORE_CONSUME, -1),
            new Lex.ParseExceptionTransition("illegal character in quoted string")
        },
        { //state 1:
            new Lex.CharTransition(STRING_CHAR, Lex.IGNORE_CONSUME, SUCCESS),
            new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_CONSUME, -2),
            new EscapeTransition(-1),
            new SymbolTransition(-1),
            new Lex.EOFTransition(-2),
            new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
        },
        { //state 2: failure from eof
            new Lex.ParseExceptionTransition("unterminated string")
        }
    };

    public static void main(String args[]) {
        try {
            Lex.Transition[][] states = {{ 
                new QuoteTransition(SUCCESS),
                new Lex.EOFTransition(),
                new Lex.ParseExceptionTransition("bad test input")
            }};
            EscapeTransition.setEscapeChar('/');
            String text = "\"hello<\"/>>/d32world\"\"<one>/\n<two>\"";
            StringReader sr = new StringReader(text);
            PushbackReader pr = new PushbackReader(sr);
            Lex parser = new Lex(states, pr);
            //parser.debug(true);
            int s = parser.nextToken();
            while (s == SUCCESS) {
                System.out.println(parser.getData());
                s = parser.nextToken();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    //{{DECLARE_CONTROLS
    //}}
}
