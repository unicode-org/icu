package com.ibm.icu.dev.demo.translit;
import com.ibm.icu.dev.demo.impl.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;

public class AnyTransliterator extends Transliterator {
	
	static final boolean DEBUG = true;
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
    
    public static class ScriptRunIterator implements RunIterator {
    	Replaceable text;
    	Position expanse = new Position();
    	Position current = new Position();
    	int script;
    	boolean done = true;
    	
    	public void reset(Replaceable text, Position expanse) {
    		set(this.expanse, expanse);
    		this.text = text;
    		reset();
    	}
    		
    	public void reset() {
    		done = false;
    		this.expanse = expanse;
    		script = UScript.INVALID_CODE;
    		// set up first range to be empty, at beginning
    		current.contextStart = expanse.contextStart;
    		current.start = current.limit = expanse.start;
    		
    		// find the COMMON stuff at the start of the expanse
    		int i, cp;
    		int limit = expanse.limit;
    		for (i = current.limit; i < limit; i += UTF16.getCharCount(cp)) {
    			cp = text.char32At(i);
    			int script = UScript.getScript(cp);
    			if (script != UScript.COMMON && script != UScript.INHERITED) break;
    		}
    		if (i == limit) done = true;
    		else current.contextLimit = i;
    		
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
    		current.contextStart = current.limit;
    		current.start = current.contextLimit;
    		
    		// set up variables and loop
    		int limit = expanse.limit; 
    		int lastScript = UScript.COMMON;
    		int veryLastScript = UScript.COMMON;
    		int i, cp;
    		for (i = current.start; i < limit; i += UTF16.getCharCount(cp)) {
    			cp = text.char32At(i);
    			int script = UScript.getScript(cp);
    			if (script == UScript.INHERITED) script = UScript.COMMON;
    			if (script != UScript.COMMON) {
    				// if we find a real script:
    				//   if we already had a script, bail
    				//   otherwise set our script
    				if (lastScript == UScript.COMMON) lastScript = script;
    				else if (lastScript != script) break;
    			} else if (veryLastScript != UScript.COMMON) {
    				// if we found COMMON -- and -- the last character was not, reset
    				current.limit = i;
    			}
    			veryLastScript = script;
    		}
    		// fix end
    		if (veryLastScript != UScript.COMMON) {
    			// if we found COMMON -- and -- the last character was not, reset
    			current.limit = i;
    		}
    		// if we are at the very end of the expanse, then expand it.
    		if (i == limit) {
    			current.contextLimit = expanse.contextLimit;
    			done = true;
    		} else {
    			current.contextLimit = i;
    		}
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
    	
    }
}