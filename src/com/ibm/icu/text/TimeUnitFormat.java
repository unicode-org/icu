/*
 *******************************************************************************
 * Copyright (C) 2008, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;

/**
 * Format or parse a TimeUnitAmount, using plural rules for the units where available.
 * @see TimeUnitAmount
 * @see TimeUnitFormat
 * @author markdavis
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
 */
public class TimeUnitFormat extends MeasureFormat {

    private static final long serialVersionUID = -3707773153184971529L;

    private NumberFormat format;
    private ULocale locale;
    private transient Map timeUnitToCountToPatterns;
    private transient PluralRules pluralRules;
    private transient boolean isReady;

    /**
     * Create empty format. Use setLocale and/or setFormat to modify.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat() {}

    /**
     * Set the locale used for formatting or parsing.
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setLocale(ULocale locale) {
        this.locale = locale;
        return this;
    }
    
    /**
     * Set the locale used for formatting or parsing.
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setLocale(Locale locale) {
        this.locale = ULocale.forLocale(locale);
        return this;
    }
    
    /**
     * Set the format used for formatting or parsing. If null or not available, use the getNumberInstance(locale).
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setNumberFormat(NumberFormat format) {
        this.format = format;
        return this;
    }

    /**
     * Format a TimeUnitAmount.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (!isReady) {
            setup();
        }
        TimeUnitAmount amount = (TimeUnitAmount) obj;
        Map countToPattern = (Map) timeUnitToCountToPatterns.get(amount.getTimeUnit());
        double number = amount.getNumber().doubleValue();
        String count = pluralRules.select(number);
        MessageFormat pattern = (MessageFormat) countToPattern.get(count);
        return pattern.format(new Object[]{amount.getNumber()}, toAppendTo, pos);
    }

    /**
     * Parse a TimeUnitAmount.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public Object parseObject(String source, ParsePosition pos) {
        if (isReady) {
            setup();
        }
        Number resultNumber = null;
        TimeUnit resultTimeUnit = null;
        int oldPos = pos.getIndex();
        int newPos = -1;
        // we don't worry too much about speed on parsing, but this can be optimized later if needed.
        for (Iterator it = timeUnitToCountToPatterns.keySet().iterator(); it.hasNext();) {
            TimeUnit timeUnit = (TimeUnit) it.next();
            Map countToPattern = (Map) timeUnitToCountToPatterns.get(timeUnit);
            for (Iterator it2 = countToPattern.keySet().iterator(); it2.hasNext();) {
                String count = (String) it2.next();
                MessageFormat pattern = (MessageFormat) countToPattern.get(count);
                pos.setIndex(oldPos);
                // see if we can parse
                Object parsed = pattern.parseObject(source, pos);
                if (parsed == null) {
                    continue;
                }
                // check to make sure that the timeUnit is consistent
                Number temp = (Number)((Object[])parsed)[0];
                String select = pluralRules.select(temp.doubleValue());
                if (!count.equals(select)) {
                    continue;
                }
                resultNumber = temp;
                resultTimeUnit = timeUnit;
                newPos = pos.getIndex();
            }
        }
        if (resultNumber == null) {
            return null;
        }
        pos.setIndex(newPos);
        pos.setErrorIndex(-1);
        return new TimeUnitAmount(resultNumber, resultTimeUnit);
    }
    
    // *********************** Stubbed until we get CLDR data *******************************
    
    // just do English for now
    private static Object[][] data = {
        {TimeUnit.SECOND, "one", "{0} second", "other", "{0} seconds"},
        {TimeUnit.MINUTE, "one", "{0} minute", "other", "{0} minutes"},
        {TimeUnit.HOUR, "one", "{0} hour", "other", "{0} hours"},
        {TimeUnit.DAY, "one", "{0} day", "other", "{0} days"},
        {TimeUnit.WEEK, "one", "{0} week", "other", "{0} weeks"},
        {TimeUnit.MONTH, "one", "{0} month", "other", "{0} months"},
        {TimeUnit.YEAR, "one", "{0} year", "other", "{0} years"}
    };

    // Initially, we are storing all of these as MessageFormats.
    // I think it might actually be simpler to make them Decimal Formats later.
    private void setup() {
        if (locale == null) {
            if (format != null) {
                locale = format.getLocale(null);
            }
            if (locale == null) {
                locale = ULocale.getDefault();
            }
        }
        if (format == null) {
            format = NumberFormat.getNumberInstance(locale);
        }
        pluralRules = PluralRules.forLocale(locale);
        timeUnitToCountToPatterns = new HashMap();
        for (int i = 0; i < data.length; ++i) {
            Map temp = new TreeMap();
            for (int j = 1; j < data[i].length; j += 2) {
                final MessageFormat messageFormat = new MessageFormat((String)data[i][j+1], locale);
                if (format != null) {
                    messageFormat.setFormatByArgumentIndex(0, format);
                }
                temp.put(data[i][j], messageFormat);
            }
            timeUnitToCountToPatterns.put((TimeUnit)data[i][0], temp);
        }
        // it's an internal error if we get a count that we can't handle
        // so check here to make sure that the rules are all covered
        // TODO
        isReady = true;
    }

}
