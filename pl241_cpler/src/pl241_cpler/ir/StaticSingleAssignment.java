package pl241_cpler.ir;

import java.util.LinkedList;

public class StaticSingleAssignment extends Instruction {
	
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2){
		super(insType_, o1, o2);
	}
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2, Operand o3){
		super(insType_, o1, o2, o3);
	}
	
	private LinkedList<Instruction> version;
}
