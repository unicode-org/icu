/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;

public class SymbolTransition extends ComplexTransition {
    private static PosixCharMap mapping = new PosixCharMap();
    public static final SymbolTransition GLOBAL = new SymbolTransition(SUCCESS);
    public static void setCharMap(PosixCharMap mappingIn) {
        mapping = mappingIn;
        if (mapping == null) {
            mapping = new PosixCharMap();
        }
    }

    public static PosixCharMap getCharMap() {
        return mapping;
    }

    public SymbolTransition(int success) {
        super(success);
            //{{INIT_CONTROLS
        //}}
    }
    public boolean accepts(int c) {
        return '<' == (char)c;
    }
    protected void handleSuccess(Lex parser, StringBuffer output) {
        String text = parser.getData();
        String mappedText = mapping.mapKey(text);
        if (mappedText != null) {
            output.append(mappedText);
        } else {
            output.append(text);
        }
    }
    protected Lex.Transition[][] getStates() {
        synchronized (getClass()) {
            if (states == null) {
                states = new Lex.Transition[][] {
                    { //state 0: 
                        new Lex.CharTransition('<', Lex.ACCUMULATE_CONSUME, -1),
                        new Lex.ParseExceptionTransition("illegal characters in symbol")
                    },
                    { //state 1:
                        new Lex.CharTransition('/', Lex.ACCUMULATE_CONSUME, -2),
                        new Lex.CharTransition('>', Lex.ACCUMULATE_CONSUME, SUCCESS),
                        new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_PUTBACK, -3),
                        new Lex.EOFTransition(-3),
                        new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
                    },
                    { //state 2:
                        new Lex.CharTransition('>', Lex.ACCUMULATE_CONSUME, -1),
                        new Lex.CharTransition('/', Lex.ACCUMULATE_CONSUME, -1),
                        new Lex.ParseExceptionTransition("illegal escape character in symbol")
                    },
                    { //state 3: failure
                        new Lex.ParseExceptionTransition("unexpected end of line/file")
                    }
                };
            }
        }
        return states;
    }
    private static Lex.Transition[][] states;
    
    public static void main(String args[]) {
        try {
            Lex.Transition[][] states = {{ 
                new SymbolTransition(SUCCESS),
                new Lex.EOFTransition(),
                new Lex.ParseExceptionTransition("bad test input")
            }};
            //String text = "<CAPITAL><\"<><//><V%><N6><CYRILLIC>";
            String text = "<U><S><D> ";
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
