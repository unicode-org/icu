/*
 *******************************************************************************
 * Copyright (C) 2013-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.SimplePatternFormatter;
import com.ibm.icu.text.PluralRules.StandardPluralCategories;

/**
 * QuantityFormatter represents an unknown quantity of something and formats a known quantity
 * in terms of that something. For example, a QuantityFormatter that represents X apples may
 * format 1 as "1 apple" and 3 as "3 apples" 
 * <p>
 * QuanitityFormatter appears here instead of in com.ibm.icu.impl because it depends on
 * PluralRules and DecimalFormat. It is package-protected as it is not meant for public use.
 */
class QuantityFormatter {
    private final SimplePatternFormatter[] templates =
            new SimplePatternFormatter[StandardPluralCategories.COUNT];

    public QuantityFormatter() {}

    /**
     * Adds a template if there is none yet for the plural form.
     *
     * @param variant the plural variant, e.g "zero", "one", "two", "few", "many", "other"
     * @param template the text for that plural variant with "{0}" as the quantity. For
     * example, in English, the template for the "one" variant may be "{0} apple" while the
     * template for the "other" variant may be "{0} apples"
     * @throws IllegalArgumentException if variant is not recognized or
     *  if template has more than just the {0} placeholder.
     */
    public void addIfAbsent(CharSequence variant, String template) {
        int idx = StandardPluralCategories.getIndex(variant);
        if (templates[idx] != null) {
            return;
        }
        templates[idx] = SimplePatternFormatter.compileMinMaxPlaceholders(template, 0, 1);
    }

    /**
     * @return true if this object has at least the "other" variant
     */
    public boolean isValid() {
        return templates[StandardPluralCategories.OTHER_INDEX] != null;
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
        String variant = pluralRules.select(quantity, numberFormat);
        return getByVariant(variant).format(formatStr);
    }
    
    /**
     * Gets the SimplePatternFormatter for a particular variant.
     * @param variant "zero", "one", "two", "few", "many", "other"
     * @return the SimplePatternFormatter
     */
    public SimplePatternFormatter getByVariant(CharSequence variant) {
        assert isValid();
        int idx = StandardPluralCategories.getIndexOrOtherIndex(variant);
        SimplePatternFormatter template = templates[idx];
        return (template == null && idx != StandardPluralCategories.OTHER_INDEX) ?
                templates[StandardPluralCategories.OTHER_INDEX] : template;
    }
}
