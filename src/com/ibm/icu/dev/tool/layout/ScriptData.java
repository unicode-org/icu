/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptData.java,v $
 * $Date: 2003/06/03 18:49:31 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.*;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.impl.Utility;

public class ScriptData
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
                UnicodeSet scriptSet  = new UnicodeSet("\\p{" + fScriptTags[script] + "}");
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
    
    public int getMinScript()
    {
        return fMinScript;
    }
    
    public int getMaxScript()
    {
        return fMaxScript;
    }
    
    public int getRecordCount()
    {
        return fRecords.length;
    }
    
    public String getScriptTag(int scriptCode)
    {
        if (scriptCode >= fMinScript && scriptCode <= fMaxScript) {
            return fScriptTags[scriptCode - fMinScript];
        }
        
        return "zyyx";
    }
    
    public String getScriptTagLabel(int scriptCode)
    {
        return getScriptTag(scriptCode);
    }
    
    public String makeScriptTag(int scriptCode)
    {
        if (scriptCode >= fMinScript && scriptCode <= fMaxScript) {
            return TagUtilities.makeTag(fScriptTags[scriptCode - fMinScript]);
        } else {
            return "0x00000000";
        }
    }
    
    public String getScriptName(int scriptCode)
    {
        if (scriptCode >= fMinScript && scriptCode <= fMaxScript) {
            return fScriptNames[scriptCode - fMinScript];
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
};