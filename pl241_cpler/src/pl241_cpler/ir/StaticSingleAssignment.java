package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.VariableSet.variable;
import pl241_cpler.ir.DefUseChain.chainNode;

import java.util.HashMap;
import java.util.HashSet;

public class StaticSingleAssignment extends Instruction {
	
	public static void pushNewPhiBlock(){
		phiRecord.push(new HashSet<VariableSet.variable>());
	}
		
	public static void popPhiToCurBlock(ControlFlowGraph cfg){
		if(!phiRecord.isEmpty()){
			HashSet<VariableSet.variable> curPhi = phiRecord.pop();
			for(VariableSet.variable v : curPhi){
				cfg.addInsToCurBlockHead(Instruction.genIns(phi, v, v));
			}
		}
	}
	
	private void addAssignmentForPhi(){
		if(isDef(this)){
			if(!phiRecord.isEmpty()){
				for(HashSet<VariableSet.variable> vs : phiRecord){
					vs.add((variable) ops.get(1));// we always move/store value to the 2nd operands
				}
			}
		}
	}

	public String print(){
		String insprint = codeToName(insType);
		insprint +=	"\t";
		for(int i = 0; i<ops.size();i++){
			Operand o = ops.get(i);
			if(o != null)
				if(o.getType() == opIns)
					insprint += "(" + Integer.toString(((Instruction)o).getId())+ ") " +"\t";
				else if(o.getType() == opFunc){
					insprint += o.print();
					if(((VariableSet.function)o).getIdentId()==callerFunc)
						insprint += "-> return";
					else
						insprint += "->" + ((VariableSet.function)o).getBlock().print();
				}else if(o.getType()<=opArray&&version.get(i)!=null)
					insprint += o.print()+"_"+Integer.toString(version.get(i).id)+"\t";
				else
					insprint += o.print()+"\t";
		}
		return insprint;
	}

	public StaticSingleAssignment(int insType_, Operand o1, Operand o2){
		super(insType_, o1, o2);
		version.add(null);
		version.add(null);
		addAssignmentForPhi();
	}

	public StaticSingleAssignment(int insType_, Operand o1, Operand o2, Operand o3){
		super(insType_, o1, o2, o3);
		version.add(null);
		version.add(null);
		version.add(null);
		addAssignmentForPhi();
	}	
	
	private static boolean isDef(Instruction i){
		return (i.insType == store && i.ops.size() == 3) || i.insType == kill || i.insType == move || i.insType == phi;
	}
	
	private static void setDU(chainNode def, Instruction use, int oi){
		def.addUse(use);
		((StaticSingleAssignment)use).version.set(oi, def.getIns());
	}
	
	private static chainNode prepareDef(Block locate, VariableSet.variable var,HashMap<Block, HashSet<Block>> rdoSet){
		DefUseChain odu = var.getDU();
		chainNode def = odu.getDef(locate, rdoSet);
		if(def == null){
			Instruction initialLoad = Instruction.genIns(load, null, var);
			loadSlot.addInsToHead(initialLoad);
			initialLoad.setBlock(loadSlot);
			odu.addDef(initialLoad, rdoSet);
			((StaticSingleAssignment)initialLoad).version.set(1, initialLoad);
			def = odu.getDef(locate, rdoSet);
		}
		if(def == null){
			def = null;
		}
		return def;
	} 
	
	private static void processPhi(Block locate, LinkedList<Instruction> insList, HashMap<Block, HashSet<Block>> rdoSet){
		for(Instruction i1 : insList){
			if(i1.insType == phi){
				StaticSingleAssignment ssaPhi = (StaticSingleAssignment)i1;
				for(int oi = i1.ops.size()-1; oi>=0; oi--){
					Operand o = i1.ops.get(oi);
					VariableSet.variable var =  (VariableSet.variable)o;
					if(ssaPhi.version.get(oi)==null){
						chainNode def = prepareDef(locate, var, rdoSet);
						setDU(def, i1, oi);
						break;
					}
				}
			}
		}
	}
	
	private static void processBlock(ControlFlowGraph.Block b, HashMap<Block, HashSet<Block>> rdoSet){
		System.out.println("process :" + b.print());
		for(int iI = 0; iI<b.getInsList().size(); iI++){
			Instruction i = b.getInsList().get(iI);
			///process variable used by instruction except phi
			if(i.insType!=phi){
				for(int oi = i.ops.size()-1; oi>=0; oi--){
					if(isDef(i)&&oi==1){
						continue;
					}else{
						Operand o = i.ops.get(oi);
						if(o!=null&&o.getType()<=opArray){
							VariableSet.variable var =  (VariableSet.variable)o;
							DefUseChain odu = var.getDU();
							//the instruction adda use the address of the array instead of the value, so return the declaration
							if(i.insType == adda&&o.getType()==opArray){
								((StaticSingleAssignment)i).version.set(oi, odu.getDecl());
								continue;
							}
							chainNode def = prepareDef(i.locate, var, rdoSet);
							setDU(def, i, oi);
						}
					}
				}
			}
			///process definitions
			if(isDef(i)){
				DefUseChain odu = ((VariableSet.variable)i.ops.get(1)).getDU();
				odu.addDef(i, rdoSet);
				if(i.insType!=phi)
					((StaticSingleAssignment)i).version.set(1, i);
			}
		}
		/////successor phi!
		LinkedList<Block> successors = b.getSuccessor();
		if(successors!=null){
			for(Block s : successors){
				processPhi(b, s.getInsList(), rdoSet);
			}
		}
	}
	
	private static void dfsT(dominatorTree.treeNode r, HashMap<Block, HashSet<Block>> rdoSet){
		visited.add(r);
		processBlock(r.b_, rdoSet);
		LinkedList<dominatorTree.treeNode> cList = r.getChild();
		if(cList!=null){
			for(dominatorTree.treeNode c : cList){
				if(!visited.contains(c))
					dfsT(c, rdoSet);
			}
		}
		
	}
	
	public static void varRename(ControlFlowGraph cfg){
		for(VariableSet.function f : cfg.getFuncSet().keySet()){
			System.out.println(f.print()+" : ");
			dominatorTree t = new dominatorTree(cfg.getFuncSet().get(f));
			visited.clear();
			loadSlot = cfg.getFuncSet().get(f).get(0);
			dfsT(t.getTreeRoot(), t.getRDSet());
		}
	}
	
	private static ControlFlowGraph.Block loadSlot = null;
	private static HashSet<dominatorTree.treeNode> visited = new HashSet<dominatorTree.treeNode>();
	
	private LinkedList<Instruction> version = new LinkedList<Instruction>();;
	private static Stack<HashSet<VariableSet.variable>> phiRecord = new Stack<HashSet<VariableSet.variable>>();

	private static final int 
							 opScale =  0,
							 opArray = 	1;
	private static final int whileRoute = 3;
	private static final int kill 		= -2;
	private static final int callerFunc		=	500;
}
