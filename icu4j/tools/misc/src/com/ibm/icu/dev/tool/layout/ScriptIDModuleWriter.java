// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.Date;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.VersionInfo;

public class ScriptIDModuleWriter extends ScriptModuleWriter
{
    private static final boolean WITH_STABLE_TAGS = false;

    public ScriptIDModuleWriter(ScriptData scriptData, LanguageData languageData)
    {
        super(scriptData, languageData);
    }

    public void writeScriptHeader(String fileName)
    {
        int minScript = scriptData.getMinValue();
        int maxScript = scriptData.getMaxValue();
        
        openFile(fileName);
        writeHeader("__LESCRIPTS_H", includeFiles, scriptBrief);
        
        totalScript = maxScript;
        what = "script";
        readFile(ScriptAndLanguages, what);
        String checkICUVersion, previousVersion;
        int arrayListIndex=0, numberOfScripts;
        boolean initialheader = false;
        boolean newScripts = false;
        
        if(totalScript>previousTotalScripts){
            newScripts = true;
        }
        //Processing old scripts
        MessageFormat format = new MessageFormat(scriptPreamble); 
        for(int script=minScript;script<=previousTotalScripts;){
             checkICUVersion = (String)scriptVersionNumber.get(arrayListIndex);
             checkICUVersion = checkICUVersion.substring(checkICUVersion.indexOf("_")+1);
             previousVersion = checkICUVersion.substring(0, checkICUVersion.indexOf("="));
             numberOfScripts = Integer.parseInt(checkICUVersion.substring(checkICUVersion.indexOf("=")+1));
             
             Object args[] = { previousVersion };
             //Check for the initial header. It should be written only one time
             if(!initialheader){
                 output.println(format.format(args));
                 initialheader = true;
             }else if(WITH_STABLE_TAGS) {
                 // Unicode API constants are "born stable".
                 format = new MessageFormat(scriptPreambleStable); 
                 output.println(format.format(args));
             }
             
             for(int i=0;i<numberOfScripts;i++){
                 output.print("    ");
                 output.print(scriptData.getTagLabel(script));
                 output.print("ScriptCode = ");
                 
                 if (script < 10) {
                     output.print(" ");
                 }
                 
                 output.print(script);
                 output.println(",");
                 if(script == UScript.INHERITED) {
                     output.println("    qaaiScriptCode = zinhScriptCode,  /* manually added alias, for API stability */");
                 }
                 script++;
             }
             arrayListIndex++;
        }
        
        if(newScripts){//Processing newly added scripts
            if(WITH_STABLE_TAGS) {
                format = new MessageFormat(scriptPreambleStable);
                Object args[] = { VersionInfo.ICU_VERSION };
                output.println(format.format(args));
            }

            for (int script = previousTotalScripts+1; script <= totalScript; script += 1) {
                output.print("    ");
                output.print(scriptData.getTagLabel(script));
                output.print("ScriptCode = ");
                
                if (script < 10) {
                    output.print(" ");
                }
                
                output.print(script);
                output.println(",");
            }
        }
        output.println();
        output.print("    scriptCodeCount = ");
        output.println(maxScript - minScript + 1);
        output.println(postamble);
        
        writeTrailer();
        closeFile();
        
    }
    
    public void writeLanguageHeader(String fileName)
    {
        int minLanguage = languageData.getMinValue();
        int maxLanguage = languageData.getMaxValue();
        
        openFile(fileName);
        writeHeader("__LELANGUAGES_H", includeFiles, languageBrief);

        totalLanguage = maxLanguage;
        what = "languages";
        readFile(ScriptAndLanguages, what);
        String checkICUVersion, previousVersion;
        int arrayListIndex=0, numberOfLanguages;
        boolean initialheader = false;
        boolean newLanguage = false;
        
        if(totalLanguage>previousTotalLanguages){
            newLanguage = true;
        }
        //Processing old languages
        MessageFormat format = new MessageFormat(languagePreamble); 
        for(int language=minLanguage;language<=previousTotalLanguages;){
             checkICUVersion = (String)languageVersionNumber.get(arrayListIndex);
             checkICUVersion = checkICUVersion.substring(checkICUVersion.indexOf("_")+1);
             previousVersion = checkICUVersion.substring(0, checkICUVersion.indexOf("="));
             numberOfLanguages = Integer.parseInt(checkICUVersion.substring(checkICUVersion.indexOf("=")+1));
             
             Object args[] = { previousVersion };
            
             //Check for the initial header. It should be written only one time
             if(!initialheader){
                 output.println(format.format(args));
                 initialheader = true;
             } else if(WITH_STABLE_TAGS) {
                 // Unicode API constants are "born stable".
                 format = new MessageFormat(languagePreambleStable); 
                 output.println(format.format(args));
             }
             
             for(int i=0;i<numberOfLanguages;i++){
                 output.print("    ");
                 output.print(languageData.getTagLabel(language).toLowerCase());
                 output.print("LanguageCode = ");
                 
                 if (language < 10) {
                     output.print(" ");
                 }
                 
                 output.print(language);
                 output.println(",");
                 language++;
             }
             arrayListIndex++;
        }
        if(newLanguage){
            //Processing newly added languages
            if(WITH_STABLE_TAGS) {
                format = new MessageFormat(languagePreambleStable); 
                Object args[] = { VersionInfo.ICU_VERSION };
                output.println(format.format(args));
            }

            for (int language = previousTotalLanguages+1; language <= totalLanguage; language += 1) {
                output.print("    ");
                output.print(languageData.getTagLabel(language).toLowerCase());
                output.print("ScriptCode = ");
                
                if (language < 10) {
                    output.print(" ");
                }
                
                output.print(language);
                output.println(",");
            }  
        }
        output.println();
        output.print("    languageCodeCount = ");
        output.println(maxLanguage - minLanguage + 1);
        
        output.println(postamble);
        
        writeTrailer();
        closeFile();
    }
    
    private static final String[] includeFiles = {"LETypes.h"};
    
    private static final String scriptPreamble = 
    "/**\n" +
    " * Constants for Unicode script values, generated using\n" +
    " * ICU4J''s <code>UScript</code> class.\n" +
    " *\n" +
    " * @deprecated ICU 54. See '{@link icu::LayoutEngine}'\n" + // was " * @stable ICU {0}\n"
    " */\n" +
    "\n" +
    "enum ScriptCodes '{'";
    
    private static final String scriptBrief =
    "/**\n" +
    " * \\file\n" + 
    " * \\brief C++ API: Constants for Unicode script values\n" +
    " */\n" +
    "\n";

    
    private static final String languagePreamble = 
    "/**\n" +
    " * A provisional list of language codes. For now,\n" +
    " * this is just a list of languages which the LayoutEngine\n" +
    " * supports.\n" +
    " *\n" +
    " * @deprecated ICU 54. See '{@link icu::LayoutEngine}'\n" + // was " * @stable ICU {0}\n"
    " */\n" +
    "\n" +
    "enum LanguageCodes '{'";
    
    private static final String languageBrief =
        "/**\n" +
        " * \\file\n" + 
        " * \\brief C++ API: List of language codes for LayoutEngine\n" +
        " */\n" +
        "\n";

    private static final String postamble =
    "};\n";
    
    private static final String ScriptAndLanguages = "ScriptAndLanguages";
    private static String ICUVersion = "ICU Version="+VersionInfo.ICU_VERSION.getMajor()+"."+VersionInfo.ICU_VERSION.getMinor();
    private static int totalScript;
    private static int totalLanguage;
    private static String what;
    
    public void updateScriptAndLanguages(){
        openScriptAndLanguages(ScriptAndLanguages);
        MessageFormat format = new MessageFormat(moduleHeader);
        Object args[] = {new Date(System.currentTimeMillis())};

        System.out.print("Updating file "+ScriptAndLanguages);
        
        updateFile.print(format.format(args));
        updateFile.println(ICUVersion);
        updateFile.println("Total Script="+totalScript);
        updateFile.println("Total Language="+totalLanguage);
        updateFile.println("Scripts={");
        for(int i=0;i<scriptVersionNumber.size();i++){
            updateFile.println(scriptVersionNumber.get(i));
        }
        if(totalScript>previousTotalScripts){
            updateFile.println("         ICU_"+VersionInfo.ICU_VERSION.getMajor()+"."+VersionInfo.ICU_VERSION.getMinor()+"="+(totalScript-previousTotalScripts));
            
        }
        updateFile.println("        }");
        updateFile.println("Languages={");
        for(int i=0;i<languageVersionNumber.size();i++){
            updateFile.println(languageVersionNumber.get(i));
        }
        if(totalLanguage>previousTotalLanguages){
            updateFile.println("           ICU_"+VersionInfo.ICU_VERSION.getMajor()+"."+VersionInfo.ICU_VERSION.getMinor()+"="+(totalLanguage-previousTotalLanguages));
        }
        updateFile.println("          }");
        scriptVersionNumber.clear();
        languageVersionNumber.clear();
        updateFile.close();
        
        System.out.println("Done");
    }

    private static final String scriptPreambleStable = 
        "/**\n" +
        " * @stable ICU {0}\n" +
        " */\n";// +

    private static final String languagePreambleStable = 
        "/**\n" +
        " * @stable ICU {0}\n" +
        " */\n";// +
}
