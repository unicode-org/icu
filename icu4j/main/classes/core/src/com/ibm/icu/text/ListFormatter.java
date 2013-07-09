/*
 *******************************************************************************
 * Copyright (C) 2012-2013, Google, International Business Machines Corporation and
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
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Immutable class for formatting a list, using data from CLDR (or supplied
 * separately). The class is not subclassable.
 *
 * @author Mark Davis
 * @draft ICU 50
 * @provisional This API might change or be removed in a future release.
 */
final public class ListFormatter {
    private final String two;
    private final String start;
    private final String middle;
    private final String end;
    private final ULocale locale;

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
     */
    public ListFormatter(String two, String start, String middle, String end) {
        this(two, start, middle, end, null);
    }
    
    private ListFormatter(String two, String start, String middle, String end, ULocale locale) {
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
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public static ListFormatter getInstance(ULocale locale) {
      return cache.get(locale);
    }

    /**
     * Create a list formatter that is appropriate for a locale.
     *
     * @param locale
     *            the locale in question.
     * @return ListFormatter
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public static ListFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Create a list formatter that is appropriate for the default FORMAT locale.
     *
     * @return ListFormatter
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public String format(Collection<?> items) {
        // TODO optimize this for the common case that the patterns are all of the
        // form {0}<sometext>{1}.
        // We avoid MessageFormat, because there is no "sub" formatting.
        Iterator<?> it = items.iterator();
        int count = items.size();
        switch (count) {
        case 0:
            return "";
        case 1:
            return it.next().toString();
        case 2:
            return format2(two, it.next(), it.next());
        }
        String result = it.next().toString();
        result = format2(start, result, it.next());
        for (count -= 3; count > 0; --count) {
            result = format2(middle, result, it.next());
        }
        return format2(end, result, it.next());
    }
    
    /**
     * Returns the pattern to use for a particular item count.
     * @param count the item count.
     * @return the pattern with {0}, {1}, {2}, etc.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String createPatternForNumItems(int count) {
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
    public ULocale getLocale() {
        return locale;
    }

    private String format2(String pattern, Object a, Object b) {
        int i0 = pattern.indexOf("{0}");
        int i1 = pattern.indexOf("{1}");
        if (i0 < 0 || i1 < 0) {
            throw new IllegalArgumentException("Missing {0} or {1} in pattern " + pattern);
        }
        if (i0 < i1) {
            return pattern.substring(0, i0) + a + pattern.substring(i0+3, i1) + b + pattern.substring(i1+3);
        } else {
            return pattern.substring(0, i1) + b + pattern.substring(i1+3, i0) + a + pattern.substring(i0+3);
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
        private final ICUCache<ULocale, ListFormatter> cache =
            new SimpleCache<ULocale, ListFormatter>();

        public ListFormatter get(ULocale locale) {
            ListFormatter result = cache.get(locale);
            if (result == null) {
                result = load(locale);
                cache.put(locale, result);
            }
            return result;
        }

        private static ListFormatter load(ULocale ulocale) {
            ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                    getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
            r = r.getWithFallback("listPattern/standard");
            return new ListFormatter(
                r.getWithFallback("2").getString(),
                r.getWithFallback("start").getString(),
                r.getWithFallback("middle").getString(),
                r.getWithFallback("end").getString(),
                ulocale);
        }
    }

    static Cache cache = new Cache();
}
