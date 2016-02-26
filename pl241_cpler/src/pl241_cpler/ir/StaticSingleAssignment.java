package pl241_cpler.ir;

import java.util.LinkedList;
import java.util.Stack;

import pl241_cpler.ir.VariableSet.variable;

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
		popFixWhileBlock();
		//once meet a join all previous killers become old
		//phi need fresh killer to determine the definition, so update it after phi insertion 
		if(!stackedKiller.isEmpty()){
			for(int i = stackedKiller.size()-1;i>=0;i--){
				if(stackedKiller.get(i).insType == oldKill)
					break;
				((StaticSingleAssignment)stackedKiller.get(i)).killUpdate();
			}
		}
	}
	
	public static void pushNewWhileBlock(){
		whileInsRecord.push(new LinkedList<Instruction>());
	}
	
	public static void popFixWhileBlock(){
		LinkedList<Instruction> fixIns = whileInsRecord.pop();
		whileFix = true;
		for(Instruction ins : fixIns){
			((StaticSingleAssignment)ins).findVersion();
		}
		whileFix = false;
	}
	
	private void addAssignmentForPhi(){
		if((insType == store && ops.size() == 3) || insType == move){
			if(!phiRecord.isEmpty()){
				for(HashSet<VariableSet.variable> vs : phiRecord){
					vs.add((variable) ops.get(1));// we always move/store value to the 2nd operands
				}
			}
		}
	}
	
	private void killForPhi(VariableSet.variable v){
		if(!phiRecord.isEmpty()){
			for(HashSet<VariableSet.variable> vs : phiRecord){
				vs.add(v);// we always move/store value to the 2nd operands
			}
		}
	}
	
	private void killUpdate(){
		insType = oldKill;
	}
	
	public static void addKillForGlobalVariable (ControlFlowGraph cfg){
		HashMap<Integer, variable> gv = varSet.getGlobalVar();
		Instruction killer = Instruction.genIns(kill, null, null);
		killer.setBlock(cfg.getCurBlock());
		stackedKiller.push(killer);
		for(VariableSet.variable v : gv.values()){
			v.getDU().addDef(killer);
		}
	}
	

	private void findVersion(){
		boolean isPop = insType==phi?true:false;
		for(int i = 0; i<ops.size();i++){
			Operand o = ops.get(i);
			//0 for scale, 1 for array
			if(o != null && o.getType()<=opArray && version.get(i) == null){
				DefUseChain odu = ((VariableSet.variable)o).getDU();
				DefUseChain.chainNode defNode = odu.getDef(locate, isPop, this);
				Instruction def = defNode.defIns;
				int defLoopLevel = def.getInsType()==decl?0:def.locate.getLoopLevel();
				if(this.insType == adda){
					if(o.getType() == opArray){
						def = ((VariableSet.array)o).getDU().defChain.get(0).defIns;//if adda return decl as define
						version.set(i, def);
						continue;
					}
				}
				///if definition is in the outer loop, the definition will possibly be the un-added phi func,  push ins
				if(((this.locate.getLoopLevel()>defLoopLevel)&&(!whileFix))||// the def is in the outer loop when not fix
						((whileInsRecord.size()>defLoopLevel)&&(whileFix))){//def is in the outer loop when in fix loop level 
						whileInsRecord.peek().add(this);
						continue;
				}
				//meet with decl, kill, oldkill
				else if(def.getInsType() <= decl){
					ControlFlowGraph.Block loadLocate = null;
					switch(def.getInsType()){
					case decl:loadLocate = this.getBlock().funcHead();break;
					case kill:loadLocate = def.getBlock();break;
					case oldKill:loadLocate = this.getBlock();break;
					}
					if(this.insType != load){//when other instruction cannot find definition, add a load as the definition
						if(o.getType() == opScale){
							def =new StaticSingleAssignment(o);
							loadLocate.addInsToHead(def);
							odu.addDef(def);
							defNode = odu.getDef(locate, isPop, this);
							def = defNode.defIns;
							defNode.addUse(this);
						}
						if(o.getType() == opScale ||o.getType() == opArray)
							killForPhi((VariableSet.variable)o);
					}else{//it is the first load, so it becomes a definition
						def = this;
						odu.addDef(def);
					}
				}
				//add use instruction when find real definition
				else{
					defNode.addUse(this);
				}
				version.set(i, def);
			}
		}
		//phi need pop three definition at max
		if(isPop){
			((VariableSet.variable)ops.get(0)).getDU().getDef(locate, isPop, this);
		}
	}
	
	
	private void addDef(){
		if(insType == move || (insType == store&&ops.size()==3)){
			((VariableSet.variable)ops.get(1)).getDU().addDef(this);
		}
	}
	
	private void addPhiDef(){
		if(insType == phi){
			((VariableSet.variable)ops.get(1)).getDU().addDef(this);
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
				}else if(o.getType()<=opArray)
					insprint += o.print()+"&"+Integer.toString(version.get(i).id)+"\t";
				else
					insprint += o.print();
		}
		return insprint;
	}
	
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2){
		super(insType_, o1, o2);
		version.add(null);
		version.add(null);
		addAssignmentForPhi();
		addDef();
		findVersion();
		addPhiDef();
	}
	//only for store/load array
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2, Operand o3){
		super(insType_, o1, o2, o3);
		version.add(null);
		version.add(null);
		version.add(null);
		addAssignmentForPhi();
		addDef();
		findVersion();
		addPhiDef();
	}
	private StaticSingleAssignment(Operand o1){
		super(load, null, o1);
		version.add(null);
		version.add(null);
		version.add(null);
		version.set(1, this);
	}	
	
	private static boolean whileFix = false;
	private static Stack<Instruction> stackedKiller = new Stack<Instruction>();
	private LinkedList<Instruction> version = new LinkedList<Instruction>();;
	private static Stack<HashSet<VariableSet.variable>> phiRecord = new Stack<HashSet<VariableSet.variable>>();
	private static Stack<LinkedList<Instruction>> whileInsRecord = new Stack<LinkedList<Instruction>>();
	private static final int 
							 opScale =  0,
							 opArray = 	1;
	private static final int whileRoute = 3;
	private static final int oldKill	=-3,
							 kill 		= -2;
}
