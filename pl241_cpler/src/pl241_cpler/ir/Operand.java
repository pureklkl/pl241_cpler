package pl241_cpler.ir;

/**
 * operand type 
 * 0 - scale
 * 1 - instruction
 * 2 - constant
 **/
public interface Operand{
	int getType();
	String print();
	static int dummy = -1;;
}
