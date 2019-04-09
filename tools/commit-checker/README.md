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

    $ git fetch --tags upstream

Run the tool and save the result into REPORT.md; set fixVersion to the upcoming ICU version, and take the revision range between the previous release and the tip for the upcoming release:

    $ pipenv run python3 check.py \
        --jira-query "project=ICU AND fixVersion=64.1" \
        --rev-range "release-63-1..upstream/maint/maint-64"
        > REPORT.md

Create a branch and open a pull request so others can view the report easily.
