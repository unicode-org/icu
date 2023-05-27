---
layout: default
title: Java Profiling and Monitoring tools
grand_parent: Setup for Contributors
parent: Java Setup
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Java Profiling and Monitoring tools
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


There are many Java development tools available for analyzing Java application
run time performance. Eclipse has a set of plug-ins called TPTP which provides
Java application profiling/monitoring framework. However, TPTP is very slow and
I experienced frequent crash while profiling ICU4J codes. For ICU4J development,
I recommend several tools described below.

## VisualVM

VisualVM is available as a separate download since JDK 9. You can download the latest
version from here - <https://visualvm.github.io/download.html>
There is an Eclipse plug-in, which allow you to launch VisualVM when you run a
Java app on Eclipse. You can monitor CPU usage of the Java app, Memory usage
(heap/permgen), classes loaded, etc in GUI. You can also get basic profiling
information, such as CPU usage by class, memory allocations and generate heap
dump, force GC etc.
