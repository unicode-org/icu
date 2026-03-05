---
layout: default
title: CI Reports
parent: Contributors
---

# CI Reports
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

<!--
© 2026 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

ICU is tested thoroughly through a variety of tests
(code coverage, quality control, etc.). \
Some of these produce reports that can be accessed here.

* ICU4J
  * Errorprone
    * [HTML report, all issues together in one big table](reports/icu4j_errorprone/errorprone1.html)
    * [HTML report, issues grouped by severity and then by type](reports/icu4j_errorprone/errorprone2.html)
    * [Markdown report](reports/icu4j_errorprone/errorprone.md)
    * [TSV report, for tooling or importing into a spreadsheet application](reports/icu4j_errorprone/errorprone.tsv)
  * Code Coverage using JaCoCo
    * [HTML report](reports/icu4j_coverage/html)
    * [XML report, for tooling](reports/icu4j_coverage/jacoco.xml) \
      WARNING, this is big, and will take a long time if opened in browser.
    * [CSV report, for tooling or importing into a spreadsheet application](reports/icu4j_coverage/jacoco.csv)
