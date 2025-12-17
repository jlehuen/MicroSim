// Function to decrement a value and return it
int my_decrement(int x) {
    x = x - 1; // x refers to the shadow variable (e.g., my_decrement_x)
    return x;  // Returns the decremented value
}

void main() {
    int initial_val;
    int final_result;

    initial_val = 10;
    final_result = 0; // Initialize

    // Call function with variable
    // initial_val (10) pushed. my_decrement_x becomes 10, then 9. Returns 9.
    // final_result becomes 9.
    final_result = my_decrement(initial_val);

    // Call function with literal
    // 5 pushed. my_decrement_x becomes 5, then 4. Returns 4.
    // final_result becomes 4.
    final_result = my_decrement(5);

    // Call function without assigning return value (just tests cleanup)
    // 4 pushed. my_decrement_x becomes 4, then 3. Returns 3.
    // This return value is discarded. final_result remains 4.
    my_decrement(final_result); 
}
