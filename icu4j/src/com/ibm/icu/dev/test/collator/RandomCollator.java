//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2002-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;


import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Quoter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Random;

public class RandomCollator extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new RandomCollator().run(args);
        //new CollationAPITest().TestGetTailoredSet();
    }

    static final int CONSTRUCT_RANDOM_COUNT = 100;
    static final int FORMAL_TEST_COUNT = 1000;
    
    static final String POSITION = "{$$$}";
    
    /*
    class Shower extends BagFormatter.Shower {
        public void print(String arg) {
            log(arg);
        }
    }
    
    public Shower LOG = new Shower();
    */
       
    public void TestRandom() throws IOException {
//        int year 
//        = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
//        if (year < 2004) {
//            System.out.println("\nTestRandom skipped for 2003");
//            return;
//        }
        //String fileName;
        PrintWriter pw = BagFormatter.openUTF8Writer(System.getProperty("user.dir")+File.separator, "RandomCollationTestLog.txt");
        TestCollator tc = new TestCollator(chars);
        pw.println("Collation Test Run");
        pw.println("Note: For parse-exception, " + POSITION + " indicates the errorOffset");
        pw.println("Rules:");
        pw.println(currentRules);
        String rules = "<unknown>";
        int sCount = 0;
        int peCount = 0;
        int oeCount = 0;
        for (int i = 0; i < CONSTRUCT_RANDOM_COUNT; ++i) {
            try {
                rules = get();
                if (true) {                   
                    Collator c = new RuleBasedCollator(rules.toString());
                    tc.test(c, FORMAL_TEST_COUNT);
                } else {
                    pw.println(rules);
                }
                logln("ok");
                sCount++;
            } catch (ParseException pe) {
                peCount++;
                pw.println("========PARSE EXCEPTION======== (" + i + ")");
                int errorOffset = pe.getErrorOffset();
                pw.print(rules.substring(0,errorOffset));
                pw.print(POSITION);
                pw.println(rules.substring(errorOffset));
                //pw.println("========ERROR======== (" + i + ")");
                //pe.printStackTrace(pw);
                //pw.println("========END======== (" + i + ")");
                errln("ParseException");
            } catch (Exception e) {
                oeCount++;
                pw.println("========OTHER EXCEPTION======== (" + i + ")");
                e.printStackTrace(pw);
                pw.println("========RULES======== (" + i + ")");
                pw.println(rules);
                //pw.println("========END======== (" + i + ")");
                errln("ParseException");
            }
        }
        pw.println("Successful: " + sCount 
            + ",\tParseException: " + peCount 
            + ",\tOther Exception: " + oeCount);
        logln("Successful: " + sCount 
            + ",\tParseException: " + peCount 
            + ",\tOther Exception: " + oeCount);
        pw.close();

    }
    
    public static class TestCollator extends TestComparator {
        BNF rs;
        
        TestCollator(UnicodeSet chars) {
            rs = new BNF(new Random(0), new Quoter.RuleQuoter())
            .addRules("$root = " + chars + "{1,8};").complete();
        }
        
        public Object newObject(Object c) {
            return rs.next();
        }
    
        public String format(Object c) {
            return BagFormatter.hex.transliterate(c.toString());
        }
    }

    private BNF bnf;
    String currentRules = null;
    UnicodeSet chars;
    
    public String get() {
        return bnf.next();
    }
    
    public RandomCollator() {
        
    }
    protected void init()throws Exception{
        init(1,10, new UnicodeSet("[AZa-z<\\&\\[\\]]"));
    }
    private void init(int minRuleCount, int maxRuleCount, UnicodeSet setOfChars) {
        this.chars = setOfChars;
        bnf = new BNF(new Random(0), new Quoter.RuleQuoter())
        .addSet("$chars", setOfChars)
        .addRules(collationBNF)
        .complete();
    }
    
    private static String collationBNF =
        "$s = ' '? 50%;\r\n" +
        "$relationList = (" +        "   '<'" +        " | '  <<'" +        " | '  ;'" +
        " | '    <<<'" +        " | '    ,'" +
        " | '      ='" +        ");\r\n" +
        "$alternateOptions = non'-'ignorable | shifted;\r\n" +
        "$caseFirstOptions = off | upper | lower;\r\n" +
        "$strengthOptions = '1' | '2' | '3' | '4' | 'I';\r\n" +
        "$commandList = '['" +        " ( alternate ' ' $alternateOptions" +        " | backwards' 2'" +        " | normalization ' ' $onoff " +        " | caseLevel ' ' $onoff " +        " | hiraganaQ ' ' $onoff" +        " | caseFirst ' ' $caseFirstOptions" +        " | strength ' ' $strengthOptions" +        " ) ']';\r\n" +
        "$ignorableTypes = (tertiary | secondary | primary) ' ' ignorable;\r\n" +
        "$allTypes = variable | regular | implicit | trailing | $ignorableTypes;\r\n" +
        "$onoff = on | off;\r\n" +
        "$positionList = '[' (first | last) ' ' $allTypes ']';\r\n" +        "$beforeList = '[before ' ('1' | '2' | '3') ']';\r\n" +        "$string = $chars{1,5}~@;\r\n" +
        "$crlf = '\r\n';\r\n" +
        "$rel1 = '[variable top]' $s ;\r\n" +
        "$p1 = ($string $s '|' $s)? 25%;\r\n" +
        "$p2 = ('\\' $s $string $s)? 25%;\r\n" +
        "$rel2 = $p1 $string $s $p2;\r\n" +
        "$relation = $relationList $s ($rel1 | $rel2) $crlf;\r\n" +
        "$command = $commandList $crlf;\r\n" +
        "$reset = '&' $s ($beforeList $s)? 10% ($positionList | $string 10%) $crlf;\r\n" +
        "$mostRules = $command 1% | $reset 5% | $relation 25%;\r\n" +
        "$root = $command{0,5} $reset $mostRules{1,20};\r\n";
    
    
/*    
    
    
    gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
    gc ; Cc        ; Control
    gc ; Cf        ; Format
    gc ; Cn        ; Unassigned
    gc ; Co        ; Private_Use
    gc ; Cs        ; Surrogate
    gc ; L         ; Letter                           # Ll | Lm | Lo | Lt | Lu
    gc ; LC        ; Cased_Letter                     # Ll | Lt | Lu
    gc ; Ll        ; Lowercase_Letter
    gc ; Lm        ; Modifier_Letter
    gc ; Lo        ; Other_Letter
    gc ; Lt        ; Titlecase_Letter
    gc ; Lu        ; Uppercase_Letter
    gc ; M         ; Mark                             # Mc | Me | Mn
    gc ; Mc        ; Spacing_Mark
    gc ; Me        ; Enclosing_Mark
    gc ; Mn        ; Nonspacing_Mark
    gc ; N         ; Number                           # Nd | Nl | No
    gc ; Nd        ; Decimal_Number
    gc ; Nl        ; Letter_Number
    gc ; No        ; Other_Number
    gc ; P         ; Punctuation                      # Pc | Pd | Pe | Pf | Pi | Po | Ps
    gc ; Pc        ; Connector_Punctuation
    gc ; Pd        ; Dash_Punctuation
    gc ; Pe        ; Close_Punctuation
    gc ; Pf        ; Final_Punctuation
    gc ; Pi        ; Initial_Punctuation
    gc ; Po        ; Other_Punctuation
    gc ; Ps        ; Open_Punctuation
    gc ; S         ; Symbol                           # Sc | Sk | Sm | So
    gc ; Sc        ; Currency_Symbol
    gc ; Sk        ; Modifier_Symbol
    gc ; Sm        ; Math_Symbol
    gc ; So        ; Other_Symbol
    gc ; Z         ; Separator                        # Zl | Zp | Zs
    gc ; Zl        ; Line_Separator
    gc ; Zp        ; Paragraph_Separator
    gc ; Zs        ; Space_Separator
*/

    /*
    // each rule can be:
    // "[" command "]"
    // "& [" position "]"
    // "&" before chars
    // relation "[variable top]"
    // relation (chars "|")? chars ("/" chars)?
    // plus, a reset must come before a relation
    
    // the following reflects the above rules, plus allows whitespace.
    Pick chars = Pick.string(1, 5, Pick.codePoint(uSet)); // insert something needing quotes
    Pick s = Pick.maybe(0.8, Pick.unquoted(" ")).name("Space");    // optional space
    Pick CRLF = Pick.unquoted("\r\n");
        
    Pick rel1 = Pick.and(Pick.unquoted("[variable top]")).and2(s);
    Pick p1 = Pick.maybe(0.25, Pick.and(chars).and2(s).and2("|").and2(s));
    Pick p2 = Pick.maybe(0.25, Pick.and("/").and2(s).and2(chars).and2(s));
    Pick rel2 = Pick.and(p1).and2(chars).and2(s).and2(p2);
    Pick relation = Pick.and(Pick.or(relationList)).and2(s)
        .and2(Pick.or(1, rel1).or2(10, rel2))
        .and2(CRLF).name("Relation");
            
    Pick command = Pick.and(Pick.or(commandList)).and2(CRLF).name("Command");
        
    Pick reset = Pick.and("&").and2(s)            
        .and2(0.1, Pick.or(beforeList)).and2(s)            
        .and2(Pick.or(0.1, Pick.or(positionList)).or2(1.0, chars))
        .and2(CRLF).name("Reset");
    Pick rule = Pick.and(Pick.or(1, command).or2(5, reset).or2(25, relation)).name("Rule");
    Pick rules2 = Pick.and(Pick.repeat(0,5,command))            
        .and2(reset)            
        .and2(Pick.repeat(1,20,rule)).name("Rules");
    rules = Pick.Target.make(rules2);
   
   static final String[] relationList = {" <", "  <<", "    <<<", "     =", "  ;", "   ,"};
    
    static final String[] commandList = {
        "[alternate non-ignorable]",        "[alternate shifted]",
        "[backwards 2]",
        "[normalization off]",
        "[normalization on]",
        "[caseLevel off]",
        "[caseLevel on]",
        "[caseFirst off]",
        "[caseFirst upper]",
        "[caseFirst lower]",
        "[strength 1]",
        "[strength 2]",
        "[strength 3]",
        "[strength 4]",
        "[strength I]",
        "[hiraganaQ off]",
        "[hiraganaQ on]"
    };
    
    static final String[] positionList = {
        "[first tertiary ignorable]",
        "[last tertiary ignorable]",
        "[first secondary ignorable]",
        "[last secondary ignorable]",
        "[first primary ignorable]",
        "[last primary ignorable]",
        "[first variable]",
        "[last variable]",
        "[first regular]",
        "[last regular]",
        "[first implicit]",
        "[last implicit]",
        "[first trailing]",
        "[last trailing]"
    };
    
    static final String[] beforeList = {
        "[before 1]", 
        "[before 2]",
        "[before 3]"
    };
    */
}
//#endif

