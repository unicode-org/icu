// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


public class ScriptModuleWriter extends ModuleWriter
{
    public ScriptModuleWriter(ScriptData theScriptData, LanguageData theLanguageData)
    {
        super();
        
        scriptData = theScriptData;
        languageData = theLanguageData;
    }

    protected ScriptData scriptData;
    protected LanguageData languageData;
}
