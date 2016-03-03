package pl241_cpler.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

import pl241_cpler.ir.ControlFlowGraph.Block;

public class DefUseChain {
	
	Stack<chainNode> stackedDef = new Stack<chainNode>();
	Stack<chainNode> defchain = new Stack<chainNode>();
	
	public Stack<chainNode> getDefChian(){
		return defchain;
	}
	
	public DefUseChain(){
		defchain.push(new chainNode(Instruction.genIns(decl, null, null)));
	}
	
	public DefUseChain(DefUseChain du) {
		// TODO Auto-generated constructor stub
	}

	private chainNode updateStackedIns(Block locate, HashMap<Block, HashSet<Block>> rdoSet){
		chainNode res = null;
		//In dominator tree, when move from one subtree to another subtree, pop the definitions from original subtree
		while(!stackedDef.isEmpty()){
			chainNode d = stackedDef.peek();
			if(!rdoSet.get(locate).contains(d.defIns.getBlock()))
				stackedDef.pop();
			else{
				res = d;
				break;
			}
		}
		return res;
	}
	
	public Instruction getDecl(){
		return defchain.get(0).defIns;
	}
	
	public chainNode getDef(Block locate, HashMap<Block, HashSet<Block>> rdoSet){
		return updateStackedIns(locate, rdoSet);
	}
	public void addDef(Instruction def, HashMap<Block, HashSet<Block>> rdoSet){
		chainNode previousDef = updateStackedIns(def.getBlock(), rdoSet);
		chainNode newNode = new chainNode(def);
		defchain.push(newNode);
		if(previousDef!=null&&previousDef.defIns.getBlock()==def.getBlock())
			stackedDef.set(stackedDef.size()-1, newNode);
		else
			stackedDef.push(newNode);
	}

	public Stack<chainNode> getStackedDef(){
		return stackedDef;
	}
	
	public class chainNode{
		public chainNode(Instruction def){
			defIns = def;
		}
		public void addUse(Instruction use){
			usedIns.add(use);
		}
		public Instruction getIns(){
			return defIns;
		}
		
		public LinkedList<Instruction> getUsedIns(){
			return usedIns;
		}
		
		Instruction defIns;
		LinkedList<Instruction> usedIns = new LinkedList<Instruction>();
	}
	
	public static final int decl = -1; 
}
