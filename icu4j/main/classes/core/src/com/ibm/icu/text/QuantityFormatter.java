/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.HashMap;
import java.util.Map;

/**
 * QuantityFormatter represents an unknown quantity of something and formats a known quantity
 * in terms of that something. For example, a QuantityFormatter that represents X apples may
 * format 1 as "1 apple" and 3 as "3 apples" 
 * <p>
 * QuanitityFormatter appears here instead of in com.ibm.icu.impl because it depends on
 * PluralRules and DecimalFormat. It is package-protected as it is not meant for public use.
 * @author rocketman
 */
class QuantityFormatter {
    
    private static final Map<String, Integer> INDEX_MAP = new HashMap<String, Integer>();
    private static final int MAX_INDEX;
    
    static {
        int idx = 0;
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
        
        private String[] templates;

        /**
         * Adds a template.
         * @param variant the plural variant, e.g "zero", "one", "two", "few", "many", "other"
         * @param template the text for that plural variant with "{0}" as the quantity. For
         * example, in English, the template for the "one" variant may be "{0} apple" while the
         * template for the "other" variant may be "{0} apples"
         * @return a reference to this Builder for chaining.
         */
        public Builder add(String variant, String template) {
            ensureCapacity();
            templates[INDEX_MAP.get(variant)] = template;
            return this;
        }

        private void ensureCapacity() {
            if (templates == null) {
                templates = new String[MAX_INDEX];
            }
        }

        /**
         * Builds the new QuantityFormatter and resets this Builder to its initial state.
         * @return the new QuantityFormatter object.
         * @throws IllegalStateException if no template is specified for the "other" variant.
         *   When throwing this exception, build() still resets this Builder to its initial
         *   state.
         */
        public QuantityFormatter build() {
            if (templates == null || templates[0] == null) {
                templates = null;
                throw new IllegalStateException("At least other variant must be set.");
            }
            QuantityFormatter result = new QuantityFormatter(templates);
            templates = null;
            return result;          
        }

    }

    private final String[] templates;

    private QuantityFormatter(String[] templates) {
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
        String variant;
        if (numberFormat instanceof DecimalFormat) {
            variant = pluralRules.select(((DecimalFormat) numberFormat).getFixedDecimal(quantity));            
        } else {
            variant = pluralRules.select(quantity);
        }
        return getByVariant(variant).replace("{0}", formatStr);
    }

    private String getByVariant(String variant) {
        String template = templates[INDEX_MAP.get(variant)];
        return template == null ? templates[0] : template;
    }
}
