add_executable(makeconv gencnvex.c genmbcs.cpp makeconv.cpp ucnvstat.c)
target_link_libraries(makeconv PRIVATE icu_defaults icuuc toolutil)