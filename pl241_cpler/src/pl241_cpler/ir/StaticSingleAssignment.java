package pl241_cpler.ir;

import java.util.LinkedList;
/**
 * operand type 
 * 0 - scale
 * 1 - instruction
 * 2 - constant
 **/
interface Operand{
	int getType();
	static int dummy = -1;;
}

class Constant{
	public int getType(){
		return opIns;
	}
	public int getValue(){
		return val;
	}
	private int val;
	private static int opIns = 3;
}

class Instruction implements Operand{
	
	public int getType(){
		return opIns;
	}
	
	public int getId(){
		return id;
	}
	
	private int insType;
	private boolean singleIns;
	private int id = insCreated++;
	private static int opIns = 2;
	private static int insCreated = 0;
}

public class StaticSingleAssignment extends Instruction {
	
	
	
	private LinkedList<Operand> ops;
	private LinkedList<Integer> version;
}
