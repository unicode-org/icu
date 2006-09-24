package com.ibm.text.UCD;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.Utility;

public class TestUnicodeInvariants {
    private static final int
    HELP1 = 0,
    FILE = 1,
    RANGE = 2,
    TABLE = 3
    ;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.create("file", 'f', UOption.REQUIRES_ARG),
        UOption.create("norange", 'n', UOption.NO_ARG),
        UOption.create("table", 't', UOption.NO_ARG),
    };
    
    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);

    	String file = "UnicodeInvariants.txt";
    	if (options[FILE].doesOccur) file = options[FILE].value;
    	boolean doRange = !options[RANGE].doesOccur;
        System.out.println("File:\t" + file);
        System.out.println("Ranges?\t" + doRange);
        System.out.println("HTML?\t" + options[TABLE].doesOccur);
    	
        testInvariants(file, doRange);
    }

    /**
    * Chain together several SymbolTables. 
    * @author Davis
    */
   static class ChainedSymbolTable implements SymbolTable {
       // TODO: add accessors?
       private List symbolTables;
       /**
        * Each SymbolTable is each accessed in order by the other methods,
        * so the first in the list is accessed first, etc.
        * @param symbolTables
        */
       ChainedSymbolTable(SymbolTable[] symbolTables) {
           this.symbolTables = Arrays.asList(symbolTables);
       }
       public char[] lookup(String s) {
           for (Iterator it = symbolTables.iterator(); it.hasNext();) {
               SymbolTable st = (SymbolTable) it.next();
               char[] result = st.lookup(s);
               if (result != null) return result;
           }
           return null;
       }

       public UnicodeMatcher lookupMatcher(int ch) {
           for (Iterator it = symbolTables.iterator(); it.hasNext();) {
               SymbolTable st = (SymbolTable) it.next();
               UnicodeMatcher result = st.lookupMatcher(ch);
               if (result != null) return result;
           }
           return null;
       }
       
       // Warning: this depends on pos being left alone unless a string is returned!!
       public String parseReference(String text, ParsePosition pos, int limit) {
           for (Iterator it = symbolTables.iterator(); it.hasNext();) {
               SymbolTable st = (SymbolTable) it.next();
               String result = st.parseReference(text, pos, limit);
               if (result != null) return result;
           }
           return null;
       }
   }
   
   static final UnicodeSet INVARIANT_RELATIONS = new UnicodeSet("[\\~ \\= \\! \\? \\< \\> \u2264 \u2265 \u2282 \u2286 \u2283 \u2287]");
   
   public static void testInvariants(String outputFile, boolean doRange) throws IOException {
       String[][] variables = new String[100][2];
       int variableCount = 0;
       PrintWriter out = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR, "UnicodeInvariantResults.txt");
       out.write('\uFEFF'); // BOM
       BufferedReader in = BagFormatter.openUTF8Reader("com/ibm/text/UCD/", outputFile);
       
       BagFormatter errorLister = new BagFormatter();
       errorLister.setMergeRanges(doRange);
       errorLister.setUnicodePropertyFactory(ToolUnicodePropertySource.make(""));
       errorLister.setShowLiteral(TransliteratorUtilities.toXML);
       if (options[TABLE].doesOccur) errorLister.setTabber(new Tabber.HTMLTabber());
       
       BagFormatter showLister = new BagFormatter();
       showLister.setUnicodePropertyFactory(ToolUnicodePropertySource.make(""));
       showLister.setMergeRanges(doRange);
       showLister.setShowLiteral(TransliteratorUtilities.toXML);
       if (options[TABLE].doesOccur) showLister.setTabber(new Tabber.HTMLTabber());
              
       ChainedSymbolTable st = new ChainedSymbolTable(new SymbolTable[] {
           ToolUnicodePropertySource.make(UCD.lastVersion).getSymbolTable("\u00D7"),
           ToolUnicodePropertySource.make(Default.ucdVersion()).getSymbolTable("")});
       ParsePosition pp = new ParsePosition(0);
       int parseErrorCount = 0;
       int testFailureCount = 0;
       while (true) {
           String line = in.readLine();
           if (line == null) break;
           if (line.startsWith("\uFEFF")) line = line.substring(1);
           out.println(line);
           line = line.trim();
           int pos = line.indexOf('#');
           if (pos >= 0) line = line.substring(0,pos).trim();
           if (line.length() == 0) continue;
           if (line.equalsIgnoreCase("Stop")) break;

           // fix all the variables
           String oldLine = line;
           line = Utility.replace(line, variables, variableCount);

           // detect variables
           if (line.startsWith("Let")) {
               int x = line.indexOf('=');
               variables[variableCount][0] = line.substring(3,x).trim();
               variables[variableCount][1] = line.substring(x+1).trim();
               variableCount++;
               if (false) System.out.println("Added variable: <" + variables[variableCount-1][0] + "><"
                        + variables[variableCount-1][1] + ">");
               continue;
           }

           // detect variables
           if (line.startsWith("Show")) {
           		String part = line.substring(4).trim();
           		if (part.startsWith("Each")) {
           			part = part.substring(4).trim();
           			showLister.setMergeRanges(false);
           		}
           		pp.setIndex(0);
           		UnicodeSet leftSet = new UnicodeSet(part, pp, st);
           		showLister.showSetNames(out, leftSet);
           		showLister.setMergeRanges(doRange);
				continue;
           }
           
           if (line.startsWith("Test")) {
        	   line = line.substring(4).trim();
           }

          char relation = 0;
           String rightSide = null;
           String leftSide = null;
           UnicodeSet leftSet = null;
           UnicodeSet rightSet = null;
           try {
               pp.setIndex(0);
               leftSet = new UnicodeSet(line, pp, st);
               leftSide = line.substring(0,pp.getIndex());
               eatWhitespace(line, pp);
               relation = line.charAt(pp.getIndex());
               if (!INVARIANT_RELATIONS.contains(relation)) {
                   throw new ParseException("Invalid relation, must be one of " + INVARIANT_RELATIONS.toPattern(false),
                       pp.getIndex());
               }
               pp.setIndex(pp.getIndex()+1); // skip char
               eatWhitespace(line, pp);
               int start = pp.getIndex();
               rightSet = new UnicodeSet(line, pp, st);
               rightSide = line.substring(start,pp.getIndex());
               eatWhitespace(line, pp);
               if (line.length() != pp.getIndex()) {
                   throw new ParseException("Extra characters at end", pp.getIndex());
               }
           } catch (ParseException e) {
               out.println("PARSE ERROR:\t" + line.substring(0,e.getErrorOffset())
                   + "<@>" + line.substring(e.getErrorOffset()));
               out.println();
               out.println("**** START Error Info ****");
               out.println(e.getMessage());
               out.println("**** END Error Info ****");
               out.println();
               parseErrorCount++;
               continue;
           } catch (IllegalArgumentException e) {
               out.println("PARSE ERROR:\t" + line);
               out.println();
               out.println("**** START Error Info ****");
               out.println(e.getMessage());
               out.println("**** END Error Info ****");
               out.println();
               parseErrorCount++;
               continue;
           }
           
           boolean ok = true;
           switch(relation) {
               case '=': case '\u2261': ok = leftSet.equals(rightSet); break;
               case '<': case '\u2282': ok = rightSet.containsAll(leftSet) && !leftSet.equals(rightSet); break;
               case '>': case '\u2283': ok = leftSet.containsAll(rightSet) && !leftSet.equals(rightSet); break;
               case '\u2264': case '\u2286': ok = rightSet.containsAll(leftSet); break;
               case '\u2265': case '\u2287': ok = leftSet.containsAll(rightSet); break;
               case '!': ok = leftSet.containsNone(rightSet); break;
               case '?': ok = !leftSet.equals(rightSet) 
                       && !leftSet.containsAll(rightSet) 
                       && !rightSet.containsAll(leftSet)
                       && !leftSet.containsNone(rightSet); 
                   break;
               default: throw new IllegalArgumentException("Internal Error");
           }
           if (ok) continue;
           out.println();
           out.println(String.valueOf(ok).toUpperCase(Locale.ENGLISH));
           out.println("**** START Error Info ****");
           errorLister.showSetDifferences(out, rightSide, rightSet, leftSide, leftSet);
           out.println("**** END Error Info ****");
           out.println();
           testFailureCount++;      
       }
       out.println();
       out.println("**** SUMMARY ****");
       out.println();
       out.println("ParseErrorCount=" + parseErrorCount);
       out.println("TestFailureCount=" + testFailureCount);
       out.close();
       System.out.println("ParseErrorCount=" + parseErrorCount);
       System.out.println("TestFailureCount=" + testFailureCount);
   }

   /**
    * @param line
    * @param pp
    */
   private static void eatWhitespace(String line, ParsePosition pp) {
       int cp = 0;
       int i;
       for (i = pp.getIndex(); i < line.length(); i += UTF16.getCharCount(cp)) {
           cp = UTF16.charAt(line, i);
           if (!com.ibm.icu.lang.UCharacter.isUWhiteSpace(cp)) {
               break;
           }
       }
       pp.setIndex(i);
   }
}
