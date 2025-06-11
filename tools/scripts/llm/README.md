# CLDR LLM Validator

+---------------------------------------------------+
| GSoC Contributor: preetsureshbhai.sojitra@email.ucr.edu |
| Mentors:                                           |
| - younies@unicode.org                              |
| - annemarie@unicode.org                            |
| Tech Lead: mark@unicode.org                        |
| Doc Status: Draft                                  |
| Last Update: June 11, 2025                          |
| Project Information:                               |
| This project is part of GSoC initiation:           |
| [Unicode Inc. Project Details](https://summerofcode.withgoogle.com/organizations/unicode-inc/projects/details/lE93K0ho) |
+---------------------------------------------------+

## Introduction

Unicode supports essential global functions—such as date/time formatting, text segmentation, and measurement conversions—by publishing over a million locale data entries. However, maintaining the quality of this vast dataset presents significant challenges.

In this document, we leverage large language models (LLMs) to automate quality control. By prompting an LLM to generate outputs comparable to those in CLDR/ICU, we will build an AI-powered classifier that flags entries deviating from expected patterns.

