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
# PROP Output_Dir "..\..\lib\Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /I "..\..\include" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /machine:I386 /out:"..\..\bin\Release/icuuc.dll"

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

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
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\include" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "COMMON_EXPORTS" /D "U_COMMON_IMPLEMENTATION" /YX /FD /GZ /c
# SUBTRACT CPP /WX
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib advapi32.lib shell32.lib /nologo /base:"0x4a800000" /dll /debug /machine:I386 /out:"..\..\bin\Debug/icuuc.dll" /pdbtype:sept

!ENDIF 

# Begin Target

# Name "common - Win32 Release"
# Name "common - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\bidi.cpp
# End Source File
# Begin Source File

SOURCE=.\chariter.cpp
# End Source File
# Begin Source File

SOURCE=.\compdata.cpp
# End Source File
# Begin Source File

SOURCE=.\compitr.cpp
# End Source File
# Begin Source File

SOURCE=.\convert.cpp
# End Source File
# Begin Source File

SOURCE=.\cpputils.cpp
# End Source File
# Begin Source File

SOURCE=.\cstring.c
# End Source File
# Begin Source File

SOURCE=.\dcmpdata.cpp
# End Source File
# Begin Source File

SOURCE=.\digitlst.cpp
# End Source File
# Begin Source File

SOURCE=.\filestrm.c
# End Source File
# Begin Source File

SOURCE=.\locid.cpp
# End Source File
# Begin Source File

SOURCE=.\locmap.cpp
# End Source File
# Begin Source File

SOURCE=.\mutex.cpp
# End Source File
# Begin Source File

SOURCE=.\normlzr.cpp
# End Source File
# Begin Source File

SOURCE=.\putil.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\rbcache.cpp
# End Source File
# Begin Source File

SOURCE=.\rbdata.cpp
# End Source File
# Begin Source File

SOURCE=.\rbread.cpp
# End Source File
# Begin Source File

SOURCE=.\resbund.cpp
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\schriter.cpp
# End Source File
# Begin Source File

SOURCE=.\scsu.c
# End Source File
# Begin Source File

SOURCE=.\ubidi.c
# End Source File
# Begin Source File

SOURCE=.\ubidiln.c
# End Source File
# Begin Source File

SOURCE=.\uchar.c
# End Source File
# Begin Source File

SOURCE=.\uchriter.cpp
# End Source File
# Begin Source File

SOURCE=.\ucmp16.c
# End Source File
# Begin Source File

SOURCE=.\ucmp32.c
# End Source File
# Begin Source File

SOURCE=.\ucmp8.c
# End Source File
# Begin Source File

SOURCE=.\ucnv.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_bld.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_cnv.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_err.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_io.c
# End Source File
# Begin Source File

SOURCE=.\udata.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\uhash.c
# End Source File
# Begin Source File

SOURCE=.\uloc.c
# End Source File
# Begin Source File

SOURCE=.\umutex.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\unames.c
# End Source File
# Begin Source File

SOURCE=.\unicode.cpp
# End Source File
# Begin Source File

SOURCE=.\unistr.cpp
# End Source File
# Begin Source File

SOURCE=.\ures.cpp
# End Source File
# Begin Source File

SOURCE=.\ustring.c
# End Source File
# Begin Source File

SOURCE=.\uvector.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\bidi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\bidi.h

"..\..\include\bidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy      bidi.h      ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\bidi.h

"..\..\include\bidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy      bidi.h      ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\chariter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\chariter.h

"..\..\include\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           chariter.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\chariter.h

"..\..\include\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           chariter.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\cmemory.h
# End Source File
# Begin Source File

SOURCE=.\compdata.h
# End Source File
# Begin Source File

SOURCE=.\compitr.h
# End Source File
# Begin Source File

SOURCE=.\convert.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\convert.h

"..\..\include\convert.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           convert.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\convert.h

"..\..\include\convert.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           convert.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\cpputils.h
# End Source File
# Begin Source File

SOURCE=.\cstring.h
# End Source File
# Begin Source File

SOURCE=.\dcmpdata.h
# End Source File
# Begin Source File

SOURCE=.\digitlst.h
# End Source File
# Begin Source File

SOURCE=.\filestrm.h
# End Source File
# Begin Source File

SOURCE=.\locid.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\locid.h

"..\..\include\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           locid.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\locid.h

"..\..\include\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           locid.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\locmap.h
# End Source File
# Begin Source File

SOURCE=.\mutex.h
# End Source File
# Begin Source File

SOURCE=.\normlzr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\normlzr.h

"..\..\include\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           normlzr.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\normlzr.h

"..\..\include\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           normlzr.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\putil.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\putil.h

"..\..\include\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            putil.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\putil.h

"..\..\include\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            putil.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\pwin32.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\pwin32.h

"..\..\include\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                       pwin32.h                        ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\pwin32.h

"..\..\include\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                       pwin32.h                        ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rbcache.h
# End Source File
# Begin Source File

SOURCE=.\rbdata.h
# End Source File
# Begin Source File

SOURCE=.\rbread.h
# End Source File
# Begin Source File

SOURCE=.\rep.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\rep.h

"..\..\include\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    rep.h    ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\rep.h

"..\..\include\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    rep.h    ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\resbund.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\resbund.h

"..\..\include\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             resbund.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\resbund.h

"..\..\include\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             resbund.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\schriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\schriter.h

"..\..\include\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            schriter.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\schriter.h

"..\..\include\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            schriter.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\scsu.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\scsu.h

"..\..\include\scsu.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       scsu.h       ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\scsu.h

"..\..\include\scsu.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       scsu.h       ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ubidi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ubidi.h

"..\..\include\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy      ubidi.h      ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ubidi.h

"..\..\include\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy      ubidi.h      ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ubidiimp.h
# End Source File
# Begin Source File

SOURCE=.\uchar.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\uchar.h

"..\..\include\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             uchar.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\uchar.h

"..\..\include\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             uchar.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uchriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\uchriter.h

"..\..\include\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           uchriter.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\uchriter.h

"..\..\include\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           uchriter.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucmp16.h
# End Source File
# Begin Source File

SOURCE=.\ucmp32.h
# End Source File
# Begin Source File

SOURCE=.\ucmp8.h
# End Source File
# Begin Source File

SOURCE=.\ucnv.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ucnv.h

"..\..\include\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ucnv.h

"..\..\include\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_bld.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ucnv_bld.h

"..\..\include\ucnv_bld.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv_bld.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ucnv_bld.h

"..\..\include\ucnv_bld.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv_bld.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_cnv.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_err.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ucnv_err.h

"..\..\include\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv_err.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ucnv_err.h

"..\..\include\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                           ucnv_err.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_imp.h
# End Source File
# Begin Source File

SOURCE=.\ucnv_io.h
# End Source File
# Begin Source File

SOURCE=.\uhash.h
# End Source File
# Begin Source File

SOURCE=.\uloc.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\uloc.h

"..\..\include\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              uloc.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\uloc.h

"..\..\include\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              uloc.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umisc.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\umisc.h
InputName=umisc

"..\..\include\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputName).h ..\..\include 
	echo $(InputName) 
	
# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\umisc.h
InputName=umisc

"..\..\include\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputName).h ..\..\include 
	echo $(InputName) 
	
# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umutex.h
# End Source File
# Begin Source File

SOURCE=.\unicode.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode.h

"..\..\include\unicode.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode.h

"..\..\include\unicode.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unistr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unistr.h

"..\..\include\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unistr.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unistr.h

"..\..\include\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unistr.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unistrm.h
# End Source File
# Begin Source File

SOURCE=.\ures.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ures.h

"..\..\include\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              ures.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ures.h

"..\..\include\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              ures.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ustring.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\ustring.h

"..\..\include\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             ustring.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\ustring.h

"..\..\include\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             ustring.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\utypes.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\utypes.h

"..\..\include\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              utypes.h                            ..\..\include

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\utypes.h

"..\..\include\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              utypes.h                            ..\..\include

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uvector.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
