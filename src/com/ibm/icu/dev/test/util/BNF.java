/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.icu.text.UnicodeSet;
import java.util.Random;

public class BNF {
    private Map map = new HashMap();
    private Set variables = new HashSet();
    private Pick pick = null;
    private Pick.Target target = null;
    private Tokenizer t;
    private Quoter quoter;
    private Random random;
    
    public String next() {
        return target.next();
    }
    
    public String getInternal() {
        return pick.getInternal(0, new HashSet());
    }
    
    /*
    + "weight = integer '%';"
    + "range = '{' integer (',' integer?)? '}' weight*;"
    + "quote = '@';"
    + "star = '*' weight*;"
    + "plus = '+' weight*;"
    + "maybe = '?' weight?;"
    + "quantifier = range | star | maybe | plus;"
    + "core = string | unicodeSet | '(' alternation ')';"
    + "sequence = (core quantifier*)+;"
    + "alternation = sequence (weight? ('|' sequence weight?)+)?;"
    + "rule = string '=' alternation;"; 
    
    
    *      Match 0 or more times
    +      Match 1 or more times
    ?      Match 1 or 0 times
    {n}    Match exactly n times
    {n,}   Match at least n times
    {n,m}  Match at least n but not more than m times  
 
 

    */
    
    public BNF(Random random, Quoter quoter) {
        this.random = random;
        this.quoter = quoter;
        t = new Tokenizer();
    }
    
    public BNF addRules(String rules) {
        t.setSource(rules);        
        while (addRule());
        return this; // for chaining
    }
    
    public BNF complete() {
        // check that the rules match the variables, except for $root in rules
        Set ruleSet = map.keySet();
        // add also 
        variables.add("$root");
        variables.addAll(t.getLookedUpItems());
        if (!ruleSet.equals(variables)) {
            String msg = showDiff(variables, ruleSet);
            if (msg.length() != 0) msg = "Error: Missing definitions for: " + msg;
            String temp = showDiff(ruleSet, variables);
            if (temp.length() != 0) temp = "Warning: Defined but not used: " + temp;
            if (msg.length() == 0) msg = temp;
            else if (temp.length() != 0) {
                msg = msg + "; " + temp;           
            }
            error(msg);           
        } 
        
        if (!ruleSet.equals(variables)) {
            String msg = showDiff(variables, ruleSet);
            if (msg.length() != 0) msg = "Missing definitions for: " + msg;
            String temp = showDiff(ruleSet, variables);
            if (temp.length() != 0) temp = "Defined but not used: " + temp;
            if (msg.length() == 0) msg = temp;
            else if (temp.length() != 0) {
                msg = msg + "; " + temp;           
            }
            error(msg);           
        } 
        
        // replace variables by definitions
        Iterator it = ruleSet.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Pick expression = (Pick) map.get(key);
            Iterator it2 = ruleSet.iterator();  
            if (false && key.equals("$crlf")) {
                System.out.println("debug") ;
            }
            while (it2.hasNext()) {
                Object key2 = it2.next();
                if (key.equals(key2)) continue;
                Pick expression2 = (Pick) map.get(key2);
                expression2.replace(key, expression);
            }
        }
        pick = (Pick) map.get("$root");
        target = Pick.Target.make(pick, random, quoter);
        // TODO remove temp collections
        return this;
    }
    
    String showDiff(Set a, Set b) {
        Set temp = new HashSet();
        temp.addAll(a);
        temp.removeAll(b);
        if (temp.size() == 0) return "";
        StringBuffer buffer = new StringBuffer();
        Iterator it = temp.iterator();
        while (it.hasNext()) {
            if (buffer.length() != 0) buffer.append(", ");
            buffer.append(it.next().toString());
        }
        return buffer.toString();
    }
    
    void error(String msg) {
        throw new IllegalArgumentException(msg
        + "\r\n" + t.toString());
    }
    
    
    
    private boolean addRule() {
        int type = t.next();
        if (type == Tokenizer.DONE) return false;
        if (type != Tokenizer.STRING) error("missing weight");
        String s = t.getString();
        if (s.length() == 0 || s.charAt(0) != '$') error("missing $ in variable");
        if (t.next() != '=') error("missing =");
        int startBody = t.index;
        Pick rule = getAlternation();
        if (rule == null) error("missing expression");
        t.addSymbol(s, t.getSource(), startBody, t.index);
        if (t.next() != ';') error("missing ;");       
        return addPick(s, rule);
    }

    protected boolean addPick(String s, Pick rule) {
        Object temp = map.get(s);
        if (temp != null) error("duplicate variable");
        if (rule.name == null) rule.name(s);
        map.put(s, rule);
        return true;
    }
    
    public BNF addSet(String variable, UnicodeSet set) {
        if (set != null) {
            String body = set.toString();
            t.addSymbol(variable, body, 0, body.length());        
            addPick(variable, Pick.codePoint(set));
        }
        return this;
    }
    
    int maxRepeat = 99;
    
    Pick qualify(Pick item) {
        int[] weights;
        int type = t.next();
        switch(type) {
            case '@': 
                return new Pick.Quote(item);
            case '~': 
                return new Pick.Morph(item);
            case '?': 
                int weight = getWeight();
                if (weight == NO_WEIGHT) weight = 50;
                weights = new int[] {100-weight, weight};
                return Pick.repeat(0, 1, weights, item);
            case '*': 
                weights = getWeights();
                return Pick.repeat(1, maxRepeat, weights, item);
            case '+': 
                weights = getWeights();
                return Pick.repeat(1, maxRepeat, weights, item);
            case '{':
                if (t.next() != Tokenizer.NUMBER) error("missing number");
                int start = (int) t.getNumber();
                int end = start;
                type = t.next();
                if (type == ',') {
                    end = maxRepeat;
                    type = t.next();
                    if (type == Tokenizer.NUMBER) {
                        end = (int)t.getNumber();
                        type = t.next();
                    }
                }
                if (type != '}') error("missing }");
                weights = getWeights();
                return Pick.repeat(start, end, weights, item);
        }
        t.backup();
        return item;
    }
    
    Pick getCore() {
        int token = t.next();
        if (token == Tokenizer.STRING) {
            String s = t.getString();
            if (s.charAt(0) == '$') variables.add(s);
            return Pick.string(s);
        }
        if (token == Tokenizer.UNICODESET) {
            return Pick.codePoint(t.getUnicodeSet());            
        }
        if (token != '(') {
            t.backup();
            return null;
        }
        Pick temp = getAlternation();
        token = t.next();
        if (token != ')') error("missing )");    
        return temp;    
    }
    
    Pick getSequence() {
        Pick.Sequence result = null;
        Pick last = null;
        while (true) {
            Pick item = getCore();
            if (item == null) {
                if (result != null) return result;
                if (last != null) return last;
                error("missing item");
            }
            // qualify it as many times as possible
            Pick oldItem;
            do {
                oldItem = item;
                item = qualify(item);
            } while (item != oldItem);
            // add it in
            if (last == null) {
                last = item;
            } else {
                if (result == null) result = Pick.makeSequence().and2(last);            
                result = result.and2(item);
            }
        }
    }
    
    // for simplicity, we just use recursive descent
    Pick getAlternation() {
        Pick.Alternation result = null;
        Pick last = null;
        int lastWeight = NO_WEIGHT;
        while (true) {
            Pick temp = getSequence();
            if (temp == null) error("empty alternation");
            int weight = getWeight();
            if (weight == NO_WEIGHT) weight = 1;
            if (last == null) {
                last = temp;
                lastWeight = weight;
            } else {
                if (result == null) result = Pick.makeAlternation().or2(lastWeight, last);
                result = result.or2(weight, temp);   
            }
            int token = t.next();
            if (token != '|') {
                t.backup();
                if (result != null) return result;
                if (last != null) return last;
            }
        }        
    }
    
    private static final int NO_WEIGHT = Integer.MIN_VALUE;
    
    int getWeight() {       
        int weight;
        int token = t.next();
        if (token != Tokenizer.NUMBER) {
            t.backup();
            return NO_WEIGHT;
        }
        weight = (int)t.getNumber();
        token = t.next();
        if (token != '%') error("missing %");
        return weight;
    }
    
    int[] getWeights() {
        ArrayList list = new ArrayList();
        while (true) {
            int weight = getWeight();
            if (weight == NO_WEIGHT) break;
            list.add(new Integer(weight));
        }
        if (list.size() == 0) return null;
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            result[i] = ((Integer)list.get(i)).intValue();
        }
        return result;
    }
}
