/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.dev.tool.localeconverter;
import java.util.*;

class PosixCollationBuilder {
    private static final int MAX_WEIGHTS = 4;
    private static final int MAX_COMPOSITION = 4;
    private static int nextCharNumber = 1;
    private Hashtable weightSymbols = new Hashtable();
    private Hashtable contractions = new Hashtable();
    private Hashtable rules = new Hashtable();
    private CollationRule lastRule = null;
    private PosixCharMap map;
    private SortedVector order;
    private static int FIRST_WEIGHT_CHAR = 0x0000F7FF;
    private int nextWeightChar = FIRST_WEIGHT_CHAR;
    private CollationRule ignoreRule;   //rule for the collating-symbol IGNORE
    
    public class CollationRule {
        int charNumber;
        String value;
        int nextWeight = 0;
        String[] weightSource = new String[MAX_WEIGHTS];
        int weight[][] = null;
        StringBuffer source = new StringBuffer();
        
        public CollationRule(String symbol) {
            charNumber= nextCharNumber++;
            value = symbol;
            for (int i = 0; i < MAX_WEIGHTS; i++) {
                weightSource[i] = symbol;
            }
            source.append(map.backmapValue(symbol));
            source.append("\t\t");
        }
        private CollationRule(CollationRule other, int composition) {
            charNumber = other.charNumber;
            value = other.value;
            nextWeight = other.nextWeight;
            for (int i = 0; i < MAX_WEIGHTS; i++) {
                String source = other.weightSource[i];
                if (source.length() > composition) {
                    weightSource[i] = ""+source.charAt(composition);
                } else {
                    weightSource[i] = value;
                }
            }
        }
 
       //HexToUnicodeTransliterator myTranslit = new HexToUnicodeTransliterator("<U###0>");
        public void addWeight(String symbol) {
         //   ReplaceableString tSymbol = new ReplaceableString(symbol);
           // myTranslit.transliterate(tSymbol);
                //limit the size of a single weight
            symbol = unescape(symbol);    
            if (symbol.length() > MAX_COMPOSITION) {
                System.err.println("WARNING: Weights of composition greater than "+MAX_COMPOSITION+" were truncated.");
                symbol = symbol.substring(0, MAX_COMPOSITION);
            }
                //limit the number of weights
            if (nextWeight < MAX_WEIGHTS) {
                if (nextWeight > 0) {
                    source.append(";");
                }
                for (int i = 0; i < symbol.length(); i++) {
                    source.append(map.backmapValue(""+symbol.charAt(i)));
                }
                weightSource[nextWeight++] = symbol;
                weight = null;
            }
        }
        public int compare(CollationRule other) {
            if (other == null) return compare(ignoreRule);
            resolveWeights();
            other.resolveWeights();
            int compareSize = Math.min(getSize(), other.getSize());
            for (int j = 0; j < compareSize; j++) {
                for (int i = 0; i < MAX_WEIGHTS; i++) {
                    int diff = weight[j][i] - other.weight[j][i];
                    if (diff < 0) {
                        return -(i+1);
                    } if (diff > 0) {
                        return i+1;
                    }
                }
            }
            return getSize() - other.getSize();
        }
        public boolean isMultiWeight() {
            return getSize() > 1;
        }
        public int getSize() {
            int size = 0;
            for (int i = 1; i < weightSource.length; i++) {
                size = Math.max(size, weightSource[i].length());
            }
            return size;
        }
        public CollationRule getComponent(int ndx) {
            return new CollationRule(this, ndx);
        }
        public String getValue() {
            return value;
        }
        public String getSymbol() {
            String newValue = isContraction();
            if (newValue != null) {
                return newValue;
            } else {
                newValue = isWeightSymbol();
                if (newValue != null) {
                    return newValue;
                } else {
                    return value;
                }
            }
        }
        public String getSource() {
            return source.toString();
        }
        private String isContraction() {
            return (String)contractions.get(value);
        }
        private String isWeightSymbol() {
            return (String)weightSymbols.get(value);
        }
        public CollationRule seeksToRule() {
            CollationRule comp;
            if (getSize() <= 1) {
                comp = this;    //save an object creation
            } else {
                comp = getComponent(0);
            }
            int ndx = order.indexOf(comp);
            if (ndx == 0) {
                return this;
            } else {
                CollationRule exp;
                do {
                    exp = (CollationRule)order.elementAt(ndx--);
                } while (ndx > 0 && exp.getSize() > 1);
                return exp;
            }
        }
        public String getExpansion() {
            if (getSize() <= 1) {
                return null;
            } else {
                StringBuffer expansion = new StringBuffer();
                for (int j = 0; j < getSize(); j++) {
                    CollationRule comp = getComponent(j);
                    int ndx = order.indexOf(comp);
                    CollationRule exp;
                    do {
                        exp = (CollationRule)order.elementAt(ndx--);
                    } while (ndx >= 0 && exp.getSize() > 1);
                    expansion.append(exp.getSymbol());
                }
                return expansion.toString();
            }
        }
        public String toString() {
            return source.toString();
/*          resolveWeights();
            StringBuffer buf = new StringBuffer();
            buf.append(charNumber);
            buf.append(' ');
            buf.append(value);
            buf.append(' ');
            buf.append(getSymbol());
            buf.append(' ');
            buf.append((isWeightSymbol() != null)?"W":" ");
            buf.append(' ');
            for (int i = 0; i < MAX_WEIGHTS; i++) {
                buf.append(weightSource[i]);
                buf.append(' ');
            }
            for (int i = 0; i < getSize(); i++) {
                buf.append("[ ");
                for (int j = 0; j < MAX_WEIGHTS; j++) {
                    int w = weight[i][j];
                    buf.append(w);
                    buf.append(' ');
                }
                buf.append(']');
            }
            return buf.toString();
*/
        }
        private void resolveWeights() {
            if (weight == null) {
                weight = new int[MAX_COMPOSITION][MAX_WEIGHTS];
                for (int j = 0; j < MAX_WEIGHTS; j++) {
                    String symbol = weightSource[j];
                    if (symbol.length() <= 1) {
                        weight[0][j] = ordinalityOf(symbol);
                    } else {                
                        for (int i = 0; i < symbol.length(); i++) {
                            char c = symbol.charAt(i);
                            weight[i][j] = ordinalityOf(""+c);
                        }
                    }
                }
            }
        }
    }
    
    public PosixCollationBuilder(PosixCharMap map) {
        this.map = map;
        String ignoreSymbol = defineWeightSymbol("IGNORE");
        ignoreRule = new CollationRule(ignoreSymbol);
        rules.put(ignoreSymbol, ignoreRule);
        lastRule = ignoreRule;
            //{{INIT_CONTROLS
//}}
}

    public String defineWeightSymbol(String symbol) {
        order = null;
        String c = nextFreeWeightChar();
        map.defineMapping(symbol, c);
        weightSymbols.put(c, symbol);
        weightSymbols.put(symbol, c);
        return c;
    }
    
    public String defineContraction(String symbol, String value) {
        order = null;
        String c = nextFreeWeightChar();
        map.defineMapping(symbol, c);
        contractions.put(c, value);
        return c;
    }
    
    private String nextFreeWeightChar() {
        String result = "";
        String mappedSource;
        do {
            result = ""+(char)nextWeightChar--;
            mappedSource = map.backmapValue(result);
        } while (result != mappedSource);
        return result;
    }
    /**
     * unescape a string in the format <U####>
     */
    public static String unescape(String src){
        StringBuffer result = new StringBuffer();
        int maxDig = 4;
        if(src == null){
            return src;
        }
        int srcLen= src.length();
        for(int i=0; i<srcLen;i++){
            char c = src.charAt(i);
            if(c == '<'){
               if(srcLen > i+1){
                   char c2 = src.charAt(++i);
                   if(c2 == 'U' && (i+maxDig+1)< srcLen){
                        i++;
                        if( src.charAt(i+maxDig)== '>'){
                            String subStr = src.substring(i,i+maxDig);
                            try{
                                Integer val = Integer.valueOf(subStr,16);
                                result.append((char) val.intValue());
                                
                            }catch(NumberFormatException ex){
                                result.append(c);
                                result.append(c2);
                                result.append(subStr);
                            }
                            i += maxDig;
                            continue;
                        }else{
                            result.append(c);
                            result.append(c2);
                            result.append(src.charAt(i));
                            System.err.println("WARNING: The escape sequence is not terminated at " + i +" in string: " + src);
                            continue;
                        }

                   }else{
                        result.append(c);
                        result.append(c2);
                        continue;
                   }
               }
            }
            result.append(c);
        }
        return result.toString();
    }
    public int ordinalityOf(String symbol) {
//        HexToUnicodeTransliterator newTranslit = new HexToUnicodeTransliterator();
//        ReplaceableString tSymbol = new ReplaceableString(symbol);
//        newTranslit.transliterate(tSymbol);
        symbol = unescape(symbol);
        CollationRule w = (CollationRule)rules.get(symbol);
        if (w != null) {
            return w.charNumber;
        } else {
            System.err.print("ERROR: Weight symbol not found: ");
            for (int i = 0 ; i < symbol.length(); i++) {
                char c = symbol.charAt(i);
                System.err.print("\\u");
                System.err.print(HEX_DIGIT[(c & 0x0F000) >> 12]); // HEX_DIGIT works for octal
                System.err.print(HEX_DIGIT[(c & 0x0F00) >> 8]); // HEX_DIGIT works for octal
                System.err.print(HEX_DIGIT[(c & 0x00F0) >> 4]);
                System.err.println(HEX_DIGIT[(c & 0x000F)]);
            }
            System.err.println("       Weight given maximum possible value.");
            return Integer.MAX_VALUE;
        }
    }
//    HexToUnicodeTransliterator myTranslit = new HexToUnicodeTransliterator("<U###0>");
    public void addRule(String symbol) {
  //      ReplaceableString tSymbol = new ReplaceableString(symbol);
   //     myTranslit.transliterate(tSymbol);
        symbol = unescape(symbol);
        if (symbol.length() > 1) {
            System.err.println("WARNING: Undefined element '"+symbol+"'.  collating-symbol generated.");
            symbol = defineWeightSymbol(symbol);
        }
    
        order = null;
        lastRule = new CollationRule(symbol);
        rules.put(symbol, lastRule);
    }
    
    public void addRule(CollationRule rule) {
        order = null;
        lastRule = rule;
        rules.put(rule.value, rule);
    }
    
    public void addWeight(String weight) {
        if (weight.length() > 1) {
            //check to see if it's a bogus weight symbol.
            weight = map.mapKey(weight);
        }
        order = null;
        lastRule.addWeight(weight);
    }

    public Enumeration getRules() {
        return rules.elements();
    }

    public SortedVector getSortOrder() {
        if (order == null) {
            order = new SortedVector(
                new Comparator() {
                    public int compare(final Object i, final Object j) {
                        final CollationRule o1 = (CollationRule)i;
                        final CollationRule o2 = (CollationRule)j;
                        if(o1 !=null && o2 != null){
                            final boolean w1 = o1.isWeightSymbol() != null;
                            final boolean w2 = o2.isWeightSymbol() != null;
                                //sort weights first
                            if (w1 && !w2) {
                                return -1;
                            } else if (!w1 && w2) {
                                return 1;
                            } else {
                                return o1.compare(o2);
                            }
                        }
                        return -1;
                    }
                }
            );
            order.addElements(rules.elements());
                //remove weight symbols from the list
            int i;
            for (i = 0; i < order.size(); i++) {
                CollationRule r = (CollationRule)order.elementAt(i);
                if (r.isWeightSymbol() == null) {
                    break;
                }
            }
            order.removeElements(0, i);
        }
        return order;
    }

    static final char[] HEX_DIGIT = {'0','1','2','3','4','5','6','7',
                     '8','9','A','B','C','D','E','F'};
    //{{DECLARE_CONTROLS
//}}
}
