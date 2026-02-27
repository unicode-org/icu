// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.text.LinkDetector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(JUnit4.class)
public class LinkDetectorTest extends CoreTestFmwk {

    /** Constructor */
    public LinkDetectorTest() {}

    // public methods -----------------------------------------------

    @Test
    public void TestInvalidDomain() {
        LinkDetector links = new LinkDetector("a test.x b");
        assertEquals("Should not linkify text.x", 0, links.results.size());
        // hm... PublicSuffix accepts .invalid as a valid TLD.
        links = new LinkDetector("a test.invalid b");
        assertEquals("Should not linkify text.invalid", 0, links.results.size());
        // Empty input
        links = new LinkDetector("");
        assertEquals("Empty input", 0, links.results.size());
    }

    @Test
    public void TestBlogspot() {
        LinkDetector links = new LinkDetector("a blogspot.com b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("result.input is the full detector input",
                     "a blogspot.com b", links.results.get(0).input);
        assertEquals("inputOffset", 2, links.results.get(0).inputOffset);
        assertEquals("inputLength", "blogspot.com".length(), links.results.get(0).inputLength);
        // https:// prefix appears in humanReadableOutput; inputLength covers the full scheme+host
        links = new LinkDetector("a https://blogspot.com b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "https://blogspot.com", links.results.get(0).humanReadableOutput);
        assertEquals("foo", "https", links.results.get(0).link.getProtocol());
        assertEquals("foo", "https://blogspot.com".length(), links.results.get(0).inputLength);
        // unrecognized scheme: hostname after // is not a trigger point; nothing detected
        links = new LinkDetector("a ftp://blogspot.com b");
        assertEquals("foo", 0, links.results.size());
        // two non-overlapping URLs in one string
        links = new LinkDetector("a blogspot.com b example.com c");
        assertEquals("foo", 2, links.results.size());
    }

    @Test
    public void TestSchemeHandling() {
        // Non-HTTP(S) scheme URIs are not linkified: the hostname sits immediately after //,
        // and the lookbehind in INITIAL_REGEX excludes / as a preceding character.
        String[] unrecognized = {
            "foo ftp://ftp.archaic.example.com bar",
            "foo ssh://server.example.com bar",
            "foo sftp://files.example.com bar",
            "foo ldap://directory.example.com bar",
        };
        for (String s : unrecognized) {
            LinkDetector links = new LinkDetector(s);
            assertEquals("unrecognized scheme in: " + s, 0, links.results.size());
        }
        // https:// humanReadableOutput includes the scheme
        LinkDetector links = new LinkDetector("foo https://example.com bar");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "https://example.com", links.results.get(0).humanReadableOutput);
        assertEquals("foo", "https", links.results.get(0).link.getProtocol());
        // http:// humanReadableOutput includes the scheme; protocol is http, not https
        links = new LinkDetector("foo http://example.com bar");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "http://example.com", links.results.get(0).humanReadableOutput);
        assertEquals("foo", "http", links.results.get(0).link.getProtocol());
    }

    @Test
    public void TestSchemeAfterNonLatin() {
        // Japanese (and other scripts that run flush against a following URL)
        // habitually precede an http:// with no space. The trigger fires at the
        // start of the unspaced run, but the link begins where the scheme does.
        LinkDetector links = new LinkDetector("テストhttp://example.com/日本語");
        assertEquals("one result", 1, links.results.size());
        LinkDetector.Result r = links.results.get(0);
        assertEquals("link starts at the scheme, past テスト", 3, r.inputOffset);
        assertEquals("humanReadable", "http://example.com/日本語", r.humanReadableOutput);
        assertEquals("protocol", "http", r.link.getProtocol());
        assertEquals("host", "example.com", r.link.getHost());
    }

    @Test
    public void TestUserinfoDeception() {
        // "https://www.bbc.co.uk@npmjs.com/something" is a classic phishing
        // shape: the real host is npmjs.com, www.bbc.co.uk is userinfo. UTS58
        // has no userinfo, so we link only https://www.bbc.co.uk and leave the
        // deceptive npmjs.com host unlinked.
        LinkDetector links =
            new LinkDetector("mumble  https://www.bbc.co.uk@npmjs.com/something stumble");
        assertEquals("exactly one link", 1, links.results.size());
        assertEquals("only the leading host is linked",
                     "https://www.bbc.co.uk", links.results.get(0).humanReadableOutput);
        assertEquals("host", "www.bbc.co.uk", links.results.get(0).link.getHost());
    }

    @Test
    public void TestEmbeddedUrl() {
        String url = "https://archive.org/20260225/https://example.com/example";
        LinkDetector links = new LinkDetector("foo " + url + " bar");
        // Exactly one result: the inner example.com must NOT be linkified as a second link.
        assertEquals("exactly one link", 1, links.results.size());
        assertEquals("full URL including embedded https://", url,
                     links.results.get(0).humanReadableOutput);
        assertEquals("protocol", "https", links.results.get(0).link.getProtocol());
        assertEquals("host", "archive.org", links.results.get(0).link.getHost());
    }

    @Test
    public void TestGetOpener() {
        // First pair: ) → (
        assertEquals("first: ) → (", 0x0028, LinkDetector.getOpener(0x0029));
        // Middle pairs (indices 31 and 32 of the 65-entry arrays):
        //   ⦍ (U+298D) ↔ ⦎ (U+298E), ⦏ (U+298F) ↔ ⦐ (U+2990)
        assertEquals("mid: ⦎ → ⦍", 0x298D, LinkDetector.getOpener(0x2990));
        assertEquals("mid: ⦐ → ⦏", 0x298F, LinkDetector.getOpener(0x298E));
        // Last pair: ｣ (U+FF63) → ｢ (U+FF62)
        assertEquals("last: ｣ → ｢", 0xFF62, LinkDetector.getOpener(0xFF63));
        // Non-closer returns -1
        assertEquals("non-closer", -1, LinkDetector.getOpener(0x0028));
    }

    @Test
    public void TestEmailDetection() {
        // Basic email: local part "test", domain validated
        LinkDetector links = new LinkDetector("foo test@example.com bar");
        assertEquals("one result", 1, links.results.size());
        String hro = links.results.get(0).humanReadableOutput;
        assertEquals("local part", "test", hro.substring(0, hro.indexOf('@')));
        assertEquals("humanReadable", "test@example.com", hro);
        assertEquals("mailto protocol", "mailto", links.results.get(0).link.getProtocol());
        // inputOffset/inputLength cover "test@example.com"
        assertEquals("inputOffset", 4, links.results.get(0).inputOffset);
        assertEquals("inputLength", "test@example.com".length(), links.results.get(0).inputLength);

        // Dot-separated local part
        links = new LinkDetector("foo user.name@example.com bar");
        assertEquals("dot local part: one result", 1, links.results.size());
        assertEquals("dot local part", "user.name@example.com",
                     links.results.get(0).humanReadableOutput);

        // Multiple dots in local part
        links = new LinkDetector("foo a.b.c@example.com bar");
        assertEquals("multi-dot local part: one result", 1, links.results.size());
        assertEquals("multi-dot local part", "a.b.c@example.com",
                     links.results.get(0).humanReadableOutput);

        // Trailing dot before '@': empty last atom, so not a valid local part.
        // And because UTS58 has no userinfo, the host after '@' is not a URL
        // either, so the whole thing is left unlinked (UTS58 conformance).
        links = new LinkDetector("foo name.@example.com bar");
        assertEquals("trailing dot: not linked", 0, links.results.size());

        // 'mailto:' prefix included in humanReadableOutput and inputOffset
        links = new LinkDetector("foo mailto:test@example.com bar");
        assertEquals("mailto: one result", 1, links.results.size());
        assertEquals("mailto: humanReadable", "mailto:test@example.com",
                     links.results.get(0).humanReadableOutput);
        assertEquals("mailto: inputOffset", 4, links.results.get(0).inputOffset);
        assertEquals("mailto: inputLength", "mailto:test@example.com".length(),
                     links.results.get(0).inputLength);
        assertEquals("mailto: protocol", "mailto", links.results.get(0).link.getProtocol());

        // Invalid domain: not linkified
        links = new LinkDetector("foo test@example.invalid bar");
        assertEquals("invalid domain not linkified", 0, links.results.size());

        // '@' with nothing before it: not linkified as email; .invalid also rejects URL
        links = new LinkDetector("foo @example.invalid bar");
        assertEquals("empty local part not linkified", 0, links.results.size());

        // URL and email in the same string; the email domain must not be
        // linkified as a separate URL result
        links = new LinkDetector("foo example.com test@example.net bar");
        assertEquals("two results", 2, links.results.size());
        assertEquals("URL first", "example.com", links.results.get(0).humanReadableOutput);
        assertEquals("email second", "test@example.net", links.results.get(1).humanReadableOutput);
        assertEquals("email protocol", "mailto", links.results.get(1).link.getProtocol());
    }

    @Test
    public void testDotCom() {
        LinkDetector links = new LinkDetector("a com b");
        assertEquals("foo", 0, links.results.size());
    }

    @Test
    public void TestPortHandling() {
        LinkDetector links = new LinkDetector("a example.com:443 b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", 443, links.results.get(0).link.getPort());
        assertEquals("foo", "example.com:443", links.results.get(0).humanReadableOutput);
        // port combined with path
        links = new LinkDetector("a example.com:8080/path b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", 8080, links.results.get(0).link.getPort());
        assertEquals("foo", "/path", links.results.get(0).link.getPath());
        // port 1: valid, linkified
        links = new LinkDetector("a example.com:1 b");
        assertEquals("port 1 linkified", 1, links.results.size());
        assertEquals("port 1 value", 1, links.results.get(0).link.getPort());
        // port 1000: valid, linkified
        links = new LinkDetector("a example.com:1000 b");
        assertEquals("port 1000 linkified", 1, links.results.size());
        assertEquals("port 1000 value", 1000, links.results.get(0).link.getPort());
        // port 0: reserved, not linkified at all
        links = new LinkDetector("a example.com:0 b");
        assertEquals("port 0 not linkified", 0, links.results.size());
        // port 100000: out of valid range, not linkified at all
        links = new LinkDetector("a example.com:100000 b");
        assertEquals("port 100000 not linkified", 0, links.results.size());
        // A very long digit run must not overflow Integer.parseInt: it is just
        // an out-of-range port, not a crash.
        links = new LinkDetector("a example.com:99999999999 b");
        assertEquals("overflowing port not linkified", 0, links.results.size());
    }

    @Test
    public void TestSimplePathHandling() {
        LinkDetector links = new LinkDetector("a example.com/123 b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "/123", links.results.get(0).link.getPath());
    }

    @Test
    public void TestQueryHandling() {
        LinkDetector links = new LinkDetector("a example.com?123 b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "123", links.results.get(0).link.getQuery());
    }

    @Test
    public void TestFragmentHandling() {
        LinkDetector links = new LinkDetector("a example.com#123 b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "123", links.results.get(0).link.getRef());
    }

    @Test
    public void TestLongExample() {
        LinkDetector links = new LinkDetector("a example.com/123?123#123 b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123?123#123", links.results.get(0).humanReadableOutput);
        assertEquals("foo", "/123", links.results.get(0).link.getPath());
        assertEquals("foo", "123", links.results.get(0).link.getQuery());
        assertEquals("foo", "123", links.results.get(0).link.getRef());
    }

    @Test
    public void TestTrailingParens() {
        LinkDetector links = new LinkDetector("a (example.com/123?123#123) b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123?123#123", links.results.get(0).humanReadableOutput);
        links = new LinkDetector("a (example.com/123?123#(123)) b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123?123#(123)",
                     links.results.get(0).humanReadableOutput);
        // Square brackets around a URL: the ] is an unmatched closer and terminates the URL
        links = new LinkDetector("a [example.com/path] b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/path", links.results.get(0).humanReadableOutput);
        // Surplus closer (no matching opener on the stack) terminates the URL
        links = new LinkDetector("a example.com/foo) b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/foo", links.results.get(0).humanReadableOutput);
        // Mismatched closer terminates at the point of mismatch
        links = new LinkDetector("a example.com/foo(bar] b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/foo(bar", links.results.get(0).humanReadableOutput);
    }

    @Test
    public void TestTrailingFullStops() {
        LinkDetector links = new LinkDetector("a example.com/123.html b");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123.html", links.results.get(0).humanReadableOutput);
        links = new LinkDetector("a example.com/123. HTML is great");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123", links.results.get(0).humanReadableOutput);
        links = new LinkDetector("a example.com/123... HTML is great");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123", links.results.get(0).humanReadableOutput);
        links = new LinkDetector("a example.com/123.");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", "example.com/123", links.results.get(0).humanReadableOutput);
    }

    @Test
    public void TestALabelDecoding() {
        String xn = "xn-----ctdbabcfhu9c2b9l1acccr4c.xn--mgbah1a3hjkrd";
        String u = "تجربة-القبول-الشامل.موريتانيا";
        LinkDetector links = new LinkDetector(xn);
        LinkDetector.Result r = links.considerLink(0);
        assertNotNull("Result should not be null", r);
        assertEquals("foo", u, r.link.getHost());
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", u, links.results.get(0).humanReadableOutput);
        assertEquals("foo", u, links.results.get(0).link.getHost());
    }

    @Test
    public void TestUASG004Domains() {
        String[] uasg004domains = {
            // 1, ASCII.ASCII, new-long, Long ASCII
            "universal-acceptance-test.international",
            // 2, ASCII.ASCII, new-short, Short ASCII
            "universal-acceptance-test.icu",
            // 3, IDN.IDN, RTL, Arabic
            "تجربة-القبول-الشامل.موريتانيا",
            // 4, IDN.IDN, , Armenian
            "համընդհանուր-ընկալում-թեստ.հայ",
            // 5, IDN.IDN, , Bengali Bangla
            "সর্বজনীন-স্বীকৃতির-পরীক্ষা.ভারত",
            // 6, IDN.IDN, , Cyrillic
            "универсальное-принятие-тест.москва",
            // 7, IDN.IDN, , Devanagari
            "सार्वभौमिक-स्वीकृति-परीक्षण.संगठन",
            // 8, IDN.IDN, , Georgian
            "უნივერსალური-თავსობადობის-ტესტი.გე",
            // 9, IDN.IDN, , Greek
            "καθολική-αποδοχή-δοκιμή.ευ",
            // 10, IDN.IDN, , Gujarati
            "સાર્વત્રિક-સ્વીકૃતિ-પરીક્ષણ.ભારત",
            // 11, IDN.IDN, , Gurmukhi
            "ਸਰਵਵਿਆਪਕ-ਪ੍ਰਵਾਨਗੀ-ਪਰਖ.ਭਾਰਤ",
            // 12, IDN.IDN, , Hangul
            "다국어도메인이용환경테스트.한국",
            // 13, IDN.IDN, RTL, Hebrew
            "מבחן-קבלה-אוניברסלי.קום",
            // 14, IDN.IDN, , Hiragana
            "どこでもつかえる.みんな",
            // 15, IDN.IDN, , Kannada
            "ಸಾರ್ವತ್ರಿಕ-ಸ್ವೀಕಾರಾರ್ಹತೆ-ಪರೀಕ್ಷೆ.ಭಾರತ",
            // 16, IDN.IDN, , Katakana
            "ユニバーサルアクセプタンス.クラウド",
            // 17, IDN.IDN, , Lao
            "ສາກົນ-ການຍອມຮັບ-ທົດລອງ.ລາວ",
            // 19, IDN.IDN, , Malayalam
            "സാർവത്രിക-സ്വീകാര്യതാ-പരിശോധന.ഭാരതം",
            // 20, IDN.IDN, , Oriya
            "ଯୁନିଭରସାଲ-ଏକସେପ୍ଟନ୍ସ-ଟେଷ୍ଟ.ଭାରତ",
            // 21, IDN.IDN, , Sinhala
            "විශ්ව-සම්මුති-පිරික්සුම.ලංකා",
            // 22, IDN.IDN, , Tamil
            "பொது-ஏற்பு-சோதனை.சிங்கப்பூர்",
            // 23, IDN.IDN, , Telugu
            "యూనివర్సల్-ఆమోదం-పరీక్ష.భారత్",
            // 24, IDN.IDN, , Thai
            "ยูเอทดสอบ.ไทย",
            // 25, IDN.IDN, , Simplified Chinese
            "普遍适用测试.我爱你",
            // 26, IDN.IDN, , Traditional Chinese
            "普遍適用測試.台灣",
            // 27, IDN.ASCII, , Ethiopic
            "ሁለንአቀፍ-ተቀባይነት-ሙከራ.com",
            // 28, IDN.ASCII, , Khmer
            "ការសាកល្បងទទួលយកជាអន្តរជាតិ.com",
            // 29, IDN.ASCII, , Myanmar
            "အလုံးစုံလက်ခံမှုစမ်းသပ်ချက်.com",
            // 30, IDN.ASCII, RTL, Thaana
            "ދުނިޔެ-ގަބޫލުކުރާ-ޓެސްޓު.com",
            // 63, ASCII.IDN, RTL, Hebrew
            "universal-acceptance-test.קום",
            // 64, IDN.ASCII, , Latin
            "épreuve-acceptation-universelle.org"
        };
        for(String domain : uasg004domains) {
            LinkDetector links =
                new LinkDetector("Lorem ipsum " + domain + " dolor sit amet");
            assertEquals("foo", 1, links.results.size());
            assertEquals("foo", domain, links.results.get(0).humanReadableOutput);
            assertEquals("foo", domain, links.results.get(0).link.getHost());
        }
    }

    @Test
    public void TestTibetanDomainWithTseg() {
        // Tibetan uses tseg (U+0F0B) which is mishandled quite often
        final String domain = "ཡོངས་ཁྱབ་ངོས་ལེན་བརྟག་དཔྱད.com";
        LinkDetector links =
            new LinkDetector("Lorem ipsum " + domain + " dolor sit amet");
        assertEquals("foo", 1, links.results.size());
        assertEquals("foo", domain, links.results.get(0).humanReadableOutput);
        assertEquals("foo", domain, links.results.get(0).link.getHost());
    }

    @Test
    public void TestUASG004Miscellany() {
        String[] uasg004domains = {
            // 18, IDN.IDN, , Latin
            "universales-akzeptanz-test.vermögensberatung",
            // 65, IDN.ASCII, not in NFC normalization form, Latin (UTS#46 output is NFC)
            "épreuve-acceptation-universelle.org",
            // 66, IDN.IDN, Ideographic Full Stop, Simplified Chinese (UTS#46 normalises 。 to .)
            "普遍适用测试.我爱你",
            // 67, IDN.IDN, RTL; A-label.U-label, Arabic
            //
            // 68, IDN.IDN, RTL; U-label.A-label, Arabic (UTS#46 converts a-label to u-label)
            "تجربة-القبول-الشامل.موريتانيا",
            // 69, IDN.IDN, RTL; A-label.A-label, Arabic (UTS#46 converts both to u-labels)
            "تجربة-القبول-الشامل.موريتانيا",
            // 70, ASCII.ASCII/Unicode, , Simplified Chinese
            "universal-acceptance-test.icu/测试",
            // 71, IDN.IDN/Unicode, , Simplified Chinese
            "普遍适用测试.我爱你/测试",
            // 72, IDN.IDN/Unicode, RTL, Arabic
            "تجربة-القبول-الشامل.موريتانيا/تجربة"
        };
        for(String domain : uasg004domains) {
            LinkDetector links =
                new LinkDetector("Lorem ipsum " + domain + " dolor sit amet");
            assertEquals("foo", 1, links.results.size());
            assertEquals("foo", domain, links.results.get(0).humanReadableOutput);
            // link.getHost() returns only the host, not path; split at first '/'
            String expectedHost = domain.split("/", 2)[0];
            assertEquals("foo", expectedHost, links.results.get(0).link.getHost());
        }
    }

    @Test
    public void testComplicatedRealStrings() {
        String[] complicated = {
            "en.wikipedia.org/wiki/The_Lovemakers_(film)",
            "example.com/?foo[1]=a&amp;foo[2]=b",
            "comoyo.com/play/S(123)",
            "example.com/knutsen_ludvigsen/ver(k)ste(d)/brilleslange.mp3",
            "example.com/Bob_Marley/Rastaman_Vibration/11_Jah_Live_(originally_issued_as_Island_Single_(WIP_6265))_(bonus_track)",
            "business.timesonline.co.uk/article/0,,9065-2473189,00.html",
            "www.mail-archive.com/ruby-talk@ruby-lang.org/",
            "tools.ietf.org/html/rfc3986",
            "www.amazon.com/Testing-Equal-Sign-In-Path/ref=pd_bbs_sr_1?ie=UTF8&s=books&qid=1198861734&sr=8-1",
            "www.google.com/doku.php?id=gps:resource:scs:start",
            "maps.google.co.uk/maps?f=q&q=the+london+eye&ie=UTF8&ll=51.503373,-0.11939&spn=0.007052,0.012767&z=16&iwloc=A",
            "www.rubyonrails.com/foo.cgi?trailing_hyphen=value-",
            "www.rubyonrails.com/foo.cgi?trailing_forward_slash=value/"
        };
        for(String s : complicated) {
            LinkDetector links =
                new LinkDetector("Lorem ipsum " + s + " dolor sit amet");
            assertEquals("foo", 1, links.results.size());
            assertEquals("foo", s, links.results.get(0).humanReadableOutput);
            // link.getHost() returns only the host, not path/query/fragment
            String expectedHost = s.split("[/?#]", 2)[0];
            assertEquals("foo", expectedHost, links.results.get(0).link.getHost());
        }
    }

    @Test
    public void testOverlongDOS() {
        StringBuilder sb = new StringBuilder();
        sb.append("Lorem ipsum ");
        int i = 0;
        while(i++ < 10000)
            sb.append("example.");
        sb.append(".com dolor sit amet");
        LinkDetector links = new LinkDetector(sb.toString());
        assertEquals("foo", 0, links.results.size());
    }

    @Test
    public void TestOverlongHost() {
        // A single label longer than the 253-character DNS limit is not a host.
        StringBuilder sb = new StringBuilder("a ");
        for (int i = 0; i < 300; i++)
            sb.append('x');
        sb.append(".com b");
        LinkDetector links = new LinkDetector(sb.toString());
        assertEquals("overlong host not linkified", 0, links.results.size());
        // A host right at the limit (253 chars including ".com") is still found.
        sb = new StringBuilder("a ");
        for (int i = 0; i < 249; i++) // 249 + ".com" == 253
            sb.append('x');
        sb.append(".com b");
        links = new LinkDetector(sb.toString());
        assertEquals("253-char host linkified", 1, links.results.size());
    }

    // The UTS58 link-detection markers wrapped around each expected link.
    private static final String OPEN = "⸠";  // ⸠
    private static final String CLOSE = "⸡"; // ⸡

    // Detect links in input and wrap each in the markers. results is already
    // sorted by offset and non-overlapping, so a single pass rebuilds the line.
    private static String detectAndMark(String input) {
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (LinkDetector.Result e : new LinkDetector(input).results) {
            int start = e.inputOffset;
            int end = e.inputOffset + e.inputLength;
            out.append(input, cursor, start)
               .append(OPEN).append(input, start, end).append(CLOSE);
            cursor = end;
        }
        return out.append(input, cursor, input.length()).toString();
    }

    @Test
    public void TestUTS58Conformance() throws IOException {
        // The official UTS58 conformance suite. Each non-comment line carries
        // zero or more links wrapped in ⸠…⸡; we strip the markers, re-detect,
        // re-wrap, and require the result to match the line exactly.
        //
        // These two lines are NOT bugs against the normative UTS58 text: UTS58
        // declares userinfo out of scope, so nothing requires us to find a link
        // in "http://john.smith@example.com". The conformance file still marks
        // them, so we list them as expected failures (matching the ruby/js ports).
        Set<String> known = new HashSet<>(Arrays.asList(
            "http://john.smith@example.com",
            "http://john.smith@example.com/foo/bar"));

        List<String> regressed = new ArrayList<>();
        List<String> fixed = new ArrayList<>();
        try (BufferedReader in =
                 TestUtil.getUtf8DataReader("unicode/LinkDetectionTest.txt")) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String input = line.replace(OPEN, "").replace(CLOSE, "");
                String got = detectAndMark(input);
                if (got.equals(line)) {
                    if (known.contains(input))
                        fixed.add(input);
                } else if (!known.contains(input)) {
                    regressed.add("\n  in : " + input +
                                  "\n  exp: " + line +
                                  "\n  got: " + got);
                }
            }
        }
        assertEquals("newly failing conformance lines:" + regressed,
                     0, regressed.size());
        assertEquals("known failures that now pass (remove from the list):" + fixed,
                     0, fixed.size());
    }
}
