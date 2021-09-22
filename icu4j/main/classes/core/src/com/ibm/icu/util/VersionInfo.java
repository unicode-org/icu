// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to store version numbers of the form major.minor.milli.micro.
 * @author synwee
 * @stable ICU 2.6
 */
public final class VersionInfo implements Comparable<VersionInfo>
{
    // public data members -------------------------------------------------

    /**
     * Unicode 1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_0;
    /**
     * Unicode 1.0.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_0_1;
    /**
     * Unicode 1.1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_1_0;
    /**
     * Unicode 1.1.5 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_1_1_5;
    /**
     * Unicode 2.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_0;
    /**
     * Unicode 2.1.2 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_2;
    /**
     * Unicode 2.1.5 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_5;
    /**
     * Unicode 2.1.8 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_8;
    /**
     * Unicode 2.1.9 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_2_1_9;
    /**
     * Unicode 3.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_0;
    /**
     * Unicode 3.0.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_0_1;
    /**
     * Unicode 3.1.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_1_0;
    /**
     * Unicode 3.1.1 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_1_1;
    /**
     * Unicode 3.2 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_3_2;

    /**
     * Unicode 4.0 version
     * @stable ICU 2.6
     */
    public static final VersionInfo UNICODE_4_0;

    /**
     * Unicode 4.0.1 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_4_0_1;

    /**
     * Unicode 4.1 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_4_1;

    /**
     * Unicode 5.0 version
     * @stable ICU 3.4
     */
    public static final VersionInfo UNICODE_5_0;

    /**
     * Unicode 5.1 version
     * @stable ICU 4.2
     */
    public static final VersionInfo UNICODE_5_1;

    /**
     * Unicode 5.2 version
     * @stable ICU 4.4
     */
    public static final VersionInfo UNICODE_5_2;

    /**
     * Unicode 6.0 version
     * @stable ICU 4.6
     */
    public static final VersionInfo UNICODE_6_0;

    /**
     * Unicode 6.1 version
     * @stable ICU 49
     */
    public static final VersionInfo UNICODE_6_1;

    /**
     * Unicode 6.2 version
     * @stable ICU 50
     */
    public static final VersionInfo UNICODE_6_2;

    /**
     * Unicode 6.3 version
     * @stable ICU 52
     */
    public static final VersionInfo UNICODE_6_3;

    /**
     * Unicode 7.0 version
     * @stable ICU 54
     */
    public static final VersionInfo UNICODE_7_0;

    /**
     * Unicode 8.0 version
     * @stable ICU 56
     */
    public static final VersionInfo UNICODE_8_0;

    /**
     * Unicode 9.0 version
     * @stable ICU 58
     */
    public static final VersionInfo UNICODE_9_0;

    /**
     * Unicode 10.0 version
     * @stable ICU 60
     */
    public static final VersionInfo UNICODE_10_0;

    /**
     * Unicode 11.0 version
     * @stable ICU 62
     */
    public static final VersionInfo UNICODE_11_0;

    /**
     * Unicode 12.0 version
     * @stable ICU 64
     */
    public static final VersionInfo UNICODE_12_0;

    /**
     * Unicode 12.1 version
     * @stable ICU 64
     */
    public static final VersionInfo UNICODE_12_1;

    /**
     * Unicode 13.0 version
     * @stable ICU 66
     */
    public static final VersionInfo UNICODE_13_0;

    /**
     * Unicode 14.0 version
     * @stable ICU 70
     */
    public static final VersionInfo UNICODE_14_0;

    /**
     * ICU4J current release version
     * @stable ICU 2.8
     */
    public static final VersionInfo ICU_VERSION;

    /**
     * Data version string for ICU's internal data.
     * Used for appending to data path (e.g. icudt43b)
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final String ICU_DATA_VERSION_PATH = "70b";

    /**
     * Data version in ICU4J.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final VersionInfo ICU_DATA_VERSION;

    /**
     * Collation runtime version (sort key generator, string comparisons).
     * If the version is different, sort keys for the same string could be different.
     * This value may change in subsequent releases of ICU.
     * @stable ICU 2.8
     */
    public static final VersionInfo UCOL_RUNTIME_VERSION;

    /**
     * Collation builder code version.
     * When this is different, the same tailoring might result
     * in assigning different collation elements to code points.
     * This value may change in subsequent releases of ICU.
     * @stable ICU 2.8
     */
    public static final VersionInfo UCOL_BUILDER_VERSION;

    /**
     * Constant version 1.
     * This was intended to be the version of collation tailorings,
     * but instead the tailoring data carries a version number.
     * @deprecated ICU 54
     */
    @Deprecated
    public static final VersionInfo UCOL_TAILORINGS_VERSION;


    // public methods ------------------------------------------------------

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param version version String in the format of "major.minor.milli.micro"
     *                or "major.minor.milli" or "major.minor" or "major",
     *                where major, minor, milli, micro are non-negative numbers
     *                &lt;= 255. If the trailing version numbers are
     *                not specified they are taken as 0s. E.g. Version "3.1" is
     *                equivalent to "3.1.0.0".
     * @return an instance of VersionInfo with the argument version.
     * @exception IllegalArgumentException when the argument version
     *                is not in the right format
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(String version)
    {
        int length  = version.length();
        int array[] = {0, 0, 0, 0};
        int count   = 0;
        int index   = 0;

        while (count < 4 && index < length) {
            char c = version.charAt(index);
            if (c == '.') {
                count ++;
            }
            else {
                c -= '0';
                if (c < 0 || c > 9) {
                    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
                }
                array[count] *= 10;
                array[count] += c;
            }
            index ++;
        }
        if (index != length) {
            throw new IllegalArgumentException(
                                               "Invalid version number: String '" + version + "' exceeds version format");
        }
        for (int i = 0; i < 4; i ++) {
            if (array[i] < 0 || array[i] > 255) {
                throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
            }
        }

        return getInstance(array[0], array[1], array[2], array[3]);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param major major version, non-negative number &lt;= 255.
     * @param minor minor version, non-negative number &lt;= 255.
     * @param milli milli version, non-negative number &lt;= 255.
     * @param micro micro version, non-negative number &lt;= 255.
     * @exception IllegalArgumentException when either arguments are negative or &gt; 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor, int milli,
                                          int micro)
    {
        // checks if it is in the hashmap
        // else
        if (major < 0 || major > 255 || minor < 0 || minor > 255 ||
            milli < 0 || milli > 255 || micro < 0 || micro > 255) {
            throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
        }
        int     version = getInt(major, minor, milli, micro);
        Integer key     = Integer.valueOf(version);
        VersionInfo  result  = MAP_.get(key);
        if (result == null) {
            result = new VersionInfo(version);
            VersionInfo tmpvi = MAP_.putIfAbsent(key, result);
            if (tmpvi != null) {
                result = tmpvi;
            }
        }
        return result;
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, minor, milli, 0).
     * @param major major version, non-negative number &lt;= 255.
     * @param minor minor version, non-negative number &lt;= 255.
     * @param milli milli version, non-negative number &lt;= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or &gt; 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor, int milli)
    {
        return getInstance(major, minor, milli, 0);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, minor, 0, 0).
     * @param major major version, non-negative number &lt;= 255.
     * @param minor minor version, non-negative number &lt;= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or &gt; 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor)
    {
        return getInstance(major, minor, 0, 0);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * Equivalent to getInstance(major, 0, 0, 0).
     * @param major major version, non-negative number &lt;= 255.
     * @exception IllegalArgumentException when either arguments are
     *                                     negative or &gt; 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major)
    {
        return getInstance(major, 0, 0, 0);
    }

    /**
     * Returns the String representative of VersionInfo in the format of
     * "major.minor.milli.micro"
     * @return String representative of VersionInfo
     * @stable ICU 2.6
     */
    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder(7);
        result.append(getMajor());
        result.append('.');
        result.append(getMinor());
        result.append('.');
        result.append(getMilli());
        result.append('.');
        result.append(getMicro());
        return result.toString();
    }

    /**
     * Returns the major version number
     * @return the major version number
     * @stable ICU 2.6
     */
    public int getMajor()
    {
        return (m_version_ >> 24) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the minor version number
     * @return the minor version number
     * @stable ICU 2.6
     */
    public int getMinor()
    {
        return (m_version_ >> 16) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the milli version number
     * @return the milli version number
     * @stable ICU 2.6
     */
    public int getMilli()
    {
        return (m_version_ >> 8) & LAST_BYTE_MASK_ ;
    }

    /**
     * Returns the micro version number
     * @return the micro version number
     * @stable ICU 2.6
     */
    public int getMicro()
    {
        return m_version_ & LAST_BYTE_MASK_ ;
    }

    /**
     * Checks if this version information is equals to the argument version
     * @param other object to be compared
     * @return true if other is equals to this object's version information,
     *         false otherwise
     * @stable ICU 2.6
     */
    @Override
    public boolean equals(Object other)
    {
        return other == this;
    }

    /**
     * Returns the hash code value for this set.
     *
     * @return the hash code value for this set.
     * @see java.lang.Object#hashCode()
     * @stable ICU 2.6
     */
    @Override
    public int hashCode() {
        return m_version_;
    }

    /**
     * Compares other with this VersionInfo.
     * @param other VersionInfo to be compared
     * @return 0 if the argument is a VersionInfo object that has version
     *           information equals to this object.
     *           Less than 0 if the argument is a VersionInfo object that has
     *           version information greater than this object.
     *           Greater than 0 if the argument is a VersionInfo object that
     *           has version information less than this object.
     * @stable ICU 2.6
     */
    @Override
    public int compareTo(VersionInfo other)
    {
        // m_version_ is an int, a signed 32-bit integer.
        // When the major version is >=128, then the version int is negative.
        // Compare it in two steps to simulate an unsigned-int comparison.
        // (Alternatively we could turn each int into a long and reset the upper 32 bits.)
        // Compare the upper bits first, using logical shift right (unsigned).
        int diff = (m_version_ >>> 1) - (other.m_version_ >>> 1);
        if (diff != 0) { return diff; }
        // Compare the remaining bits.
        return (m_version_ & 1) - (other.m_version_ & 1);
    }

    // private data members ----------------------------------------------

    /**
     * Unicode data version used by the current release.
     * Defined here privately for printing by the main() method in this class.
     * Should be the same as {@link com.ibm.icu.lang.UCharacter#getUnicodeVersion()}
     * which gets the version number from a data file.
     * We do not want VersionInfo to have an import dependency on UCharacter.
     */
    private static final VersionInfo UNICODE_VERSION;

    /**
     * Version number stored as a byte for each of the major, minor, milli and
     * micro numbers in the 32 bit int.
     * Most significant for the major and the least significant contains the
     * micro numbers.
     */
    private int m_version_;
    /**
     * Map of singletons
     */
    private static final ConcurrentHashMap<Integer, VersionInfo> MAP_ = new ConcurrentHashMap<>();
    /**
     * Last byte mask
     */
    private static final int LAST_BYTE_MASK_ = 0xFF;
    /**
     * Error statement string
     */
    private static final String INVALID_VERSION_NUMBER_ =
        "Invalid version number: Version number may be negative or greater than 255";

    // static declaration ------------------------------------------------

    /**
     * Initialize versions only after MAP_ has been created
     */
    static {
        UNICODE_1_0   = getInstance(1, 0, 0, 0);
        UNICODE_1_0_1 = getInstance(1, 0, 1, 0);
        UNICODE_1_1_0 = getInstance(1, 1, 0, 0);
        UNICODE_1_1_5 = getInstance(1, 1, 5, 0);
        UNICODE_2_0   = getInstance(2, 0, 0, 0);
        UNICODE_2_1_2 = getInstance(2, 1, 2, 0);
        UNICODE_2_1_5 = getInstance(2, 1, 5, 0);
        UNICODE_2_1_8 = getInstance(2, 1, 8, 0);
        UNICODE_2_1_9 = getInstance(2, 1, 9, 0);
        UNICODE_3_0   = getInstance(3, 0, 0, 0);
        UNICODE_3_0_1 = getInstance(3, 0, 1, 0);
        UNICODE_3_1_0 = getInstance(3, 1, 0, 0);
        UNICODE_3_1_1 = getInstance(3, 1, 1, 0);
        UNICODE_3_2   = getInstance(3, 2, 0, 0);
        UNICODE_4_0   = getInstance(4, 0, 0, 0);
        UNICODE_4_0_1 = getInstance(4, 0, 1, 0);
        UNICODE_4_1   = getInstance(4, 1, 0, 0);
        UNICODE_5_0   = getInstance(5, 0, 0, 0);
        UNICODE_5_1   = getInstance(5, 1, 0, 0);
        UNICODE_5_2   = getInstance(5, 2, 0, 0);
        UNICODE_6_0   = getInstance(6, 0, 0, 0);
        UNICODE_6_1   = getInstance(6, 1, 0, 0);
        UNICODE_6_2   = getInstance(6, 2, 0, 0);
        UNICODE_6_3   = getInstance(6, 3, 0, 0);
        UNICODE_7_0   = getInstance(7, 0, 0, 0);
        UNICODE_8_0   = getInstance(8, 0, 0, 0);
        UNICODE_9_0   = getInstance(9, 0, 0, 0);
        UNICODE_10_0   = getInstance(10, 0, 0, 0);
        UNICODE_11_0   = getInstance(11, 0, 0, 0);
        UNICODE_12_0   = getInstance(12, 0, 0, 0);
        UNICODE_12_1   = getInstance(12, 1, 0, 0);
        UNICODE_13_0   = getInstance(13, 0, 0, 0);
        UNICODE_14_0   = getInstance(14, 0, 0, 0);

        ICU_VERSION   = getInstance(70, 1, 0, 0);
        ICU_DATA_VERSION = ICU_VERSION;
        UNICODE_VERSION = UNICODE_14_0;

        UCOL_RUNTIME_VERSION = getInstance(9);
        UCOL_BUILDER_VERSION = getInstance(9);
        UCOL_TAILORINGS_VERSION = getInstance(1);
    }

    // private constructor -----------------------------------------------

    /**
     * Constructor with int
     * @param compactversion a 32 bit int with each byte representing a number
     */
    private VersionInfo(int compactversion)
    {
        m_version_ = compactversion;
    }

    /**
     * Gets the int from the version numbers
     * @param major non-negative version number
     * @param minor non-negative version number
     * @param milli non-negative version number
     * @param micro non-negative version number
     */
    private static int getInt(int major, int minor, int milli, int micro)
    {
        return (major << 24) | (minor << 16) | (milli << 8) | micro;
    }
    ///CLOVER:OFF
    /**
     * Main method prints out ICU version information
     * @param args arguments (currently not used)
     * @stable ICU 4.6
     */
    public static void main(String[] args) {
        String icuApiVer;

        if (ICU_VERSION.getMajor() <= 4) {
            if (ICU_VERSION.getMinor() % 2 != 0) {
                // Development mile stone
                int major = ICU_VERSION.getMajor();
                int minor = ICU_VERSION.getMinor() + 1;
                if (minor >= 10) {
                    minor -= 10;
                    major++;
                }
                icuApiVer = "" + major + "." + minor + "M" + ICU_VERSION.getMilli();
            } else {
                icuApiVer = ICU_VERSION.getVersionString(2, 2);
            }
        } else {
            if (ICU_VERSION.getMinor() == 0) {
                // Development mile stone
                icuApiVer = "" + ICU_VERSION.getMajor() + "M" + ICU_VERSION.getMilli();
            } else {
                icuApiVer = ICU_VERSION.getVersionString(2, 2);
            }
        }


        System.out.println("International Components for Unicode for Java " + icuApiVer);

        System.out.println("");
        System.out.println("Implementation Version: " + ICU_VERSION.getVersionString(2, 4));
        System.out.println("Unicode Data Version:   " + UNICODE_VERSION.getVersionString(2, 4));
        System.out.println("CLDR Data Version:      " + LocaleData.getCLDRVersion().getVersionString(2, 4));
        System.out.println("Time Zone Data Version: " + getTZDataVersion());
    }

    /**
     * Generate version string separated by dots with
     * the specified digit width.  Version digit 0
     * after <code>minDigits</code> will be trimmed off.
     * @param minDigits Minimum number of version digits
     * @param maxDigits Maximum number of version digits
     * @return A tailored version string
     * @internal
     * @deprecated This API is ICU internal only. (For use in CLDR, etc.)
     */
    @Deprecated
    public String getVersionString(int minDigits, int maxDigits) {
        if (minDigits < 1 || maxDigits < 1
                || minDigits > 4 || maxDigits > 4 || minDigits > maxDigits) {
            throw new IllegalArgumentException("Invalid min/maxDigits range");
        }

        int[] digits = new int[4];
        digits[0] = getMajor();
        digits[1] = getMinor();
        digits[2] = getMilli();
        digits[3] = getMicro();

        int numDigits = maxDigits;
        while (numDigits > minDigits) {
            if (digits[numDigits - 1] != 0) {
                break;
            }
            numDigits--;
        }

        StringBuilder verStr = new StringBuilder(7);
        verStr.append(digits[0]);
        for (int i = 1; i < numDigits; i++) {
            verStr.append(".");
            verStr.append(digits[i]);
        }

        return verStr.toString();
    }
    ///CLOVER:ON


    // Moved from TimeZone class
    private static volatile String TZDATA_VERSION = null;

    static String getTZDataVersion() {
        if (TZDATA_VERSION == null) {
            synchronized (VersionInfo.class) {
                if (TZDATA_VERSION == null) {
                    UResourceBundle tzbundle = UResourceBundle.getBundleInstance("com/ibm/icu/impl/data/icudt"
                            + VersionInfo.ICU_DATA_VERSION_PATH, "zoneinfo64");
                    TZDATA_VERSION = tzbundle.getString("TZVersion");
                }
            }
        }
        return TZDATA_VERSION;
    }
}
