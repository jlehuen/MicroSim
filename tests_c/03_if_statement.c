void main() {
    int x = 10;
    int y = 5;
    int z = 20;

    // First if block: should execute (10 > 5 is true)
    if (x > y) {
        z = z - 10; // z becomes 10
    }

    // Second if block: should NOT execute (5 > 10 is false)
    if (y > x) {
        z = z - 1; // This line should not be reached
    }
    
    // At the end: x=10, y=5, z=10
}
