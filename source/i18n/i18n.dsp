# Microsoft Developer Studio Project File - Name="i18n" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=i18n - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "i18n.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "i18n.mak" CFG="i18n - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "i18n - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "i18n - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "i18n - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "i18n - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "i18n - Win32 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /FD /c
# ADD CPP /nologo /G6 /MD /Za /W3 /GX /Zi /O2 /Ob2 /I "..\..\include" /I "..\..\source\common" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /FR /FD /GF /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "../common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 icuuc.lib /nologo /base:"0x4a900000" /dll /machine:I386 /out:"..\..\bin\icuin30.dll" /implib:"..\..\lib\icuin.lib" /libpath:"..\..\lib"
# SUBTRACT LINK32 /pdb:none /debug

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\..\source\common" /D "_WINDOWS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /D "WIN32" /D "_DEBUG" /D "_MBCS" /D "UDATA_MAP" /FR /FD /GF /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "../common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 icuucd.lib /nologo /base:"0x4a900000" /dll /debug /machine:I386 /out:"..\..\bin\icuin30d.dll" /implib:"..\..\lib\icuind.lib" /pdbtype:sept /libpath:"..\..\lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /I "..\..\include" /I "..\..\source\common" /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GF /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "../common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 icuuc.lib /nologo /base:"0x4a900000" /dll /machine:IX86 /out:"..\..\bin\icuin30.dll" /implib:"..\..\lib\icuin.lib" /libpath:"..\..\lib" /machine:IA64
# SUBTRACT LINK32 /debug

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /I "..\..\include" /I "..\..\source\common" /D "_WINDOWS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /D "WIN64" /D "_DEBUG" /D "_MBCS" /D "UDATA_MAP" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GF /GZ /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "../common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 icuucd.lib /nologo /base:"0x4a900000" /dll /incremental:no /debug /machine:IX86 /out:"..\..\bin\icuin30d.dll" /implib:"..\..\lib\icuind.lib" /pdbtype:sept /libpath:"..\..\lib" /machine:IA64

!ENDIF 

# Begin Target

# Name "i18n - Win32 Release"
# Name "i18n - Win32 Debug"
# Name "i18n - Win64 Release"
# Name "i18n - Win64 Debug"
# Begin Group "collation"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\bocsu.c
# End Source File
# Begin Source File

SOURCE=.\bocsu.h
# End Source File
# Begin Source File

SOURCE=.\coleitr.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\coleitr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\coll.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\coll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\search.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\search.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\search.h

"..\..\include\unicode\search.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\search.h

"..\..\include\unicode\search.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\search.h

"..\..\include\unicode\search.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\search.h

"..\..\include\unicode\search.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\sortkey.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\sortkey.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\stsearch.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\stsearch.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\stsearch.h

"..\..\include\unicode\stsearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\stsearch.h

"..\..\include\unicode\stsearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\stsearch.h

"..\..\include\unicode\stsearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\stsearch.h

"..\..\include\unicode\stsearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tblcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\tblcoll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucol.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\ucol.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucol_bld.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol_bld.h
# End Source File
# Begin Source File

SOURCE=.\ucol_cnt.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol_cnt.h
# End Source File
# Begin Source File

SOURCE=.\ucol_elm.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol_elm.h
# End Source File
# Begin Source File

SOURCE=.\ucol_imp.h
# End Source File
# Begin Source File

SOURCE=.\ucol_sit.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol_tok.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol_tok.h
# End Source File
# Begin Source File

SOURCE=.\ucol_wgt.c
# End Source File
# Begin Source File

SOURCE=.\ucol_wgt.h
# End Source File
# Begin Source File

SOURCE=.\ucoleitr.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\ucoleitr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucoleitr.h

"..\..\include\unicode\ucoleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucoleitr.h

"..\..\include\unicode\ucoleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucoleitr.h

"..\..\include\unicode\ucoleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucoleitr.h

"..\..\include\unicode\ucoleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\usearch.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\usearch.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\usearch.h

"..\..\include\unicode\usearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\usearch.h

"..\..\include\unicode\usearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\usearch.h

"..\..\include\unicode\usearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\usearch.h

"..\..\include\unicode\usearch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\usrchimp.h
# End Source File
# End Group
# Begin Group "formatting"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\astro.cpp
# End Source File
# Begin Source File

SOURCE=.\astro.h
# End Source File
# Begin Source File

SOURCE=.\buddhcal.cpp
# End Source File
# Begin Source File

SOURCE=.\buddhcal.h
# End Source File
# Begin Source File

SOURCE=.\calendar.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\calendar.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\chnsecal.cpp
# End Source File
# Begin Source File

SOURCE=.\chnsecal.h
# End Source File
# Begin Source File

SOURCE=.\choicfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\choicfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\curramt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\curramt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\curramt.h

"..\..\include\unicode\curramt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\curramt.h

"..\..\include\unicode\curramt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\curramt.h

"..\..\include\unicode\curramt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\curramt.h

"..\..\include\unicode\curramt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\currfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\currfmt.h
# End Source File
# Begin Source File

SOURCE=.\currunit.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\currunit.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\currunit.h

"..\..\include\unicode\currunit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\currunit.h

"..\..\include\unicode\currunit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\currunit.h

"..\..\include\unicode\currunit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\currunit.h

"..\..\include\unicode\currunit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\datefmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\datefmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dcfmtsym.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\dcfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\decimfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\decimfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\digitlst.cpp
# End Source File
# Begin Source File

SOURCE=.\digitlst.h
# End Source File
# Begin Source File

SOURCE=.\dtfmtsym.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\dtfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\fieldpos.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\fmtable.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\fmtable.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\format.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\format.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\gregocal.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\gregocal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\gregoimp.cpp
# End Source File
# Begin Source File

SOURCE=.\gregoimp.h
# End Source File
# Begin Source File

SOURCE=.\hebrwcal.cpp
# End Source File
# Begin Source File

SOURCE=.\hebrwcal.h
# End Source File
# Begin Source File

SOURCE=.\islamcal.cpp
# End Source File
# Begin Source File

SOURCE=.\islamcal.h
# End Source File
# Begin Source File

SOURCE=.\japancal.cpp
# End Source File
# Begin Source File

SOURCE=.\japancal.h
# End Source File
# Begin Source File

SOURCE=.\measfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\measfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\measfmt.h

"..\..\include\unicode\measfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\measfmt.h

"..\..\include\unicode\measfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\measfmt.h

"..\..\include\unicode\measfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\measfmt.h

"..\..\include\unicode\measfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\measure.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\measure.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\measure.h

"..\..\include\unicode\measure.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\measure.h

"..\..\include\unicode\measure.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\measure.h

"..\..\include\unicode\measure.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\measure.h

"..\..\include\unicode\measure.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\msgfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\msgfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\nfrlist.h
# End Source File
# Begin Source File

SOURCE=.\nfrs.cpp
# End Source File
# Begin Source File

SOURCE=.\nfrs.h
# End Source File
# Begin Source File

SOURCE=.\nfrule.cpp
# End Source File
# Begin Source File

SOURCE=.\nfrule.h
# End Source File
# Begin Source File

SOURCE=.\nfsubs.cpp
# End Source File
# Begin Source File

SOURCE=.\nfsubs.h
# End Source File
# Begin Source File

SOURCE=.\numfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\numfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\olsontz.cpp
# End Source File
# Begin Source File

SOURCE=.\olsontz.h
# End Source File
# Begin Source File

SOURCE=.\rbnf.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\rbnf.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rbnf.h

"..\..\include\unicode\rbnf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rbnf.h

"..\..\include\unicode\rbnf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\rbnf.h

"..\..\include\unicode\rbnf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\rbnf.h

"..\..\include\unicode\rbnf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\simpletz.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\simpletz.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\smpdtfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\smpdtfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\timezone.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\timezone.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucal.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\ucal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucurr.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\ucurr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucurr.h

"..\..\include\unicode\ucurr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucurr.h

"..\..\include\unicode\ucurr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucurr.h

"..\..\include\unicode\ucurr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucurr.h

"..\..\include\unicode\ucurr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucurrimp.h
# End Source File
# Begin Source File

SOURCE=.\udat.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\udat.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umsg.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\umsg.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umsg_imp.h
# End Source File
# Begin Source File

SOURCE=.\unum.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\unum.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "misc"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\i18n.rc
# ADD BASE RSC /l 0x409
# ADD RSC /l 0x409
# End Source File
# Begin Source File

SOURCE=.\ucln_in.c
# End Source File
# Begin Source File

SOURCE=.\ucln_in.h
# End Source File
# End Group
# Begin Group "regex"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\unicode\regex.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\regex.h

"..\..\include\unicode\regex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\regex.h

"..\..\include\unicode\regex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\regex.h

"..\..\include\unicode\regex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\regex.h

"..\..\include\unicode\regex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\regexcmp.cpp
# End Source File
# Begin Source File

SOURCE=.\regexcmp.h
# End Source File
# Begin Source File

SOURCE=.\regexcst.h
# End Source File
# Begin Source File

SOURCE=.\regeximp.h
# End Source File
# Begin Source File

SOURCE=.\regexst.cpp
# End Source File
# Begin Source File

SOURCE=.\regexst.h
# End Source File
# Begin Source File

SOURCE=.\rematch.cpp
# End Source File
# Begin Source File

SOURCE=.\repattrn.cpp
# End Source File
# Begin Source File

SOURCE=.\uregex.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uregex.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uregex.h

"..\..\include\unicode\uregex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uregex.h

"..\..\include\unicode\uregex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uregex.h

"..\..\include\unicode\uregex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uregex.h

"..\..\include\unicode\uregex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "transforms"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\anytrans.cpp
# End Source File
# Begin Source File

SOURCE=.\anytrans.h
# End Source File
# Begin Source File

SOURCE=.\cpdtrans.cpp
# End Source File
# Begin Source File

SOURCE=.\cpdtrans.h
# End Source File
# Begin Source File

SOURCE=.\esctrn.cpp
# End Source File
# Begin Source File

SOURCE=.\esctrn.h
# End Source File
# Begin Source File

SOURCE=.\funcrepl.cpp
# End Source File
# Begin Source File

SOURCE=.\funcrepl.h
# End Source File
# Begin Source File

SOURCE=.\name2uni.cpp
# End Source File
# Begin Source File

SOURCE=.\name2uni.h
# End Source File
# Begin Source File

SOURCE=.\nortrans.cpp
# End Source File
# Begin Source File

SOURCE=.\nortrans.h
# End Source File
# Begin Source File

SOURCE=.\nultrans.cpp
# End Source File
# Begin Source File

SOURCE=.\nultrans.h
# End Source File
# Begin Source File

SOURCE=.\quant.cpp
# End Source File
# Begin Source File

SOURCE=.\quant.h
# End Source File
# Begin Source File

SOURCE=.\rbt.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt.h
# End Source File
# Begin Source File

SOURCE=.\rbt_data.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_data.h
# End Source File
# Begin Source File

SOURCE=.\rbt_pars.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_pars.h
# End Source File
# Begin Source File

SOURCE=.\rbt_rule.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_rule.h
# End Source File
# Begin Source File

SOURCE=.\rbt_set.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_set.h
# End Source File
# Begin Source File

SOURCE=.\remtrans.cpp
# End Source File
# Begin Source File

SOURCE=.\remtrans.h
# End Source File
# Begin Source File

SOURCE=.\strmatch.cpp
# End Source File
# Begin Source File

SOURCE=.\strmatch.h
# End Source File
# Begin Source File

SOURCE=.\strrepl.cpp
# End Source File
# Begin Source File

SOURCE=.\titletrn.cpp
# End Source File
# Begin Source File

SOURCE=.\titletrn.h
# End Source File
# Begin Source File

SOURCE=.\tolowtrn.cpp
# End Source File
# Begin Source File

SOURCE=.\tolowtrn.h
# End Source File
# Begin Source File

SOURCE=.\toupptrn.cpp
# End Source File
# Begin Source File

SOURCE=.\toupptrn.h
# End Source File
# Begin Source File

SOURCE=.\translit.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\translit.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\transreg.cpp
# End Source File
# Begin Source File

SOURCE=.\transreg.h
# End Source File
# Begin Source File

SOURCE=.\tridpars.cpp
# End Source File
# Begin Source File

SOURCE=.\tridpars.h
# End Source File
# Begin Source File

SOURCE=.\unesctrn.cpp
# End Source File
# Begin Source File

SOURCE=.\unesctrn.h
# End Source File
# Begin Source File

SOURCE=.\uni2name.cpp
# End Source File
# Begin Source File

SOURCE=.\uni2name.h
# End Source File
# Begin Source File

SOURCE=.\unicode\unirepl.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unirepl.h

"..\..\include\unicode\unirepl.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unirepl.h

"..\..\include\unicode\unirepl.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unirepl.h

"..\..\include\unicode\unirepl.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unirepl.h

"..\..\include\unicode\unirepl.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utrans.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\utrans.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utrans.h

"..\..\include\unicode\utrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utrans.h

"..\..\include\unicode\utrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utrans.h

"..\..\include\unicode\utrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utrans.h

"..\..\include\unicode\utrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "locale"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\ulocdata.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ulocdata.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ulocdata.h

"..\..\include\unicode\ulocdata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ulocdata.h

"..\..\include\unicode\ulocdata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ulocdata.h

"..\..\include\unicode\ulocdata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ulocdata.h

"..\..\include\unicode\ulocdata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# End Target
# End Project
