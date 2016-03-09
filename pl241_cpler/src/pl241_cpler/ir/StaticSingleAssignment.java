package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

import pl241_cpler.backend.Location;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.VariableSet.variable;
import pl241_cpler.ir.DefUseChain.chainNode;

import java.util.HashMap;
import java.util.HashSet;

public class StaticSingleAssignment extends Instruction {
	
	public static final int showSSA = 0,
			 				showREG = 1,
			 				showAsm = 2;
	
	private static ControlFlowGraph.Block loadSlot = null;
	private static HashSet<dominatorTree.treeNode> visited = new HashSet<dominatorTree.treeNode>();
	
	private LinkedList<Instruction> version = new LinkedList<Instruction>();
	private LinkedList<Block> phiFrom = null;
	private static Stack<HashSet<VariableSet.variable>> phiRecord = new Stack<HashSet<VariableSet.variable>>();
	
	private static int showType = showSSA;
	
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2){
		super(insType_, o1, o2);
		if(insType_ == phi){
			phiFrom =  new LinkedList<Block>();
			phiFrom.add(null);phiFrom.add(null);
		}
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
	
	public StaticSingleAssignment(int insType, Location a, Location b, Location c) {
		super(insType, null, null);
		version.add(null);
		version.add(null);
		this.insType = insType;
		this.la = a;
		this.ll.set(0, b);
		this.ll.set(1, c);
	}
	
	public StaticSingleAssignment(int insType, Location a, Operand b, Location c){
		super(insType, b, null);
		version.add(null);
		version.add(null);
		this.insType = insType;
		this.la = a;
		this.ll.set(1, c);
	}
	
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

	private static boolean isDef(Instruction i){
		return (i.insType == store && i.ops.size() == 3) || i.insType == kill || i.insType == move || i.insType == phi;
	}
	
	@Override
	public void setOpSeqId(){
		for(int i = version.size() - 1; i>=0 ;i--){
			if(version.get(i) != null && version.get(i).insType!=decl && ops.get(i).getType() == opScale)
				opsInsId.set(i, version.get(i).outputId);
			if(ops.get(i)!=null  &&ops.get(i).getType() == opIns)
				opsInsId.set(i, ((Instruction)ops.get(i)).outputId);
		}
	}
	
	@Override
	public void setOpLoc(){
		for(int i = version.size()-1; i>=0; i--){
			Instruction ins = version.get(i);
			if(ins!=null){
				ll.set(i, ins.la);
			}else if(ops.get(i)!=null&&ops.get(i).getType()==opIns){
				Instruction insResult = (Instruction)ops.get(i);
				ll.set(i, insResult.la);
			}else if(ops.get(i)!=null){
				int id = 0;
				if(ops.get(i).getType() == opConstant)
					id = ((Constant)ops.get(i)).getValue();
				ll.set(i, new Location(CON, id));
			}
		}
	}
		
	public LinkedList<Block> getPhiFrom() {
		return phiFrom;
	}
	
	public static void setShowType(int showType) {
		StaticSingleAssignment.showType = showType;
	}
	
	//functions below are used for rename variable
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
		if(def.getIns().getInsType() == kill){
			def.getIns().setInsType(load);
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
						ssaPhi.phiFrom.set(oi, locate);
						break;
					}
				}
			}
		}
	}
	
	private static void processBlock(ControlFlowGraph.Block b, HashMap<Block, HashSet<Block>> rdoSet){
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
			//System.out.println(f.print()+" : ");
			dominatorTree t = new dominatorTree(cfg.getFuncSet().get(f));
			cfg.putFuncDTree(f, t);
			visited.clear();
			loadSlot = cfg.getFuncSet().get(f).get(0);
			dfsT(t.getTreeRoot(), t.getRDSet());
		}
	}
	
	public String ssaPrint(Operand o, int i){
		String insprint = "";
		if(o.getType() == opIns)
			insprint += "(" + Integer.toString(((Instruction)o).getId())+ ")";
		else if(o.getType() == opFunc){
			if(((VariableSet.function)o).getIdentId()==callerFunc)
				insprint += "->ret";
			else
				insprint += o.print();
		}else if(o.getType()<=opArray&&version.get(i)!=null)
			insprint += o.print()+"_"+Integer.toString(version.get(i).id);
		else
			insprint += o.print();
		return insprint;
	}
	
	public String regPrint(){
		String insprint = "";
		if(la != null){
			insprint+=la.print();
		}
		insprint += "\t";
		int opNum;
		if(showType == showREG)
			opNum = ops.size();
		else
			opNum = 2;
		for(int i=0;i<opNum;i++){
			if(ll.get(i)!=null){
				insprint+=ll.get(i).print();
				if(ops.get(i)!=null&&showType == showREG)
					insprint+="{"+ssaPrint(ops.get(i), i)+"}";
			}else if(showType == showREG){
				Operand o = ops.get(i);
				if(o!=null)
					insprint += ssaPrint(o, i);
			}
			insprint += "\t";
		}
		return insprint;
	}
	
	@Override
	public String printVcg(){
		String insprint = Integer.toString(id)+" : ";
		insprint+=codeToName(insType)+" ";
		for(int i = 0; i<ops.size();i++){
			Operand o = ops.get(i);
			if(o != null)
					insprint += ssaPrint(o, i)+" ";
		}
		return insprint;
	}
	
	public String print(){
		String insprint = Integer.toString(id)+"\t";
		if(seqId>=0)
			insprint += Integer.toString(seqId)+"\t";
		else
			insprint += "\t";
		if(outputId>=0)
			insprint += Integer.toString(outputId)+"\t";
		else
			insprint += "\t";
		if(PC>=0)
			insprint += Integer.toString(PC)+"\t";
		else
			insprint += "\t";
		insprint+=": " + codeToName(insType)+"\t";
		if(showType == showAsm)
			insprint+="& " + asmToName(assemblyType);
		insprint +=	"\t";
		if(showType == showSSA)
			for(int i = 0; i<ops.size();i++){
				Operand o = ops.get(i);
				if(o != null)
						insprint += ssaPrint(o, i);
				if(i<2)
					insprint += "\t";
			}
		else{
			insprint += regPrint();
		}
		return insprint;
	}
		
	private static final int 
							 opScale =  0,
							 opArray = 	1,
							 opConstant	=	3;
	private static final int CON = 3;
	private static final int kill 		= -2;
	private static final int callerFunc		=	500;

}
