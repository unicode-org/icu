// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.Vector;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class ScriptData extends TagValueData
{
    public static class Record
    {
        private int startChar;
        private int endChar;
        private int scriptCode;
        
        Record()
        {
            // nothing?
        }
        
        Record(int theChar, int theScriptCode)
        {
            this(theChar, theChar, theScriptCode);
        }
        
        Record(int theStartChar, int theEndChar, int theScriptCode)
        {
            startChar = theStartChar;
            endChar = theEndChar;
            scriptCode = theScriptCode;
        }
        
        public int startChar()
        {
            return startChar;
        }
        
        public int endChar()
        {
            return endChar;
        }
        
        public int scriptCode()
        {
            return scriptCode;
        }
        
        public int compareTo(Record that)
        {
            return this.startChar - that.startChar;
        }
        
        public String toString()
        {
            return "[" +  Utility.hex(startChar, 6) + ".." +
                   Utility.hex(endChar, 6) + ", " +
                   UScript.getShortName(scriptCode).toLowerCase() + "ScriptCode]";
        }
    }
    
    // TODO: Exceptions could be generated algorithmically
    private static class TagException
    {
        private String icuTag;
        private String otTag;
        
        public TagException(String icu, String ot)
        {
            icuTag = icu;
            otTag  = ot;
        }
        
        public String getICUTag()
        {
            return icuTag;
        }
        
        public String getOTTag()
        {
            return otTag;
        }
    }
    
    // TODO: short name longer than long name, replace repeated chars w/ space...
    private ScriptData.TagException exceptions[] = {
            new ScriptData.TagException("laoo", "lao "),
            new ScriptData.TagException("nkoo", "nko "), // New code from ISO 15924, not sure this will be OT tag
            new ScriptData.TagException("vaii", "vai "), // New code from ISO 15924, not sure this will be OT tag
            new ScriptData.TagException("yiii", "yi  ")
        };
        
    
    // TODO: binary search the exceptions list?
    private String getException(String icu)
    {
        for(int i = 0; i < exceptions.length; i += 1) {
            if (exceptions[i].getICUTag().equals(icu)) {
                return exceptions[i].getOTTag();
            }
        }
        
        return icu;
    }
        
    //
    // Straight insertion sort from Knuth vol. III, pg. 81
    //
    private void sort()
    {
        for (int j = 1; j < fRecords.length; j += 1) {
            int i;
            Record v = fRecords[j];

            for (i = j - 1; i >= 0; i -= 1) {
                if (v.compareTo(fRecords[i]) >= 0) {
                    break;
                }

                fRecords[i + 1] = fRecords[i];
            }

            fRecords[i + 1] = v;
        }
    }

    ScriptData()
    {
        int commonScript = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, "COMMON");
        int scriptCount;
        Vector rv = new Vector();
        
        fMinScript  = UCharacter.getIntPropertyMinValue(UProperty.SCRIPT);
        fMaxScript  = UCharacter.getIntPropertyMaxValue(UProperty.SCRIPT);
        scriptCount = fMaxScript - fMinScript + 1;
        
        System.out.println("Collecting script data for " + scriptCount + " scripts...");
        
        fScriptNames = new String[scriptCount];
        fScriptTags  = new String[scriptCount];
        
        for (int script = fMinScript; script <= fMaxScript; script += 1) {
            fScriptNames[script - fMinScript] = UScript.getName(script).toUpperCase();
            fScriptTags[script - fMinScript]  = UScript.getShortName(script).toLowerCase();
            
            if (script != commonScript) {
                UnicodeSet scriptSet  = new UnicodeSet("\\p{" + fScriptTags[script - fMinScript] + "}");
                UnicodeSetIterator it = new UnicodeSetIterator(scriptSet);
            
                while (it.nextRange()) {
                    Record record = new Record(it.codepoint, it.codepointEnd, script);
                    
                    rv.addElement(record);
                }
            }
        }
        
        fRecords = new Record[rv.size()];
        
        for (int i = 0; i < rv.size(); i += 1) {
            fRecords[i] = (Record) rv.elementAt(i);
        }
        
        System.out.println("Collected " + rv.size() + " records. Sorting...");
        sort();
        
        System.out.println("Done.");
    }
    
    public int getMinValue()
    {
        return fMinScript;
    }
    
    public int getMaxValue()
    {
        return fMaxScript;
    }
    
    public int getRecordCount()
    {
        return fRecords.length;
    }
    
    public String getTag(int value)
    {
        if (value >= fMinScript && value <= fMaxScript) {
            return getException(fScriptTags[value - fMinScript]);
        }
        
        return "zyyx";
    }
    
    public String getTagLabel(int value)
    {
        if (value >= fMinScript && value <= fMaxScript) {
            return fScriptTags[value - fMinScript];
        }
        
        return "zyyx";
    }
    
    public String makeTag(int value)
    {
        if (value >= fMinScript && value <= fMaxScript) {
            String tag = getException(fScriptTags[value - fMinScript]);
            
            return TagUtilities.makeTag(tag);
        } else {
            return "0x00000000";
        }
    }
    
    public String getName(int value)
    {
        if (value >= fMinScript && value <= fMaxScript) {
            return fScriptNames[value - fMinScript];
        }
        
        return "COMMON";
    }
    
    public Record getRecord(int index)
    {
        if (fRecords != null && index < fRecords.length) {
            return fRecords[index];
        }
        
        return null;
    }
    
    private int fMinScript;
    private int fMaxScript;
    private String fScriptNames[];
    private String fScriptTags[];
    private Record fRecords[];
}
