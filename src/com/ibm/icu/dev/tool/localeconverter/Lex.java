/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;

/**
 * A Lex is a state machine.  Transitions can be activated
 * arbitrarily and can consume arbitrary amounts of text.
 * A transition simply says it can consume the next character
 * and returns the state that the machine should transition into.
 * States that are > 0 are final states and cause the nextToken
 * routine to return a value.
 */
public final class Lex {
    private Transition[][] states;  //final
    private PushbackReader input;   //final
    private int state;
    private String data;
    private final StringBuffer dataBuffer = new StringBuffer();
    private boolean debugMessagesOn;
    private String debugTag;
    
    public static final int END_OF_FILE = Integer.MAX_VALUE;

        /** Construct a new machine.  NOTE: setInput must be
         * called before nextToken is called */
    public Lex(final Transition[][] states) {
        this.states = states;
            //{{INIT_CONTROLS
//}}
}

        /** Construct a new machine. */
    public Lex(final Transition[][] statesIn, final PushbackReader inputIn) {
        states = statesIn;
        input = inputIn;
    }

        /** Return the current state */
    public int getState() {
        return state;
    }

        /** Return the data resulting from the last call to nextToken */
    public String getData() {
        if (data == null) {
            data = dataBuffer.toString();
        }
        return data;
    }
    
        /** Return the input reader used by this machine */
    public PushbackReader getInput() {
        return input;
    }

        /** set the input reader used by this machine */
    public void setInput(PushbackReader input) {
        this.input = input;
    }

        /** Return the states used by this machine */
    public Transition[][] getStates() {
        return states;
    }

    public void setStates(Transition[][] states) {
        this.states = states;
    }

        /** Return true if the specified string equals the
         * string returned by getData().  This routine
         * may be faster than calling getData because
         * it does not create a string on the heap.
         */
    public boolean dataEquals(final String other) {
        if (data != null) {
            //if dataBuffer has already been converted to
            //a string, just compare the strings.
            return data.equals(other);
        } else {
            if (other.length() != dataBuffer.length()) return false;
            final int len = dataBuffer.length();
            for (int i = 0; i < len; i++) {
                if (other.charAt(i) != dataBuffer.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
    
        /**
         * Append the data returned from getData() to the
         * specified StringBuffer.  This routine avoids
         * the creation of a String on the heap.
         */
    public void appendDataTo(StringBuffer buffer) {
        buffer.append(dataBuffer.toString());
    }
    
        /**
         * Return true if the data returned by getData()
         * starts with the specified string.  This routine avoids
         * the creation of a String on the heap.
         */
    public boolean dataStartsWith(String s) {
        if (dataBuffer.length() < s.length()) {
            return false;
        } else {
            final int sLength = s.length();
            for (int i = 0; i < sLength; i++) {
                if (dataBuffer.charAt(i) != s.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
        
        /** 
         * Convert the contents of the data buffer to an integer
         * of the specified radix
         */
    public int dataAsNumber(int radix) {
        int value = 0;
        final int len = dataBuffer.length();
        for (int i = 0; i < len; i++) {
            value = value*radix + Character.digit(dataBuffer.charAt(i), radix);
        }
        return value;
    }
    
        /**
         * Get the next token from the input stream.  The
         * dataBuffer is cleared and the state is set to zero before
         * parsing begins.  Parsing continues until a state
         * greater of equal to 0 s reached or an exception is thrown.
         * After each non-terminal transition, the state machine 
         * walks through all the transitions, in order, for the current
         * state until it finds one that will accept the current
         * input character and then calls doAction on that transition.
         */
    public int nextToken() throws IOException {
        state = 0;
        dataBuffer.setLength(0);

        do {
            int c = input.read();
            final Transition[] transition = states[-state];
            for (int i = 0; i < transition.length; i++) {
                if (transition[i].accepts(c)) {
                    //System.out.println("state["+ -state+"].transition["+i+"] on "+c+"  '"+(char)c+"' to state[");
                    state = transition[i].doAction(c, input, dataBuffer);
                    //println("" + -state + "]");
                    break;
                }
            }
        } while (state <= 0);
        data = null;    //dump the cached data string
        return state;
    }

        /** 
         * Get the next token and throw an acception if
         * the state machine is not in the specified state.
         */
    public void accept(final int neededState) throws IOException {
        if (neededState != nextToken()) {
            Exception e = new Exception();
            e.printStackTrace();
            throw new ParseException("Unexpected token - "+getData());
        }
    }
    
        /**
         * Get the next token and throw an exception if the
         * state machine is not in the specified state and the
         * value returned by getData() does not match the 
         * specified value.
         */
    public void accept(final int neededState, final String neededValue) throws IOException {
        accept(neededState);
        if (!dataEquals(neededValue)) {
            Exception e = new Exception();
            e.printStackTrace();
            throw new ParseException("Unexpected token - "+getData());
        }
    }

    public void debug(boolean debugMessagesOn) {
        this.debugMessagesOn = debugMessagesOn;
        debugTag = null;
    }
    
    public void debug(boolean debugMessagesOn, String tag) {
        this.debugMessagesOn = debugMessagesOn;
        this.debugTag = tag;
    }
    
/*  private void print(String s) {
        if (debugMessagesOn) {
            System.out.print(s);
        }
    }

    private void println(String s) {
        if (debugMessagesOn) {
            System.out.println(s+"  <"+debugTag);
        }
    }

    /**
     * The interface for state machine transitions. 
     */
    public interface Transition {
        /** 
         * Return true if the transition can accept the current input
         * character.
         */
        public boolean accepts(int c);
        /**
         * Perform the transition.
         * @param c the current input character
         * @param input the current input stream, minus the current input character
         * @param buffer the current output buffer
         * @return the state the machine should be in next
         */
        public int doAction(int c, PushbackReader input, StringBuffer buffer) throws IOException;
    }

        /* constants for BaseTransitions */
        /** Don't copy the current character to the output */
    public static final byte IGNORE = 0x01;
        /** Append the current character to the output */
    public static final byte ACCUMULATE = 0x00;
    private static final byte BUFFER_MASK = 0x01;

        /** Remove the current character from the input stream */
    public static final byte CONSUME = 0x00;
        /** Return the current character to the input stream */
    public static final byte PUTBACK = 0x10;
    private static final byte INPUT_MASK = 0x10;

    public static final byte 
            ACCUMULATE_CONSUME = (byte)(ACCUMULATE | CONSUME),
            IGNORE_CONSUME = (byte)(IGNORE | CONSUME),
            ACCUMULATE_PUTBACK = (byte)(ACCUMULATE | PUTBACK),
            IGNORE_PUTBACK = (byte)(IGNORE | PUTBACK);

    /**
     * Base class for simple transition classes
     */
    public static abstract class BaseTransition implements Transition {
        private final boolean addToBuffer;
        private final boolean unreadInput;
        private final int next;
        /**
         * Construct a new transition.  On execution, the 
         * specified action is performed and the
         * specified state is returned.
         * @param action the actions to perform to the 
         * input and output buffers.
         * @param next the next state the machine should
         * move into
         */
        public BaseTransition(byte action, int next) {
            this.addToBuffer = (action & BUFFER_MASK) == ACCUMULATE;
            this.unreadInput = (action & INPUT_MASK) == PUTBACK;
            this.next = next;
        }
        public abstract boolean accepts(int c);
        public int doAction(final int c, 
                final PushbackReader input, 
                final StringBuffer buffer) throws IOException {
                
            if (addToBuffer) {
                buffer.append((char)c);
            }
            if (unreadInput) {
                input.unread(c);
            }
            return next;
        }
    }

    /**
     * Accept end-of-file.
     */
    public static final class EOFTransition extends BaseTransition {
        public EOFTransition() {
            this(IGNORE_CONSUME, END_OF_FILE);
        }
        public EOFTransition(int next) {
            this(IGNORE_CONSUME, next);
        }
        public EOFTransition(byte action, int next) {
            super(action, next);
        }
        public boolean accepts(int c) {
            return c == -1;
        }
    }

    /**
     * Accept anything.
     */
    public static final class DefaultTransition extends BaseTransition {
        public DefaultTransition(byte action, int nextState) {
            super(action, nextState);
        }
        public boolean accepts(int c) {
            return true;
        }
    }

    /**
     * Accept any characters in the specified string.
     */
    public static final class StringTransition extends BaseTransition {
        private String chars;
        public StringTransition(String chars, byte action, int nextState) {
            super(action, nextState);
            this.chars = chars;
        }
        public boolean accepts(int c) {
            return chars.indexOf((char)c) != -1;
        }
    }

    /**
     * Accept only the specified character.
     */
    public static final class CharTransition extends BaseTransition {
        private char c;
        public CharTransition(char c, byte action, int nextState) {
            super(action, nextState);
            this.c = c;
        }
        public boolean accepts(int c) {
            return this.c == (char)c;
        }
    }

    /**
     * Accept anything, but throw the specified exception after
     * performing the specified action
     */
    public static final class ExceptionTransition extends BaseTransition {
        private IOException e;
        public ExceptionTransition(IOException e) {
            super(IGNORE_PUTBACK, END_OF_FILE); //state is ignored
        }
        public ExceptionTransition(byte action, IOException e) {
            super(action, END_OF_FILE); //state is ignored
        }
        public boolean accepts(int c) {
            return true;
        }
        public final int doAction(final int c, 
                final PushbackReader input, 
                final StringBuffer buffer) throws IOException {
            super.doAction(c, input, buffer);
            throw e;
        }
    }

        /**
         * The base class for parse exceptions.  Exceptions
         * resulting from parsing errors should be subclasses of this
         * class.
         */
    public static final class ParseException extends IOException {
        public final String reason;
        public ParseException() {
            this.reason = "unkown";
        }
        public ParseException(String reason) {
            this.reason = reason;
        }
        public String toString() {
            return reason;
        }
    }

    /**
     * Accept anything, execute as IGNORE_PUTBACK, and throw
     * a ParseException with the specified message
     */
    public static final class ParseExceptionTransition implements Transition {
        private String reason;
        public ParseExceptionTransition(String reason) {
            this.reason = reason;
        }
        public boolean accepts(int c) {
            return true;
        }
        public final int doAction(final int c, 
                final PushbackReader input, 
                final StringBuffer buffer) throws IOException {
            input.unread((char)c);
            throw new ParseException(reason);
        }
    }

    //{{DECLARE_CONTROLS
//}}
}
