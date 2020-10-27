<!---
Copyright (C) 2018 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Commit Report
=============

Environment:
- Latest Commit: df72f8cbaf81cf9dcd94071961f198e1fa416ff8
- Jira Query: project=ICU AND fixVersion=68.1
- Rev Range: release-67-1..origin/maint/maint-68
- Authenticated: Yes

## Problem Categories
### Closed Issues with No Commit
Tip: Tickets with type 'Task' or 'User Guide' or resolution 'Fixed by Other Ticket' are ignored.

*Success: No problems in this category!*
### Closed Issues with Illegal Resolution or Commit
Tip: Fixed tickets should have resolution 'Fixed by Other Ticket' or 'Fixed'.
Duplicate tickets should have their fixVersion tag removed.
Tickets with resolution 'Fixed by Other Ticket' are not allowed to have commits.

*Success: No problems in this category!*

### Commits without Jira Issue Tag
Tip: If you see your name here, make sure to label your commits correctly in the future.

*Success: No problems in this category!*

### Commits with Jira Issue Not Found
Tip: Check that these tickets have the correct fixVersion tag.

*Success: No problems in this category!*

### Commits with Open Jira Issue
Tip: Consider closing the ticket if it is fixed.

#### Issue ICU-21249

- ICU-21249: `ICU 68 RC BRS`
	- Assigned to Markus Scherer
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21249

##### Commits with Issue ICU-21249

- d1dcb69 `ICU-21249 integrate CLDR release-38-beta2 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-10-09T14:54:02-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/d1dcb6931884dcf4b8b9a88fa17d19159a95a04c

- a3d83de `ICU-21249 Update instructions for generating C++ API change reports`
	- Authored by Craig Cornelius <cwcornelius@gmail.com>
	- Committed at 2020-10-07T21:24:21-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/a3d83dedbafc2a02838274dc9174780b127c9195

- 5de5cab `ICU-21249 Add new U_HIDE_INTERNAL_API guards`
	- Authored by Richard Gillam <richard_gillam@apple.com>
	- Committed at 2020-10-06T10:09:18-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/5de5cabfb06970d543e2b8f06959489ebd5810f8

- c0d4065 `ICU-21249 Adds error code check to prevent segmentation fault if`
	- Authored by Norbert Runge <41129501+gnrunge@users.noreply.github.com>
	- Committed at 2020-10-02T09:57:19-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/c0d4065607b9edde49a6799e609a33b87fe4409f

- c4fa504 `ICU-21249 BRS68RC Remaining updates for Readmes for ICU4C and ICU4J`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-10-01T17:57:16-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/c4fa504fec4e2308c0413fcc4f80224c6fa0a3ff

- ff7cc3f `ICU-21249 add unumberrangeformatter.h to API docs main page`
	- Authored by Markus Scherer <markus.icu@gmail.com>
	- Committed at 2020-10-01T15:37:42-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/ff7cc3f1a8837acbdf31ddb4bb3708fe76e0e230

- a61c6ff `ICU-21249 API Change Reports for C++`
	- Authored by Craig Cornelius <cwcornelius@gmail.com>
	- Committed at 2020-10-01T12:55:49-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/a61c6ff23aea3cae122a31ae6487255d4316dc58

- 6c96550 `ICU-21249 Fix warnings about uninitialized variables in locid.cpp`
	- Authored by Jeff Genovy <29107334+jefgen@users.noreply.github.com>
	- Committed at 2020-10-01T11:57:24-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/6c9655060b43763e8d4896e45d09bf1ed1df8e09

- 9b9db68 `ICU-21249 API signature file for ICU 68`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-10-01T14:35:53-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/9b9db682e1cfe1430ae0523372d4c9975f609cc1

- 1d3277b `ICU-21249 ICU4J 68 serialization test data`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-10-01T13:29:02-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/1d3277bfc23c91536b1bbebf75bf1dcbf1dd362e

- 69d1a46 `ICU-21249 Fixed warnings in last minute changes for 68RC`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-10-01T13:28:22-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/69d1a465dcb249c81f1d25cfe4a02af981571aaa

- 75e7e0b `ICU-21249 Change AppVeyor to not use parallel build due to Cygwin stability issues.`
	- Authored by Jeff Genovy <29107334+jefgen@users.noreply.github.com>
	- Committed at 2020-09-30T15:06:48-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/75e7e0bb08121fd90b0704d4b6dc54d99902c4ca

- 8caba0e `ICU-21249 Fixed some coding problems found by Spotbugs`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-09-30T15:55:29-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/8caba0e6c96be7850e0c13eecd1a142f8938e670

- 54e7bac `ICU-21249 Fixing java/javadoc compiler warnings.`
	- Authored by yumaoka <y.umaoka@gmail.com>
	- Committed at 2020-09-30T14:10:10-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/54e7bace04bd661676ea3290dc14bacfec06950e

- 61f0e16 `ICU-21249 Fixing javadoc errors`
	- Authored by yumaoka <y.umaoka@gmail.com>
	- Committed at 2020-09-30T00:57:10-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/61f0e16b11a709616630e3d9010ea2d22e349f28

- 2abe936 `ICU-21249 Fix API status tag issues`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-09-29T18:42:55-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/2abe9369a6efce93f0a05cd0106a61d18dee3c25

- b2edfb8 `ICU-21249 ICU 68 BRS - Clean up import statements`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-09-29T13:17:51-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/b2edfb89ccb534ff882954328c0d65d6721bd688

- 802aedc `ICU-21249 Update copy of LICENSE file in icu4j tree`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-09-29T12:14:25-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/802aedc6596e51baf3f61e76245f27a6c8afc6ab

- b11e4bc `ICU-21249 ICU4J 68 API status updates`
	- Authored by Yoshito Umaoka <yumaoka@users.noreply.github.com>
	- Committed at 2020-09-29T09:12:29-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/b11e4bcba1103c08e2fcb501e90f7a9905ce1e01

- 1ff371c `ICU-21249 ICU4C API promotions`
	- Authored by Rich Gillam <62772518+richgillam@users.noreply.github.com>
	- Committed at 2020-09-28T17:19:04-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/1ff371c2257bdcccbabdde61f77224b68fc0e295

- 1c8bc80 `ICU-21249 restore tests of Version resources`
	- Authored by Markus Scherer <markus.icu@gmail.com>
	- Committed at 2020-09-25T12:09:38-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/1c8bc8078939f01ec9763db2586627d69bc3cea1

- 43aa7dd `ICU-21249 integrate CLDR release-38-beta to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-09-25T09:33:13-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/43aa7ddda21994f1199f925b154dc784d452f3c6

- 4881333 `ICU-21249 BRS68rc update urename.h`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-09-23T16:57:10-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/4881333155ab49cc0937562182351d68ffa937ac

- 6a78e67 `ICU-21249 BRS68RC: Fix ICU4C Samples.`
	- Authored by Jeff Genovy <29107334+jefgen@users.noreply.github.com>
	- Committed at 2020-09-23T14:52:07-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/6a78e6799543ecb30664418212436447f8842801

- 8dc8552 `ICU-21249 add hide-draft guards for some @draft ICU 68 API`
	- Authored by Markus Scherer <markus.icu@gmail.com>
	- Committed at 2020-09-20T21:04:41-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/8dc85529e1298c9902bad8ed9de6d45710e22809

- 10face8 `ICU-21249 Adds !UCONFIG_NO_BREAK_ITERATION around selected test data items.`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-17T13:22:05-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/10face81400f31175e9352eb3e1075fa2bad386f

- 4583c1e `ICU-21249 Bump ant from 1.10.6 to 1.10.8 in /tools/cldr/cldr-to-icu`
	- Authored by dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
	- Committed at 2020-09-17T11:34:20-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/4583c1e77cba4d62dc5a538bead88bd48c5ad493

- 7888b23 `ICU-21249 integrate CLDR release-38-alpha2 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-09-16T15:10:05-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/7888b23e875cd967e8d33577c28319054cc7ac0f

- 3a55ce0 `ICU-21249 Adds #if !UCONFIG_NO_FORMATTING around all code in number_symbolswrapper.cpp.`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-16T09:19:03-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/3a55ce096af22579b4ec26a6c5eabe52ac2a04f6

- 215ca37 `ICU-21249 Adds #if !UCONFIG_NO_BREAK_ITERATOR to prevent compilation`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-16T09:18:40-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/215ca37e4b47e58d3e3f886938ebdc64d7ac485b

- b41f120 `ICU-21249 Fix TODOs to reference ICU-21284 instead of ICU-20920`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-15T12:27:44-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/b41f12065f430c1ff55ae921cf0722e05eba54fe

- 74b7882 `ICU-21249 Remove obsolete TODO for ICU-13591`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-15T12:27:44-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/74b7882a86ad5cc8e2252d6be298686ca7621975

- 7111def `ICU-21249 Fix logKnownIssue for ICU-13574`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-15T12:27:44-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/7111def4942c48104b5da5e2d4e5d14691b841a4

- 1baf0ea `ICU-21249 Fix common/uniquecharstr.h to pass the internal header check: include uassert.h needed.`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-10T15:09:09-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/1baf0ea9b9f20b2f30d82e612dd26525751cadfb

- b066f65 `ICU-21249 integrate CLDR release-38-alpha1 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-09-04T15:05:22-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/b066f65a506c2b6709ebdd364736e1d193bc5210

- 82545ec `ICU-21249 Update instructions in StaticUnicodeSetsTest`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-03T12:29:44+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/82545ecc2cbb9ab0598cf8c1fc9e4a9ffcc115c7

- 28707b4 `ICU-21249 Patch for failing exhastive unit tests`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-03T12:29:44+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/28707b44b4808b4e7bb2cd2c63a588f5322fac31

- fcc3bcb `ICU-21249 Update numberpermutationtest.txt`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-03T00:46:59+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/fcc3bcb43ecea0774fd71ede4b19cd01e254a05d

- e618a1c `ICU-21249 integrate CLDR release 38 alpha0 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-09-02T10:23:14-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/e618a1cc2db6ac067a829c3d472bb9db123d5ecc

- ba3b6ac `ICU-21249 BRS68RC Version update and regenerate configure for v68`
	- Authored by Erik Torres <26077674+erik0686@users.noreply.github.com>
	- Committed at 2020-08-31T19:35:14-04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/ba3b6acd5c2599466c3b3fb460212c438f556768

- 4767be7 `ICU-21249 Updating double-conversion for ICU 68`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-08-26T19:09:54-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/4767be7f4ff361612a5d62caa92884cfa9d8c224

#### Issue ICU-21250

- ICU-21250: `ICU 68.1 Release BRS`
	- Assigned to Markus Scherer
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21250

##### Commits with Issue ICU-21250

- c7024d5 `ICU-21250 Update ICU4J and ICU4C APIChangeReports`
	- Authored by Craig Cornelius <cwcornelius@gmail.com>
	- Committed at 2020-10-23T22:18:19-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/c7024d5faa366f1f26a5b6e4fa974acdc5a6790f

- 97eec04 `ICU-21250 integrate CLDR release-38-final0 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-10-23T08:45:50-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/97eec046dfa789e51488b47fc3439d8d3d9c8cb8

- acd571a `ICU-21250 integrate CLDR release-38-beta3 to ICU trunk`
	- Authored by Peter Edberg <pedberg@unicode.org>
	- Committed at 2020-10-19T19:27:52-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/acd571a4d2297c7f8ba7b4e7892f2e612cfe519f

- 93deb0e `ICU-21250 BRS68GA Update version numbers for 68GA and regenerate jar files`
	- Authored by Erik Torres Aguilar <26077674+erik0686@users.noreply.github.com>
	- Committed at 2020-10-14T11:38:45-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/93deb0e4487164e4bf12a3e7ecc5afb222a0aad0

#### Issue ICU-21308

- ICU-21308: `MeasureUnitImpl$UnitParser thread safety issue`
	- Assigned to Younies Mahmoud
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21308

##### Commits with Issue ICU-21308

- dd87efa `ICU-21308 Fix icu4j build for Java 7 in MeasureUnitImpl`
	- Authored by younies <younies@chromium.org>
	- Committed at 2020-10-22T21:31:31+04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/dd87efa2994cf47f390e2c6a3af65dae4a68e898

- 1a47279 `ICU-21308 Fix units parser thread safety issue`
	- Authored by younies <younies@chromium.org>
	- Committed at 2020-10-20T20:10:47+04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/1a4727924ff4823c6fc58f8b1ba6d70b3475bde0

#### Issue ICU-21344

- ICU-21344: `AliasReplacer::replaceTerritory calls the Locale constructor with wrong arguments`
	- Assigned to Frank Yung-Fong Tang
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21344

##### Commits with Issue ICU-21344

- 9a82de7 `ICU-21344 merge localebuilder into resourcebundle`
	- Authored by Frank Tang <ftang@chromium.org>
	- Committed at 2020-10-22T17:44:34-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/9a82de789f7a8afc77673d3fcf719a50e51e56ff

- 9ab5487 `ICU-21344 Fix wrong passing of script in Locale.`
	- Authored by Frank Tang <ftang@chromium.org>
	- Committed at 2020-10-21T16:46:01-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/9ab5487eb8eae94dcacb810c6ed3318f7f3c1262


## Total Problems: 4
