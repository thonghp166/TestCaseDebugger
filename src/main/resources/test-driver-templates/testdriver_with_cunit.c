// testdriver_with_cunit.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "CUnit/Basic.h"
#include "CUnit/Automated.h"

int AKA_MARK_MAX = 100;
int aka_mark_iterator = 0;

FILE* AKA_TP_FILE;

 void AKA_WRITE_CONTENT_TO_FILE(const char * path, char content[]) {
    AKA_TP_FILE = fopen(path, "a");
    fputs(content, AKA_TP_FILE);
    aka_mark_iterator++;
    char buffer[16] = {0};
    sprintf(buffer, "%d\n", aka_mark_iterator);
     fputs(buffer, AKA_TP_FILE);

   // if the test path is too long, we need to terminate the process
   if (aka_mark_iterator >= AKA_MARK_MAX){
     fputs("\nThe test path is too long. Terminate the program automatically!", AKA_TP_FILE);
     fclose (AKA_TP_FILE);
     exit(0);
   }

    fclose (AKA_TP_FILE);
 }

char AKA_BUILD[100000] = "";

int AKA_MARK(char* append) {
  strcat(AKA_BUILD, append);
  strcat(AKA_BUILD, "\n");
  AKA_WRITE_CONTENT_TO_FILE("{{INSERT_PATH_OF_TEST_PATH_HERE}}", AKA_BUILD);
  return 1;
}

#define AKA_GTEST_EXECUTION_MODE

char* AKA_TEST_CASE_NAME;
int AKA_FCALLS = 0;

{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}

//CU_pSuite NAME_TEST_SCRIPT(void){
//     AKA_TEST_CASE_NAME = "TestCase";
//     init variable statements;
//     function call statements;
//     assertion statements;
//}

// NOTE: JUST SUPPORT SINGLE UNIT TEST
{{INSERT_TEST_SCRIPTS_HERE}}

/* The main() function for setting up and running the tests.
 * Returns a CUE_SUCCESS on successful running, another
 * CUnit error code on failure.
 */
int main()
{
   CU_set_output_filename("{{INSERT_PATH_OF_XML_RESULT_HERE}}");
   CU_pSuite pSuite = NULL;

   /* initialize the CUnit test registry */
   if (CUE_SUCCESS != CU_initialize_registry())
      return CU_get_error();

   /* add a suite to the registry */
   pSuite = CU_add_suite("Suite_1", NULL, NULL);
   if (NULL == pSuite) {
      CU_cleanup_registry();
      return CU_get_error();
   }

   /* add the tests to the suite */
   /* NOTE - ORDER IS IMPORTANT - MUST TEST fread() AFTER fprintf() */
   if ((NULL == CU_add_test(pSuite, "TEST_AKA", AKA_TEST_{{TEST_CASE_NAME}})))
   {
      CU_cleanup_registry();
      return CU_get_error();
   }

   /* Run all tests using the CUnit Basic interface */
   CU_automated_run_tests();
   CU_cleanup_registry();
   return CU_get_error();
}

