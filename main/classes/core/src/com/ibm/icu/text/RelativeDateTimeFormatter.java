/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.EnumMap;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
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
 * @draft ICU 53
 * @provisional This API might change or be removed in a future release.
 */
public final class RelativeDateTimeFormatter {
    
    /**
     * Represents the unit for formatting a relative date. e.g "in 5 days"
     * or "in 3 months"
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static enum RelativeUnit {
        
        /**
         * Seconds
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        SECONDS,
        
        /**
         * Minutes
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        MINUTES,
        
       /**
        * Hours
        * @draft ICU 53
        * @provisional This API might change or be removed in a future release.
        */
        HOURS,
        
        /**
         * Days
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        DAYS,
        
        /**
         * Weeks
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        WEEKS,
        
        /**
         * Months
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        MONTHS,
        
        /**
         * Years
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        YEARS,
    }
    
    /**
     * Represents an absolute unit.
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static enum AbsoluteUnit {
        
       /**
        * Sunday
        * @draft ICU 53
        * @provisional This API might change or be removed in a future release.
        */
        SUNDAY,
        
        /**
         * Monday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        MONDAY,
        
        /**
         * Tuesday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        TUESDAY,
        
        /**
         * Wednesday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        WEDNESDAY,
        
        /**
         * Thursday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        THURSDAY,
        
        /**
         * Friday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        FRIDAY,
        
        /**
         * Saturday
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        SATURDAY,
        
        /**
         * Day
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        DAY,
        
        /**
         * Week
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        WEEK,
        
        /**
         * Month
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        MONTH,
        
        /**
         * Year
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        YEAR,
        
        /**
         * Now
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        NOW,
      }

      /**
       * Represents a direction for an absolute unit e.g "Next Tuesday"
       * or "Last Tuesday"
       * @draft ICU 53
       * @provisional This API might change or be removed in a future release.
       */
      public static enum Direction {
          
          /**
           * Two before. Not fully supported in every locale
           * @draft ICU 53
           * @provisional This API might change or be removed in a future release.
           */
          LAST_2,

          
          /**
           * Last
           * @draft ICU 53
           * @provisional This API might change or be removed in a future release.
           */  
        LAST,
        
        /**
         * This
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        THIS,
        
        /**
         * Next
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        NEXT,
        
        /**
         * Two after. Not fully supported in every locale
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        NEXT_2,
        
        /**
         * Plain, which means the absence of a qualifier
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        PLAIN;
      }
    
    /**
     * Returns a RelativeDateTimeFormatter for the default locale.
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance() {
        return getInstance(ULocale.getDefault());
    }
    
    /**
     * Returns a RelativeDateTimeFormatter for a particular locale.
     * 
     * @param locale the locale.
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale) {
        RelativeDateTimeFormatterData data = cache.get(locale);
        return new RelativeDateTimeFormatter(
                data.qualitativeUnitMap,
                data.quantitativeUnitMap,
                new MessageFormat(data.dateTimePattern),
                PluralRules.forLocale(locale),
                NumberFormat.getInstance(locale));
    }
    
    /**
     * Returns a RelativeDateTimeFormatter for a particular locale that uses a particular
     * NumberFormat object.
     * 
     * @param locale the locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class. 
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale, NumberFormat nf) {
        RelativeDateTimeFormatterData data = cache.get(locale);
        return new RelativeDateTimeFormatter(
                data.qualitativeUnitMap,
                data.quantitativeUnitMap,
                new MessageFormat(data.dateTimePattern),
                PluralRules.forLocale(locale),
                (NumberFormat) nf.clone());
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
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public String format(double quantity, Direction direction, RelativeUnit unit) {
        if (direction != Direction.LAST && direction != Direction.NEXT) {
            throw new IllegalArgumentException("direction must be NEXT or LAST");
        }
        // This class is thread-safe, yet numberFormat is not. To ensure thread-safety of this
        // class we must guarantee that only one thread at a time uses our numberFormat.
        synchronized (numberFormat) {
            return getQuantity(
                    unit, direction == Direction.NEXT).format(
                            quantity, numberFormat, pluralRules);
        }
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
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public String format(Direction direction, AbsoluteUnit unit) {
        if (unit == AbsoluteUnit.NOW && direction != Direction.PLAIN) {
            throw new IllegalArgumentException("NOW can only accept direction PLAIN.");
        }
        return this.qualitativeUnitMap.get(unit).get(direction);
    }

    /**
     * Combines a relative date string and a time string in this object's
     * locale. This is done with the same date-time separator used for the
     * default calendar in this locale.
     * @param relativeDateString the relative date e.g 'yesterday'
     * @param timeString the time e.g '3:45'
     * @return the date and time concatenated according to the default
     * calendar in this locale e.g 'yesterday, 3:45'
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public String combineDateAndTime(String relativeDateString, String timeString) {
        return this.combinedDateAndTime.format(
            new Object[]{timeString, relativeDateString}, new StringBuffer(), null).toString();
    }
    
    /**
     * Returns a copy of the NumberFormat this object is using.
     * 
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public NumberFormat getNumberFormat() {
        // This class is thread-safe, yet numberFormat is not. To ensure thread-safety of this
        // class we must guarantee that only one thread at a time uses our numberFormat.
        synchronized (numberFormat) {
            return (NumberFormat) numberFormat.clone();
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
        bundle = bundle.findWithFallback(key);
        if (bundle != null) {
            unitStrings.put(direction, bundle.getString());
        }
    }

    private RelativeDateTimeFormatter(
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap,
            EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap,
            MessageFormat combinedDateAndTime,
            PluralRules pluralRules,
            NumberFormat numberFormat) {
        this.qualitativeUnitMap = qualitativeUnitMap;
        this.quantitativeUnitMap = quantitativeUnitMap;
        this.combinedDateAndTime = combinedDateAndTime;
        this.pluralRules = pluralRules;
        this.numberFormat = numberFormat;
    }
    
    private QuantityFormatter getQuantity(RelativeUnit unit, boolean isFuture) {
        QuantityFormatter[] quantities = quantitativeUnitMap.get(unit);
        return isFuture ? quantities[1] : quantities[0];
    }
    
    private final EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap;
    private final EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap;
    private final MessageFormat combinedDateAndTime;
    private final PluralRules pluralRules;
    private NumberFormat numberFormat;
    
    private static class RelativeDateTimeFormatterData {
        public RelativeDateTimeFormatterData(
                EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap,
                EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap,
                String dateTimePattern) {
            this.qualitativeUnitMap = qualitativeUnitMap;
            this.quantitativeUnitMap = quantitativeUnitMap;
            this.dateTimePattern = dateTimePattern;
        }
        
        public final EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap;
        public final EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap;
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
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap = 
                    new EnumMap<AbsoluteUnit, EnumMap<Direction, String>>(AbsoluteUnit.class);
            
            EnumMap<RelativeUnit, QuantityFormatter[]> quantitativeUnitMap =
                    new EnumMap<RelativeUnit, QuantityFormatter[]>(RelativeUnit.class);
                    
            ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                    getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
            addTimeUnit(
                    r.getWithFallback("fields/day"),
                    RelativeUnit.DAYS,
                    AbsoluteUnit.DAY,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/week"),
                    RelativeUnit.WEEKS,
                    AbsoluteUnit.WEEK,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/month"),
                    RelativeUnit.MONTHS,
                    AbsoluteUnit.MONTH,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/year"),
                    RelativeUnit.YEARS,
                    AbsoluteUnit.YEAR,
                    quantitativeUnitMap,
                    qualitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/second"),
                    RelativeUnit.SECONDS,
                    quantitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/minute"),
                    RelativeUnit.MINUTES,
                    quantitativeUnitMap);
            addTimeUnit(
                    r.getWithFallback("fields/hour"),
                    RelativeUnit.HOURS,
                    quantitativeUnitMap);
            addQualitativeUnit(
                    qualitativeUnitMap,
                    AbsoluteUnit.NOW,
                    r.getStringWithFallback("fields/second/relative/0"));
            
            EnumMap<AbsoluteUnit, String> dayOfWeekMap = readDaysOfWeek(
                r.getWithFallback("calendar/gregorian/dayNames/stand-alone/wide")
            );
            
            addWeekDay(
                    r.getWithFallback("fields/mon"),
                    dayOfWeekMap,
                    AbsoluteUnit.MONDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/tue"),
                    dayOfWeekMap,
                    AbsoluteUnit.TUESDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/wed"),
                    dayOfWeekMap,
                    AbsoluteUnit.WEDNESDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/thu"),
                    dayOfWeekMap,
                    AbsoluteUnit.THURSDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/fri"),
                    dayOfWeekMap,
                    AbsoluteUnit.FRIDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/sat"),
                    dayOfWeekMap,
                    AbsoluteUnit.SATURDAY,
                    qualitativeUnitMap);
            addWeekDay(
                    r.getWithFallback("fields/sun"),
                    dayOfWeekMap,
                    AbsoluteUnit.SUNDAY,
                    qualitativeUnitMap);   
            CalendarData calData = new CalendarData(
                    ulocale, r.getStringWithFallback("calendar/default"));  
            return new RelativeDateTimeFormatterData(
                    qualitativeUnitMap, quantitativeUnitMap, calData.getDateTimePattern());
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

        private static void addWeekDay(
                ICUResourceBundle weekdayBundle,
                EnumMap<AbsoluteUnit, String> dayOfWeekMap,
                AbsoluteUnit weekDay,
                EnumMap<AbsoluteUnit, EnumMap<Direction, String>> qualitativeUnitMap) {
            weekdayBundle = weekdayBundle.findWithFallback("relative");
            addQualitativeUnit(
                    qualitativeUnitMap,
                    weekDay,
                    weekdayBundle,
                    dayOfWeekMap.get(weekDay));
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
