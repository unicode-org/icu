<!---
Copyright (C) 2018 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html 
-->

# Commit Checker Tool

This tool checks the ICU Git repository against the ICU Jira issue tracker to ensure that the two are consistent with one another.

Author: Shane Carr

## Installation

Install `pipenv` globally:

    $ sudo pip3 install pipenv

Install this project's dependencies locally:

    $ pipenv install

Optional: save your Jira credentials in a `.env` file in this directory:

    JIRA_USERNAME=hello@example.com
    JIRA_PASSWORD=world

This is required if you want to process sensitive tickets.  Note: JIRA_PASSWORD needs to be an API Token generated according to the following instructions:

https://confluence.atlassian.com/cloud/api-tokens-938839638.html

## Usage

Make sure you have updated your repository:

    $ git fetch --tags upstream

Run the tool and save the result into REPORT.md; set fixVersion to the upcoming ICU version, and take the revision range between the previous release and the tip for the upcoming release:

    $ pipenv run python3 check.py \
        --jira-query "project=ICU AND fixVersion=64.1" \
        --rev-range "release-63-1..upstream/maint/maint-64"
        > REPORT.md

If the maintenance branch hasn't been cut yet, use upstream/master as the tip:

        --rev-range "release-64-2..upstream/master"

Note 1: These examples assume that your remote named "upstream" points to unicode-org/icu, the source of truth.

Note 2: Please change the previous-release tag (release-63-1, release-64-2, etc) to the correct version at the time you run the tool!

### Preview the Report

To preview the report, render the Markdown file in your favorite Markdown preview tool, like grip:

    $ pip3 install grip
    $ grip REPORT.md
     * Running on http://localhost:6419/ (Press CTRL+C to quit)
 
### Sending for Review
 
Before sending the report to ICU-TC, do some basic cleanup yourself by adjusting fix versions and resolutions on Jira issues.

- Tickets with commits should be closed as Fixed, and fixed tickets without commits should be closed as Fixed by Other Ticket.  These tickets should have a fix version.
- Tickets closed for any other reason, such as Duplicate, should not have a fix version.  Semantically, duplicate tickets inherit the fix version from the ticket to which they are duplicated.

It should be possible to clear the first two sections of the report simply by correcting the ticket resolutions and fix versions in Jira.

When ready, create a branch  and push to your fork so others can view the report easily.  Team members should close issues they own that are correctly fixed.  Re-generate the report periodically until it comes back clean.

Note: REPORT.md is not intended to be merged back into master.
