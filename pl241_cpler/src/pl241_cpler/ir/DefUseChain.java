package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

public class DefUseChain {
	
	class chainNode{
		public chainNode(Instruction def){
			defIns = def;
		}
		public void addUse(Instruction use){
			usedIns.add(use);
		}
		Instruction defIns;
		LinkedList<Instruction> usedIns = new LinkedList<Instruction>();
	}

	Stack<Instruction> stackedDef = new Stack<Instruction>();
	Stack<chainNode> defchain = new Stack<chainNode>();
	
	public DefUseChain(){
	}
	
	public void pushDefStack(){
		stackedDef.push(null);
	}
	public void popDefStack(){
		stackedDef.pop();
	}
	public void addDef(Instruction def){
		defchain.push(new chainNode(def));
		stackedDef.set(stackedDef.size()-1, def);
	}
	
	public Instruction getDef(){
		Instruction res = null;
		for(int i = stackedDef.size()-1; i>=0; i--){
			res = stackedDef.get(i);
			if(res != null){
				break;
			}
		}
		return res;
	}
	
	public static final int decl = -1; 
}
