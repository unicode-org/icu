# Microsoft Developer Studio Project File - Name="common" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=common - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "common.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "common.mak" CFG="common - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "common - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "common - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "common - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "common - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "common - Win32 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /FD /c
# ADD CPP /nologo /G6 /MD /Za /W3 /GX /Zi /O2 /Ob2 /D "NDEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /D "UDATA_STATIC_LIB" /FR /FD /GF /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 icudata.lib kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /machine:I386 /out:"..\..\bin\icuuc30.dll" /implib:"..\..\lib\icuuc.lib" /libpath:"..\..\lib"
# SUBTRACT LINK32 /pdb:none /debug

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

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
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /Za /W3 /Gm /GX /ZI /Od /D "_DEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /D "UDATA_STATIC_LIB" /D "RBBI_DEBUG" /FR /FD /GF /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 icudata.lib kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /debug /machine:I386 /out:"..\..\bin\icuuc30d.dll" /implib:"..\..\lib\icuucd.lib" /pdbtype:sept /libpath:"..\..\lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "common - Win64 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /D "NDEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /D "UDATA_STATIC_LIB" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /GF /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 icudata.lib kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /machine:IX86 /out:"..\..\bin\icuuc30.dll" /implib:"..\..\lib\icuuc.lib" /libpath:"..\..\lib" /machine:IA64
# SUBTRACT LINK32 /debug

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

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
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /D "_DEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /D "UDATA_STATIC_LIB" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GF /GZ /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 icudata.lib kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /incremental:no /debug /machine:IX86 /out:"..\..\bin\icuuc30d.dll" /implib:"..\..\lib\icuucd.lib" /pdbtype:sept /libpath:"..\..\lib" /machine:IA64

!ENDIF 

# Begin Target

# Name "common - Win32 Release"
# Name "common - Win32 Debug"
# Name "common - Win64 Release"
# Name "common - Win64 Debug"
# Begin Group "bidi"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\ubidi.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ubidi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ubidiimp.h
# End Source File
# Begin Source File

SOURCE=.\ubidiln.c
# End Source File
# Begin Source File

SOURCE=.\ubidiwrt.c
# End Source File
# Begin Source File

SOURCE=.\ushape.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ushape.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ushape.h

"..\..\include\unicode\ushape.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ushape.h

"..\..\include\unicode\ushape.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ushape.h

"..\..\include\unicode\ushape.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ushape.h

"..\..\include\unicode\ushape.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "break iteration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\brkdict.cpp
# End Source File
# Begin Source File

SOURCE=.\brkdict.h
# End Source File
# Begin Source File

SOURCE=.\brkiter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\brkiter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\brkiter.h

"..\..\include\unicode\brkiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dbbi.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\dbbi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\dbbi.h

"..\..\include\unicode\dbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\dbbi_tbl.cpp
# End Source File
# Begin Source File

SOURCE=.\dbbi_tbl.h
# End Source File
# Begin Source File

SOURCE=.\rbbi.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\rbbi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\rbbi.h

"..\..\include\unicode\rbbi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbbidata.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbidata.h
# End Source File
# Begin Source File

SOURCE=.\rbbinode.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbinode.h
# End Source File
# Begin Source File

SOURCE=.\rbbirb.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbirb.h
# End Source File
# Begin Source File

SOURCE=.\rbbirpt.h
# End Source File
# Begin Source File

SOURCE=.\rbbiscan.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbiscan.h
# End Source File
# Begin Source File

SOURCE=.\rbbisetb.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbisetb.h
# End Source File
# Begin Source File

SOURCE=.\rbbistbl.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbitblb.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbitblb.h
# End Source File
# Begin Source File

SOURCE=.\ubrk.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\ubrk.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ubrk.h

"..\..\include\unicode\ubrk.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "collation"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\ucol_swp.c
# ADD CPP /I "..\i18n"
# End Source File
# Begin Source File

SOURCE=.\ucol_swp.h
# End Source File
# End Group
# Begin Group "collections"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\hash.h
# End Source File
# Begin Source File

SOURCE=.\unicode\strenum.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\strenum.h

"..\..\include\unicode\strenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\strenum.h

"..\..\include\unicode\strenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\strenum.h

"..\..\include\unicode\strenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\strenum.h

"..\..\include\unicode\strenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uarrsort.c
# End Source File
# Begin Source File

SOURCE=.\uarrsort.h
# End Source File
# Begin Source File

SOURCE=.\ucmp8.c
# End Source File
# Begin Source File

SOURCE=.\ucmp8.h
# End Source File
# Begin Source File

SOURCE=.\uenum.c
# End Source File
# Begin Source File

SOURCE=.\unicode\uenum.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uenum.h

"..\..\include\unicode\uenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uenum.h

"..\..\include\unicode\uenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uenum.h

"..\..\include\unicode\uenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uenum.h

"..\..\include\unicode\uenum.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uenumimp.h
# End Source File
# Begin Source File

SOURCE=.\uhash.c
# End Source File
# Begin Source File

SOURCE=.\uhash.h
# End Source File
# Begin Source File

SOURCE=.\uhash_us.cpp
# End Source File
# Begin Source File

SOURCE=.\ustack.cpp
# End Source File
# Begin Source File

SOURCE=.\ustrenum.cpp
# End Source File
# Begin Source File

SOURCE=.\ustrenum.h
# End Source File
# Begin Source File

SOURCE=.\utrie.c
# End Source File
# Begin Source File

SOURCE=.\utrie.h
# End Source File
# Begin Source File

SOURCE=.\uvector.cpp
# End Source File
# Begin Source File

SOURCE=.\uvector.h
# End Source File
# Begin Source File

SOURCE=.\uvectr32.cpp
# End Source File
# Begin Source File

SOURCE=.\uvectr32.h
# End Source File
# End Group
# Begin Group "configuration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\common.rc
# End Source File
# Begin Source File

SOURCE=.\cpputils.h
# End Source File
# Begin Source File

SOURCE=.\filestrm.c
# End Source File
# Begin Source File

SOURCE=.\filestrm.h
# End Source File
# Begin Source File

SOURCE=.\locmap.c
# End Source File
# Begin Source File

SOURCE=.\locmap.h
# End Source File
# Begin Source File

SOURCE=.\mutex.h
# End Source File
# Begin Source File

SOURCE=.\putil.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\unicode\putil.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\pwin32.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uassert.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uconfig.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uconfig.h

"..\..\include\unicode\uconfig.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uconfig.h

"..\..\include\unicode\uconfig.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uconfig.h

"..\..\include\unicode\uconfig.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uconfig.h

"..\..\include\unicode\uconfig.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\udeprctd.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udeprctd.h

"..\..\include\unicode\udeprctd.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udeprctd.h

"..\..\include\unicode\udeprctd.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\udeprctd.h

"..\..\include\unicode\udeprctd.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\udeprctd.h

"..\..\include\unicode\udeprctd.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\udraft.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udraft.h

"..\..\include\unicode\udraft.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udraft.h

"..\..\include\unicode\udraft.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\udraft.h

"..\..\include\unicode\udraft.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\udraft.h

"..\..\include\unicode\udraft.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\umachine.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umemstrm.c
# End Source File
# Begin Source File

SOURCE=.\umemstrm.h
# End Source File
# Begin Source File

SOURCE=.\umutex.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\umutex.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uobslete.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uobslete.h

"..\..\include\unicode\uobslete.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uobslete.h

"..\..\include\unicode\uobslete.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uobslete.h

"..\..\include\unicode\uobslete.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uobslete.h

"..\..\include\unicode\uobslete.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\urename.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\urename.h

"..\..\include\unicode\urename.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\urename.h

"..\..\include\unicode\urename.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\urename.h

"..\..\include\unicode\urename.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\urename.h

"..\..\include\unicode\urename.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utrace.c
# End Source File
# Begin Source File

SOURCE=.\unicode\utrace.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utrace.h

"..\..\include\unicode\utrace.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utrace.h

"..\..\include\unicode\utrace.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utrace.h

"..\..\include\unicode\utrace.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utrace.h

"..\..\include\unicode\utrace.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utracimp.h
# End Source File
# Begin Source File

SOURCE=.\unicode\utypes.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\uversion.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uversion.h

"..\..\include\unicode\uversion.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uversion.h

"..\..\include\unicode\uversion.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uversion.h

"..\..\include\unicode\uversion.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uversion.h

"..\..\include\unicode\uversion.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "conversion"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\ucnv.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ucnv.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv2022.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_bld.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_bld.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_cb.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ucnv_cb.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_cb.h

"..\..\include\unicode\ucnv_cb.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_cb.h

"..\..\include\unicode\ucnv_cb.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_cb.h

"..\..\include\unicode\ucnv_cb.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_cb.h

"..\..\include\unicode\ucnv_cb.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_cnv.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_cnv.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_err.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ucnv_err.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_ext.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_ext.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_imp.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_io.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_io.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_lmb.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_u16.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_u32.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_u7.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_u8.c
# End Source File
# Begin Source File

SOURCE=.\ucnvbocu.c
# End Source File
# Begin Source File

SOURCE=.\ucnvhz.c
# End Source File
# Begin Source File

SOURCE=.\ucnvisci.c
# End Source File
# Begin Source File

SOURCE=.\ucnvlat1.c
# End Source File
# Begin Source File

SOURCE=.\ucnvmbcs.c
# End Source File
# Begin Source File

SOURCE=.\ucnvmbcs.h
# End Source File
# Begin Source File

SOURCE=.\ucnvscsu.c
# End Source File
# End Group
# Begin Group "data & memory"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\cmemory.c
# End Source File
# Begin Source File

SOURCE=.\cmemory.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uclean.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uclean.h

"..\..\include\unicode\uclean.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uclean.h

"..\..\include\unicode\uclean.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uclean.h

"..\..\include\unicode\uclean.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uclean.h

"..\..\include\unicode\uclean.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucln.h
# End Source File
# Begin Source File

SOURCE=.\ucln_cmn.c
# End Source File
# Begin Source File

SOURCE=.\ucln_cmn.h
# End Source File
# Begin Source File

SOURCE=.\ucmndata.c
# End Source File
# Begin Source File

SOURCE=.\ucmndata.h
# End Source File
# Begin Source File

SOURCE=.\udata.c
# ADD CPP /Za
# End Source File
# Begin Source File

SOURCE=.\unicode\udata.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\udatamem.c
# End Source File
# Begin Source File

SOURCE=.\udatamem.h
# End Source File
# Begin Source File

SOURCE=.\udataswp.c
# End Source File
# Begin Source File

SOURCE=.\udataswp.h
# End Source File
# Begin Source File

SOURCE=.\umapfile.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\umapfile.h
# End Source File
# Begin Source File

SOURCE=.\uobject.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uobject.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uobject.h

"..\..\include\unicode\uobject.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uobject.h

"..\..\include\unicode\uobject.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uobject.h

"..\..\include\unicode\uobject.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uobject.h

"..\..\include\unicode\uobject.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "formatting"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\unicode\parseerr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\parseerr.h

"..\..\include\unicode\parseerr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\parsepos.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\parsepos.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\parsepos.h

"..\..\include\unicode\parsepos.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\umisc.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\umisc.h
InputName=umisc

"..\..\include\unicode\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umisc.h
InputName=umisc

"..\..\include\unicode\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\umisc.h
InputName=umisc

"..\..\include\unicode\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\umisc.h
InputName=umisc

"..\..\include\unicode\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ustrfmt.c
# End Source File
# Begin Source File

SOURCE=.\ustrfmt.h
# End Source File
# Begin Source File

SOURCE=.\util.cpp
# End Source File
# Begin Source File

SOURCE=.\util.h
# End Source File
# End Group
# Begin Group "idna"

# PROP Default_Filter "*.c,*.h"
# Begin Source File

SOURCE=.\punycode.c
# End Source File
# Begin Source File

SOURCE=.\punycode.h
# End Source File
# Begin Source File

SOURCE=.\uidna.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uidna.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uidna.h

"..\..\include\unicode\uidna.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uidna.h

"..\..\include\unicode\uidna.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uidna.h

"..\..\include\unicode\uidna.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uidna.h

"..\..\include\unicode\uidna.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "locales & resources"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\locbased.cpp
# End Source File
# Begin Source File

SOURCE=.\locbased.h
# End Source File
# Begin Source File

SOURCE=.\locid.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\locid.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\resbund.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\resbund.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucat.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ucat.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucat.h

"..\..\include\unicode\ucat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucat.h

"..\..\include\unicode\ucat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ucat.h

"..\..\include\unicode\ucat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ucat.h

"..\..\include\unicode\ucat.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uloc.c
# End Source File
# Begin Source File

SOURCE=.\ulocimp.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uloc.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ures.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uresbund.c
# End Source File
# Begin Source File

SOURCE=.\uresdata.c
# End Source File
# Begin Source File

SOURCE=.\uresdata.h
# End Source File
# Begin Source File

SOURCE=.\uresimp.h
# End Source File
# End Group
# Begin Group "normalization"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\caniter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\caniter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\caniter.h

"..\..\include\unicode\caniter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\caniter.h

"..\..\include\unicode\caniter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\caniter.h

"..\..\include\unicode\caniter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\caniter.h

"..\..\include\unicode\caniter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\normlzr.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\normlzr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unorm.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\unorm.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unorm.h

"..\..\include\unicode\unorm.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unorm.h

"..\..\include\unicode\unorm.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unorm.h

"..\..\include\unicode\unorm.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unorm.h

"..\..\include\unicode\unorm.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unorm_it.c
# End Source File
# Begin Source File

SOURCE=.\unorm_it.h
# End Source File
# Begin Source File

SOURCE=.\unormimp.h
# End Source File
# End Group
# Begin Group "properties & sets"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\propname.cpp
# End Source File
# Begin Source File

SOURCE=.\propname.h
# End Source File
# Begin Source File

SOURCE=.\ruleiter.cpp
# End Source File
# Begin Source File

SOURCE=.\ruleiter.h
# End Source File
# Begin Source File

SOURCE=.\unicode\symtable.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\symtable.h

"..\..\include\unicode\symtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\symtable.h

"..\..\include\unicode\symtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\symtable.h

"..\..\include\unicode\symtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\symtable.h

"..\..\include\unicode\symtable.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uchar.c
# End Source File
# Begin Source File

SOURCE=.\unicode\uchar.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unames.c
# End Source File
# Begin Source File

SOURCE=.\unifilt.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\unifilt.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unifilt.h

"..\..\include\unicode\unifilt.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unifunct.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\unifunct.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unifunct.h

"..\..\include\unicode\unifunct.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unifunct.h

"..\..\include\unicode\unifunct.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unifunct.h

"..\..\include\unicode\unifunct.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unifunct.h

"..\..\include\unicode\unifunct.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unimatch.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unimatch.h

"..\..\include\unicode\unimatch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unimatch.h

"..\..\include\unicode\unimatch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unimatch.h

"..\..\include\unicode\unimatch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unimatch.h

"..\..\include\unicode\unimatch.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uniset.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uniset.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uniset.h

"..\..\include\unicode\uniset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uprops.c
# End Source File
# Begin Source File

SOURCE=.\uprops.h
# End Source File
# Begin Source File

SOURCE=.\usc_impl.c
# End Source File
# Begin Source File

SOURCE=.\usc_impl.h
# End Source File
# Begin Source File

SOURCE=.\uscript.c
# End Source File
# Begin Source File

SOURCE=.\unicode\uscript.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uscript.h

"..\..\include\unicode\uscript.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uscript.h

"..\..\include\unicode\uscript.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uscript.h

"..\..\include\unicode\uscript.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uscript.h

"..\..\include\unicode\uscript.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uset.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uset.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uset.h

"..\..\include\unicode\uset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uset.h

"..\..\include\unicode\uset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uset.h

"..\..\include\unicode\uset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uset.h

"..\..\include\unicode\uset.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\usetiter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\usetiter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\usetiter.h

"..\..\include\unicode\usetiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\usetiter.h

"..\..\include\unicode\usetiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\usetiter.h

"..\..\include\unicode\usetiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\usetiter.h

"..\..\include\unicode\usetiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "registration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\iculdata.h
# End Source File
# Begin Source File

SOURCE=.\iculserv.cpp
# End Source File
# Begin Source File

SOURCE=.\iculserv.h
# End Source File
# Begin Source File

SOURCE=.\icunotif.cpp
# End Source File
# Begin Source File

SOURCE=.\icunotif.h
# End Source File
# Begin Source File

SOURCE=.\icurwlck.h
# End Source File
# Begin Source File

SOURCE=.\icuserv.cpp
# End Source File
# Begin Source File

SOURCE=.\icuserv.h
# End Source File
# End Group
# Begin Group "sprep"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\sprpimpl.h
# End Source File
# Begin Source File

SOURCE=.\usprep.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\usprep.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\usprep.h

"..\..\include\unicode\usprep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\usprep.h

"..\..\include\unicode\usprep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\usprep.h

"..\..\include\unicode\usprep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\usprep.h

"..\..\include\unicode\usprep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "strings"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\chariter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\chariter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\charstr.h
# End Source File
# Begin Source File

SOURCE=.\cstring.c
# End Source File
# Begin Source File

SOURCE=.\cstring.h
# End Source File
# Begin Source File

SOURCE=.\cwchar.c
# End Source File
# Begin Source File

SOURCE=.\cwchar.h
# End Source File
# Begin Source File

SOURCE=.\unicode\rep.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\schriter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\schriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uchriter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uchriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uiter.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\uiter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uiter.h

"..\..\include\unicode\uiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uiter.h

"..\..\include\unicode\uiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\uiter.h

"..\..\include\unicode\uiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\uiter.h

"..\..\include\unicode\uiter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unistr.cpp
# End Source File
# Begin Source File

SOURCE=.\unicode\unistr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\urep.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\urep.h

"..\..\include\unicode\urep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\urep.h

"..\..\include\unicode\urep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\urep.h

"..\..\include\unicode\urep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\urep.h

"..\..\include\unicode\urep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ustr_imp.h
# End Source File
# Begin Source File

SOURCE=.\ustrcase.c
# End Source File
# Begin Source File

SOURCE=.\ustring.c
# End Source File
# Begin Source File

SOURCE=.\unicode\ustring.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ustrtrns.c
# End Source File
# Begin Source File

SOURCE=.\unicode\utf.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf16.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf32.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf8.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utf_impl.c
# End Source File
# Begin Source File

SOURCE=.\unicode\utf_old.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf_old.h

"..\..\include\unicode\utf_old.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf_old.h

"..\..\include\unicode\utf_old.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\utf_old.h

"..\..\include\unicode\utf_old.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\utf_old.h

"..\..\include\unicode\utf_old.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# End Target
# End Project
