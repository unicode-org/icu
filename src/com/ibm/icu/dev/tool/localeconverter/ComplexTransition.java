/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;

/**
 * A ComplexTransition is conceptually a single transition that
 * consumes multiple input characters.
 */
public abstract class ComplexTransition implements Lex.Transition {
        //the value that is returned by subclasses indicating that
        //the transition was successfull.  This value is then
        //discarded and the value passed to the constructor
        //is then returned to the caller.
    protected static final int SUCCESS = Lex.END_OF_FILE - 1;
    private int success;    //value to return if successfull
    private Lex parser;     //the parser used for this transition
        
    public ComplexTransition(int success) {
        this.success = success;
        this.parser = new Lex(null);
        //this.parser.debug(true);
    }
    
    public int doAction(int c, PushbackReader input, StringBuffer buffer) throws IOException {
        input.unread(c);
        parser.setStates(getStates());
        parser.setInput(input);
        try {
            parser.nextToken();
            handleSuccess(parser, buffer);
            return success;
        } catch (IOException e) {
            handleFailure(parser, buffer);
            throw e;
        }
    }

        //called after a successful parse
    protected void handleSuccess(Lex parser, StringBuffer output) throws IOException {
        parser.appendDataTo(output);
    }
    
        //called after a failed parse
    protected void handleFailure(Lex parser, StringBuffer output) {
    }
    
        //subclasses should return the states to use to parse this
        //transition
    protected abstract Lex.Transition[][] getStates();

    public ComplexTransition debug(boolean debug) {
        parser.debug(debug);
        return this;
    }
    
    public ComplexTransition debug(boolean debug, String tag) {
        parser.debug(debug, tag);
        return this;
    }
}
