---
layout: default
title: Source Code Setup
parent: Setup for Contributors
has_children: true
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Source Code Setup
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


> Announcement 07/16/2018: The ICU source code repository has been migrated from
> Subversion to Git, and is now hosted on GitHub.

## Quick Start

You can view ICU source code online: <https://github.com/unicode-org/icu>

***Make sure you have git lfs installed.*** See the following section.

For read-only usage, create a local clone:

```
git clone https://github.com/unicode-org/icu.git
```

or

```
git clone git@github.com:unicode-org/icu.git
```

This will check out a new directory `icu` which contains **icu4c** and
**icu4j** subdirectories as detailed below.

*For ICU development*, do *not* work directly with the Unicode ICU `main` branch!
See the [git for ICU Developers](../../userguide/dev/gitdev) page instead.

For cloning from your own fork, replace `unicode-org` with your GitHub user
name.

**For fetching just the files for an ICU release tag**, you can use a shallow
clone:

```
git clone https://github.com/unicode-org/icu.git --depth=1 --branch=release-63-1
```

If you already have a clone of the ICU repository, you can add and extract
release files like this:

```
mkdir /tmp/extracted-icu  # or wherever you want to extract to
cd  local-git-repo-top-level-dir
git fetch upstream
git tag --list "*63*"  # List tags relevant to ICU 63, e.g., release-63-1
git archive release-63-1 | tar -x -C /tmp/extracted-icu
```

## Detailed Instructions

### Prerequisites: Git and Git LFS

(Note: you do not need a [GitHub](http://github.com) *account* to download the
ICU source code. However, you might want such an account to be able to
contribute to ICU.)

*   Install a **git client**
    *   <https://git-scm.com/downloads>
    *   Linux: `sudo apt install git`
*   Install **git-lfs** if your git client does not already have LFS support
    (ICU uses git Large File Storage to store large binary content such as
    \*.jar files.)
    *   <https://git-lfs.github.com/>
        *   See also
            <https://help.github.com/articles/installing-git-large-file-storage/>
    *   Linux: `sudo apt install git-lfs`
    *   MacOS: Consider using Homebrew or MacPorts.
    *   The command `git lfs version` will indicate if LFS is installed.
*   Setup git LFS for your local user account once on each machine:
    *   `git lfs install --skip-repo`

### Working with git

There are many resources available to help you work with git, here are a few:

*   <https://git-scm.com/> - the homepage of the git project
*   <https://help.github.com/> - GitHub’s help page
*   <https://try.github.io/> - Resources to learn Git

Want to contribute back to ICU? See
[How to contribute](../../userguide/processes/contribute.md).

## Repository Layout

The top level
[README.md](https://github.com/unicode-org/icu#international-components-for-unicode)
contains the latest information about the repository’s layout. Currently:

*   **icu4c**/ ICU for C/C++
*   **icu4j**/ ICU for Java
*   **tools**/ Tools
*   **vendor**/ Vendor dependencies (copied here for reference)

### Tags and Branches

The repository is **tagged** with different release versions of ICU.

For example,
[release-55-1](https://github.com/unicode-org/icu/tree/release-55-1) is the tag
which corresponds to version 55.1 of ICU (for both C and J).

Branches in the main fork are used for maintenance branches of ICU.

For example,
[maint/maint-61](https://github.com/unicode-org/icu/tree/maint/maint-61) is a
branch containing the latest maintenance work on the 61.x line of ICU.

There are other tags and branches which may be cleaned up/deleted at any time.

*   branches/tags/releases from [before the icu4c and icu4j trees were
    merged](https://unicode-org.atlassian.net/browse/ICU-12800) - items prefixed
    with "icu-" are for icu4c, and "icu4j-" for icu4j, etc.
*   old personal work branches (with a person's username, such as **andy/6910**)
*   long running shared feature branches (In general, feature work is done on
    personal forks of the repository.)

See also the [Tips (for developers)](repository/tips/index.md) subpage.

## A Bit of History

ICU was first open sourced in 1999 using CVS and Jitterbug. The source files
were imported from other source control systems internal to IBM at that time.

The ICU project moved to using a Subversion source code repository and a Trac
bug database on Nov 30, 2006. These replace our original CVS source code
repository and Jitterbug bug data base. All history from the older systems has
been migrated into the new, so there should normally be no need to refer back to
Jitterbug or CVS.

In July 2018, the ICU project [moved
again](http://blog.unicode.org/2018/07/icu-moves-to-github-and-jira.html), this
time from svn to git on GitHub, and from trac to Atlassian Cloud Jira. Many
tools and much effort was involved in migration and testing. There is a
[detailed blog post](https://srl295.github.io/2018/07/02/icu-infra/) on the
topic (not an official ICU-TC document!) for those interested in the technical
details of this move.

