#include <stdio.h>
void swap(int *a, int *b)
{
    int c = *a;
    *a = *b;
    *b = c;
}

int main()
{
    printf("Hello World");
    int a =10;
    int b = 11;
    swap(&a,&b);

    return 0;
}
