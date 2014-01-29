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
 * Compiled version of a template such as "{1} was born in {0}".
 * <p>
 * Using Template objects is both faster and safer than adhoc replacement 
 * such as <code>pattern.replace("{0}", "Colorado").replace("{1} "Fred");</code>.
 * They are faster because they are precompiled; they are safer because they
 * account for curly braces escaped by apostrophe (').
 * 
 * Placeholders are of the form \{[0-9]+\}. If a curly brace is preceded
 * by a single quote, it becomes a curly brace instead of the start of a
 * placeholder. Two single quotes resolve to one single quote. 
 * <p>
 * Template objects are immutable and can be safely cached like strings.
 * <p>
 * Example:
 * <pre>
 * Template template = Template.compile("{1} '{born} in {0}");
 * 
 * // Output: "paul {born} in england"
 * System.out.println(template.evaluate("england", "paul"));
 * </pre>
 */
public class Template {
    private final String patternWithoutPlaceholders;
    private final int placeholderCount;
    
    // [0] first offset; [1] first placeholderId; [2] second offset;
    // [3] second placeholderId etc.
    private final int[] placeholderIdsOrderedByOffset;

    private Template(String pattern, PlaceholdersBuilder builder) {
        this.patternWithoutPlaceholders = pattern;
        this.placeholderIdsOrderedByOffset =
                builder.getPlaceholderIdsOrderedByOffset();
        this.placeholderCount = builder.getPlaceholderCount();
    }

    /**
     * Compiles a string into a template.
     * @param pattern The string.
     * @return the new template object.
     */
    public static Template compile(String pattern) {
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
        return new Template(newPattern.toString(), placeholdersBuilder);
        
    }
    
    /**
     * Evaluates this template with given values. The first value
     * corresponds to {0}; the second to {1} etc.
     * @param values the values.
     * @return The result.
     * @throws IllegalArgumentException if the number of arguments is
     * insufficient to match all the placeholders.
     */
    public String evaluate(Object... values) {
        StringResultBuilder builder = new StringResultBuilder();
        evaluatePrivate(values, builder);
        return builder.build();
    }
    
    /**
     * Evaluates this template with given values. The first value
     * corresponds to {0}; the second to {1} etc.
     * @param values the values.
     * @return The result of the evaluation.
     * @throws IllegalArgumentException if the number of arguments is
     * insufficient to match all the placeholders.
     */
    public Evaluation evaluateFull(Object... values) {
        EvaluationResultBuilder builder = new EvaluationResultBuilder();
        evaluatePrivate(values, builder);
        return builder.build();
    }
    
    /**
     * Returns the max placeholder ID + 1.
     */
    public int getPlaceholderCount() {
        return placeholderCount;
    }
    
    /**
     * Evaluates this template using values {0}, {1} etc. Note that this is
     * not the same as the original pattern string used to build the template.
     */
    @Override
    public String toString() {
        String[] values = new String[this.getPlaceholderCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.format("{%d}", i);
        }
        return evaluate((Object[]) values);
    }
    
    /**
     * The immutable evaluation of a template.
     */
    public static class Evaluation {
        
        private final String result;
        private final int[] offsets;

        private Evaluation(String result, int[] placeholderOffsets) {
            this.result = result;
            this.offsets = placeholderOffsets;
        }

        /**
         * Returns the offset of a particular placeholder in the evaluated
         * string. Returns -1 if the placeholder did not exist in the 
         * corresponding template.
         * @throws IndexOutOfBoundsException if placeholderId is negative.
         */
        public int getOffset(int placeholderId) {
            if (placeholderId < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (placeholderId >= offsets.length) {
                return -1;
            }
            return offsets[placeholderId];
        }

        /**
         * Returns the evaluated string.
         */
        public String toString() {
            return result;
        }

    }
    
    private void evaluatePrivate(Object[] values, ResultBuilder builder) {
        if (values.length < placeholderCount) {
            throw new IllegalArgumentException(
                    "There must be at least as values as placeholders.");
        }
        builder.setPlaceholderCount(placeholderCount);   
        if (placeholderIdsOrderedByOffset.length == 0) {
            builder.setResult(patternWithoutPlaceholders);
            return;
        }
        StringBuilder result = new StringBuilder();
        result.append(
                patternWithoutPlaceholders,
                0,
                placeholderIdsOrderedByOffset[0]);
        builder.setPlaceholderOffset(
                placeholderIdsOrderedByOffset[1], result.length());
        result.append(values[placeholderIdsOrderedByOffset[1]]);
        for (int i = 2; i < placeholderIdsOrderedByOffset.length; i += 2) {
            result.append(
                    patternWithoutPlaceholders,
                    placeholderIdsOrderedByOffset[i - 2],
                    placeholderIdsOrderedByOffset[i]);
            builder.setPlaceholderOffset(
                    placeholderIdsOrderedByOffset[i + 1], result.length());
            result.append(values[placeholderIdsOrderedByOffset[i + 1]]);
        }
        result.append(
                patternWithoutPlaceholders,
                placeholderIdsOrderedByOffset[placeholderIdsOrderedByOffset.length - 2],
                patternWithoutPlaceholders.length());
        builder.setResult(result.toString());
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
    
    private static interface ResultBuilder {
        void setPlaceholderCount(int length);
        void setPlaceholderOffset(int i, int length);
        void setResult(String patternWithoutPlaceholders);
    }
    
    private static class StringResultBuilder implements ResultBuilder {

        private String result;
        
        public void setPlaceholderCount(int count) {
        }

        public void setPlaceholderOffset(int placeholderId, int offset) {
        }

        public void setResult(String result) {
            this.result = result;    
        }
        
        public String build() {
            return result;
        }
    }
    
    private static class EvaluationResultBuilder implements ResultBuilder {
        private int[] placeholderOffsets;
        private String result;
        
        public void setPlaceholderCount(int count) {
            placeholderOffsets = new int[count];
            for (int i = 0; i < count; i++) {
                placeholderOffsets[i] = -1;
            }
        }

        public void setPlaceholderOffset(int placeholderId, int offset) {
            placeholderOffsets[placeholderId] = offset;
        }

        public void setResult(String result) {
            this.result = result;
        }
        
        public Evaluation build() {
            return new Evaluation(this.result, this.placeholderOffsets);
        }
        
    }

}
