# Microsoft Developer Studio Project File - Name="makedata" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) External Target" 0x0106

CFG=makedata - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "makedata.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "makedata.mak" CFG="makedata - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "makedata - Win32 Release" (based on "Win32 (x86) External Target")
!MESSAGE "makedata - Win32 Debug" (based on "Win32 (x86) External Target")
!MESSAGE "makedata - Win64 Release" (based on "Win32 (x86) External Target")
!MESSAGE "makedata - Win64 Debug" (based on "Win32 (x86) External Target")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""

!IF  "$(CFG)" == "makedata - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Cmd_Line "NMAKE /f makedata.mak"
# PROP BASE Rebuild_Opt "/a"
# PROP BASE Target_File "makedata.exe"
# PROP BASE Bsc_Name "makedata.bsc"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Cmd_Line "NMAKE /f makedata.mak icumake=$(MAKEDIR)$(ProjectDir)  cfg=release"
# PROP Rebuild_Opt "clean all"
# PROP Target_File "makedata.exe"
# PROP Bsc_Name ""
# PROP Target_Dir ""

!ELSEIF  "$(CFG)" == "makedata - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Cmd_Line "NMAKE /f makedata.mak"
# PROP BASE Rebuild_Opt "/a"
# PROP BASE Target_File "makedata.exe"
# PROP BASE Bsc_Name "makedata.bsc"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Cmd_Line "NMAKE /f makedata.mak icumake=$(MAKEDIR)$(ProjectDir) cfg=debug"
# PROP Rebuild_Opt "clean all"
# PROP Bsc_Name ""
# PROP Target_Dir ""

!ELSEIF  "$(CFG)" == "makedata - Win64 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Cmd_Line "NMAKE /f makedata.mak"
# PROP BASE Rebuild_Opt "/a"
# PROP BASE Target_File "makedata.exe"
# PROP BASE Bsc_Name "makedata.bsc"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Cmd_Line "NMAKE /f makedata.mak icumake=$(MAKEDIR)$(ProjectDir)  cfg=release"
# PROP Rebuild_Opt "clean all"
# PROP Target_File "makedata.exe"
# PROP Bsc_Name ""
# PROP Target_Dir ""

!ELSEIF  "$(CFG)" == "makedata - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Cmd_Line "NMAKE /f makedata.mak"
# PROP BASE Rebuild_Opt "/a"
# PROP BASE Target_File "makedata.exe"
# PROP BASE Bsc_Name "makedata.bsc"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Cmd_Line "NMAKE /f makedata.mak icumake=$(MAKEDIR)$(ProjectDir) cfg=debug"
# PROP Rebuild_Opt "clean all"
# PROP Bsc_Name ""
# PROP Target_Dir ""

!ENDIF 

# Begin Target

# Name "makedata - Win32 Release"
# Name "makedata - Win32 Debug"
# Name "makedata - Win64 Release"
# Name "makedata - Win64 Debug"

!IF  "$(CFG)" == "makedata - Win32 Release"

!ELSEIF  "$(CFG)" == "makedata - Win32 Debug"

!ELSEIF  "$(CFG)" == "makedata - Win64 Release"

!ELSEIF  "$(CFG)" == "makedata - Win64 Debug"

!ENDIF 

# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=.\makedata.mak
# PROP Intermediate_Dir ".\out\build"
# End Source File
# Begin Source File

SOURCE=.\misc\miscfiles.mk
# End Source File
# Begin Source File

SOURCE=.\locales\resfiles.mk
# End Source File
# Begin Source File

SOURCE=..\test\testdata\testdata.mk
# End Source File
# Begin Source File

SOURCE=.\translit\trnsfiles.mk
# End Source File
# Begin Source File

SOURCE=.\mappings\ucmcore.mk
# End Source File
# Begin Source File

SOURCE=.\mappings\ucmebcdic.mk
# End Source File
# Begin Source File

SOURCE=.\mappings\ucmfiles.mk
# End Source File
# End Group
# End Target
# End Project
