/**
 *******************************************************************************
 * Copyright (C) 2001-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.translit;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import java.util.*;

public class AnyTransliterator extends Transliterator {
    
    static final boolean DEBUG = false;
    private String targetName;
    private RunIterator it;
    private Position run;
    
    
    public AnyTransliterator(String targetName, UnicodeFilter filter, RunIterator it){
        super("Any-" + targetName, filter);
        this.targetName = targetName;
        this.it = it;
        run = new Position();
    }
    
    public AnyTransliterator(String targetName, UnicodeFilter filter){
        this(targetName, filter, new ScriptRunIterator());
    }
    
    static private Transliterator hex = Transliterator.getInstance("[^\\u0020-\\u007E] hex");
    
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        if (DEBUG) {
            System.out.println("- handleTransliterate " + hex.transliterate(text.toString())
                + ", " + toString(offsets));
        }
        it.reset(text, offsets);
        
        while (it.next(run)) {
            if (targetName.equalsIgnoreCase(it.getName())) {
                if (DEBUG) System.out.println("Skipping identical: " + targetName);
                run.start = run.limit; // show we processed
                continue; // skip if same
            }
            
            Transliterator t;
            String id = it.getName() + '-' + targetName;
            try {
                t = Transliterator.getInstance(id);
            } catch (IllegalArgumentException ex) {
                if (DEBUG) System.out.println("Couldn't find: " + id + ", Trying Latin as Pivot");
                id = it.getName() + "-Latin; Latin-" + targetName;
                try {
                    t = Transliterator.getInstance(id);
                } catch (IllegalArgumentException ex2) {
                    if (DEBUG) System.out.println("Couldn't find: " + id);
                    continue;
                }
            }
            // TODO catch error later!!
                
            if (DEBUG) {
                System.out.println(t.getID());
                System.out.println("input: " + hex.transliterate(text.toString())
                 + ", " + toString(run));
            }
            
            if (isIncremental && it.atEnd()) {
                t.transliterate(text, run);
            } else {
                t.finishTransliteration(text, run);
            }
            // adjust the offsets in line with the changes
            it.adjust(run.limit);
            
            if (DEBUG) {
                System.out.println("output: " + hex.transliterate(text.toString())
                 + ", " + toString(run));
            }
        }

        // show how far we got!
        it.getExpanse(offsets);
        if (run.start == run.limit) offsets.start = offsets.limit;
        else offsets.start = run.start;
        if (DEBUG) {
            System.out.println("+ handleTransliterate: " + ", " + toString(offsets));
            System.out.println();
        }
    }
    
    // should be method on Position
    public static String toString(Position offsets) {
        return "[cs: " + offsets.contextStart
                + ", s: " + offsets.start
                + ", l: " + offsets.limit
                + ", cl: " + offsets.contextLimit
                + "]";
    }
    
    public interface RunIterator {
        public void reset(Replaceable text, Position expanse);
        public void getExpanse(Position run);
        public void reset();
        public boolean next(Position run);
        public void getCurrent(Position run);
        public String getName();
        public void adjust(int newCurrentLimit);
        public boolean atEnd();
    }
    
    /**
     * Returns a series of ranges corresponding to scripts. They will be of the form:
     * ccccSScSSccccTTcTcccc    - where c is common, S is the first script and T is the second
     *|            |            - first run
     *         |            |    - second run
     * That is, the runs will overlap. The reason for this is so that a transliterator can
     * consider common characters both before and after the scripts.
     * The only time that contextStart != start is for the first run 
     *    (the context is the start context of the entire expanse)
     * The only time that contextLimit != limit is for the last run 
     *    (the context is the end context of the entire expanse)
     */
    public static class ScriptRunIterator implements RunIterator {
        private Replaceable text;
        private Position expanse = new Position();
        private Position current = new Position();
        private int script;
        private boolean done = true;
        

        public void reset(Replaceable repText, Position expansePos) {
            set(this.expanse, expansePos);
            this.text = repText;
            reset();
        }
            
        public void reset() {
            done = false;
            //this.expanse = expanse;
            script = UScript.INVALID_CODE;
            // set up first range to be empty, at beginning
            current.contextStart = expanse.contextStart;
            current.start = current.limit = current.contextLimit = expanse.start;            
        }
            
        public boolean next(Position run) {
            if (done) return false;
            if (DEBUG) {
                System.out.println("+cs: " + current.contextStart
                    + ", s: " + current.start
                    + ", l: " + current.limit
                    + ", cl: " + current.contextLimit);
            }
            // reset start context run to the last end
            current.start = current.limit;
            
            // Phase 1. Backup the START value through COMMON until we get to expanse.start or a real script.
            int i, cp;
            int limit = expanse.start;
            for (i = current.start; i > limit; i -= UTF16.getCharCount(cp)) {
                cp = text.char32At(i);
                int script = UScript.getScript(cp);
                if (script != UScript.COMMON && script != UScript.INHERITED) break;
            }
            current.start = i;
            current.contextStart = (i == limit) ? expanse.contextStart : i; // extend at start
            
            // PHASE 2. Move up the LIMIT value through COMMON or single script until we get to expanse.limit
            int lastScript = UScript.COMMON;
            //int veryLastScript = UScript.COMMON;
            limit = expanse.limit; 
            for (i = current.limit; i < limit; i += UTF16.getCharCount(cp)) {
                cp = text.char32At(i);
                int script = UScript.getScript(cp);
                if (script == UScript.INHERITED) script = UScript.COMMON;
                if (script != UScript.COMMON) {
                    // if we find a real script:
                    //   if we already had a script, bail
                    //   otherwise set our script
                    if (lastScript == UScript.COMMON) lastScript = script;
                    else if (lastScript != script) break;
                }
            }
            current.limit = i;
            current.contextLimit = (i == limit) ? expanse.contextLimit : i; // extend at end
            done = (i == limit);
            script = lastScript;
            
            if (DEBUG) {
                System.out.println("-cs: " + current.contextStart
                    + ", s: " + current.start
                    + ", l: " + current.limit
                    + ", cl: " + current.contextLimit);
            }
            
            set(run, current);
            return true;
        }
        
        // SHOULD BE METHOD ON POSITION
        public static void set(Position run, Position current) {
            run.contextStart = current.contextStart;
            run.start = current.start;
            run.limit = current.limit;
            run.contextLimit = current.contextLimit;
        }
        
        public boolean atEnd() {
            return current.limit == expanse.limit;
        }
        
        public void getCurrent(Position run) {
            set(run, current);
        }
        
        public void getExpanse(Position run) {
            set(run, expanse);
        }
        
        public String getName() {
            return UScript.getName(script);
        }
        
        public void adjust(int newCurrentLimit) {
            if (expanse == null) {
                throw new IllegalArgumentException("Must reset() before calling");
            }
            int delta = newCurrentLimit - current.limit;
            current.limit += delta;
            current.contextLimit += delta;
            expanse.limit += delta;
            expanse.contextLimit += delta;
        }
        
        // register Any-Script for every script.
        
        private static Set scriptList = new HashSet();
        
        public static void registerAnyToScript() {
            synchronized (scriptList) {
                Enumeration sources = Transliterator.getAvailableSources();
                while(sources.hasMoreElements()) {
                    String source = (String) sources.nextElement();
                    if (source.equals("Any")) continue; // to keep from looping
                    
                    Enumeration targets = Transliterator.getAvailableTargets(source);
                    while(targets.hasMoreElements()) {
                        String target = (String) targets.nextElement();
                        if (UScript.getCode(target) == null) continue; // SKIP unless we have a script (or locale)
                        if (scriptList.contains(target)) continue; // already encountered
                        scriptList.add(target); // otherwise add for later testing
                        
                        Set variantSet = add(new TreeSet(), Transliterator.getAvailableVariants(source, target));
                        if (variantSet.size() < 2) {
                            AnyTransliterator at = new AnyTransliterator(target, null);
                            DummyFactory.add(at.getID(), at);
                        } else {
                            Iterator variants = variantSet.iterator();
                            while(variants.hasNext()) {
                                String variant = (String) variants.next();
                                AnyTransliterator at = new AnyTransliterator(
                                    (variant.length() > 0) ? target + "/" + variant : target, null);
                                DummyFactory.add(at.getID(), at);
                            }
                        }
                    }
                }
            }
        }
        
        static class DummyFactory implements Transliterator.Factory {
            static DummyFactory singleton = new DummyFactory();
            static HashMap m = new HashMap();

            // Since Transliterators are immutable, we don't have to clone on set & get
            static void add(String ID, Transliterator t) {
                m.put(ID, t);
                System.out.println("Registering: " + ID + ", " + t.toRules(true));
                Transliterator.registerFactory(ID, singleton);
            }
            public Transliterator getInstance(String ID) {
                return (Transliterator) m.get(ID);
            }
        }
        
        // Nice little Utility for converting Enumeration to collection
        static Set add(Set s, Enumeration enumeration) {
            while(enumeration.hasMoreElements()) {
                s.add(enumeration.nextElement());
            }
            return s;
        }
        
        
    }
}
