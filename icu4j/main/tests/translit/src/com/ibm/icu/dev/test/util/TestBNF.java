/*
 *******************************************************************************
 * Copyright (C) 2002-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Random;

import com.ibm.icu.text.UnicodeSet;

public class TestBNF {
    
    static final String[] testRules = {
        "$root = [ab]{3};",
        
        "$root = [ab]{3,};",
        
        "$root = [ab]{3,5};",
        
        "$root = [ab]*;",
        
        "$root = [ab]?;",
        
        "$root = [ab]+;",
        
        "$us = [a-z];" +
        "$root = [0-9$us];",
        
        "$root = a $foo b? 25% end 30% | $foo 50%;\r\n" +
        "$foo = c{1,5} 20%;",
        
        "$root = [a-z]{1,5}~;",
        
        "$root = [a-z]{5}~;",
        
        "$root = '\\' (u | U0010 | U000 $hex) $hex{4} ;\r\n" +
        "$hex = [0-9A-Fa-f];",
    };
        
    static String unicodeSetBNF = "" +
    "$root = $leaf | '[' $s $root2 $s ']' ;\r\n" +
    "$root2 = $leaf | '[' $s $root3 $s ']' | ($root3 $s ($op $root3 $s){0,3}) ;\r\n" +
    "$root3 = $leaf | '[' $s $root4 $s ']' | ($root4 $s ($op $root4 $s){0,3}) ;\r\n" +
    "$root4 = $leaf | ($leaf $s ($op $leaf $s){0,3}) ;\r\n" +
    "$op = (('&' | '-') $s)? 70%;" +
    "$leaf = '[' $s $list $s ']' | $prop;\r\n" +
    "$list = ($char $s ('-' $s $char $s)? 30%){1,5} ;\r\n" +
    "$prop = '\\' (p | P) '{' $s $propName $s '}' | '[:' '^'? $s $propName $s ':]';\r\n" +
    "$needsQuote = [\\-\\][:whitespace:][:control:]] ;\r\n" +
    "$char = [[\\u0000-\\U00010FFFF]-$needsQuote] | $quoted ;\r\n" +
    "$quoted = '\\' ('u' | 'U0010' | 'U000' $hex) $hex{4} ;\r\n" +
    "$hex = [0-9A-Fa-f];\r\n" +
    "$s = ' '? 20%;\r\n" +
    "$propName = (whitespace | ws) | (uppercase | uc) | (lowercase | lc) | $category;\r\n" +
    "$category = ((general | gc) $s '=' $s)? $catvalue;\r\n" +
    "$catvalue = (C | Other | Cc | Control | Cf | Format | Cn | Unassigned | L | Letter);\r\n";

    public static void main (String[] args) {
        testTokenizer();
        for (int i = 0; i < testRules.length; ++i) {
            testBNF(testRules[i], null, 20);          
        }
        
        testBNF(unicodeSetBNF, null, 20);
        //testParser();
    }
    
    static void testBNF(String rules, UnicodeSet chars, int count) {
        BNF bnf = new BNF(new Random(0), new Quoter.RuleQuoter())
        .addSet("$chars", chars)
        .addRules(rules)
        .complete();

        System.out.println("====================================");
        System.out.println("BNF");
        System.out.println(rules);
        System.out.println(bnf.getInternal());
        for (int i = 0; i < count; ++i) {
            System.out.println(i + ": " + bnf.next());
        }
    }
    
    /*
    public static testManual() {
        Pick p = Pick.maybe(75,Pick.unquoted("a"));
        testOr(p, 1);
        p = Pick.or(new String[]{"", "a", "bb", "ccc"});
        testOr(p, 3);
        p = Pick.repeat(3, 5, new int[]{20, 30, 20}, "a");
        testOr(p, 5);        
        p = Pick.codePoint("[a-ce]");
        testCodePoints(p);        
        p = Pick.codePoint("[a-ce]");
        testCodePoints(p);        
        p = Pick.string(2, 8, p);
        testOr(p,10);
        
        p = Pick.or(new String[]{"", "a", "bb", "ccc"});
        p = Pick.and(p).and2(p).and2("&");
        testMatch(p, "abb&");
        testMatch(p, "bba");
        
        // testEnglish();        
    }
    */
    
    static void testMatch(Pick p, String source) {
        Pick.Position pp = new Pick.Position();
        boolean value = p.match(source, pp);
        System.out.println("Match: " + value + ", " + pp);      
    }
    /*
    static void testParser() {
        try {
            Pick.Target target = new Pick.Target();
            for (int i = 0; i < rules.length; ++i) {
                target.addRule(rules[i]);
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    */
    
    static class Counts {
        int[] counts;       
        Counts(int max) {
            counts = new int[max+1];
        }
        void inc(int index) {
            counts[index]++;
        }
        void show() {
            System.out.println("Printing Counts");
            for (int i = 0; i < counts.length; ++i) {
                if (counts[i] == 0) continue;
                System.out.println(i + ": " + counts[i]);
            }
            System.out.println();
        }
    }
    
/*    static final String[] rules = {
        "$s = ' ';",
        "$noun = dog | house | government | wall | street | zebra;",
        "$adjective = red | glorious | simple | nasty | heavy | clean;",
        "$article = quickly | oddly | silently | boldly;",
        "$adjectivePhrase = ($adverb $s)? 50% $adjective* 0% 30% 20% 10%;",
        "$nounPhrase = $articles $s ($adjectivePhrase $s)? 30% $noun;",
        "$verb = goes | fishes | walks | sleeps;",
        "$tverb = carries | lifts | overturns | hits | jumps on;",
        "$copula = is 30% | seems 10%;",
        "$sentence1 = $nounPhrase $s $verb $s ($s $adverb)? 30%;",
        "$sentence2 = $nounPhrase $s $tverb $s $nounPhrase ($s $adverb)? 30%;",
        "$sentence3 = $nounPhrase $s $copula $s $adjectivePhrase;",
        "$conj = but | and | or;",
        "$sentence4 = $sentence1 | $sentence2 | $sentence3 20% | $sentence4 $conj $sentence4 20%;",
        "$sentence = $sentence4 '.';"};
 */
    /*
    private static void testEnglish() {
        Pick s = Pick.unquoted(" ");
        Pick verbs = Pick.or(new String[]{"goes", "fishes", "walks", "sleeps"});
        Pick transitive = Pick.or(new String[]{"carries", "lifts", "overturns", "hits", "jumps on"});
        Pick nouns = Pick.or(new String[]{"dog", "house", "government", "wall", "street", "zebra"});
        Pick adjectives = Pick.or(new String[]{"red", "glorious", "simple", "nasty", "heavy", "clean"});
        Pick articles = Pick.or(new String[]{"the", "a"});
        Pick adverbs = Pick.or(new String[]{"quickly", "oddly", "silently", "boldly"});
        Pick adjectivePhrase = Pick.and(0.5, Pick.and(adverbs).and2(s)).and2(adjectives);
        Pick nounPhrase = Pick.and(articles).and2(s)
            .and2(0.3, Pick.and(adjectivePhrase).and2(s))
            .and2(nouns);
        Pick copula = Pick.or(new String[]{"is", "seems"});
        Pick sentence1 = Pick.and(nounPhrase).and2(s).and2(verbs)
            .and2(0.3, Pick.and(s).and2(adverbs)).name("s1");
        Pick sentence2 = Pick.and(nounPhrase).and2(s).and2(transitive).and2(s).and2(nounPhrase)
            .and2(0.3, Pick.and(s).and2(adverbs)).name("s2");
        Pick sentence3 = Pick.and(nounPhrase).and2(s).and2(copula).and2(s).and2(adjectivePhrase).name("s3");
        Pick conj = Pick.or(new String[]{", but", ", and", ", or"});
        Pick forward = Pick.unquoted("forward");
        Pick pair = Pick.and(forward).and2(conj).and2(s).and2(forward).name("part");
        Pick sentenceBase = Pick.or(sentence1).or2(sentence2).or2(sentence3).or2(0.6666, pair).name("sentence");
        sentenceBase.replace(forward, sentenceBase);
        Pick sentence = Pick.and(sentenceBase).and2(Pick.unquoted("."));
        Pick.Target target = Pick.Target.make(sentence);
        for (int i = 0; i < 50; ++i) {
            System.out.println(i + ": " + target.next());
        }
    }
    private static void testOr(Pick p, int count) {
        Pick.Target target = Pick.Target.make(p);
        Counts counts = new Counts(count + 10);
        for (int i = 0; i < 1000; ++i) {
            String s = target.next();
            counts.inc(s.length());
        }
        counts.show();
    }
    private static void testCodePoints(Pick p) {
        Pick.Target target = Pick.Target.make(p);
        Counts counts = new Counts(128);
        for (int i = 0; i < 10000; ++i) {
            String s = target.next();
            counts.inc(s.charAt(0));
        }
        counts.show();
    }
    */
    public static void printRandoms() {
        BNF bnf = new BNF(new Random(0), new Quoter.RuleQuoter())
        .addRules("[a-z]{2,5}").complete();
        System.out.println("Start");
        for (int i = 0; i < 100; ++i) {
            String temp = bnf.next();
            System.out.println(i + ")\t" + temp);
        }
    }
    
    public static void testTokenizer() {
        Tokenizer t = new Tokenizer();
        
        String[] samples = {"a'b'c d #abc\r e", "'a '123 321", 
            "\\\\", "a'b", "a'", "abc def%?ghi", "%", "a", "\\ a", "a''''b"};
        for (int i = 0; i < samples.length; ++i) {
            t.setSource(samples[i]);
            System.out.println();
            System.out.println("Input: " + t.getSource());
            int type = 0;
            while (type != Tokenizer.DONE) {
                type = t.next();
                System.out.println(t.toString(type, false));
            }
        }
    }

}

