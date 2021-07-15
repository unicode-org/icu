// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class IndexerTest {
    @Test
    public void testSimple() {
        Indexer<String, Integer> indexer = Indexer.create();
        assertThat(indexer.apply("foo")).isEqualTo(0);
        assertThat(indexer.apply("bar")).isEqualTo(1);
        assertThat(indexer.apply("baz")).isEqualTo(2);
        assertThat(indexer.apply("foo")).isEqualTo(0);
    }

    @Test
    public void testWithTransform() {
        ImmutableList<String> words = ImmutableList.of("ONE", "TWO", "THREE");
        Indexer<String, String> indexer = Indexer.create(words::get);
        assertThat(indexer.apply("foo")).isEqualTo("ONE");
        assertThat(indexer.apply("bar")).isEqualTo("TWO");
        assertThat(indexer.apply("baz")).isEqualTo("THREE");
        assertThat(indexer.apply("foo")).isEqualTo("ONE");

    }

    @Test
    public void getValues() {
        Indexer<String, Integer> indexer = Indexer.create();
        indexer.apply("foo");
        indexer.apply("bar");
        indexer.apply("baz");
        indexer.apply("bar");
        assertThat(indexer.getValues()).containsExactly("foo", "bar", "baz").inOrder();
    }
}
