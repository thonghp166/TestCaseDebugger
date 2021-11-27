// testdriver_with_googletest.c
//#include <stdio.h>
//#include <stdlib.h>
//#include <string.h>
//#include "CUnit/Basic.h"
//
//int AKA_MARK_MAX = 5000;
//int aka_mark_iterator = 0;
//
//FILE* AKA_TP_FILE;
//
// void AKA_WRITE_CONTENT_TO_FILE(const char * path, char content[]) {
//    AKA_TP_FILE = fopen(path, "a");
//    fputs(content, AKA_TP_FILE);
//    aka_mark_iterator++;
//
//   // if the test path is too long, we need to terminate the process
//   if (aka_mark_iterator >= AKA_MARK_MAX){
//     fputs("\nThe test path is too long. Terminate the program automatically!", AKA_TP_FILE);
//     fclose (AKA_TP_FILE);
//     exit(0);
//   }
//
//    fclose (AKA_TP_FILE);
// }
//
//char AKA_BUILD[100000] = "";
//
//int AKA_MARK(char* append) {
//  strcat(AKA_BUILD, append);
//  strcat(AKA_BUILD, "\n");
//  AKA_WRITE_CONTENT_TO_FILE("{{INSERT_PATH_OF_TEST_PATH_HERE}}", AKA_BUILD);
//  return 1;
//}
//
//#define AKA_GTEST_EXECUTION_MODE
//
//char* AKA_TEST_CASE_NAME;
//int AKA_FCALLS = 0;
//
//{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}
//
////CU_pSuite NAME_TEST_SCRIPT(void){
////     AKA_TEST_CASE_NAME = "TestCase";
////     init variable statements;
////     function call statements;
////     assertion statements;
////}
//
//// NOTE: JUST SUPPORT SINGLE UNIT TEST
//{{INSERT_TEST_SCRIPTS_HERE}}
//
//int main(int argc, char **argv) {
//	CU_pSuite pSuite = {{INSERT_NAME_TEST_SCRIPT_HERE}}();
//    return 0;
//}