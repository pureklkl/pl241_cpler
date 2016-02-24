package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

public class DefUseChain {
	
	class chainNode{
		Instruction defIns;
		LinkedList<Instruction> usedIns;
		LinkedList<Instruction> nextDef;
		LinkedList<Instruction> prevDef;//only phi ins has multiple defs for same var
	}

	Stack<Instruction> stackedDef;
	Stack<chainNode> defchain = new Stack<chainNode>();
	
	public DefUseChain(){
		duHead = new chainNode();
		duHead.defIns = Instruction.genIns(decl, null, null);
		defchain.push(duHead);
	}
	
	public void addDef(){
		
	}
	
	public void addUse(){
		
	}
	
	public Instruction getDef(){
		return defchain.peek().defIns;
	}
	
	chainNode duHead;
	
	public static final int decl = -1; 
}
