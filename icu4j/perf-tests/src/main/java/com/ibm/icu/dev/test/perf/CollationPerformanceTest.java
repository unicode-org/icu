// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2002-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.perf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;

public class CollationPerformanceTest {
    static final String usageString = 
        "usage:  collperf options...\n"
        + "-help                      Display this message.\n"
        + "-file file_name            utf-16 format file of names.\n"
        + "-locale name               ICU locale to use.  Default is en_US\n"
        + "-rules file_name           Collation rules file (overrides locale)\n"
        //+ "-langid 0x1234             Windows Language ID number.  Default to value for -locale option\n"
        //+ "                              see http://msdn.microsoft.com/library/psdk/winbase/nls_8xo3.htm\n"
        //+ "-win                       Run test using Windows native services.  (ICU is default)\n"
        //+ "-unix                      Run test using Unix strxfrm, strcoll services.\n"
        //+ "-uselen                    Use API with string lengths.  Default is null-terminated strings\n"
        + "-usekeys                   Run tests using sortkeys rather than strcoll\n"
        + "-strcmp                    Run tests using u_strcmp rather than strcoll\n"
        + "-strcmpCPO                 Run tests using u_strcmpCodePointOrder rather than strcoll\n"
        + "-loop nnnn                 Loopcount for test.  Adjust for reasonable total running time.\n"
        + "-iloop n                   Inner Loop Count.  Default = 1.  Number of calls to function\n"
        + "                               under test at each call point.  For measuring test overhead.\n"
        + "-terse                     Terse numbers-only output.  Intended for use by scripts.\n"
        + "-french                    French accent ordering\n"
        + "-frenchoff                 No French accent ordering (for use with French locales.)\n"
        + "-norm                      Normalizing mode on\n"
        + "-shifted                   Shifted mode\n"
        + "-lower                     Lower case first\n"
        + "-upper                     Upper case first\n"
        + "-case                      Enable separate case level\n"
        + "-level n                   Sort level, 1 to 5, for Primary, Secndary, Tertiary, Quaternary, Identical\n"
        + "-keyhist                   Produce a table sort key size vs. string length\n"
        + "-binsearch                 Binary Search timing test\n"
        + "-keygen                    Sort Key Generation timing test\n"
        + "-qsort                     Quicksort timing test\n"
        + "-iter                      Iteration Performance Test\n"
        + "-dump                      Display strings, sort keys and CEs.\n"
        + "-java                      Run test using java.text.Collator.\n";
    
    //enum {FLAG, NUM, STRING} type;
    static StringBuffer temp_opt_fName      = new StringBuffer("");
    static StringBuffer temp_opt_locale     = new StringBuffer("en_US");
    //static StringBuffer temp_opt_langid     = new StringBuffer("0");         // Defaults to value corresponding to opt_locale.
    static StringBuffer temp_opt_rules      = new StringBuffer("");
    static StringBuffer temp_opt_help       = new StringBuffer("");
    static StringBuffer temp_opt_loopCount  = new StringBuffer("1");
    static StringBuffer temp_opt_iLoopCount = new StringBuffer("1");
    static StringBuffer temp_opt_terse      = new StringBuffer("false");
    static StringBuffer temp_opt_qsort      = new StringBuffer("");
    static StringBuffer temp_opt_binsearch  = new StringBuffer("");
    static StringBuffer temp_opt_icu        = new StringBuffer("true");
    //static StringBuffer opt_win        = new StringBuffer("");      // Run with Windows native functions.
    //static StringBuffer opt_unix       = new StringBuffer("");      // Run with UNIX strcoll, strxfrm functions.
    //static StringBuffer opt_uselen     = new StringBuffer("");
    static StringBuffer temp_opt_usekeys    = new StringBuffer("");
    static StringBuffer temp_opt_strcmp     = new StringBuffer("");
    static StringBuffer temp_opt_strcmpCPO  = new StringBuffer("");
    static StringBuffer temp_opt_norm       = new StringBuffer("");
    static StringBuffer temp_opt_keygen     = new StringBuffer("");
    static StringBuffer temp_opt_french     = new StringBuffer("");
    static StringBuffer temp_opt_frenchoff  = new StringBuffer("");
    static StringBuffer temp_opt_shifted    = new StringBuffer("");
    static StringBuffer temp_opt_lower      = new StringBuffer("");
    static StringBuffer temp_opt_upper      = new StringBuffer("");
    static StringBuffer temp_opt_case       = new StringBuffer("");
    static StringBuffer temp_opt_level      = new StringBuffer("0");
    static StringBuffer temp_opt_keyhist    = new StringBuffer("");
    static StringBuffer temp_opt_itertest   = new StringBuffer("");
    static StringBuffer temp_opt_dump       = new StringBuffer("");
    static StringBuffer temp_opt_java       = new StringBuffer("");
    
    
    static String   opt_fName      = "";
    static String   opt_locale     = "en_US";
    //static int      opt_langid     = 0;         // Defaults to value corresponding to opt_locale.
    static String   opt_rules      = "";
    static boolean  opt_help       = false;
    static int      opt_loopCount  = 1;
    static int      opt_iLoopCount = 1;
    static boolean  opt_terse      = false;
    static boolean  opt_qsort      = false;
    static boolean  opt_binsearch  = false;
    static boolean  opt_icu        = true;
    //static boolean  opt_win        = false;      // Run with Windows native functions.
    //static boolean  opt_unix       = false;      // Run with UNIX strcoll, strxfrm functions.
    //static boolean  opt_uselen     = false;
    static boolean  opt_usekeys    = false;
    static boolean  opt_strcmp     = false;
    static boolean  opt_strcmpCPO  = false;
    static boolean  opt_norm       = false;
    static boolean  opt_keygen     = false;
    static boolean  opt_french     = false;
    static boolean  opt_frenchoff  = false;
    static boolean  opt_shifted    = false;
    static boolean  opt_lower      = false;
    static boolean  opt_upper      = false;
    static boolean  opt_case       = false;
    static int      opt_level      = 0;
    static boolean  opt_keyhist    = false;
    static boolean  opt_itertest   = false;
    static boolean  opt_dump       = false;
    static boolean  opt_java       = false;

    static OptionSpec[] options = {
        new OptionSpec("-file", 2, temp_opt_fName),
        new OptionSpec("-locale", 2, temp_opt_locale),
        //new OptionSpec("-langid", 1, temp_opt_langid),
        new OptionSpec("-rules", 2, temp_opt_rules),
        new OptionSpec("-qsort", 0, temp_opt_qsort),
        new OptionSpec("-binsearch", 0, temp_opt_binsearch),
        new OptionSpec("-iter", 0, temp_opt_itertest),
        //new OptionSpec("-win", 0, temp_opt_win),
        //new OptionSpec("-unix", 0, temp_opt_unix),
        //new OptionSpec("-uselen", 0, temp_opt_uselen),
        new OptionSpec("-usekeys", 0, temp_opt_usekeys),
        new OptionSpec("-strcmp", 0, temp_opt_strcmp),
        new OptionSpec("-strcmpCPO", 0, temp_opt_strcmpCPO),
        new OptionSpec("-norm", 0, temp_opt_norm),
        new OptionSpec("-french", 0, temp_opt_french),
        new OptionSpec("-frenchoff", 0, temp_opt_frenchoff),
        new OptionSpec("-shifted", 0, temp_opt_shifted),
        new OptionSpec("-lower", 0, temp_opt_lower),
        new OptionSpec("-upper", 0, temp_opt_upper),
        new OptionSpec("-case", 0, temp_opt_case),
        new OptionSpec("-level", 1, temp_opt_level),
        new OptionSpec("-keyhist", 0, temp_opt_keyhist),
        new OptionSpec("-keygen", 0, temp_opt_keygen),
        new OptionSpec("-loop", 1, temp_opt_loopCount),
        new OptionSpec("-iloop", 1, temp_opt_iLoopCount),
        new OptionSpec("-terse", 0, temp_opt_terse),
        new OptionSpec("-dump", 0, temp_opt_dump),
        new OptionSpec("-help", 0, temp_opt_help),
        new OptionSpec("-?", 0, temp_opt_help),
        new OptionSpec("-java", 0, temp_opt_java),
    };
    
    static java.text.Collator javaCol = null;
    static com.ibm.icu.text.Collator icuCol = null;
    static NumberFormat nf = null;
    static NumberFormat percent = null;
    ArrayList list = null;
    String[] tests = null;
    int globalCount = 0;
    
    public static void main(String[] args) {
        CollationPerformanceTest collPerf = new CollationPerformanceTest();
        if ( !CollationPerformanceTest.processOptions(args) || opt_help || opt_fName.length()==0) {
            System.out.println(usageString);
            System.exit(1);
        }
        
        nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        percent = NumberFormat.getPercentInstance();
        
        collPerf.setOptions();
        collPerf.readDataLines();
        
        if (opt_dump) {
            collPerf.doDump();
        }
        
        if (opt_qsort) {
            collPerf.doQSort();
        }
        
        if (opt_binsearch) {
            collPerf.doBinarySearch();
        }
        
        if (opt_keygen) {
            collPerf.doKeyGen();
        }
        
        if (opt_keyhist) {
            collPerf.doKeyHist();
        }
        
        if (opt_itertest) {
            collPerf.doIterTest();
        }
        
    }
    
    //Dump file lines, CEs, Sort Keys if requested
    void doDump() {
        for(int i = 0; i < list.size(); i++) {
            //print the line
            String line = com.ibm.icu.impl.Utility.escape((String)list.get(i));
            System.out.println(line);
            
            System.out.print("  CEs:  ");
            CollationElementIterator CEiter = ((com.ibm.icu.text.RuleBasedCollator)icuCol).getCollationElementIterator(line);
            int ce;
            int j = 0;
            for(;;) {
                ce = CEiter.next();
                if (ce == CollationElementIterator.NULLORDER) {
                    break;
                }
                //System.out.print();
                String outStr = Integer.toHexString(ce); 
                for (int len = 0; len < 8 - outStr.length(); len++) {
                    outStr ='0' + outStr;
                }
                System.out.print(outStr + "  ");
                if(++j >8) {
                    System.out.print("\n        ");
                    j = 0;
                }
            }
                
            System.out.print("\n   ICU Sort Key: ");
            CollationKey ck = ((com.ibm.icu.text.RuleBasedCollator)icuCol).getCollationKey(line);
            byte[] cks = ck.toByteArray();
            j = 0;
            for(int k = 0; k < cks.length; k++) {
                String outStr = Integer.toHexString(cks[k]);
                switch (outStr.length()) {
                case 1:     outStr = '0' + outStr;
                            break;
                case 8:     outStr = outStr.substring(6);
                            break; 
                }
                System.out.print(outStr);
                System.out.print("  ");
                j++;
                if(j > 0 && j % 20 == 0) {
                    System.out.print("\n                 ");
                }
            }
            System.out.println("\n");
        }
    }
    
    /**---------------------------------------------------------------------------------------
     *
     *   doQSort()    The quick sort timing test.
     *
     *---------------------------------------------------------------------------------------
     */
    void doQSort() {
        callGC();
        //String[] sortTests = (String[]) tests.clone();
        //Adjust loop count to compensate for file size. QSort should be nlog(n) 
        double dLoopCount = opt_loopCount * 3000 / ((Math.log(tests.length) / Math.log(10)* tests.length));
 
        if(opt_usekeys) {
            dLoopCount *= 5;
        }
        
        int adj_loopCount = (int)dLoopCount;
        if(adj_loopCount < 1) {
            adj_loopCount = 1;
        }
        
        globalCount = 0;
        long startTime = 0;
        long endTime = 0;
        if (opt_icu && opt_usekeys) {
            startTime = System.currentTimeMillis();
            qSortImpl_icu_usekeys(tests, 0, tests.length -1, icuCol);
            endTime = System.currentTimeMillis();
        }
        if (opt_icu && !opt_usekeys){
            startTime = System.currentTimeMillis();
            qSortImpl_nokeys(tests, 0, tests.length -1, icuCol);
            endTime = System.currentTimeMillis();
        }
        if (opt_java && opt_usekeys) {
            startTime = System.currentTimeMillis();
            qSortImpl_java_usekeys(tests, 0, tests.length -1, javaCol);
            endTime = System.currentTimeMillis();
        }
        if (opt_java && !opt_usekeys){
            startTime = System.currentTimeMillis();
            qSortImpl_nokeys(tests, 0, tests.length -1, javaCol);
            endTime = System.currentTimeMillis();
        }
        long elapsedTime = endTime - startTime;
        int ns = (int)(1000000 * elapsedTime / (globalCount + 0.0));
        if (!opt_terse) {
            System.out.println("qsort:  total # of string compares = " + globalCount);
            System.out.println("qsort:  time per compare = " + ns);
        } else {
            System.out.println(ns);
        }
    }
    
    /**---------------------------------------------------------------------------------------
     *
     *    doBinarySearch()    Binary Search timing test.  Each name from the list
     *                        is looked up in the full sorted list of names.
     *
     *---------------------------------------------------------------------------------------
     */
    void doBinarySearch() {
        callGC();
        int gCount = 0;
        int loops = 0;
        double dLoopCount = opt_loopCount * 3000 / (Math.log(tests.length) / Math.log(10)* tests.length);
        long startTime = 0;
        long elapsedTime = 0;
        
        if(opt_usekeys) {
            dLoopCount *= 5;
        }
        int adj_loopCount = (int)dLoopCount;
        if(adj_loopCount < 1) {
            adj_loopCount = 1;
        }
        
        //int opt2 = 0;
        
        for(;;) {   //not really a loop, just allows "break" to work, to simplify 
                    //inadvertently running more than one test through here
            if(opt_strcmp) {
                int r = 0;
                startTime = System.currentTimeMillis();
                for(loops = 0; loops < adj_loopCount; loops++) {
                    for (int j = 0; j < tests.length; j++) {
                        int hi = tests.length-1;
                        int lo = 0;
                        int guess = -1;
                        for(;;) {
                            int newGuess = (hi + lo) / 2;
                            if(newGuess == guess){
                                break;
                            }
                            guess = newGuess;
                            r = tests[j].compareTo(tests[guess]);
                            gCount++;
                            if(r == 0) {
                                break;
                            }
                            if (r < 0) {
                                hi = guess;
                            } else {
                                lo = guess;
                            }
                        }
                    }
                }
                elapsedTime = System.currentTimeMillis() - startTime;
                break;
            }
            
            if (opt_strcmpCPO) {
                int r = 0;
                startTime = System.currentTimeMillis();
                for(loops = 0; loops < adj_loopCount; loops++) {
                    for (int j = 0; j < tests.length; j++) {
                        int hi = tests.length-1;
                        int lo = 0;
                        int guess = -1;
                        for(;;) {
                            int newGuess = (hi + lo) / 2;
                            if(newGuess == guess){
                                break;
                            }
                            guess = newGuess;
                            r = com.ibm.icu.text.Normalizer.compare(tests[j], tests[guess], Normalizer.COMPARE_CODE_POINT_ORDER);
                            gCount++;
                            if(r == 0) {
                                break;
                            }
                            if (r < 0) {
                                hi = guess;
                            } else {
                                lo = guess;
                            }
                        }
                    }
                }
                elapsedTime = System.currentTimeMillis() - startTime;
                break;
            }
            
            if (opt_icu) {
               
                int r = 0;
                startTime = System.currentTimeMillis();
                for (loops = 0; loops < adj_loopCount; loops++) {
                    for (int j = 0; j < tests.length; j++) {
                        int hi = tests.length - 1;
                        int lo = 0;
                        int guess = -1;
                        for (;;) {
                            int newGuess = (hi + lo) / 2;
                            if (newGuess == guess) {
                                break;
                            }
                            guess = newGuess;
                            if (opt_usekeys) {
                                com.ibm.icu.text.CollationKey sortKey1 = icuCol.getCollationKey(tests[j]);
                                com.ibm.icu.text.CollationKey sortKey2 = icuCol.getCollationKey(tests[guess]);
                                r = sortKey1.compareTo(sortKey2);
                                gCount ++;
                            } else {
                                r = icuCol.compare(tests[j], tests[guess]);
                                gCount++;
                            }
                            if (r == 0) {
                                break;
                            }
                            if (r < 0) {
                                hi = guess;
                            } else {
                                lo = guess;
                            }
                        }
                    }
                }
                elapsedTime = System.currentTimeMillis() - startTime;
                break;
            }
            if (opt_java) {
               
                int r = 0;
                startTime = System.currentTimeMillis();
                for (loops = 0; loops < adj_loopCount; loops++) {
                    for (int j = 0; j < tests.length; j++) {
                        int hi = tests.length - 1;
                        int lo = 0;
                        int guess = -1;
                        for (;;) {
                            int newGuess = (hi + lo) / 2;
                            if (newGuess == guess) {
                                break;
                            }
                            guess = newGuess;
                            if (opt_usekeys) {
                                java.text.CollationKey sortKey1 = javaCol.getCollationKey(tests[j]);
                                java.text.CollationKey sortKey2 = javaCol.getCollationKey(tests[guess]);
                                r = sortKey1.compareTo(sortKey2);
                                gCount ++;
                            } else {
                                r = javaCol.compare(tests[j], tests[guess]);
                                gCount++;
                            }
                            if (r == 0) {
                                break;
                            }
                            if (r < 0) {
                                hi = guess;
                            } else {
                                lo = guess;
                            }
                        }
                    }
                }
                elapsedTime = System.currentTimeMillis() - startTime;
                break;
            }
            break; 
        }
        int ns = (int)((float)(1000000) * (float)elapsedTime / (float)gCount);
        if (!opt_terse) {
            System.out.println("binary search:  total # of string compares = " + gCount);
            System.out.println("binary search:  compares per loop = " + gCount / loops);
            System.out.println("binary search:  time per compare = " + ns);
        } else {
            System.out.println(ns);
        }
    }
    
    /**---------------------------------------------------------------------------------------
     *
     *   doKeyGen()     Key Generation Timing Test
     *
     *---------------------------------------------------------------------------------------
     */
    void doKeyGen() {
        callGC();
        
        // Adjust loop count to compensate for file size.   Should be order n
        double dLoopCount = opt_loopCount * (1000.0 /  (double)list.size());
        int adj_loopCount = (int)dLoopCount;
        if (adj_loopCount < 1) adj_loopCount = 1;

        long startTime = 0;
        long totalKeyLen = 0;
        long totalChars = 0;
        if (opt_java) {
            startTime = System.currentTimeMillis();
            for (int loops=0; loops<adj_loopCount; loops++) {
                for (int line=0; line < tests.length; line++) {
                    for (int iLoop=0; iLoop < opt_iLoopCount; iLoop++) {
                        totalChars += tests[line].length();
                        byte[] sortKey = javaCol.getCollationKey(tests[line]).toByteArray();
                        totalKeyLen += sortKey.length;
                    }
                }
            }
        } else {
            startTime = System.currentTimeMillis();
            for (int loops=0; loops<adj_loopCount; loops++) {
                for (int line=0; line < tests.length; line++) {
                    for (int iLoop=0; iLoop < opt_iLoopCount; iLoop++) {
                        totalChars += tests[line].length();
                        byte[] sortKey = icuCol.getCollationKey(tests[line]).toByteArray();
                        totalKeyLen += sortKey.length;
                    }
                }
            }
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        long ns = (long)(1000000 * elapsedTime / (adj_loopCount * tests.length + 0.0));
        if (!opt_terse) {
            System.out.println("Sort Key Generation:  total # of keys =" + adj_loopCount * tests.length);
            System.out.println("Sort Key Generation:  time per key = " + ns + " ns");
            System.out.println("Key Length / character = " + nf.format(totalKeyLen / (totalChars + 0.0)));
        }
        else {
            System.out.print(ns + ",  ");
            System.out.println(nf.format(totalKeyLen / (totalChars + 0.0)) + ", ");
        }
    }
    
    /**---------------------------------------------------------------------------------------
     *
     *    doKeyHist()       Output a table of data for average sort key size vs. string length.
     *
     *---------------------------------------------------------------------------------------
     */
    void doKeyHist() {
        callGC();
        int     maxLen = 0;

        // Find the maximum string length
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].length() > maxLen) maxLen = tests[i].length();
        }
        
        int[] accumulatedLen  = new int[maxLen + 1];
        int[] numKeysOfSize   = new int[maxLen + 1];
        
        // Fill the arrays...
        for (int i = 0; i < tests.length; i++) {
            int len = tests[i].length();
            accumulatedLen[len] += icuCol.getCollationKey(tests[i]).toByteArray().length;
            numKeysOfSize[len]  += 1;
        }
        
        // And write out averages
        System.out.println("String Length,  Avg Key Length,  Avg Key Len per char");
        for (int i = 1; i <= maxLen; i++) {
            if (numKeysOfSize[i] > 0) {
                System.out.println(i + ", " + nf.format(accumulatedLen[i] / (numKeysOfSize[i]+ 0.0)) + ", " 
                    + nf.format(accumulatedLen[i] / (numKeysOfSize[i] * i + 0.0)));
            }
        }
        
    }
    
    void doForwardIterTest() {
        callGC();
        System.out.print("\n\nPerforming forward iteration performance test with ");
        System.out.println("performance test on strings from file -----------");
    
        CollationElementIterator iter = ((RuleBasedCollator)icuCol).getCollationElementIterator("");
        
        int gCount = 0;
        int count = 0;
        long startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int linecount = 0;
            while (linecount < tests.length) {
                String str = tests[linecount];
                iter.setText(str);
                while (iter.next() != CollationElementIterator.NULLORDER) {
                    gCount++;
                }
                linecount ++;
            }
            count ++;
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("elapsedTime " + elapsedTime + " ms");
        
        // empty loop recalculation
        count = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) { 
            int linecount = 0;
            while (linecount < tests.length) {
                String str = tests[linecount];
                iter.setText(str);
                linecount ++;
            }
            count ++;
        }
        elapsedTime -= (System.currentTimeMillis() - startTime);
        System.out.println("elapsedTime " + elapsedTime + " ms");

        int ns = (int)(1000000 * elapsedTime / (gCount + 0.0));
        System.out.println("Total number of strings compared " + tests.length 
                            + "in " + opt_loopCount + " loops");
        System.out.println("Average time per CollationElementIterator.next() nano seconds " + ns);
        System.out.println("performance test on skipped-5 concatenated strings from file -----------");
        
        String totalStr = "";
        int    strlen = 0;
        // appending all the strings
        int linecount = 0;
        while (linecount < tests.length) {
            totalStr += tests[linecount];
            strlen += tests[linecount].length();
            linecount ++;
        }
        System.out.println("Total size of strings " + strlen);
        
        gCount = 0;
        count  = 0;
        iter = ((RuleBasedCollator)icuCol).getCollationElementIterator(totalStr);
        strlen -= 5; // any left over characters are not iterated,
                     // this is to ensure the backwards and forwards iterators
                     // gets the same position
        int strindex = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int count5 = 5;
            strindex = 0;
            iter.setOffset(strindex);
            while (true) {
                if (iter.next() == CollationElementIterator.NULLORDER) {
                    break;
                }
                gCount++;
                count5 --;
                if (count5 == 0) {
                    strindex += 10;
                    if (strindex > strlen) {
                        break;
                    }
                    iter.setOffset(strindex);
                    count5 = 5;
                }
            }
            count ++;
        }
    
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("elapsedTime " + elapsedTime);
        
        // empty loop recalculation
        int tempgCount = 0;
        count = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int count5 = 5;
            strindex = 0;
            iter.setOffset(strindex);
            while (true) {
                tempgCount ++;
                count5 --;
                if (count5 == 0) {
                    strindex += 10;
                    if (strindex > strlen) {
                        break;
                    }
                    iter.setOffset(strindex);
                    count5 = 5;
                }
            }
            count ++;
        }
        elapsedTime -= (System.currentTimeMillis() - startTime);
        System.out.println("elapsedTime " + elapsedTime);
    
        System.out.println("gCount " + gCount);
        ns = (int)(1000000 * elapsedTime / (gCount + 0.0));
        System.out.println("Average time per CollationElementIterator.next() nano seconds " + ns);
    }
    
    void doBackwardIterTest() {
        System.out.print("\n\nPerforming backward iteration performance test with ");
        System.out.println("performance test on strings from file -----------\n");
        
        CollationElementIterator iter = ((RuleBasedCollator)icuCol).getCollationElementIterator("");
        
        int gCount = 0;
        int count = 0;
        long startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int linecount = 0;
            while (linecount < tests.length) {
                String str = tests[linecount];
                iter.setText(str);
                while (iter.previous() != CollationElementIterator.NULLORDER) {
                    gCount++;
                }
                linecount ++;
            }
            count ++;
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("elapsedTime " + elapsedTime + " ms");
        
        // empty loop recalculation
        count = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int linecount = 0;
            while (linecount < tests.length) {
                String str = tests[linecount];
                iter.setText(str);
                linecount ++;
            }
            count ++;
        }
        elapsedTime -= (System.currentTimeMillis() - startTime);
        System.out.println("elapsedTime " + elapsedTime + " ms");
        
        int ns = (int)(1000000 * elapsedTime / (gCount + 0.0));
        System.out.println("Total number of strings compared " + tests.length 
                            + "in " + opt_loopCount + " loops");
        System.out.println("Average time per CollationElementIterator.previous() nano seconds " + ns);
        System.out.println("performance test on skipped-5 concatenated strings from file -----------");
    
        String totalStr = "";
        int    strlen = 0;
        // appending all the strings
        int linecount = 0;
        while (linecount < tests.length) {
            totalStr += tests[linecount];
            strlen += tests[linecount].length();
            linecount ++;
        }
        System.out.println("Total size of strings " + strlen);
        
        gCount = 0;
        count  = 0;
    
        iter = ((RuleBasedCollator)icuCol).getCollationElementIterator(totalStr);
        int strindex = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int count5 = 5;
            strindex = 5;
            iter.setOffset(strindex);
            while (true) {
                if (iter.previous() == CollationElementIterator.NULLORDER) {
                    break;
                }
                 gCount ++;
                 count5 --;
                 if (count5 == 0) {
                     strindex += 10;
                     if (strindex > strlen) {
                        break;
                     }
                     iter.setOffset(strindex);
                     count5 = 5;
                 }
            }
            count ++;
        }
    
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("elapsedTime " + elapsedTime);
        
        // empty loop recalculation
        count = 0;
        int tempgCount = 0;
        startTime = System.currentTimeMillis();
        while (count < opt_loopCount) {
            int count5 = 5;
            strindex = 5;
            iter.setOffset(strindex);
            while (true) {
                 tempgCount ++;
                 count5 --;
                 if (count5 == 0) {
                     strindex += 10;
                     if (strindex > strlen) {
                        break;
                     }
                     iter.setOffset(strindex);
                     count5 = 5;
                 }
            }
            count ++;
        }
        elapsedTime -= (System.currentTimeMillis() - startTime);
        System.out.println("elapsedTime " + elapsedTime);
    
        System.out.println("gCount " + gCount);
        ns = (int)(1000000 * elapsedTime / (gCount + 0.0));
        System.out.println("Average time per CollationElementIterator.previous() nano seconds " + ns);
    }
    
    
    /**---------------------------------------------------------------------------------------
     *
     *    doIterTest()       Iteration test
     *
     *---------------------------------------------------------------------------------------
     */
    void doIterTest() {
        doForwardIterTest();
        doBackwardIterTest();
    }
    
    void setOptions() {
        
        if (opt_java) {
            opt_icu = false;
        }
        
        if (opt_rules.length() != 0) {
            try {
                icuCol = new com.ibm.icu.text.RuleBasedCollator(getCollationRules(opt_rules));
            } catch (Exception e) {
                System.out.println("Cannot open rules:" + e.getMessage());
                System.exit(1);
            }
        } else {
            icuCol = com.ibm.icu.text.Collator.getInstance(
                                LocaleUtility.getLocaleFromName(opt_locale));
        }
        
        javaCol = java.text.Collator.getInstance(
                                LocaleUtility.getLocaleFromName(opt_locale));
        
        if (opt_norm) {
            javaCol.setDecomposition(java.text.Collator.CANONICAL_DECOMPOSITION);
            icuCol.setDecomposition(com.ibm.icu.text.Collator.CANONICAL_DECOMPOSITION);
        }
        
        if (opt_french && opt_frenchoff) {
            System.err.println("Error: specified both -french and -frenchoff options.");
        }
        
        if (opt_french) {
            ((com.ibm.icu.text.RuleBasedCollator)icuCol).setFrenchCollation(true);
        }
        if (opt_frenchoff) {
            ((com.ibm.icu.text.RuleBasedCollator)icuCol).setFrenchCollation(false);
        }
        
        if (opt_lower) {
            ((com.ibm.icu.text.RuleBasedCollator)icuCol).setLowerCaseFirst(true);
        }
        
        if (opt_upper) {
            ((com.ibm.icu.text.RuleBasedCollator)icuCol).setUpperCaseFirst(true);
        }
        
        if (opt_shifted) {
            ((com.ibm.icu.text.RuleBasedCollator)icuCol).setAlternateHandlingShifted(true);
        }
        
        if (opt_level != 0) {
            switch (opt_level) {
                case 1 :
                        javaCol.setStrength(java.text.Collator.PRIMARY);
                        icuCol.setStrength(com.ibm.icu.text.Collator.PRIMARY);
                        break;
                case 2 :
                        javaCol.setStrength(java.text.Collator.SECONDARY);
                        icuCol.setStrength(com.ibm.icu.text.Collator.SECONDARY);
                        break;
                case 3 :
                        javaCol.setStrength(java.text.Collator.TERTIARY);
                        icuCol.setStrength(com.ibm.icu.text.Collator.TERTIARY);
                        break;
                case 4 :
                        icuCol.setStrength(com.ibm.icu.text.Collator.QUATERNARY);
                        break;
                case 5 :
                        javaCol.setStrength(java.text.Collator.IDENTICAL);
                        icuCol.setStrength(com.ibm.icu.text.Collator.IDENTICAL);
                        break;
                default:
                    System.err.println("-level param must be between 1 and 5\n");
                    System.exit(1);
            }
        }
        // load classes at least once before starting
        javaCol.compare("a", "b");
        icuCol.compare("a", "b");
    }
    
    static boolean processOptions(String[] args) {
        int argNum;
        for (argNum =0; argNum < args.length; argNum++) {
            for (int i = 0; i < options.length; i++) {
                if (args[argNum].equalsIgnoreCase(options[i].name)) {
                    switch (options[i].type) {
                        case 0:
                                options[i].value.delete(0, options[i].value.capacity()).append("true");
                                break;
                        case 1:
                                argNum++;
                                if ((argNum >= args.length) || (args[argNum].charAt(0)=='-')) {
                                    System.err.println("value expected for"+ options[i].name +"option.\n");
                                    return false;
                                }
                                try {
                                   /* int value =*/ Integer.parseInt(args[argNum]);
                                    options[i].value.delete(0, options[i].value.capacity()).append(args[argNum]);
                                } catch (NumberFormatException e) {
                                    System.err.println("Expected: a number value");
                                    return false;    
                                }
                                break;
                        case 2:
                                argNum++;
                                if ((argNum >= args.length) || (args[argNum].charAt(0)=='-')) {
                                    System.err.println("value expected for"+ options[i].name +"option.\n");
                                    return false;
                                }
                                options[i].value.delete(0, options[i].value.capacity()).append(args[argNum]);
                                break;
                        default:
                                System.err.println("Option type error: {FLAG=0, NUM=1, STRING=2}");
                                return false;
                    }
                }
            }
        }
        
        opt_fName      = temp_opt_fName.toString();
        opt_locale     = temp_opt_locale.toString();
        opt_rules      = temp_opt_rules.toString();
        if (temp_opt_help.toString().equalsIgnoreCase("true")) {
            opt_help = true;
        }
        opt_loopCount  = Integer.parseInt(temp_opt_loopCount.toString());
        opt_iLoopCount = Integer.parseInt(temp_opt_iLoopCount.toString());
        if (temp_opt_terse.toString().equalsIgnoreCase("true")) {
            opt_terse = true;
        }
        if (temp_opt_qsort.toString().equalsIgnoreCase("true")) {
            opt_qsort = true;
        }
        if (temp_opt_binsearch.toString().equalsIgnoreCase("true")) {
            opt_binsearch = true;
        }
        if (temp_opt_icu.toString().equalsIgnoreCase("true")) {
            opt_icu = true;
        }
        if (temp_opt_usekeys.toString().equalsIgnoreCase("true")) {
            opt_usekeys = true;
        }
        if (temp_opt_strcmp.toString().equalsIgnoreCase("true")) {
            opt_strcmp = true;
        }
        if (temp_opt_strcmpCPO.toString().equalsIgnoreCase("true")) {
            opt_strcmpCPO = true;
        }
        if (temp_opt_keygen.toString().equalsIgnoreCase("true")) {
            opt_keygen = true;
        }
        if (temp_opt_norm.toString().equalsIgnoreCase("true")) {
            opt_norm = true;
        }
        if (temp_opt_french.toString().equalsIgnoreCase("true")) {
            opt_french = true;
        }
        if (temp_opt_frenchoff.toString().equalsIgnoreCase("true")) {
            opt_frenchoff = true;
        }
        if (temp_opt_shifted.toString().equalsIgnoreCase("true")) {
            opt_shifted = true;
        }
        if (temp_opt_lower.toString().equalsIgnoreCase("true")) {
            opt_lower = true;
        }
        if (temp_opt_upper.toString().equalsIgnoreCase("true")) {
            opt_upper = true;
        }
        if (temp_opt_case.toString().equalsIgnoreCase("true")) {
            opt_case = true;
        }
        opt_level      = Integer.parseInt(temp_opt_level.toString());
        if (temp_opt_keyhist.toString().equalsIgnoreCase("true")) {
            opt_keyhist = true;
        }
        if (temp_opt_itertest.toString().equalsIgnoreCase("true")) {
            opt_itertest = true;
        }
        if (temp_opt_dump.toString().equalsIgnoreCase("true")) {
            opt_dump = true;
        }
        if (temp_opt_java.toString().equalsIgnoreCase("true")) {
            opt_java = true;
        }
        
        return true;
    }
    
    /**
     * Invoke the runtime's garbage collection procedure repeatedly
     * until the amount of free memory stabilizes to within 10%.
     */    
    private void callGC() {
        // From "Java Platform Performance".  This is the procedure
        // recommended by Javasoft.
        try {
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
            
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }

    //private boolean needCRLF = false;
    
    public int DOTMASK = 0x7FF;
 
    void dot(int i) {
        if ((i % DOTMASK) == 0) {
            //needCRLF = true;
            // I do not know why print the dot here
            //System.out.print('.');
        }
    }
    
    String readDataLine(BufferedReader br) throws Exception {
        String originalLine = "";
        String line = "";
        
        try {
            line = originalLine = br.readLine();
            if (line == null) return null;
            if (line.length() > 0 && line.charAt(0) == 0xFEFF) line = line.substring(1);
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) line = line.substring(0, commentPos);
            line = line.trim();
        } catch (Exception e) {
            throw new Exception("Line \"{0}\",  \"{1}\"" + originalLine + " "
                                + line + " " + e.toString());
        }
        return line;
    }
    
    void readDataLines() {
        // Read in  the input file.
        //   File assumed to be utf-16.
        //   Lines go onto heap buffers.  Global index array to line starts is created.
        //   Lines themselves are null terminated.
        //
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(opt_fName);
            isr = new InputStreamReader(fis, "UTF-8");
            br= new BufferedReader(isr, 32*1024);
        } catch (Exception e) {
            System.err.println("Error: File access exception: " + e.getMessage() + "!");
            System.exit(2);
        }
        
        int counter = 0;
        
        list = new ArrayList();
        while (true) {
            String line = null;
            try {
                line = readDataLine(br);
            } catch (Exception e) {
                System.err.println("Read File Error" + e.getMessage() + "!");
                System.exit(1);
            }
            
            if (line == null) break;
            if (line.length() == 0) continue;
            dot(counter++);
            list.add(line);
        }
        if (!opt_terse) {
            System.out.println("Read " + counter + " lines in file");
        }
        
        int size = list.size();
        tests = new String [size];
        
        for (int i = 0; i < size; ++i) {
            tests[i] = (String) list.get(i);
        }
    }
    
    /**
     * Get the Collator Rules
     * The Rule File format:
     * 1. leading and trailing whitespaces will be omitted
     * 2. lines with the leading character '#' will be treated as comments
     * 3. File encoding is ISO-8859-1
     */
    String getCollationRules(String ruleFileName) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(opt_rules);
            isr = new InputStreamReader(fis,"ISO-8859-1");
            br= new BufferedReader(isr);
        } catch (Exception e) {
            System.err.println("Error: File access exception: " + e.getMessage() + "!");
            System.exit(2);
        }
        String rules = "";
        String line = "";
        while (true) {
            try {
                line = br.readLine();
            } catch (IOException e) {
                System.err.println("Read File Error" + e.getMessage() + "!");
                System.exit(1);
            }
            if (line == null) {
                break;
            }
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) line = line.substring(0, commentPos);
            line = line.trim();
            rules = rules + line;
        }
        return rules;
    }
    
    //Implementing qsort
    void qSortImpl_java_usekeys(String src[], int fromIndex, int toIndex, java.text.Collator c) {
        int low = fromIndex;
        int high = toIndex;
        String middle = "";
        if (high > low) {
            middle = src[ (low + high) / 2 ];
            while(low <= high) {
                while((low < toIndex) && (compare(c.getCollationKey(src[low]), c.getCollationKey(middle)) < 0)) {
                    ++low;
                }
                while((high > fromIndex) && (compare(c.getCollationKey(src[high]), c.getCollationKey(middle)) > 0)) {
                    --high;
                }
                if(low <= high) {
                    String swap = src[low];
                    src[low] = src[high];
                    src[high] = swap;
                    ++low;
                    --high;
                }
            }
            if(fromIndex < high) {
                qSortImpl_java_usekeys(src, fromIndex, high, c);
            }
            
            if(low < toIndex) {
                qSortImpl_java_usekeys(src, low, toIndex, c);
            }
        }
    }
    
    void qSortImpl_icu_usekeys(String src[], int fromIndex, int toIndex, com.ibm.icu.text.Collator c) {
        int low = fromIndex;
        int high = toIndex;
        String middle = "";
        if (high > low) {
            middle = src[ (low + high) / 2 ];
            while(low <= high) {
                while((low < toIndex) && (compare(c.getCollationKey(src[low]), c.getCollationKey(middle)) < 0)) {
                    ++low;
                }
                while((high > fromIndex) && (compare(c.getCollationKey(src[high]), c.getCollationKey(middle)) > 0)) {
                    --high;
                }
                if(low <= high) {
                    String swap = src[low];
                    src[low] = src[high];
                    src[high] = swap;
                    ++low;
                    --high;
                }
            }
            if(fromIndex < high) {
                qSortImpl_icu_usekeys(src, fromIndex, high, c);
            }
            
            if(low < toIndex) {
                qSortImpl_icu_usekeys(src, low, toIndex, c);
            }
        }
    }
    
    void qSortImpl_nokeys(String src[], int fromIndex, int toIndex, Comparator c) {
        int low = fromIndex;
        int high = toIndex;
        String middle = "";
        if (high > low) {
            middle = src[ (low + high) / 2 ];
            while(low <= high) {
                while((low < toIndex) && (compare(src[low], middle, c) < 0)) {
                    ++low;
                }
                while((high > fromIndex) && (compare(src[high], middle, c) > 0)) {
                    --high;
                }
                if(low <= high) {
                    String swap = src[low];
                    src[low] = src[high];
                    src[high] = swap;
                    ++low;
                    --high;
                }
            }
            if(fromIndex < high) {
                qSortImpl_nokeys(src, fromIndex, high, c);
            }
            
            if(low < toIndex) {
                qSortImpl_nokeys(src, low, toIndex, c);
            }
        }
    }
    
    int compare(String source, String target, Comparator c) {
        globalCount++;
        return c.compare(source, target);
    }
    
    int compare(java.text.CollationKey source, java.text.CollationKey target) {
        globalCount++;
        return source.compareTo(target);
    }
    
    int compare(com.ibm.icu.text.CollationKey source, com.ibm.icu.text.CollationKey target) {
        globalCount++;
        return source.compareTo(target);
    } 
    
    //Class for command line option
    static class OptionSpec {
        String name;
        int type;
        StringBuffer value;
        public OptionSpec(String name, int type, StringBuffer value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }
}
