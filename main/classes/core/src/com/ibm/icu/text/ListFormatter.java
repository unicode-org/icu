/*
 *******************************************************************************
 * Copyright (C) 2012-2014, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.SimplePatternFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Immutable class for formatting a list, using data from CLDR (or supplied
 * separately). The class is not subclassable.
 *
 * @author Mark Davis
 * @stable ICU 50
 */
final public class ListFormatter {
    private final SimplePatternFormatter two;
    private final SimplePatternFormatter start;
    private final SimplePatternFormatter middle;
    private final SimplePatternFormatter end;
    private final ULocale locale;
    
    /**
     * Indicates the style of Listformatter
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public enum Style {
        /**
         * Standard style.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        STANDARD("standard"),
        /**
         * Style for full durations
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        DURATION("unit"),
        /**
         * Style for durations in abbrevated form
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        DURATION_SHORT("unit-short"),
        /**
         * Style for durations in narrow form
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        DURATION_NARROW("unit-narrow");
        
        private final String name;
        
        Style(String name) {
            this.name = name;
        }
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public String getName() {
            return name;
        }
        
    }

    /**
     * <b>Internal:</b> Create a ListFormatter from component strings,
     * with definitions as in LDML.
     *
     * @param two
     *            string for two items, containing {0} for the first, and {1}
     *            for the second.
     * @param start
     *            string for the start of a list items, containing {0} for the
     *            first, and {1} for the rest.
     * @param middle
     *            string for the start of a list items, containing {0} for the
     *            first part of the list, and {1} for the rest of the list.
     * @param end
     *            string for the end of a list items, containing {0} for the
     *            first part of the list, and {1} for the last item.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public ListFormatter(String two, String start, String middle, String end) {
        this(
                SimplePatternFormatter.compile(two),
                SimplePatternFormatter.compile(start),
                SimplePatternFormatter.compile(middle),
                SimplePatternFormatter.compile(end),
                null);
        
    }
    
    private ListFormatter(SimplePatternFormatter two, SimplePatternFormatter start, SimplePatternFormatter middle, SimplePatternFormatter end, ULocale locale) {
        this.two = two;
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.locale = locale;
    }

    /**
     * Create a list formatter that is appropriate for a locale.
     *
     * @param locale
     *            the locale in question.
     * @return ListFormatter
     * @stable ICU 50
     */
    public static ListFormatter getInstance(ULocale locale) {
      return getInstance(locale, Style.STANDARD);
    }

    /**
     * Create a list formatter that is appropriate for a locale.
     *
     * @param locale
     *            the locale in question.
     * @return ListFormatter
     * @stable ICU 50
     */
    public static ListFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), Style.STANDARD);
    }
    
    /**
     * Create a list formatter that is appropriate for a locale and style.
     *
     * @param locale the locale in question.
     * @param style the style
     * @return ListFormatter
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static ListFormatter getInstance(ULocale locale, Style style) {
        return cache.get(locale, style.getName());
    }

    /**
     * Create a list formatter that is appropriate for the default FORMAT locale.
     *
     * @return ListFormatter
     * @stable ICU 50
     */
    public static ListFormatter getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    /**
     * Format a list of objects.
     *
     * @param items
     *            items to format. The toString() method is called on each.
     * @return items formatted into a string
     * @stable ICU 50
     */
    public String format(Object... items) {
        return format(Arrays.asList(items));
    }

    /**
     * Format a collection of objects. The toString() method is called on each.
     *
     * @param items
     *            items to format. The toString() method is called on each.
     * @return items formatted into a string
     * @stable ICU 50
     */
    public String format(Collection<?> items) {
        return format(items, -1).toString();
    }
    
    // Formats a collection of objects and returns the formatted string plus the offset
    // in the string where the index th element appears. index is zero based. If index is
    // negative or greater than or equal to the size of items then this function returns -1 for
    // the offset.
    FormattedListBuilder format(Collection<?> items, int index) {
        Iterator<?> it = items.iterator();
        int count = items.size();
        switch (count) {
        case 0:
            return new FormattedListBuilder("", false);
        case 1:
            return new FormattedListBuilder(it.next(), index == 0);
        case 2:
            return new FormattedListBuilder(it.next(), index == 0).append(two, it.next(), index == 1);
        }
        FormattedListBuilder builder = new FormattedListBuilder(it.next(), index == 0);
        builder.append(start, it.next(), index == 1);
        for (int idx = 2; idx < count - 1; ++idx) {
            builder.append(middle, it.next(), index == idx);
        }
        return builder.append(end, it.next(), index == count - 1);
    }
    
    /**
     * Returns the pattern to use for a particular item count.
     * @param count the item count.
     * @return the pattern with {0}, {1}, {2}, etc. For English,
     * getPatternForNumItems(3) == "{0}, {1}, and {2}"
     * @throws IllegalArgumentException when count is 0 or negative.
     * @stable ICU 52
     */
    public String getPatternForNumItems(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            list.add(String.format("{%d}", i));
        }
        return format(list);
    }
    
    /**
     * Returns the locale of this object.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public ULocale getLocale() {
        return locale;
    }
    
    // Builds a formatted list
    static class FormattedListBuilder {
        private StringBuilder current;
        private int offset;
        
        // Start is the first object in the list; If recordOffset is true, records the offset of
        // this first object.
        public FormattedListBuilder(Object start, boolean recordOffset) {
            this.current = new StringBuilder(start.toString());
            this.offset = recordOffset ? 0 : -1;
        }
        
        // Appends additional object. pattern is a template indicating where the new object gets
        // added in relation to the rest of the list. {0} represents the rest of the list; {1}
        // represents the new object in pattern. next is the object to be added. If recordOffset
        // is true, records the offset of next in the formatted string.
        public FormattedListBuilder append(SimplePatternFormatter pattern, Object next, boolean recordOffset) {
            if (pattern.getPlaceholderCount() != 2) {
                throw new IllegalArgumentException("Need {0} and {1} only in pattern " + pattern);
            }
           int[] offsets = (recordOffset || offsetRecorded()) ? new int[2] : null;
           pattern.formatAndReplace(
                   current, offsets, current, next.toString());
           if (offsets != null) {
               if (offsets[0] == -1 || offsets[1] == -1) {
                   throw new IllegalArgumentException(
                           "{0} or {1} missing from pattern " + pattern);
               }
               if (recordOffset) {
                   offset = offsets[1];
               } else {
                   offset += offsets[0];
               }
           }
           return this;
        }

        @Override
        public String toString() {
            return current.toString();
        }
        
        // Gets the last recorded offset or -1 if no offset recorded.
        public int getOffset() {
            return offset;
        }
        
        private boolean offsetRecorded() {
            return offset >= 0;
        }
    }

    /** JUST FOR DEVELOPMENT */
    // For use with the hard-coded data
    // TODO Replace by use of RB
    // Verify in building that all of the patterns contain {0}, {1}.

    static Map<ULocale, ListFormatter> localeToData = new HashMap<ULocale, ListFormatter>();
    static void add(String locale, String...data) {
        localeToData.put(new ULocale(locale), new ListFormatter(data[0], data[1], data[2], data[3]));
    }

    private static class Cache {
        private final ICUCache<String, ListFormatter> cache =
            new SimpleCache<String, ListFormatter>();

        public ListFormatter get(ULocale locale, String style) {
            String key = String.format("%s:%s", locale.toString(), style);
            ListFormatter result = cache.get(key);
            if (result == null) {
                result = load(locale, style);
                cache.put(key, result);
            }
            return result;
        }

        private static ListFormatter load(ULocale ulocale, String style) {
            ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                    getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
           
            return new ListFormatter(
                SimplePatternFormatter.compile(r.getWithFallback("listPattern/" + style + "/2").getString()),
                SimplePatternFormatter.compile(r.getWithFallback("listPattern/" + style + "/start").getString()),
                SimplePatternFormatter.compile(r.getWithFallback("listPattern/" + style + "/middle").getString()),
                SimplePatternFormatter.compile(r.getWithFallback("listPattern/" + style + "/end").getString()),
                ulocale);
        }
    }

    static Cache cache = new Cache();
}
