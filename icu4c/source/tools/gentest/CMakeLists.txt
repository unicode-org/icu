add_executable(gentest gentest.c genres32.c)
target_link_libraries(gentest PRIVATE icu_defaults icuuc toolutil)