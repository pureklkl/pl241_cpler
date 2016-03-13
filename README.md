# pl241_cpler

Compiler Course Project - compiler of education oriented language PL241

Description:

	The compiler

	is based on a descent down parser, Static Single Assignment(SSA) and Linear Scan register allocator;

	can optimize programs by Copy propagation and Common Sub-Expression Elimination;

	can generate executable code for DLX simulator;

	can generate VCG files for the program's Control Flow Graph and Dominator Tree, 
	which include the code generated after each step of optimization and the register allocation result.


Compile Usage:

	pl241_cpl.core.CLI filename [Compiler Options]

	The program file name must be the first argument.

	Example: pl241_cpl.core.CLI test001.txt cp cse sim

Compiler Options:

	cp		:	enable copy propagation.

	cse 	: 	enable common sub-expression elimination.

	sim 	:	run code by DLX simulator, output will be printed in the console.
	(The DLX simulator is from http://www.michaelfranz.com/CS241/DLX.java)

	debug	:	run code by a debug version of DLX simulator which will also show the code it running in the console.
