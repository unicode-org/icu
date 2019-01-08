// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.Objects;

final class LSR {
    static final int REGION_INDEX_LIMIT = 1000 + 26 * 26;

    final String language;
    final String script;
    final String region;
    /** Index for region, negative if ill-formed. @see indexForRegion */
    final int regionIndex;

    LSR(String language, String script, String region) {
        this.language = language;
        this.script = script;
        this.region = region;
        regionIndex = indexForRegion(region);
    }

    /**
     * Returns a non-negative index for a well-formed region code.
     * Do not rely on a particular region->index mapping; it may change.
     * Returns -1 for ill-formed strings.
     */
    static final int indexForRegion(String region) {
        if (region.length() == 2) {
            int a = region.charAt(0) - 'A';
            if (a < 0 || 25 < a) { return -1; }
            int b = region.charAt(1) - 'A';
            if (b < 0 || 25 < b) { return -1; }
            return 26 * a + b + 1000;
        } else if (region.length() == 3) {
            int a = region.charAt(0) - '0';
            if (a < 0 || 9 < a) { return -1; }
            int b = region.charAt(1) - '0';
            if (b < 0 || 9 < b) { return -1; }
            int c = region.charAt(2) - '0';
            if (c < 0 || 9 < c) { return -1; }
            return (10 * a + b) * 10 + c;
        }
        return -1;
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
    @Override
    public boolean equals(Object obj) {
        LSR other;
        return this == obj ||
                (obj != null
                && obj.getClass() == this.getClass()
                && language.equals((other = (LSR) obj).language)
                && script.equals(other.script)
                && region.equals(other.region));
    }
    @Override
    public int hashCode() {
        return Objects.hash(language, script, region);
    }
}
