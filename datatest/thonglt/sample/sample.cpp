#include <iostream>


using namespace std;


void sample(int x){

	switch (x) {
	case 1:
		cout << "x is 1";
		break;
	case 2:
	case 3:
		cout << "x is 1, 2 or 3";
		break;
	default:
    		cout << "x is not 1, 2 nor 3";
  }

}

int sum(int a, int b) {
	int sum = 2;
	sum += a;
	sum += b;
	return sum;
}

int main() {

	int res = sum(3,4);
	return 0;
}
