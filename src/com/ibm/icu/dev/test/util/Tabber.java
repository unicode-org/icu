
/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/Tabber.java,v $
 * $Date: 2003/11/21 01:03:39 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.util;

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
        int count = 0;
        while (lastPos < source.length()) {
            int pos = source.indexOf('\t', lastPos);
            if (pos < 0) pos = source.length();
            process_field(count, source, lastPos, pos, result);
            lastPos = pos+1;
            ++count; // skip type
        }
        if (lastPos < source.length()) {
            result.append(source.substring(lastPos));
        }
        return result.toString();
    }
    
    public abstract void process_field(int count, String source, int start, int limit, StringBuffer output);

    public static class MonoTabber extends Tabber {
    
        private int[] tabs;
    
        public MonoTabber(int[] tabs) {
            this.tabs = (int[]) tabs.clone();
        }
    
        public String process(String source) {
            StringBuffer result = new StringBuffer();
            int lastPos = 0;
            int count = 0;
            while (lastPos < source.length() && count < tabs.length) {
                int pos = source.indexOf('\t', lastPos);
                if (pos < 0) pos = source.length();
                String piece = source.substring(lastPos, pos);
                if (result.length() < tabs[count]) {
                    result.append(repeat(" ", tabs[count] - result.length()));
                    // TODO fix type
                }
                result.append(piece);
                lastPos = pos+1;
                count += 2; // skip type
            }
            if (lastPos < source.length()) {
                result.append(source.substring(lastPos));
            }
            return result.toString();
        }
    
        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            String piece = source.substring(start, limit);
            if (output.length() < tabs[count*2]) {
                output.append(repeat(" ", tabs[count*2] - output.length()));
                // TODO fix type
            } else {
                output.append(" ");
            }
            output.append(piece);
        }
    }
}