/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;
import com.ibm.icu.text.*;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A class for testing UnicodeSet performance.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public class UnicodeSetPerf extends PerfTest {

    String pattern;
    UnicodeSet testChars;
    UnicodeSetIterator it;
    UnicodeSet us;
    HashSet hs;

    public static void main(String[] args) throws Exception {
        new UnicodeSetPerf().run(args);
    }

    protected void setup(String[] args) {
        // We only take one argument, the pattern
        if (args.length != 1) {
            throw new RuntimeException("Please supply UnicodeSet pattern");
        }

        pattern = args[0];
        testChars = new UnicodeSet(pattern);
        it = new UnicodeSetIterator(testChars);
        us = new UnicodeSet();
        hs = new HashSet();
    }

    PerfTest.Function testUnicodeSetAdd() {
        return new PerfTest.Function() {
            public void call() {
                us.clear();
                it.reset();
                int n=0;
                while (it.nextRange()) {
                    for (int cp = it.codepoint; cp <= it.codepointEnd; ++cp) {
                        us.add(cp);
                        ++n;
                    }
                }
            }

            public long getOperationsPerIteration() {
                return testChars.size();
            }
        };
    }

    PerfTest.Function testHashSetAdd() {
        return new PerfTest.Function() {
            public void call() {
                hs.clear();
                it.reset();
                int n=0;
                while (it.nextRange()) {
                    for (int cp = it.codepoint; cp <= it.codepointEnd; ++cp) {
                        hs.add(new Integer(cp));
                        ++n;
                    }
                }
            }

            public long getOperationsPerIteration() {
                return testChars.size();
            }
        };
    }

    PerfTest.Function testUnicodeSetContains() {
        us.clear();
        us.set(testChars);
        
        return new PerfTest.Function() {
            public void call() {
                int temp = 0;
                for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                    if (us.contains(cp)) {
                        temp += cp;
                    }
                }
            }

            public long getOperationsPerIteration() {
                return 0x110000;
            }
        };
    }

    PerfTest.Function testHashSetContains() {
        hs.clear();
        it.reset();
        while (it.next()) {
            hs.add(new Integer(it.codepoint));
        }
        return new PerfTest.Function() {
            public void call() {
                int temp = 0;
                for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                    if (hs.contains(new Integer(cp))) {
                        temp += cp;
                    }
                }
            }

            public long getOperationsPerIteration() {
                return 0x110000;
            }
        };
    }

    PerfTest.Function testUnicodeSetIterate() {
        return new PerfTest.Function() {
            public void call() {
                int temp = 0;
                UnicodeSetIterator uit = new UnicodeSetIterator(testChars);
                while (uit.next()) {
                    temp += uit.codepoint;
                }
            }

            public long getOperationsPerIteration() {
                return testChars.size();
            }
        };
    }

    PerfTest.Function testHashSetIterate() {
        hs.clear();
        it.reset();
        while (it.next()) {
            hs.add(new Integer(it.codepoint));
        }
        return new PerfTest.Function() {
            public void call() {
                int temp = 0;
                Iterator it = hs.iterator();
                while (it.hasNext()) {
                    temp += ((Integer)it.next()).intValue();
                }
            }

            public long getOperationsPerIteration() {
                return testChars.size();
            }
        };
    }
}
