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

    JIRA_USERNAME=hello
    JIRA_PASSWORD=world

This is required if you want to process sensitive tickets.

## Usage

Make sure you have updated your repository:

    $ git checkout master
    $ git pull upstream master
    $ git fetch --tags upstream

Sanity check: ensure that the "latest" tag is correct (points to the latest release).  You may need to force-fetch the tags.

    $ git show latest
    # should show a commit with both "latest" and the previous version number

Run the tool and save the result into REPORT.md; set fixVersion to the *upcoming* ICU version:

    $ pipenv run python3 check.py --jira-query "project=ICU AND fixVersion=64.1" > REPORT.md

Create a branch and open a pull request so others can view the report easily.
