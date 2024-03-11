---
layout: default
title: Continuous Integration
parent: Contributors
---

# Continuous Integration
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

<!--
© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

ICU is tested thoroughly through a variety of tests
(unit tests, integration tests, exhaustive tests, fuzzer tests).
Many tests are specific to ICU4C or ICU4J individually,
using the language specific testing setups tools
(ex: ICU4C's C++ `intltest` and C `cintltest` for unit tests,
ICU4J's unit tests in JUnit).

ICU uses Continuous Integration systems to run these tests automatically for each new code contribution,
and upon each update to a future contribution under development
(that is: for each [successful Pull Request merge, and upon a new push of new git commits to a Pull Request](../gitdev.md)).
ICU has 2 systems configured to run Continuous Integration testing:

* Github Actions
* Azure Pipelines

Continuous Integration systems can also be used to regularly and automatically run other tasks besides testing.
ICU uses a CI workflow to automatically publish changes to its User Guide that is hosted on Github Pages.

## General ICU Settings for CI

### Automated Testing Enforcement

The value of automated testing isn't fully realized unless its enforcement is also automatic.

Individual tests can be configured in the ICU Github repository:

1. "Settings" tab for [`unicode-org/icu`](https://github.com/unicode-org/icu/)
2. "Branches"
3. "Branch protection rules"
4. Choose one of the rule sets and click "Edit".  ***Note: all rule sets should be changed in the same way***
5. Ensure that "Require status checks to pass before merging" is enabled
6. Underneath that enabled checkbox, the table "Status checks that are required." lists the currently enabled tests ("checks") that must pass before a Pull Request can be merged to the branch
7. If a new test (check) needs to be added, use the search box above the table and type the display name of the check, then click on it in the drop down list to add it to the table of required checks.


## Github Actions Configuration

ICU uses [Github Actions](https://github.com/features/actions) to run unit tests, environment tests, deployments of the User Guide, etc.

### Workflow files

Workflow files are in YAML form stored at `.github/workflows/`.

### UI Dashboard

All workflow instances/runs show up in the ["Actions" tab](https://github.com/unicode-org/icu/actions) of the repository,
using the display name that was set for the workflow.
All of the runs can be filtered by the Workflow name and/or by the Pull Request (via the Branch filter).

Each PR will show the status of the current/latest run of each job in the "Checks" section of the Pull Request's main page (Conversation tab), 
with special details and logs being navigable in the Checks tab.

### Configuration

#### Jobs

Jobs can be added, removed, and edited merely by editing the YAML workflow files.

#### Conditional Triggers

Workflow files can be conditionally triggered based on many factors.
A few of the useful factors:

* whether the trigger event is a push to a pull request, or the merge of a pull request, or a manual request
* the name of the originating branch for the trigger event
* whether the files modified in a pull request belong to certain file paths

ICU uses the file path matching condition to run ICU4J or ICU4C tests respectively only when their corresponding directories `icu4j` and `icu4c` contain file changes.
Thus, there is a separate workflow for ICU4J and ICU4C,
each with its own separate set of trigger conditions based on their corresponding file paths.
User Guide deployments are similarly run conditionally only when the `docs` directory has changed.
Some tests that are commonly needed or cross boundaries of concern are always run.

Some long-running tests are run once a week using a cronjob-like schedule trigger.

Some workflows exist to interact with the Github Actions platform,
such as keeping the cache alive for Maven artifacts due to the [noticeable flakiness that PRs face](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows) without the cache.
Because of a [cache eviction policy](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows#usage-limits-and-eviction-policy), which affects any subsequent PRs until they are merged,
it is easier to keep the cache alive in an automated way using a cronjob-like schedule trigger whose repetition period is shorter than the eviction period.

### Caveats

Github branch protection rules are not capable of appropriately handling jobs from a conditionally triggered workflow.
In other words, we cannot currently configure the required checks to include a job from the ICU4J workflow,
because if a PR does not include ICU4J code changes,
the workflow will not run and Github is not capable of ignoring the check in that scenario. Furthermore, workflow conditional triggers can only be triggered at the workflow level, not the job level.

Open Source users can now access machines with [at least 4 cores available per job](https://github.blog/2024-01-17-github-hosted-runners-double-the-power-for-open-source/).

## Azure Pipelines Configuration

ICU also uses [Azure Pipelines](https://azure.microsoft.com/en-us/products/devops/pipelines) to run CI jobs.

### Workflow files

Workflow files are in YAML form stored at `.ci-builds/`.

### UI Dashboard

All Azure piplines show up in the 
[Azure Piplines ICU project dashboard](https://dev.azure.com/ms/icu/),
specifically in the 
[Pipelines page](https://dev.azure.com/ms/icu/_build).

After clicking on a specific pipeline,
all of the instances/runs for that pipeline appear.
All of the runs can be filtered in the Branch filter by the destination branch name (ex: `main`) or the PR number (ex: `2874`).

### Configuration

#### Initial Setup with Github.

The upstream Github repo needs to be connected to Azure Pipelines in order for Azure Pipelines to listen for and trigger new pipelines based on events occuring in Github,
and to return the status back to Github.
This configuration is started by ensuring that the upstream ICU repo `unicode-org/icu` has the Azure Pipelines 3rd party app installed from the Github Marketplace.

Once installed, the app will appear in the repo's Github "Settings" page under "Integrations" > "Github Apps".

In order to configure a newly pipeline in Azure using the Github app for Azure Pipelines, you must do:

1. Create and check-in a new YAML file in the icu repo at `.ci-builds/`
2. In the repo settings, go to "Github Apps"
3. In the Installed GitHub Apps section, click option to configure "Azure Pipelines".
4. A page to update Azure Pipeline's repository access apears next. Click on "Update access".
5. Next a page to authenticate with your Microsoft credentials appear. Sign in with Microsoft credentials.
6. Select ADO org as `ms` and project as `icu`, click on continue.
7. After authentication from Azure and Github, you come to the new pipeline wizard.
8. Select repo as `unicode-org/icu` and select "Existing Azure Pipelines YAML file" and choose the yaml file created in step #1
9. Review YAML file and click save. You will find a new pipeline created with name `unicode-org.icu`. Rename it to a more appropriate name

***The pipline should now run as per the YAML rules and would be visible from Github settings for branch protection.***


#### Jobs

Jobs can be added, removed, and edited merely by editing the YAML workflow files.

The syntax for Azure Pipelines workflows is very similar to Github Actions, including the YAML format.
However, there are noteworthy differences in functionality and the expression of equivalent configurations aross the systems and syntaxes.

#### Conditional Triggers

Conditional triggers can be configured for Azure Pipelines similarly to Github Actions.

Note: The triggers for merges to a branch (ex: `main`) may need to be duplicated into a separate trigger section for Pull Requests because they seem to be handled differently.

### Caveats

In order to set up a pipeline, a person must simultaneously has access to the Azure Pipelines project for ICU and to the Github ICU repository.