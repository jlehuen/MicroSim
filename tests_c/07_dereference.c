void main() {
    int val = 15;
    int ptr; // Should be int* ptr; but current compiler doesn't support pointer types explicitly
    int result;

    ptr = &val;
    result = *ptr;
}
