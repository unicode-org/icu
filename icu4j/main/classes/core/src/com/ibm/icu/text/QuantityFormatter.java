/*
 *******************************************************************************
 * Copyright (C) 2013-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

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
    /**
     * Plural forms in index order: "other", "zero", "one", "two", "few", "many"
     * "other" must be first.
     */
    private static final int getPluralIndex(String pluralForm) {
        if (pluralForm.equals("other")) {
            return 0;
        }
        if (pluralForm.length() == 3) {
            if (pluralForm.equals("one")) {
                return 2;
            }
            if (pluralForm.equals("two")) {
                return 3;
            }
            if (pluralForm.equals("few")) {
                return 4;
            }
        }
        if (pluralForm.length() == 4) {
            if (pluralForm.equals("many")) {
                return 5;
            }
            if (pluralForm.equals("zero")) {
                return 1;
            }
        }
        return -1;
    }
    private static final int INDEX_COUNT = 6;

    /**
     * Builder builds a QuantityFormatter.
     * 
     * @author rocketman
     */
    static class Builder {
        private SimplePatternFormatter[] templates;

        boolean hasPatterns() {
            return templates != null;
        }

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
            int idx = getPluralIndex(variant);
            if (idx < 0) {
                throw new IllegalArgumentException(variant);
            }
            SimplePatternFormatter newT = SimplePatternFormatter.compile(template);
            if (newT.getPlaceholderCount() > 1) {
                throw new IllegalArgumentException(
                        "Extra placeholders: " + template);
            }
            // Keep templates == null until we add one.
            ensureCapacity();
            templates[idx] = newT;
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
                templates = new SimplePatternFormatter[INDEX_COUNT];
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
        int idx = getPluralIndex(variant);
        SimplePatternFormatter template = templates[idx < 0 ? 0 : idx];
        return template == null ? templates[0] : template;
    }
 
    private String computeVariant(double quantity, NumberFormat numberFormat, PluralRules pluralRules) {
        if (numberFormat instanceof DecimalFormat) {
            return pluralRules.select(((DecimalFormat) numberFormat).getFixedDecimal(quantity));            
        }
        return pluralRules.select(quantity);
    }
}
