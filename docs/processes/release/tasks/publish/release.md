---
layout: default
title: Publish - Version 76.1 - WIP
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 90
---

<!--
© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Publish - Version 76.1 - WIP
{: .no_toc }

## Contents
{: .no_toc .text-delta }

---

## Most artifacts are now built in the GitHub CI. Work in Progress!!!

Many of the tasks that used to be done before "by hand" are now at least
partially done by GitHub Actions.

The release requires (for now) triggering the actions "by hand".

First go to [Github - unicode-org/icu](https://github.com/unicode-org/icu).

Create a release, give it a tag (something like `release-76` or `release-74-2`) \
Make sure the release is **DRAFT**.

See [Release & Milestone Tasks - Tagging](index.md#tagging) for details.

That tag will need to be passed to several of the actions below.

Go to [Github - unicode-org/icu](https://github.com/unicode-org/icu) -- Actions
and select the action to run from the left side.

Select an action and (from the right side) select "Run workflow".

Some actions will have an "Run the tests." option. \
**KEEP IT ON!** It is there for development, but you MUST run the tests for release.

Most will have a "Release tag to upload to." option. \
Here you should use the release tag.

1. **GHA ICU4C** \
   This will create and add to release: \
   * The Windows binaries (`icu4c-{icuver}-Win32-MSVC20??.zip`,
   `icu4c-{icuver}-Win64-MSVC20??.zip`, `icu4c-{icuver}-WinARM64-MSVC20??.zip`)
   * The packaged data for ICU4X (`icuexportdata_tag-goes-here.zip`)

1. **Release - ICU4C artifacts on Fedora** (`release-icu4c-fedora.yml`) \
   This will create and add to release:
   * `icu4c-{icuver}-Fedora_Linux??-x64.tgz`.

1. **Release - ICU4C artifacts on Ubuntu** (`release-icu4c-ubuntu.yml`) \
   This will create and add to release:
   * The `icu4c-{icuver}-Ubuntu??.04-x64.tgz` file
   * The icu4c data files (`icu4c-{icuver}-data.zip`,
     `icu4c-{icuver}-data-bin-b.zip`, `icu4c-{icuver}-data-bin-l.zip`)
   * The icu4c source archives (`icu4c-{icuver}-src.tgz` and `icu4c-{icuver}-src.zip`)
   * The ICU4C documentation (`icu4c-76_1-docs.zip`) \
   **WARNING:** this is also the one to be published (unpacked) for web access

1. **Release - ICU4J publish to Maven Central** (`release-icu4j-maven.yml`) \
   This will create, publish to Maven Cental (using Sonatype), and add to release:
   * All the official Maven artifacts, including sources and javadoc. \
     The Maven Central artifacts have checksums and are digitally signed. \
     Someone with access to Sonatype Nexus should still login there and authorize
     the promotion to Maven Central.
   * The unified Java documentation, (`icu4j-76.1-fulljavadoc.jar`) \
     **WARNING:** this is also the one to be published (unpacked) for web access

1. **Release - Create checksums and GPG sign** (`release-check-sign.yml`) \
   THIS SHOULD BE THE LAST ACTION YOU RUN. \
   After all the artifacts from the previous steps are posted to the release. \
   The action will download all the artifacts from release,
   create checksum files (`SHASUM512.txt` and `*.md5`),
   and digital signature files (`*.asc`)
