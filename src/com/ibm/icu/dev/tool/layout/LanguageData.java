/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Apr 4, 2003
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
public class LanguageData extends TagValueData
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
        
        public String label()
        {
            return TagUtilities.tagLabel(tag);
        }
        
        public String makeTag()
        {
            return TagUtilities.makeTag(tag);
        }
    }
        
    /*
     * This is temporary data until there is some resolution between
     * the OpenType language system tags and the ISO and RFC standards...
     */
    private Record[] languages =
    {
        new Record("",    "null"),
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
    private int maxLanguage = minLanguage + languages.length - 1;
    
    public int getMinValue()
    {
        return minLanguage;
    }
    
    public int getMaxValue()
    {
        return maxLanguage;
    }
    
    public String getTag(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].tag();
    }
    
    public String getTagLabel(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].label();
    }
    
    public String makeTag(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].makeTag();
    }
    
    public String getName(int value) {
        if (value < minLanguage || value > maxLanguage) {
            return "(UNKNOWN)";
        }
        
        return languages[value - minLanguage].name();
    }
}
