/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.impl.SimplePatternFormatter;

/**
 * QuantityFormatter represents an unknown quantity of something and formats a known quantity
 * in terms of that something. For example, a QuantityFormatter that represents X apples may
 * format 1 as "1 apple" and 3 as "3 apples" 
 * <p>
 * QuanitityFormatter appears here instead of in com.ibm.icu.impl because it depends on
 * PluralRules and DecimalFormat. It is package-protected as it is not meant for public use.
 */
class QuantityFormatter {
    
    private static final Map<String, Integer> INDEX_MAP = new HashMap<String, Integer>();
    private static final int MAX_INDEX;
    
    static {
        int idx = 0;
        // Other must be first.
        INDEX_MAP.put("other", idx++);
        INDEX_MAP.put("zero", idx++);
        INDEX_MAP.put("one", idx++);
        INDEX_MAP.put("two", idx++);
        INDEX_MAP.put("few", idx++);
        INDEX_MAP.put("many", idx++);
        
        MAX_INDEX = idx;
    }

    /**
     * Builder builds a QuantityFormatter.
     * 
     * @author rocketman
     */
    static class Builder {
        
        private SimplePatternFormatter[] templates;

        /**
         * Adds a template.
         * @param variant the plural variant, e.g "zero", "one", "two", "few", "many", "other"
         * @param template the text for that plural variant with "{0}" as the quantity. For
         * example, in English, the template for the "one" variant may be "{0} apple" while the
         * template for the "other" variant may be "{0} apples"
         * @return a reference to this Builder for chaining.
         * @throws IllegalArgumentException if variant is not recognized or
         *  if template has more than just the {0} placeholder.
         */
        public Builder add(String variant, String template) {
            ensureCapacity();
            Integer idx = INDEX_MAP.get(variant);
            if (idx == null) {
                throw new IllegalArgumentException(variant);
            }
            SimplePatternFormatter newT = SimplePatternFormatter.compile(template);
            if (newT.getPlaceholderCount() > 1) {
                throw new IllegalArgumentException(
                        "Extra placeholders: " + template);
            }
            templates[idx.intValue()] = newT;
            return this;
        }

        /**
         * Builds the new QuantityFormatter and resets this Builder to its initial state.
         * @return the new QuantityFormatter object.
         * @throws IllegalStateException if no template is specified for the "other" variant.
         *  When throwing this exception, build leaves this builder in its current state.
         */
        public QuantityFormatter build() {
            if (templates == null || templates[0] == null) {
                throw new IllegalStateException("At least other variant must be set.");
            }
            QuantityFormatter result = new QuantityFormatter(templates);
            templates = null;
            return result;          
        }

        /**
         * Resets this builder to its initial state.
         */
        public Builder reset() {
            templates = null;
            return this;
        }
        
        private void ensureCapacity() {
            if (templates == null) {
                templates = new SimplePatternFormatter[MAX_INDEX];
            }
        }

    }

    private final SimplePatternFormatter[] templates;

    private QuantityFormatter(SimplePatternFormatter[] templates) {
        this.templates = templates;
    }

    /**
     * Format formats a quantity with this object.
     * @param quantity the quantity to be formatted
     * @param numberFormat used to actually format the quantity.
     * @param pluralRules uses the quantity and the numberFormat to determine what plural
     *  variant to use for fetching the formatting template.
     * @return the formatted string e.g '3 apples'
     */
    public String format(double quantity, NumberFormat numberFormat, PluralRules pluralRules) {
        String formatStr = numberFormat.format(quantity);
        String variant = computeVariant(quantity, numberFormat, pluralRules);
        return getByVariant(variant).format(formatStr);
    }
    
    /**
     * Gets the SimplePatternFormatter for a particular variant.
     * @param variant "zero", "one", "two", "few", "many", "other"
     * @return the SimplePatternFormatter
     */
    public SimplePatternFormatter getByVariant(String variant) {
        Integer idxObj = INDEX_MAP.get(variant);
        SimplePatternFormatter template = templates[idxObj == null ? 0 : idxObj.intValue()];
        return template == null ? templates[0] : template;
    }
 
    private String computeVariant(double quantity, NumberFormat numberFormat, PluralRules pluralRules) {
        if (numberFormat instanceof DecimalFormat) {
            return pluralRules.select(((DecimalFormat) numberFormat).getFixedDecimal(quantity));            
        }
        return pluralRules.select(quantity);
    }

 
}
