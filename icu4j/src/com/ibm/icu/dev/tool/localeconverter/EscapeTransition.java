/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;
import java.io.*;

/**
 * An escape transition parses a POSIX escape sequence.  An escape
 * sequence can be a hex, octal, or decimal constant, or an escaped
 * character.  The resultant value is ALWAYS on byte long.  Longer
 * escaped values (ie.\xFF63) overflow and are truncated.  An escape
 * character followed by an EOL sequence is silently eaten.
 */
public class EscapeTransition extends ComplexTransition {
    public static final EscapeTransition GLOBAL = new EscapeTransition(SUCCESS);

    public static final char DEFAULT_ESCAPE_CHAR = '\\';
    public static char ESCAPE_CHAR = DEFAULT_ESCAPE_CHAR;
    
    private static final int DECIMAL = 1;   //decimal escape sequence was parsed
    private static final int OCTAL = 2;     //octal escape sequence was parsed
    private static final int HEX = 3;       //hex escape sequence was parsed
    private static final int ESCAPE = 4;    //character escape sequence was parsed
    private static final int EOL = 5;       //an escape character followed by EOF eaten
    
    private static final String OCTAL_CHARS = "01234567";
    private static final String DECIMAL_CHARS = "0123456789";
    private static final String HEX_CHARS = "0123456789abcdefABCDEF";
    
        /** Set the escape character to the default */
    public static synchronized char setDefaultEscapeChar() {
        return setEscapeChar(DEFAULT_ESCAPE_CHAR);
    }
    
        /** Set the escape character */
    public static synchronized char setEscapeChar(char c) {
        char result = ESCAPE_CHAR;
        ESCAPE_CHAR = c;
        theStates = null;
        return result;
    }
    
    public EscapeTransition(int success) {
        super(success);
            //{{INIT_CONTROLS
        //}}
}
    
    public boolean accepts(int c) {
        return ESCAPE_CHAR == (char)c;
    }
    
        /** Convert the accepted text into the appropriate unicode character */
    protected void handleSuccess(Lex parser, StringBuffer output) throws IOException {
        switch (parser.getState()) {
        case DECIMAL:
            output.append((char)parser.dataAsNumber(10));
            break;
        case OCTAL:
            output.append((char)parser.dataAsNumber(8));
            break;
        case HEX:
            output.append((char)parser.dataAsNumber(16));
            break;
        case ESCAPE:
            parser.appendDataTo(output);
            break;
        case EOL:
            //silently eat the EOL characters
            break;
        default:
            //should never get here
            throw new Lex.ParseException("Internal error parsing escape sequence");
//          parser.appendDataTo(output);
        }
    }
        /** return the states for this transaction */
    protected Lex.Transition[][] getStates() {
        synchronized (getClass()) {
            if (theStates == null) {
                //cache the states so they can be shared.  They must
                //be rebuilt when the escape character is changed.
                theStates = new Lex.Transition[][] {
                    { //state 0: 
                        new Lex.CharTransition(ESCAPE_CHAR, Lex.IGNORE_CONSUME, -1),
                        new Lex.ParseExceptionTransition("illegal escape character")
                    },
                    { //state 1:
                        new Lex.EOFTransition(OCTAL),
                        new Lex.CharTransition('d', Lex.IGNORE_CONSUME, -3),
                        new Lex.CharTransition('x', Lex.IGNORE_CONSUME, -2),
                        new Lex.StringTransition(OCTAL_CHARS, Lex.ACCUMULATE_CONSUME, -4),
                        new Lex.CharTransition(ESCAPE_CHAR, Lex.ACCUMULATE_CONSUME, ESCAPE),
                        new EOLTransition(EOL),
                        new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, ESCAPE)
                    },
                    { //state 2: hex
                        new Lex.EOFTransition(HEX),
                        new Lex.StringTransition(HEX_CHARS, Lex.ACCUMULATE_CONSUME, -2),
                        new Lex.DefaultTransition(Lex.IGNORE_PUTBACK, HEX)
                    },
                    { //state 3: decimal
                        new Lex.EOFTransition(DECIMAL),
                        new Lex.StringTransition(DECIMAL_CHARS, Lex.ACCUMULATE_CONSUME, -3),
                        new Lex.DefaultTransition(Lex.IGNORE_PUTBACK, DECIMAL)
                    },
                    { //state 4: octal
                        new Lex.EOFTransition(OCTAL),
                        new Lex.StringTransition(OCTAL_CHARS, Lex.ACCUMULATE_CONSUME, -4),
                        new Lex.DefaultTransition(Lex.IGNORE_PUTBACK, OCTAL)
                    },
                };
            }
        }
        return theStates;
    }
    private static Lex.Transition[][] theStates = null;

    public static void main(String[] args) {
        try {
            Lex.Transition[][] states = {{ 
                new EscapeTransition(SUCCESS),
                new Lex.EOFTransition(),
                new Lex.ParseExceptionTransition("bad test input")
            }};
            String text = "\\d100\\xAf\\\\\\777\\\n\\123\\x2028\\x2029";
            StringReader sr = new StringReader(text);
            PushbackReader pr = new PushbackReader(sr);
            Lex parser = new Lex(states, pr);
            //parser.debug(true);
            int s = parser.nextToken();
            while (s == SUCCESS) {
                System.out.println(parser.getState());
                s = parser.nextToken();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    //{{DECLARE_CONTROLS
    //}}
}
