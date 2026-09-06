add_executable(iotest filetst.c iotest.cpp stream.cpp strtst.c trnstst.c)
target_link_libraries(iotest icu_defaults icuuc ctestfw icuio icui18n)

if(BUILD_TESTING)
    add_test(NAME iotest COMMAND iotest WORKING_DIRECTORY ${CMAKE_RUNTIME_OUTPUT_DIRECTORY})
endif()
