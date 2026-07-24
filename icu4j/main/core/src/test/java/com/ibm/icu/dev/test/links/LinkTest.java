// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.links;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.links.LinkHandlingUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.LinkUtilities;
import com.ibm.icu.util.LinkUtilities.Extent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test the APIs in LinkUtilties using some basic tests, plus the test data from UTS58 Note: the
 * UTC58 tests check whole links, since that is easiest for implementers to write tests against.<br>
 * However, the Unicode testdata currently takes certain positions on edge cases that some client
 * software might not agree with. A simple case is that it only recognizes TLDs that are valid, not
 * just well formed. It mostly handles this by not using invalid TLDs. So it doesn't use
 * "example.somefuturetld" as a test case. But it does take some positions that theoretically
 * clients might disagree with, such as not recognizing "efg.com" in "abc..efg.com". This currently
 * tests with code that duplicates that.<br>
 * There are also some edge cases
 *
 * <table>
 * <tr><th>Text</th><th>Approach</th></tr>
 * <tr><td>dav.is@dav.is/dav.is
 * </td><td>Note that a string may both be a valid local-part and a valid domain name.
 * The UTS58 test data recognizes the start of a link;
 * recognizing the email address (dav.is@dav.is), and skipping the PDF (/dav.is).
 * </td></tr>
 * <tr><td>mailto:αβγ..δεζ@example.com
 * </td><td>A failing email local-part (αβγ..δεζ) fails as a whole (eg δεζ@example.com is not recognized)
 * </td></tr>
 * </table>
 */
@RunWith(JUnit4.class)
public class LinkTest extends CoreTestFmwk {
    /**
     * There are 2 format test cases that need to be fixed in UTS58 In each of these, the = in a 𝑽
     * doesn't need to be quoted, because it doesn't initiate a different component.
     */
    static final Set<String> SKIP_FORMAT_TEST =
            Set.of(
                    "# {𝑺=https:// 𝑯=example.com 𝑸=α=β 𝑽=γ=δ}",
                    "# {𝑺=https:// 𝑯=example.com 𝑷=α 𝑷=b/?#c 𝑸=αβ 𝑽=γ&ζ=#Ξ 𝑸=k 𝑽=v 𝑭=frag}");

    /**
     * There is 1 detection test case that need to be removed in UTS58 UTS58 checks for valid domain
     * names, and for neutrality it should avoid listing such cases
     */
    static final Set<String> SKIP_DETECTION_TEST =
            Set.of("Lorem ipsum موريتانيا.xn-----ctdbabcfhu9c2b9l1acccr4c dolor sit amet");

    /** Constructor */
    public LinkTest() {}

    @Test
    public void test1PairedBracket() {
        // int actual = // UCharacter.getIntPropertyValue(')', UProperty.BIDI_PAIRED_BRACKET);
        int actual = UCharacter.getBidiPairedBracket(')');
        assertEquals("", '(', actual);
    }

    @Test
    public void testSimpleScan() {
        checkScan("/α(β) on…", "α(β)");
        checkScan("/αβγ?def#ghi ", "ghi");
        // checkScan("⸠https://ja.wikipedia.org/wiki/フィンセント・ファン・ゴッホ", "ゴッホ");
    }

    private void checkScan(String toCheck, String expectedAfter) {
        String source = toCheck;
        int expected = source.indexOf(expectedAfter) + expectedAfter.length();
        int actual = LinkUtilities.scanPathQueryFragment(source, 0, source.length());
        assertEquals(source, expected, actual);
    }

    @Test
    public void testSimpleEscape() {
        List<List<String>> tests = // source, minimal escaping, maximal escaping
                List.of(
                        List.of(
                                "https://example.com/αβγ/δεζ?θ=ικλ&μ=γξο#πρς",
                                "https://example.com/αβγ/δεζ?θ=ικλ&μ=γξο#πρς",
                                "https://example.com/%CE%B1%CE%B2%CE%B3/%CE%B4%CE%B5%CE%B6?%CE%B8=%CE%B9%CE%BA%CE%BB&%CE%BC=%CE%B3%CE%BE%CE%BF#%CF%80%CF%81%CF%82"),
                        List.of("https://example.com/α/β%2Fγ", "https://example.com/α/β%2Fγ", ""),
                        List.of(
                                "https://example.com/α%23β?γ=δ%23ε",
                                "https://example.com/α%23β?γ=δ%23ε", ""),
                        List.of(
                                "ex.com/%CE%B1%CE%B2%CE%B3/%CE%B4.%CE%B5%CE%B6%2E",
                                "ex.com/αβγ/δ.εζ%2E", ""),
                        List.of(
                                "https://example.com/α%3Fμπ",
                                "", "https://example.com/%CE%B1%3F%CE%BC%CF%80"));
        for (List<String> test : tests) {
            String source = test.get(0);
            String expectedMin = test.get(1);
            String expectedMax = test.get(2);
            if (!expectedMin.isEmpty()) {
                String actual = LinkUtilities.escapePathQueryFragment(source, Extent.MINIMAL);
                assertEquals(
                        "MIN " + LinkHandlingUtilities.getInternals(source), expectedMin, actual);
            }
            if (!expectedMax.isEmpty()) {
                String actual = LinkUtilities.escapePathQueryFragment(source, Extent.MAXIMAL);
                assertEquals(
                        "MAX " + LinkHandlingUtilities.getInternals(source), expectedMax, actual);
            }
        }
    }

    @Test
    public void testSimpleScanBackEmail() {
        checkEmail("See αβγ@", 0, "αβγ");
        checkEmail("See mailto:αβγ@", 0, "mailto:α");
        checkEmail("See mailto:αβγ.δεζ.@", 0, null);
        checkEmail("See mailto:αβγ.δεζ@", 0, "mailto:α");
    }

    private void checkEmail(String source, int start, String localPartStart) {
        int end = source.indexOf('@') + 1;
        int expected = localPartStart == null ? source.length() : source.indexOf(localPartStart);
        int actual = LinkUtilities.scanBackEmailLocalPart(source, start, end);
        assertEquals(source, expected, actual);
    }

    @Test
    public void testHackDomainName() {
        String source = // "普遍适用测试。我爱你";
                "موريتانيا.xn-----ctdbabcfhu9c2b9l1acccr4c";
        IntRange domain = new IntRange();
        boolean actual = hackFindDomainName(source, 0, source.length(), false, domain);
        assertEquals(source, true, actual);
        assertTrue(source, domain.start == 0 && domain.end == source.length());
    }

    // Would like to use the following for JUnit, but can't
    //  @ParameterizedTest
    //  @MethodSource("testItemsProvider")

    Pattern SCHEME = Pattern.compile("https?://$");
    Pattern PORT = Pattern.compile(":\\d+");

    @Test
    public void testAgainstLinkDetectionTest() {
        List<String> failures = new ArrayList<>();
        try (BufferedReader idnaTestFile =
                TestUtil.getUtf8DataReader("unicode/links/LinkDetectionTest.txt"); ) {
            for (final String line : (Iterable<String>) idnaTestFile.lines()::iterator) {
                if (line.startsWith("#") || line.isBlank() || SKIP_DETECTION_TEST.contains(line)) {
                    continue;
                }
                StringBuilder actual = new StringBuilder();
                String rawLine2 = line.replace("⸠", "").replace("⸡", "");
                int start = 0;
                IntRange domain = new IntRange();
                IntRange domainAtDomain = new IntRange();
                try {
                    while (hackFindDomainName(rawLine2, start, rawLine2.length(), false, domain)) {
                        boolean haveEmail = false;
                        // check for '@' before the domain name
                        // if there is one, we see if there is an email address. If not,
                        // we skip over the domain name
                        if (domain.start > 0
                                && UCharacter.codePointBefore(rawLine2, domain.start) == '@') {
                            // handle email
                            // check backwards for email
                            int local_part_start =
                                    LinkUtilities.scanBackEmailLocalPart(
                                            rawLine2, start, domain.start);
                            if (local_part_start != domain.start) {
                                domain.start = local_part_start;
                                haveEmail = true;
                            } else {
                                // we skip over the domain name
                                actual.append(rawLine2.subSequence(start, domain.end));
                                start = domain.end;
                                continue;
                            }

                        } else { // we could have chanced on a case where the domain name is also
                            // a local_part (like john.uk@foo.com)
                            // For that case we check if there is an @ sign *after* the end of the
                            // domain name.
                            // If so:
                            // - we scan forwards (after the @) to see if there is a domain name,
                            // AND
                            // - we scan backwards (before the @) to see if there is local_part,
                            // AND
                            // - we scan backwards before that to ensure that there is no https?://
                            // if both are true, we have an email address.
                            // if only the first is true, we skip over the 2nd domain name;
                            // ie, we skip over " @example.com"
                            if (domain.end < rawLine2.length()
                                    && UCharacter.codePointAt(rawLine2, domain.end) == '@') {
                                if (hackFindDomainName(
                                        rawLine2,
                                        domain.end + 1,
                                        rawLine2.length(),
                                        true,
                                        domainAtDomain)) {
                                    domain.start =
                                            LinkUtilities.scanBackEmailLocalPart(
                                                    rawLine2, start, domain.end + 1);
                                    boolean failing = domain.start == domain.end + 1;
                                    if (!failing) {
                                        // slash is a valid email character, so check
                                        // two characters after
                                        Matcher schemeMatcher = SCHEME.matcher(rawLine2);
                                        schemeMatcher.region(start, domain.start + 2);
                                        failing = schemeMatcher.find();
                                    }
                                    if (failing) {
                                        // failed to find local part, or found scheme
                                        // we failed, so copy up to here so we don't rescan
                                        actual.append(
                                                rawLine2.subSequence(start, domainAtDomain.end));
                                        start = domainAtDomain.end;
                                        continue;
                                    }
                                    if (UCharacter.codePointBefore(rawLine2, domainAtDomain.end)
                                            == '.') {
                                        // backup; might be end of sentence
                                        domainAtDomain.end--;
                                    }
                                    domain.end = domainAtDomain.end;
                                    haveEmail = true;
                                }
                            }
                        }
                        // we only check for PQF if we don't have an email address
                        if (!haveEmail) {
                            // handle PQF
                            // check backwards for scheme "[a-z]://"
                            Matcher schemeMatcher = SCHEME.matcher(rawLine2);
                            schemeMatcher.region(start, domain.start);
                            if (schemeMatcher.find()) {
                                domain.start = schemeMatcher.start();
                            }

                            Matcher portMatcher = PORT.matcher(rawLine2);
                            portMatcher.region(domain.end, rawLine2.length());
                            if (portMatcher.lookingAt()) {
                                domain.end = portMatcher.end();
                            }

                            // check forwards for PQF
                            int pqfEnd =
                                    LinkUtilities.scanPathQueryFragment(
                                            rawLine2, domain.end, rawLine2.length());
                            if (pqfEnd == domain.end) {
                                // special case dot at end of domain name and no PQF
                                if (UCharacter.codePointBefore(rawLine2, domain.end) == '.') {
                                    // backup; might be end of sentence
                                    pqfEnd--;
                                }
                            }
                            domain.end = pqfEnd;
                        }
                        actual.append(rawLine2.subSequence(start, domain.start))
                                .append("⸠")
                                .append(rawLine2.subSequence(domain.start, domain.end))
                                .append("⸡");
                        start = domain.end;
                    }
                    actual.append(rawLine2.substring(start));

                    assertEquals("", line, actual.toString());
                    if (isVerbose()) {
                        System.out.println("OK\t" + line);
                    }
                } catch (AssertionError e) {
                    failures.add("FAIL " + e.getMessage());
                    if (isVerbose()) {
                        System.out.println("FAIL\t" + line + "\t  " + actual.toString());
                    }
                }
            }
            if (!failures.isEmpty()) {
                fail(
                        "Test finished with "
                                + failures.size()
                                + " failures:\n"
                                + String.join("\n", failures));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testAgainstLinkFormattingTest() {
        //        	# All parts
        //        	# {𝑺=https:// 𝑯=example.com 𝑷=αβγ 𝑷=δεζ 𝑸=θ 𝑽=ικλ 𝑸=μ 𝑽=γξο 𝑭=πρς}
        //
        //	https://example.com/%CE%B1%CE%B2%CE%B3/%CE%B4%CE%B5%CE%B6?%CE%B8=%CE%B9%CE%BA%CE%BB&%CE%BC=%CE%B3%CE%BE%CE%BF#%CF%80%CF%81%CF%82
        //        	https://example.com/αβγ/δεζ?θ=ικλ&μ=γξο#πρς

        List<String> failures = new ArrayList<>();
        // the test case is broken onto 3 lines for ease of reading
        // so we assemble each one before testing.
        List<String> testCase = new ArrayList<>();
        try (BufferedReader idnaTestFile =
                TestUtil.getUtf8DataReader("unicode/links/LinkFormattingTest.txt"); ) {
            String parts = null;
            String pqf = null;
            StringBuilder schemeHost = new StringBuilder();
            String minimalPqf = null;
            String maximalPqf = null;
            for (String line : (Iterable<String>) idnaTestFile.lines()::iterator) {
                try {
                    if (line.startsWith("# {")) {
                        testCase.clear();
                        testCase.add(line);
                    } else if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    } else if (testCase.size() == 1) {
                        testCase.add(line);
                    } else { // we have assembled the first two lines, so we can process
                        String partsLine = testCase.get(0);
                        if (SKIP_FORMAT_TEST.contains(partsLine)) {
                            continue;
                        }
                        parts =
                                partsLine.substring(
                                        3, partsLine.length() - 1); // just used in reporting
                        pqf = assemblePqf(partsLine, schemeHost);
                        String maximalLine = testCase.get(1);
                        String minimalLine = line;

                        // Check explicit test cases

                        maximalPqf = LinkUtilities.escapePathQueryFragment(pqf, Extent.MAXIMAL);
                        assertEquals(
                                Extent.MAXIMAL + " " + parts, maximalLine, schemeHost + maximalPqf);

                        minimalPqf = LinkUtilities.escapePathQueryFragment(pqf, Extent.MINIMAL);
                        assertEquals(
                                Extent.MINIMAL + " " + parts, minimalLine, schemeHost + minimalPqf);

                        // TODO Look at roundtripping both of these.
                    }
                    if (isVerbose()) {
                        System.out.println("OK " + line);
                    }
                } catch (AssertionError e) {
                    failures.add("FAIL " + e.getMessage());
                    if (isVerbose()) {
                        System.out.println("FAIL\t" + line);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!failures.isEmpty()) {
            fail(
                    String.join(
                            " ",
                            "Test finished with",
                            failures.size() + "",
                            "failures:\n" + String.join("\n", failures)));
        }
    }

    @Test
    public void testParts() {
        String source =
                "# {𝑺=https:// 𝑯=example.com 𝑷=αβγ 𝑷=δεζ 𝑸=θ 𝑽=ικλ 𝑸=μ 𝑽=γξο 𝑭=πρς}";
        List<List<String>> expected = new ArrayList<>();
        expected.add(List.of("𝑺", "https://"));
        expected.add(List.of("𝑯", "example.com"));
        expected.add(List.of("𝑷", "αβγ"));
        expected.add(List.of("𝑷", "δεζ"));
        expected.add(List.of("𝑸", "θ"));
        expected.add(List.of("𝑽", "ικλ"));
        expected.add(List.of("𝑸", "μ"));
        expected.add(List.of("𝑽", "γξο"));
        expected.add(List.of("𝑭", "πρς"));
        List<List<String>> actual = getParts(source);
        assertEquals(source, expected, actual);
    }

    Pattern UrlTypes = Pattern.compile("([𝑺𝑯𝑷𝑸𝑽𝑭])=([^𝑺𝑯𝑷𝑸𝑽𝑭]+)");

    private List<List<String>> getParts(String line) {
        // # {𝑺=https:// 𝑯=example.com 𝑷=αβγ 𝑷=δεζ 𝑸=θ 𝑽=ικλ 𝑸=μ 𝑽=γξο 𝑭=πρς}
        line = line.substring(3, line.length() - 1); // subtract the # { and the }
        Matcher urlType = UrlTypes.matcher(line);
        List<List<String>> result = new ArrayList<>();
        while (urlType.find()) {
            String key = urlType.group(1);
            String value = urlType.group(2);
            if (value.endsWith(" ")) {
                value = value.substring(0, value.length() - 1);
            }
            result.add(List.of(key, value));
        }
        return List.copyOf(result);
    }

    /**
     * When composing a URL, the following have to be escaped within each part. They are the
     * characters that could initiate a different component
     */
    final UnicodeSet PATH_ESCAPE = new UnicodeSet("[/?#]").freeze();

    final UnicodeSet QUERY_ESCAPE = new UnicodeSet("[\\&=#]").freeze();
    final UnicodeSet VALUE_ESCAPE = new UnicodeSet("[\\&#]").freeze();

    private String assemblePqf(String line, StringBuilder schemeHost) {
        StringBuilder sb = new StringBuilder();
        schemeHost.setLength(0);
        List<List<String>> parts = getParts(line);
        char query = '?';
        for (List<String> entry : parts) {
            String type = entry.get(0);
            String value = entry.get(1);
            // # {𝑺=https:// 𝑯=example.com 𝑷=αβγ 𝑷=δεζ 𝑸=θ 𝑽=ικλ 𝑸=μ 𝑽=γξο 𝑭=πρς}
            switch (type) {
                case "𝑺":
                case "𝑯":
                    if (value != null) {
                        schemeHost.append(value);
                    }
                    break;
                case "𝑷":
                    sb.append('/').append(escape(value, PATH_ESCAPE));
                    break;
                case "𝑸":
                    sb.append(query).append(escape(value, QUERY_ESCAPE));
                    query = '&';
                    break;
                case "𝑽":
                    sb.append('=').append(escape(value, VALUE_ESCAPE));
                    break;
                case "𝑭":
                    sb.append('#').append(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected line: " + line);
            }
        }
        return sb.toString();
    }

    private Object escape(String value, UnicodeSet unicodeSet) {
        return LinkHandlingUtilities.escape(value, unicodeSet);
    }

    static final class IntRange {
        int start;
        int end;

        @Override
        public String toString() {
            return start + ";" + end;
        }
    }

    Pattern DOT = Pattern.compile("[.。]");
    Pattern DOT2 = Pattern.compile("[.。][.。]");

    /**
     * The UTS58 tests take whole domain names, because that is the easiest for clients to test. We
     * have to hack it because ICU IDN support doesn't provide enough API. Returns true if found,
     * with the range in result.
     *
     * @param mustBeAtStart TODO check with isValidDomain
     */
    boolean hackFindDomainName(
            CharSequence source, int start, int limit, boolean mustBeAtStart, IntRange result) {
        if (mustBeAtStart && start < limit) {
            int firstCodePoint = UCharacter.codePointAt(source, start);
            if (!HACK_DOMAIN_SET.contains(firstCodePoint)) {
                return false;
            }
        }
        Matcher dot = DOT.matcher(source);
        Matcher dot2 = DOT2.matcher(source);
        while (start < limit) {
            int endNot = HACK_DOMAIN_SET.span(source, start, SpanCondition.NOT_CONTAINED);
            if (endNot > limit) {
                return false;
            }
            int end = HACK_DOMAIN_SET.span(source, endNot, SpanCondition.SIMPLE);
            if (end > limit) {
                end = limit;
            }
            if (end == endNot) {
                return false;
            }
            // Check that the possible domain name (endNot..end) is of the right form
            // otherwise continue after end.
            // Must not start with [.。], and must find a [.。] before the end
            dot.region(endNot, end);
            boolean found = dot.find();
            if (!found || dot.start() == endNot || dot.end() == end) {
                start = end;
                continue;
            }
            // Must not find [.。][.。]
            dot2.region(endNot, end);
            if (dot2.find()) {
                start = end;
                continue;
            }
            // and a final test for valid
            if (!isValidDomain(source.subSequence(endNot, end).toString())) {
                start = end;
                continue;
            }
            result.start = endNot;
            result.end = end;
            return true;
        }
        return false;
    }

    private final UnicodeSet HACK_DOMAIN_SET =
            new UnicodeSet(
                            "[。.\\-0-9a-z·ß-öø-ÿāăą ćĉċčďđēĕėęěĝğġģĥħĩ"
                                    + "īĭį ıĵķĸĺļľłńņňŋōŏőœŕŗřśŝ şšţťŧũūŭůűųŵŷźżžƀƃƅƈƌ ƍƒƕƙ-ƛƞơƣƥƨƪƫƭưƴƶƹ-ƻƽ-ǃ ǎǐ"
                                    + "ǒǔǖǘǚǜǝǟǡǣǥǧǩǫǭǯǰǵǹ ǻǽǿȁȃȅȇȉȋȍȏȑȓȕȗșțȝȟȡȣ ȥȧȩȫȭȯȱȳ-ȹȼȿɀɂɇɉɋɍɏ-ʯ ʹ-ˁˆ-ˑˬˮ̀-̿͂͆-͎͐-ͯͱͳ͵ ͷ-"
                                    + "͹ ͻ-ͽ΀-΃΋΍ ΐ΢ ά-ώϗϙϛϝϟϡϣϥϧϩϫϭϯϳϸϻϼа-џ ѡѣѥѧѩѫѭѯѱѳѵѷѹѻѽѿҁ҃-҇ҋ ҍҏґғҕҗҙқҝҟҡңҥҧҩҫ"
                                    + "ҭүұҳҵ ҷҹһҽҿӂӄӆӈӊӌӎӏӑӓӕӗәӛӝӟ ӡӣӥӧөӫӭӯӱӳӵӷӹӻӽӿԁԃԅԇԉ ԋԍԏԑԓԕԗԙԛԝԟԡԣԥԧԩԫԭԯ԰՗-ՙ ՠ-ֆֈ"
                                    + "֋֌֐-ׇֽֿׁׂׅׄ-׿ؐ-ؚ ؠ-ؿف-٩ٮ-ٴٹ-ۓە-ۜ۟-۪ۨ-ۿ܎ ܐ-ߵ߻-߽ ࠀ-࠯࠿-࡝࡟-ࢇ ࢉ-࢏࢒-ࣣ࣡-ॗ ॠ-ॣ०-९ॱ-৛৞ ৠ-ৱৼ"
                                    + "৾-ਲ਴ ਵ਷-੘ ੜ੝੟-ੵ੷-૯૲-୛୞-୯ ୱ୸-௯௻-౶ ಀ-ಃಅ-ൎ൐-ൗ ൟ-൯ൺ-ෳ෵-าิ-฾ เ-๎๐-๙๜-າິ-"
                                    + "໛ ໞ-ༀ་༘༙༠-༩༹༵༷༾-གང-ཌཎ-ད ན-བམ-ཛཝ-ཨཪ-ིེུ-ྀྂ-྄྆-ྒྔ-ྜྞ-ྡྣ-ྦྨ-ྫྭ-ྸྺ-྽࿆࿍࿛-၉ ၐ-ႝ჆჈-჌჎-ჺ ჽ-ჿሀ-፟፽-ᎏ᎚-᏷᏾᏿ ᐁ-"
                                    + "ᙬᙯ-ᙿᚁ-ᚚ᚝-ᛪ ᛱ-᜴᜷-ឳា-៓ ៗៜ-៯៺-៿ ᠐-᤿᥁-᥃ ᥆-᧙᧛-᧝ ᨀ-᨝ ᨠ-᪟ ᪧ᪮-᪽ᪿ-᭍ ᭐-᭙᭫-᭳ᮀ-᯻ "
                                    + "ᰀ-᰺ ᱀-ᱽᲊ-᲏᲻᲼᳈-᳔᳒-ᴫ ᴯᴻᵎᵫ-ᵷᵹ-ᶚ᷀-᷿ḁḃḅḇḉḋḍḏḑ ḓḕḗḙḛḝḟḡḣḥḧḩḫḭḯḱḳḵḷḹḻ ḽḿṁṃṅṇṉṋṍṏṑṓṕṗ"
                                    + "ṙṛṝṟṡṣṥ ṧṩṫṭṯṱṳṵṷṹṻṽṿẁẃẅẇẉẋẍẏ ẑẓẕ-ẙẜẝẟạảấầẩẫậắằẳẵặẹ ẻẽếềểễệỉịọỏốồổỗộớờởỡợ ụủứừử"
                                    + "ữựỳỵỷỹỻỽỿ-ἇἐ-἗἞-ἧ ἰ-ἷὀ-὇὎-὘὚὜὞ ὠ-ὧὰὲὴὶὸὺὼ὾὿ ᾰᾱ᾵ ᾶ῅ ῆῐ-ῒ῔-ῗ῜ ῠ-ῢῤ-ῧ"
                                    + "῰῱῵ ῶ῿ ‌‍⁥⁲⁳₏₝-₟⃂-⃏⃱-⃿ ⅎↄ↌-↏␪-␿⑋-⑟⭴⭵ ⰰ-ⱟⱡⱥⱦⱨⱪⱬⱱⱳⱴⱶ-ⱻⲁⲃⲅⲇⲉⲋ ⲍⲏⲑⲓⲕⲗ"
                                    + "ⲙⲛⲝⲟⲡⲣⲥⲧⲩⲫⲭⲯⲱⲳⲵ ⲷⲹⲻⲽⲿⳁⳃⳅⳇⳉⳋⳍⳏⳑⳓⳕⳗⳙⳛⳝⳟ ⳡⳣⳤⳬⳮ-⳱ⳳ-⳸ ⴀ-⵮⵱-ⷿ ⸯ⹞-⹿⺚⻴-⻿⿖-⿯ 々-〇〪-〭"
                                    + "〼぀-゚ ゝゞァ-ヾ㄀-㄰㆏ ㆠ-ㆿ㇦-㇮ ㇰ-ㇿ㈟ 㐀-䶿一-꒏꓇-ꓽ ꔀ-ꘌꘐ-꘿ ꙁꙃꙅꙇꙉꙋꙍꙏꙑꙓꙕꙗꙙꙛꙝꙟꙡꙣ"
                                    + "ꙥꙧꙩ ꙫꙭ-꙯ꙴ-꙽ꙿꚁꚃꚅꚇꚉꚋꚍꚏꚑꚓꚕꚗꚙ ꚛꚞ-ꛥ꛰꛱꛸-꛿ ꜗ-ꜟꜣꜥꜧꜩꜫꜭꜯ-ꜱꜳꜵꜷꜹꜻꜽꜿꝁꝃ ꝅꝇꝉꝋꝍꝏꝑꝓꝕꝗꝙꝛꝝꝟꝡꝣꝥꝧꝩꝫꝭ ꝯꝱ-ꝸꝺꝼꝿ"
                                    + "ꞁꞃꞅꞇꞈꞌꞎꞏꞑꞓ-ꞕꞗꞙ ꞛꞝꞟꞡꞣꞥꞧꞩꞯꞵꞷꞹꞻꞽꞿꟁꟃꟈꟊꟍ꟏ ꟑꟓꟕꟗꟙꟛ꟝-꟰ ꟶꟷꟺ-ꠧ꠬-꠯꠺-ꡳ꡸-꣍ ꣐-ꣷꣻꣽ-꤭ꤰ-꥞꥽-꧀꧎-꧝ "
                                    + "ꧠ-꩛ ꩠ-ꩶꩺ-ꫝꫠ-ꫯꫲ-ꭚꭠ-ꭨ꭬-꭯ ꯀ-ꯪ꯬-힯퟇-퟊퟼-퟿ 﨎﨏﨑﨓﨔﨟﨡﨣﨤﨧-﨩﩮﩯﫚-﫿﬇-﬒"
                                    + "﬘-﬜ﬞ﬷﬽﬿﭂﭅︚-︯﹓﹧﹬-﹯ ﹳ﹵﻽﻾＀﾿-￁￈￉￐￑￘￙￝-￟￧￯-￸ 𐀀-𐃿𐄃-𐄆𐄴-𐄶𐆏𐆝"
                                    + "-𐆟𐆡-𐇏𐇽-𐋠𐋼-𐌟𐌤-𐍀 𐍂-𐍉𐍋-𐎞 𐎠-𐏏𐏖-𐏿 𐐨-𐒯𐓔-𐕮𐕻𐖋𐖓𐖖-𐞀𐞆𐞱𐞻-𐡖 𐡠-𐡶𐢀-𐢦𐢰-"
                                    + "𐣺 𐤀-𐤕𐤜-𐤞 𐤠-𐤾 𐥀-𐦻 "
                                    + "𐦾𐦿𐧐𐧑 𐨀-𐨿𐩉-𐩏𐩙-𐩼 "
                                    + "𐪀-𐪜𐪠-𐫇 𐫉-𐫪𐫷-𐬸 𐭀-"
                                    + "𐭗 𐭠-𐭷 𐮀-𐮘𐮝-𐮨𐮰"
                                    + "-𐱿𐲳-𐳹 𐴀-𐵏𐵦-𐵭 𐵯-"
                                    + "𐶍𐶐-𐹟𐹿-𐺬𐺮-𐻏𐻙-𐼜"
                                    + " 𐼧-𐽐𐽚-𐾅𐾊-𐿄𐿌-𑁆𑁎-𑁑 𑁦-𑂺𑃂-𑃌𑃎-𑄿 𑅄-𑅳𑅶-𑇄𑇉-𑇌𑇎-𑇚𑇜𑇠𑇵-𑈷𑈾-𑊨𑊪-𑏓𑏖𑏙-𑑊 𑑐-𑑙𑑜𑑞-"
                                    + "𑓅 𑓇-𑗀𑗘-𑙀𑙄-𑙟𑙭-𑚸𑚺-𑜹 𑝀-𑠺𑠼-𑢟 𑣀-𑣩𑣳-𑥃𑥇-𑧡 𑧣-𑨾𑩇-𑪙𑪝𑪣-𑫿𑬊-𑯠𑯢-𑱀𑱆-𑱙𑱭-𑱯 "
                                    + "𑱲-𑻶𑻹-𑽂 𑽐-𑾿𑿲-𑿾 𒀀-𒏿𒑯𒑵-𒿰𒿳-𓐯𓑀-𖩭 𖩰-𖫴𖫶-𖬶 𖭀-𖭃𖭆-𖭚𖭢-𖵬 𖵰-𖸿 𖹠-𖹿𖺛-𖺟𖺹-𖿡 "
                                    + "𖿣-𖿳𖿷-𛲛𛲝𛲞𛲤-𜯿𜳽-𜳿𜺴-𜺹𜻑-𜻟𜻱-𜽏𜿄-𜿿𝃶-𝃿𝄧𝄨𝇫-𝇿𝉆-𝊿𝋔-𝋟𝋴-𝋿𝍗-𝍟𝍹-𝏿𝑕𝒝𝒠𝒡𝒣𝒤"
                                    + "𝒧𝒨𝒭𝒺𝒼𝓄𝔆𝔋𝔌𝔕𝔝𝔺𝔿𝕅𝕇-𝕉𝕑𝚦𝚧𝟌𝟍𝨀-𝨶𝨻-𝩬𝩵𝪄𝪌-𞀯𞁮-𞅎𞅐-𞋾𞌀-𞗾𞘀-𞣆𞣐-𞣿 𞤢-𞥝𞥠-𞱰𞲵-𞴀𞴾-𞷿𞸄𞸠𞸣𞸥𞸦𞸨𞸳𞸸𞸺𞸼-𞹁𞹃-𞹆𞹈𞹊𞹌𞹐𞹓𞹕𞹖𞹘𞹚𞹜𞹞𞹠𞹣𞹥𞹦𞹫𞹳𞹸𞹽𞹿𞺊𞺜"
                                    + "-𞺠𞺤𞺪𞺼-𞻯𞻲-𞿿🀬-🀯🂔-🂟🂯🂰🃀🃐🃶-🃿🆮-🇥🈃-🈏🈼-🈿🉉-🉏🉒-🉟🉦-🋿🛙-🛛🛭-🛯🛽-🛿🟚-🟟🟬-🟯🟱-"
                                    + "🟿🠌-🠏🡈-🡏🡚-🡟🢈-🢏🢮🢯🢼-🢿🣂-🣏🣙-🣿🩘-🩟🩮🩯🩽-🩿🪋-🪍🫇🫉-🫌🫝🫞🫫-🫮🫹-🫿🮓🯻-🿽 𠀀-𯟿𯨞-𯿽"
                                    + " 𰀀-𿿽񀀀-񏿽񐀀-񟿽񠀀-񯿽񰀀-񿿽򀀀-򏿽򐀀-򟿽򠀀-򯿽򰀀-򿿽󀀀-󏿽󐀀-󟿽󠀀󠀂-󠀟󠂀-󠃿󠇰-󯿽]")
                    .closeOver(UnicodeSet.CASE_INSENSITIVE)
                    .freeze();

    static boolean isValidDomain(String domainName) {
        if (domainName == null || domainName.isEmpty()) {
            return false;
        }

        // Initialize the UTS#46 standard IDNA instance
        IDNA idna =
                IDNA.getUTS46Instance(
                        IDNA.NONTRANSITIONAL_TO_ASCII
                                | IDNA.NONTRANSITIONAL_TO_UNICODE
                                | IDNA.CHECK_BIDI
                                | IDNA.CHECK_CONTEXTJ
                                | IDNA.CHECK_CONTEXTO
                                | IDNA.USE_STD3_RULES);

        StringBuilder dest = new StringBuilder();
        IDNA.Info info = new IDNA.Info();

        try {
            // Perform name-to-ASCII conversion/validation
            idna.nameToASCII(domainName, dest, info);
        } catch (IllegalArgumentException e) {
            // Fails on severe structural issues
            return false;
        }

        // Return true only if no errors were encountered during validation
        return !info.hasErrors();
    }
}
