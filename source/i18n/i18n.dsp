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
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\..\source\common" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "I18N_EXPORTS" /D "U_I18N_IMPLEMENTATION" /FR /YX /FD /GZ /c
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

SOURCE=.\brkiter.cpp
# End Source File
# Begin Source File

SOURCE=.\calendar.cpp
# End Source File
# Begin Source File

SOURCE=.\chbkdat.cpp
# End Source File
# Begin Source File

SOURCE=.\choicfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\colcache.cpp
# End Source File
# Begin Source File

SOURCE=.\coleitr.cpp
# End Source File
# Begin Source File

SOURCE=.\coll.cpp
# End Source File
# Begin Source File

SOURCE=.\colrules.cpp
# End Source File
# Begin Source File

SOURCE=.\cpdtrans.cpp
# End Source File
# Begin Source File

SOURCE=.\datefmt.cpp
# End Source File
# Begin Source File

SOURCE=.\dcfmtsym.cpp
# End Source File
# Begin Source File

SOURCE=.\decimfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\dtfmtsym.cpp
# End Source File
# Begin Source File

SOURCE=.\fmtable.cpp
# End Source File
# Begin Source File

SOURCE=.\format.cpp
# End Source File
# Begin Source File

SOURCE=.\gregocal.cpp
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\hextouni.cpp
# End Source File
# Begin Source File

SOURCE=.\lnbkdat.cpp
# End Source File
# Begin Source File

SOURCE=.\mergecol.cpp
# End Source File
# Begin Source File

SOURCE=.\msgfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\numfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\ptnentry.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_data.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_pars.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_rule.cpp
# End Source File
# Begin Source File

SOURCE=.\rbt_set.cpp
# End Source File
# Begin Source File

SOURCE=.\simpletz.cpp
# End Source File
# Begin Source File

SOURCE=.\simtxbd.cpp
# End Source File
# Begin Source File

SOURCE=.\smpdtfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\snbkdat.cpp
# End Source File
# Begin Source File

SOURCE=.\sortkey.cpp
# End Source File
# Begin Source File

SOURCE=.\tables.cpp
# End Source File
# Begin Source File

SOURCE=.\tblcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\tcoldata.cpp
# End Source File
# Begin Source File

SOURCE=.\timezone.cpp
# End Source File
# Begin Source File

SOURCE=.\translit.cpp
# End Source File
# Begin Source File

SOURCE=.\txtbdat.cpp
# End Source File
# Begin Source File

SOURCE=.\txtbdry.cpp
# End Source File
# Begin Source File

SOURCE=.\ubrk.cpp
# End Source File
# Begin Source File

SOURCE=.\ucal.cpp
# End Source File
# Begin Source File

SOURCE=.\ucol.cpp
# End Source File
# Begin Source File

SOURCE=.\udat.cpp
# End Source File
# Begin Source File

SOURCE=.\umsg.cpp
# End Source File
# Begin Source File

SOURCE=.\unicdcm.cpp
# End Source File
# Begin Source File

SOURCE=.\unifltlg.cpp
# End Source File
# Begin Source File

SOURCE=.\unirange.cpp
# End Source File
# Begin Source File

SOURCE=.\uniset.cpp
# End Source File
# Begin Source File

SOURCE=.\unitohex.cpp
# End Source File
# Begin Source File

SOURCE=.\unum.cpp
# End Source File
# Begin Source File

SOURCE=.\wdbkdat.cpp
# End Source File
# Begin Source File

SOURCE=.\wdbktbl.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\brkiter.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\brkiter.h

"..\..\include\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               brkiter.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\brkiter.h

"..\..\include\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               brkiter.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\calendar.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\calendar.h

"..\..\include\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               calendar.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\calendar.h

"..\..\include\calendar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               calendar.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\choicfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\choicfmt.h

"..\..\include\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               choicfmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\choicfmt.h

"..\..\include\choicfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               choicfmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colcache.h
# End Source File
# Begin Source File

SOURCE=.\coleitr.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\coleitr.h

"..\..\include\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              coleitr.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\coleitr.h

"..\..\include\coleitr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              coleitr.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\coll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\coll.h

"..\..\include\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              coll.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\coll.h

"..\..\include\coll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              coll.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\colrules.h
# End Source File
# Begin Source File

SOURCE=.\cpdtrans.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\cpdtrans.h

"..\..\include\cpdtrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               cpdtrans.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\cpdtrans.h

"..\..\include\cpdtrans.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               cpdtrans.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\datefmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\datefmt.h

"..\..\include\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               datefmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\datefmt.h

"..\..\include\datefmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               datefmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dcfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\dcfmtsym.h

"..\..\include\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               dcfmtsym.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\dcfmtsym.h

"..\..\include\dcfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               dcfmtsym.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\decimfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\decimfmt.h

"..\..\include\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              decimfmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\decimfmt.h

"..\..\include\decimfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              decimfmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dtfmtsym.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\dtfmtsym.h

"..\..\include\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               dtfmtsym.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\dtfmtsym.h

"..\..\include\dtfmtsym.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               dtfmtsym.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\fieldpos.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\fieldpos.h

"..\..\include\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               fieldpos.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\fieldpos.h

"..\..\include\fieldpos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               fieldpos.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\fmtable.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\fmtable.h

"..\..\include\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              fmtable.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\fmtable.h

"..\..\include\fmtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              fmtable.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\format.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\format.h

"..\..\include\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              format.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\format.h

"..\..\include\format.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              format.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\gregocal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\gregocal.h

"..\..\include\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               gregocal.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\gregocal.h

"..\..\include\gregocal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               gregocal.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\hextouni.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\hextouni.h

"..\..\include\hextouni.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               hextouni.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\hextouni.h

"..\..\include\hextouni.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               hextouni.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\mergecol.h
# End Source File
# Begin Source File

SOURCE=.\msgfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\msgfmt.h

"..\..\include\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               msgfmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\msgfmt.h

"..\..\include\msgfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               msgfmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\numfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\numfmt.h

"..\..\include\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               numfmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\numfmt.h

"..\..\include\numfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               numfmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\parsepos.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\parsepos.h

"..\..\include\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               parsepos.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\parsepos.h

"..\..\include\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               parsepos.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ptnentry.h
# End Source File
# Begin Source File

SOURCE=.\rbbi.h
# End Source File
# Begin Source File

SOURCE=.\rbbi_bld.h
# End Source File
# Begin Source File

SOURCE=.\rbt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\rbt.h

"..\..\include\rbt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               rbt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\rbt.h

"..\..\include\rbt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               rbt.h                ..\..\include

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

SOURCE=.\simpletz.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\simpletz.h

"..\..\include\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               simpletz.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\simpletz.h

"..\..\include\simpletz.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               simpletz.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\simtxbd.h
# End Source File
# Begin Source File

SOURCE=.\smpdtfmt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\smpdtfmt.h

"..\..\include\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               smpdtfmt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\smpdtfmt.h

"..\..\include\smpdtfmt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               smpdtfmt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\sortkey.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\sortkey.h

"..\..\include\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              sortkey.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\sortkey.h

"..\..\include\sortkey.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy              sortkey.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\spclmap.h
# End Source File
# Begin Source File

SOURCE=.\tables.h
# End Source File
# Begin Source File

SOURCE=.\tblcoll.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\tblcoll.h

"..\..\include\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               tblcoll.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\tblcoll.h

"..\..\include\tblcoll.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               tblcoll.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\tcoldata.h
# End Source File
# Begin Source File

SOURCE=.\timezone.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\timezone.h

"..\..\include\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               timezone.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\timezone.h

"..\..\include\timezone.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               timezone.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\translit.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\translit.h

"..\..\include\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               translit.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\translit.h

"..\..\include\translit.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               translit.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\txtbdat.h
# End Source File
# Begin Source File

SOURCE=.\txtbdry.h
# End Source File
# Begin Source File

SOURCE=.\ubrk.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\ubrk.h

"..\..\include\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ubrk.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\ubrk.h

"..\..\include\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ubrk.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucal.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\ucal.h

"..\..\include\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ucal.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\ucal.h

"..\..\include\ucal.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ucal.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucol.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\ucol.h

"..\..\include\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ucol.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\ucol.h

"..\..\include\ucol.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               ucol.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\udat.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\udat.h

"..\..\include\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               udat.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\udat.h

"..\..\include\udat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               udat.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umsg.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\umsg.h

"..\..\include\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               umsg.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\umsg.h

"..\..\include\umsg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               umsg.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicdcm.h
# End Source File
# Begin Source File

SOURCE=.\unifilt.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unifilt.h

"..\..\include\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unifilt.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unifilt.h

"..\..\include\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unifilt.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unifltlg.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unifltlg.h

"..\..\include\unifltlg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unifltlg.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unifltlg.h

"..\..\include\unifltlg.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unifltlg.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unirange.h
# End Source File
# Begin Source File

SOURCE=.\uniset.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\uniset.h

"..\..\include\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               uniset.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\uniset.h

"..\..\include\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               uniset.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unitohex.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unitohex.h

"..\..\include\unitohex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unitohex.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unitohex.h

"..\..\include\unitohex.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unitohex.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unum.h

!IF  "$(CFG)" == "i18n - Win32 Release"

# Begin Custom Build
InputPath=.\unum.h

"..\..\include\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unum.h                ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "i18n - Win32 Debug"

# Begin Custom Build
InputPath=.\unum.h

"..\..\include\unum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy               unum.h                ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\wdbktbl.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
