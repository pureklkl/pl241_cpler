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
	}
	//only for store/load array
	public StaticSingleAssignment(int insType_, Operand o1, Operand o2, Operand o3){
		super(insType_, o1, o2, o3);
		version.add(null);
		version.add(null);
		version.add(null);
		addAssignmentForPhi();
	}
	private StaticSingleAssignment(Operand o1){
		super(load, null, o1);
		version.add(null);
		version.add(null);
		version.add(null);
		version.set(1, this);
	}	
	
	private LinkedList<Instruction> version = new LinkedList<Instruction>();;
	private static Stack<HashSet<VariableSet.variable>> phiRecord = new Stack<HashSet<VariableSet.variable>>();

	private static final int 
							 opScale =  0,
							 opArray = 	1;
	private static final int whileRoute = 3;
	private static final int oldKill	=-3,
							 kill 		= -2;
	private static final int callerFunc		=	500;
}
