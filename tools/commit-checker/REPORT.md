<!---
Copyright (C) 2018 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Commit Report
=============

Environment:
- Latest Commit: 5de5cabfb06970d543e2b8f06959489ebd5810f8
- Jira Query: project=ICU AND fixVersion=68.1
- Rev Range: release-67-1..origin/master
- Authenticated: Yes

## Problem Categories
### Closed Issues with No Commit
Tip: Tickets with type 'Task' or 'User Guide' or resolution 'Fixed by Other Ticket' are ignored.

- ICU-21070: `The Locale Explorer Help page still has the IBM logo`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21070

### Closed Issues with Illegal Resolution or Commit
Tip: Fixed tickets should have resolution 'Fixed by Other Ticket' or 'Fixed'.
Duplicate tickets should have their fixVersion tag removed.
Tickets with resolution 'Fixed by Other Ticket' are not allowed to have commits.

- ICU-21070: `The Locale Explorer Help page still has the IBM logo`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21070


### Commits without Jira Issue Tag
Tip: If you see your name here, make sure to label your commits correctly in the future.

*Success: No problems in this category!*

### Commits with Jira Issue Not Found
Tip: Check that these tickets have the correct fixVersion tag.

#### Issue ICU-21137

- ICU-21137: `Improve VSCode configuration for ICU4C build and test`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21137

##### Commits with Issue ICU-21137

- 8e26518 `ICU-21137 Adjust VSCode IDE settings and README.`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-06-18T03:15:42+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/8e2651828a1177495e0d252bb9eddf7c64500703

#### Issue ICU-21270

- ICU-21270: `test failure with "fr" plural rules case "many": computeLimited == isLimited`
	- Assigned to Elango Cheran
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21270

##### Commits with Issue ICU-21270

- 6700602 `ICU-21270 Support exponent in FixedDecimal and samples in C++`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-10-02T09:21:29-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/6700602974c0a7ece4c2adced6629010dd1f7bc1

- 4723001 `ICU-21270 Update FixedDecimal in Java to support exponent`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-09-22T21:16:12-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/47230019c6e43f5924ca3325aaea3492cfa96a69


### Commits with Open Jira Issue
Tip: Consider closing the ticket if it is fixed.

#### Issue ICU-20717

- ICU-20717: `remove more files from git-lfs`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-20717

##### Commits with Issue ICU-20717

- 6b35e70 `ICU-20717 move small files out of lfs: .dat, .gz, .gif`
	- Authored by Steven R. Loomis <srl295@gmail.com>
	- Committed at 2020-09-17T11:09:48-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/6b35e70b680c403230c13df6e433aaf4c8c53fa9

#### Issue ICU-20963

- ICU-20963: `Add .clang-format file to icu4c source to unify the style among icu4c`
	- Assigned to Younies Mahmoud
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-20963

##### Commits with Issue ICU-20963

- 692005d `ICU-20963 Remove executable bit from .clang-format file.`
	- Authored by Fredrik Roubert <roubert@google.com>
	- Committed at 2020-10-01T10:58:04-05:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/692005d3485afdaeb090015dee518c0c815c1161

- 1574782 `ICU-20963 Add .clang-format file to icu4c/source`
	- Authored by younies <younies@chromium.org>
	- Committed at 2020-09-01T21:19:06+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/15747825bc5024dddf0bc3c6d0d1a22a1cd1d950

#### Issue ICU-21010

- ICU-21010: `ICU4J: Add support for CLDR 37 unit identifiers`
	- Assigned to Younies Mahmoud
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21010

##### Commits with Issue ICU-21010

- 9a06bdb `ICU-21010 add the @draft to the public methods`
	- Authored by younies <younies@chromium.org>
	- Committed at 2020-09-26T22:17:50+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/9a06bdb651ee5571cc660ed624b92d79ef4b5e26

- d149031 `ICU-21010 MeasureUnit extension in Java`
	- Authored by younies <younies@chromium.org>
	- Committed at 2020-09-14T21:24:14+04:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/d1490314337ac074166adbad86d2416fba41c8f5

#### Issue ICU-21078

- ICU-21078: `Better instructions for installing CLDR JARs and managing Maven build.`
	- Assigned to David Beaumont
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21078

##### Commits with Issue ICU-21078

- 85aee40 `ICU-21078 Improve instructions and gitignore files for cldr-to-icu.`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-06-16T20:09:46+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/85aee40cc3b25eeaccba6ef59fca124fd9ee5100

- bf9421f `ICU-21078 Adding missing copyright notice (sorry!)`
	- Authored by David Beaumont <david.beaumont+github@gmail.com>
	- Committed at 2020-04-22T01:33:40+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/bf9421f8e48abf5455cc9fa11d69c015c34eebb4

- b0fb483 `ICU-21078 Adding script and updating docs for CLDR jars`
	- Authored by David Beaumont <dbeaumont@google.com>
	- Committed at 2020-04-16T20:14:24+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/b0fb4839c24b743a6786b8c6b3a7e066478f0bad

#### Issue ICU-21098

- ICU-21098: `ICU4C logKnownIssue does not return false for closed issues`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21098

##### Commits with Issue ICU-21098

- 4231ca5 `ICU-21098 fix ticket URLs for logKnownIssue tickets.`
	- Authored by Steven R. Loomis <srloomis@us.ibm.com>
	- Committed at 2020-05-20T15:58:51-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/4231ca5be053a22a1be24eb891817458c97db709

#### Issue ICU-21135

- ICU-21135: `Pseudo locale functionality broken for Android integration.`
	- Assigned to David Beaumont
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21135

##### Commits with Issue ICU-21135

- 87bbd3d `ICU-21135 Fix pseudo locales to filter only non-root paths and avoid aliases.`
	- Authored by David Beaumont <dbeaumont@google.com>
	- Committed at 2020-06-03T17:32:23+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/87bbd3d067f32375d939b35df8a0dc5a2e9e7771

#### Issue ICU-21137

- ICU-21137: `Improve VSCode configuration for ICU4C build and test`
	- Assigned to Steven R. Loomis
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21137

##### Commits with Issue ICU-21137

- 8e26518 `ICU-21137 Adjust VSCode IDE settings and README.`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-06-18T03:15:42+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/8e2651828a1177495e0d252bb9eddf7c64500703

#### Issue ICU-21248

- ICU-21248: `Automate selected BRS tasks `
	- Assigned to Norbert Runge
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21248

##### Commits with Issue ICU-21248

- 5a92254 `ICU-21248 Adds line ending check to Travis CI check. The eol check fits`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-24T09:56:55-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/5a922544be05285a7408a9f1c99499e5205039d0

- 5e0cec2 `ICU-21248 Adds source file check (UTF-8 and absence of BOM) to`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-18T13:17:50-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/5e0cec2c2b3426d633e7a65899abbad1f9ad3b4c

- 7bdc26e `ICU-21248 Adds internal header check to Travis Continued Integration.`
	- Authored by gnrunge <nrunge@google.com>
	- Committed at 2020-09-15T09:33:08-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/7bdc26e2a1fe665bae70c44e8ade14d4b0925125

#### Issue ICU-21249

- ICU-21249: `ICU 68 RC BRS`
	- Assigned to Markus Scherer
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21249

##### Commits with Issue ICU-21249

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

#### Issue ICU-21258

- ICU-21258: `StandardPlural cannot handle plural keywords '1' or '0'`
	- Assigned to Shane Carr
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21258

##### Commits with Issue ICU-21258

- 4f18ef2 `ICU-21258 Refactor code and tests for compact data known issue`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-09-03T16:17:40-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/4f18ef2ef857738e977c211873a7b157bcdb87ea

#### Issue ICU-21266

- ICU-21266: `Improve NumberFormatter .unit().perUnit() and skeleton behaviour`
	- Assigned to Hugo van der Merwe
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21266

##### Commits with Issue ICU-21266

- a84fdd0 `ICU-21266 Support toSkeleton() for all functional Unit Formatters`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-10-01T02:51:27+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/a84fdd0e903fb20acd93ed186a0da4c0c071a0e6

#### Issue ICU-21270

- ICU-21270: `test failure with "fr" plural rules case "many": computeLimited == isLimited`
	- Assigned to Elango Cheran
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21270

##### Commits with Issue ICU-21270

- 6700602 `ICU-21270 Support exponent in FixedDecimal and samples in C++`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-10-02T09:21:29-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/6700602974c0a7ece4c2adced6629010dd1f7bc1

- 4723001 `ICU-21270 Update FixedDecimal in Java to support exponent`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-09-22T21:16:12-07:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/47230019c6e43f5924ca3325aaea3492cfa96a69

#### Issue ICU-21304

- ICU-21304: `ICU4C links need porting to new ICU user guide location`
	- Assigned to Hugo van der Merwe
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21304

##### Commits with Issue ICU-21304

- a08ac00 `ICU-21304 Update old userguide links to unicode-org.github.io`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-10-01T01:28:05+02:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/a08ac00c6757c117f33c319f50388c869b730bfe


## Total Problems: 17
