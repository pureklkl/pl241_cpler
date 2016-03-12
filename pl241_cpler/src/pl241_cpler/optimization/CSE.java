package pl241_cpler.optimization;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.StaticSingleAssignment;
import pl241_cpler.ir.dominatorTree;
import pl241_cpler.ir.dominatorTree.treeNode;

public class CSE {
	Stack<StaticSingleAssignment> firstAppear = new Stack<StaticSingleAssignment>();
	
	ControlFlowGraph cfg;
	
	public CSE(ControlFlowGraph cfg){
		this.cfg = cfg;
	}
	
	private StaticSingleAssignment retrieval(StaticSingleAssignment ssaIns){
		for(StaticSingleAssignment s : firstAppear){
			if(s.cseEqual(ssaIns))
				return s;
		}
		return null;
	}
	
	private void processBlock(Block b){
		LinkedList<Instruction> bList = b.getInsList();
		for(int i=0; i<bList.size();i++){
			StaticSingleAssignment ssaIns = (StaticSingleAssignment)bList.get(i);
			for(int i1=0;i1<ssaIns.getOps().size();i1++){
				if(ssaIns.getOp(i1)!=null&&ssaIns.getOp(i1).getType()==opIns){
					StaticSingleAssignment ssaOp = (StaticSingleAssignment)ssaIns.getOp(i1);
					if(ssaOp.getCseLinke()!=null){
						ssaIns.getOps().set(i1, ssaOp.getCseLinke());
					}
				}
			}
			if(isSE(ssaIns)){
				StaticSingleAssignment res = retrieval(ssaIns);
				if(res!=null){
					ssaIns.setCseLinke(res);
					bList.remove(ssaIns);
					i--;
				}else{
					firstAppear.push(ssaIns);
				}
			}
		}
	}
	
	private void popFirst(Block b){
		while(!firstAppear.isEmpty()){
			if(firstAppear.peek().getBlock()==b){
				firstAppear.pop();
			}
			else{
				break;
			}
		}
	}
	
	private void dfs(treeNode n, HashSet<treeNode> visited){
		visited.add(n);
		processBlock(n.getBlock());
		if(n.getChild()!=null){
			for(treeNode c : n.getChild()){
				if(!visited.contains(c)){
					dfs(c, visited);
					popFirst(c.getBlock());
				}
			}
		}
	}
	
	private boolean isSE(Instruction ins){
		int insType = ins.getInsType();
		return insType == neg	|| insType == add	|| insType == sub	|| insType == mul	|| insType == div	||
			   insType == cmp	|| 
			   insType == adda	|| insType == load ||
			   (insType == phi	&& !ins.isArray());// phi for array has no output
	}
	
	public void runCSE(){
		HashSet<treeNode> visited = new HashSet<treeNode>();
		for(dominatorTree t : cfg.getFuncDTree().values()){
			visited.clear();
			firstAppear.clear();
			dfs(t.getRoot(),visited);
		}
	}
	
	public static void main(String[] args){
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		p.getCFG().print();
		
		System.out.println("CSE!!!");
		
		CSE cse = new CSE(p.getCFG());
		cse.runCSE();
		p.getCFG().print();
	}
	
	static final int opIns			=	2;
	
	//instruction code
	static final int
						kill			=	-2,
						decl			=	-1,
						neg				= 	0,
						add				=	11,
						sub				=	12,
						mul				=	1,
						div				=	2,
						cmp				=	5,

						adda			=	40,
						load			=	41,
						store			=	42,
						move			=	43,
						phi				=	44,
						end				=	45,
						bra				=	46,
						
						bne				=	20,
						beq				=	21,
						ble				=	25,
						blt				=	23,
						bge				=	22,
						bgt				=	24,
						
						read			=	30,
						write			=	31,
						writeNL			=	32;

}
