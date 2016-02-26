package pl241_cpler.ir;

import java.util.HashSet;
import java.util.Stack;

public class DefUseChain {
	
	class chainNode{
		Instruction defIns;
		HashSet<Instruction> usedIns = new HashSet<Instruction>();
		//LinkedList<Instruction> nextDef;
		//LinkedList<Instruction> prevDef;//only phi ins has multiple defs for same var
		public chainNode(Instruction defIns_){
			defIns = defIns_;
		}
		public void addUse(Instruction ins){
			usedIns.add(ins);
		}
	}

	Stack<chainNode> stackedDef = new Stack<chainNode>();
	Stack<chainNode> defChain = new Stack<chainNode>();
	
	public DefUseChain(){
		defChain.push(new chainNode(Instruction.genIns(decl, null, null)));
	}
	
	public void addDef(Instruction def){
		chainNode newDefNode = new chainNode(def);
		defChain.push(newDefNode);
		//if stackedDef is empty, it is the first define
		if(stackedDef.isEmpty()){
			stackedDef.push(newDefNode);
		}else{
			switch(def.getBlock().compareRoute(stackedDef.peek().defIns.getBlock())){
			case sameRoute	:	stackedDef.set(stackedDef.size()-1, newDefNode);break;
			case dominatorRoute	:
			case reverseDominatorRoute	:
			case otherRoute		:	stackedDef.push(newDefNode);break;
			}
		}
	}
	
	public chainNode getDef(ControlFlowGraph.Block b, boolean isPop, Instruction phi){
		//no initialization, return the declaration
		if(stackedDef.isEmpty())
			return defChain.get(0);
		else{
			chainNode def = null;
			for(int i = stackedDef.size()-1; i>=0&&stackedDef.get(i)!=null; i--){
				//phi can't pop itself
				if(isPop){
					if(stackedDef.get(i).defIns == phi)
						continue;
				}
				int compRouteR = b.compareRoute(stackedDef.get(i).defIns.getBlock());
				if(compRouteR < otherRoute){
					def =  stackedDef.get(i);
					if((compRouteR == reverseDominatorRoute||compRouteR == sameRoute) && isPop){
						stackedDef.remove(i);
					}
					return def;
				}
			}
		}
		//no initialization for this route, return the declaration
		return defChain.get(0);
	}
	
	
	public static final int 
							oldKill = -3,
							kill 	= -2,
							decl 	= -1; 
	static final int 
					sameRoute		= 0,
					dominatorRoute  = 1,
					reverseDominatorRoute  = 2,
					otherRoute		= 3;
}
