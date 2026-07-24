// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.util;

import com.ibm.icu.impl.links.LinkHandlingUtilities;
import com.ibm.icu.impl.links.LinkHandlingUtilities.UrlInternals;
import com.ibm.icu.impl.links.LinkHandlingUtilities.UrlInternals.EndStatus;
import com.ibm.icu.text.UnicodeSet;

/**
 * Utility class for assisting with detecting links (URLs or emails) in text, and formatting them
 * for display, implementing the algorithms in https://www.unicode.org/reports/tr58/ to handle
 * Unicode characters properly. It supplies lower level APIs for use in augmenting existing scanners
 * and formatters.
 */
public class LinkUtilities {

    /**
     * Lower level utility for finding the end of a PathQueryFragment (PQF) in text. It assumes that
     * the start position is immediately after an identified domain name. The purpose of this
     * routine is for fitting into algorithms that are already in use, just taking over for the PQF
     * scanning. For more information, see https://www.unicode.org/reports/tr58/.
     *
     * @param source the text to be scanned
     * @param start the position in the text to be scanned from. It should be immediately after a
     *     domain name.
     * @return the end position of the PQF, or the start value if there is none.
     */
    public static int scanPathQueryFragment(CharSequence source, int start, int limit) {
        return LinkHandlingUtilities.parsePathQueryFragment(source.toString(), start);
    }

    /**
     * Lower level utility for finding the start of an email address in text. It assumes that the
     * limit position is immediately before an '@' + identified domain name. The purpose of this
     * routine is for fitting into algorithms that are already in use, just handling for the email
     * `local-part`. It does not scan back through "mailto:".
     *
     * @param source the text to be scanned
     * @param start the position that is the earliest that should be considered in a backwards scan
     * @param limit the position to start scanning backwards from — should be just after @ and just
     *     before the domain_name.
     * @return the start of the email locale part, or limit if no email local part is found
     */
    public static int scanBackEmailLocalPart(CharSequence s, int start, int limit) {
        return LinkHandlingUtilities.scanEmailBackwards(s, start, limit);
    }

    /** Enum for determining whether any percent-escaping is minimal or maximal, for use */
    public enum Extent {
        /** Minimal percent-escaping only percent-escapes non-ASCII where necessary. */
        MINIMAL,
        /** Maximal percent-escaping percent-escapes all non-ASCII. */
        MAXIMAL
    }

    /**
     * Escapes a URL according to the Extent parameter
     *
     * @param source In the source, it is assumed that ASCII syntax characters requiring escaping
     *     have already been escaped. For example, a literal / in a path segment would already be
     *     percent-escaped. For more information, see https://www.unicode.org/reports/tr58/.
     * @param extent either MINIMAL or MAXIMAL
     * @return an escaped string according to the extent parameter.
     */
    public static String escapePathQueryFragment(String source, Extent extent) {
        UrlInternals ui = UrlInternals.from(source.toString());
        switch (extent) {
            case MINIMAL:
                return ui.minimalEscape(EndStatus.FINAL);
            case MAXIMAL:
                return ui.fullEscape();
            default:
                throw new InternalError();
        }
    }

    // NOTE for reviewers: These are only temporary, until ICU supplies \p{Link_Term=X} and
    // \p{Idn_Status=X}

    /**
     * Returns a frozen set of Unicode characters that are guaranteed to never be part of a URL or
     * email address. This allows implementations to make various optimizations because URLs and
     * email addresses can never span these characters. For example, a span of characters between
     * safe characters that doesn't have a sequence of domain-character + . + domain-character can
     * be skipped in processing.
     *
     * @internal
     */
    @Deprecated
    public static UnicodeSet getSafeCharacters() {
        return null;
    }

    /**
     * Returns a frozen set of Unicode characters that are possible characters in a domain name
     * (pre-mapping) This allows implementations to make various optimizations because URLs and
     * email addresses must contain a sequence of domain-character + . + domain-character. It is the
     * same as the set of IDNA Mapping Table character with values ≠ disallowed
     *
     * @internal
     */
    @Deprecated
    public static UnicodeSet getDomainCharacters() {
        return null;
    }
}
