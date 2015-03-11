/*
 *******************************************************************************
 * Copyright (C) 2013-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.EnumMap;
import java.util.Locale;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;


/**
 * Formats simple relative dates. There are two types of relative dates that
 * it handles:
 * <ul>
 *   <li>relative dates with a quantity e.g "in 5 days"</li>
 *   <li>relative dates without a quantity e.g "next Tuesday"</li>
 * </ul>
 * <p>
 * This API is very basic and is intended to be a building block for more
 * fancy APIs. The caller tells it exactly what to display in a locale
 * independent way. While this class automatically provides the correct plural
 * forms, the grammatical form is otherwise as neutral as possible. It is the
 * caller's responsibility to handle cut-off logic such as deciding between
 * displaying "in 7 days" or "in 1 week." This API supports relative dates
 * involving one single unit. This API does not support relative dates
 * involving compound units.
 * e.g "in 5 days and 4 hours" nor does it support parsing.
 * This class is both immutable and thread-safe.
 * <p>
 * Here are some examples of use:
 * <blockquote>
 * <pre>
 * RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance();
 * fmt.format(1, Direction.NEXT, RelativeUnit.DAYS); // "in 1 day"
 * fmt.format(3, Direction.NEXT, RelativeUnit.DAYS); // "in 3 days"
 * fmt.format(3.2, Direction.LAST, RelativeUnit.YEARS); // "3.2 years ago"
 * 
 * fmt.format(Direction.LAST, AbsoluteUnit.SUNDAY); // "last Sunday"
 * fmt.format(Direction.THIS, AbsoluteUnit.SUNDAY); // "this Sunday"
 * fmt.format(Direction.NEXT, AbsoluteUnit.SUNDAY); // "next Sunday"
 * fmt.format(Direction.PLAIN, AbsoluteUnit.SUNDAY); // "Sunday"
 * 
 * fmt.format(Direction.LAST, AbsoluteUnit.DAY); // "yesterday"
 * fmt.format(Direction.THIS, AbsoluteUnit.DAY); // "today"
 * fmt.format(Direction.NEXT, AbsoluteUnit.DAY); // "tomorrow"
 * 
 * fmt.format(Direction.PLAIN, AbsoluteUnit.NOW); // "now"
 * </pre>
 * </blockquote>
 * <p>
 * In the future, we may add more forms, such as abbreviated/short forms
 * (3 secs ago), and relative day periods ("yesterday afternoon"), etc.
 * 
 * @stable ICU 53
 */
public final class RelativeDateTimeFormatter {
    
    /**
     * The formatting style
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     *
     */
    public static enum Style {
        
        /**
         * Everything spelled out.
         * @draft ICU 54
         * @provisional This API might change or be removed in a future release.
         */
        LONG,
        
        /**
         * Abbreviations used when possible.
         * @draft ICU 54
         * @provisional This API might change or be removed in a future release.
         */
        SHORT,
        
        /**
         * Use single letters when possible.
         * @draft ICU 54
         * @provisional This API might change or be removed in a future release.
         */
        NARROW,
    }
    
    /**
     * Represents the unit for formatting a relative date. e.g "in 5 days"
     * or "in 3 months"
     * @stable ICU 53
     */
    public static enum RelativeUnit {
        
        /**
         * Seconds
         * @stable ICU 53
         */
        SECONDS,
        
        /**
         * Minutes
         * @stable ICU 53
         */
        MINUTES,
        
       /**
        * Hours
        * @stable ICU 53
        */
        HOURS,
        
        /**
         * Days
         * @stable ICU 53
         */
        DAYS,
        
        /**
         * Weeks
         * @stable ICU 53
         */
        WEEKS,
        
        /**
         * Months
         * @stable ICU 53
         */
        MONTHS,
        
        /**
         * Years
         * @stable ICU 53
         */
        YEARS,
    }
    
    /**
     * Represents an absolute unit.
     * @stable ICU 53
     */
    public static enum AbsoluteUnit {
        
       /**
        * Sunday
        * @stable ICU 53
        */
        SUNDAY,
        
        /**
         * Monday
         * @stable ICU 53
         */
        MONDAY,
        
        /**
         * Tuesday
         * @stable ICU 53
         */
        TUESDAY,
        
        /**
         * Wednesday
         * @stable ICU 53
         */
        WEDNESDAY,
        
        /**
         * Thursday
         * @stable ICU 53
         */
        THURSDAY,
        
        /**
         * Friday
         * @stable ICU 53
         */
        FRIDAY,
        
        /**
         * Saturday
         * @stable ICU 53
         */
        SATURDAY,
        
        /**
         * Day
         * @stable ICU 53
         */
        DAY,
        
        /**
         * Week
         * @stable ICU 53
         */
        WEEK,
        
        /**
         * Month
         * @stable ICU 53
         */
        MONTH,
        
        /**
         * Year
         * @stable ICU 53
         */
        YEAR,
        
        /**
         * Now
         * @stable ICU 53
         */
        NOW,
      }

      /**
       * Represents a direction for an absolute unit e.g "Next Tuesday"
       * or "Last Tuesday"
       * @stable ICU 53
       */
      public static enum Direction {
          
          /**
           * Two before. Not fully supported in every locale
           * @stable ICU 53
           */
          LAST_2,

          /**
           * Last
           * @stable ICU 53
           */  
          LAST,

          /**
           * This
           * @stable ICU 53
           */
          THIS,

          /**
           * Next
           * @stable ICU 53
           */
          NEXT,

          /**
           * Two after. Not fully supported in every locale
           * @stable ICU 53
           */
          NEXT_2,

          /**
           * Plain, which means the absence of a qualifier
           * @stable ICU 53
           */
          PLAIN;
      }

    /**
     * Returns a RelativeDateTimeFormatter for the default locale.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance() {
        return getInstance(ULocale.getDefault(), null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular locale.
     * 
     * @param locale the locale.
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale) {
        return getInstance(locale, null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular JDK locale.
     * 
     * @param locale the JDK locale.
     * @return An instance of RelativeDateTimeFormatter.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular locale that uses a particular
     * NumberFormat object.
     * 
     * @param locale the locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class. 
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale, NumberFormat nf) {
        return getInstance(locale, nf, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }
 
    /**
     * Returns a RelativeDateTimeFormatter for a particular locale that uses a particular
     * NumberFormat object, style, and capitalization context
     * 
     * @param locale the locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class. May be null.
     * @param style the style.
     * @param capitalizationContext the capitalization context.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance(
            ULocale locale,
            NumberFormat nf,
            Style style,
            DisplayContext capitalizationContext) {
        RelativeDateTimeFormatterData data = cache.get(locale);
        if (nf == null) {
            nf = NumberFormat.getInstance(locale);
        } else {
            nf = (NumberFormat) nf.clone();
        }
        return new RelativeDateTimeFormatter(
                data.qualitativeUnitMap,
                data.quantitativeUnitMap,
                new MessageFormat(data.dateTimePattern),
                PluralRules.forLocale(locale),
                nf,
                style,
                capitalizationContext,
                capitalizationContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ?
                    BreakIterator.getSentenceInstance(locale) : null,
                locale);
                
    }
           
    /**
     * Returns a RelativeDateTimeFormatter for a particular JDK locale that uses a particular
     * NumberFormat object.
     * 
     * @param locale the JDK locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class. 
     * @return An instance of RelativeDateTimeFormatter.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance(Locale locale, NumberFormat nf) {
        return getInstance(ULocale.forLocale(locale), nf);
    }

    /**
     * Formats a relative date with a quantity such as "in 5 days" or
     * "3 months ago"
     * @param quantity The numerical amount e.g 5. This value is formatted
     * according to this object's {@link NumberFormat} object.
     * @param direction NEXT means a future relative date; LAST means a past
     * relative date.
     * @param unit the unit e.g day? month? year?
     * @return the formatted string
     * @throws IllegalArgumentException if direction is something other than
     * NEXT or LAST.
     * @stable ICU 53
     */
    public String format(double quantity, Direction direction, RelativeUnit unit) {
        if (direction != Direction.LAST && direction != Direction.NEXT) {
            throw new IllegalArgumentException("direction must be NEXT or LAST");
        }
        String result;
        // This class is thread-safe, yet numberFormat is not. To ensure thread-safety of this
        // class we must guarantee that only one thread at a time uses our numberFormat.
        synchronized (numberFormat) {
            result = getQuantity(
                    unit, direction == Direction.NEXT).format(
                            quantity, numberFormat, pluralRules);
        }
        return adjustForContext(result);
    }
    

    /**
     * Formats a relative date without a quantity.
     * @param direction NEXT, LAST, THIS, etc.
     * @param unit e.g SATURDAY, DAY, MONTH
     * @return the formatted string. If direction has a value that is documented as not being
     *  fully supported in every locale (for example NEXT_2 or LAST_2) then this function may
     *  return null to signal that no formatted string is available.
     * @throws IllegalArgumentException if the direction is incompatible with
     * unit this can occur with NOW which can only take PLAIN.
     * @stable ICU 53
     */
    public String format(Direction direction, AbsoluteUnit unit) {
        if (unit == AbsoluteUnit.NOW && direction != Direction.PLAIN) {
            throw new IllegalArgumentException("NOW can only accept direction PLAIN.");
        }
        String result = this.qualitativeUnitMap.get(style).get(unit).get(direction);
        return result != null ? adjustForContext(result) : null;
    }

    /**
     * Combines a relative date string and a time string in this object's
     * locale. This is done with the same date-time separator used for the
     * default calendar in this locale.
     * @param relativeDateString the relative date e.g 'yesterday'
     * @param timeString the time e.g '3:45'
     * @return the date and time concatenated according to the default
     * calendar in this locale e.g 'yesterday, 3:45'
     * @stable ICU 53
     */
    public String combineDateAndTime(String relativeDateString, String timeString) {
        return this.combinedDateAndTime.format(
            new Object[]{timeString, relativeDateString}, new StringBuffer(), null).toString();
    }
    
    /**
     * Returns a copy of the NumberFormat this object is using.
     * @return A copy of the NumberFormat.
     * @stable ICU 53
     */
    public NumberFormat getNumberFormat() {
        // This class is thread-safe, yet numberFormat is not. To ensure thread-safety of this
        // class we must guarantee that only one thread at a time uses our numberFormat.
        synchronized (numberFormat) {
            return (NumberFormat) numberFormat.clone();
        }
    }
    
    /**
     * Return capitalization context.
     *
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public DisplayContext getCapitalizationContext() {
        return capitalizationContext;
    }

    /**
     * Return style
     *
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public Style getFormatStyle() {
        return style;
    }
    
    private String adjustForContext(String originalFormattedString) {
        if (breakIterator == null || originalFormattedString.length() == 0 
                || !UCharacter.isLowerCase(UCharacter.codePointAt(originalFormattedString, 0))) {
            return originalFormattedString;
        }
        synchronized (breakIterator) {
            return UCharacter.toTitleCase(
                    locale,
                    originalFormattedString,
                    breakIterator,
                    UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
        }
    }
    
    private static void addQualitativeUnit(
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnits,
            AbsoluteUnit unit,
            String current) {
        EnumMap<Direction, String> unitStrings =
                new EnumMap<Direction, String>(Direction.class);
        unitStrings.put(Direction.PLAIN, current);
        qualitativeUnits.put(unit,  unitStrings);       
    }

    private static void addQualitativeUnit(
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnits,
            AbsoluteUnit unit, ICUResourceBundle bundle, String plain) {
        EnumMap<Direction, String> unitStrings =
                new EnumMap<Direction, String>(Direction.class);
        unitStrings.put(Direction.LAST, bundle.getStringWithFallback("-1"));
        unitStrings.put(Direction.THIS, bundle.getStringWithFallback("0"));
        unitStrings.put(Direction.NEXT, bundle.getStringWithFallback("1"));
        addOptionalDirection(unitStrings, Direction.LAST_2, bundle, "-2");
        addOptionalDirection(unitStrings, Direction.NEXT_2, bundle, "2");
        unitStrings.put(Direction.PLAIN, plain);
        qualitativeUnits.put(unit,  unitStrings);
    }
 
    private static void addOptionalDirection(
            EnumMap<Direction, String> unitStrings,
            Direction direction,
            ICUResourceBundle bundle,
            String key) {
        String s = bundle.findStringWithFallback(key);
        if (s != null) {
            unitStrings.put(direction, s);
        }
    }

    private RelativeDateTimeFormatter(
            EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap,
            EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap,
            MessageFormat combinedDateAndTime,
            PluralRules pluralRules,
            NumberFormat numberFormat,
            Style style,
            DisplayContext capitalizationContext,
            BreakIterator breakIterator,
            ULocale locale) {
        this.qualitativeUnitMap = qualitativeUnitMap;
        this.quantitativeUnitMap = quantitativeUnitMap;
        this.combinedDateAndTime = combinedDateAndTime;
        this.pluralRules = pluralRules;
        this.numberFormat = numberFormat;
        this.style = style;
        if (capitalizationContext.type() != DisplayContext.Type.CAPITALIZATION) {
            throw new IllegalArgumentException(capitalizationContext.toString());
        }
        this.capitalizationContext = capitalizationContext;
        this.breakIterator = breakIterator;
        this.locale = locale;
    }
    
    private QuantityFormatter getQuantity(RelativeUnit unit, boolean isFuture) {
        QuantityFormatter[] quantities = quantitativeUnitMap.get(style).get(unit);
        return isFuture ? quantities[1] : quantities[0];
    }
    
    private final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
    private final EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap;
    private final MessageFormat combinedDateAndTime;
    private final PluralRules pluralRules;
    private final NumberFormat numberFormat;
    private final Style style;
    private final DisplayContext capitalizationContext;
    private final BreakIterator breakIterator;
    private final ULocale locale;
    
    private static class RelativeDateTimeFormatterData {
        public RelativeDateTimeFormatterData(
                EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap,
                EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap,
                String dateTimePattern) {
            this.qualitativeUnitMap = qualitativeUnitMap;
            this.quantitativeUnitMap = quantitativeUnitMap;
            this.dateTimePattern = dateTimePattern;
        }
        
        public final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
        public final EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap;
        public final String dateTimePattern;  // Example: "{1}, {0}"
    }
    
    private static class Cache {
        private final ICUCache<String, RelativeDateTimeFormatterData> cache =
            new SimpleCache<String, RelativeDateTimeFormatterData>();

        public RelativeDateTimeFormatterData get(ULocale locale) {
            String key = locale.toString();
            RelativeDateTimeFormatterData result = cache.get(key);
            if (result == null) {
                result = new Loader(locale).load();
                cache.put(key, result);
            }
            return result;
        }
    }
    
    private static class Loader {
        private final ULocale ulocale;
        
        public Loader(ULocale ulocale) {
            this.ulocale = ulocale;
        }

        public RelativeDateTimeFormatterData load() {
            EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap = 
                    new EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>>(Style.class);
            
            EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap =
                    new EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>>(Style.class);
            
            for (Style style : Style.values()) {
                qualitativeUnitMap.put(style, new EnumMap<AbsoluteUnit, EnumMap<Direction, String>>(AbsoluteUnit.class));
                quantitativeUnitMap.put(style, new EnumMap<RelativeUnit, QuantityFormatter[]>(RelativeUnit.class));                
            }
                    
            ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                    getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
            addTimeUnits(
                    r,
                    "fields/day", "fields/day-short", "fields/day-narrow",
                    RelativeUnit.DAYS,
                    AbsoluteUnit.DAY,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnits(
                    r,
                    "fields/week", "fields/week-short", "fields/week-narrow",
                    RelativeUnit.WEEKS,
                    AbsoluteUnit.WEEK,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnits(
                    r,
                    "fields/month", "fields/month-short", "fields/month-narrow",
                    RelativeUnit.MONTHS,
                    AbsoluteUnit.MONTH,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnits(
                    r,
                    "fields/year", "fields/year-short", "fields/year-narrow",
                    RelativeUnit.YEARS,
                    AbsoluteUnit.YEAR,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            initRelativeUnits(
                    r,
                    "fields/second", "fields/second-short", "fields/second-narrow",
                    RelativeUnit.SECONDS,
                    quantitativeUnitMap);
            initRelativeUnits(
                    r,
                    "fields/minute", "fields/minute-short", "fields/minute-narrow",
                    RelativeUnit.MINUTES,
                    quantitativeUnitMap);
            initRelativeUnits(
                    r,
                    "fields/hour", "fields/hour-short", "fields/hour-narrow",
                    RelativeUnit.HOURS,
                    quantitativeUnitMap);
            
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.LONG),
                    AbsoluteUnit.NOW,
                    r.getStringWithFallback("fields/second/relative/0"));
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.SHORT),
                    AbsoluteUnit.NOW,
                    r.getStringWithFallback("fields/second-short/relative/0"));
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.NARROW),
                    AbsoluteUnit.NOW,
                    r.getStringWithFallback("fields/second-narrow/relative/0"));
            
            EnumMap<Style, EnumMap<AbsoluteUnit, String>> dayOfWeekMap = 
                    new EnumMap<Style, EnumMap<AbsoluteUnit, String>>(Style.class);
            dayOfWeekMap.put(Style.LONG, readDaysOfWeek(
                    r.getWithFallback("calendar/gregorian/dayNames/stand-alone/wide")));
            dayOfWeekMap.put(Style.SHORT, readDaysOfWeek(
                    r.getWithFallback("calendar/gregorian/dayNames/stand-alone/short")));
            dayOfWeekMap.put(Style.NARROW, readDaysOfWeek(
                    r.getWithFallback("calendar/gregorian/dayNames/stand-alone/narrow")));
            
            addWeekDays(
                    r,
                    "fields/mon/relative",
                    "fields/mon-short/relative",
                    "fields/mon-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.MONDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/tue/relative",
                    "fields/tue-short/relative",
                    "fields/tue-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.TUESDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/wed/relative",
                    "fields/wed-short/relative",
                    "fields/wed-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.WEDNESDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/thu/relative",
                    "fields/thu-short/relative",
                    "fields/thu-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.THURSDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/fri/relative",
                    "fields/fri-short/relative",
                    "fields/fri-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.FRIDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/sat/relative",
                    "fields/sat-short/relative",
                    "fields/sat-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.SATURDAY,
                    qualitativeUnitMap);
            addWeekDays(
                    r,
                    "fields/sun/relative",
                    "fields/sun-short/relative",
                    "fields/sun-narrow/relative",
                    dayOfWeekMap,
                    AbsoluteUnit.SUNDAY,
                    qualitativeUnitMap);   
            CalendarData calData = new CalendarData(
                    ulocale, r.getStringWithFallback("calendar/default"));  
            return new RelativeDateTimeFormatterData(
                    qualitativeUnitMap, quantitativeUnitMap, calData.getDateTimePattern());
        }

        private void addTimeUnits(
                ICUResourceBundle r,
                String path, String pathShort, String pathNarrow,
                RelativeUnit relativeUnit, 
                AbsoluteUnit absoluteUnit,
                EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap,
                EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap) {
           addTimeUnit(
                   r.getWithFallback(path),
                   relativeUnit,
                   absoluteUnit,
                   quantitativeUnitMap.get(Style.LONG),
                   qualitativeUnitMap.get(Style.LONG));
           addTimeUnit(
                   r.getWithFallback(pathShort),
                   relativeUnit,
                   absoluteUnit,
                   quantitativeUnitMap.get(Style.SHORT),
                   qualitativeUnitMap.get(Style.SHORT));
           addTimeUnit(
                   r.getWithFallback(pathNarrow),
                   relativeUnit,
                   absoluteUnit,
                   quantitativeUnitMap.get(Style.NARROW),
                   qualitativeUnitMap.get(Style.NARROW));
            
        }

        private void addTimeUnit(
                ICUResourceBundle timeUnitBundle,
                RelativeUnit relativeUnit,
                AbsoluteUnit absoluteUnit,
                EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap,
                EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap) {
            addTimeUnit(timeUnitBundle, relativeUnit, quantitativeUnitMap);
            String unitName = timeUnitBundle.getStringWithFallback("dn");
            // TODO(Travis Keep): This is a hack to get around CLDR bug 6818.
            if (ulocale.getLanguage().equals("en")) {
                unitName = unitName.toLowerCase();
            }
            timeUnitBundle = timeUnitBundle.getWithFallback("relative");
            addQualitativeUnit(
                    qualitativeUnitMap,
                    absoluteUnit,
                    timeUnitBundle,
                    unitName);
        }
        
        private void initRelativeUnits(
                ICUResourceBundle r, 
                String path,
                String pathShort,
                String pathNarrow,
                RelativeUnit relativeUnit,
                EnumMap<Style, EnumMap<RelativeUnit, QuantityFormatter[]>> quantitativeUnitMap) {
            addTimeUnit(
                    r.getWithFallback(path),
                    relativeUnit,
                    quantitativeUnitMap.get(Style.LONG));
            addTimeUnit(
                    r.getWithFallback(pathShort),
                    relativeUnit,
                    quantitativeUnitMap.get(Style.SHORT));
            addTimeUnit(
                    r.getWithFallback(pathNarrow),
                    relativeUnit,
                    quantitativeUnitMap.get(Style.NARROW));
        }

        private static void addTimeUnit(
                ICUResourceBundle timeUnitBundle,
                RelativeUnit relativeUnit,
                EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap) {
            QuantityFormatter.Builder future = new QuantityFormatter.Builder();
            QuantityFormatter.Builder past = new QuantityFormatter.Builder();
            timeUnitBundle = timeUnitBundle.getWithFallback("relativeTime");
            addTimeUnit(
                    timeUnitBundle.getWithFallback("future"),
                    future);
            addTimeUnit(
                    timeUnitBundle.getWithFallback("past"),
                    past);
            quantitativeUnitMap.put(
                    relativeUnit, new QuantityFormatter[] { past.build(), future.build() });
        }

        private static void addTimeUnit(
                ICUResourceBundle pastOrFuture, QuantityFormatter.Builder builder) {
            int size = pastOrFuture.getSize();
            for (int i = 0; i < size; i++) {
                UResourceBundle r = pastOrFuture.get(i);
                builder.add(r.getKey(), r.getString());
            }
        }
        
        private void addWeekDays(
                ICUResourceBundle r,
                String path,
                String pathShort,
                String pathNarrow,
                EnumMap<Style, EnumMap<AbsoluteUnit, String>> dayOfWeekMap,
                AbsoluteUnit weekDay,
                EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap) {
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.LONG),
                    weekDay,
                    r.findWithFallback(path),
                    dayOfWeekMap.get(Style.LONG).get(weekDay)); 
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.SHORT),
                    weekDay,
                    r.findWithFallback(pathShort),
                    dayOfWeekMap.get(Style.SHORT).get(weekDay)); 
            addQualitativeUnit(
                    qualitativeUnitMap.get(Style.NARROW),
                    weekDay,
                    r.findWithFallback(pathNarrow),
                    dayOfWeekMap.get(Style.NARROW).get(weekDay)); 
            
        }

        private static EnumMap<AbsoluteUnit, String> readDaysOfWeek(ICUResourceBundle daysOfWeekBundle) {
            EnumMap<AbsoluteUnit, String> dayOfWeekMap = new EnumMap<AbsoluteUnit, String>(AbsoluteUnit.class);
            if (daysOfWeekBundle.getSize() != 7) {
                throw new IllegalStateException(String.format("Expect 7 days in a week, got %d", daysOfWeekBundle.getSize()));
            }
            // Sunday always comes first in CLDR data.
            int idx = 0;
            dayOfWeekMap.put(AbsoluteUnit.SUNDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.MONDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.TUESDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.WEDNESDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.THURSDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.FRIDAY, daysOfWeekBundle.getString(idx++));
            dayOfWeekMap.put(AbsoluteUnit.SATURDAY, daysOfWeekBundle.getString(idx++));
            return dayOfWeekMap;
        }
    }

    private static final Cache cache = new Cache();
}
