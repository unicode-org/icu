add_executable(gensprep gensprep.c store.c)
target_link_libraries(gensprep PRIVATE icu_defaults icuuc toolutil)