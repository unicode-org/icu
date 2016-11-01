/*
 *******************************************************************************
 * Copyright (C) 2008-2015, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.Arrays;
import java.util.EnumSet;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;

/**
 * Utility class for returning the plural category for a range of numbers, such as 1–5, so that appropriate messages can
 * be chosen. The rules for determining this value vary widely across locales.
 * 
 * @author markdavis
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public final class PluralRanges implements Freezable<PluralRanges>, Comparable<PluralRanges> {

    private volatile boolean isFrozen;
    private Matrix matrix = new Matrix();
    private boolean[] explicit = new boolean[StandardPlural.COUNT];

    /**
     * Constructor
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public PluralRanges() {
    }

    /**
     * Internal class for mapping from two StandardPluralCategories values to another.
     */
    private static final class Matrix implements Comparable<Matrix>, Cloneable {
        private byte[] data = new byte[StandardPlural.COUNT * StandardPlural.COUNT];
        {
            for (int i = 0; i < data.length; ++i) {
                data[i] = -1;
            }
        }

        Matrix() {
        }

        /**
         * Internal method for setting.
         */
        @SuppressWarnings("unused")
        void set(StandardPlural start, StandardPlural end, StandardPlural result) {
            data[start.ordinal() * StandardPlural.COUNT + end.ordinal()] = result == null ? (byte) -1
                    : (byte) result.ordinal();
        }

        /**
         * Internal method for setting; throws exception if already set.
         */
        void setIfNew(StandardPlural start, StandardPlural end,
                StandardPlural result) {
            byte old = data[start.ordinal() * StandardPlural.COUNT + end.ordinal()];
            if (old >= 0) {
                throw new IllegalArgumentException("Previously set value for <" + start + ", " + end + ", "
                        + StandardPlural.VALUES.get(old) + ">");
            }
            data[start.ordinal() * StandardPlural.COUNT + end.ordinal()] = result == null ? (byte) -1
                    : (byte) result.ordinal();
        }

        /**
         * Internal method for getting.
         */
        StandardPlural get(StandardPlural start, StandardPlural end) {
            byte result = data[start.ordinal() * StandardPlural.COUNT + end.ordinal()];
            return result < 0 ? null : StandardPlural.VALUES.get(result);
        }

        /**
         * Internal method to see if <*,end> values are all the same.
         */
        @SuppressWarnings("unused")
        StandardPlural endSame(StandardPlural end) {
            StandardPlural first = null;
            for (StandardPlural start : StandardPlural.VALUES) {
                StandardPlural item = get(start, end);
                if (item == null) {
                    continue;
                }
                if (first == null) {
                    first = item;
                    continue;
                }
                if (first != item) {
                    return null;
                }
            }
            return first;
        }

        /**
         * Internal method to see if <start,*> values are all the same.
         */
        @SuppressWarnings("unused")
        StandardPlural startSame(StandardPlural start,
                EnumSet<StandardPlural> endDone, Output<Boolean> emit) {
            emit.value = false;
            StandardPlural first = null;
            for (StandardPlural end : StandardPlural.VALUES) {
                StandardPlural item = get(start, end);
                if (item == null) {
                    continue;
                }
                if (first == null) {
                    first = item;
                    continue;
                }
                if (first != item) {
                    return null;
                }
                // only emit if we didn't cover with the 'end' values
                if (!endDone.contains(end)) {
                    emit.value = true;
                }
            }
            return first;
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < data.length; ++i) {
                result = result * 37 + data[i];
            }
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Matrix)) {
                return false;
            }
            return 0 == compareTo((Matrix) other);
        }

        public int compareTo(Matrix o) {
            for (int i = 0; i < data.length; ++i) {
                int diff = data[i] - o.data[i];
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }

        @Override
        public Matrix clone() {
            Matrix result = new Matrix();
            result.data = data.clone();
            return result;
        }
        
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (StandardPlural i : StandardPlural.values()) {
                for (StandardPlural j : StandardPlural.values()) {
                    StandardPlural x = get(i, j);
                    if (x != null) {
                        result.append(i + " & " + j + " → " + x + ";\n");
                    }
                }
            }
            return result.toString();
        }
    }

    /**
     * Internal method for building. If the start or end are null, it means everything of that type.
     * 
     * @param rangeStart
     *            plural category for the start of the range
     * @param rangeEnd
     *            plural category for the end of the range
     * @param result
     *            the resulting plural category
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void add(StandardPlural rangeStart, StandardPlural rangeEnd,
            StandardPlural result) {
        if (isFrozen) {
            throw new UnsupportedOperationException();
        }
        explicit[result.ordinal()] = true;
        if (rangeStart == null) {
            for (StandardPlural rs : StandardPlural.values()) {
                if (rangeEnd == null) {
                    for (StandardPlural re : StandardPlural.values()) {
                        matrix.setIfNew(rs, re, result);
                    }
                } else {
                    explicit[rangeEnd.ordinal()] = true;
                    matrix.setIfNew(rs, rangeEnd, result);
                }
            }
        } else if (rangeEnd == null) {
            explicit[rangeStart.ordinal()] = true;
            for (StandardPlural re : StandardPlural.values()) {
                matrix.setIfNew(rangeStart, re, result);
            }
        } else {
            explicit[rangeStart.ordinal()] = true;
            explicit[rangeEnd.ordinal()] = true;
            matrix.setIfNew(rangeStart, rangeEnd, result);
        }
    }

    /**
     * Returns the appropriate plural category for a range from start to end. If there is no available data, then
     * 'end' is returned as an implicit value. (Such an implicit value can be tested for with {@link #isExplicit}.)
     * 
     * @param start
     *            plural category for the start of the range
     * @param end
     *            plural category for the end of the range
     * @return the resulting plural category, or 'end' if there is no data.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public StandardPlural get(StandardPlural start, StandardPlural end) {
        StandardPlural result = matrix.get(start, end);
        return result == null ? end : result;
    }

    /**
     * Returns whether the appropriate plural category for a range from start to end
     * is explicitly in the data (vs given an implicit value). See also {@link #get}.
     * 
     * @param start
     *            plural category for the start of the range
     * @param end
     *            plural category for the end of the range
     * @return whether the value for (start,end) is explicit or not.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isExplicit(StandardPlural start, StandardPlural end) {
        return matrix.get(start, end) != null;
    }

    /**
     * Internal method to determines whether the StandardPluralCategories was explicitly used in any add statement.
     * 
     * @param count
     *            plural category to test
     * @return true if set
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isExplicitlySet(StandardPlural count) {
        return explicit[count.ordinal()];
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PluralRanges)) {
            return false;
        }
        PluralRanges otherPR = (PluralRanges)other;
        return matrix.equals(otherPR.matrix) && Arrays.equals(explicit, otherPR.explicit);
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public int hashCode() {
        return matrix.hashCode();
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int compareTo(PluralRanges that) {
        return matrix.compareTo(that.matrix);
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public PluralRanges freeze() {
        isFrozen = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public PluralRanges cloneAsThawed() {
        PluralRanges result = new PluralRanges();
        result.explicit = explicit.clone();
        result.matrix = matrix.clone();
        return result;
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public String toString() {
        return matrix.toString();
    }
}