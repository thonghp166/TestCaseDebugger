#include <set> // <set>
#include <string>
#include "class.h"

int intArgument(std::set<int> v) {
    return v.size();
}

int charArgument(std::set<char> v) {
    return v.size();
}

int stringArgument(std::set<std::string> v) {
    return v.size();
}

int structureArgument(std::set<MyClass> v) {
    return v.size();
}

int stltypeArgument(std::set<std::set<char> > v) {
    return v.size();
}

int MyClassTemplateArgument(std::set<Array<int> > v) {
    return v.size();
}

int oneLevelBasicArgument(std::set<char*> v) {
    return v.size();
}


int twoLevelBasicArgument(std::set<char**> v) {
    return v.size();
}


int oneLevelStructureArgument(std::set<MyClass*> v) {
    return v.size();
}


int twoLevelStructureArgument(std::set<MyClass**> v) {
    return v.size();
}

int oneLevelBasic(std::set<char>* v) {
    return v[0].size();
}

int twoLevelBasic(std::set<char>** v) {
    return v[0][0].size();
}

int oneDimBasic(std::set<char> v[]) {
    return v[0].size();
}

int twoDimBasic(std::set<char> v[][4]) {
    return v[0][0].size();
}

int oneLevelStructure(std::set<MyClass>* v) {
    return v[0].size();
}

int twoLevelStructure(std::set<MyClass>** v) {
    return v[0][0].size();
}

int oneDimStructure(std::set<MyClass> v[]) {
    return v[0].size();
}

int twoDimStructure(std::set<MyClass> v[][4]) {
    return v[0][0].size();
}

int twoLevelOneLevel(std::set<int*>** v) {
    return v[0][0].size();
}


int twoDimOneLevel(std::set<int*> v[][4]) {
    return v[0][0].size();
}