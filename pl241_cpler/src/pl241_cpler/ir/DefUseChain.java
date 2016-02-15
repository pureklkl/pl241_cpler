package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

public class DefUseChain {
	
	class chainNode{
		Instruction defIns;
		LinkedList<Instruction> usedIns;
		LinkedList<Instruction> nextDef;
		LinkedList<Instruction> prevDef;
	}
	Stack<Instruction> stackedDef;
	
	static int defStatus;
	public static final int normalDef 	= 	1;
	public static final int ifdef		=	2;
	public static final int whiledef	=	3;
	
	public DefUseChain(){
		duHead = new chainNode();
		duHead.defIns = Instruction.genIns(decl, null, null);
		curDef = duHead;
	}
	
	public void setDefStatus(int status){
		defStatus = status;
	}
	
	public void addDef(){
		
	}
	
	public void setIfDef(){
		
	}
	
	public void setElseDef(){
		
	}
	
	public void addJoinDef(){
		
	}
	
	public void addUse(){
		
	}
	
	public Instruction getDef(){
		return curDef.defIns;
	}
	
	chainNode duHead;
	chainNode curDef;
	
	public static final int decl = -1; 
}
