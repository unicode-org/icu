add_executable(gencmn gencmn.c)
target_link_libraries(gencmn PRIVATE icu_defaults icuuc toolutil)