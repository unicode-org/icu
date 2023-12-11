---
layout: default
title: Building ICU4C
nav_order: 2
parent: ICU4C
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Building ICU4C
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Recommended Build Options

Depending on the platform and the type of installation, we recommend a small number of modifications and build options. Note that C99 compatibility is now required.

*   **Namespace (ICU 61 and later):** Since ICU 61, call sites need to qualify ICU types explicitly, for example `icu::UnicodeString`, or do `using icu::UnicodeString;` where appropriate. If your code relies on the "using namespace icu;" that used to be in `unicode/uversion.h`, then you need to update your code.
    You could temporarily (until you have more time to update your code) revert to the default "using" via `-DU_USING_ICU_NAMESPACE=1` or by modifying `unicode/uversion.h`:

```
    Index: icu4c/source/common/unicode/uversion.h
    ===================================================================
    --- icu4c/source/common/unicode/uversion.h      (revision 40704)
    +++ icu4c/source/common/unicode/uversion.h      (working copy)
    @@ -127,7 +127,7 @@
                     defined(U_LAYOUTEX_IMPLEMENTATION) || defined(U_TOOLUTIL_IMPLEMENTATION)
     #           define U_USING_ICU_NAMESPACE 0
     #       else
    -#           define U_USING_ICU_NAMESPACE 0
    +#           define U_USING_ICU_NAMESPACE 1
     #       endif
     #   endif
     #   if U_USING_ICU_NAMESPACE
```

*   **Namespace (ICU 60 and earlier):** By default, unicode/uversion.h has "using namespace icu;" which defeats much of the purpose of the namespace. (This is for historical reasons: Originally, ICU4C did not use namespaces, and some compilers did not support them. The default "using" statement preserves source code compatibility.)
    You should turn this off via `-DU_USING_ICU_NAMESPACE=0` or by modifying unicode/uversion.h:

```
    Index: source/common/unicode/uversion.h
    ===================================================================
    --- source/common/unicode/uversion.h    (revision 26606)
    +++ source/common/unicode/uversion.h    (working copy)
    @@ -180,7 +180,8 @@
     #   define U_NAMESPACE_QUALIFIER U_ICU_NAMESPACE::

     #   ifndef U_USING_ICU_NAMESPACE
    -#       define U_USING_ICU_NAMESPACE 1
    +        // Set to 0 to force namespace declarations in ICU usage.
    +#       define U_USING_ICU_NAMESPACE 0
     #   endif
     #   if U_USING_ICU_NAMESPACE
             U_NAMESPACE_USE
```

    ICU call sites then either qualify ICU types explicitly, for example `icu::UnicodeString`, or do `using icu::UnicodeString;` where appropriate.
*   **Hardcode the default charset to UTF-8:** On platforms where the default charset is always UTF-8, like MacOS X and some Linux distributions, we recommend hardcoding ICU's default charset to UTF-8. This means that some implementation code becomes simpler and faster, and statically linked ICU libraries become smaller. (See the [U_CHARSET_IS_UTF8](https://unicode-org.github.io/icu-docs/apidoc/dev/icu4c/platform_8h.html#a0a33e1edf3cd23d9e9c972b63c9f7943) API documentation for more details.)
    You can `-DU_CHARSET_IS_UTF8=1` or modify `unicode/utypes.h` (in ICU 4.8 and below) or modify unicode/platform.h (in ICU 49 and higher):

```
    Index: source/common/unicode/utypes.h
    ===================================================================
    --- source/common/unicode/utypes.h      (revision 26606)
    +++ source/common/unicode/utypes.h      (working copy)
    @@ -160,7 +160,7 @@
      * @see UCONFIG_NO_CONVERSION
      */
     #ifndef U_CHARSET_IS_UTF8
    -#   define U_CHARSET_IS_UTF8 0
    +#   define U_CHARSET_IS_UTF8 1
     #endif

     /*===========================================================================*/
```

*   **UnicodeString constructors:** The UnicodeString class has several single-argument constructors that are not marked "explicit" for historical reasons. This can lead to inadvertent construction of a `UnicodeString` with a single character by using an integer, and it can lead to inadvertent dependency on the conversion framework by using a C string literal.
    Beginning with ICU 49, you should do the following:
    *   Consider marking the from-`UChar` and from-`UChar32` constructors explicit via `-DUNISTR_FROM_CHAR_EXPLICIT=explicit` or similar.
    *   Consider marking the from-`const char*` and from-`const UChar*` constructors explicit via `-DUNISTR_FROM_STRING_EXPLICIT=explicit` or similar.
    > :point_right: **Note**:  The ICU test suites cannot be compiled with these settings.
*   **utf.h, utf8.h, utf16.h, utf_old.h:** By default, utypes.h (and thus almost every public ICU header) includes all of these header files. Often, none of them are needed, or only one or two of them. All of utf_old.h is deprecated or obsolete.
    Beginning with ICU 49, you should define `U_NO_DEFAULT_INCLUDE_UTF_HEADERS` to 1 (via -D or uconfig.h, as above) and include those header files explicitly that you actually need.
    > :point_right: **Note**:  The ICU test suites cannot be compiled with this setting.
*   **utf_old.h:** All of utf_old.h is deprecated or obsolete.
    Beginning with ICU 60, you should define `U_HIDE_OBSOLETE_UTF_OLD_H` to 1 (via -D or uconfig.h, as above). Use of any of these macros should be replaced as noted in the comments for the obsolete macro.
    > :point_right: **Note**:  The ICU test suites _can_ be compiled with this setting.
*   **.dat file:** By default, the ICU data is built into a shared library (DLL). This is convenient because it requires no install-time or runtime configuration, but the library is platform-specific and cannot be modified. A .dat package file makes the opposite trade-off: Platform-portable (except for endianness and charset family, which can be changed with the icupkg tool) and modifiable (also with the icupkg tool). If a path is set, then single data files (e.g., .res files) can be copied to that location to provide new locale data or conversion tables etc.
    The only drawback with a .dat package file is that the application needs to provide ICU with the file system path to the package file (e.g., by calling `u_setDataDirectory()`) or with a pointer to the data (`udata_setCommonData()`) before other ICU API calls. This is usually easy if ICU is used from an application where `main()` takes care of such initialization. It may be hard if ICU is shipped with another shared library (such as the Xerces-C++ XML parser) which does not control `main()`.
    See the [User Guide ICU Data](../icu_data) chapter for more details.
    If possible, we recommend building the .dat package. Specify `--with-data-packaging=archive` on the configure command line, as in
    `runConfigureICU Linux --with-data-packaging=archive`
    (Read the configure script's output for further instructions. On Windows, the Visual Studio build generates both the .dat package and the data DLL.)
    Be sure to install and use the tiny stubdata library rather than the large data DLL.
*   **Static libraries:** It may make sense to build the ICU code into static libraries (.a) rather than shared libraries (.so/.dll). Static linking reduces the overall size of the binary by removing code that is never called.
    Example configure command line:
    `runConfigureICU Linux --enable-static --disable-shared`
*   **Out-of-source build:** It is usually desirable to keep the ICU source file tree clean and have build output files written to a different location. This is called an "out-of-source build". Simply invoke the configure script from the target location:
```
    ~/icu$ git clone https://github.com/unicode-org/icu.git
    ~/icu$ mkdir icu4c-build
    ~/icu$ cd icu4c-build
    ~/icu/icu4c-build$ ../icu/icu4c/source/runConfigureICU Linux
    ~/icu/icu4c-build$ make check
```
    > :point_right: **Note**:  this example shows a relative path to `runConfigureICU`. If you experience difficulty, try using an absolute path to `runConfigureICU` instead.

### ICU as a System-Level Library

If ICU is installed as a system-level library, there are further opportunities and restrictions to consider. For details, see the _Using ICU as an Operating System Level Library_ section of the [User Guide ICU Architectural Design](../icu/design) chapter.

*   **Data path:** For a system-level library, it is best to load ICU data from the .dat package file because the file system path to the .dat package file can be hardcoded. ICU will automatically set the path to the final install location using `U_ICU_DATA_DEFAULT_DIR`. Alternatively, you can set `-DICU_DATA_DIR=/path/to/icu/data` when building the ICU code. (Used by source/common/putil.c.)
    Consider also setting `-DICU_NO_USER_DATA_OVERRIDE` if you do not want the `ICU_DATA` environment variable to be used. (An application can still override the data path via `u_setDataDirectory()` or `udata_setCommonData()`.
*   **Hide draft API:** API marked with `@draft` is new and not yet stable. Applications must not rely on unstable APIs from a system-level library. Define `U_HIDE_DRAFT_API`, `U_HIDE_INTERNAL_API` and `U_HIDE_SYSTEM_API` by modifying `unicode/utypes.h` before installing it.
*   **Only C APIs:** Applications must not rely on C++ APIs from a system-level library because binary C++ compatibility across library and compiler versions is very hard to achieve. Most ICU C++ APIs are in header files that contain a comment with `\brief C++ API`. Consider not installing these header files, or define `U_SHOW_CPLUSPLUS_API` to be `0` by modifying `unicode/utypes.h` before installing it.
*   **Disable renaming:** By default, ICU library entry point names have an ICU version suffix. Turn this off for a system-level installation, to enable upgrading ICU without breaking applications. For example:
    `runConfigureICU Linux --disable-renaming`
    The public header files from this configuration must be installed for applications to include and get the correct entry point names.

## User-Configurable Settings

ICU4C can be customized via a number of user-configurable settings. Many of them are controlled by preprocessor macros which are defined in the `source/common/unicode/uconfig.h` header file. Some turn off parts of ICU, for example conversion or collation, trading off a smaller library for reduced functionality. Other settings are recommended (see previous section) but their default values are set for better source code compatibility.

In order to change such user-configurable settings, you can either modify the `uconfig.h` header file by adding a specific `#define ...` for one or more of the macros before they are first tested, or set the compiler's preprocessor flags (`CPPFLAGS`) to include an equivalent `-D` macro definition.

## How To Build And Install On Windows

Building International Components for Unicode requires:

*   Microsoft Windows 7 or newer. (Windows XP and Windows Vista are not  supported)
*   Microsoft Visual C++ (part of [Visual Studio](https://www.visualstudio.com/)) (from either Visual Studio 2017 or Visual Studio 2019)
*   _**Optional:**_ A version of the [Windows 10 SDK](https://developer.microsoft.com/windows/downloads) is needed if you want to build the UWP projects.

Notes regarding Windows specific issues:
- When using "`@compat=host`" on versions of Windows below Windows 10 version 1703, there are 6 locales with date and number formatting issues ([#13119](https://unicode-org.atlassian.net/browse/ICU-13119)).

- The LCID conversion APIs don't round-trip Kurdish (ku) and Central Kurdish (ckb) due to Windows not having a ckb locale ([#20181](https://unicode-org.atlassian.net/browse/ICU-20181)).


> :point_right: **Note**: [Cygwin](#how-to-build-and-install-on-windows-with-cygwin) is required if using a version of MSVC other than the one compatible with the supplied project files or if other compilers are used to build ICU. (e.g. GCC)

The steps are:

1.  Unzip the `icu-XXXX.zip` file into any convenient location.
    *   You can use the built-in zip functionality of Windows Explorer to do this. Right-click on the .zip file and choose the "Extract All" option from the context menu. This will open a new window where you can choose the output location to put the files.
    *   Alternatively, you can use a 3<sup>rd</sup> party GUI tool like 7-Zip or WinZip to do this as well.
2.  Be sure that the ICU binary directory, (ex: `<ICU>\bin\`), is included in the **PATH** environment variable. The tests will not work without the location of the ICU DLL files in the path. Note that the binary directory name can depend on what architecture you select when you compile ICU. For x86 or 32-bit builds, the binary directory is `bin`. Whereas for x64 or 64-bit builds the binary directory is `bin64`.
3.  Open the `<ICU>\source\allinone\allinone.sln` solution file in 'Visual Studio 2017'. (This solution includes all the International Components for Unicode libraries, necessary ICU building tools, and the test suite projects). Please see the [command line note below](#using-msbuild-at-the-command-line) if you want to build from the command line instead.
4.  If you are building using 'Visual Studio 2015' instead, or if you are building the UWP projects and you have a different version of the Windows 10 SDK installed you will first need to modify the two `Build.Windows.*.props` files in the `allinone` directory before you can open the "allinone" solution file. Please see the notes below about [building with other versions of Visual Studio](#building-with-other-versions-of-visual-studio) and the notes on [re-targeting the Windows 10 SDK for the UWP projects](#re-targeting-the-windows-10-sdk-for-the-uwp-projects) for details. Alternatively, you can [skip building the UWP projects](#re-targeting-the-windows-10-sdk-for-the-uwp-projects) entirely as well.
5.  Set the active platform to "Win32" or "x64" (See [Windows platform note](#setting-active-platform) below) and configuration to "Debug" or "Release" (See [Windows configuration note](#setting-active-configuration) below).
6.  Choose the "Build" menu and select "Rebuild Solution". If you want to build the Debug and Release at the same time, see the [batch configuration note](#batch-configuration) below.
7.  Run the tests. They can be run from the command line or from within Visual Studio.

    #### Running the Tests from the Windows Command Line (cmd)

    *   The general syntax is:

        <pre>
        &lt;ICU&gt;\source\allinone\icucheck.bat <i>Platform</i> <i>Configuration</i>
        </pre>

    *   So, for example for x86 (32-bit) and Debug, use the following:
        ```
        <ICU>\source\allinone\icucheck.bat x86 Debug
        ```
        For x86 (32-bit) and Release:
        ```
        <ICU>\source\allinone\icucheck.bat x86 Release
        ```
        For x64 (64-bit) and Debug:
        ```
        <ICU>\source\allinone\icucheck.bat x64 Debug
        ```
        For x64 (64-bit) and Release:
        ```
        <ICU>\source\allinone\icucheck.bat x64 Release
        ```

    #### Running the Tests from within Visual Studio

    1.  Run the C++ test suite, `intltest`. To do this: set the active startup project to "intltest", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
    2.  Run the C test suite, `cintltst`. To do this: set the active startup project to "cintltst", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
    3.  Run the I/O test suite, `iotest`. To do this: set the active startup project to "iotest", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
8.  You are now able to develop applications with ICU by using the libraries and tools in `<ICU>\bin\`. The headers are in `<ICU>\include\` and the link libraries are in `<ICU>\lib\`. To install the ICU runtime on a machine, or ship it with your application, copy the needed components from `<ICU>\bin\` to a location on the system PATH or to your application directory.

### Building with other versions of Visual Studio

The particular version of the MSVC compiler tool-set (and thus the corresponding version of Visual Studio) that is used to compile ICU is determined by the `PlatformToolset` property. This property is stored in two different shared files that are used to set common configuration settings amongst the various ICU `*.vcxproj` project files. For the non-UWP projects, this setting is in the shared file called `Build.Windows.ProjectConfiguration.props` located in the `allinone` directory. For the UWP projects, this setting is in the shared file called `Build.Windows.UWP.ProjectConfiguration.props`, also located in the `allinone` directory.

The value of `v140` corresponds to the Visual Studio 2015 compiler tool set, whereas the value of `v141` corresponds to the Visual Studio 2017 compiler tool set.

In order to build the non-UWP projects with Visual Studio 2015 you will need to modify the file called `Build.Windows.ProjectConfiguration.props` to change the value of the `PlatformToolset` property. Note however that Visual Studio 2017 is required for building the UWP projects.

> :point_right: **Note**: Using older versions of the MSVC compiler is generally not recommended due to the improved support for the C++17 standard in newer versions of the compiler.

### Re-targeting the Windows 10 SDK for the UWP projects

If the version of the Windows 10 SDK that you have installed does not match the version used by the UWP projects, then you will need to "retarget" them to use the version of the SDK that you have installed instead. There are two ways to do this:

* In Visual Studio you can right-click on the UWP projects in the 'Solution Explorer' and select the option 'Retarget Projects' from the context menu. This will open up a window where you can select the SDK version to target from a drop-down list of the various SDKs that are installed on the machine.
* Alternatively, you can manually edit the shared file called `Build.Windows.UWP.ProjectConfiguration.props` which is located in the `allinone` directory. You will need to change the of the `WindowsTargetPlatformVersion` property to the version of the SDK that you would like to use instead.

### Using MSBUILD At The Command Line

You can build ICU from the command line instead of using the Visual Studio GUI. Assuming that you have properly installed Visual Studio to support command line building, you should have a shortcut for the "Developer Command Prompt" listed in the Start Menu. (For Visual Studio 2017 you will need to install the "Desktop development with C++" option).

* Open the "Developer Command Prompt" shortcut from the Start Menu. (This will open up a new command line window).
* From within the "Developer Command Prompt" change directory (`cd`) to the ICU source directory.
* You can then use either `msbuild` directly, or you can use the `devenv.com` command to build ICU.
* Using `MSBUILD`:
  - To build the 32-bit Debug version, use the following command line:
    ```
    msbuild source\allinone\allinone.sln /p:Configuration=Debug /p:Platform=Win32
    ```
  - To build the 64-bit Release version, use the following command line:
    ```
    msbuild source\allinone\allinone.sln /p:Configuration=Release /p:Platform=x64
    ```
* Using `devenv.com`:
  - To build the 32-bit Debug version, use the following command line:
    ```
    devenv.com source\allinone\allinone.sln /build "Debug|Win32"
    ```
  - To build the 64-bit Release version, use the following command line:
    ```
    devenv.com source\allinone\allinone.sln /build "Release|x64"
    ```

### Skipping the UWP Projects on the Command Line

You can skip (or omit) building the UWP projects on the command line by passing the argument '`SkipUWP=true`' to either MSBUILD or devenv.

* For example, using `MSBUILD`:
  - To skip building the UWP projects with a 32-bit Debug build, use the following command line:
    ```
    msbuild source\allinone\allinone.sln /p:Configuration=Debug /p:Platform=Win32 /p:SkipUWP=true
    ```
  - To skip building the UWP projects with a 64-bit Release version, use the following command line:
    ```
    msbuild source\allinone\allinone.sln /p:Configuration=Release /p:Platform=x64 /p:SkipUWP=true
    ```

You can also use Cygwin with the MSVC compiler to build ICU, and you can refer to the [How To Build And Install On Windows with Cygwin](#how-to-build-and-install-on-windows-with-cygwin) section for more details.

### Setting Active Platform

Even though you are able to select "x64" as the active platform, if your operating system is not a 64 bit version of Windows, the build will fail. To set the active platform, two different possibilities are:

* Choose "Build" menu, select "Configuration Manager...", and select "Win32" or "x64" for the Active Platform Solution.
* Another way is to select the desired build configuration from "Solution Platforms" dropdown menu from the standard toolbar. It will say "Win32" or "x64" in the dropdown list.

### Setting Active Configuration

To set the active configuration, two different possibilities are:

*   Choose "Build" menu, select "Configuration Manager...", and select "Release" or "Debug" for the Active Configuration Solution.
*   Another way is to select the desired build configuration from "Solution Configurations" dropdown menu from the standard toolbar. It will say "Release" or "Debug" in the dropdown list.

### Batch Configuration

If you want to build the Win32 and x64 platforms and Debug and Release configurations at the same time, choose "Build" menu, and select "Batch Build...". Click the "Select All" button, and then click the "Rebuild" button.

## How To Build And Install On Windows with Cygwin

Building International Components for Unicode with this configuration requires:

*   Microsoft Windows
*   Microsoft Visual C++ (from Visual Studio 2015 or newer, when gcc isn't used).
*   Cygwin with the following installed:
    *   bash
    *   GNU make
    *   ar
    *   ranlib
    *   man (if you plan to look at the man pages)

There are two ways you can build ICU with Cygwin. You can build with gcc or Microsoft Visual C++. If you use gcc, the resulting libraries and tools will depend on the Cygwin environment. If you use Microsoft Visual C++, the resulting libraries and tools do not depend on Cygwin and can be more easily distributed to other Windows computers (the generated man pages and shell scripts still need Cygwin). To build with gcc, please follow the "[How To Build And Install On UNIX](#how-to-build-and-install-on-unix)" instructions, while you are inside a Cygwin bash shell. To build with Microsoft Visual C++, please use the following instructions:

1.  Start the Windows "Command Prompt" window. This is different from the gcc build, which requires the Cygwin Bash command prompt. The Microsoft Visual C++ compiler will not work with a bash command prompt.
2.  If the computer isn't set up to use Visual C++ from the command line, you need to run vcvars32.bat.
    For example:
    `C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\vcvars32.bat` can be used for 32-bit builds **or**
    `C:\Program Files (x86)\Microsoft Visual Studio 14\VC\bin\x86_amd64\vcvarsx86_amd64.bat` can be used for 64-bit builds on Windows x64.
3.  Unzip the icu-XXXX.zip file into any convenient location. Using command line zip, type "unzip -a icu-XXXX.zip -d drive:\directory", or just use WinZip.
4.  Change directory to "icu/source", which is where you unzipped ICU.
5.  Run `bash ./runConfigureICU Cygwin/MSVC` (See [Windows configuration note](#setting-active-configuration) and non-functional configure options below; see source for [./runConfigureICU](https://github.com/unicode-org/icu/blob/main/icu4c/source/runConfigureICU)).
6.  Type `make` to compile the libraries and all the data files. This make command should be GNU make.
7.  Optionally, type `make check` to run the test suite, which checks for ICU's functionality integrity (See [testing note](#running-the-tests-from-the-command-line) below).
8.  Type `make install` to install ICU. If you used the `--prefix=` option on `configure` or `runConfigureICU`, ICU will be installed to the directory you specified. (See [installation note](#installing-icu) below).

### Configuring ICU on Windows

Ensure that the order of the PATH is MSVC, Cygwin, and then other PATHs. The configure script needs certain tools in Cygwin (e.g. grep).

Also, you may need to run `dos2unix.exe` on all of the scripts (e.g. `configure`) in the top source directory of ICU. To avoid this issue, you can download the ICU source for Unix platforms (icu-xxx.tgz).

In addition to the Unix [configuration note](#configuring-icu) the following configure options currently do not work on Windows with Microsoft's compiler. Some options can work by manually editing `icu/source/common/unicode/pwin32.h`, but manually editing the files is not recommended.

*   `--disable-renaming`
*   `--enable-tracing`
*   `--enable-rpath`
*   `--enable-static` (Requires that U_STATIC_IMPLEMENTATION be defined in user code that links against ICU's static libraries.)
*   `--with-data-packaging=files` (The pkgdata tool currently does not work in this mode. Manual packaging is required to use this mode.)

## How To Build And Install On UNIX

Building International Components for Unicode on UNIX requires:

*   A C++ compiler installed on the target machine (for example: gcc, CC, xlC_r, aCC, cxx, etc...).
*   An ANSI C compiler installed on the target machine (for example: cc).
*   A recent version of GNU make (3.80+).
*   For a list of z/OS tools please view the [z/OS build section](#how-to-build-and-install-on-zos-os390) of this document for further details.

Here are the steps to build ICU:

1.  Decompress the icu-_X_._Y_.tgz (or icu-_X_._Y_.tar.gz) file. For example,
    ```
    gunzip -d < icu-_X_._Y_.tgz | tar xvf -
    ```
1.  Change directory to `icu/source`.
    ```
    cd icu/source
    ```
1.  Some files may have the wrong permissions.
    ```
    chmod +x runConfigureICU configure install-sh
    ```
1.  Run the [`runConfigureICU`](https://github.com/unicode-org/icu/blob/main/icu4c/source/runConfigureICU) script for your platform. (See [configuration note](#configuring-icu) below).
1.  Now build:
```
gmake
```
     (or just `make` if GNU make is the default make on your platform) to compile the libraries and all the data files. The proper name of the GNU make command is printed at the end of the configuration run, as in `"You must use gmake to compile ICU"`.
    Note that the compilation command output may be simplified on your platform. If this is the case, you will see just: `gcc ... stubdata.c` rather than `gcc -DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1 -D_REENTRANT -I../common -DU_ATTRIBUTE_DEPRECATED= -O2 -Wall -std=c99 -pedantic -Wshadow -Wpointer-arith -Wmissing-prototypes -Wwrite-strings -c -DPIC -fPIC -o stubdata.o stubdata.c`
    If you need to see the whole compilation line, use `gmake VERBOSE=1`. The full compilation line will print if an error occurs.
1.  Optionally,
```
gmake check
```
    will run the test suite, which checks for ICU's functionality integrity (See [testing note](#running-the-tests-from-the-command-line) below).
1.  To install,
```
gmake install
```
    to install ICU. If you used the `--prefix=` option on `configure` or `runConfigureICU`, ICU will be installed to the directory you specified. (See [installation note](#installing-icu) below).

### Configuring ICU

Type `"./runConfigureICU --help"` for help on how to run it and a list of supported platforms. You may also want to type `"./configure --help"` to print the available configure options that you may want to give `runConfigureICU`. If you are not using the `runConfigureICU` script, or your platform is not supported by the script, you may need to set your `CC`, `CXX`, `CFLAGS` and `CXXFLAGS` environment variables, and type `"./configure"`. HP-UX users, please see this [note regarding HP-UX multithreaded build issues](#using-icu-in-a-multithreaded-environment-on-hp-ux) with newer compilers. Solaris users, please see this [note regarding Solaris multithreaded build issues](#linking-on-solaris).

ICU is built with strict compiler warnings enabled by default. If this causes excessive numbers of warnings on your platform, use the `--disable-strict` option to configure to reduce the warning level.

### Running The Tests From The Command Line

You may have to set certain variables if you with to run test programs individually, that is apart from "gmake check". The environment variable **ICU_DATA** can be set to the full pathname of the data directory to indicate where the locale data files and conversion mapping tables are when you are not using the shared library (e.g. by using the .dat archive or the individual data files). The trailing "/" is required after the directory name (e.g. `$Root/source/data/out/` will work, but the value `$Root/source/data/out` is not acceptable). You do not need to set **ICU_DATA** if the complete shared data library is in your library path.

### Installing ICU

Some platforms use package management tools to control the installation and uninstallation of files on the system, as well as the integrity of the system configuration. You may want to check if ICU can be packaged for your package management tools by looking into the `packaging` directory. (Please note that if you are using a snapshot of ICU from Git, it is probable that the packaging scripts or related files are not up to date with the contents of ICU at this time, so use them with caution).

## How To Build And Install On z/OS (OS/390)

You can install ICU on z/OS or OS/390 (the previous name of z/OS), but IBM tests only the z/OS installation. You install ICU in a z/OS UNIX system services file system such as HFS or zFS. On this platform, it is important that you understand a few details:

*   The makedep and GNU make tools are required for building ICU. If it is not already installed on your system, it is available at the [z/OS UNIX - Tools and Toys](http://www-03.ibm.com/servers/eserver/zseries/zos/unix/bpxa1toy.html) site. The PATH environment variable should be updated to contain the location of this executable prior to build. Failure to add these tools to your PATH will cause ICU build failures or cause pkgdata to fail to run.
*   Since USS does not support using the mmap() function over NFS, it is recommended that you build ICU on a local filesystem. Once ICU has been built, you should not have this problem while using ICU when the data library has been built as a shared library, which is this is the default setting.
*   Encoding considerations: The source code assumes that it is compiled with codepage ibm-1047 (to be exact, the UNIX System Services variant of it). The pax command converts all of the source code files from ASCII to codepage ibm-1047 (USS) EBCDIC. However, some files are binary files and must not be converted, or must be converted back to their original state. You can use the [unpax-icu.sh](https://github.com/unicode-org/icu/blob/main/icu4c/as_is/os390/unpax-icu.sh) script to do this for you automatically. It will unpackage the tar file and convert all the necessary files for you automatically.
*   z/OS supports both native S/390 hexadecimal floating point and (with OS/390 2.6 and later) IEEE 754 binary floating point. This is a compile time option. Applications built with IEEE should use ICU DLLs that are built with IEEE (and vice versa). The environment variable IEEE390=0 will cause the z/OS version of ICU to be built without IEEE floating point support and use the native hexadecimal floating point. By default ICU is built with IEEE 754 support. Native floating point support is sufficient for codepage conversion, resource bundle and UnicodeString operations, but the Format APIs require IEEE binary floating point.
*   z/OS introduced the concept of Extra Performance Linkage (XPLINK) to bring performance improvement opportunities to call-intensive C and C++ applications such as ICU. XPLINK is enabled on a DLL-by-DLL basis, so if you are considering using XPLINK in your application that uses ICU, you should consider building the XPLINK-enabled version of ICU. You need to set ICU's environment variable `OS390_XPLINK=1` prior to invoking the make process to produce binaries that are enabled for XPLINK. The XPLINK option, which is available for z/OS 1.2 and later, requires the PTF PQ69418 to build XPLINK enabled binaries.
*   ICU requires XPLINK for the icuio library. If you want to use the rest of ICU without XPLINK, then you must use the --disable-icuio configure option.
*   The latest versions of z/OS use [XPLINK version (C128) of the C++ standard library](https://www.ibm.com/support/knowledgecenter/SSLTBW_2.2.0/com.ibm.zos.v2r2.cbcux01/oebind6.htm) by default. You may see [an error](https://www.ibm.com/support/knowledgecenter/SSLTBW_2.2.0/com.ibm.zos.v2r2.cbcux01/oebind5.htm) when running with XPLINK disabled. To avoid this error, set the following environment variable or similar:

```
export _CXX_PSYSIX="CEE.SCEELIB(C128N)":"CBC.SCLBSID(IOSTREAM,COMPLEX)"
```

*   When building ICU data, the heap size may need to be increased with the following environment variable:

```
export _CEE_RUNOPTS="HEAPPOOLS(ON),HEAP(4M,1M,ANY,FREE,0K,4080)"
```

*   The rest of the instructions for building and testing ICU on z/OS with UNIX System Services are the same as the [How To Build And Install On UNIX](#how-to-build-and-install-on-unix) section.

### z/OS (Batch/PDS) support outside the UNIX system services environment

By default, ICU builds its libraries into the UNIX file system (HFS). In addition, there is a z/OS specific environment variable (OS390BATCH) to build some libraries into the z/OS native file system. This is useful, for example, when your application is externalized via Job Control Language (JCL).

The OS390BATCH environment variable enables non-UNIX support including the batch environment. When OS390BATCH is set, the libicui18n_XX_.dll, libicuuc_XX_.dll, and libicudt_XX_e.dll binaries are built into data sets (the native file system). Turning on OS390BATCH does not turn off the normal z/OS UNIX build. This means that the z/OS UNIX (HFS) DLLs will always be created.

Two additional environment variables indicate the names of the z/OS data sets to use. The LOADMOD environment variable identifies the name of the data set that contains the dynamic link libraries (DLLs) and the LOADEXP environment variable identifies the name of the data set that contains the side decks, which are normally the files with the .x suffix in the UNIX file system.

A data set is roughly equivalent to a UNIX or Windows file. For most kinds of data sets the operating system maintains record boundaries. UNIX and Windows files are byte streams. Two kinds of data sets are PDS and PDSE. Each data set of these two types contains a directory. It is like a UNIX directory. Each "file" is called a "member". Each member name is limited to eight bytes, normally EBCDIC.

Here is an example of some environment variables that you can set prior to building ICU:

```
OS390BATCH=1
LOADMOD=_USER_.ICU.LOAD
LOADEXP=_USER_.ICU.EXP
```

The PDS member names for the DLL file names are as follows:

```
IXMI_XX_IN --> libicui18n_XX_.dll
IXMI_XX_UC --> libicuuc_XX_.dll
IXMI_XX_DA --> libicudt_XX_e.dll
```

You should point the LOADMOD environment variable at a partitioned data set extended (PDSE) and point the LOADEXP environment variable at a partitioned data set (PDS). The PDSE can be allocated with the following attributes:

```
Data Set Name . . . : USER.ICU.LOAD
Management class. . : **None**
Storage class . . . : BASE
Volume serial . . . : TSO007
Device type . . . . : 3390
Data class. . . . . : LOAD
Organization  . . . : PO
Record format . . . : U
Record length . . . : 0
Block size  . . . . : 32760
1st extent cylinders: 1
Secondary cylinders : 5
Data set name type  : LIBRARY
```

The PDS can be allocated with the following attributes:

```
Data Set Name . . . : USER.ICU.EXP
Management class. . : **None**
Storage class . . . : BASE
Volume serial . . . : TSO007
Device type . . . . : 3390
Data class. . . . . : **None**
Organization  . . . : PO
Record format . . . : FB
Record length . . . : 80
Block size  . . . . : 3200
1st extent cylinders: 3
Secondary cylinders : 3
Data set name type  : PDS
```

## How To Build And Install On The IBM i Family (IBM i, i5/OS OS/400)

Before you start building ICU, ICU requires the following:

*   QSHELL interpreter installed (install base option 30, operating system)
*   ILE C/C++ Compiler installed on the system
*   The latest IBM tools for Developers for IBM i — [https://www-356.ibm.com/partnerworld/wps/servlet/ContentHandler/pw_com_porting_tools_index](https://www-356.ibm.com/partnerworld/wps/servlet/ContentHandler/pw_com_porting_tools_index)

The following describes how to setup and build ICU. For background information, you should look at the [UNIX build instructions](#how-to-build-and-install-on-unix).

1.  Copy the ICU source .tgz to the IBM i environment, as binary. Also, copy the [unpax-icu.sh](https://github.com/unicode-org/icu/blob/main/icu4c/as_is/os400/unpax-icu.sh) script into the same directory, as a text file.
2.  Create target library. This library will be the target for the resulting modules, programs and service programs. You will specify this library on the OUTPUTDIR environment variable.
```
CRTLIB LIB(_libraryname_)
ADDENVVAR ENVVAR(OUTPUTDIR) VALUE('_libraryname_') REPLACE(*YES)
```
3.  Set up the following environment variables and job characteristics in your build process
```
ADDENVVAR ENVVAR(MAKE) VALUE('gmake') REPLACE(*YES)
CHGJOB CCSID(37)
```
4.  Fire up the QSH _(all subsequent commands are run inside the qsh session.)_
```
qsh
```
5.  Set up the PATH:
```
export PATH=/QIBM/ProdData/DeveloperTools/qsh/bin:$PATH:/QOpenSys/usr/bin
```
6.  Unpack the ICU source code archive:
```
gzip -d icu-_X_._Y_.tgz
```
7.  Run unpax-icu.sh on the tar file generated from the previous step.
```
unpax-icu.sh icu.tar
```
8.  Build the program ICULD which ICU will use for linkage.
```
cd icu/as_is/os400
qsh bldiculd.sh
cd ../../..
```
9.  Change into the 'source' directory, and configure ICU. (See [configuration note](#HowToConfigureICU) for details). Note that --with-data-packaging=archive and setting the --prefix are recommended, building in default (dll) mode is currently not supported.
```
cd icu/source
./runConfigureICU IBMi --prefix=_/path/to/somewhere_ --with-data-packaging=archive
```
10.  Build ICU.
> :point_right: **Note**: Do not use the -j option
```
gmake
```
11.  Test ICU.
```
gmake check
```
(The `QIBM_MULTI_THREADED=Y` flag will be automatically applied to intltest - you can look at the [iSeries Information Center](https://www.ibm.com/support/knowledgecenter/ssw_ibm_i_73/rzahw/rzahwceeco.htm) for more details regarding the running of multiple threads on IBM i.)

## How To Cross Compile ICU

This section will explain how to build ICU on one platform, but to produce binaries intended to run on another. This is commonly known as a cross compile.

Normally, in the course of a build, ICU needs to run the tools that it builds in order to generate and package data and test-data.  In a cross compilation setting, ICU is built on a different system from that which it eventually runs on. An example might be, if you are building for a small/headless system (such as an embedded device), or a system where you can't easily run the ICU command line tools (any non-UNIX-like system).

To reduce confusion, we will here refer to the "A" and the "B" system. System "A" is the actual system we will be running on - the only requirements on it is are it is able to build ICU from the command line targeting itself (with `configure` or `runConfigureICU`), and secondly, that it also contain the correct toolchain for compiling and linking for the resultant platform, referred to as the "B" system.

The autoconf docs use the term "build" for A, and "host" for B. More details at: [http://www.gnu.org/software/autoconf/manual/html_node/Specifying-Names.html](http://www.gnu.org/software/autoconf/manual/html_node/Specifying-Names.html#Specifying-Names)

Three initially-empty directories will be used in this example:

| **/icu**    | a copy of the ICU source                                                    |
| **/buildA** | an empty directory, it will contain ICU built for A  (MacOSX in this case)  |
| **/buildB** | an empty directory, it will contain ICU built for B  (HaikuOS in this case) |

1.  Check out or unpack the ICU source code into the `/icu` directory.You will have the directories `/icu/source`, etc.
2.  Build ICU in `/buildA` normally (using `runConfigureICU` or `configure`):
```
cd /buildA
sh /icu/source/runConfigureICU MacOSX
gnumake
```
3.  Set `PATH` or other variables as needed, such as `CPPFLAGS`.
4.  Build ICU in `/buildB`
```
cd /buildB
sh /icu/source/configure --host=i586-pc-haiku--with-cross-build=/buildA
gnumake
```
> :point_right: **Note**: `--with-cross-build` takes an absolute path.
5.  Tests and testdata can be built with `gnumake tests`.


* * *

Copyright © 2016 and later: Unicode, Inc. and others. License & terms of use: [http://www.unicode.org/copyright.html](http://www.unicode.org/copyright.html)
Copyright © 1997-2016 International Business Machines Corporation and others. All Rights Reserved.
