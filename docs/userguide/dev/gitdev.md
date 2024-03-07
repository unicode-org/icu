---
layout: default
title: git and Github for ICU Developers
parent: Contributors
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# git and Github for ICU Developers
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


For git & git lfs installation see the [Source Code Setup](../../devsetup/source/)
page.

For setup with language compilers and IDEs, see the [Setup for Contributors](../../devsetup/source/) page
and its subpages.

## Overview

ICU development is on GitHub, in the **main** branch of the git repository.
<https://github.com/unicode-org/icu>

In preparation for a release, we create a maintenance branch, such as
[maint/maint-62](https://github.com/unicode-org/icu/tree/maint/maint-62) for ICU
62 and its maintenance releases.

For each release we create a release tag.
[releases/tag/release-62-1](https://github.com/unicode-org/icu/releases/tag/release-62-1)
(GitHub project page > Releases > Tags > select one; a Release is a Tag with
metadata.)

There are additional branches that you can ignore. Some are old development
branches.

Also, when you edit a file directly on the GitHub source browser (for docs: API
comments, or .md/.html/.txt), it creates a branch for your pull request. Make
sure to delete this branch when you are done.

## Development

We do *not* develop directly on the main repository. Do *not* clone from
there to commit and push back into the main repository.

Instead, use the GitHub UI (top right) to create a fork of the repository in
your own GitHub account. Then clone that to your local machine. You need only
one fork for all of your ICU work.

```
mkdir -p icu/mine/src
git clone git@github.com:markusicu/icu.git icu/mine/src
cd icu/mine/src
```

You should be in the **main** branch of your fork's clone.

Do *not* do any development in your own **main** branch either! That would
lead to messy merging with the upstream **main** branch.

Instead, create a new branch in your local clone for each piece of work. You
need a separate branch for each pull request. More on that later.

```
git checkout -b mybranchname
```

Now you are in a new development branch in your local git repo. Confirm with
`git status`. Change stuff. Do `git status` again, use `git add` for staging and
`git commit -m 'ICU-23456 what I changed'` to commit, or use `git commit -a -m 'ICU-23456 what I changed'` if you want to commit everything that git status
shows as changed.

For looking at changes, you should set up a visual diff program for use with
`git difftool`. See the [Setup: git difftool & mergetool](../../devsetup/source/gittooling.md) page.

For new files: Remember to add the appropriate copyright lines. Copy from a file
of the same type, and set the copyright year to the current year (that is, the
year you are creating the file).

You should have a Jira ticket for each line of work. (See [Submitting ICU Bugs and Feature Requests](https://icu.unicode.org/bugs) and [ICU Ticket Life cycle](https://icu.unicode.org/processes/ticket-lifecycle).) You can have multiple pull
requests per ticket. Each pull request needs a ticket in Accepted state.

Always prefix your commit statements with the Jira ticket number using this
pattern (including the space after the number; note: no colon):
`**ICU-23456** what I changed`

Local commits are only on your local machine. If your local disk crashes, your
changes are gone. `git push` your commits to your GitHub fork.

**Tips for Branches**

Shane
[recommends](https://blog.sffc.xyz/post/185195398930/why-you-should-use-git-pull-ff-only)
setting the default behavior of `git pull` to `--ff-only`. Shane also
[prevents](https://stackoverflow.com/a/40465455/1407170) local commits to the
**main** branch via *.git/hooks/pre-commit*. These two measures make it easier
to do the right thing in Git.

## Trivial changes

For trivial changes, such as small fixes in API docs or text files, it is ok to
edit the file in the GitHub GUI, in the main unicode-org/icu repository.

You still need a Jira ticket.

Once you are done editing, the GUI lets you create a branch and a commit right
in the main repository. Use the usual **ICU-23456** what I changed pattern
for the commit message.

Pull request, review, merge as usual, see the next section.

*Remember to delete your branch after merging.*

## Review & commit to Unicode main

When you are ready for code review, go to your GitHub page and your ICU fork.

Select your dev branch (Branch drop-down on the left, search for your branch).

Click "New pull request" next to the Branch button, or "Pull request" on the
right near "Compare". *Make sure it compares with unicode-org/icu main on the
left and your own fork's dev branch on the right*.

Prefix the title of your pull request with the Jira ticket number, same format
as for a commit.

Follow the rest of the checklist in the PR template.

Set the PR assignee to your main reviewer. You may add more people as reviewers,
but there is normally just one assignee. Be somewhat judicious with additional
reviewers: Don't just add them because they were recommended by GitHub.

Nice to have: Optionally set the Jira ticket reviewer field for documentation.
Still possible to close the ticket if the field is empty.

Watch the PR status for build failures and other issues.

A PR reviewer (at least the assignee) should look to see if the PR does what the
ticket says.

Respond to review feedback. Make changes on your local machine, commit, push to
your fork. The GitHub PR will update automatically for your additional commits.

Try to not rebase, squash, or force-push until the reviewer gives you a green
light.

*You should normally squash multiple commits into one in your fork before
merging (after the reviewer is satisfied)*. For multiple commits, the reviewer
should first respond with something like "lgtm please squash" but not yet
GitHub-approve; after squashing, they should check that the changes are the
same, and then GitHub-approve. A bot will respond to the PR confirming whether
the squash succeeded without changing the file contents.

If you squash, since you are rewriting the commit message anyway, please append
the pull request number to the first line of the updated message, using the
format "` (#199)`".

When you squash, please keep the parent hash (sha) the same so that the squash
is nothing more than a squash. If you change the parent hash, you may also be
pulling in other people's changes, and it may be harder for the reviewer to
verify that the squash was done correctly.

### Options on how to squash

#### Option 1: Use the online PR commit checker bot

Please note: this makes the
change in your remote branch but not in your local branch. Click the "Details"
link in the GitHub status, which brings you to a page with a summary of your PR.
Find the "Squash..." button. Sign in using your GitHub account, and follow the
flow to squash your branch.

Warning: do not `git pull` after you use the remote tool! If you subsequently need
to update your local branch to the squash commit, you need to fetch and reset:

```
git fetch origin BRANCHNAME
git checkout BRANCHNAME
git reset origin/BRANCHNAME
```

#### Option 2: Use git rebase

This works as long as you have no merge commits with
conflicts in your history. Plenty of examples:

*   <https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History#_squashing>
*   <https://github.com/todotxt/todo.txt-android/wiki/Squash-All-Commits-Related-to-a-Single-Issue-into-a-Single-Commit>
*   <https://blog.carbonfive.com/2017/08/28/always-squash-and-rebase-your-git-commits/>
*   <http://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html>
*   <https://medium.com/@slamflipstrom/a-beginners-guide-to-squashing-commits-with-git-rebase-8185cf6e62ec>
*   Several other options:
    <https://stackoverflow.com/questions/5189560/squash-my-last-x-commits-together-using-git>

#### Option 3: Use git merge

This is a little tricker but works even if you have
merge commits with conflicts. Assuming your feature branch is called BRANCHNAME:

```
# Make sure your branch is up-to-date with main and that the tests pass:

git checkout BRANCHNAME
git merge main
git push

# At this point, wait for an LGTM from a reviewer before proceeding.
# Once confirmed, make your squash commit in a new temp branch.
# NOTE: In the first line, make sure to checkout the same sha as
# you most recently merged into your branch!

git checkout main
git checkout -b temp
git merge --squash BRANCHNAME
git commit

# Point your branch to the squash commit, and there should be no dirty files:

git checkout BRANCHNAME
git reset temp
git status  # should be empty! If it's not, you didn't check out the right sha.

# Push your squash commit and clean up:

git push -f
git branch -d temp
```

#### Option 4: Amend a small commit

When making code review changes on a small PR, you can amend your
previous commit rather than making a new commit. Instead of running "git
commit", just run "git commit --amend". You will need to force-push. The PR bot
will post a link for the reviewer to see the changes from your old commit to
your new commit.

Once the reviewer(s) has/have approved your (squashed) changes:

*   If you are an ICU team member with main repo write access:
    *   Merge your commits into the Unicode main.
        *   We almost always want to "rebase and merge" the commits. We normally
            want them pre-squashed for a simple, clean change history. We rarely
            want to permanently keep intermediate commits.
        *   (For ICU 63 we used "squash merge" but ended up with some ill-formed
            commit messages. "Rebase and merge" lets us review the commit
            messages before merging.)
    *   After you click the Merge button, if you don't use "rebase and merge"
        (although normally you should...), make sure that the commit message
        includes the "ICU-23456 " prefix, and add a suffix like " (#65)" with
        the pull request number (if it's not there already).
        *   Known limitation: We won't have the PR number in the commit
            message(s) when using the recommended "rebase and merge" -- unless
            you manually amend the commit message(s) and add it.
    *   You should probably check the box for deleting your dev branch after
        merging.
    *   Remember one branch per PR. You can create multiple branches & PRs per
        ticket.
    *   If this was the last commit to finish work on the ticket, then go to
        Jira and close the ticket as Fixed.
    *   You can optionally have someone (probably the same person as your PR
        assignee) review the ticket as well, but that's not normally necessary.
    *   (We normally use ticket reviews for non-code changes, such as a
        non-coding task or a web site update for the User Guide etc.)
*   Otherwise:
    *   The PR assignee should be an ICU team member, and they are responsible
        both for reviewing and for merging your PR, and then also for closing
        the ticket.


## Merge conflicts

When someone else has made changes that conflict with yours, then you can't
merge as is. (The GitHub pull request page will tell you if there is a
conflict.)

You need to update your fork's **main** via your local clone, rebase your local
dev branch with that, resolve conflicts as you go, and force-push to your fork.

As easy as it is in GitHub to *create* a fork, you would think that it would be
a simple button-click to *update* your fork's **main** with commits on the
Unicode **main**. If you find a way to do this, please update this section.

Switch to your local **main**.

```
git checkout main
```

### Pull from upstream

Pull updates from the Unicode main (rather than a vanilla `git pull` which
pulls form your out-of-date fork), push to your fork's main.

*Norbert’s version:*

```
git pull git@github.com:unicode-org/icu.git
git push
```

*Andy’s version:*

Once per local git repo, set up an additional "remote". Something like the
following, but this may be incomplete!

```
git remote add upstream https://github.com/unicode-org/icu.git
git pull upstream main
git push origin main
```

*Andy's Version, take 2:*

Set the local main to track the upstream (unicode-org) main instead of your
fork's main (orign). Your fork's main is effectively out of the loop.

```
# one time setup
git branch -u upstream/main
# subsequent pulls from upstream (unicode.org) main
git pull
```

### Resolve conflicts

There are two ways to do this. You can rebase, or you can create a merge commit.
The advantage of rebase is that it makes it somewhat easier to squash later on.
The advantage of creating a merge commit is that you don't have to force-push,
so it makes it easier to work across different workstations, you are less likely
to get something wrong, and it makes it easier for the reviewer because GitHub
keeps track of comment history better when shas don't change.

#### Option 1: Merge

Switch to your dev branch, then merge in main. I like to use
the --no-commit option:

```
git checkout mybranchname
git merge main --no-commit
```

If you have conflicts, resolve them. Then, review the merge commit. It should
have all changes from main that were not yet on your branch. If it looks good,
commit the merge. You can push the merge commit without having to use -f.

```
git commit
git push
```

#### Option 2: Rebase

First switch back to your dev branch (without the -b option
which is for creating a new branch).

```
git checkout mybranchname
```

Then rebase, which reapplies your branch changes on top of the new main commits.

```
git rebase main
```

Sometimes you need to manually resolve conflicts. Follow the instructions git
prints or look for help...

If it had stopped and you are done resolving conflicts, continue rebasing.

```
git rebase --continue
```

You might get conflicts at several stages; resolve & continue until done.

When done, push to your GitHub fork. You need to force-push after rebasing.

```
git push -f
```

## Update your fork

Once in a while, you should update your fork's main with changes from the
Unicode main, so that you don't fall too far behind and your new changes don't
create unnecessary merge conflicts.

Go to your local main, pull commits from the Unicode main, and push to your
GitHub fork. See the "Merge conflicts" section above for details. If you don't
have a current dev branch, you can skip the rebasing.

## Committing to Maintenance Branch

Follow these steps for adding a commit to a maintenance branch.

The process is different between when we are between RC and GA and when we are
after GA.

### Between RC and GA

When working on a commit that you know at the time of
authorship to be a candidate for the maintenance branch, write the commit and
send the PR directly against the maintenance branch. All commits on the maint
branch will be merged *from maint to main* as a BRS task (see the next section).

Check out the current maint branch:

```
git fetch upstream maint/maint-64
git checkout maint/maint-64
```

Next, make a local branch off of the maint branch. For example, to use the
branch name "ICU-12345-maint-64", you can do:

```
git checkout -b ICU-12345-maint-64
```

Now, write your change and send it for review. Open your PR against the maint
branch.

### After GA

Write the commit against the main branch, and send your own
cherry-pick commits to put it on the desired maint branches.

Update your local main from the Unicode main (see above). Otherwise your git
workspace won't recognize the commits you are trying to cherry-pick.

Make a note of the SHA hash/ID of your commit on the main branch. You will use
this later when cherry-picking into the maint branch.

*   The commit ID is listed on the pull request page.
*   You can use git log to see the SHA once your change is on main.
*   You can look at the commit history on GitHub too.

Next, checkout the maintenance branch locally. For example, for the ICU 63
maintenance branch:

```
git fetch upstream maint/maint-64
git checkout maint/maint-64
```

Next, make a local branch off of the maint branch. This new branch will be used
for your cherry-pick.

For example, to use the branch name "ICU-12345-maint-64", you can do:

```
git checkout -b ICU-12345-maint-64
```

Next, cherry-pick the commit(s) you want to apply to the maintenance branch.
(Note: If you only have one commit to merge to the maint branch then you would
only have one command below).

```
git cherry-pick 7d99ba4
git cherry-pick e578f3f
...
```

This creates **new** commits directly onto your local branch.

Look at the output from each of these commands to double-check that you got the
intended commits.

Finally, push your branch to your fork (should be "origin"), and open a PR into
the Unicode ICU branch maint/maint-64.

```
git push -u your-fork ICU-12345-maint-64
```

The reviewer of the PR has the following special responsibilities:

1.  Don't approve the PR unless ICU-TC has agreed that this should be a
    maintenance fix.
2.  Make sure that the PR is targeting the correct branch in the Unicode ICU
    repro. (ex: maint/maint-64 ).
3.  Make sure that the PR includes all commits associated with the fix, which
    was already approved for main.
4.  Use "Rebase and merge".

## Checking for Missing Commits (BRS Task)

It is not hard to accidentally make a commit against main that should have been
against maint. As a BRS task before tagging, you should check the list of
commits that are on main but not maint and make sure  of them belong on
maint.

To get the list, run:

```
git fetch upstream
git cherry -v upstream/maint/maint-64 upstream/main
```

Commits prefixed with "+" are on main but not on the specified maint branch.
Commits prefixed with "-" are present on both branches.

Send the list to the team and discuss in the weekly meeting if there are any
problems.

## Merging from Maint to Main (BRS Task)

Merging from the maint branch to main might be as easy as opening a pull
request, without having to touch the command line. However, if there are merge
conflicts, more work will need to be done.

**The Easy Way (No Merge Conflicts):** Open a pull request on GitHub from the
maint branch to main. If it says there are no merge conflicts, congratulations!
Use a new ticket number for the PR (it is suggested to NOT use the main BRS
ticket). The new ticket should have the next release as its fix version, because
the merge commit used to pull the commits from maint to main will be in the next
release but not the current release.

You may need to add "DISABLE_JIRA_ISSUE_MATCH=true" and/or
"ALLOW_MANY_COMMITS=true" to the PR description to silence errors coming from
the Unicode bot.

You should use a MERGE COMMIT to merge from maint to main, NOT REBASE MERGE as
is normally recommended. You will need to go into the admin panel on GitHub,
enable merge commits, perform your merge commit, and then disable merge commits
again from the admin panel. When making your merge commit, remember to use the
correct commit message syntax: prefix the merge commit message with ICU-#####,
the new ticket number you created above.

**The Hard Way (Merge Conflicts):** At the end of the day, the goal is that main
should share the maint branch's history. This is done using merge commits. What
follows is an example of how to create merge commits that retain full branch
history.

Create a new branch based on the tag you want to merge:

```
git fetch upstream
git checkout main
git checkout -b 64-merge-branch  # use any name you like
```

*If you already have this branch from a previous release tag*, you could either
use a new branch, or merge the latest main into your branch:

```
git checkout 64-merge-branch
# DANGER: Please make sure your workspace is clean before proceeding!
# If it's not, you might sneak in unreviewed changes.
git merge --no-commit main
git commit -am "ICU-##### Merge tag 'main' into 64-merge-branch"
```

Now, merge in maint:

```
# DANGER: Please make sure your workspace is clean before proceeding!
# If it's not, you might sneak in unreviewed changes.
git merge --no-commit upstream/maint/maint-39
```

After running the final line, you will have the opportunity to resolve merge
conflicts. If the conflict is in a large binary file like the ICU4J data jar
files, you may need to re-generate them.

Remember to prefix your commit message with the ticket number:

```
git commit -am "ICU-##### Merge branch 'maint/maint-39' into 64-merge-branch"
git push -u origin 64-merge-branch
```

As in the Easy Way, you may need to add `DISABLE_JIRA_ISSUE_MATCH=true` and/or
`ALLOW_MANY_COMMITS=true` to the PR description to silence errors coming from
the Unicode bot.

Send the PR off for review. As in the Easy Way, **you should use the MERGE COMMIT option in GitHub to land the PR!!**

## Requesting an Exhaustive Test run on a Pull-Request (PR)

The ICU4C and ICU4J Exhaustive Tests run on the main branch after a pull-request
has been submitted. They do not run on pull-requests by default as they take 1-2
hours to run.

However, you can manually request the CI builds to run the exhaustive tests on a
PR by commenting with the following text:

```
/azp run CI-Exhaustive
```

This will trigger the test run on the PR. This is covered more in a separate
[document](https://docs.google.com/document/d/1kmcFFUozpWah_y7dk_Inlw_BIq3vG3-ZR2A28tIiXJc/edit?usp=sharing).
