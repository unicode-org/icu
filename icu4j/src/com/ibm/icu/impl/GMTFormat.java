/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.MissingResourceException;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * GMTFormat is a class implementing Unicode TR#35 localized
 * GMT format.  This class is used for formatting TimeZone offset
 * or parsing GMT formatted string into offset.
 */
public class GMTFormat {
    private static final String[] DEF_GMT_PATTERNS = {"GMT{0}"};
    private static final int[] DEF_GMT_PREFIX_LEN = {3};

    private static final String DEF_HOUR_FORMAT = "+HH:mm;-HH:mm";
    private static final String[][] DEF_OFFSET_PATTERNS = {
        {"+HH:mm:ss"},  // Positive offset with seconds
        {"-HH:mm:ss"},  // Negative offset with seconds
        {"+HH:mm"},     // Positive offset
        {"-HH:mm"}      // Negative offset
    };

    private ULocale locale;
    private MessageFormat msgfmt;
    private SimpleDateFormat dtfmt;

    private String[] gmtPatterns;
    private String[][] offsetPatterns;
    private int[] gmtPrefixLen;

    /**
     * Constructs a GMTFormat for the locale
     * @param loc The locale
     */
    public GMTFormat(ULocale loc) {
        locale = loc;
        
        // Load format patterns for the locale
        String localGmtFormat = null;
        String localHourFormat = null;
        try {
            // Get localized GMT format
            String gmtFormat = ZoneMeta.getTZLocalizationInfo(locale, ZoneMeta.GMT);
            if (!gmtFormat.equals(DEF_GMT_PATTERNS[0])) {
                localGmtFormat = gmtFormat;
            }
            // Get localized hour formats
            String hourFormat = ZoneMeta.getTZLocalizationInfo(locale, ZoneMeta.HOUR);
            if (!hourFormat.equals(DEF_HOUR_FORMAT)) {
                localHourFormat = hourFormat;
            }
        } catch (MissingResourceException mre) {
            // If no locale data is available, use the default one.
        }

        if (localGmtFormat != null) {
            gmtPatterns = new String[2];
            gmtPatterns[0] = localGmtFormat;
            gmtPatterns[1] = DEF_GMT_PATTERNS[0];

            gmtPrefixLen = new int[2];
            // Get prefix length of localized GMT pattern
            // This code ignores quote characters in MessageFormat pattern,
            // but it should work well with existing locale data.
            // The prefix length is used for quick validation in parse method.
            gmtPrefixLen[0] = localGmtFormat.indexOf('{');
            if (gmtPrefixLen[0] < 0) {
                // GMT pattern must contain {0}
                throw new IllegalStateException("Invalid localized GMT format pattern - " + localGmtFormat);
            }
            gmtPrefixLen[1] = DEF_GMT_PREFIX_LEN[0];
        
        } else {
            gmtPatterns = DEF_GMT_PATTERNS;
            gmtPrefixLen = DEF_GMT_PREFIX_LEN;
        }

        if (localHourFormat != null) {
            offsetPatterns = new String[4][2];
            int sepIdx = localHourFormat.indexOf(';');
            if (sepIdx != -1) {
                String posPattern = localHourFormat.substring(0, sepIdx);
                String negPattern = localHourFormat.substring(sepIdx + 1);

                String posPatternS = null, negPatternS = null;

                // CLDR1.5 does not have GMT offset pattern including second field.
                // For now, append "ss" to the end.
                if (posPattern.indexOf(':') != -1) {
                    posPatternS = posPattern + ":ss";
                } else {
                    posPatternS = posPattern + "ss";
                }
                if (negPattern.indexOf(':') != -1) {
                    negPatternS = posPattern + ":ss";
                } else {
                    negPatternS = posPattern + "ss";
                }

                offsetPatterns[0][0] = posPatternS;
                offsetPatterns[1][0] = negPatternS;
                offsetPatterns[2][0] = posPattern;
                offsetPatterns[3][0] = negPattern;

                offsetPatterns[0][1] = DEF_OFFSET_PATTERNS[0][0];
                offsetPatterns[1][1] = DEF_OFFSET_PATTERNS[1][0];
                offsetPatterns[2][1] = DEF_OFFSET_PATTERNS[2][0];
                offsetPatterns[3][1] = DEF_OFFSET_PATTERNS[3][0]; 
            }
        } else {
            offsetPatterns = DEF_OFFSET_PATTERNS;
        }
    }

    /**
     * Constructs a GMTFormat for the SimpleDateFormat.
     * The instance created by this constructor uses the same
     * locale and number format used by the given SimpleDateFormat.
     * @param sdf The SimpleDateFormat, which won't be modified
     * by the instance of GMTFormat
     */
    public GMTFormat(SimpleDateFormat sdf) {
        this(sdf.getLocale(ULocale.ACTUAL_LOCALE));
        dtfmt = (SimpleDateFormat)sdf.clone();
        dtfmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Formats the specified GMT offset
     * @param offset GMT offset in milliseconds
     * @return TimeZone text in localized GMT format
     */
    public String format(int offset) {
        StringBuffer buf = new StringBuffer();
        format(offset, buf, null);
        return buf.toString();
    }

    /**
     * Format the specified GMT offset
     * @param offset GMT offset in milliseconds
     * @param toAppendTo A StringBuffer where the result text it to be appended
     * @param pos A FieldPosition identifying a field
     * @return The StringBuffer passed in as toAppendTo
     */
    public StringBuffer format(int offset, StringBuffer toAppendTo, FieldPosition pos) {
        String offsetPat = null;
        if (offset >= 0) {
            // Positive offset
            offsetPat = offset%(60*1000) == 0 ? offsetPatterns[2][0] : offsetPatterns[0][0];
        } else {
            // Negative offset
            offsetPat = offset%(60*1000) == 0 ? offsetPatterns[3][0] : offsetPatterns[1][0];
            offset = -offset;
        }
        MessageFormat fmt = getFormat(gmtPatterns[0], offsetPat);
        return fmt.format(new Object[] {new Long(offset)}, toAppendTo, pos);
    }

    /**
     * Parse the localized GMT string
     * @param source The localized GMT string defined by UTR#35
     * @return GMT offset parsed from the string
     * @throws ParseException if the beginning of the text cannot be parsed
     */
    public Integer parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Integer result = parse(source, pos);
        if (result == null) {
            throw new ParseException("Invalid GMT format", pos.getErrorIndex());
        }
        return result;
    }

    /**
     * Parse the localized GMT string
     * @param source The localized GMT string defined by UTR#35
     * @param pos On input, the position at which to start parsing.  On output, the position at
     * which parsing terminated or the start position if the parse failed.
     * @return GMT offset parsed from the string
     */
    public Integer parse(String source, ParsePosition pos) {
        Integer result = null;
        int start = pos.getIndex();
        outer: for (int gmtPatIdx = 0; gmtPatIdx < gmtPatterns.length; gmtPatIdx++) {
            if (!source.regionMatches(start, gmtPatterns[gmtPatIdx], 0, gmtPrefixLen[gmtPatIdx])) {
                // Prefix does not match.  We do not even try to set up MessageFormat
                // and invoke parse().
                continue;
            }            
            for (int offsetPatIdx = 0; offsetPatIdx < offsetPatterns.length; offsetPatIdx++) {
                for (int fallbackIdx = 0; fallbackIdx < offsetPatterns[offsetPatIdx].length; fallbackIdx++) {
                    MessageFormat fmt = getFormat(gmtPatterns[gmtPatIdx], offsetPatterns[offsetPatIdx][fallbackIdx]);
                    Object[] parsedObjects = fmt.parse(source, pos);
                    if ((parsedObjects != null) && (parsedObjects[0] instanceof Date)) {
                        int offset = (int)((Date)parsedObjects[0]).getTime();
                        if (offsetPatIdx%2 != 0) {
                            // negative
                            offset = -offset;
                        }
                        result = new Integer(offset);
                        break outer;
                    }
                    // Reset ParsePosition
                    pos.setIndex(start);
                    pos.setErrorIndex(-1);
                }
            }
        }
        if (result == null) {
            pos.setErrorIndex(start);
        }
        return result;
    }

    /*
     * Return MessageFormat configured for the patterns
     */
    private MessageFormat getFormat(String gmtPattern, String hourPattern) {
        if (dtfmt == null) {
            // Create a SimpleDateFormat instance which will be used for
            // formatting/parsing the offset part.
            dtfmt = new SimpleDateFormat(hourPattern, locale);
            dtfmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else if (!hourPattern.equals(dtfmt.toPattern())) {
            dtfmt.applyPattern(hourPattern);
        }

        if (msgfmt == null) {
            msgfmt = new MessageFormat(gmtPattern);
        } else if (!msgfmt.toPattern().equals(gmtPattern)) {
            msgfmt.applyPattern(gmtPattern);
        }
        msgfmt.setFormat(0, dtfmt);
        return msgfmt;
    }
}
