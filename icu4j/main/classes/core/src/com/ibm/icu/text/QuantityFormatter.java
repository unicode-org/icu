/*
 *******************************************************************************
 * Copyright (C) 2013-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.SimplePatternFormatter;
import com.ibm.icu.impl.UResource;

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
    private static final int getPluralIndex(CharSequence pluralForm) {
        switch (pluralForm.length()) {
        case 3:
            if ("one".contentEquals(pluralForm)) {
                return 2;
            } else if ("two".contentEquals(pluralForm)) {
                return 3;
            } else if ("few".contentEquals(pluralForm)) {
                return 4;
            }
            break;
        case 4:
            if ("many".contentEquals(pluralForm)) {
                return 5;
            } else if ("zero".contentEquals(pluralForm)) {
                return 1;
            }
            break;
        case 5:
            if ("other".contentEquals(pluralForm)) {
                return 0;
            }
            break;
        default:
            break;
        }
        return -1;
    }
    private static final int INDEX_COUNT = 6;

    private final SimplePatternFormatter[] templates = new SimplePatternFormatter[INDEX_COUNT];

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
        addIfAbsent(variant, template, null);
    }

    /**
     * Adds a template if there is none yet for the plural form.
     * This version only calls UResource.Value.getString()
     * if there is no template yet for the plural form.
     *
     * @param variant the plural variant, e.g "zero", "one", "two", "few", "many", "other"
     * @param template the text for that plural variant with "{0}" as the quantity. For
     * example, in English, the template for the "one" variant may be "{0} apple" while the
     * template for the "other" variant may be "{0} apples"
     * @throws IllegalArgumentException if variant is not recognized or
     *  if template has more than just the {0} placeholder.
     */
    public void addIfAbsent(CharSequence variant, UResource.Value template) {
        addIfAbsent(variant, null, template);
    }

    private void addIfAbsent(CharSequence variant, String template, UResource.Value templateValue) {
        int idx = getPluralIndex(variant);
        if (idx < 0) {
            throw new IllegalArgumentException(variant.toString());
        }
        if (templates[idx] != null) {
            return;
        }
        if (template == null) {
            template = templateValue.getString();
        }
        SimplePatternFormatter newT = SimplePatternFormatter.compile(template);
        if (newT.getPlaceholderCount() > 1) {
            throw new IllegalArgumentException(
                    "Extra placeholders: " + template);
        }
        templates[idx] = newT;
    }

    /**
     * @return true if this object has at least the "other" variant
     */
    public boolean isValid() {
        return templates[0] != null;
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
    public SimplePatternFormatter getByVariant(CharSequence variant) {
        assert isValid();
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
