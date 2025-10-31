// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.text.BreakTransliteratorAccess;
import com.ibm.icu.text.Transliterator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BreakTransliteratorPerfTest {

    static final Transliterator TITLE = BreakTransliteratorAccess.newInstance();

    @Benchmark
    public String testShort() {
        return TITLE.transliterate("Cat");
    }

    @Benchmark
    public String testSentence() {
        return TITLE.transliterate("The Quick Brown Fox jumped over the Lazy Dog");
    }
}
