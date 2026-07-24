// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.links;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.locale.XCldrStub;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.Output;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LinkHandlingUtilities {

    private static final UnicodeSet SOFAR = new UnicodeSet();

    /**
     * Based on https://url.spec.whatwg.org/#percent-encoded-bytes. Is the default for maximal
     * encoding, but can be customized
     */
    private enum WhatWgPercentEncoded {
        /** C0 controls and all code points greater than U+007E (~). */
        C0(null, "[\\u0000-\\u001F\\u007E-\\x{10FFFF}]"),

        /** C0 and U+0020 SPACE, U+0022 ("), U+003C (<), U+003E (>), andU+0060 (`). */
        FRAGMENT(C0, "[\\ \"<>`]"),

        /** C0 and U+0020 SPACE, U+0022 ("), U+0023 (#), U+003C (<), and U+003E (>) */
        QUERY(C0, "[\\ \"<>]"),

        /** query percent-encode set and U+0027 ('). Used for http://... */
        SPECIAL_QUERY(QUERY, "[']"),

        /**
         * query percent-encode set and<br>
         * U+003F (?), U+005E (^), U+0060 (`), U+007B ({), and U+007D (}).
         */
        PATH(QUERY, "[?\\^`\\{\\}]"),

        /**
         * path and<br>
         * U+002F (/), U+003A (:), U+003B (;), U+003D (=), U+0040 (@), U+005B ([) to U+005D (]),
         * inclusive, and U+007C (|).
         */
        USERINFO(PATH, "[/:;=@\\[-\\]|]"),

        /**
         * userinfo and <br>
         * U+0024 ($) to U+0026 (&), inclusive, U+002B (+), and U+002C (,).
         */
        COMPONENT(USERINFO, "[\\$-\\&+,]");

        private final UnicodeSet set;

        private WhatWgPercentEncoded(WhatWgPercentEncoded base, String uset) {
            set =
                    new UnicodeSet(uset)
                            .addAll(base == null ? UnicodeSet.EMPTY : base.getSet())
                            .freeze();
        }

        public UnicodeSet getSet() {
            return set;
        }
    }

    /**
     * Defines the getOpening mapping<br>
     * It will be replaced once we have a real property value in ICU.
     */
    private static int getOpening(int cp) {
        return cp == '>' ? '<' : UCharacter.getBidiPairedBracket(cp);
    }

    /**
     * Defines the LinkTermination property<br>
     * These will be replaced once we have a real property value in ICU.
     */
    private enum LinkTermination {
        HARD("[\\p{whitespace}\\p{NChar}[\\p{C}-\\p{Cf}]\\p{deprecated}]"),
        SOFT("[\\p{Term}\\p{lb=qu}-\\p{deprecated}]"),
        CLOSE("[\\p{Bidi_Paired_Bracket_Type=Close}[>]-\\p{deprecated}]"),
        OPEN("[\\p{Bidi_Paired_Bracket_Type=Open}[<]-\\p{deprecated}]"),
        INCLUDE(null), // all else
        ;

        private final UnicodeSet base;

        private LinkTermination(String uset) {
            if (uset == null) { // only called with Include, the "none of the above" option
                this.base = SOFAR.complement().freeze();
            } else {
                this.base = new UnicodeSet(uset).freeze();
                SOFAR.addAll(this.base);
            }
        }

        private static final UnicodeMap<LinkTermination> PROPERTY_MAP = new UnicodeMap<>();

        static {
            // Verify consistency
            for (LinkTermination pv1 : values()) {
                for (LinkTermination pv2 : values()) {
                    if (pv1.compareTo(pv2) <= 0) {
                        continue;
                    }
                    if (pv1.base.containsSome(pv2.base)) {
                        throw new IllegalArgumentException(
                                "Values in LinkTermination overlap! "
                                        + pv1
                                        + ", "
                                        + pv2
                                        + ": "
                                        + new UnicodeSet(pv1.base).retainAll(pv2.base));
                    }
                }
            }
            for (LinkTermination lt : values()) {
                PROPERTY_MAP.putAll(lt.base, lt);
            }
            PROPERTY_MAP.freeze();
        }
    }

    // https://www.rfc-editor.org/rfc/rfc5322.html#section-3.2.3 has the full list for ASCII part
    // See also https://en.wikipedia.org/wiki/Email_address#Local-part
    // We add dot (ascii '.'), and then check after for the special dot constraints.

    /** These will be replaced once we have a real property value in ICU. */
    static final UnicodeSet EMAIL_ASCII_INCLUDES =
            new UnicodeSet("[[a-zA-Z][0-9][_ \\- ! ? ' \\{ \\} * / \\& # % ` \\^ + = | ~ \\$]]")
                    .add('.')
                    .freeze();

    static final UnicodeSet VALID_EMAIL_LOCAL_PART =
            new UnicodeSet("[\\p{XID_Continue}-\\p{block=basic_latin}]")
                    .addAll(EMAIL_ASCII_INCLUDES)
                    .freeze();

    public static int scanEmailBackwards(CharSequence source, int hardStart, int domainStart) {
        if (UCharacter.codePointBefore(source, domainStart) != '@') {
            throw new IllegalArgumentException("Scanning must start after an '@' sign");
        }
        int result = VALID_EMAIL_LOCAL_PART.spanBack(source, domainStart - 1, SpanCondition.SIMPLE);
        if (result == domainStart - 1) {
            return domainStart;
        } else if (result < hardStart) {
            result = hardStart;
        }
        String localPart = source.subSequence(result, domainStart - 1).toString();
        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            return domainStart;
        }
        if (source.toString().substring(0, result).endsWith("mailto:")) {
            result -= "mailto:".length();
        }
        return result;
    }

    private enum Structure {
        single,
        listP("/", "𝑷"),
        listF(":~:", "𝑭"), // directive
        listQ2("&", "𝑸", "=", "𝑽");
        public final String sub;
        public final String prefix;
        public final String sub2; // only used for Query, with 2 values
        public final String prefix2;

        Structure() {
            this(null, null, null, null);
        }

        Structure(String sub, String prefix) {
            this(sub, prefix, null, null);
        }

        Structure(String sub, String prefix, String sub2, String prefix2) {
            this.sub = sub;
            this.sub2 = sub2;
            this.prefix = prefix;
            this.prefix2 = prefix2;
        }
    }

    /** Parallels the spec parts table */
    private enum Part {
        // initiator, terminators, clearStack
        PROTOCOL(Structure.single, '\u0000', "[{//}]", "[]", "[]", null),
        HOST(Structure.single, '\u0000', "[/?#]", "[]", "[]", null),
        PATH(Structure.listP, '/', "[?#]", "[/]", "[]", WhatWgPercentEncoded.PATH),
        QUERY(Structure.listQ2, '?', "[#]", "[=\\&]", "[+]", WhatWgPercentEncoded.SPECIAL_QUERY),
        FRAGMENT(
                Structure.listF,
                '#',
                "[]",
                "[{:~:}]",
                "[]",
                WhatWgPercentEncoded.FRAGMENT), // the :~: is handled by code
    // FRAGMENT_DIRECTIVE(Structure.listF, '#', "[]", "[{:~:}]", "[]"), // the :~: is handled by
    // code
    ;

        static final Set<Part> ALL = XCldrStub.setofOrdered(values());

        static final int[] FRAGMENT_DIRECTIVE_STRING = ":~:".codePoints().toArray();

        public final Structure structure;
        final int initiator;
        final UnicodeSet terminators;
        final UnicodeSet clearStack;
        final UnicodeSet whatwg_quoted;

        private Part(
                Structure structure,
                char initiator,
                String terminators,
                String clearStack,
                String extraQuoted,
                WhatWgPercentEncoded whatwg_quoted) {
            this.structure = structure;
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators).freeze();
            this.clearStack = new UnicodeSet(clearStack).freeze();
            new UnicodeSet(extraQuoted).addAll(this.clearStack).addAll(this.terminators).freeze();
            this.whatwg_quoted = whatwg_quoted == null ? UnicodeSet.EMPTY : whatwg_quoted.getSet();
        }

        static Part fromInitiator(int cp) {
            for (Part part : Part.values()) {
                if (part.initiator == cp) {
                    return part;
                }
            }
            return null;
        }

        private String fullEscape(List<List<String>> source) {
            if (source.isEmpty()) {
                return "";
            }
            if (structure == Structure.single) {
                return source.get(0).get(0);
            }
            StringBuilder result = new StringBuilder().appendCodePoint(initiator);
            UnicodeSet allEscape = whatwg_quoted;
            // new UnicodeSet(0, 0x21, 0x7F, 0x10FFFF).addAll(extraQuoted).freeze();
            boolean atStart = true;
            for (List<String> list : source) {
                if (atStart) {
                    atStart = false;
                } else {
                    result.append(structure.sub);
                }
                if (structure.sub2 == null) {
                    result.append(escape(list.get(0), allEscape));
                } else {
                    boolean atStart2 = true;
                    for (String string : list) {
                        if (atStart2) {
                            atStart2 = false;
                        } else {
                            result.append(structure.sub2);
                        }
                        result.append(escape(string, allEscape));
                    }
                }
            }
            return result.toString();
        }
    }

    public static class UrlInternals {
        private NavigableMap<Part, List<List<String>>>
                data; // note for some parts, the lists only ever have 1 member

        /**
         * Pull apart a URL string into Parts. <br>
         * TODO: unescape the %escapes.
         *
         * @param source
         * @param unescape TODO
         * @return
         */
        public static UrlInternals from(String source) {
            Map<Part, List<List<String>>> result = new EnumMap<>(Part.class);
            // quick and dirty
            int partStart = 0;
            int partEnd;
            main:
            for (Part part : Part.values()) {
                switch (part) {
                    case PROTOCOL:
                        partEnd = source.indexOf("://"); // TODO fix for mailto
                        if (partEnd > 0) {
                            partEnd += 3;
                            result.put(
                                    Part.PROTOCOL, List.of(List.of(source.substring(0, partEnd))));
                            partStart = partEnd;
                        }
                        break;
                    default:
                        partEnd =
                                part.terminators.span(
                                        source, partStart, SpanCondition.NOT_CONTAINED);
                        if (partStart != partEnd) {
                            if (part != Part.HOST) {
                                ++partStart;
                            }
                            String partString = source.substring(partStart, partEnd);
                            if (part.structure.sub == null) { // only host, no unescaping
                                result.put(part, List.of(List.of(partString)));
                            } else if (part.structure.sub2 == null) { // we have just a list
                                List<List<String>> cleanList = cleanList(part, partString);
                                result.put(part, cleanList);
                            } else { // we have a list of lists (query)
                                List<List<String>> cleanList =
                                        Splitter.on(part.structure.sub)
                                                .splitToStream(partString)
                                                .map(x -> splitList(part, part.structure.sub2, x))
                                                .collect(Collectors.toList());
                                result.put(part, cleanList);
                            }
                        }
                        if (partEnd == source.length()) {
                            break main;
                        }
                        partStart = partEnd;
                        break;
                }
            }
            return new UrlInternals(result);
        }

        private UrlInternals(Map<Part, List<List<String>>> data) {
            this.data = XCldrStub.immutableSortedMap(data);
        }

        public enum EndStatus {
            MEDIAL,
            FINAL
        }

        /**
         * Minimally escape. Presumes that the parts use \ for interior quoting.<br>
         *
         * @param endStatus TODO
         * @param escapedCounter TODO
         */
        public String minimalEscape(EndStatus endStatus) {
            StringBuilder output = new StringBuilder();
            // get the last part
            List<Entry<Part, List<List<String>>>> ordered = List.copyOf(data.entrySet());
            Part lastPart = null;

            for (Entry<Part, List<List<String>>> entry : ordered) {
                if (!entry.getValue().isEmpty()) {
                    lastPart = entry.getKey();
                }
            }
            // process all parts
            for (Entry<Part, List<List<String>>> partEntry : ordered) {
                Part part = partEntry.getKey();
                final List<List<String>> partParts = partEntry.getValue();
                if (partParts.isEmpty()) {
                    continue;
                }
                if (part.structure.sub == null) {
                    output.append(partParts.get(0).get(0)); // just copy
                    continue;
                }
                String unified =
                        joinListListEscaping(part.structure.sub, part.structure.sub2, partParts);

                int[] cps = unified.codePoints().toArray();
                int n = cps.length;
                if (cps[0] != part.initiator) {
                    output.appendCodePoint(part.initiator);
                }
                ;
                int copiedAlready = 0;
                Stack<Integer> openingStack = new Stack<>();
                for (int i = 0; i < n; ++i) {
                    final int cp = cps[i];
                    switch (cp) { // was just for test files
                        case '\\': // if we have \ followed by x, just emit the literal x; otherwise
                            // \
                            // This is ONLY used for our test files;
                            // in production the parts of the path/query
                            // would be handled separately.

                            // append soft code points
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            if (i < n - 1) {
                                // append next code point
                                ++i;
                                appendPercentEscaped(output, cps[i]);
                                copiedAlready = i + 1;
                            } else {
                                // append '\' alone (at end)
                                appendPercentEscaped(output, cp);
                                copiedAlready = i + 1;
                            }
                            continue;
                            //
                            //                        case '%': // if we have %xy, and x and y are
                            // hex, escape the %
                            //                            if (i < n - 2 && HEX.contains(cps[i + 1])
                            // && HEX.contains(cps[i + 2])) {
                            //                                // append soft code points
                            //                                appendCodePointsBetween(output, cps,
                            // copiedAlready, i);
                            //                                appendPercentEscaped(output, cp,
                            // escapedCounter);
                            //                                copiedAlready = i + 1;
                            //                                continue;
                            //                            }
                            //                            break;
                    }
                    LinkTermination lt =
                            part.terminators.contains(cp)
                                    ? LinkTermination.HARD
                                    : LinkTermination.PROPERTY_MAP.get(cp);
                    switch (lt) {
                        case INCLUDE:
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            output.appendCodePoint(cp);
                            copiedAlready = i + 1;
                            break;
                        case HARD:
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            appendPercentEscaped(output, cp);
                            copiedAlready = i + 1;
                            continue;
                        case SOFT: // fix
                            continue;
                        case OPEN:
                            openingStack.push(cp);
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            output.appendCodePoint(cp);
                            copiedAlready = i + 1;
                            continue; // fix
                        case CLOSE: // fix
                            if (openingStack.empty()) {
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                appendPercentEscaped(output, cp);
                            } else {
                                Integer topOfStack = openingStack.pop();
                                int matchingOpening = getOpening(cp);
                                if (matchingOpening == topOfStack) {
                                    appendCodePointsBetween(output, cps, copiedAlready, i);
                                    output.appendCodePoint(cp);
                                } else { // failed to match
                                    appendCodePointsBetween(output, cps, copiedAlready, i);
                                    appendPercentEscaped(output, cp);
                                }
                            }
                            copiedAlready = i + 1;
                            continue;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
                if (endStatus == EndStatus.MEDIAL || part != lastPart) {
                    appendCodePointsBetween(output, cps, copiedAlready, n);
                } else if (copiedAlready < n) {
                    appendCodePointsBetween(output, cps, copiedAlready, n - 1);
                    appendPercentEscaped(output, cps[n - 1]);
                }
            }

            return output.toString();
        }

        public String fullEscape() {
            Output<Part> last = new Output<>();
            String result =
                    data.entrySet().stream()
                            .map(
                                    x -> {
                                        Part part = x.getKey();
                                        last.value = part;
                                        List<List<String>> value = x.getValue();
                                        return part.fullEscape(value);
                                    })
                            .collect(Collectors.joining());
            // handle the very last character, if soft
            if (last.value != null && last.value != Part.HOST && last.value != Part.PROTOCOL) {
                int lastCodePoint = result.codePointBefore(result.length());
                if (LinkTermination.SOFT.base.contains(lastCodePoint)) {
                    // escape final soft code point
                    int indexBeforeLast = result.length() - Character.charCount(lastCodePoint);
                    StringBuilder resultBuilder =
                            new StringBuilder(result.substring(0, indexBeforeLast));
                    appendPercentEscaped(resultBuilder, lastCodePoint);
                    result = resultBuilder.toString();
                }
            }
            return result;
        }

        public List<List<String>> get(Part part) {
            return data.get(part);
        }

        @Override
        public String toString() {
            return toString(Part.ALL); // show all
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UrlInternals) {
                return data.equals(((UrlInternals) obj).data);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(data);
        }

        public String toString(Set<Part> partsToShow) {
            StringBuilder result = new StringBuilder("{");
            for (Entry<Part, List<List<String>>> partListList : data.entrySet()) {
                Part part = partListList.getKey();
                List<List<String>> listList = partListList.getValue();
                int index1 = -1;
                for (List<String> list1 : listList) {
                    ++index1;
                    int index2 = -1;
                    for (String value : list1) {
                        ++index2;
                        if (result.length() != 1) {
                            result.append(" ");
                        }
                        switch (part.structure) {
                            case single:
                                // we don't care about interior structure
                                result.append(part == Part.PROTOCOL ? "𝑺" : "𝑯");
                                break;
                            case listP:
                                result.append(part.structure.prefix);
                                break;
                            case listF:
                                // Hack the difference between Fragment and FragmentDirective for
                                // now
                                result.append(index1 == 0 ? part.structure.prefix : "𝑫");
                                break;
                            case listQ2:
                                result.append(
                                        index2 == 0
                                                ? part.structure.prefix
                                                : part.structure.prefix2);
                                break;
                        }
                        result.append('=');
                        result.append(value);
                    }
                }
            }
            return result.append("}").toString();
        }
    }

    private static String joinListListEscaping(
            String sep, String sep2, List<List<String>> partParts) {
        if (sep.codePointCount(0, sep.length()) == 0
                || (sep2 != null && sep2.codePointCount(0, sep2.length()) == 0)) {
            throw new IllegalArgumentException();
        }
        UnicodeSet toEscape = new UnicodeSet().add(sep);
        if (sep2 != null) {
            toEscape.add(sep2);
        }
        return partParts.stream()
                .map(
                        x -> {
                            return joinListEscaping(sep2, toEscape, x);
                        })
                .collect(Collectors.joining(sep));
    }

    private static String joinListEscaping(
            String sep, UnicodeSet toEscape, List<String> partParts) {
        return sep == null
                ? escape(partParts.get(0), toEscape)
                : partParts.stream()
                        .map(
                                x -> {
                                    return escape(x, toEscape);
                                })
                        .collect(Collectors.joining(sep));
    }

    private static List<List<String>> cleanList(Part part, String partString) {
        return splitList(part, part.structure.sub, partString).stream()
                .map(
                        x -> {
                            return List.of(x);
                        })
                .collect(Collectors.toUnmodifiableList());
    }

    private static List<String> splitList(Part part, String separator, String partString) {
        return Splitter.on(separator)
                .splitToStream(partString)
                .map(x -> unescape(x))
                .collect(Collectors.toList());
    }

    /**
     * Set lastSafe to 0 — this marks the last code point that is definitely included in the
     * linkification.<br>
     * Set closingStack to empty<br>
     * Set the current code point position i to 0<br>
     * Loop from i = 0 to n<br>
     * Set LT to LinkTermination(cp[i])<br>
     * If LT == none, set lastSafe to be i+1, continue loop<br>
     * If LT == soft, continue loop<br>
     * If LT == hard, stop linkification and return lastSafe<br>
     * If LT == opening, push cp[i] onto closingStack<br>
     * If LT == closing, set open to the pop of closingStack, or 0 if the closingStack is empty<br>
     * If LinkPairedOpeners(cp[i]) == open, set lastSafe to be i+1, continue loop.<br>
     * Otherwise, stop linkification and return lastSafe<br>
     * If lastSafe == n+1, then the entire part is safe; continue to the next part<br>
     * Otherwise, stop linkification and return lastSafe<br>
     */
    public static int parsePathQueryFragment(String source, int codePointOffset) {
        // For simplicity, and to match the spec, we just get the code points
        // Production code would be optimized, of course.

        int[] codePoints = source.codePoints().toArray();
        int lastSafe = codePointOffset;
        Part part = null;

        Stack<Integer> openingStack = new Stack<>();
        LinkTermination lt = LinkTermination.SOFT;
        for (int i = codePointOffset; i < codePoints.length; ++i) {
            int cp = codePoints[i];
            if (part == null) {
                part = Part.fromInitiator(cp);
                if (part == null) {
                    return i; // failed, don't move cursor
                }
                lastSafe = i + 1;
                continue;
            }

            lt = LinkTermination.PROPERTY_MAP.get(cp);
            // handle terminators and interior syntax
            if (part.terminators.contains(cp)) {
                lastSafe = i;
                part = Part.fromInitiator(cp);
                if (part == null) {
                    return lastSafe;
                }
                lastSafe = i + 1;
                continue;
            } else if (part.clearStack.contains(cp)) {
                openingStack.clear();
            } else if (part == Part.FRAGMENT
                    && matches(codePoints, i, Part.FRAGMENT_DIRECTIVE_STRING)) {
                // there is one string form, so hard-code it
                openingStack.clear();
            }
            switch (lt) {
                case INCLUDE:
                    lastSafe = i + 1;
                    break;
                case SOFT: // no action
                    break;
                case HARD:
                    return lastSafe;
                case OPEN:
                    // We're not testing for stack limits; not needed for test data
                    openingStack.push(cp);
                    lastSafe = i + 1;
                    break;
                case CLOSE:
                    if (openingStack.empty()) {
                        return lastSafe;
                    }
                    int matchingOpening = getOpening(cp);
                    Integer topOfStack = openingStack.pop();
                    if (matchingOpening == topOfStack) {
                        lastSafe = i + 1;
                        break;
                    } // else failed to match
                    return lastSafe;
            }
        }
        // if we hit the end, it acts like we hit a hard character ***
        return lt == LinkTermination.SOFT ? lastSafe : codePoints.length;
    }

    /** Simple utility for finding matching int array regions */
    private static boolean matches(int[] codePoints, int cpIndex, int[] fragmentDirective) {
        if (cpIndex + fragmentDirective.length > codePoints.length) {
            return false;
        }
        for (int i = 0; i < fragmentDirective.length; ++i) {
            if (codePoints[i + cpIndex] != fragmentDirective[i]) {
                return false;
            }
        }
        return true;
    }

    private static void appendCodePointsBetween(
            StringBuilder output, int[] cp, int copyEnd, int notToCopy) {
        for (int i = copyEnd; i < notToCopy; ++i) {
            output.appendCodePoint(cp[i]);
        }
    }

    /** Regex for percent escaping */
    private static final Pattern ESCAPED_BYTE_PATTERN =
            Pattern.compile("(%[a-fA-F0-9][a-fA-F0-9])+");

    private static final Pattern ESCAPED_SINGLE = Pattern.compile("%[a-fA-F0-9][a-fA-F0-9]");

    /** Unescape a string. */
    private static String unescape(String stringWithEscapes) {
        return unescape(stringWithEscapes, UnicodeSet.EMPTY); // no characters escaped bak
    }

    /** Unescape a string; however, code points in toEscape are escaped back. */
    private static String unescape(String stringWithEscapes, UnicodeSet toEscape) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = ESCAPED_BYTE_PATTERN.matcher(stringWithEscapes);
        int current = 0;
        while (matcher.find(current)) {
            result.append(
                    stringWithEscapes.substring(
                            current, matcher.start())); // append intervening text
            String unescaped = percentUnescape(matcher.group());
            unescaped
                    .chars()
                    .forEach(
                            x -> {
                                if (toEscape.contains(x)) {
                                    // quote it
                                    appendPercentEscaped(result, x);
                                } else {
                                    result.appendCodePoint(x);
                                }
                            });
            current = matcher.end();
        }
        result.append(stringWithEscapes.substring(current, stringWithEscapes.length()));
        return result.toString();
    }

    private static void appendPercentEscaped(StringBuilder output, int cp) {
        byte[] bytes = Character.toString(cp).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            output.append('%');
            output.append(Utility.hex(bytes[i] & 0xFF, 2));
        }
    }

    private static void appendPercentEscaped(StringBuilder output, String source) {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            output.append('%');
            output.append(Utility.hex(bytes[i] & 0xFF, 2));
        }
    }

    /** percent-escape all the toEscape characters. */
    public static String escape(String source, UnicodeSet toEscape) {
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (true) {
            int start = toEscape.span(source, lastEnd, SpanCondition.NOT_CONTAINED);
            appendCheckingPercent(result, source, lastEnd, start);
            if (start == source.length()) {
                break;
            }
            lastEnd = toEscape.span(source, start, SpanCondition.SIMPLE);
            appendPercentEscaped(result, source.substring(start, lastEnd));
            if (lastEnd == source.length()) {
                break;
            }
        }
        return result.toString();
    }

    /** If we have in an unescaped string %xy, and xy are hex, then we have to escape the % sign. */
    private static void appendCheckingPercent(
            StringBuilder toAppendTo, String source, int lastEnd, int start) {
        int pos = source.indexOf("%", lastEnd); // quickcheck
        if (pos < 0 || pos >= start) {
            toAppendTo.append(source, lastEnd, start);
            return;
        }
        String sub = source.substring(lastEnd, start);
        Matcher matcher = ESCAPED_SINGLE.matcher(sub);
        Function<MatchResult, String> replacer =
                x -> {
                    return "%25"
                            + x.group(0)
                                    .substring(1); // separate the % from the xy, and return %25xy
                };
        String fixed = matcher.replaceAll(replacer);
        toAppendTo.append(fixed);
    }

    /** We are guaranteed that string is all percent escaped utf8, %a3%c0 ... */
    private static String percentUnescape(String escapedSource) {
        byte[] temp = new byte[escapedSource.length() / 3];
        int tempOffset = 0;
        for (int i = 0; i < escapedSource.length(); i += 3) {
            if (escapedSource.charAt(i) != '%') {
                throw new IllegalArgumentException();
            }
            byte b = (byte) Integer.parseInt(escapedSource.substring(i + 1, i + 3), 16);
            temp[tempOffset++] = b;
        }
        return new String(temp, StandardCharsets.UTF_8);
    }

    public static Map<String, List<List<String>>> getInternals(String source) {
        UrlInternals ui = UrlInternals.from(source.toString());
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        for (Part p : Part.values()) {
            List<List<String>> list = ui.get(p);
            if (list != null) {
                result.put(p.toString(), list);
            }
        }
        return result;
    }
}
