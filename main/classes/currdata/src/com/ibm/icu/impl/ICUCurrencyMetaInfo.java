/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.text.CurrencyMetaInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ICU's currency meta info data.
 */
public class ICUCurrencyMetaInfo extends CurrencyMetaInfo {
    private ICUResourceBundle regionInfo;
    private ICUResourceBundle digitInfo;

    public ICUCurrencyMetaInfo() {
        ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(
            ICUResourceBundle.ICU_CURR_BASE_NAME, "supplementalData",
            ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        regionInfo = bundle.findTopLevel("CurrencyMap");
        digitInfo = bundle.findTopLevel("CurrencyMeta");
    }

    @Override
    public List<CurrencyInfo> currencyInfo(CurrencyFilter filter) {
        return collect(new InfoCollector(), filter);
    }

    @Override
    public List<String> currencies(CurrencyFilter filter) {
        return collect(new CurrencyCollector(), filter);
   }

    @Override
    public List<String> regions(CurrencyFilter filter) {
        return collect(new RegionCollector(), filter);
    }

    @Override
    public CurrencyDigits currencyDigits(String isoCode) {
        ICUResourceBundle b = digitInfo.findWithFallback(isoCode);
        if (b == null) {
            b = digitInfo.findWithFallback("DEFAULT");
        }
        int[] data = b.getIntVector();
        return new CurrencyDigits(data[0], data[1]);
    }

    private <T> List<T> collect(Collector<T> collector, CurrencyFilter filter) {
        // We rely on the fact that the data lists the regions in order, and the
        // priorities in order within region.  This means we don't need
        // to sort the results to ensure the ordering matches the spec.

        if (filter == null) {
            filter = CurrencyFilter.all();
        }
        int needed = collector.collects();
        if (filter.region != null) {
            needed |= Region;
        }
        if (filter.currency != null) {
            needed |= Currency;
        }
        if (filter.from != Long.MIN_VALUE || filter.to != Long.MAX_VALUE) {
            needed |= Date;
        }

        if (needed != 0) {
            if (filter.region != null) {
                ICUResourceBundle b = regionInfo.findWithFallback(filter.region);
                if (b != null) {
                    collectRegion(collector, filter, needed, b);
                }
            } else {
                for (int i = 0; i < regionInfo.getSize(); i++) {
                    collectRegion(collector, filter, needed, regionInfo.at(i));
                }
            }
        }

        return collector.getList();
    }

    private <T> void collectRegion(Collector<T> collector, CurrencyFilter filter,
            int needed, ICUResourceBundle b) {

        String region = b.getKey();
        if ((needed & nonRegion) == 0) {
            collector.collect(b.getKey(), null, 0, 0, -1);
            return;
        }

        for (int i = 0; i < b.getSize(); i++) {
            ICUResourceBundle r = b.at(i);
            if (r.getSize() == 0) {
                // AQ[0] is an empty array instead of a table, so the bundle is null.
                // There's no data here, so we skip this entirely.
                // We'd do a type test, but the ResourceArray type is private.
                continue;
            }
            String currency = null;
            long from = Long.MIN_VALUE;
            long to = Long.MAX_VALUE;

            if ((needed & Currency) != 0) {
                ICUResourceBundle currBundle = r.at("id");
                currency = currBundle.getString();
                if (filter.currency != null && !filter.currency.equals(currency)) {
                    continue;
                }
            }

            if ((needed & Date) != 0) {
                from = getDate(r.at("from"), Long.MIN_VALUE);
                to = getDate(r.at("to"), Long.MAX_VALUE);
                // In the data, to is always > from.  This means that when we have a range
                // from == to, the comparisons below will always do the right thing, despite
                // the range being technically empty.  It really should be [from, from+1) but
                // this way we don't need to fiddle with it.
                if (filter.from >= to) {
                    continue;
                }
                if (filter.to <= from) {
                    continue;
                }
            }

            // data lists elements in priority order, so 'i' suffices
            collector.collect(region, currency, from, to, i);
        }
    }

    private static final long MASK = 4294967295L;
    private long getDate(ICUResourceBundle b, long defaultValue) {
        if (b == null) {
            return defaultValue;
        }
        int[] values = b.getIntVector();
        return ((long) values[0] << 32) | ((long) values[1] & MASK);
    }

    // Utility, just because I don't like the n^2 behavior of using list.contains to build a
    // list of unique items.  If we used java 6 we could use their class for this.
    private static class UniqueList<T> {
        private Set<T> seen = new HashSet<T>();
        private List<T> list = new ArrayList<T>();

        private static <T> UniqueList<T> create() {
            return new UniqueList<T>();
        }

        void add(T value) {
            if (!seen.contains(value)) {
                list.add(value);
                seen.add(value);
            }
        }

        List<T> list() {
            return Collections.unmodifiableList(list);
        }
    }

    private static class InfoCollector implements Collector<CurrencyInfo> {
        // Data is already unique by region/priority, so we don't need to be concerned
        // about duplicates.
        private List<CurrencyInfo> result = new ArrayList<CurrencyInfo>();

        public void collect(String region, String currency, long from, long to, int priority) {
            result.add(new CurrencyInfo(region, currency, from, to, priority));
        }

        public List<CurrencyInfo> getList() {
            return Collections.unmodifiableList(result);
        }

        public int collects() {
            return Region | Currency | Date;
        }
    }

    private static class RegionCollector implements Collector<String> {
        private final UniqueList<String> result = UniqueList.create();

        public void collect(String region, String currency, long from, long to, int priority) {
            result.add(region);
        }

        public int collects() {
            return Region;
        }

        public List<String> getList() {
            return result.list();
        }
    }

    private static class CurrencyCollector implements Collector<String> {
        private final UniqueList<String> result = UniqueList.create();

        public void collect(String region, String currency, long from, long to, int priority) {
            result.add(currency);
        }

        public int collects() {
            return Currency;
        }

        public List<String> getList() {
            return result.list();
        }
    }

    private static final int Region = 1;
    private static final int Currency = 2;
    private static final int Date = 4;

    private static final int nonRegion = Currency | Date;

    private static interface Collector<T> {
        /**
         * A bitmask of Region/Currency/Date indicating which features we collect.
         * @return the bitmask
         */
        int collects();

        /**
         * Called with data passed by filter.  Values not collected by filter should be ignored.
         * @param region the region code (null if ignored)
         * @param currency the currency code (null if ignored)
         * @param from start time (0 if ignored)
         * @param to end time (0 if ignored)
         * @param priority priority (-1 if ignored)
         */
        void collect(String region, String currency, long from, long to, int priority);

        /**
         * Return the list of unique items in the order in which we encountered them for the
         * first time.  The returned list is unmodifiable.
         * @return the list
         */
        List<T> getList();
    }
}
