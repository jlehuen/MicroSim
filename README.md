# MicroSim 8 bits

![License](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Language](https://img.shields.io/badge/Language-Java-orange.svg)
![Platform](https://img.shields.io/badge/Platform-macOS%20%7C%20Linux%20%7C%20Windows-lightgrey.svg)

![MicroSim 8 bits Interface](man/img/main.png)

## Introduction

MicroSim is an 8-bit microprocessor simulator designed as an educational tool for learning x86 MASM-type assembly language programming. It provides a complete environment: a code editor with syntax highlighting, a visualization of registers and 256-byte RAM (8-bit addresses), as well as several interactive virtual devices.

The goal is to illustrate the fundamental concepts of computer architecture, such as registers, memory, the stack, and the internal workings of a CPU through a simple yet complete and realistic instruction set.

This project is inspired by two other similar open-source projects:

- The [x86 Microprocessor Simulator](https://github.com/dwhinham/Microprocessor-Simulator) by C. N. Bauers, hosted by Dale Whinham.
- The online [Simple 8-bit Assembler Simulator](https://github.com/Schweigi/assembler-simulator) by Marco Schweighauser.

## Features

- **Code Editor:** Write your assembly code with syntax highlighting and a code beautifier to standardize formatting.
- **CPU Visualization:** Real-time display of all CPU registers and flags.
- **RAM Inspector:** View the entire 256-byte memory in hexadecimal or ASCII. The `IP` (Instruction Pointer) and `SP` (Stack Pointer) locations are highlighted.
- **Execution Control:** Run the code continuously at adjustable speeds or execute it instruction by instruction in step-by-step mode.
- **Virtual Devices:** Interact with your programs through several I/O devices.
- **Cross-Platform:** Built with Java, MicroSim runs on macOS, Linux, and Windows.

## Virtual Devices

MicroSim includes several input/output devices that can be controlled by your programs:

| Device | Description | Image |
| :--- | :--- | :--- |
| **ASCII Terminal** | A 4x16 character text screen. To display a character, write its ASCII code to the memory area between `0xC0` and `0xFF`. | ![ASCII Terminal](man/img/ascii.png) |
| **Keyboard** | Allows character input using the `IN 0x01` instruction. The ASCII code of the pressed key is placed in the `AL` register. | ![Keyboard](man/img/keyboard.png) |
| **Traffic Lights** | Simulates two traffic lights controlled via the `OUT 0x02` instruction, based on the byte in the `AL` register. | ![Traffic Lights](man/img/lights.png) |
| **Heating System** | A heater controlled with `OUT 0x03` and a thermostat read with `IN 0x03`. The goal is to regulate the temperature. | ![Heating System](man/img/heater.png) |

## Quick Start Guide

1.  **Write your code:**

	- Type your program in the code editor.

2.  **Assemble it:**

	- Click the "Assemble" button. Syntax errors will be displayed in a dialog box.

3.  **Run your program:**

    - Click "Run" to start the program and adjust the execution speed.
    - Click "Step" to execute one instruction at a time and observe its effect on the registers and memory.

4.  **Debug:**

	- Use the step-by-step mode and the device windows to find and fix logic errors.

### "Hello World" Example

Here is the classic "Hello World" implemented in MicroSim:

```asm
; ----- Program Section ------------

    mov CL, 0xC0  ; Display address
    mov BL, 0x50  ; String address
Rep:
    mov AL, [BL]  ; Character address
    cmp AL, 0x00  ; End of string?
    jz  Fin
    mov [CL], AL  ; Display character
    inc CL        ; Next character
    inc BL        ; Next location
    jmp Rep
Fin:
    hlt

; ----- Data Section ------------

    org 0x50
    
    db  "Hello World"
    db  0x00
```

## Building from Source

If you want to build the project from the source files, you can use the provided shell scripts.

**Prerequisites:**

- A Java Development Kit (JDK) version 21 or newer.

**Steps:**

1.  **Compile the code:**

    - The `compile.sh` script compiles the `.java` files from the `src` directory into the `class` directory.
    - You may need to edit the script to set the correct path to your JDK's `javac` compiler.

2.  **Build the JAR:**

    - The `build.sh` script packages the compiled classes and data resources into an executable JAR file.
    - The final `.jar` and its dependencies will be placed in the `build` directory.

## Documentation

A comprehensive reference manual for the assembler, including the full instruction set, is available.

- **[Read the HTML Manual](man/man.html)**

## License

Copyright (c) 2025 Jérôme Lehuen

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

