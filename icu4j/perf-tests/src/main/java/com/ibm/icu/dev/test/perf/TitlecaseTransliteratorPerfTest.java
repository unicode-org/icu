// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.text.Transliterator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class TitlecaseTransliteratorPerfTest {

    static final Transliterator TITLE = Transliterator.getInstance("Title");

    @Benchmark
    public String testShort() {
        return TITLE.transliterate("CAT");
    }

    @Benchmark
    public String testSentence() {
        return TITLE.transliterate("the quick brown fox jumped over the lazy dog");
    }
}
