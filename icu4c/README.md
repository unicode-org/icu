# International Components for Unicode
# ICU 63.1 ReadMe

_Last updated: 2018-Oct-18_

Copyright &copy; 2016 and later: Unicode, Inc. and others. License & terms
of use:  
[http://www.unicode.org/copyright.html](http://www.unicode.org/copyright.html)  
Copyright &copy; 1997-2016 International Business Machines Corporation and
others. All Rights Reserved.

---

## Table of Contents

- [Introduction](#introduction)
- [Getting Started](#getting-started)
- [What Is New In This Release?](#what-is-new-in-this-release)
- [How To Download the Source Code](#how-to-download-the-source-code)
- [ICU Source Code Organization](#icu-source-code-organization)
- [How To Build And Install ICU](#how-to-build-and-install-icu)
    - [Recommended Build Options](#recommended-build-options)
    - [User-Configurable Settings](#user-configurable-settings)
    - [Windows](#windows)
    - [Cygwin](#cygwin)
    - [UNIX](#unix)
    - [z/OS (os/390)](#HowToBuildZOS)
    - [IBM i family (IBM i, i5/OS, OS/400)](#HowToBuildOS400)
    - [How to Cross Compile ICU](#how-to-cross-compile-icu)
- [How To Package ICU](#how-to-package-icu)
- [Important Notes About Using ICU](#important-notes-about-using-icu)
    - [Using ICU in a Multithreaded
      Environment](#using-icu-in-a-multithreaded-environment)
    - [Windows Platform](#windows-platform)
    - [UNIX Type Platforms](#unix-type-platforms)
- [Platform Dependencies](#platform-dependencies)
    - [Porting To A New Platform](#porting-to-a-new-platform)
    - [Platform Dependent Implementations](#platform-dependent-implementations)

---

## Introduction

Today's software market is a global one in which it is desirable to develop and
maintain one application (single source/single binary) that supports a wide
variety of languages. The International Components for Unicode (ICU) libraries
provide robust and full-featured Unicode services on a wide variety of platforms
to help this design goal. The ICU libraries provide support for:
- The latest version of the Unicode standard
- Character set conversions with support for over 220 codepages
- Locale data for more than 300 locales
- Language sensitive text collation (sorting) and searching based on the Unicode
  Collation Algorithm (=ISO 14651)
- Regular expression matching and Unicode sets
- Transformations for normalization, upper/lowercase, script transliterations
  (50+ pairs)
- Resource bundles for storing and accessing localized information
- Date/Number/Message formatting and parsing of culture specific input/output
  formats
- Calendar specific date and time manipulation
- Text boundary analysis for finding characters, word and sentence boundaries

ICU has a sister project ICU4J that extends the internationalization
capabilities of Java to a level similar to ICU. The ICU C/C++ project is also
called ICU4C when a distinction is necessary.

## Getting started

This document describes how to build and install ICU on your machine. For other
information about ICU please see the following table of links.  
The ICU homepage also links to related information about writing
internationalized software.

**Here are some useful links regarding ICU and internationalization in
general:**
- [ICU, ICU4C & ICU4J Homepage](http://icu-project.org/)
- [FAQ - Frequently Asked Questions about
  ICU](http://userguide.icu-project.org/icufaq)
- [ICU User's Guide](http://userguide.icu-project.org/)
- [How To Use ICU](http://userguide.icu-project.org/howtouseicu)
- [Download ICU Releases](http://site.icu-project.org/download)
- [ICU4C API Documentation Online](http://icu-project.org/apiref/icu4c/)
- [Online ICU Demos](http://demo.icu-project.org/icu-bin/icudemos)
- [Contacts and Bug Reports/Feature
  Requests](http://site.icu-project.org/contacts)

**Important:** Please make sure you understand the [Copyright and License
Information](http://source.icu-project.org/repos/icu/trunk/icu4c/LICENSE).

## What Is New In This Release?

See the [ICU 63 download page](http://site.icu-project.org/download/63) for an
overview of this release, important changes, new features, bug fixes, known
issues, changes to supported platforms and build environments, and migration
issues for existing applications migrating from previous ICU releases.  
See the [API Change Report](APIChangeReport.html) for a complete list of APIs
added, removed, or changed in this release.  
For changes in previous releases, see the main [ICU download
page](http://site.icu-project.org/download) with its version-specific subpages.

## How To Download the Source Code

There are two ways to download ICU releases:
- **Official Release Snapshot:**  
If you want to use ICU (as opposed to developing it), you should download an
official packaged version of the ICU source code. These versions are tested more
thoroughly than day-to-day development builds of the system, and they are
packaged in zip and tar files for convenient download. These packaged files can
be found at
[http://site.icu-project.org/download](http://site.icu-project.org/download).  
The packaged snapshots are named **icu-nnnn.zip** or **icu-nnnn.tgz**, where
nnnn is the version number. The .zip file is used for Windows platforms, while
the .tgz file is preferred on most other platforms.  
Please unzip this file.
- **GitHub Source Repository:**  
If you are interested in developing features, patches, or bug fixes for ICU, you
should probably be working with the latest version of the ICU source code. You
will need to clone and checkout the code from our GitHub repository to ensure
that you have the most recent version of all of the files. See our [source
repository](http://site.icu-project.org/repository) for details.
