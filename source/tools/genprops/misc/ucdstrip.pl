#!/usr/lib/perl -p
# Simple tool for Unicode Character Database files with semicolon-delimited fields.
# Removes comments behind data lines but not in others.
# The Perl option -p above runs a while(<>) loop and prints the expression output.
s/^([0-9a-fA-F]+.+?) *#.*/\1/;
