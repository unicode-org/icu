/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

/**
 * Provides information about currencies that is not specific to a locale.
 * @draft ICU 4.4
 */
public class CurrencyMetaInfo {
    private static final CurrencyMetaInfo impl;
    private static final boolean hasData;

    /**
     * Returns the unique instance of the currency meta info.
     * @return the meta info
     * @draft ICU 4.4
     */
    public static CurrencyMetaInfo getInstance() {
        return impl;
    }

    /**
     * Returns true if there is data for the currency meta info.
     * @return true if there is actual data
     * @draft ICU 4.4
     */
    public static boolean hasData() {
        return hasData;
    }

    /**
     * Subclass constructor.
     * @internal
     */
    protected CurrencyMetaInfo() {
    }

    /**
     * A filter used to select which currency info is returned.
     * @draft ICU 4.4
     */
    public static final class CurrencyFilter {
        /**
         * The region to filter on.  If null, accepts any region.
         * @draft ICU 4.4
         */
        public final String region;

        /**
         * The currency to filter on.  If null, accepts any currency.
         * @draft ICU 4.4
         */
        public final String currency;

        /**
         * The from date to filter on (milliseconds).  Accepts any currency on or after this date.
         * @draft ICU 4.4
         */
        public final long from;

        /**
         * The to date to filter on (milliseconds).  Accepts any currency on or before this date.
         * @draft ICU 4.4
         */
        public final long to;

        private CurrencyFilter(String region, String currency, long from, long to) {
            this.region = region;
            this.currency = currency;
            this.from = from;
            this.to = to;
        }

        private CurrencyFilter(String region, String currency, Date dateFrom, Date dateTo) {
            this.region = region;
            this.currency = currency;
            this.from = dateFrom == null ? Long.MIN_VALUE : dateFrom.getTime();
            this.to = dateTo == null ? Long.MAX_VALUE : dateTo.getTime();
        }

        private static final CurrencyFilter ALL = new CurrencyFilter(null, null, null, null);

        /**
         * Returns a filter that accepts all currency data.
         * @return a filter
         * @draft ICU 4.4
         */
        public static CurrencyFilter all() {
            return ALL;
        }

        /**
         * Returns a filter that accepts all currencies in use as of the current date.
         * @return a filter
         * @see #withDate(Date)
         * @draft ICU 4.4
         */
        public static CurrencyFilter now() {
            return ALL.withDate(new Date());
        }

        /**
         * Returns a filter that accepts all currencies ever used in the given region.
         * @param region the region code
         * @return a filter
         * @see #withRegion(String)
         * @draft ICU 4.4
         */
        public static CurrencyFilter onRegion(String region) {
            return ALL.withRegion(region);
        }

        /**
         * Returns a filter that accepts the given currency.
         * @param currency the currency code
         * @return a filter
         * @see #withCurrency(String)
         * @draft ICU 4.4
         */
        public static CurrencyFilter onCurrency(String currency) {
            return ALL.withCurrency(currency);
        }

        /**
         * Returns a filter that accepts all currencies in use on the given date.
         * @param date the date
         * @return a filter
         * @see #withDate(Date)
         * @draft ICU 4.4
         */
        public static CurrencyFilter onDate(Date date) {
            return ALL.withDate(date);
        }

        /**
         * Returns a filter that accepts all currencies that were in use at some point between
         * the given dates, or if dates are equal, currencies in use on that date.
         * @param from date on or after a currency must have been in use
         * @param to date before which a currency must have been in use, or if equal to from,
         * the date on which a currency must have been in use
         * @return a filter
         * @see #withRange(Date, Date)
         * @draft ICU 4.4
         */
        public static CurrencyFilter onRange(Date from, Date to) {
            return ALL.withRange(from, to);
        }

        /**
         * Returns a copy of this filter, with the specified region.  Region can be null to
         * indicate no filter on region.
         * @param region the region code
         * @return the filter
         * @see #onRegion(String)
         * @draft ICU 4.4
         */
        public CurrencyFilter withRegion(String region) {
            return new CurrencyFilter(region, this.currency, this.from, this.to);
        }

        /**
         * Returns a copy of this filter, with the specified currency.  Currency can be null to
         * indicate no filter on currency.
         * @param currency the currency code
         * @return the filter
         * @see #onCurrency(String)
         * @draft ICU 4.4
         */
        public CurrencyFilter withCurrency(String currency) {
            return new CurrencyFilter(this.region, currency, this.from, this.to);
        }

        /**
         * Returns a copy of this filter, with from and to set to the given date.
         * @param date the date on which the currency must have been in use
         * @return the filter
         * @see #onDate(Date)
         * @draft ICU 4.4
         */
        public CurrencyFilter withDate(Date date) {
            return new CurrencyFilter(this.region, this.currency, date, date);
        }

        /**
         * Returns a copy of this filter, with from and to set to the given dates.
         * @param from date on or after which the currency must have been in use
         * @param to date before which the currency must have been in use
         * @return the filter
         * @see #onRange(Date, Date)
         * @draft ICU 4.4
         */
        public CurrencyFilter withRange(Date from, Date to) {
            return new CurrencyFilter(this.region, this.currency, from, to);
        }

        @Override
        public boolean equals(Object rhs) {
            return rhs instanceof CurrencyFilter &&
                equals((CurrencyFilter) rhs);
        }

        /**
         * Type-safe override of {@link #equals(Object)}.
         * @param rhs the currency filter to compare to
         * @return true if the filters are equal
         * @draft ICU 4.4
         */
        public boolean equals(CurrencyFilter rhs) {
            return this == rhs || (rhs != null &&
                    equals(this.region, rhs.region) &&
                    equals(this.currency, rhs.currency) &&
                    this.from == rhs.from &&
                    this.to == rhs.to);
        }

        @Override
        public int hashCode() {
            int hc = 0;
            if (region != null) {
                hc = region.hashCode();
            }
            if (currency != null) {
                hc = hc * 31 + currency.hashCode();
            }
            hc = hc * 31 + (int) from;
            hc = hc * 31 + (int) (from >>> 32);
            hc = hc * 31 + (int) to;
            hc = hc * 31 + (int) (to >>> 32);
            return hc;
        }

        /**
         * Return a string representing the filter, for debugging.
         */
        @Override
        public String toString() {
            return debugString(this);
        }

        private static boolean equals(String lhs, String rhs) {
            return lhs == rhs || (lhs != null && lhs.equals(rhs));
        }
    }

    /**
     * Represents the raw information about fraction digits and rounding increment.
     * @draft ICU 4.4
     */
    public static final class CurrencyDigits {
        /**
         * Number of fraction digits used to display this currency.
         * @draft ICU 4.4
         */
        public final byte fractionDigits;
        /**
         * Rounding increment used when displaying this currency.
         * @draft ICU 4.4
         */
        public final byte roundingIncrement;

        /**
         * Constructor for CurrencyDigits.
         * @param fractionDigits the fraction digits
         * @param roundingIncrement the rounding increment
         * @draft ICU 4.4
         */
        public CurrencyDigits(int fractionDigits, int roundingIncrement) {
            this.fractionDigits = (byte) fractionDigits;
            this.roundingIncrement = (byte) roundingIncrement;
        }

        /**
         * Returns a string representing the currency digits, for debugging.
         */
        @Override
        public String toString() {
            return debugString(this);
        }
    }

    /**
     * Represents a complete currency info record listing the region, currency, from and to dates,
     * and priority.
     * @draft ICU 4.4
     */
    public static final class CurrencyInfo {
        /**
         * Region code where currency is used.
         * @draft ICU 4.4
         */
        public final String region;

        /**
         * The three-letter ISO currency code.
         * @draft ICU 4.4
         */
        public final String code;

        /**
         * Date on which the currency was first officially used in the region.  If there is no
         * date, this is Long.MIN_VALUE;
         * @draft ICU 4.4
         */
        public final long from;

        /**
         * Date at which the currency stopped being officially used in the region.  If there is
         * no date, this is Long.MAX_VALUE;
         * @draft ICU 4.4
         */
        public final long to;

        /**
         * Preference order of currencies being used at the same time in the region.  Lower
         * values are preferred (generally, this is a transition from an older to a newer
         * currency).  Priorities within a single country are unique.
         * @draft ICU 4.4
         */
        public final short priority;

        /**
         * Constructs a currency info.
         * @param region region code
         * @param code currency code
         * @param from start date in milliseconds
         * @param to end date in milliseconds
         * @param priority priority value, 0 is highest priority, increasing values are lower
         * @draft ICU 4.4
         */
        public CurrencyInfo(String region, String code, long from, long to, int priority) {
            this.region = region;
            this.code = code;
            this.from = from;
            this.to = to;
            this.priority = (short) priority;
        }

        /**
         * Returns a string useful for debugging.
         */
        @Override
        public String toString() {
            return debugString(this);
        }
    }

    /**
     * Returns the list of CurrencyInfos matching the provided filter.  Results
     * are ordered by country code,  then by highest to lowest priority (0 is highest).
     * The returned list is unmodifiable.
     * @param filter the filter to control which currency info to return
     * @return the matching information
     * @draft ICU 4.4
     */
    public List<CurrencyInfo> currencyInfo(CurrencyFilter filter) {
        return Collections.emptyList();
    }

    /**
     * Returns the list of currency codes matching the provided filter.
     * Results are ordered as in {@link #currencyInfo(CurrencyFilter)}.
     * The returned list is unmodifiable.
     * @param filter the filter to control which currencies to return.  If filter is null,
     * returns all currencies for which information is available.
     * @return the matching currency codes
     * @draft ICU 4.4
     */
    public List<String> currencies(CurrencyFilter filter) {
        return Collections.emptyList();
   }

    /**
     * Returns the list of region codes matching the provided filter.
     * Results are ordered as in {@link #currencyInfo(CurrencyFilter)}.
     * The returned list is unmodifiable.
     * @param filter the filter to control which regions to return.  If filter is null,
     * returns all regions for which information is available.
     * @return the matching region codes
     * @draft ICU 4.4
     */
    public List<String> regions(CurrencyFilter filter) {
        return Collections.emptyList();
    }

    /**
     * Returns the CurrencyDigits for the currency code.
     * @param isoCode the currency code
     * @return the CurrencyDigits
     * @draft ICU 4.4
     */
    public CurrencyDigits currencyDigits(String isoCode) {
        return defaultDigits;
    }

    /** @internal */
    protected static final CurrencyDigits defaultDigits = new CurrencyDigits(2, 0);

    static {
        CurrencyMetaInfo temp = null;
        boolean tempHasData = false;
        try {
            Class<?> clzz = Class.forName("com.ibm.icu.impl.ICUCurrencyMetaInfo");
            temp = (CurrencyMetaInfo) clzz.newInstance();
            tempHasData = true;
        } catch (Throwable t) {
            temp = new CurrencyMetaInfo();
        }
        impl = temp;
        hasData = tempHasData;
    }

    private static String dateString(long date) {
        if (date == Long.MAX_VALUE || date == Long.MIN_VALUE) {
            return null;
        }
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(date);
        return "" + gc.get(Calendar.YEAR) + '-' + (gc.get(Calendar.MONTH) + 1) + '-' +
                gc.get(Calendar.DAY_OF_MONTH);
    }

    private static String debugString(Object o) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Field f : o.getClass().getFields()) {
                Object v = f.get(o);
                if (v != null) {
                    String s;
                    if (v instanceof Date) {
                        s = dateString(((Date)v).getTime());
                    } else if (v instanceof Long) {
                        s = dateString(((Long)v).longValue());
                    } else {
                        s = String.valueOf(v);
                    }
                    if (s == null) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(f.getName())
                        .append("='")
                        .append(s)
                        .append("'");
                }
            }
        } catch (Throwable t) {
        }
        sb.insert(0, o.getClass().getSimpleName() + "(");
        sb.append(")");
        return sb.toString();
    }
}
