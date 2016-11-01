/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiled version of a pattern such as "{1} was born in {0}".
 * <p>
 * Using SimplePatternFormatter objects is both faster and safer than adhoc replacement 
 * such as <code>pattern.replace("{0}", "Colorado").replace("{1} "Fred");</code>.
 * They are faster because they are precompiled; they are safer because they
 * account for curly braces escaped by apostrophe (').
 * 
 * Placeholders are of the form \{[0-9]+\}. If a curly brace is preceded
 * by a single quote, it becomes a curly brace instead of the start of a
 * placeholder. Two single quotes resolve to one single quote. 
 * <p>
 * SimplePatternFormatter objects are immutable and can be safely cached like strings.
 * <p>
 * Example:
 * <pre>
 * SimplePatternFormatter fmt = SimplePatternFormatter.compile("{1} '{born} in {0}");
 * 
 * // Output: "paul {born} in england"
 * System.out.println(fmt.format("england", "paul"));
 * </pre>
 */
public class SimplePatternFormatter {
    private final String patternWithoutPlaceholders;
    private final int placeholderCount;
    
    // [0] first offset; [1] first placeholderId; [2] second offset;
    // [3] second placeholderId etc.
    private final int[] placeholderIdsOrderedByOffset;

    private SimplePatternFormatter(String pattern, PlaceholdersBuilder builder) {
        this.patternWithoutPlaceholders = pattern;
        this.placeholderIdsOrderedByOffset =
                builder.getPlaceholderIdsOrderedByOffset();
        this.placeholderCount = builder.getPlaceholderCount();
    }

    /**
     * Compiles a string.
     * @param pattern The string.
     * @return the new SimplePatternFormatter object.
     */
    public static SimplePatternFormatter compile(CharSequence pattern) {
        PlaceholdersBuilder placeholdersBuilder = new PlaceholdersBuilder();
        PlaceholderIdBuilder idBuilder =  new PlaceholderIdBuilder();
        StringBuilder newPattern = new StringBuilder();
        State state = State.INIT;
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            switch (state) {
            case INIT:
                if (ch == 0x27) {
                    state = State.APOSTROPHE;
                } else if (ch == '{') {
                    state = State.PLACEHOLDER;
                    idBuilder.reset();
                } else {
                    newPattern.append(ch);
                }
                break;
            case APOSTROPHE:
                if (ch == 0x27) {
                    newPattern.append("'");
                } else if (ch == '{') {
                    newPattern.append("{");
                } else {
                    newPattern.append("'");
                    newPattern.append(ch);
                }
                state = State.INIT;
                break;
            case PLACEHOLDER:
                if (ch >= '0' && ch <= '9') {
                    idBuilder.add(ch);
                } else if (ch == '}' && idBuilder.isValid()) {
                    placeholdersBuilder.add(idBuilder.getId(), newPattern.length());
                    state = State.INIT;
                } else {
                    newPattern.append('{');
                    idBuilder.appendTo(newPattern);
                    newPattern.append(ch);
                    state = State.INIT;
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
        switch (state) {
        case INIT:
            break;
        case APOSTROPHE:
            newPattern.append("'");
            break;
        case PLACEHOLDER:
            newPattern.append('{');
            idBuilder.appendTo(newPattern);
            break;
        default:
            throw new IllegalStateException();
        }
        return new SimplePatternFormatter(newPattern.toString(), placeholdersBuilder);
        
    }
      
    /**
     * Returns the max placeholder ID + 1.
     */
    public int getPlaceholderCount() {
        return placeholderCount;
    }
    
    /**
     * Returns true if this instance starts with placeholder with given id.
     */
    public boolean startsWithPlaceholder(int id) {
        if (placeholderIdsOrderedByOffset.length == 0) {
            return false;
        }
        return (placeholderIdsOrderedByOffset[0] == 0 && placeholderIdsOrderedByOffset[1] == id);
    }
    
    /**
     * Formats the given values.
     */
    public String format(CharSequence... values) {
        return format(new StringBuilder(), null, values).toString();
    }

    /**
     * Formats the given values.
     * 
     * @param appendTo the result appended here. Optimization: If the pattern this object
     * represents starts with a placeholder AND appendTo references the value of that same
     * placeholder (corresponding values parameter must also be a StringBuilder), then that
     * placeholder value is not copied to appendTo (Its already there). If the value of the
     * starting placeholder is very large, this optimization can offer huge savings.
     * @param offsets position of first value in appendTo stored in offsets[0];
     *   second in offsets[1]; third in offsets[2] etc. An offset of -1 means that the
     *   corresponding value is not in appendTo. offsets.length and values.length may
     *   differ. If caller is not interested in offsets, caller may pass null here.
     * @param values the values
     * @return appendTo
     */
    public StringBuilder format(
            StringBuilder appendTo, int[] offsets, CharSequence... values) {
        if (values.length < placeholderCount) {
            throw new IllegalArgumentException("Too few values.");
        }
        int offsetLen = offsets == null ? 0 : offsets.length;
        for (int i = 0; i < offsetLen; i++) {
            offsets[i] = -1;
        }
        if (placeholderIdsOrderedByOffset.length == 0) {
            appendTo.append(patternWithoutPlaceholders);
            return appendTo;
        }
        if (placeholderIdsOrderedByOffset[0] > 0 ||
                appendTo != values[placeholderIdsOrderedByOffset[1]]) {
            appendTo.append(
                    patternWithoutPlaceholders,
                    0,
                    placeholderIdsOrderedByOffset[0]);
            setPlaceholderOffset(
                    placeholderIdsOrderedByOffset[1],
                    appendTo.length(),
                    offsets,
                    offsetLen);
            appendTo.append(values[placeholderIdsOrderedByOffset[1]]);
        } else {
            setPlaceholderOffset(
                    placeholderIdsOrderedByOffset[1],
                    0,
                    offsets,
                    offsetLen);
        }
        for (int i = 2; i < placeholderIdsOrderedByOffset.length; i += 2) {
            appendTo.append(
                    patternWithoutPlaceholders,
                    placeholderIdsOrderedByOffset[i - 2],
                    placeholderIdsOrderedByOffset[i]);
            setPlaceholderOffset(
                    placeholderIdsOrderedByOffset[i + 1],
                    appendTo.length(),
                    offsets,
                    offsetLen);
            appendTo.append(values[placeholderIdsOrderedByOffset[i + 1]]);
        }
        appendTo.append(
                patternWithoutPlaceholders,
                placeholderIdsOrderedByOffset[placeholderIdsOrderedByOffset.length - 2],
                patternWithoutPlaceholders.length());
        return appendTo;
    }
    
    /**
     * Formats this object using values {0}, {1} etc. Note that this is
     * not the same as the original pattern string used to build this object.
     */
    @Override
    public String toString() {
        String[] values = new String[this.getPlaceholderCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.format("{%d}", i);
        }
        return format(new StringBuilder(), null, values).toString();
    }
    
    private static void setPlaceholderOffset(
            int placeholderId, int offset, int[] offsets, int offsetLen) {
        if (placeholderId < offsetLen) {
            offsets[placeholderId] = offset;
        }
    }

    private static enum State {
        INIT,
        APOSTROPHE,
        PLACEHOLDER,
    }
    
    private static class PlaceholderIdBuilder {
        private int id = 0;
        private int idLen = 0;
        
        public void reset() {
            id = 0;
            idLen = 0;
        }

        public int getId() {
           return id;
        }

        public void appendTo(StringBuilder appendTo) {
            if (idLen > 0) {
                appendTo.append(id);
            }
        }

        public boolean isValid() {
           return idLen > 0;
        }

        public void add(char ch) {
            id = id * 10 + ch - '0';
            idLen++;
        }     
    }
    
    private static class PlaceholdersBuilder {
        private List<Integer> placeholderIdsOrderedByOffset = new ArrayList<Integer>();
        private int placeholderCount = 0;
        
        public void add(int placeholderId, int offset) {
            placeholderIdsOrderedByOffset.add(offset);
            placeholderIdsOrderedByOffset.add(placeholderId);
            if (placeholderId >= placeholderCount) {
                placeholderCount = placeholderId + 1;
            }
        }
        
        public int getPlaceholderCount() {
            return placeholderCount;
        }
        
        public int[] getPlaceholderIdsOrderedByOffset() {
            int[] result = new int[placeholderIdsOrderedByOffset.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = placeholderIdsOrderedByOffset.get(i).intValue();
            }
            return result;
        }
    }

    /**
     * Returns this pattern with none of the placeholders.
     */
    public String getPatternWithNoPlaceholders() {
        return patternWithoutPlaceholders;
    }
}
