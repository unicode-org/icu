// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <stddef.h>
#include <stdint.h>

#include "cmemory.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size);

int main(int argc, char* argv[])
{
  (void) argc;
  (void) argv;
  const char *fuzzer_data = "abc123";
   
  LLVMFuzzerTestOneInput((const uint8_t *) fuzzer_data, strlen(fuzzer_data));

  return 0;
}
