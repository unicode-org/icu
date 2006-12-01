/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

public class POSIXLocaleReader {
    private final String localeDataPath;
    private final Locale locale;

    public static final int TAG_TOKEN = 1;
    public static final int SEPARATOR_TOKEN = 2;
    public static final int EOL_TOKEN = 3;
    public static final int EOF_TOKEN = 4;

        //these states are used to parse the bulk of the
        //input file.  They translate escaped characters
        //and symolic character references inline.
    static final Lex.Transition[][] dataStates = {
        { //state 0: start
            new SpaceTransition(0),
            new Lex.CharTransition(';', Lex.IGNORE_CONSUME, SEPARATOR_TOKEN),
            new Lex.CharTransition(',', Lex.IGNORE_CONSUME, SEPARATOR_TOKEN),
            new EOLTransition(EOL_TOKEN),
            new TokenTransition(TAG_TOKEN),
            new Lex.EOFTransition(EOF_TOKEN),
            new Lex.ParseExceptionTransition("unexpected characters")
        }
    };

    static final Lex.Transition[][] LCStates = {
        { //state 0: start
            new SpaceTransition(0),
            new EOLTransition(EOL_TOKEN),
            new Lex.EOFTransition(EOF_TOKEN),
            new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
        },
        {   //grab first word
            new Lex.StringTransition(SpaceTransition.SPACE_CHARS, Lex.IGNORE_PUTBACK, TAG_TOKEN),
            new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_PUTBACK, TAG_TOKEN),
            new Lex.EOFTransition(TAG_TOKEN),
            new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
        }
    };

    public POSIXLocaleReader(final String localeDataPath, final Locale locale) {
        this.localeDataPath = localeDataPath;
        this.locale = locale;
            //{{INIT_CONTROLS
//}}
}
    
    public Hashtable parse(String fileName, byte flags) throws IOException {
        try {
            Hashtable table = parseNative(fileName);
            Hashtable result = new PosixToNeutralConverter(flags, locale,fileName).convert(table);
            return result;
        } catch (LocaleConverter.ConversionError e) {
            System.err.println("Internal error converting locale data");
            return null;
        }
    }
    
    public Hashtable parseNative(String fileName) throws IOException {
        char oldEscapeChar = EscapeTransition.setDefaultEscapeChar();
        char oldCommentChar = EOLTransition.setDefaultCommentChar();
        Hashtable table = new Hashtable();
        try {
            
            LineCharNumberReader lines = new LineCharNumberReader(
                new BufferedReader(
                    new FileReader(
                        new File(localeDataPath, fileName)
                    )
                )
            );
            PushbackReader reader = new PushbackReader(lines);
            
            //Shove a newline at the start of the file.  This has the affect of allowing
            //the file to start with a comment, since the parser only allows comments as 
            //part of an EOL
            reader.unread('\n');

            String sectionTag = seekLC(reader);
            while (sectionTag != null) {
                try {
                    parseSection(table, reader, sectionTag);
                } catch (Lex.ParseException e) {
                    System.err.println("ERROR parsing: "+e.reason);
                    System.err.println("         Line: "+lines.getLineNumber());
                    System.err.println("         char: "+lines.getCharNumber());
                    seekEND(reader);
                    System.err.println("Skipped to line: "+(lines.getLineNumber()+1));
                }
                sectionTag = seekLC(reader);
            }
        } finally {
            EscapeTransition.setEscapeChar(oldEscapeChar);
            EOLTransition.setCommentChar(oldCommentChar);
        }
        return table;
    }
    
    private void parseSection(Hashtable table, PushbackReader reader, String sectionTag) throws IOException {
        if (sectionTag.equals("LC_CTYPE")) {
            parseCTYPE(table, reader);
        } else if (sectionTag.equals("LC_COLLATE")) {
            parseCOLLATE(table, reader);
        } else if (sectionTag.equals("LC_MONETARY")) {
            parseLC(table, reader, sectionTag);
        } else if (sectionTag.equals("LC_NUMERIC")) {
            parseLC(table, reader, sectionTag);
        } else if (sectionTag.equals("LC_TIME")) {
            parseLC(table, reader, sectionTag);
        } else if (sectionTag.equals("LC_MESSAGES")) {
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_MEASUREMENT")){
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_ADDRESS")){
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_PAPER")){
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_NAME")){
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_IDENTIFICATION")){
            parseLC(table, reader, sectionTag);
        }else if(sectionTag.equals("LC_TELEPHONE")){
            parseLC(table, reader, sectionTag);
            
        }else {
            System.out.println("Unrecognised section:"+sectionTag);
            System.out.println("Default parsing applied.");
            parseLC(table, reader, sectionTag);
        }
    }

    private PushbackReader createParserInput(String localeName) throws IOException {
        PushbackReader reader = new PushbackReader(
            new BufferedReader(
                new FileReader(
                    new File(localeDataPath, localeName)
                )
            )
        );
        //Shove a newline at the start of the file.  This has the affect of allowing
        //the file to start with a comment, since the parser only allows comments as 
        //part of an EOL
        reader.unread('\n');
        return reader;
    }
    
    private String seekLC(PushbackReader reader) throws IOException {
        Lex p = new Lex(LCStates, reader);
        final String LC = "LC_";
        int s = p.nextToken();
        while ((s != EOF_TOKEN)) {
            if (s == TAG_TOKEN) {
                if (p.dataStartsWith(LC)) {
                    String tag = p.getData();
                    do {
                        s = p.nextToken();
                    } while (s != EOL_TOKEN && s != EOF_TOKEN);
                    return tag;
                } else if (p.dataEquals("escape_char")) {
                    s = p.nextToken();
                    if (s == TAG_TOKEN || p.getData().length() != 1) {
                        String escape_char = p.getData();
                        EscapeTransition.setEscapeChar(escape_char.charAt(0));
                    } else {
                        System.out.println("Error in escape_char directive.  Directive ignored.");
                    }
                } else if (p.dataEquals("comment_char")) {
                    s = p.nextToken();
                    if (s == TAG_TOKEN || p.getData().length() != 1) {
                        String comment_char = p.getData();
                        if(comment_char.length() > 0){
                            EOLTransition.setCommentChar(comment_char.charAt(0));
                        }
                    } else {
                        System.out.println("Error in escape_char directive.  Directive ignored.");
                    }
                }
            }
            s = p.nextToken();
        }
        return null;
    }

    private boolean seekEND(PushbackReader reader) throws IOException {
        Lex p = new Lex(LCStates, reader);
        final String END = "END";
        int s = p.nextToken();
        while ((s != EOF_TOKEN)) {
            if (s == TAG_TOKEN) {
                if (p.dataStartsWith(END)) {
                    do {
                        s = p.nextToken();
                    } while (s != EOL_TOKEN && s != EOF_TOKEN);
                    return true;
                }
            }
            s = p.nextToken();
        }
        return false;
    }
    
    private void parseCTYPE(Hashtable table, PushbackReader reader) throws IOException {
        Lex p = new Lex(dataStates, reader);
        StringBuffer temp = new StringBuffer();
        int s = p.nextToken();
        if ((s == TAG_TOKEN) && p.dataEquals("copy")) {
            p.accept(TAG_TOKEN);
            parseCopy("LC_CTYPE", p.getData(), table);
            p.accept(EOL_TOKEN);
            p.accept(TAG_TOKEN, "END");
            p.accept(TAG_TOKEN, "LC_CTYPE");
        } else {
            while ((s == TAG_TOKEN) && !p.dataEquals("END")) {  
                //IGNORE the CTYPE definition ... we dont need it
                
                String key = p.getData();
                temp.setLength(0);
                p.accept(TAG_TOKEN);
                p.appendDataTo(temp);
                s = p.nextToken();
                while (s == SEPARATOR_TOKEN) {
                    p.accept(TAG_TOKEN);
                    p.appendDataTo(temp);
                    s = p.nextToken();
                }
                if (s != EOL_TOKEN) {
                    System.err.println("WARNING: Could not parse the Unexpected token: Expecting EOL got "+s);
                } else {
                    table.put(key, temp.toString());
                }
                
                s = p.nextToken();
                
            }
            p.accept(TAG_TOKEN, "LC_CTYPE");
        }
    }
    
    private void parseCopy(String section, String toCopy, Hashtable t) throws IOException {
        char oldEscapeChar = EscapeTransition.setDefaultEscapeChar();
        char oldCommentChar = EOLTransition.setDefaultCommentChar();
        try {
            PushbackReader reader = createParserInput(toCopy);
            String tag = seekLC(reader);
            while (tag != null && !section.equals(tag)) {
                tag = seekLC(reader);
            }
            if (tag != null) {
                parseSection(t, reader, section);
            } else {
                //hey {jf} - is this an error?
            }
        } finally {
            EscapeTransition.setEscapeChar(oldEscapeChar);
            EOLTransition.setCommentChar(oldCommentChar);
        }
    }
    
    private void parseLC(Hashtable t, PushbackReader reader, String sectionTag) throws IOException {
        Lex input = new Lex(dataStates, reader);
        input.accept(TAG_TOKEN);
        if (input.dataEquals("copy")) {
            input.accept(TAG_TOKEN);
            parseCopy(sectionTag, input.getData(), t);          
        } else {
            while ((input.getState() == TAG_TOKEN) && !input.dataEquals("END")) {   
                String label = input.getData();
                Vector values = new Vector();
                input.accept(TAG_TOKEN);
                String temp = input.getData();
                values.addElement(temp);
                while (input.nextToken() == SEPARATOR_TOKEN) {
                    input.accept(TAG_TOKEN);
                    String value = input.getData();
                    values.addElement(value);
                }
                if (values.size() > 1) {
                    String[] data = new String[values.size()];
                    values.copyInto(data);
                    t.put(label, data);
                } else {
                    t.put(label, values.elementAt(0));
                }
                if (input.getState() != EOL_TOKEN) {
                    System.out.println("Extraneous text after label: " +label);
                    throw new IOException();
                }
                input.nextToken();
            }
        }
        input.accept(TAG_TOKEN, sectionTag);
    }

    private void parseCOLLATE(Hashtable table, PushbackReader reader) 
            throws IOException {
        PosixCharMap map = new PosixCharMap(SymbolTransition.getCharMap());
        SymbolTransition.setCharMap(map);
        try {
            Lex input = new Lex(dataStates, reader);
            PosixCollationBuilder builder = new PosixCollationBuilder(map);

            int s = input.nextToken();
            while (s == EOL_TOKEN) s = input.nextToken();
            while (s == TAG_TOKEN) {
                if (input.dataEquals("END")) {
                    break;
                } else if (input.dataEquals("UNDEFINED")) {
                    System.err.println("WARNING: Undefined characters will sort last.");
                    s = input.nextToken();
                    while (s != EOF_TOKEN && s != EOL_TOKEN) {
                        s = input.nextToken();
                    }
                } else if (input.dataEquals("copy")) {
                    //copy collation rules from another locale
                    input.accept(TAG_TOKEN);
                    String toCopy = input.getData();
                    input.accept(EOL_TOKEN);
                    parseCopy("LC_COLLATE", toCopy, table);
                    System.err.println("Copying collation rules from "+toCopy+"...");
                } else if (input.dataEquals("...")) {
                    //fill the space between the last element and the next element
                    System.err.println("ERROR: Ellipsis not supported in collation rules.");
                    System.err.println("       Line ignored");
                } else if (input.dataEquals("replace-after")) {
                    System.err.println("ERROR: Replace-after not supported in collation rules.");
                    System.err.println("       Skipping until next replace-end.");
                    s = input.nextToken();
                    while (s != EOF_TOKEN) {
                        if (s == TAG_TOKEN && input.dataEquals("replace-end")) {
                            input.accept(EOL_TOKEN);
                            break;
                        }
                    }
                } else if (input.dataEquals("collating-element")) {
                    //Several characters should sort as a single element.
                    input.accept(TAG_TOKEN);    //get the symbol
                    String key = input.getData();
                    input.accept(TAG_TOKEN, "from");
                    input.accept(TAG_TOKEN);    //get the expansion
                    String value = input.getData();
                    builder.defineContraction(key, value);
                    input.accept(EOL_TOKEN);
                } else if (input.dataEquals("collating-symbol")) {
                    //define a weight symbol.  This symbol does not represent a character.
                    //It's only used for comparison purposes.  We define the character
                    //value for this character to be in the private area since our
                    //collation stuff doesn't sort that area.
                    input.accept(TAG_TOKEN);
                    builder.defineWeightSymbol(input.getData());
                    input.accept(EOL_TOKEN);
                } else if (input.dataEquals("order_start")) {
                    Vector tempVector = new Vector();
                    //start reading collation ordering rules. 
                    input.accept(TAG_TOKEN);
                    tempVector.addElement(input.getData());
                    s = input.nextToken();
                    while (s == SEPARATOR_TOKEN) {
                        input.accept(TAG_TOKEN);
                        tempVector.addElement(input.getData());
                        s = input.nextToken();
                    }
                    String[] order_start = new String[tempVector.size()];
                    tempVector.copyInto(order_start);
                    table.put("sort_order", order_start);
                } else if (input.dataEquals("order_end")) {
                    //build a list of ordered collation elements
                    input.accept(EOL_TOKEN);
                    SortedVector order = builder.getSortOrder();
                    PosixCollationBuilder.CollationRule[] ruleSource = 
                        new PosixCollationBuilder.CollationRule[order.size()];
                    order.copyInto(ruleSource); //copy into an array so we can add it to the output table
                        //this is only for information purposes so we can retrieve the source of the
                        //collationItems with the weights if we want them later
                    table.put("posix_sort_rules", ruleSource);
                } else {
                    //add a collation item to the list
                    builder.addRule(input.getData());
                    s = input.nextToken();
                    while (s == TAG_TOKEN) {
                        //we're expecting weights here
                        builder.addWeight(input.getData());
                        s = input.nextToken();
                        if (s == SEPARATOR_TOKEN) {
                            s = input.nextToken();
                        }
                    }
                }
                s = input.nextToken();
            }
            input.accept(TAG_TOKEN, "LC_COLLATE");
        } finally {
            SymbolTransition.setCharMap(map.getParent());
        }
    }
    //{{DECLARE_CONTROLS
//}}
}
