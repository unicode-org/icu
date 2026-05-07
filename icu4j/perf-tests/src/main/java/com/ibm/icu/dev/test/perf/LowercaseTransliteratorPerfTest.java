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
public class LowercaseTransliteratorPerfTest {

    static final Transliterator LOWER = Transliterator.getInstance("Lower");

    @Benchmark
    public String testShort() {
        return LOWER.transliterate("Cat");
    }

    @Benchmark
    public String testSentence() {
        return LOWER.transliterate("The Quick Brown Fox Jumped Over The Lazy Dog");
    }
}
