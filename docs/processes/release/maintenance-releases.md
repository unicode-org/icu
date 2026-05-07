---
layout: default
title: Maintenance Release Procedure
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 75
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Maintenance Release Procedure

When a critical problem is found in ICU libraries, we try to fix the problem in
the latest development stream first. If there is a demand for the fix in a past
release, an ICU project developer may escalate the fix to be integrated in the
release to the ICU project management committee. Once the committee approved to
merge the fix into back level stream, the developer can merge the bug fix back
to the past release suggested by the committee. This merge activity must be
tracked by maintenance release place holder tickets and the developer should
provide original ticket number and description as the response in each
maintenance ticket. These fixes are automatically included in a future ICU
maintenance release.

## Place Holder Ticket

Once a major version of ICU library is released, we create maintenance release
place holder tickets for the major release (one for C, one for J). The ticket
should have subject: "ICU4\[C|J\] m.n.X". For example, after ICU 4.8 release, we
create two tickets - "ICU4C 4.8.X" and "ICU4J 4.8.X". These tickets must use the
target milestone - "maintenance-release".

## Maintenance Release

When the ICU project committee agree on releasing a new maintenance release, the
corresponding place holder ticket will be promoted to a real maintenance release
task ticket. This is done by following steps.

*   Create the new actual maintenance release milestone (e.g. 4.8.1)
*   Change the place holder ticket's subject to the actual version (e.g. "ICU4C
    4.8.X" -> "ICU4C 4.8.1")
*   Retarget the place holder ticket to the actual release (e.g.
    "maintenance-release" -> "4.8.1")
*   Create a new place holder ticket for future release (e.g. new ticket "ICU4C
    4.8.X", milestone: "maintenance-release")
