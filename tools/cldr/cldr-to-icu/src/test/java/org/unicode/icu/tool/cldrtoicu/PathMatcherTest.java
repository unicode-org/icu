// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.unicode.cldr.api.CldrPath.parseDistinguishingPath;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrPath;

@RunWith(JUnit4.class)
public class PathMatcherTest {
    @Test
    public void testMatcher() {
        CldrPath calEra = parseDistinguishingPath(
            "//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr/era[@type=\"0\"]");
        CldrPath chineseMon1 = monthInfo("chinese", "format", "abbreviated", 1);
        CldrPath chineseMon2 = monthInfo("chinese", "format", "abbreviated", 2);
        CldrPath genericMon1 = monthInfo("generic", "stand-alone", "narrow", 1);
        CldrPath genericMon2 = monthInfo("generic", "stand-alone", "narrow", 2);
        List<CldrPath> calPaths =
            Arrays.asList(calEra, chineseMon1, chineseMon2, genericMon1, genericMon2);

        PathMatcher anyCalendarPaths = PathMatcher.of("ldml/dates/calendars/calendar");
        assertThat(calPaths.stream().allMatch(anyCalendarPaths::matchesPrefixOf)).isTrue();
        assertThat(calPaths.stream().noneMatch(anyCalendarPaths::matches)).isTrue();
        assertThat(calPaths.stream().noneMatch(anyCalendarPaths::matchesSuffixOf)).isTrue();

        PathMatcher chineseCalendars =
            PathMatcher.of("ldml/dates/calendars/calendar[@type=\"chinese\"]");
        assertThat(calPaths.stream().filter(chineseCalendars::matchesPrefixOf))
            .containsExactly(chineseMon1, chineseMon2);

        PathMatcher anyMonth = PathMatcher.of("monthWidth[@type=*]/month[@type=*]");
        assertThat(calPaths.stream().filter(anyMonth::matchesSuffixOf))
            .containsExactly(chineseMon1, chineseMon2, genericMon1, genericMon2);

        PathMatcher narrowMonth = PathMatcher.of("monthWidth[@type=\"narrow\"]/month[@type=*]");
        assertThat(calPaths.stream().filter(narrowMonth::matchesSuffixOf))
            .containsExactly(genericMon1, genericMon2);
        assertThat(calPaths.stream().filter(narrowMonth::matches)).isEmpty();

        PathMatcher firstMonth = PathMatcher.of("month[@type=\"1\"]");
        assertThat(calPaths.stream().filter(firstMonth::matchesSuffixOf))
            .containsExactly(chineseMon1, genericMon1);

        PathMatcher fullMatch = PathMatcher.of("ldml/dates"
            + "/calendars/calendar[@type=\"generic\"]"
            + "/months/monthContext[@type=\"stand-alone\"]"
            + "/monthWidth[@type=\"narrow\"]"
            + "/month[@type=\"2\"]");
        assertThat(calPaths.stream().filter(fullMatch::matches)).containsExactly(genericMon2);
    }

    @Test
    public void testWildcardSegment() {
        PathMatcher wildcard = PathMatcher.of("ldml/dates"
            + "/calendars/calendar[@type=\"generic\"]"
            + "/*/*[@type=\"format\"]/*[@type=\"narrow\"]/*[@type=*]");

        assertThat(wildcard.matches(monthInfo("generic", "format", "narrow", 1))).isTrue();
        assertThat(wildcard.matches(monthInfo("generic", "format", "narrow", 9))).isTrue();
        assertThat(wildcard.matches(dayInfo("generic", "format", "narrow", "sun"))).isTrue();

        assertThat(wildcard.matches(monthInfo("chinese", "format", "narrow", 1))).isFalse();
        assertThat(wildcard.matches(monthInfo("generic", "stand-alone", "narrow", 1))).isFalse();
        assertThat(wildcard.matches(dayInfo("generic", "format", "wide", "mon"))).isFalse();
    }

    @Test
    public void testAnyOf_match() {
        PathMatcher narrowMonth =
            PathMatcher.of("ldml/dates/calendars/calendar[@type=*]/months"
                + "/monthContext[@type=\"format\"]/monthWidth[@type=\"narrow\"]/month[@type=*]");
        PathMatcher narrowDay =
            PathMatcher.of("ldml/dates/calendars/calendar[@type=*]/days"
                + "/dayContext[@type=\"format\"]/dayWidth[@type=\"narrow\"]/day[@type=*]");
        PathMatcher prefix = PathMatcher.anyOf(narrowMonth, narrowDay);

        assertThat(prefix.matches(monthInfo("gregorian", "format", "narrow", 1))).isTrue();
        assertThat(prefix.matches(dayInfo("buddhist", "format", "narrow", "sun"))).isTrue();

        assertThat(prefix.matches(monthInfo("hindu", "format", "wide", 1))).isFalse();
        assertThat(prefix.matches(dayInfo("hindu", "format", "wide", "mon"))).isFalse();
    }

    @Test
    public void testAnyOf_suffix() {
        PathMatcher monthSuffix = PathMatcher.of("monthWidth[@type=\"narrow\"]/month[@type=*]");
        PathMatcher daySuffix = PathMatcher.of("dayWidth[@type=\"narrow\"]/day[@type=*]");
        PathMatcher suffix = PathMatcher.anyOf(monthSuffix, daySuffix);

        assertThat(suffix.matchesSuffixOf(monthInfo("generic", "format", "narrow", 1))).isTrue();
        assertThat(suffix.matchesSuffixOf(dayInfo("generic", "format", "narrow", "sun"))).isTrue();

        assertThat(suffix.matchesSuffixOf(monthInfo("generic", "format", "wide", 1))).isFalse();
        assertThat(suffix.matchesSuffixOf(dayInfo("generic", "format", "wide", "mon"))).isFalse();
    }

    @Test
    public void testAnyOf_prefix() {
        PathMatcher monthPrefix =
            PathMatcher.of("ldml/dates/calendars/calendar[@type=\"gregorian\"]/months");
        PathMatcher dayPrefix =
            PathMatcher.of("ldml/dates/calendars/calendar[@type=\"buddhist\"]/days");
        PathMatcher prefix = PathMatcher.anyOf(monthPrefix, dayPrefix);

        assertThat(prefix.matchesPrefixOf(monthInfo("gregorian", "format", "narrow", 1))).isTrue();
        assertThat(prefix.matchesPrefixOf(dayInfo("buddhist", "format", "narrow", "sun"))).isTrue();

        assertThat(prefix.matchesPrefixOf(monthInfo("hindu", "format", "wide", 1))).isFalse();
        assertThat(prefix.matchesPrefixOf(dayInfo("hindu", "format", "wide", "mon"))).isFalse();
    }

    @Test
    public void testBadSpecifiers() {
        assertInvalidPathSpecification("");
        // Leading and trailing '/' are not permitted (they imply empty segments.
        assertInvalidPathSpecification("/foo/");
        assertInvalidPathSpecification("foo//bar");
        assertInvalidPathSpecification("foo/bad segment name");
        assertInvalidPathSpecification("foo/bar[type=*]");
        assertInvalidPathSpecification("foo/bar[@type=**]");
        assertInvalidPathSpecification("foo/bar[@type='double-quotes-only']");
    }

    private void assertInvalidPathSpecification(String spec) {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> PathMatcher.of(spec));
        assertThat(e).hasMessageThat().startsWith("invalid path specification");
        assertThat(e).hasMessageThat().contains(spec);
    }

    private static CldrPath monthInfo(String type, String context, String width, int number) {
        return CldrPath.parseDistinguishingPath(String.format(
            "//ldml/dates/calendars/calendar[@type=\"%s\"]"
                + "/months/monthContext[@type=\"%s\"]"
                + "/monthWidth[@type=\"%s\"]"
                + "/month[@type=\"%d\"]",
            type, context, width, number));
    }

    private static CldrPath dayInfo(String type, String context, String width, String id) {
        return CldrPath.parseDistinguishingPath(String.format(
            "//ldml/dates/calendars/calendar[@type=\"%s\"]"
                + "/days/dayContext[@type=\"%s\"]"
                + "/dayWidth[@type=\"%s\"]"
                + "/day[@type=\"%s\"]",
            type, context, width, id));
    }
}
