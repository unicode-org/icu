package com.ibm.text;

/**
 * <code>UnicodeFilterLogic</code> provides logical operators on
 * {@link UnicodeFilter} objects.  This class cannot be instantiated;
 * it consists only of static methods.  The static methods return
 * filter objects that perform logical inversion (<tt>not</tt>),
 * intersection (<tt>and</tt>), or union (<tt>or</tt>) of the given
 * filter objects.
 */
public final class UnicodeFilterLogic {

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
     * the given filter.
     */
    public static UnicodeFilter not(final UnicodeFilter f) {
        return new UnicodeFilter() {
            public boolean isIn(char c) {
                return !f.isIn(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the two given filters.  That is,
     * if <tt>f.isIn()</tt> is <tt>false</tt>, then <tt>g.isIn()</tt>
     * is not called, and <tt>isIn()</tt> returns <tt>false</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    public static UnicodeFilter and(final UnicodeFilter f,
                                    final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new UnicodeFilter() {
            public boolean isIn(char c) {
                return f.isIn(c) && g.isIn(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the given filters.  That is, if
     * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
     * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>isIn()</tt> returns <tt>false</tt>.
     */
    public static UnicodeFilter and(final UnicodeFilter[] f) {
        return new UnicodeFilter() {
            public boolean isIn(char c) {
                for (int i=0; i<f.length; ++i) {
                    if (!f[i].isIn(c)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the two given filters.  That is, if
     * <tt>f.isIn()</tt> is <tt>true</tt>, then <tt>g.isIn()</tt> is
     * not called, and <tt>isIn()</tt> returns <tt>true</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    public static UnicodeFilter or(final UnicodeFilter f,
                                   final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new UnicodeFilter() {
            public boolean isIn(char c) {
                return f.isIn(c) || g.isIn(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the given filters.  That is, if
     * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
     * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>isIn()</tt> returns <tt>true</tt>.
     */
    public static UnicodeFilter or(final UnicodeFilter[] f) {
        return new UnicodeFilter() {
            public boolean isIn(char c) {
                for (int i=0; i<f.length; ++i) {
                    if (f[i].isIn(c)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    // TODO: Add nand() & nor() for convenience, if needed.
}
