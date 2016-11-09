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
    
    private final boolean firstPlaceholderReused;

    private SimplePatternFormatter(String pattern, PlaceholdersBuilder builder) {
        this.patternWithoutPlaceholders = pattern;
        this.placeholderIdsOrderedByOffset =
                builder.getPlaceholderIdsOrderedByOffset();
        this.placeholderCount = builder.getPlaceholderCount();
        this.firstPlaceholderReused = builder.getFirstPlaceholderReused();
    }

    /**
     * Compiles a string.
     * @param pattern The string.
     * @return the new SimplePatternFormatter object.
     */
    public static SimplePatternFormatter compile(String pattern) {
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
     * Formats the given values.
     */
    public String format(CharSequence... values) {
        return formatAndAppend(new StringBuilder(), null, values).toString();
    }

    /**
     * Formats the given values.
     * 
     * @param appendTo the result appended here. 
     * @param offsets position of first value in appendTo stored in offsets[0];
     *   second in offsets[1]; third in offsets[2] etc. An offset of -1 means that the
     *   corresponding value is not in appendTo. offsets.length and values.length may
     *   differ. If offsets.length < values.length then only the first offsets are written out;
     *   If offsets.length > values.length then the extra offsets get -1.
     *   If caller is not interested in offsets, caller may pass null here.
     * @param values the placeholder values. A placeholder value may not be the same object as
     *   appendTo.
     * @return appendTo
     */
    public StringBuilder formatAndAppend(
            StringBuilder appendTo, int[] offsets, CharSequence... values) {
        if (values.length < placeholderCount) {
            throw new IllegalArgumentException("Too few values.");
        }
        PlaceholderValues placeholderValues = new PlaceholderValues(values);
        if (placeholderValues.isAppendToInAnyIndexExcept(appendTo, -1)) {
            throw new IllegalArgumentException("Parameter values cannot be the same as appendTo.");
        }
        formatReturningOffsetLength(appendTo, offsets, placeholderValues);
        return appendTo;
    }
    
    /**
     * Formats the given values.
     * 
     * @param result The result is stored here overwriting any previously stored value. 
     * @param offsets position of first value in result stored in offsets[0];
     *   second in offsets[1]; third in offsets[2] etc. An offset of -1 means that the
     *   corresponding value is not in result. offsets.length and values.length may
     *   differ. If offsets.length < values.length then only the first offsets are written out;
     *   If offsets.length > values.length then the extra offsets get -1.
     *   If caller is not interested in offsets, caller may pass null here.
     * @param values the placeholder values. A placeholder value may be result itself in which case
     *   The previous value of result is used.
     * @return result
     */
    public StringBuilder formatAndReplace(
            StringBuilder result, int[] offsets, CharSequence... values) {
        if (values.length < placeholderCount) {
            throw new IllegalArgumentException("Too few values.");
        }
        PlaceholderValues placeholderValues = new PlaceholderValues(values);
        int placeholderAtStart = getUniquePlaceholderAtStart();
        
        // If patterns starts with a placeholder and the value for that placeholder
        // is result, then we can may be able optimize by just appending to result.
        if (placeholderAtStart >= 0 && values[placeholderAtStart] == result) {
            
            // If result is the value for other placeholders, call off optimization.
            if (placeholderValues.isAppendToInAnyIndexExcept(result, placeholderAtStart)) {
                placeholderValues.snapshotAppendTo(result);
                result.setLength(0);
                formatReturningOffsetLength(result, offsets, placeholderValues);
                return result;
            }
            
            // Otherwise we can optimize
            int offsetLength = formatReturningOffsetLength(result, offsets, placeholderValues);
            
            // We have to make the offset for the placeholderAtStart placeholder be 0.
            // Otherwise it would be the length of the previous value of result.
            if (offsetLength > placeholderAtStart) {
                offsets[placeholderAtStart] = 0;
            }
            return result;
        }
        if (placeholderValues.isAppendToInAnyIndexExcept(result, -1)) {
            placeholderValues.snapshotAppendTo(result);
        }
        result.setLength(0);
        formatReturningOffsetLength(result, offsets, placeholderValues);
        return result;
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
        return formatAndAppend(new StringBuilder(), null, values).toString();
    }
    
    /**
     * Returns this pattern with none of the placeholders.
     */
    public String getPatternWithNoPlaceholders() {
        return patternWithoutPlaceholders;
    }
    
    /**
     * Just like format, but uses placeholder values exactly as they are.
     * A placeholder value that is the same object as appendTo is treated
     * as the empty string. In addition, returns the length of the offsets
     * array. Returns 0 if offsets is null.
     */
    private int formatReturningOffsetLength(
            StringBuilder appendTo,
            int[] offsets,
            PlaceholderValues values) {
        int offsetLen = offsets == null ? 0 : offsets.length;
        for (int i = 0; i < offsetLen; i++) {
            offsets[i] = -1;
        }
        if (placeholderIdsOrderedByOffset.length == 0) {
            appendTo.append(patternWithoutPlaceholders);
            return offsetLen;
        }
        appendTo.append(
                patternWithoutPlaceholders,
                0,
                placeholderIdsOrderedByOffset[0]);
        setPlaceholderOffset(
                placeholderIdsOrderedByOffset[1],
                appendTo.length(),
                offsets,
                offsetLen);
        CharSequence placeholderValue = values.get(placeholderIdsOrderedByOffset[1]);
        if (placeholderValue != appendTo) {
            appendTo.append(placeholderValue);
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
            placeholderValue = values.get(placeholderIdsOrderedByOffset[i + 1]);
            if (placeholderValue != appendTo) {
                appendTo.append(placeholderValue);
            }
        }
        appendTo.append(
                patternWithoutPlaceholders,
                placeholderIdsOrderedByOffset[placeholderIdsOrderedByOffset.length - 2],
                patternWithoutPlaceholders.length());
        return offsetLen;
    }
    
    
    /**
     * Returns the placeholder at the beginning of this pattern (e.g 3 for placeholder {3}).
     * Returns -1 if the beginning of pattern is text or if the placeholder at beginning
     * of this pattern is used again elsewhere in pattern.
     */
    private int getUniquePlaceholderAtStart() {
        if (placeholderIdsOrderedByOffset.length == 0
                || firstPlaceholderReused || placeholderIdsOrderedByOffset[0] != 0) {
            return -1;
        }
        return placeholderIdsOrderedByOffset[1];
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
        private boolean firstPlaceholderReused = false;
        
        public void add(int placeholderId, int offset) {
            placeholderIdsOrderedByOffset.add(offset);
            placeholderIdsOrderedByOffset.add(placeholderId);
            if (placeholderId >= placeholderCount) {
                placeholderCount = placeholderId + 1;
            }
            int len = placeholderIdsOrderedByOffset.size();
            if (len > 2
                    && placeholderIdsOrderedByOffset.get(len - 1)
                            .equals(placeholderIdsOrderedByOffset.get(1))) {
                firstPlaceholderReused = true;
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
        
        public boolean getFirstPlaceholderReused() {
            return firstPlaceholderReused;
        }
    }
    
    /**
     * Represents placeholder values.
     */
    private static class PlaceholderValues {
        private final CharSequence[] values;
        private CharSequence appendTo;
        private String appendToCopy;
        
        public PlaceholderValues(CharSequence ...values) {
            this.values = values;
            this.appendTo = null;
            this.appendToCopy = null;
        }
        
        /**
         * Returns true if appendTo value is at any index besides exceptIndex.
         */
        public boolean isAppendToInAnyIndexExcept(CharSequence appendTo, int exceptIndex) {
            for (int i = 0; i < values.length; ++i) {
                if (i != exceptIndex && values[i] == appendTo) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * For each appendTo value, stores the snapshot of it in its place.
         */
        public void snapshotAppendTo(CharSequence appendTo) {
            this.appendTo = appendTo;
            this.appendToCopy = appendTo.toString();
        }
        
        /**
         *  Return placeholder at given index.
         */
        public CharSequence get(int index) {
            if (appendTo == null || appendTo != values[index]) {
                return values[index];
            }
            return appendToCopy;
        }       
    }
   
}
