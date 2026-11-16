// Function to process a number: decrements if > threshold
int process_number(int num) {
    int threshold = 2; // Fixed threshold inside function
    int processed_val_in_func;
    processed_val_in_func = num; // Copy param to a local-like variable

    if (processed_val_in_func > threshold) { // e.g., if 5 > 2
        processed_val_in_func = processed_val_in_func - 1;
    }
    return processed_val_in_func;
}

void main() {
    int loop_counter;
    int upper_limit = 5;
    int lower_limit = 1;
    int returned_from_func;

    loop_counter = 10;
    returned_from_func = 0;

    // Outer loop: process numbers from 10 down to 2
    while (loop_counter > upper_limit) { // loop_counter = 10, 9, 8, 7, 6
        returned_from_func = process_number(loop_counter); // Call func with current counter
        loop_counter = returned_from_func; // Update counter with processed value
    }
    // Expected: At this point, loop_counter should be 5.
    // loop_counter=10. process(10) -> 9. loop_counter=9.
    // loop_counter=9. process(9) -> 8. loop_counter=8.
    // loop_counter=8. process(8) -> 7. loop_counter=7.
    // loop_counter=7. process(7) -> 6. loop_counter=6.
    // loop_counter=6. process(6) -> 5. loop_counter=5.
    // loop ends because 5 > 5 is false.

    // Second part: simple if within main
    if (loop_counter > lower_limit) { // 5 > 1 is true
        returned_from_func = returned_from_func - 1; // returned_from_func (which was 5) becomes 4.
    }
    // Final expected state: loop_counter = 5, returned_from_func = 4.
}
