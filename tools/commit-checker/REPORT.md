<!---
Copyright (C) 2018 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Commit Report
=============

Environment:
- Latest Commit: 3172958d56eeab3bc6bfd87d09190e951afe439e
- Jira Query: project=ICU AND fixVersion=68.2
- Rev Range: release-68-1..ICU-21405-cherrypick
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

#### Issue ICU-21363

- ICU-21363: `IBM's XL compiler can't build c++11 with xlC_r`
	- Assigned to Yoshito Umaoka
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21363

##### Commits with Issue ICU-21363

- ea75e3e `ICU-21363 Resurrect support for IBM's XL compiler`
	- Authored by yumaoka <y.umaoka@gmail.com>
	- Committed at 2020-12-09T12:39:28-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/ea75e3ecce7951fa3caf6a7bfb8d545b3927e5c7

#### Issue ICU-21383

- ICU-21383: `ListFormat crash in nextPositionImpl when there are 9 or more items `
	- Assigned to Shane Carr
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21383

##### Commits with Issue ICU-21383

- e7f6673 `ICU-21383 Fix memory problem in FormattedStringBuilder`
	- Authored by Shane F. Carr <shane@unicode.org>
	- Committed at 2020-11-11T09:40:12-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/e7f66732f8fbd40992d85a85154755f1b792f1be

#### Issue ICU-21387

- ICU-21387: `Number and Date Format in 68 is  slower than 67`
	- Assigned to Hugo van der Merwe
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21387

##### Commits with Issue ICU-21387

- 420369a `ICU-21387 measunit: check for nullptr before calling delete`
	- Authored by Hugo van der Merwe <17109322+hugovdm@users.noreply.github.com>
	- Committed at 2020-12-02T13:11:33-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/420369aa97f981a1b2b3efe1f812edeebc06af1f

#### Issue ICU-21394

- ICU-21394: `.unit(NoUnit.BASE) doesn't unset the unit`
	- Assigned to Shane Carr
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21394

##### Commits with Issue ICU-21394

- f48165d `ICU-21394 Don't use null to test for unset ICU4J NumberFormatter options`
	- Authored by Shane F. Carr <sffc@google.com>
	- Committed at 2020-11-16T15:13:40-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/f48165d9ccf36b9520f6599177458fe6c467e215

#### Issue ICU-21405

- ICU-21405: `ICU 68.2 BRS`
	- Assigned to Markus Scherer
	- Jira Link: https://unicode-org.atlassian.net/browse/ICU-21405

##### Commits with Issue ICU-21405

- 33157f8 `ICU-21405 Update ICU4J Readme for BRS 68.2`
	- Authored by Elango Cheran <elango@unicode.org>
	- Committed at 2020-12-09T12:39:15-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/33157f820a099b647a7d86302149666943dadbd3

- 7542382 `ICU-21405 BRS 68.2 Version update and regenerate configure for v68.2`
	- Authored by Erik Torres Aguilar <ertorres@microsoft.com>
	- Committed at 2020-12-09T12:39:07-06:00
	- GitHub Link: https://github.com/unicode-org/icu/commit/7542382508bfb0bee1f7868e93d7f4abdb6a2d70


## Total Problems: 5
