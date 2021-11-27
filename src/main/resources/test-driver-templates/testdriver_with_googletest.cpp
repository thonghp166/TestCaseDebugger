// testdriver_with_googletest.cpp
#include <string>
#include <fstream>
#include "gtest/gtest.h"

// Some test cases needs to include specific headers
// generated by automated test data generation
/*{{INSERT_ADDITIONAL_HEADER_HERE}}*/

int AKA_MARK_MAX = 5000;
int aka_mark_iterator = 0;
void AKA_WRITE_CONTENT_TO_FILE(const char * path, std::string content) {
  std::ofstream myfile;
  myfile.open(path);
  myfile << content;
  myfile.close();
  aka_mark_iterator++;

  // if the test path is too long, we need to terminate the process
  if (aka_mark_iterator >= AKA_MARK_MAX){
    std::ofstream myfile;
    myfile.open(path);
    myfile << content + "\nThe test path is too long. Terminate the program automatically!";
    myfile.close();
    throw std::exception();
  }
}

std::string AKA_BUILD = "";
bool AKA_MARK(std::string append) {
  AKA_BUILD += append + "\n";
  AKA_WRITE_CONTENT_TO_FILE("{{INSERT_PATH_OF_TEST_PATH_HERE}}", AKA_BUILD);
  return true;
}

#define AKA_GTEST_EXECUTION_MODE

std::string AKA_TEST_CASE_NAME = "";
int AKA_FCALLS = 0;

{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}

/**
 * TEST(TestSuite, TestCase) {
 *    AKA_TEST_CASE_NAME = "TestCase";
 *    init variable statements;
 *    function call statements;
 *    assertion statements;
 * }
 */
{{INSERT_TEST_SCRIPTS_HERE}}

int main(int argc, char **argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}