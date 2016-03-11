package pl241_cpler.optimization;

import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.DefUseChain;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.StaticSingleAssignment;
import pl241_cpler.ir.VariableSet;
import pl241_cpler.ir.VariableSet.variable;
import pl241_cpler.ir.dominatorTree;
import pl241_cpler.ir.dominatorTree.treeNode;
import pl241_cpler.ir.DefUseChain.chainNode;

import java.util.HashSet;

import pl241_cpler.frontend.Parser;


public class CopyPropagation {
	Parser p;
	DefUseChain vdu = new DefUseChain();
	VariableSet varSet;
	ControlFlowGraph cfg;
	
	public CopyPropagation(Parser o){
		p = o;
		varSet = p.getVarSet();// get variable set
		cfg = o.getCFG();
	}
	
	public void getCP(DefUseChain v, Instruction cp){
		for(chainNode i : v.getDefChian()){
			if (i.getIns() == cp){
				for(Instruction j: i.getUsedIns())
				{
					if(j.getInsType() == store && j.getOp(1)==null){//special case, save global variable, we should retain the variable information
						j.setRetainAddr();
					}
					if( ((StaticSingleAssignment) j).getVersion().get(0) == i.getIns())
					{
						j.getOps().set(0,i.getIns().getOps().get(0));
						((StaticSingleAssignment) j).getVersion().set(0, ((StaticSingleAssignment ) i.getIns()).getVersion().get(0));
					}
					if( ((StaticSingleAssignment) j).getVersion().get(1) == i.getIns())
					{
						j.getOps().set(1,i.getIns().getOps().get(0));
						((StaticSingleAssignment) j).getVersion().set(1,  ((StaticSingleAssignment ) i.getIns()).getVersion().get(0));
					}
				}
				break;
			}
		}
	}
	
	private void processBlock(Block b){
		for(int i = 0;i<b.getInsList().size();i++){
			Instruction ins = b.getInsList().get(i);
			if(ins.getInsType() == move && ins.getOp(1).getType() == opScale){
				variable v = (variable)ins.getOp(1);
				vdu = v.getDU();
				getCP(vdu, ins);
				b.getInsList().remove(ins);
				i--;
			}
		}
	}
	
	private void dfs(treeNode n, HashSet<treeNode> visited){
		visited.add(n);
		processBlock(n.getBlock());
		if(n.getChild()!=null){
			for(treeNode c : n.getChild()){
				if(!visited.contains(c))
					dfs(c, visited);
			}
		}
	}
	
	public void runCP(){
		HashSet<treeNode> visited = new HashSet<treeNode>();
		for(dominatorTree t : cfg.getFuncDTree().values()){
			visited.clear();
			dfs(t.getRoot(),visited);
		}
	}
	
	/**
	public void RunCP(){
		for(VariableSet.variableScope gS : varSet.getScopeList()){ // get global variable scope
			boolean flag  = true;
			while(flag){
				flag = false;
				for(VariableSet.variable v : gS.getVarSet().values()){// get variable - check type!
					if(v.getType() == opScale){
						vdu = v.getDU();//get variable's def-use chain
						getCP(vdu);
					}
				}
			}
		}	
	}
	
	public boolean getCP(DefUseChain v){
		boolean flag = false;
			for(chainNode i : v.getDefChian()){
				if (i.getIns().getInsType() == move){
					for(Instruction j: i.getUsedIns())
					{
						if( ((StaticSingleAssignment) j).getVersion().get(0) == i.getIns())
						{
							flag = true;
							j.getOps().set(0,i.getIns().getOps().get(0));
							((StaticSingleAssignment) j).getVersion().set(0, ((StaticSingleAssignment ) i.getIns()).getVersion().get(0));
						}
						if( ((StaticSingleAssignment) j).getVersion().get(1) == i.getIns())
						{
							flag = true;
							j.getOps().set(1,i.getIns().getOps().get(0));
							((StaticSingleAssignment) j).getVersion().set(1,  ((StaticSingleAssignment ) i.getIns()).getVersion().get(0));
						}
					}
					i.getIns().getBlock().getInsList().remove(i.getIns());
				}
			}
		return flag;
	}
	*/
	static final int opScale = 0;
	static final int store = 42,
	                 move =	43;
}