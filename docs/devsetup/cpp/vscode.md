---
layout: default
title: Configuring VS Code for ICU4C
grand_parent: Setup for Contributors
parent: C++ Setup
---

<!--- © 2020 and later: Unicode, Inc. and others. ---> 
<!--- License & terms of use: http://www.unicode.org/copyright.html --->

# Configuring VS Code for ICU4C

{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


  - Create a `.vscode` folder in icu4c/source
  - Copy the [`tasks.json`](tasks.json), [`launch.json`](launch.json) and [`c_cpp_properties.json`](c_cpp_properties.json) files into
    the `.vscode` folder.
  - To test only specific test targets, specify them under `args` in
    `launch.json`.
  - To adjust the parallelism when building, adjust the `args` in `tasks.json`.
    - `-l20` tells VSCode to not launch jobs if the system load average is above
      20 (note that the [system load
      average](https://en.wikipedia.org/wiki/Load_(computing)) is *not* a CPU
      usage percentage).
    - `-j24` limits the number of jobs launched in parallel to 24. The system
      load average takes a while to respond, reducing this number helps the
      initial bad system performance when a new build is launched.

NOTE:
Run the
[`./runConfigureICU` command](https://unicode-org.github.io/icu/userguide/icufaq)
before building `icu4c` from VSCode.
