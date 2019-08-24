// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static org.unicode.icu.tool.cldrtoicu.testing.RbPathSubjectFactory.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RbPathTest {
    @Test
    public void testEmpty() {
        assertThat(RbPath.empty()).hasSegments();
        assertThat(RbPath.empty()).hasLength(0);
    }

    @Test
    public void testParseVsOf() {
        assertThat(RbPath.of("foo", "bar")).hasSegments("foo", "bar");
        assertThat(RbPath.of("foo/bar")).hasSegments("foo/bar");
        assertThat(RbPath.parse("foo/bar")).hasSegments("foo", "bar");
    }

    @Test
    public void testBadArgs() {
        assertBadPath("", "empty path string");
        assertBadPath("foo//bar", "empty path segment");
        assertBadPath("foo/<bar/baz", "mismatched quoting");
        assertBadPath("foo/\"bar", "mismatched quoting");
        assertBadPath("foo/\"bar\"baz\"", "invalid character");
        assertBadPath("foo/bar baz", "invalid character");
    }

    private static void assertBadPath(String path, String errorSnippet) {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> RbPath.parse(path));
        assertThat(e).hasMessageThat().contains(errorSnippet);
    }
}
