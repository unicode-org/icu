# Contributing to ICU

Thank you for wanting to contribute to ICU!

## Why contribute?

ICU is an open source library that is a de-facto industry standard for internationalization libraries. Our goal is to provide top of the line i18n support on all widely used platforms. By contributing your code to the ICU library, you will get the benefit of continuing improvement by the ICU team and the community, as well as testing and multi-platform portability. In addition, it saves you from having to re-merge your own additions into ICU each time you upgrade to a new ICU release.

## License

Starting with ICU 58, ICU is a project of the Unicode® Consortium and the ICU Project Management Committee is a Unicode Technical Committee, the ICU-TC. ICU will continue to be released under the [Unicode open source license][unicode-license], which is similar to the old ICU license. For more details, see [the Unicode announcement][unicode-announcement].

Since 2018-July, ICU is hosted on GitHub.

For patches, please submit both a Jira ticket and a GitHub pull request. See [Submitting ICU Bugs and Feature Requests][bugs].

For contributions — bug fix patches, one-time feature additions, or ongoing work as a team member — please sign the Unicode CLA. You are automatically asked to do so when you create a pull request for the first time. Signing it once per person or organization is sufficient for future contributions.

 (As of 2018-Aug, the CLA assistant is not yet set up to recognize an organization affiliation, so individuals still have to sign it for now.)

## Process

See also [git for ICU Developers][git4icu].

If you want to join the team, then please contact us. Once everything is agreed, the ICU team adds you to the GitHub project and the Jira issue tracker.

## General Contribution Requirements

We will be glad to take a look at the code you wish to contribute to ICU. We cannot guarantee that the code will be included. Contributions of general interest and written according to the following guidelines have a better chance of becoming a part of ICU.

For any significant new functionality, contact the ICU development team through the icu-design mailing list first, and discuss the features, design and scope of the possible contribution. This helps ensure that the contribution is expected and will be welcome, that it will fit in well with the rest of ICU, and that it does not overlap with other development work that may be underway.

While you are considering contributing code to ICU, make sure that the legal terms (see [License](#license) above) are acceptable to you and your organization.

Here are several things to keep in mind when developing a potential contribution to the ICU project:

1.  ICU has both C/C++ and Java versions. If you develop in one programming language, please either provide a port or make sure that the logic is clear enough so that the code can be reasonably ported. We cannot guarantee that we will port a contribution to the other library.

2.  Before implementation, read and understand ICU's [coding guidelines][coding-guidelines]. Contributions that require too much adaptation to be included in the ICU tree will probably wait for a long time.

3.  During implementation, try to mimic the style already present in the ICU source code.

4.  Always develop the code as an integral part of the library, rather than an add-on.

5.  Always provide enough test code and test cases. We require that our APIs are 100% tested and that tests cover at least 85% of the ICU library code. Make sure that your tests are integrated into one of ICU's test suites ([cintltst][cintltst] and [intltest][intltest] for ICU4C and [com.ibm.icu.dev.test][com.ibm.icu.dev.test] classes in ICU4J). New tests and the complete test suite should pass.

6.  Compile using the strictest compiler options. Due to ICU's multi-platform nature, warnings on some platforms may mean disastrous errors on other platforms. This can be enabled for C++ by using the `--enable-strict` configure option on any platform using the gcc or clang compilers.

7.  Test on more than one platform. For ICU4C, it is good to combine testing on Windows with testing on Linux, Mac OS X or another Unix platform. It is always good to try to mix big and little endian platforms. For ICU4J, test using both Oracle's and IBM's JDKs and/or on Android.

8.  Each contribution should contain everything that will allow building, testing and running ICU with the contribution. This usually includes: source code, build files and test files.


## Team

We have several [mailing lists][contacts]. Contributors should at least subscribe to the **icu-design** mailing list, and we also have a team-internal list that you should get added to.

We meet once a week by phone. See the [agenda & meeting minutes][meetings].

## Understand ICU


*   Home page: <https://icu.unicode.org>

*   User Guide: <https://unicode-org.github.io/icu/userguide/>

    *   [Coding guidelines][coding-guidelines]

    *   [Introduction][introduction]

    *   [Design][design]

    *   [How to use ICU][howtouse]

    *   etc.

    *   API References: [C][icu4c-api] & [J][icu4j-api]

## Setup & Workflow

- [Source Code Access][repository]
- [git for ICU Developers][git4icu]
- [Tips for developers][tips]
- [Eclipse and other setup][setup]
- [Submitting ICU Bugs and Feature Requests][bugs]
- [ICU Ticket Life cycle][ticket-lifecycle]

Significant/disruptive changes should be discussed on the icu-design list or on the team-internal list.

**API changes must be proposed** on the **icu-design** list, see the [API Proposal Email Template][proposal-template].

Proposed changes will be discussed at the ICU meeting that follows at least six days later. Proposals are frequently modified during email and in-meeting discussion. Please do not "jump the gun" unless you are very confident your proposal will go through as-is, or be prepared to revert your changes.

# Welcome

_… and thanks for contributing!_

### License

Please see [./LICENSE](./LICENSE)

> Copyright © 2021 and later Unicode, Inc. and others. All Rights Reserved.
Unicode and the Unicode Logo are registered trademarks
of Unicode, Inc. in the U.S. and other countries.
[Terms of Use and License](http://www.unicode.org/copyright.html)


[coding-guidelines]: docs/userguide/dev/codingguidelines.md
[git4icu]: https://icu.unicode.org/repository/gitdev
[unicode-license]: https://www.unicode.org/license.txt
[unicode-announcement]: http://blog.unicode.org/2016/05/icu-joins-unicode-consortium.html
[bugs]: https://icu.unicode.org/bugs
[repository]: https://icu.unicode.org/repository
[tips]: https://icu.unicode.org/repository/tips
[setup]: https://icu.unicode.org/setup
[ticket-lifecycle]: https://icu.unicode.org/processes/ticket-lifecycle
[proposal-template]: https://icu.unicode.org/processes/proposal-template
[icu4c-api]: https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/
[icu4j-api]: https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/
[howtouse]: docs/userguide/icu/howtouseicu.md
[design]: docs/userguide/icu/design.md
[introduction]: docs/userguide/index.md
[contacts]: https://icu.unicode.org/contacts
[meetings]: https://icu.unicode.org/projectinfo/meetings
[cintltst]: ./icu4c/source/test/cintltst/
[intltest]: ./icu4c/source/test/intltest/
[com.ibm.icu.dev.test]: ./icu4j/main/tests/framework/src/com/ibm/icu/dev/test/
