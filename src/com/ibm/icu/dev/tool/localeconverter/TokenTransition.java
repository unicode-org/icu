/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;


public class TokenTransition extends ComplexTransition {
    public static final TokenTransition GLOBAL = new TokenTransition(SUCCESS);
    public static final String SEPARATOR_CHARS = ";" + SpaceTransition.SPACE_CHARS;
    
    public TokenTransition(int success) {
        super(success);
            //{{INIT_CONTROLS
        //}}
}
    public boolean accepts(int c) {
        return (c > 0) && 
            !EOLTransition.GLOBAL.accepts(c) && 
            !SpaceTransition.GLOBAL.accepts(c) &&  
            (
                (SEPARATOR_CHARS.indexOf((char)c) < 0) ||
                SymbolTransition.GLOBAL.accepts(c) ||
                QuoteTransition.GLOBAL.accepts(c) ||
                EscapeTransition.GLOBAL.accepts(c)
            );
    }
    protected Lex.Transition[][] getStates() {
        return states;
    }
    private static final Lex.Transition[][] states = {
        { //state 0: 
            new SymbolTransition(-1),
            new QuoteTransition(-1), 
            new EscapeTransition(-1),
            new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_PUTBACK, -2),
            new Lex.StringTransition(SEPARATOR_CHARS, Lex.IGNORE_PUTBACK, -3),
            new Lex.EOFTransition(-4),
            new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
        },
        { //state 1:
            new SymbolTransition(-1),
            new QuoteTransition(-1), 
            new EscapeTransition(-1),
            new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_PUTBACK, SUCCESS),
            new Lex.StringTransition(SEPARATOR_CHARS, Lex.IGNORE_PUTBACK, SUCCESS),
            new Lex.EOFTransition(SUCCESS),
            new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
        },
        { //state 2: failure - unexpected EOL
            new Lex.ParseExceptionTransition("unexpected EOL in token")
        },
        { //state 3: failure
            new Lex.ParseExceptionTransition("unexpected seperator character in token")
        },
        { //state 4: failure
            new Lex.ParseExceptionTransition("unexpected EOF in token")
        },
    };
    //{{DECLARE_CONTROLS
    //}}
}
