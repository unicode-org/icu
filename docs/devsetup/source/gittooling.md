---
layout: default
title: Local tooling configs for git and Github
grand_parent: Setup for Contributors
parent: Source Code Setup
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Local tooling configs for git and Github
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## git difftool & mergetool

The `git diff` command prints changes to stdout, normally to the terminal
screen.

Set up a visual diff and merge program for use with `git difftool` and `git
mergetool`.

Changes in binary files do not show well in common diff tools and can take a
long time for them to compute visual diffs.

This is easily avoided using the -d option: `git difftool -d`

This shows all changed files in the diff program, and you can view and skip
files there as appropriate.

### Linux example

[stackoverflow/.../setting-up-and-using-meld-as-your-git-difftool-and-mergetool](https://stackoverflow.com/questions/34119866/setting-up-and-using-meld-as-your-git-difftool-and-mergetool)

#### Linux meld

`gedit ~/.gitconfig` →

```
[diff]
    tool = meld
[difftool]
    prompt = false
[difftool "meld"]
    cmd = meld "$LOCAL" "$REMOTE"
[merge]
    tool = meld
[mergetool "meld"]
    cmd = meld "$LOCAL" "$MERGED" "$REMOTE" --output "$MERGED"
```

## Auto-link from GitHub to Jira tickets

GitHub itself does not linkify text like "ICU-23456" to point to the Jira
ticket. You can get links via browser extensions.

### Chrome Jira HotLinker

Install the [Jira
HotLinker](https://chrome.google.com/webstore/detail/jira-hotlinker/lbifpcpomdegljfpfhgfcjdabbeallhk)
from the Chrome Web Store.

Configuration Options:

*   Jira instance url: https://unicode-org.atlassian.net/
*   Locations: https://github.com/

### Safari extension from SRL

<https://github.com/unicode-org/icu-jira-safari>

### Firefox extension from JefGen

Install from the Mozilla Firefox Add-ons site:
<https://addons.mozilla.org/en-US/firefox/addon/github-jira-issue-linkifier/>

Source:
<https://github.com/jefgen/github-jira-linkifier-webextension>
