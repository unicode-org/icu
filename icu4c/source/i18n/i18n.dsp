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
# PROP Output_Dir "..\..\lib\Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /I "..\..\source\common" /I "..\..\include" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 ..\..\lib\release\icuuc.lib /nologo /base:"0x4a900000" /dll /machine:I386 /out:"..\..\bin\Release/icui18n.dll"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MDd /Ze /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\..\source\common" /D "_WINDOWS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /D "WIN32" /D "_DEBUG" /D "_MBCS" /D "UDATA_MAP" /FR /YX /FD /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ..\..\lib\debug\icuuc.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /base:"0x4a900000" /dll /debug /machine:I386 /out:"..\..\bin\Debug/icui18n.dll" /pdbtype:sept

!ENDIF 

# Begin Target

# Name "i18n - Win32 Release"
# Name "i18n - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\brkdict.cpp
# End Source File
# Begin Source File

SOURCE=.\brkiter.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\calendar.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\choicfmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colcache.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\coleitr.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\coll.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colrules.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\cpdtrans.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\datefmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dbbi.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dbbi_tbl.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dcfmtsym.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\decimfmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dtfmtsym.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\fmtable.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\format.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\gregocal.cpp
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\hangjamo.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\hextouni.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\jamohang.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\mergecol.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\msgfmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\nultrans.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\numfmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ptnentry.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbbi.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbbi_tbl.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt_data.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt_pars.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt_rule.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt_set.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\simpletz.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\smpdtfmt.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\sortkey.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tables.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tblcoll.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tcoldata.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\timezone.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\translit.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\txtbdry.cpp
# End Source File
# Begin Source File

SOURCE=.\ubrk.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucal.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucol.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\udat.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umsg.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unifltlg.cpp
# End Source File
# Begin Source File

SOURCE=.\unirange.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uniset.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unitohex.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unum.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /Ze

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utrans.cpp

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# ADD CPP /W3

!ENDIF 

# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\brkdict.h
# End Source File
# Begin Source File

SOURCE=.\unicode\brkiter.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\brkiter.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\brkiter.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\calendar.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\calendar.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\calendar.h

"..\..\include\unicode\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\calendar.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\choicfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\choicfmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\choicfmt.h

"..\..\include\unicode\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\choicfmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colcache.h
# End Source File
# Begin Source File

SOURCE=.\unicode\coleitr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\coleitr.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\coleitr.h

"..\..\include\unicode\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\coleitr.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\coll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\coll.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\coll.h

"..\..\include\unicode\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\coll.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colrules.h
# End Source File
# Begin Source File

SOURCE=.\unicode\cpdtrans.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\cpdtrans.h

"..\..\include\unicode\cpdtrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\cpdtrans.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\cpdtrans.h

"..\..\include\unicode\cpdtrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\cpdtrans.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\datefmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\datefmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\datefmt.h

"..\..\include\unicode\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\datefmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\dbbi.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dbbi.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dbbi.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dbbi_tbl.h
# End Source File
# Begin Source File

SOURCE=.\unicode\dcfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dcfmtsym.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dcfmtsym.h

"..\..\include\unicode\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dcfmtsym.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\decimfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\decimfmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\decimfmt.h

"..\..\include\unicode\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\decimfmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\dtfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dtfmtsym.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dtfmtsym.h

"..\..\include\unicode\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\dtfmtsym.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\fieldpos.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\fieldpos.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\fieldpos.h

"..\..\include\unicode\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\fieldpos.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\fmtable.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\fmtable.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\fmtable.h

"..\..\include\unicode\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\fmtable.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\format.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\format.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\format.h

"..\..\include\unicode\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\format.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\gregocal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\gregocal.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\gregocal.h

"..\..\include\unicode\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\gregocal.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\hangjamo.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\hangjamo.h

"..\..\include\unicode\hangjamo.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\hangjamo.h    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\hangjamo.h

"..\..\include\unicode\hangjamo.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\hangjamo.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\hextouni.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\hextouni.h

"..\..\include\unicode\hextouni.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\hextouni.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\hextouni.h

"..\..\include\unicode\hextouni.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\hextouni.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\jamohang.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\jamohang.h

"..\..\include\unicode\jamohang.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\jamohang.h    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\jamohang.h

"..\..\include\unicode\jamohang.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\jamohang.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\mergecol.h
# End Source File
# Begin Source File

SOURCE=.\unicode\msgfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\msgfmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\msgfmt.h

"..\..\include\unicode\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\msgfmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\nultrans.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\nultrans.h

"..\..\include\unicode\nultrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\nultrans.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\nultrans.h

"..\..\include\unicode\nultrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\nultrans.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\numfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\numfmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\numfmt.h

"..\..\include\unicode\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\numfmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\parseerr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\parseerr.h    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy   unicode\parseerr.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\parsepos.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\parsepos.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\parsepos.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ptnentry.h
# End Source File
# Begin Source File

SOURCE=.\unicode\rbbi.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\rbbi.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\rbbi.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbbi_tbl.h
# End Source File
# Begin Source File

SOURCE=.\unicode\rbt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rbt.h

"..\..\include\unicode\rbt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\rbt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rbt.h

"..\..\include\unicode\rbt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\rbt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbt_data.h
# End Source File
# Begin Source File

SOURCE=.\rbt_pars.h
# End Source File
# Begin Source File

SOURCE=.\rbt_rule.h
# End Source File
# Begin Source File

SOURCE=.\rbt_set.h
# End Source File
# Begin Source File

SOURCE=.\unicode\simpletz.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\simpletz.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\simpletz.h

"..\..\include\unicode\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\simpletz.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\smpdtfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\smpdtfmt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\smpdtfmt.h

"..\..\include\unicode\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\smpdtfmt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\sortkey.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\sortkey.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\sortkey.h

"..\..\include\unicode\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unicode\sortkey.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tables.h
# End Source File
# Begin Source File

SOURCE=.\unicode\tblcoll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\tblcoll.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\tblcoll.h

"..\..\include\unicode\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\tblcoll.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tcoldata.h
# End Source File
# Begin Source File

SOURCE=.\unicode\timezone.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\timezone.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\timezone.h

"..\..\include\unicode\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\timezone.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\translit.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\translit.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\translit.h

"..\..\include\unicode\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\translit.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\txtbdry.h
# End Source File
# Begin Source File

SOURCE=.\unicode\ubrk.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ubrk.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ubrk.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ucal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ucal.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucal.h

"..\..\include\unicode\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ucal.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ucol.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ucol.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucol.h

"..\..\include\unicode\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\ucol.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\udat.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\udat.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udat.h

"..\..\include\unicode\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\udat.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\umsg.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\umsg.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umsg.h

"..\..\include\unicode\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\umsg.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unifilt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unifilt.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unifilt.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unifltlg.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unifltlg.h

"..\..\include\unicode\unifltlg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unifltlg.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unifltlg.h

"..\..\include\unicode\unifltlg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unifltlg.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unirange.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uniset.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\uniset.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\uniset.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unitohex.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unitohex.h

"..\..\include\unicode\unitohex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unitohex.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unitohex.h

"..\..\include\unicode\unitohex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unitohex.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unum.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unum.h                ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unum.h

"..\..\include\unicode\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                unicode\unum.h                ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\urep.h

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\urep.h

"..\..\include\unicode\urep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    unicode\urep.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utrans.h

!IF  "$(CFG)" == "i18n - Win32 Release"

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utrans.h

"..\..\include\unicode\utrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    unicode\utrans.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
