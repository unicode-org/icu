# Microsoft Developer Studio Project File - Name="layout" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=layout - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "layout.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "layout.mak" CFG="layout - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "layout - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layout - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layout - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layout - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "layout - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /FD /c
# ADD CPP /nologo /G6 /MD /Za /W3 /GX /O2 /Ob2 /I "..\..\include" /I "..\common" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 ..\..\lib\icuuc.lib /nologo /dll /machine:I386 /out:"..\..\bin\icule30.dll" /implib:"..\..\lib\icule.lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "layout___Win32_Debug"
# PROP BASE Intermediate_Dir "layout___Win32_Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\common" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /FR /FD /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ..\..\lib\icuucd.lib /nologo /dll /debug /machine:I386 /out:"..\..\bin\icule30d.dll" /implib:"..\..\lib\iculed.lib" /pdbtype:sept
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /I "..\..\include" /I "..\common" /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 ..\..\lib\icuuc.lib /nologo /dll /machine:IX86 /out:"..\..\bin\icule30.dll" /implib:"..\..\lib\icule.lib" /machine:IA64

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "layout___Win64_Debug"
# PROP BASE Intermediate_Dir "layout___Win64_Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /I "..\..\include" /I "..\common" /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GZ /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 ..\..\lib\icuucd.lib /nologo /dll /incremental:no /debug /machine:IX86 /out:"..\..\bin\icule30d.dll" /implib:"..\..\lib\iculed.lib" /pdbtype:sept /machine:IA64

!ENDIF 

# Begin Target

# Name "layout - Win32 Release"
# Name "layout - Win32 Debug"
# Name "layout - Win64 Release"
# Name "layout - Win64 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\AlternateSubstSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\AnchorTables.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicLigatureData.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\CanonData.cpp
# End Source File
# Begin Source File

SOURCE=.\ClassDefinitionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstProc.cpp
# End Source File
# Begin Source File

SOURCE=.\ContextualSubstSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\CoverageTables.cpp
# End Source File
# Begin Source File

SOURCE=.\CursiveAttachmentSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\DeviceTables.cpp
# End Source File
# Begin Source File

SOURCE=.\ExtensionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\Features.cpp
# End Source File
# Begin Source File

SOURCE=.\GDEFMarkFilter.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphDefinitionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphIterator.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphLookupTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphPosnLookupProc.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstLookupProc.cpp
# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\HanLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\HebrewLigatureData.cpp
# End Source File
# Begin Source File

SOURCE=.\HebrewShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicClassTables.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangementProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicReordering.cpp
# End Source File
# Begin Source File

SOURCE=.\LayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\LEFontInstance.cpp
# End Source File
# Begin Source File

SOURCE=.\LEGlyphStorage.cpp
# End Source File
# Begin Source File

SOURCE=.\LEInsertionList.cpp
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstProc.cpp
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\loengine.cpp
# End Source File
# Begin Source File

SOURCE=.\LookupProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\Lookups.cpp
# End Source File
# Begin Source File

SOURCE=.\LookupTables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkArrays.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToBasePosnSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToLigaturePosnSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToMarkPosnSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MorphTables.cpp
# End Source File
# Begin Source File

SOURCE=.\MPreFixups.cpp
# End Source File
# Begin Source File

SOURCE=.\MultipleSubstSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubstProc.cpp
# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\OpenTypeUtilities.cpp
# End Source File
# Begin Source File

SOURCE=.\PairPositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguage.cpp
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguageTags.cpp
# End Source File
# Begin Source File

SOURCE=.\SegmentArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SegmentSingleProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SimpleArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SinglePositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\SingleSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\SingleTableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\StateTableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SubstitutionLookups.cpp
# End Source File
# Begin Source File

SOURCE=.\SubtableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiStateTables.cpp
# End Source File
# Begin Source File

SOURCE=.\TrimmedArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\ValueRecords.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\AlternateSubstSubtables.h
# End Source File
# Begin Source File

SOURCE=.\AnchorTables.h
# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\ArabicShaping.h
# End Source File
# Begin Source File

SOURCE=.\AttachmentPosnSubtables.h
# End Source File
# Begin Source File

SOURCE=.\CanonShaping.h
# End Source File
# Begin Source File

SOURCE=.\CharSubstitutionFilter.h
# End Source File
# Begin Source File

SOURCE=.\ClassDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphInsertion.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstProc.h
# End Source File
# Begin Source File

SOURCE=.\ContextualSubstSubtables.h
# End Source File
# Begin Source File

SOURCE=.\CoverageTables.h
# End Source File
# Begin Source File

SOURCE=.\CursiveAttachmentSubtables.h
# End Source File
# Begin Source File

SOURCE=.\DefaultCharMapper.h
# End Source File
# Begin Source File

SOURCE=.\DeviceTables.h
# End Source File
# Begin Source File

SOURCE=.\ExtensionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\Features.h
# End Source File
# Begin Source File

SOURCE=.\GDEFMarkFilter.h
# End Source File
# Begin Source File

SOURCE=.\GlyphDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphIterator.h
# End Source File
# Begin Source File

SOURCE=.\GlyphLookupTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPositionAdjustments.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPosnLookupProc.h
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstLookupProc.h
# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\HanLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\HebrewShaping.h
# End Source File
# Begin Source File

SOURCE=.\HindiFeatureTags.h
# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangement.h
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangementProcessor.h
# End Source File
# Begin Source File

SOURCE=.\IndicReordering.h
# End Source File
# Begin Source File

SOURCE=.\LayoutEngine.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LayoutTables.h
# End Source File
# Begin Source File

SOURCE=.\LEFontInstance.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEGlyphFilter.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEGlyphStorage.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEGlyphStorage.h

"..\..\include\layout\LEGlyphStorage.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEGlyphStorage.h

"..\..\include\layout\LEGlyphStorage.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LEGlyphStorage.h

"..\..\include\layout\LEGlyphStorage.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LEGlyphStorage.h

"..\..\include\layout\LEGlyphStorage.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEInsertionList.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEInsertionList.h

"..\..\include\layout\LEInsertionList.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEInsertionList.h

"..\..\include\layout\LEInsertionList.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LEInsertionList.h

"..\..\include\layout\LEInsertionList.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LEInsertionList.h

"..\..\include\layout\LEInsertionList.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LELanguages.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LELanguages.h

"..\..\include\layout\LELanguages.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LELanguages.h

"..\..\include\layout\LELanguages.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LELanguages.h

"..\..\include\layout\LELanguages.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LELanguages.h

"..\..\include\layout\LELanguages.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEScripts.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LESwaps.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LETypes.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LigatureSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstProc.h
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstSubtables.h
# End Source File
# Begin Source File

SOURCE=.\unicode\loengine.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\loengine.h

"..\..\include\unicode\loengine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\loengine.h

"..\..\include\unicode\loengine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\loengine.h

"..\..\include\unicode\loengine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\loengine.h

"..\..\include\unicode\loengine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\Lookups.h
# End Source File
# Begin Source File

SOURCE=.\LookupTables.h
# End Source File
# Begin Source File

SOURCE=.\MarkArrays.h
# End Source File
# Begin Source File

SOURCE=.\MarkToBasePosnSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MarkToLigaturePosnSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MarkToMarkPosnSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MorphStateTables.h
# End Source File
# Begin Source File

SOURCE=.\MorphTables.h
# End Source File
# Begin Source File

SOURCE=.\MPreFixups.h
# End Source File
# Begin Source File

SOURCE=.\MultipleSubstSubtables.h
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubst.h
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubstProc.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeTables.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeUtilities.h
# End Source File
# Begin Source File

SOURCE=.\PairPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguage.h
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguageTags.h
# End Source File
# Begin Source File

SOURCE=.\SegmentArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SegmentSingleProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SimpleArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SinglePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\SingleSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\SingleTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\StateTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\StateTables.h
# End Source File
# Begin Source File

SOURCE=.\SubstitutionLookups.h
# End Source File
# Begin Source File

SOURCE=.\SubtableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.h
# End Source File
# Begin Source File

SOURCE=.\TrimmedArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ValueRecords.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=.\layout.rc
# End Source File
# End Group
# End Target
# End Project
