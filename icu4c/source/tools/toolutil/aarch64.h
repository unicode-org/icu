// Copyright (C) 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

/* Add BTI/PAC/GCS tags to GNU Notes section */
#if defined(__ARM_FEATURE_BTI_DEFAULT) && __ARM_FEATURE_BTI_DEFAULT == 1
    #define GNU_PROPERTY_AARCH64_FEATURE_1_AND      0xc0000000
    #define GNU_PROPERTY_AARCH64_FEATURE_1_BTI      (1U << 0)
    #define GNU_PROPERTY_AARCH64_FEATURE_1_PAC      (1U << 1)
    #define GNU_PROPERTY_AARCH64_FEATURE_1_GCS      (1U << 2)

    .pushsection .note.gnu.property, "a"; /* Start a new allocatable section */
    .balign 8; /* align it on a byte boundry */
    .long 4; /* size of "GNU\0" */
    .long 0x10; /* size of descriptor */
    .long 0x5; /* NT_GNU_PROPERTY_TYPE_0 */
    .asciz "GNU";
    .long GNU_PROPERTY_AARCH64_FEATURE_1_AND;
    .long 4; /* Four bytes of data */
    .long GNU_PROPERTY_AARCH64_FEATURE_1_BTI | GNU_PROPERTY_AARCH64_FEATURE_1_PAC | GNU_PROPERTY_AARCH64_FEATURE_1_GCS;
    .long 0; /* padding for 8 byte alignment */
    .popsection; /* end the section */
#endif
