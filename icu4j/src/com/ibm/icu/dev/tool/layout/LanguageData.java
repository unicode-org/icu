/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Apr 4, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/LanguageData.java,v $ $Date: 2003/04/15 01:23:49 $ $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

/**
 * This class holds the list of languages.
 * 
 * @author emader
 *
 */
public class LanguageData
{
    public static class Record
    {
        private String tag;
        private String name;
        
        public Record(String tag, String name)
        {
            this.tag = tag;
            this.name = name;
        }
        
        public String tag()
        {
            return tag;
        }
        
        public String name()
        {
            return name;
        }
    }
        
    /*
     * This is temporary data until there is some resolution between
     * the OpenType language system tags and the ISO and RFC standards...
     */
    private Record[] languages =
    {
        new Record("",    "(Default)"),
        new Record("ARA", "Arabic"),
        new Record("ASM", "Assamese"),
        new Record("BEN", "Bengali"),
        new Record("FAR", "Farsi"),
        new Record("GUJ", "Gujarati"),
        new Record("HIN", "Hindi"),
        new Record("IWR", "Hebrew"),
        new Record("JII", "Yiddish"),
        new Record("JAN", "Japanese"),
        new Record("KAN", "Kannada"),
        new Record("KOK", "Konkani"),
        new Record("KOR", "Korean"),
        new Record("KSH", "Kashmiri"),
        new Record("MAL", "Malayalam (Traditional)"),
        new Record("MAR", "Marathi"),
        new Record("MLR", "Malayalam (Reformed)"),
        new Record("MNI", "Manipuri"),
        new Record("ORI", "Oriya"),
        new Record("SAN", "Sanscrit"),
        new Record("SND", "Sindhi"),
        new Record("SNH", "Sinhalese"),
        new Record("SYR", "Syriac"),
        new Record("TAM", "Tamil"),
        new Record("TEL", "Telugu"),
        new Record("THA", "Thai"),
        new Record("URD", "Urdu"),
        new Record("ZHP", "Chinese (Phonetic)"),
        new Record("ZHS", "Chinese (Simplified)"),
        new Record("ZHT", "Chinese (Traditional)")
    };
    
    private int minLanguage = 0;
    private int maxLanguage = languages.length - 1;
    
    public int getMinLanguage()
    {
        return minLanguage;
    }
    
    public int getMaxLanguage()
    {
        return maxLanguage;
    }
    
    public String getLanguageTag(int i)
    {
        if (i < minLanguage || i > maxLanguage) {
            return null;
        }
        
        return languages[i - minLanguage].tag();
    }
    
    public String getLanguageTagLabel(int i)
    {
        if (i < minLanguage || i > maxLanguage) {
            return null;
        }
        
        return TagUtilities.tagLabel(languages[i - minLanguage].tag());
    }
    
    public String makeLanguageTag(int i)
    {
        if (i < minLanguage || i > maxLanguage) {
            return null;
        }
        
        return TagUtilities.makeTag(languages[i - minLanguage].tag());
    }
    
    public String getLanguageName(int i) {
        if (i < minLanguage || i > maxLanguage) {
            return "UNKNOWN";
        }
        
        return languages[i - minLanguage].name();
    }
}
