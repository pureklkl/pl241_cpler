package pl241_cpler.ir;

import java.util.ArrayList;

public class Instruction implements Operand{
		
	public Instruction(int insType_, Operand o1, Operand o2){
		ops = new ArrayList<Operand>();
		insType = insType_;
		ops.add(o1);
		ops.add(o2);
		this.locate = curLocate;
	}
	//record value, address, variable name for array - the only three operands instruction
	public Instruction(int insType_, Operand o1, Operand o2, Operand o3){
		ops = new ArrayList<Operand>();
		insType = insType_;
		ops.add(o1);
		ops.add(o2);
		ops.add(o3);
		this.locate = curLocate;
	}	
	
	public int getInsType(){
		return insType;
	}
	
	public int getType(){
		return opIns;
	}
	
	public int getId(){
		return id;
	}
	
	public static void genSSA(){
		irType = irSSA;
	}
	
	public static void genSimple(){
		irType = irSimple;
	}
	
	public static Instruction genIns(int insType_, Operand o1, Operand o2){
		if(irType == irSimple){
			return new Instruction(insType_, o1, o2);
		}else{
			return new StaticSingleAssignment(insType_, o1, o2);
		}
	}
	public static Instruction genIns(int insType_, Operand o1, Operand o2, Operand o3){
		if(irType == irSimple){
			return new Instruction(insType_, o1, o2, o3);
		}else{
			return new StaticSingleAssignment(insType_, o1, o2, o3);
		}
	}
	public void setBlock(ControlFlowGraph.Block b){
		locate = b;
	}
	
	public static void setCurBlock(ControlFlowGraph.Block b){
		curLocate = b;
	}
	
	public static void setVarSet(VariableSet varSet_){
		varSet = varSet_;
	}
	
	public ControlFlowGraph.Block getBlock(){
		return locate;
	}
	
	//set first parameter to be o, used for fix bra which used for if/while statement
	public void fix(Operand o){
		ops.set(1, o);
	}
	
	public String print(){
		String insprint = codeToName(insType);
		insprint +=	"\t";
		for(Operand o : ops){
			if(o != null)
				if(o.getType() == opIns)
					insprint += "(" + Integer.toString(((Instruction)o).getId())+ ") " +"\t";
				else if(o.getType() == opFunc){
					insprint += o.print();
					if(((VariableSet.function)o).getIdentId()==callerFunc)
						insprint += "-> return";
					else
						insprint += "->" + ((VariableSet.function)o).getBlock().print();
					}
				else
					insprint += o.print()+"\t";
		}
		return insprint;
	}
	
	public String codeToName(int insCode){
		switch(insCode){
		case	decl	:	return new String("decl ");
		case	neg		:	return new String("neg ");
		case	add		:	return new String("add ");
		case	sub		:	return new String("sub ");
		case	mul		:	return new String("mul ");
		case	div		:	return new String("div ");
		case	cmp		:	return new String("cmp ");
		
		case	adda	:	return new String("adda ");
		case	load	:	return new String("load ");
		case	store	:	return new String("store ");
		case	move	:	return new String("move ");
		case	phi		:	return new String("phi ");
		case	end		:	return new String("end ");
		case	bra		:	return new String("bra ");
		
		case	bne		:	return new String("bne ");
		case	beq		:	return new String("beq ");
		case	ble		:	return new String("ble ");
		case	blt		:	return new String("blt ");
		case	bge		:	return new String("bge ");
		case	bgt		:	return new String("bgt ");
		
		case	read	:	return new String("read ");
		case	write	:	return new String("write ");
		case	writeNL	:	return new String("writeNL ");
		default			:	return new String("Error Ins");
		}
	}
	
	protected ArrayList<Operand> ops;
	protected ControlFlowGraph.Block locate;
	
	protected static ControlFlowGraph.Block curLocate;
	protected int insType;
	protected boolean singleIns;
	protected int id = insCreated++;
	protected static int insCreated = 0;
	protected static VariableSet varSet;
	
	protected static final int opIns = 2,
							   opFunc	= 4;
	private static final int irSimple = 0;
	private static final int irSSA = 1;
	
	private static int irType = irSimple;
	public static final int
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
	protected static final int 	defaultFuncMin	=	255,
								defaultFuncMax	=	1024,
								OutputNum      	=	300,
								OutputNewLine 	= 	301,//OutPutNum(x)
								InputNum		=	302,//x=InputNum()
								callerFunc		=	500;
}
