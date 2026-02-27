// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.LinkEmailProps;
import com.ibm.icu.impl.LinkTermProps;
import com.ibm.icu.text.IDNA;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkDetector {
    private final String input;

    static public class Result {
        /** The input that corresponds to this detected result. */
        public final String input;

        /** The offset of the start of this result in the LinkDetector's input */
        public final int inputOffset;
        /** The number of characters this result spans in the
         *  input. Note that in a few cases, this differs from the
         *  length of humanReadableOutput. */
        public final int inputLength;

        /** The human-readable form of the detected URL/email
         *  address. This is often the same as the input, but may
         *  differ, such as if the input uses an a-label (xn--gr-zia) in
         *  a domain. */
        public final String humanReadableOutput;

        /** The detected link, which may be a mailto URL. */
        public final URL link;

        public Result(final String input,
                      final int inputOffset, final int inputLength,
                      final String humanReadableOutput,
                      final URL link) {
            this.input = input;
            this.inputOffset = inputOffset;
            this.inputLength = inputLength;
            this.humanReadableOutput = humanReadableOutput;
            this.link = link;
        }
    }

    /** The result is an array of nonoverlapping substrings, each
      * enriched with the detected link. */

    public final List<Result> results;

    // Path/query/fragment closers, and the separators that restart bracket
    // pairing within a part. All kept sorted for binary search.
    static private int[] PATH_CLOSERS = {0x23, 0x2f, 0x3f};         // # / ?
    static private int[] QUERY_CLOSERS = {0x23};                    // #
    static private int[] FRAGMENT_CLOSERS = {};
    static private int[] NO_SEPARATORS = {};
    static private int[] QUERY_SEPARATORS = {0x26, 0x3d};           // & =
    static private int[] DIRECTIVE_SEPARATORS = {0x26, 0x2c, 0x3d}; // & , =

    static final private IDNA UTS46 = IDNA.getUTS46Instance(IDNA.DEFAULT);

    // The '@' in the lookbehind is non-obvious: UTS58 has no
    // userinfo, so a host that immediately follows an '@' may be part
    // of an email address but never an http(s) URL.
    static final private Pattern INITIAL_REGEX = Pattern.compile("(?<![-\\p{L}\\p{N}\\p{M}./@])(?=[\\p{L}\\p{N}][-\\p{L}\\p{N}\\p{M}ßς۽۾་〇]*[\\.:。])");
    static final private Pattern SLOPPY_HOSTNAME = Pattern.compile("^([-\\p{L}\\p{N}\\p{M}ßς۽۾་〇]+[\\.。]){1,4}[-\\p{L}\\p{N}\\p{M}]+(?![-\\p{L}\\p{N}\\p{M}])");
    // The leading-scripts group lets glued text like "テストhttp://example.com"
    // attach the scheme: the trigger fires at the start of the unspaced run, and
    // the link begins a few code points later, where the scheme does. These are
    // the scripts that habitually run flush against a following URL.
    static final private Pattern PROTO = Pattern.compile("^([\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}\\p{IsThai}\\p{IsLao}\\p{IsKhmer}\\p{IsMyanmar}]*?)(https?)://", Pattern.CASE_INSENSITIVE);

    // RFC5321 caps an email local part at 64 octets; a longer run is not an
    // address, rather than an address with a 64-character tail.
    static final private int MAX_LOCALPART_CP = 64;

    // A DNS name is at most 253 characters; anything longer is not a host, and
    // bailing early protects us against burning CPU on malevolent input.
    static final private int MAX_HOST_CP = 253;

    public LinkDetector(final String text) {
        input = text;
        List<Result> candidates = new ArrayList<>();
        // 1. Scan for HTTP(S) URLs.
        Matcher m = INITIAL_REGEX.matcher(input);
        int start = 0;
        while (m.find(start)) {
            Result r = considerLink(m.start());
            start = m.start() + 1;
            if (r != null) {
                candidates.add(r);
                start = r.inputOffset + r.inputLength;
            }
        }
        // 2. Scan for email addresses.
        int i = input.indexOf('@');
        while (i >= 0) {
            Result r = considerEmail(i);
            if (r != null)
                candidates.add(r);
            i = input.indexOf('@', i + 1);
        }

        // 3. Merge, dropping any entity that overlaps an earlier one.
        candidates.sort((a, b) -> a.inputOffset != b.inputOffset
                                  ? Integer.compare(a.inputOffset, b.inputOffset)
                                  : Integer.compare(b.inputLength, a.inputLength));
        results = new ArrayList<Result>();
        Result previous = null;
        for (Result c : candidates) {
            if (previous == null ||
                previous.inputOffset + previous.inputLength <= c.inputOffset) {
                results.add(c);
                previous = c;
            }
        }
    }

    public Result considerLink(final int start) { // public only for a test, meh
        Matcher p = PROTO.matcher(input);
        p.region(start, input.length());
        String proto = "https";
        boolean hasScheme = false;
        int scriptOffset = 0;
        int hostnameStart = start;
        if (p.lookingAt()) {
            scriptOffset = p.end(1) - p.start(1); // leading scripts before the scheme
            proto = p.group(2).toLowerCase(Locale.US);
            hostnameStart = p.end();
            hasScheme = true;
        }
        // PROTO only matches http/https, so proto is always one of those here.
        int linkStart = start + scriptOffset;
        DomainMatch dm = matchDomain(hostnameStart);
        if (dm == null)
            return null;
        final int len = input.length();
        int pos = dm.end;
        int port = -1;

        // port: optional :\d+.
        if (pos < len && input.charAt(pos) == ':') {
            int portEnd = pos + 1;
            while (portEnd < len && Character.isDigit(input.charAt(portEnd))) {
                if (portEnd - pos > 5) // more than five digits ⇒ port > 65535
                    return null;
                portEnd++;
            }
            if (portEnd > pos + 1) {
                port = Integer.parseInt(input.substring(pos + 1, portEnd));
                if (port < 1 || port > 65535)
                    return null;
                pos = portEnd;
            }
        }

        int fileStart = pos;

        // A trailing root-label dot stays in the URL only when a
        // path, query, or fragment follows; at the end of a sentence
        // the dot is prose.
        if (pos + 1 < len && input.charAt(pos) == '.' &&
            isComponentStart(input.charAt(pos + 1)))
            pos++;

        // path: zero or more /... segments
        while (pos < len && input.charAt(pos) == '/')
            pos = skipComponent(pos, PATH_CLOSERS, NO_SEPARATORS, false);
        // query
        if (pos < len && input.charAt(pos) == '?')
            pos = skipComponent(pos, QUERY_CLOSERS, QUERY_SEPARATORS, false);
        // fragment
        if (pos < len && input.charAt(pos) == '#')
            pos = skipComponent(pos, FRAGMENT_CLOSERS, NO_SEPARATORS, true);

        String file = input.substring(fileStart, pos);
        String schemePrefix = hasScheme ? proto + "://" : "";
        String humanReadable =
            schemePrefix + dm.host + (port != -1 ? ":" + port : "") + file;
        try {
            return new Result(input, linkStart, pos - linkStart,
                              humanReadable,
                              new URL(proto, dm.host, port, file));
        } catch (java.net.MalformedURLException e) {
            // How can this possibly be reached, since we check the
            // proto above? But we don't want to linkify anything
            // weird, so the response is clear:
            return null;
        }
    }

    private Result considerEmail(final int at) {
        // Domain after '@'
        int domainStart = at + 1;
        if (domainStart >= input.length())
            return null;
        DomainMatch dm = matchDomain(domainStart);
        if (dm == null)
            return null;

        // Local part: walk left over Link_Email code points, capped at the
        // RFC5321 limit. Scanning (rather than slicing the whole prefix and
        // matching anchored) protects us from @a.b@a.b@a.b@a.b@...
        int localStart = at;
        int lpLen = 0;
        while (localStart > 0) {
            int cp = input.codePointBefore(localStart);
            if (!LinkEmailProps.INSTANCE.contains(cp))
                break;
            localStart -= Character.charCount(cp);
            if (++lpLen > MAX_LOCALPART_CP)
                break;
        }
        if (lpLen == 0 || lpLen > MAX_LOCALPART_CP)
            return null;

        String localPart = input.substring(localStart, at);
        // A dot may separate atoms but not lead, trail, or double up.
        if (localPart.charAt(0) == '.'
                || localPart.charAt(localPart.length() - 1) == '.'
                || localPart.contains(".."))
            return null;

        // Include a "mailto:" scheme prefix if it immediately precedes the local part.
        int effectiveStart = localStart;
        String schemePrefix = "";
        if (localStart >= 7
                && input.substring(localStart - 7, localStart).equalsIgnoreCase("mailto:")) {
            effectiveStart = localStart - 7;
            schemePrefix   = "mailto:";
        }

        String emailAddr    = localPart + "@" + dm.host;
        String humanReadable = schemePrefix + emailAddr;
        try {
            return new Result(input, effectiveStart, dm.end - effectiveStart,
                              humanReadable,
                              new URL("mailto:" + emailAddr));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /** Holds the result of a successful hostname match. */
    private static final class DomainMatch {
        final String host;  // Unicode-normalized hostname
        final int end;      // index in input past the last matched character
        DomainMatch(String host, int end) {
            this.host = host;
            this.end = end;
        }
    }

    /**
     * Tries to match a valid hostname starting at {@code start}.
     * Returns a DomainMatch on success, or null if the text does not begin
     * with a syntactically valid, IANA-recognised hostname.
     */
    private DomainMatch matchDomain(int start) {
        Matcher h = SLOPPY_HOSTNAME.matcher(input);
        h.region(start, input.length());
        if (!h.lookingAt())
            return null;
        if (input.codePointCount(start, h.end()) > MAX_HOST_CP)
            return null;
        // A label may not start or end with a hyphen (the LDH rule), so a host
        // like -foo.example-.com is not a link at all. xn-- A-labels pass, since
        // they neither start nor end with '-'.
        if (!validLabels(h.group().replace('。', '.')))
            return null;
        IDNA.Info idnaInfo = new IDNA.Info();
        String host = UTS46.nameToUnicode(h.group(),
                                          new StringBuilder(),
                                          idnaInfo).toString();
        if (host.endsWith(".invalid"))
            return null;
        int lastDot = host.lastIndexOf('.');
        if (lastDot < 0)
            return null;
        if (!IanaTlds.isTld(host.substring(lastDot + 1)))
            return null;
        return new DomainMatch(host, h.end());
    }

    private static boolean validLabels(String host) {
        for (String label : host.split("\\.", -1))
            if (label.startsWith("-") || label.endsWith("-"))
                return false;
        return true;
    }

    private static boolean isComponentStart(char c) {
        return c == '/' || c == '?' || c == '#';
    }

    private int skipComponent(int start, final int[] closers,
                              final int[] separators, final boolean directive) {
        Stack<Integer> openers = new Stack<>();
        int[] seps = separators;
        final int len = input.length();
        int i = start;
        while (i < len) {
            if (i == start) {
                // the lead-in character (e.g. '/', '?', '#')
                i += Character.charCount(input.codePointAt(i));
                continue;
            }
            int cp = input.codePointAt(i);
            if (contained(closers, cp))
                return i;
            if (directive && cp == 0x3a && i + 2 < len &&
                input.charAt(i + 1) == 0x7e && input.charAt(i + 2) == 0x3a) {
                // ":~:" begins a fragment text directive; its own separators take
                // over and bracket pairing restarts (the directive is a fresh part).
                openers.clear();
                seps = DIRECTIVE_SEPARATORS;
                i += 3;
                continue;
            }
            if (contained(seps, cp)) {
                // A separator ends one part of the component. The open-bracket
                // stack restarts here, but the link continues.
                openers.clear();
                i += Character.charCount(cp);
                continue;
            }
            switch (getTermination(cp)) {
            case HARD:
                return i;
            case SOFT:
                int nextI = i + Character.charCount(cp);
                while (nextI < len &&
                       getTermination(input.codePointAt(nextI)) == Termination.SOFT)
                    nextI += Character.charCount(input.codePointAt(nextI));
                if (nextI >= len ||
                    getTermination(input.codePointAt(nextI)) == Termination.HARD)
                    return i;
                break;
            case CLOSE:
                if (!openers.isEmpty() && getOpener(cp) == openers.peek())
                    openers.pop();
                else
                    return i;
                break;
            case OPEN:
                openers.push(cp);
                break;
            default:
                // INCLUDE: part of the link, keep scanning
                break;
            }
            i += Character.charCount(cp);
        }
        return i;
    }

    enum Termination {
        // Ordinals must match the ULinkTerm enum values in linktermprops.h.
        HARD,     // 0: always terminates; default for unlisted code points
        INCLUDE,  // 1: may appear in a URL
        SOFT,     // 2: terminates only when followed by Hard
        CLOSE,    // 3: closing bracket
        OPEN      // 4: opening bracket
    }

    private static final Termination[] TERMINATION_VALUES = Termination.values();

    private Termination getTermination(int cp) {
        return TERMINATION_VALUES[LinkTermProps.INSTANCE.get(cp)];
    }

    // Mapping from closing bracket code point to its matching opener.
    // Parallel arrays: OPENERS_CLOSER[i] is a closer, OPENERS_OPEN[i] is its paired opener.
    // Source: https://www.unicode.org/Public/17.0.0/linkification/LinkBracket.txt
    // Kept sorted by closer for binary search.
    // To regenerate (update the version in the URL for new Unicode releases):
    //   curl -s 'https://www.unicode.org/Public/17.0.0/linkification/LinkBracket.txt' |
    //   awk '/^#|^[[:space:]]*$/{next}
    //        {c[++n]="0x"$1; o[n]="0x"$3}
    //        END { for (a=1; a<=2; a++) {
    //                nm = (a==1 ? "OPENERS_CLOSER" : "OPENERS_OPEN")
    //                print "    private static final int[] " nm " = {"
    //                for (i=1; i<=n; i++) {
    //                  printf "        %s,", (a==1 ? c[i] : o[i])
    //                  if (i%7==0 || i==n) printf "\n"
    //                }
    //                print "    };" } }'
    private static final int[] OPENERS_CLOSER = {
        0x0029, 0x003E, 0x005D, 0x007D, 0x0F3B, 0x0F3D, 0x169C,
        0x2046, 0x207E, 0x208E, 0x2309, 0x230B, 0x232A,
        0x2769, 0x276B, 0x276D, 0x276F, 0x2771, 0x2773, 0x2775,
        0x27C6, 0x27E7, 0x27E9, 0x27EB, 0x27ED, 0x27EF,
        0x2984, 0x2986, 0x2988, 0x298A, 0x298C, 0x298E, 0x2990,
        0x2992, 0x2994, 0x2996, 0x2998, 0x29D9, 0x29DB, 0x29FD,
        0x2E23, 0x2E25, 0x2E27, 0x2E29,
        0x2E56, 0x2E58, 0x2E5A, 0x2E5C,
        0x3009, 0x300B, 0x300D, 0x300F, 0x3011, 0x3015, 0x3017, 0x3019, 0x301B,
        0xFE5A, 0xFE5C, 0xFE5E,
        0xFF09, 0xFF3D, 0xFF5D, 0xFF60, 0xFF63,
    };
    private static final int[] OPENERS_OPEN = {
        0x0028, 0x003C, 0x005B, 0x007B, 0x0F3A, 0x0F3C, 0x169B,
        0x2045, 0x207D, 0x208D, 0x2308, 0x230A, 0x2329,
        0x2768, 0x276A, 0x276C, 0x276E, 0x2770, 0x2772, 0x2774,
        0x27C5, 0x27E6, 0x27E8, 0x27EA, 0x27EC, 0x27EE,
        0x2983, 0x2985, 0x2987, 0x2989, 0x298B, 0x298F, 0x298D,
        0x2991, 0x2993, 0x2995, 0x2997, 0x29D8, 0x29DA, 0x29FC,
        0x2E22, 0x2E24, 0x2E26, 0x2E28,
        0x2E55, 0x2E57, 0x2E59, 0x2E5B,
        0x3008, 0x300A, 0x300C, 0x300E, 0x3010, 0x3014, 0x3016, 0x3018, 0x301A,
        0xFE59, 0xFE5B, 0xFE5D,
        0xFF08, 0xFF3B, 0xFF5B, 0xFF5F, 0xFF62,
    };

    /** Returns the opening bracket paired with the given closing bracket, or -1. */
    public static int getOpener(int closer) {
        int idx = Arrays.binarySearch(OPENERS_CLOSER, closer);
        return idx >= 0 ? OPENERS_OPEN[idx] : -1;
    }

    /** Returns true if cp is in the sorted array arr. */
    private static boolean contained(int[] arr, int cp) {
        return Arrays.binarySearch(arr, cp) >= 0;
    }
}
