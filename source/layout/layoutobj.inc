# Copyright (c) IBM, Inc. and Others, All Rights Reserved.
# Makefile include file listing the objects for the layout subproject

LAYOUT_OBJS=OpenTypeLayoutEngine.o \
ThaiLayoutEngine.o \
ThaiShaping.o \
ThaiStateTables.o \
ArabicLayoutEngine.o \
GXLayoutEngine.o \
IndicLayoutEngine.o \
LayoutEngine.o

# aat/
AAT_OBJS=ContextualGlyphSubstitutionProcessor.o \
IndicRearrangementProcessor.o \
LigatureSubstitutionProcessor.o \
LookupTables.o \
MorphTables.o \
NonContextualGlyphSubstitutionProcessor.o \
SegmentArrayProcessor.o \
SegmentSingleProcessor.o \
SimpleArrayProcessor.o \
SingleTableProcessor.o \
StateTableProcessor.o \
SubtableProcessor.o \
TrimmedArrayProcessor.o

# opentype/
OPENTYPE_OBJS=AlternateSubstitutionSubtables.o \
AnchorTables.o \
ArabicLigatureData.o \
ArabicShaping.o \
CDACLayout.o \
CDACLigatureData.o \
ClassDefinitionTables.o \
ContextualSubstitutionSubtables.o \
CoverageTables.o \
DeviceTables.o \
Features.o \
GDEFMarkFilter.o \
GlyphDefinitionTables.o \
GlyphIterator.o \
GlyphPositioningLookupProcessor.o \
GlyphPositioningTables.o \
GlyphSubstitutionLookupProcessor.o \
GlyphSubstitutionTables.o \
HebrewLigatureData.o \
HebrewShaping.o \
IndicClassTables.o \
IndicReordering.o \
LigatureSubstitutionSubtables.o \
LookupProcessor.o \
Lookups.o \
MarkArrays.o \
MarkToBasePositioningSubtables.o \
MarkToLigaturePositioningSubtables.o \
MarkToMarkPositioningSubtables.o \
MultipleSubstitutionSubtables.o \
OpenTypeUtilities.o \
PairPositioningSubtables.o \
ScriptAndLanguage.o \
SinglePositioningSubtables.o \
SingleSubstitutionSubtables.o \
SubstitutionLookups.o \
ValueRecords.o
