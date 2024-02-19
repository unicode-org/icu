// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.locale;

import java.util.List;
import java.util.Objects;

import com.ibm.icu.lang.UScript;

public final class LSR {
    public static final int REGION_INDEX_LIMIT = 1001 + 26 * 26;

    public static final int EXPLICIT_LSR = 7;
    public static final int EXPLICIT_LANGUAGE = 4;
    public static final int EXPLICIT_SCRIPT = 2;
    public static final int EXPLICIT_REGION = 1;
    public static final int IMPLICIT_LSR = 0;
    public static final int DONT_CARE_FLAGS = 0;

    public static final boolean DEBUG_OUTPUT = false;

    public final String language;
    public final String script;
    public final String region;
    /** Index for region, negative if ill-formed. @see indexForRegion */
    final int regionIndex;
    public final int flags;

    public LSR(String language, String script, String region, int flags) {
        this.language = language;
        this.script = script;
        this.region = region;
        regionIndex = indexForRegion(region);
        this.flags = flags;
    }

    /**
     * Returns a positive index (>0) for a well-formed region code.
     * Do not rely on a particular region->index mapping; it may change.
     * Returns 0 for ill-formed strings.
     */
    public static final int indexForRegion(String region) {
        if (region.length() == 2) {
            int a = region.charAt(0) - 'A';
            if (a < 0 || 25 < a) { return 0; }
            int b = region.charAt(1) - 'A';
            if (b < 0 || 25 < b) { return 0; }
            return 26 * a + b + 1001;
        } else if (region.length() == 3) {
            int a = region.charAt(0) - '0';
            if (a < 0 || 9 < a) { return 0; }
            int b = region.charAt(1) - '0';
            if (b < 0 || 9 < b) { return 0; }
            int c = region.charAt(2) - '0';
            if (c < 0 || 9 < c) { return 0; }
            return (10 * a + b) * 10 + c + 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(language);
        if (!script.isEmpty()) {
            result.append('-').append(script);
        }
        if (!region.isEmpty()) {
            result.append('-').append(region);
        }
        return result.toString();
    }

    public boolean isEquivalentTo(LSR other) {
        return language.equals(other.language)
                && script.equals(other.script)
                && region.equals(other.region);
    }

    @Override
    public boolean equals(Object obj) {
        LSR other;
        return this == obj ||
                (obj != null
                && obj.getClass() == this.getClass()
                && language.equals((other = (LSR) obj).language)
                && script.equals(other.script)
                && region.equals(other.region)
                && flags == other.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, script, region, flags);
    }

    // This method is added only to support encodeToIntForResource()
    // It only support [a-z]{2,3} and will not work for other cases.
    private int encodeLanguageToInt() {
        assert language.length() >= 2;
        assert language.length() <= 3;
        assert language.charAt(0) >= 'a';
        assert language.charAt(0) <= 'z';
        assert language.charAt(1) >= 'a';
        assert language.charAt(1) <= 'z';
        assert language.length() == 2 || language.charAt(2) >= 'a';
        assert language.length() == 2 || language.charAt(2) <= 'z';
        return language.charAt(0) - 'a' + 1 +
               27 * (language.charAt(1) - 'a' + 1) +
               ((language.length() == 2) ? 0 : 27 * 27 * (language.charAt(2) - 'a' + 1));
    }
    // This method is added only to support encodeToIntForResource()
    // It only support [A-Z][a-z]{3} which defined in UScript and does not work for other cases.
    private int encodeScriptToInt() {
        int ret = UScript.getCodeFromName(script);
        assert ret != UScript.INVALID_CODE;
        return ret;
    }
    // This method is added only to support encodeToIntForResource()
    // It only support [A-Z]{2} and the code in m49 but does not work for other cases.
    private int encodeRegionToInt(List<String> m49) {
        assert region.length() >= 2;
        assert region.length() <= 3;
        if (region.length() == 3) {
            int index = m49.indexOf(region);
            assert index >= 0;
            if (index < 0) {
                throw new IllegalStateException(
                    "Please add '" + region + "' to M49 in LocaleDistanceMapper.java");
            }
            return index;
        }
        assert region.charAt(0) >= 'A';
        assert region.charAt(0) <= 'Z';
        assert region.charAt(1) >= 'A';
        assert region.charAt(1) <= 'Z';
        // 'AA' => 1+27*1  = 28
        // ...
        // 'AZ' => 1+27*26 = 703
        // 'BA' => 2+27*1  = 29
        // ...
        // 'IN' => 9+27*14 = 387
        // 'ZZ' => 26+27*26 = 728
        return (region.charAt(0) - 'A' + 1) + 27 * (region.charAt(1) - 'A' + 1);
    }
    // This is designed to only support encoding some LSR into resources but not for other cases.
    public int encodeToIntForResource(List<String> m49) {
        return (encodeLanguageToInt() + (27*27*27) * encodeRegionToInt(m49)) |
            (encodeScriptToInt() << 24);
    }
    private static String toLanguage(int encoded) {
        if (encoded == 0) return "";
        if (encoded == 1) return "skip";
        encoded &= 0x00ffffff;
        encoded %= 27*27*27;
        StringBuilder res = new StringBuilder(3);
        res.append((char)('a' + ((encoded % 27) - 1)));
        res.append((char)('a' + (((encoded / 27 ) % 27) - 1)));
        if (encoded / (27 * 27) != 0) {
            res.append((char)('a' + ((encoded / (27 * 27)) - 1)));
        }
        return res.toString();
    }
    private static String toScript(int encoded) {
        if (encoded == 0) return "";
        if (encoded == 1) return "script";
        encoded = (encoded >> 24) & 0x000000ff;
        return UScript.getShortName(encoded);
    }
    private static String toRegion(int encoded, String[] m49) {
        if (encoded == 0 || encoded == 1) return "";
        encoded &= 0x00ffffff;
        encoded /= 27 * 27 * 27;
        encoded %= 27 * 27;
        if (encoded < 27) {
            return m49[encoded];
        }
        StringBuilder res = new StringBuilder(3);
        res.append((char)('A' + ((encoded % 27) - 1)));
        res.append((char)('A' + (((encoded / 27) % 27) - 1)));
        return res.toString();
    }

    public static LSR[] decodeInts(int[] nums, String[] m49) {
        LSR[] lsrs = new LSR[nums.length];
        for (int i = 0; i < nums.length; ++i) {
            int n = nums[i];
            lsrs[i] = new LSR(toLanguage(n), toScript(n), toRegion(n, m49), LSR.IMPLICIT_LSR);
        }
        return lsrs;
    }
}
