
/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/Tabber.java,v $
 * $Date: 2004/02/07 00:59:26 $
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.List;

public abstract class Tabber {
    static final byte LEFT = 0, CENTER = 1, RIGHT = 2;
    
    /**
     * Repeats a string n times
     * @param source
     * @param times
     * @return
     */
    // TODO - optimize repeats using doubling?
    public static String repeat(String source, int times) {
        if (times <= 0) return "";
        if (times == 1) return source;
        StringBuffer result = new StringBuffer();
        for (; times > 0; --times) {
            result.append(source);
        }
        return result.toString();
    }
    
    public String process(String source) {
        StringBuffer result = new StringBuffer();
        int lastPos = 0;
        for (int count = 0; lastPos < source.length(); ++count) {
            int pos = source.indexOf('\t', lastPos);
            if (pos < 0) pos = source.length();
            process_field(count, source, lastPos, pos, result);
            lastPos = pos+1;
        }
        return prefix + result.toString() + postfix;
    }
    
    private String prefix = "";
    private String postfix = "";
    
    public abstract void process_field(int count, String source, int start, int limit, StringBuffer output);

    public static class MonoTabber extends Tabber {
    
        private List stops = new ArrayList();
        private List types = new ArrayList();
    
        public void addAbsolute(int tabPos, int type) {
            stops.add(new Integer(tabPos));
            types.add(new Integer(type));
        }
    
        public void add(int fieldWidth, byte type) {
            int last = getStop(stops.size()-1);
            stops.add(new Integer(last + fieldWidth));
            types.add(new Integer(type));
        }
        
        public int getStop(int fieldNumber) {
            if (fieldNumber < 0) return 0;
            return ((Integer)stops.get(fieldNumber)).intValue();
        }
        /*
        public String process(String source) {
            StringBuffer result = new StringBuffer();
            int lastPos = 0;
            int count = 0;
            for (count = 0; lastPos < source.length() && count < stops.size(); count++) {
                int pos = source.indexOf('\t', lastPos);
                if (pos < 0) pos = source.length();
                String piece = source.substring(lastPos, pos);
                int stopPos = getStop(count);
                if (result.length() < stopPos) {
                    result.append(repeat(" ", stopPos - result.length()));
                    // TODO fix type
                }
                result.append(piece);
                lastPos = pos+1;
            }
            if (lastPos < source.length()) {
                result.append(source.substring(lastPos));
            }
            return result.toString();
        }
        */
        
        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            String piece = source.substring(start, limit);
            int pos = getStop(count);
            if (output.length() < pos) {
                output.append(repeat(" ", pos - output.length()));
                // TODO fix type
            } else {
                output.append(" ");
            }
            output.append(piece);
        }
        
    }
    
    public static class HTMLTabber extends Tabber {
        private List parameters = new ArrayList();
        {
            setPrefix("<tr>");
            setPostfix("</tr>");
        }
        public void setParameters(int count, String params) {
            parameters.set(count,params);
        }
        
        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            output.append("<td");
            String params = null;
            if (count < parameters.size()) params = (String) parameters.get(count);
            if (params != null) {
                output.append(' ');
                output.append(params);
            }
            output.append(">");
            output.append(source.substring(start, limit));
            // TODO Quote string
            output.append("</td>");            
        }
    }
    /**
     * @return
     */
    public String getPostfix() {
        return postfix;
    }

    /**
     * @return
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param string
     */
    public Tabber setPostfix(String string) {
        postfix = string;
        return this;
    }

    /**
     * @param string
     */
    public Tabber setPrefix(String string) {
        prefix = string;
        return this;
    }

}