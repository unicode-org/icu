package com.ibm.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.Utility;

public class CheckICU {
    static final BagFormatter bf = new BagFormatter();
    
    public static void main(String[] args) throws IOException {
        System.out.println("Start");
        test();
        System.out.println("End");
    }
    
    static UnicodeSet itemFailures;
    static ICUPropertyFactory icuFactory;
    static ToolUnicodePropertySource toolFactory;
    
    static class ReplaceLabel extends UnicodeLabel {
        UnicodeProperty p;
        ReplaceLabel(UnicodeProperty p) {
            this.p = p;
        }
        public String getValue(int codepoint, boolean isShort) {
            // TODO Auto-generated method stub
            return p.getValue(codepoint, isShort).replace('_',' ');
        }
        public int getMaxWidth(boolean v) {
            return p.getMaxWidth(v);           
        }
    }
    
 
    public static void test() throws IOException {
        //generateFile("4.0.0", "DerivedCombiningClass");
        //generateFile("4.0.0", "DerivedCoreProperties");
        checkUCD();
        itemFailures = new UnicodeSet();
        icuFactory = ICUPropertyFactory.make();
        toolFactory = ToolUnicodePropertySource.make("4.0.0");

        String[] quickList = {
            "Canonical_Combining_Class",
            // "Script", "Bidi_Mirroring_Glyph", "Case_Folding",
            //"Numeric_Value"
        };
        for (int i = 0; i < quickList.length; ++i) {
            testProperty(quickList[i], -1);
        }
        if (quickList.length > 0) return;

        Collection availableTool = toolFactory.getAvailableNames();
       
        Collection availableICU = icuFactory.getAvailableNames();
        System.out.println(showDifferences("Property Aliases", "ICU", availableICU, "Tool", availableTool));
        Collection common = new TreeSet(availableICU);
        common.retainAll(availableTool);
        
        for (int j = UnicodeProperty.BINARY; j < UnicodeProperty.LIMIT_TYPE; ++j) {
            System.out.println();
            System.out.println(UnicodeProperty.getTypeName(j));
            Iterator it = common.iterator();
            while (it.hasNext()) {
                String prop = (String)it.next();
                testProperty(prop, j);
            }
        }
    }

    private static void checkUCD() throws IOException {
        UCD myUCD = UCD.make("4.0.0");
        Normalizer nfc = new Normalizer(Normalizer.NFC, "4.0.0");
        UnicodeSet leading = new UnicodeSet();
        UnicodeSet trailing = new UnicodeSet();
        UnicodeSet starter = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (myUCD.getCombiningClass(i) == 0) starter.add(i);
            if (nfc.isTrailing(i)) trailing.add(i);
            if (nfc.isLeading(i)) leading.add(i);
        }
        PrintWriter pw = bf.openUTF8Writer(UCD_Types.GEN_DIR, "Trailing.txt");
        pw.println("+Trailing+Starter");
        bf.showSetNames(pw,  new UnicodeSet(trailing).retainAll(starter));
        pw.println("+Trailing-Starter");
        bf.showSetNames(pw, new UnicodeSet(trailing).removeAll(starter));
        pw.println("-Trailing-Starter");
        bf.showSetNames(pw, new UnicodeSet(trailing).complement().removeAll(starter));
        pw.println("+Trailing+Leading");
        bf.showSetNames(pw, new UnicodeSet(trailing).retainAll(leading));
        pw.println("+Trailing-Leading");
        bf.showSetNames(pw, new UnicodeSet(trailing).removeAll(leading));
        pw.close();
    }
    /*
     *                 int icuType;
                int toolType;
                Collection icuAliases;
                Collection toolAliases;
                String firstDiffICU;
                String firstDiffTool;
                String firstDiffCP;
                String icuProp;
                String toolProp;

     */

    private static void testProperty(String prop, int typeFilter) {
        UnicodeProperty icuProp = icuFactory.getProperty(prop);
        int icuType = icuProp.getType();
        
        if (typeFilter >= 0 && icuType != typeFilter) return;
        
        System.out.println();
        System.out.println("Testing: " + prop);
        UnicodeProperty toolProp = toolFactory.getProperty(prop);
        
        int toolType = toolProp.getType();
        if (icuType != toolType) {
            System.out.println("FAILURE Type: ICU: " + UnicodeProperty.getTypeName(icuType)
                + "\tTool: " + UnicodeProperty.getTypeName(toolType));
        }
        
        Collection icuAliases = icuProp.getNameAliases(new ArrayList());
        Collection toolAliases = toolProp.getNameAliases(new ArrayList());
        System.out.println(showDifferences("Aliases", "ICU", icuAliases, "Tool", toolAliases));
        
        icuAliases = icuProp.getAvailableValues(new ArrayList());
        toolAliases = toolProp.getAvailableValues(new ArrayList());
        System.out.println(showDifferences("Value Aliases", "ICU", icuAliases, "Tool", toolAliases));
        
        // TODO do property value aliases
        itemFailures.clear();
        String firstDiffICU = null, firstDiffTool = null, firstDiffCP = null;
        for (int i = 0; i <= 0x10FFFF; ++i) {
            /*if (i == 0x0237) {
                System.out.println();
            }
            */
            String icuValue = icuProp.getValue(i);
            String toolValue = toolProp.getValue(i);
            if (!equals(icuValue, toolValue)) {
                itemFailures.add(i);
                if (firstDiffCP == null) {
                    firstDiffICU = icuValue;
                    firstDiffTool = toolValue;
                    firstDiffCP = Utility.hex(i);
                }
            }
        }
        if (itemFailures.size() != 0) {
            System.out.println("FAILURE " + itemFailures.size() + " Differences: ");
            System.out.println(itemFailures.toPattern(true));
            if (firstDiffICU != null) firstDiffICU = bf.hex.transliterate(firstDiffICU);
            if (firstDiffTool != null) firstDiffTool = bf.hex.transliterate(firstDiffTool);
            System.out.println(firstDiffCP 
                + "\tICU: <" + firstDiffICU
                + ">\tTool: <" + firstDiffTool + ">");
        }
        System.out.println("done"); 
        
        // do values later, and their aliases
        /*
        System.out.println("-Values");
        UnicodeSet
        System.out.println(showDifferences("ICU", availableICU, "Tool", availableTool));
        */
    }
    
    static boolean equals(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
    
    static public String showDifferences(
        String title,
        String name1,
        Collection set1,
        String name2,
        Collection set2) {
        
        Collection temp = new TreeSet(set1);
        temp.retainAll(set2);

        if (set1.size() == temp.size()) {
            return title + ": " + name1 + " == " + name2 + ": " + bf.join(set1);
        }

        StringBuffer result = new StringBuffer();
        result.append(title + "\tFAILURE\r\n");
        result.append("\t" + name1 + " = " + bf.join(set1) + "\r\n");
        result.append("\t" + name2 + " = " + bf.join(set2) + "\r\n");

        // damn'd collection doesn't have a clone, so
        // we go with Set, even though that
        // may not preserve order and duplicates
         if (temp.size() != 0) {
            result.append("\t" + name2 + " & " + name1 + ":\r\n");
            result.append("\t" + bf.join(temp));
            result.append("\r\n");
        }


        temp.clear();
        temp.addAll(set1);
        temp.removeAll(set2);
        if (temp.size() != 0) {
            result.append("\t" + name1 + " - " + name2 + ":\r\n");
            result.append("\t" + bf.join(temp));
            result.append("\r\n");
        }
        
        temp.clear();
        temp.addAll(set2);
        temp.removeAll(set1);
        if (temp.size() != 0) {
            result.append("\t" + name2 + " - " + name1 + ":\r\n");
            result.append("\t" + bf.join(temp));
            result.append("\r\n");
        }


        return result.toString();
    }
    
    
}