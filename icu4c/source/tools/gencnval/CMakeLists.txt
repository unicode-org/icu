add_executable(gencnval gencnval.c)
target_link_libraries(gencnval PRIVATE icu_defaults icuuc toolutil)