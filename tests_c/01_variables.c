void main() {
	int var1;
	int var2 = 5;
	int var3;

	var1 = 10;
	var3 = var1; // var3 = 10
	var1 = var1 - 2; // var1 = 8
	var2 = var3; // var2 = 10

	// At the end: var1=8, var2=10, var3=10
}