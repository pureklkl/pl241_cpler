package pl241_cpler.backend;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.Constant;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.Operand;
import pl241_cpler.ir.StaticSingleAssignment;
import pl241_cpler.ir.VariableSet;
import pl241_cpler.ir.VariableSet.function;
import pl241_cpler.ir.VariableSet.variable;
import pl241_cpler.ir.VariableSet.variableScope;
import pl241_cpler.optimization.CopyPropagation;

public class DLXCodeGeneration {
	//TODO Memory layout
	//TODO Transfer to assembly, except for branch Ins, cfg structure retain
	//TODO Assign PC, and resolve bra
	//TODO test with no function call test case
	//TODO Function call convention 
	//TODO test all
		
	private VariableSet varSet;
	private ControlFlowGraph cfg;
	private VariableSet.function curFunc = null;
	
	private LinkedList<Instruction> braAssign = new LinkedList<Instruction>();
	int PC = 0;
	int maxPC = 0;// stack is after maxPC+1

	private Location reg0 = new Location(REG, REG0),
					 spillREG1 = new Location(REG, SPILLREG1),
					 spillREG2 = new Location(REG, SPILLREG2),
					 fpREG = new Location(REG, FPREG),
					 spREG = new Location(REG, SPREG),
					 heapBaseREG = new Location(REG, HEAPBASEREG),
					 retAddrREG = new Location(REG, RETADDRREG),
					 
					 stackPUSH = new Location(CON, 4),
					 stackPOP = new Location(CON, -4),
					 returnOFF = new Location(CON, -8);
	
	public DLXCodeGeneration(VariableSet varSet, ControlFlowGraph cfg){
		this.varSet = varSet;
		this.cfg = cfg;
	}
	
	public void setSpillRegister(Location sp1, Location sp2){
		sp1.id = SPILLREG1;
		sp2.id = SPILLREG2;
	}
	
	/**
	 * R[30] = MemSize - 1; heap
	 * R[a] = M[(R[b]+c) / 4]; memory address is in byte type
	 * Address is like 0, -4, -8....,
	 * example layout for function func
	 * int func(a, b, c) var d e f, spill stk 1,2,3, used reg 1,2,3
	 * 	a				-5
	 *	b				-4
	 *	c				-3
	 *	return value	-2
	 *	caller FP		-1
	 *	return address	0	<-point by R28(FP)
	 *	d				1
	 *	e				2
	 *	f				3
	 *	stk	1			4
	 *	stk	2			5
	 *	stk	3			6
	 *	used reg 1		7
	 *	used reg 2		8
	 *	used reg 3		9	<-point by R29(SP)
	*/
	private void globalVarMem(){
		variableScope gs = varSet.getGlobalScope();
		int baseOffset = 0;
		for(variable v:gs.getVarSet().values()){
			if(v.getType() == opScale){
				v.setAddrOffset(baseOffset);
				baseOffset-=4;
			}else if(v.getType() == opArray){//array is positive growth
				baseOffset-=4*((VariableSet.array)v).getArraySize();
				v.setAddrOffset(baseOffset);
				baseOffset-=4;
			}
		}
	}
	private void addUsedSTKREG(Location l, HashSet<Location> usedSTKREG){
		if(l!=null&&l.type!=MEM)
			usedSTKREG.add(l);
	}
	private int assignLocVar(VariableSet.function func){
		int varSize = localVarOffset;
		int parOff = func.getReturnState()?parOffset:parOffset+1;//if no return value, parameter can be adjacent to FP
		if(func.getVscope()!=null){
			varSize = func.assignVar(localVarOffset, parOff);
		}
		return varSize;
	}
	private HashSet<Location> countUseLoc(LinkedList<Block> bSet){
		HashSet<Location> usedSTKREG = new HashSet<Location>();
		for(Block b : bSet){
			for(Instruction ins : b.getInsList()){
				addUsedSTKREG(ins.getLoc(0), usedSTKREG);
				addUsedSTKREG(ins.getLoc(1), usedSTKREG);
				addUsedSTKREG(ins.getOutputLoc(), usedSTKREG);
			}
		}
		return usedSTKREG;
	}
	private void localVarMem(){
		for(Map.Entry<VariableSet.function, LinkedList<Block>> e : cfg.getFuncSet().entrySet()){
			VariableSet.function func = e.getKey();
			HashSet<Location> usedSTKREG = countUseLoc(e.getValue());
			int offset = assignLocVar(func);
			func.assignSTKREG(usedSTKREG, offset);
		}
	}
	private void memLayer(){
		globalVarMem();
		localVarMem();
	}
	//Maybe memory to memory move for phi resolution
	//Store load for spilled register
	private int calConstant(Instruction ins){
		int op0 = ins.getLoc(0).getId(), op1 = ins.getLoc(1).getId();
		switch(ins.getInsType()){
		case adda:
		case add:	return op0+op1;
		case sub:	return op0-op1;
		case mul:	return op0*op1;
		case div:	return op0/op1;
		case cmp:   return (int) Math.signum(op0-op1);
		default: return 0;
		}
	}
	//Constant constraint - op0 must be reg op1 can be constant (ADD, MUL can exchange op)
	private void processMathIns(Instruction ins, int index, LinkedList<Instruction> insList){
		index = processSpill(ins, index, insList);
		Operand op1 = ins.getOp(1);
		Location lb = ins.getLoc(0), lc = ins.getLoc(1);
		if(ins.getInsType() == adda){
			VariableSet.array a = (VariableSet.array)op1;
			Instruction toByteAddr = Instruction.genLIR(mul, spillREG1, lb, new Location(CON, 4));//TODO use LSH
			processMathIns(toByteAddr, index, insList);
			insList.add(index, toByteAddr);
			ins.resetOpLoc(spillREG1, 0);
			ins.resetOpLoc(new Location(CON, a.getAddrOffset()), 1);
			lb = ins.getLoc(0);lc= ins.getLoc(1);
		}
		if(lb.type == CON&&lc.type == CON){
			int res = calConstant(ins);
			ins.resetOpLoc(new Location(CON, res), 1);
			ins.resetOpLoc(reg0, 0);
			ins.setInsType(add);
		}else if(lb.type == CON){
			if(ins.getInsType() == add||ins.getInsType() == mul){//just exchange
				Location tmp;
				tmp = ins.getLoc(0);
				ins.resetOpLoc(ins.getLoc(1), 0);
				ins.resetOpLoc(tmp, 1);
			}else{
				conToReg(lb, index, insList);
				ins.resetOpLoc(spillREG2, 0);
			}
		}
		lc= ins.getLoc(1);
		if(lc.type == CON){
			ins.setAssemblyType(insTypeTransfer(ins.getInsType(), CONINSOFF));
		}else{
			ins.setAssemblyType(insTypeTransfer(ins.getInsType()));
		}
	}
	
	private void resetLSIns(Instruction ins, int assembleType, Location a, Location c, int scopeLevel){
		if(scopeLevel == global)
			 resetIns(ins, assembleType, a, heapBaseREG, c);
		else
			resetIns(ins, assembleType, a, fpREG, c);
	}
	
	private void resetIns(Instruction ins, int assembleType, Location a, Location b, Location c){
		ins.setAssemblyType(assembleType);
		ins.setOutputLocation(a);
		ins.resetOpLoc(b, 0);
		ins.resetOpLoc(c, 1);
	}
	/**
	 * move constant to spill reg2
	 * 
	 * @param con
	 * @param index
	 * @param insList
	 */
	private void conToReg(Location con, int index, LinkedList<Instruction> insList){
		Instruction toReg = Instruction.genLIR(add, spillREG2, reg0, con);
		toReg.setAssemblyType(ADD+CONINSOFF);
		insList.add(index, toReg);
	}
	
	//parser: l/s array, return value, parameter; ssa : load scale; spill : l/s stk/mem
	private void processLSIns(Instruction ins, int index, LinkedList<Instruction> insList){
		if(ins.getInsType() == store){
			if(ins.getOp(1)!=null&&ins.getOp(1).getType() == opParam){//transfer parameter
				resetIns(ins, PSH, ins.getLoc(0), spREG, stackPUSH);
			}else if(ins.getOp(1)!=null&&ins.getOp(1).getType() == opArray){//store array
				VariableSet.array a = (VariableSet.array)ins.getOp(1);
				resetLSIns(ins, STW, ins.getLoc(0), ins.getLoc(2), a.getScopeLevel());
			}else if(ins.getOp(1)!=null&&ins.getOp(1).getType() == opFunc){//store return value
				resetIns(ins, STW, ins.getLoc(0), fpREG, returnOFF);
			}else if(ins.getOp(0)!=null&&ins.getOp(0).getType()==opScale&&ins.getOp(1)==null){//global variable kill
				resetIns(ins, STW, ins.getLoc(0), heapBaseREG, new Location(CON, ((VariableSet.scale)ins.getOp(0)).getAddrOffset()));
			}else if(ins.getRetainVar()!=null){
				resetLSIns(ins, STW, ins.getLoc(0), new Location(CON,ins.getRetainVar().getAddrOffset()), ins.getRetainVar().getScopeLevel());
			}else if(ins.getLoc(0)==spillREG1){//spill
				if(ins.getOutputLoc().getLocType() == STK){
					resetIns(ins, STW, ins.getLoc(0), fpREG, new Location(CON,curFunc.findLoc(ins.getOutputLoc())));
				}else if(ins.getOutputLoc().getLocType() == MEM){
					VariableSet.variable v = varSet.findById(ins.getOutputLoc().id);
					resetLSIns(ins, STW, ins.getLoc(0), new Location(CON,  v.getAddrOffset()), v.getScopeLevel());
				}
			}
			if(ins.getOutputLoc().type == CON){//can only store R.a to memory!
				conToReg(ins.getOutputLoc(), index, insList);
				ins.setOutputLocation(spillREG2);
			}
		}else if(ins.getInsType() == load){
			if(ins.getOp(1)!=null&&ins.getOp(1).getType() == opScale){//load parameter, global variable kill
				VariableSet.scale s = (VariableSet.scale)ins.getOp(1);
				resetLSIns(ins, LDW, ins.getOutputLoc(), new Location(CON, s.getAddrOffset()), s.getScopeLevel());
			}else if(ins.getOpSize()==3&&ins.getOp(2).getType() == opArray){//load array
				VariableSet.array a = (VariableSet.array)ins.getOp(2);
				resetLSIns(ins, LDX, ins.getOutputLoc(), ins.getLoc(1), a.getScopeLevel());
			}else if(ins.getOp(1)!=null&&ins.getOp(1).getType() == opFunc){//load return value
				resetIns(ins, POP, ins.getOutputLoc(), spREG, stackPOP);
			}else if(ins.getOutputLoc() == spillREG1||ins.getOutputLoc() == spillREG2){//spill
				if(ins.getLoc(0).getLocType() == STK){
					resetIns(ins, LDW, ins.getOutputLoc(), fpREG, new Location(CON,curFunc.findLoc(ins.getLoc(0))));
				}else if(ins.getLoc(0).getLocType() == MEM){
					VariableSet.variable v = varSet.findById(ins.getLoc(0).id);
					resetLSIns(ins, LDW, ins.getOutputLoc(), new Location(CON,  v.getAddrOffset()), v.getScopeLevel());
				}
			}
		}
		lsSpill(ins, index, insList);
	}
	private void processMove(Instruction ins, int index, LinkedList<Instruction> insList) {
		Location l0 = ins.getLoc(0), l1 = ins.getLoc(1);
		if(l0.type == REG&&l1.type == REG){
			resetIns(ins, ADD, l1, reg0, l0);
		}else if(l0.type == CON&&l1.type == REG){
			resetIns(ins, ADD+CONINSOFF, l1, reg0, l0); 
		}else if(l1.type == MEM&&l0.type == REG){
			VariableSet.variable v = varSet.findById(l1.id);
			resetLSIns(ins, STW, l0,  new Location(CON,  v.getAddrOffset()), v.getScopeLevel());
		}else if(l1.type == STK&&l0.type == REG){
			resetIns(ins, STW, l0, fpREG, new Location(CON, curFunc.findLoc(l1)));
		}else if(l0.type == MEM&&l1.type == REG){
			VariableSet.variable v = varSet.findById(l0.id);
			resetLSIns(ins, LDW, l1, new Location(CON,  v.getAddrOffset()), v.getScopeLevel());
		}else if(l0.type == STK&&l1.type == REG){
			resetIns(ins, LDW, l1, fpREG, new Location(CON, curFunc.findLoc(l0)));
		}else if(l0.type == CON && l1.type == MEM){
			conToReg(l0, index, insList);
			VariableSet.variable v = varSet.findById(l1.getId());
			resetLSIns(ins, STW, spillREG2, new Location(CON, v.getAddrOffset()), v.getScopeLevel());
		}else if(l0.type == CON && l1.type == STK){
			conToReg(l0, index, insList);
			resetIns(ins, STW, spillREG2, fpREG, new Location(CON, curFunc.findLoc(l1)));
		}else if((l0.type == STK||l0.type == MEM)&&(l1.type == STK||l1.type == MEM)){
			ins.resetOpLoc(spillREG1, 1);//sp2 is used to unlock circular move, so that we can only use sp1 to spill the mem to mem move
			processSpill(ins, index, insList);//it will issue load l0 to spillREG1 and store spillREG1 to l1
			insList.remove(ins);
		}
	}
	private int lsSpill(Instruction ins, int index, LinkedList<Instruction> insList){
		if(ins.getInsType() == store){
			if(ins.getOutputLoc().getLocType() == MEM||ins.getOutputLoc().getLocType() == STK){
				Instruction ld = Instruction.genLIR(load, spillREG1, ins.getOutputLoc(), spillREG1);
				processLSIns(ld, index, insList);
				insList.add(index, ld);
				ins.setOutputLocation(spillREG1);;
			}
		}else if(ins.getInsType() == load){
			if(ins.getOutputLoc().getLocType() == MEM||ins.getOutputLoc().getLocType() == STK){
				Instruction st = Instruction.genLIR(store, ins.getOutputLoc(), spillREG1, ins.getOutputLoc());
				processLSIns(st, index, insList);
				insList.add(index+1, st);
				ins.setOutputLocation(spillREG1);
			}
		}
		return index;
	}
	private int processSpill(Instruction ins, int index, LinkedList<Instruction> insList){
		for(int i = 0; i<2;i++){
			Location sr = i==0?spillREG1:spillREG2;
			if(ins.getLoc(i)!=null&&(ins.getLoc(i).getLocType()==MEM||ins.getLoc(i).getLocType()==STK)){
				Instruction ld = Instruction.genLIR(load, sr, ins.getLoc(i), sr);
				processLSIns(ld, index, insList);
				insList.add(index, ld);
				ins.resetOpLoc(sr, i);
				index++;
			}
		}
		if(ins.getOutputLoc()!=null&&(ins.getOutputLoc().getLocType()==MEM||ins.getOutputLoc().getLocType()==STK)){
			Instruction st = Instruction.genLIR(store, ins.getOutputLoc(), spillREG1, ins.getOutputLoc());
			processLSIns(st, index, insList);
			insList.add(index+1, st);
			ins.setOutputLocation(spillREG1);
		}
		return index;
	}
	private int insTypeTransfer(int insType){
		int assembleType = insTypeTransfer(insType, 0);
		return assembleType;
	}
	private int insTypeTransfer(int insType, int conOffset){
		int assembleType = 0;
		switch(insType){
		case adda:
		case add: assembleType = ADD;break;
		case sub: assembleType = SUB;break;
		case mul: assembleType = MUL;break;
		case div: assembleType = DIV;break;
		case cmp: assembleType = CMP;break;
		case beq: assembleType = BEQ;break;
		case bne: assembleType = BNE;break;
		case blt: assembleType = BLT;break;
		case bge: assembleType = BGE;break;
		case ble: assembleType = BLE;break;
		case bgt: assembleType = BGT;break;
		case read: assembleType = RDD;break;
		case write: assembleType = WRD;break;
		}
		return assembleType+conOffset;
	}
	private void processEnd(Instruction ins){
		ins.setAssemblyType(RET);
		ins.resetOpLoc(reg0, 1);
	}
	private void processBra(Instruction ins){
		if(ins.getOp(0)==null&&ins.getOp(1)==null){//return
			ins.setAssemblyType(RET);
			ins.resetOpLoc(retAddrREG, 1);
		}else{//if/while function call
			ins.setAssemblyType(JSR);// R.c will be set after PC assign
			braAssign.add(ins);
		}
	}
	private void processConditionBra(Instruction ins, int index, LinkedList<Instruction> insList){
		Location l0 = ins.getLoc(0);
		if(l0.type == CON){
			conToReg(l0, index, insList);
			ins.resetOpLoc(spillREG2, 0);
		}else if(l0.type == STK||l0.type == MEM){
			processSpill(ins, index, insList);
		}
		ins.setAssemblyType(insTypeTransfer(ins.getInsType()));
		ins.setOutputLocation(ins.getLoc(0));
		ins.resetOpLoc(null, 0);
		braAssign.add(ins);// R.c will be set after PC assign
	}
	private void processRead(Instruction ins, int index, LinkedList<Instruction> insList){
		processSpill(ins, index, insList);
		ins.setAssemblyType(insTypeTransfer(ins.getInsType()));
	}
	private void processWrite(Instruction ins, int index, LinkedList<Instruction> insList){
		Location l0 = ins.getLoc(0);
		if(l0.type == CON){
			conToReg(l0, index, insList);
			ins.resetOpLoc(spillREG2, 0);
		}else if(l0.type == STK||l0.type == MEM){
			processSpill(ins, index, insList);
		}
		ins.setAssemblyType(insTypeTransfer(ins.getInsType()));
	}
	private void processBlock(Block b){
		LinkedList<Instruction> insList = b.getInsList();
		for(int i = insList.size()-1; i>=0; i--){
			Instruction ins = insList.get(i);
			switch(ins.getInsType()){
			case add:
			case sub:
			case mul:
			case div:
			case cmp:
			case adda: processMathIns(ins, i, insList);break;
			case load:
			case store:processLSIns(ins, i, insList);break;
			case move: processMove(ins, i, insList);break;
			case end: processEnd(ins);break;
			case bra: processBra(ins);break;
			case beq:
			case bne:
			case blt:
			case bge:
			case ble:
			case bgt:processConditionBra(ins, i, insList);break;
			case read:processRead(ins, i, insList);break;
			case write:processWrite(ins, i, insList);break;
			case writeNL: ins.setAssemblyType(WRL);break;
			case phi:insList.remove(i);break;
			}
		}
	}

	private void assembly(){
		for(Map.Entry<VariableSet.function, LinkedList<Block>> e : cfg.getFuncSet().entrySet()){
			curFunc = e.getKey();
			for(Block b : e.getValue()){
				processBlock(b);
			}
		}
	}
	/**
	 * first block	-	ins1:	save return address by push R31
	 * 					ins2:	move sp register to fp register, so that fp register is pointed to return address
	 * 					ins3:	move sp to the end of stack which contains variable and temporary (spilled value)
	 * 					ins4~:	push working register which will be recovered when return
	 * 
	 * @param first	-	first block of the function
	 * @param func	-	the callee function
	 */
	private void assignCalleeInit(Block first, VariableSet.function func){
		Instruction storeRetAddr = Instruction.genLIR(store, retAddrREG, spREG, stackPUSH);
		storeRetAddr.setAssemblyType(PSH);
		first.getInsList().add(0, storeRetAddr);
		Instruction newFP = Instruction.genLIR(move, fpREG, spREG, reg0);
		newFP.setAssemblyType(ADD);
		first.getInsList().add(1, newFP);
		Instruction moveSp = Instruction.genLIR(add, spREG, spREG, new Location(CON, func.getStackEnd()));
		moveSp.setAssemblyType(ADD+CONINSOFF);
		first.getInsList().add(2, moveSp);
		int index=3;
		for(Location l:func.getUsedSTKREG()){
			if(l.type == REG){
				Instruction pshClrReg = Instruction.genLIR(store, l, spREG, stackPUSH);
				pshClrReg.setAssemblyType(PSH);
				first.getInsList().add(index, pshClrReg);
				index++;
			}
		}
	}
	/**
	 * last block	-	ins1~:	pop working register
	 * 					ins2:	move fp regiter to sp register, so that sp is reseted to pointed to return address
	 * 					ins3:	pop return address to R31
	 * @param last	- return ins's block
	 * @param func	- the callee function
	 */
	private void assignCalleeRET(Block last, VariableSet.function func){
		int endIns = last.getInsList().size()-1;//last ins is RET R31
		for(int i = func.getUsedSTKREG().size()-1;i>=0;i--){
			Location l=func.getUsedSTKREG().get(i);
			if(l.type == REG){
				Instruction popClrReg = Instruction.genLIR(load, l, spREG, stackPOP);
				popClrReg.setAssemblyType(POP);
				last.getInsList().add(endIns, popClrReg);
				endIns = last.getInsList().size()-1;
			}
		}
		Instruction resetSp = Instruction.genLIR(move, spREG, fpREG, reg0);
		resetSp.setAssemblyType(ADD);
		last.getInsList().add(endIns, resetSp);
		endIns = last.getInsList().size()-1;
		Instruction loadRetAddr = Instruction.genLIR(load, retAddrREG, spREG, stackPOP);
		loadRetAddr.setAssemblyType(POP);
		last.getInsList().add(endIns, loadRetAddr);//now sp is pointed to the old fp
	}
	
	private void assignCaller(Block cur, Block suc, VariableSet.function func){
		int endIns;
		if(func.getReturnState()){
			endIns = cur.getInsList().size()-1;//last ins will be JSR func
			Instruction retValR = Instruction.genLIR(add, spREG, spREG, new Location(CON, 4));//keep space for return value
			retValR.setAssemblyType(ADD+CONINSOFF);
			cur.getInsList().add(endIns, retValR);
		}
		Instruction pushFP = Instruction.genLIR(store, fpREG, spREG, stackPUSH);
		pushFP.setAssemblyType(PSH);
		endIns = cur.getInsList().size()-1;
		cur.getInsList().add(endIns, pushFP);//now spREG is pointed to old FP

		
		Instruction popFp = Instruction.genLIR(load, fpREG, spREG, stackPOP);
		popFp.setAssemblyType(POP);
		if(func.paramNum()!=0){
			Instruction parSp = Instruction.genLIR(sub, spREG, spREG, new Location(CON, func.paramNum()*4));
			parSp.setAssemblyType(SUB+CONINSOFF);
			if(suc.getInsList().isEmpty()){
				suc.getInsList().add(0, parSp);
			}else{
				Instruction firstSuc = suc.getInsList().getFirst();
				if(firstSuc.getInsType()==load&&firstSuc.getOp(1)!=null&&firstSuc.getOp(1).getType()==opFunc){//pop parameters should be after pop return value
					suc.getInsList().add(1, parSp);
				}else{
					suc.getInsList().add(0, parSp);
				}
			}
		}
		suc.getInsList().add(0, popFp);
	}
	
	private void funCall(){
		VariableSet.function mainFunc = (function) varSet.getGlobalVar().get(mainToken);
		for(Map.Entry<VariableSet.function, LinkedList<Block>> e : cfg.getFuncSet().entrySet()){
			if(e.getKey()!=mainFunc)
				assignCalleeInit(e.getValue().getFirst(), e.getKey());
			for(Block b : e.getValue()){
				for(Instruction ins: b.getInsList()){
					if(ins.getInsType()==bra&&ins.getOp(1)!=null&&ins.getOp(1).getType()==opFunc){
						assignCaller(b, b.getSuccessor().getFirst(), (VariableSet.function)ins.getOp(1));
						break;
					}else if(ins.getInsType() == bra && ins.getOp(0)==null && ins.getOp(1)==null){
						assignCalleeRET(b, e.getKey());
						break;
					}
				}
			}
		}
	}
	
	private Block findNonEmptyB(Block b, HashSet<Block> visited){
		visited.add(b);
		LinkedList<Instruction> bList = b.getInsList();
		Block res = null;
		if(bList.isEmpty()){
			for(Block c : b.getSuccessor()){
				if(!visited.contains(c)){
					res = findNonEmptyB(c, visited);
				}
				if(res!=null){
					break;
				}
			}
			return res;
		}else{
			return b;
		}
	}
	
	private void resolvePC(){
		for(Instruction ins : braAssign){
			Operand op1 = ins.getOp(1);
			Location jumpTo;
			if(op1.getType() == opBlock){
				Block b = (Block)op1;
				HashSet<Block> visited = new HashSet<Block>();
				b = findNonEmptyB(b, visited);
				if(ins.getAssemblyType() == JSR)
					jumpTo = new Location(CON, (b.getInsList().get(0).getPC()));
				else
					jumpTo = new Location(CON, ((b.getInsList().get(0).getPC()-ins.getPC())/4));
				
			}else{
				VariableSet.function func = (VariableSet.function)op1;
				jumpTo = new Location(CON, cfg.getFuncSet().get(func).get(0).getInsList().get(0).getPC());
				
			}
			ins.resetOpLoc(jumpTo, 1);
		}
	}
	private void assignBlock(Block b) {
		for(Instruction ins : b.getInsList()){
			ins.setPC(PC);
			PC+=4;
		}
	}
	private void initialIns(Block b){
		int spStart = ((VariableSet.function)varSet.getGlobalVar().get(mainToken)).getStackEnd();
		Instruction initFp = Instruction.genLIR(move, fpREG, new Location(CON, maxPC), fpREG),
				 	initSp = Instruction.genLIR(move, spREG, new Location(CON, maxPC+spStart), spREG);
		initFp.setPC(0);
		initSp.setPC(4);
		processMove(initFp, 0, null);
		processMove(initSp, 0, null);
		b.getInsList().add(0, initSp);
		b.getInsList().add(0, initFp);
	}
	private void pcAssign(){
		PC = startPC;
		VariableSet.function mainFunc = (function) varSet.getGlobalVar().get(mainToken);
		for(Block b : cfg.getFuncSet().get(mainFunc))
			assignBlock(b);
		for(Map.Entry<VariableSet.function, LinkedList<Block>> e : cfg.getFuncSet().entrySet()){
			if(e.getKey()!=mainFunc)
				for(Block b : e.getValue()){
					assignBlock(b);
				}
		}
		resolvePC();
		maxPC = PC;//there is a flag after the last instruction
		initialIns(cfg.getFuncSet().get(mainFunc).getFirst());
	}

	//TODO test memLayer
	//TODO test move
	
	public void assembleDLX(){
		memLayer();
		assembly();
		funCall();
		pcAssign();
	}
		
	public int getMaxPC() {
		return maxPC;
	}
	public void setMaxPC(int maxPC) {
		this.maxPC = maxPC;
	}
	
	public ControlFlowGraph getCfg() {
		return cfg;
	}
	
	private static void testIns(){
		Instruction.genSSA();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showAsm);
		
		LinkedList<Instruction> testList = new LinkedList<Instruction>();
		VariableSet testvSet = new VariableSet();
		VariableSet.function testFunc = testvSet.new function();
		testFunc.setName("test", 100);
		testvSet.addAndMoveToNewScope();
		testFunc.setVscope(testvSet.getCurScope());
		VariableSet.scale sd = testvSet.new scale(), se=testvSet.new scale();
		sd.setName("sd", 10);se.setName("se", 11);
		testvSet.add(10, sd);testvSet.add(11, se);
		Location a = new Location(STK, 1), b = new Location(STK, 2), c = new Location(STK, 3),
				 d = new Location(MEM, sd.getId()), e = new Location(MEM, se.getId());
		DLXCodeGeneration codeGen = new DLXCodeGeneration(testvSet, null);
		
		HashSet<Location> usedL = new HashSet<Location>();
		usedL.add(c);usedL.add(b);usedL.add(c);
		int varOff = testFunc.assignVar(localVarOffset, parOffset);
		testFunc.assignSTKREG(usedL, varOff);
		codeGen.curFunc = testFunc;
		//mem to mem move
		Instruction t1 = Instruction.genLIR(move, c, b, c),
					t2 = Instruction.genLIR(move, e, d, e);
		testList.add(t1);testList.add(t2);
		codeGen.processMove(t2, 1, testList);
		codeGen.processMove(t1, 0, testList);
		for(Instruction ins:testList){
			System.out.println(ins.print());
		}
		testList.clear();
		System.out.println();
		//mem to mem s/l
		t1 = Instruction.genIns(store, sd, testFunc);
		t1.resetOpLoc(d, 0);
		t2 = Instruction.genIns(load, null, testFunc);
		t2.setOutputLocation(e);
		testList.add(t1);testList.add(t2);
		codeGen.processLSIns(t2, 1, testList);
		codeGen.processLSIns(t1, 0, testList);
		for(Instruction ins:testList){
			System.out.println(ins.print());
		}
		testList.clear();
		System.out.println();
		//mem to mem add
		t1 = Instruction.genLIR(add, c, d, e);
		testList.add(t1);
		codeGen.processMathIns(t1, 0, testList);
		for(Instruction ins:testList){
			System.out.println(ins.print());
		}
		testList.clear();
		System.out.println();
	}
	
	private static void test(String[] args){
		int maxReg = 8;
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		VariableSet varSet = p.getVarSet();
		LiveTime lt = new LiveTime(cfg);
		lt.analysisLiveTime();
		LinearScan allocator = new LinearScan(lt.getLr(), maxReg);
		allocator.allocate();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showREG);
		allocator.regAllocate(cfg);
		cfg.print();
		DLXCodeGeneration codeGen = new DLXCodeGeneration(varSet, cfg);
		LinkedList<Location> lsp = allocator.getSp1Sp2();
		codeGen.setSpillRegister(lsp.get(0), lsp.get(1));
		codeGen.assembleDLX();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showAsm);
		
		varSet.printLayout();
		cfg.print();
	}
	
	static void testCP(String[] args){
		int maxReg = 8;
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		p.getCFG().print();
		
		System.out.println("Copy Propagation!!!");
		
		CopyPropagation g = new CopyPropagation(p);
		g.runCP();
		p.getCFG().print();
		
		ControlFlowGraph cfg = p.getCFG();
		VariableSet varSet = p.getVarSet();
		LiveTime lt = new LiveTime(cfg);
		lt.analysisLiveTime();
		LinearScan allocator = new LinearScan(lt.getLr(), maxReg);
		allocator.allocate();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showREG);
		allocator.regAllocate(cfg);
		cfg.print();
		DLXCodeGeneration codeGen = new DLXCodeGeneration(varSet, cfg);
		LinkedList<Location> lsp = allocator.getSp1Sp2();
		codeGen.setSpillRegister(lsp.get(0), lsp.get(1));
		codeGen.assembleDLX();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showAsm);
		
		varSet.printLayout();
		cfg.print();
		
	}
	
	public static void main(String[] args){
		testCP(args);
	}
	
	static final int mainToken		=	200;
	
	static final int parOffset = -3,
					 returnValueOffset = -2,
					 callerFPOffset = -1,
					 returnAddrOffset = 0,
					 localVarOffset = 1;
	static final int	opScale			= 	0,
						opArray			= 	1,
						opIns			=	2,
						opConstant		=	3,
						opFunc			=	4,
						opBlock			=	5,
						opParam			=	6;
	static final int 	global			=	0,
						local			=	1;
	static final int REG = 0,//register
					 MEM = 1,//variable
					 STK = 2,//stack
					 CON = 3;//constant
	static final int CONINSOFF = 16;
	static final int startPC = 8;//first and second ins is initialize FP and SP register
	static final int REG0 = 0,
					 SPILLREG1 = 26,
					 SPILLREG2 = 27,
					 FPREG = 28,
					 SPREG = 29,
					 HEAPBASEREG = 30,
					 RETADDRREG = 31;
	static final int ADD = 0,//ADDI 16
					 SUB = 1,//SUBI 17
					 MUL = 2,//MULI 18
					 DIV = 3,//DIVI 19
					 CMP = 5,//CMPI 21
					 
					 LDW = 32,
					 LDX = 33,
					 POP = 34,
					 STW = 36,
					 STX = 37,
					 PSH = 38,
					 
					 BEQ = 40,
					 BNE = 41,
					 BLT = 42,
					 BGE = 43,
					 BLE = 44,
					 BGT = 45,
					 
					 BSR = 46,
					 JSR = 48,
					 RET = 49,
					 
					 RDD = 50,
					 WRD = 51,
					 WRH = 52,
					 WRL = 53;
	
	static final int add	=	11,
					 sub	=	12,
					 mul	=	1,
					 div	=	2,
					 cmp	=	5,
		
					 adda	=	40,
					 load	=	41,
					 store	=	42,
					 move	=	43,
					 phi	=	44,
					 end	=	45,
					 bra	=	46,
					
					 bne	=	20,
					 beq	=	21,
					 ble	=	25,
					 blt	=	23,
					 bge	=	22,
					 bgt	=	24,
					
					 read	=	30,
					 write	=	31,
					 writeNL	=	32;
}
