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

SOURCE=.\ucnv2022.c
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

SOURCE=.\ucnv_lmb.c
# End Source File
# Begin Source File

SOURCE=.\ucnv_utf.c
# End Source File
# Begin Source File

SOURCE=.\ucnvlat1.c
# End Source File
# Begin Source File

SOURCE=.\ucnvmbcs.c
# End Source File
# Begin Source File

SOURCE=.\ucnvsbcs.c
# End Source File
# Begin Source File

SOURCE=.\udata.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\uhash.c
# End Source File
# Begin Source File

SOURCE=.\uhash_us.cpp
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

SOURCE=.\uresdata.c
# End Source File
# Begin Source File

SOURCE=.\ustring.c
# End Source File
# Begin Source File

SOURCE=.\utf_impl.c
# End Source File
# Begin Source File

SOURCE=.\uvector.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\unicode\bidi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\bidi.h

"..\..\include\unicode\bidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       unicode\bidi.h      ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\bidi.h

"..\..\include\unicode\bidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       unicode\bidi.h      ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\chariter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\chariter.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\chariter.h

"..\..\include\unicode\chariter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\chariter.h                            ..\..\include\unicode

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

SOURCE=.\unicode\convert.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\convert.h

"..\..\include\unicode\convert.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\convert.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\convert.h

"..\..\include\unicode\convert.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\convert.h                            ..\..\include\unicode

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

SOURCE=.\unicode\locid.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\locid.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\locid.h

"..\..\include\unicode\locid.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\locid.h                            ..\..\include\unicode

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

SOURCE=.\unicode\normlzr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\normlzr.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\normlzr.h

"..\..\include\unicode\normlzr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\normlzr.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\putil.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode\putil.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\putil.h

"..\..\include\unicode\putil.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode\putil.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\pwin32.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                        unicode\pwin32.h                        ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\pwin32.h

"..\..\include\unicode\pwin32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                        unicode\pwin32.h                        ..\..\include\unicode

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

SOURCE=.\unicode\rep.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy     unicode\rep.h    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\rep.h

"..\..\include\unicode\rep.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy     unicode\rep.h    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\resbund.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\resbund.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\resbund.h

"..\..\include\unicode\resbund.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\resbund.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\schriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode\schriter.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\schriter.h

"..\..\include\unicode\schriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                             unicode\schriter.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\scsu.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\scsu.h

"..\..\include\unicode\scsu.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy        unicode\scsu.h       ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\scsu.h

"..\..\include\unicode\scsu.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy        unicode\scsu.h       ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ubidi.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       unicode\ubidi.h      ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ubidi.h

"..\..\include\unicode\ubidi.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy       unicode\ubidi.h      ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ubidiimp.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uchar.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\uchar.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uchar.h

"..\..\include\unicode\uchar.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\uchar.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\uchriter.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\uchriter.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uchriter.h

"..\..\include\unicode\uchriter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\uchriter.h                            ..\..\include\unicode

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

SOURCE=.\unicode\ucnv.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv.h

"..\..\include\unicode\ucnv.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ucnv_bld.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_bld.h

"..\..\include\unicode\ucnv_bld.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv_bld.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_bld.h

"..\..\include\unicode\ucnv_bld.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv_bld.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ucnv_cnv.h
# End Source File
# Begin Source File

SOURCE=.\unicode\ucnv_err.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv_err.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ucnv_err.h

"..\..\include\unicode\ucnv_err.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                            unicode\ucnv_err.h                            ..\..\include\unicode

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

SOURCE=.\unicode\udata.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  unicode\udata.h ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\udata.h

"..\..\include\unicode\udata.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  unicode\udata.h ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uhash.h
# End Source File
# Begin Source File

SOURCE=.\unicode\uloc.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\uloc.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\uloc.h

"..\..\include\unicode\uloc.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\uloc.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\umachine.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umachine.h

"..\..\include\unicode\umachine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

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
	copy unicode\$(InputName).h ..\..\include\unicode 
	echo $(InputName) 
	
# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\umisc.h
InputName=umisc

"..\..\include\unicode\$(InputName).h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy unicode\$(InputName).h ..\..\include\unicode 
	echo $(InputName) 
	
# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\umutex.h
# End Source File
# Begin Source File

SOURCE=.\unicode\unicode.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unicode.h

"..\..\include\unicode\unicode.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\unicode.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unicode.h

"..\..\include\unicode\unicode.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\unicode.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\unistr.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\unistr.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\unistr.h

"..\..\include\unicode\unistr.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\unistr.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unistrm.h
# End Source File
# Begin Source File

SOURCE=.\unicode\ures.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\ures.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ures.h

"..\..\include\unicode\ures.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\ures.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\uresdata.h
# End Source File
# Begin Source File

SOURCE=.\unicode\ustring.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\ustring.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ustring.h

"..\..\include\unicode\ustring.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\ustring.h                            ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf.h

"..\..\include\unicode\utf.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf16.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf16.h

"..\..\include\unicode\utf16.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf32.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf32.h

"..\..\include\unicode\utf32.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utf8.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utf8.h

"..\..\include\unicode\utf8.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(InputPath) ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\utypes.h

!IF  "$(CFG)" == "common - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\utypes.h                            ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "common - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\utypes.h

"..\..\include\unicode\utypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy                              unicode\utypes.h                            ..\..\include\unicode

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
