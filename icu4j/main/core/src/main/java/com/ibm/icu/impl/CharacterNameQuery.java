// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl;

/**
 * For a character name {@code name}, a query string {@code query} matches {@code name} under
 * UAX44-LM2 if and only if the following returns true:
 *
 * <pre>
 *     final var matcher = new CharacterNameQuery(query).matcher();
 *     for (final char c : name) {
 *         if (!matcher.consistentWith(c)) {
 *             return false;
 *         }
 *     }
 *
 *     return matcher.matches();
 * </pre>
 *
 * {@code name} must be an exact character name, in particular, all uppercase, with no underscores,
 * no double hyphens, no isolated hyphens. There are no constraints on {@code query}; for instance,
 * this class will match name="ZANABAZAR SQUARE LETTER -A" with query="Zanabazar-square_letter_-A".
 * {@code Matcher::consistentWith} is not retryable: once it returns false, the {@code Matcher}
 * should not be used again.
 */
class CharacterNameQuery {
    static class Matcher {
        private Matcher(CharacterNameQuery query) {
            this.query = query;
            this.skeletonIterator = 0;
        }

        boolean consistentWith(char c) {
            // Instead of constructing a skeleton from the name as in the constructor of
            // CharacterNameQuery, we check character-by-character if the skeleton we would
            // construct from the name would be consistent with `query->skeleton`.
            // We require that this be called with characters from an actual character name, so we
            // need not worry about case or underscores here.

            if (skeletonIterator == query.skeleton.length()) {
                return false;
            }
            if (c == ' ') {
                // The last hyphen was word-final; check that there is a corresponding significant
                // hyphen in the skeleton.  A hyphen in a character name cannot be both word-final
                // and word-initial by rule R3 in
                // https://unicode.org/versions/Unicode17.0.0/core-spec/chapter-4/#G135165, so we do
                // not have to worry about checking the skeleton for the same significant hyphen
                // twice.
                if (lastChar == '-') {
                    if (query.skeleton.charAt(skeletonIterator++) != lastChar) {
                        return false;
                    }
                }
                lastChar = c;
                return true;
            } else if (c == '-') {
                if (lastChar == ' '
                        || (query.is1180 && skeletonIterator == query.skeleton.length() - 2)) {
                    // If lastChar == ' ', this is a word-initial hyphen, so we know it is
                    // significant.  Check that we are expecting it.
                    // If we are looking for U+1180 and we matched everything but the trailing -E,
                    // this could be that hyphen; move past it.  This could turn out to be a
                    // different character name if there is something other than E afterwards, in
                    // which case we should in principle have ignored the hyphen; but then since the
                    // suffixes will differ, we will return false anyway.
                    // For example, when searching for HANGUL JUNGSEONG O-E and checking consistency
                    // with HANGUL JUNGSEONG O-U, if we computed both skeleta and compared them,
                    // the skeleta would be HANGULJUNGSEONGO-E and HANGULJUNGSEONGOU, and the
                    // comparison would fail on - vs. U.
                    // Here, while feeding HANGUL JUNGSEONG O-U character-by-character, we assume
                    // its skeleton would be HANGULJUNGSEONGO-, and fail on E vs. U.
                    if (query.skeleton.charAt(skeletonIterator++) != c) {
                        return false;
                    }
                }
                // If lastChar is not ' ', we do not know whether this hyphen is word-final, so we
                // cannot check against the skeleton.
                lastChar = c;
                return true;
            } else {
                if (query.skeleton.charAt(skeletonIterator++) != c) {
                    return false;
                }
                lastChar = c;
                return true;
            }
        }

        boolean consistentWith(CharSequence substring) {
            for (int i = 0; i < substring.length(); ++i) {
                if (!consistentWith(substring.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        boolean matches() {
            // If a character name could end with a HYPHEN-MINUS, that HYPHEN-MINUS would be
            // significant: we would need to check that if lastChar == '-',
            // *skeletonIterator == '-', and then advance skeletonIterator.
            // However, this is disallowed by rule R3 in
            // https://unicode.org/versions/Unicode17.0.0/core-spec/chapter-4/#G135165.
            assert (lastChar != '-');
            return skeletonIterator == query.skeleton.length();
        }

        CharSequence remainingSignificantCharacters() {
            return query.skeleton.subSequence(skeletonIterator, query.skeleton.length());
        }

        private CharacterNameQuery query;
        private int skeletonIterator;
        // Initialized to ' ' so that a leading hyphen is treated like a hyphen that follows a
        // space (non-medial and thus not ignorable).  There are in fact no leading hyphens in
        // character names by rule R3 in
        // https://unicode.org/versions/Unicode17.0.0/core-spec/chapter-4/#G135165, and technically
        // we do not read the value of this variable before writing to it because the first call to
        // consistentWith never has c=' ' either by R4, but it seems cleanest to initialize lastChar
        // nonetheless.
        char lastChar = ' ';
    }

    CharacterNameQuery(CharSequence query) {
        // Construct a skeleton obtained by
        // 1. removing medial hyphens (except the one in the name of U+1180);
        // 2. removing spaces and underscores;
        // 3. uppercasing,
        // as described in https://www.unicode.org/reports/tr44/#UAX44-LM2.
        // We do all three in a single pass.
        for (int i = 0; i < query.length(); ++i) {
            assert (skeleton.length() < skeleton.capacity());
            if (skeleton.length() >= skeleton.capacity()) {
                // The caller should limit the query length appropriately; if they do not, assert,
                // and if assertions are disabled, query for the empty string (which will quickly
                // find nothing).
                skeleton.setLength(0);
                break;
            }
            if (query.charAt(i) == ' ' || query.charAt(i) == '_') {
                continue;
            }
            if (query.charAt(i) == '-') {
                boolean isMedial;
                boolean is1180MedialHyphen = false;
                if (i == 0 || i == query.length() - 1) {
                    isMedial = false;
                } else {
                    isMedial =
                            isASCIILetterOrDigit(query.charAt(i - 1))
                                    && isASCIILetterOrDigit(query.charAt(i + 1));
                }
                // A medial hyphen is the hyphen in the name of U+1180 HANGUL JUNGSEONG O-E if what
                // comes before skeletonizes to HANGULJUNGSEONGO and what comes after skeletonizes
                // to E.
                if (isMedial
                        && Character.toUpperCase(query.charAt(i + 1)) == 'E'
                        && "HANGULJUNGSEONGO".contentEquals(skeleton)) {
                    is1180MedialHyphen = true;
                    // If there is anything significant after the E, the part of the name after the
                    // hyphen does not skeletonize to E, and thus this is not U+1180.
                    // There can be no hyphens there: if there is one, it is either non-medial, or
                    // there is another letter beyond the E.  The insignificant characters are thus
                    // only spaces and underscores.
                    for (int j = i + 2; j < query.length(); ++j) {
                        if (query.charAt(j) != ' ' && query.charAt(j) != '_') {
                            is1180MedialHyphen = false;
                        }
                    }
                }
                if (!isMedial || is1180MedialHyphen) {
                    skeleton.append(query.charAt(i));
                }
            } else {
                skeleton.append(Character.toUpperCase(query.charAt(i)));
            }
        }
        // This can be true even if we never went through the is1180MedialHyphen path, e.g., if the
        // query was "HANGUL JUNGSEONG O -E".
        is1180 = "HANGULJUNGSEONGO-E".contentEquals(skeleton);
    }

    Matcher matcher() {
        return new Matcher(this);
    }

    private static boolean isASCIILetterOrDigit(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    StringBuilder skeleton = new StringBuilder(120);
    boolean is1180;
}
