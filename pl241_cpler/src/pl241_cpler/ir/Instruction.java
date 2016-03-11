package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import pl241_cpler.backend.Location;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.VariableSet.variable;

public class Instruction implements Operand{
	
	protected ArrayList<Operand> ops;
	protected ControlFlowGraph.Block locate;
	
	protected static ControlFlowGraph.Block curLocate;
	protected int insType;
	protected boolean singleIns;
	protected int id;
	private static int insCreated = 0;
	
	private static final int irSimple	= 0,
			                 irSSA 		= 1;
	protected static int irType = irSimple;
	
	// used for live range analysis
	protected int seqId = -1;
	protected int outputId = nonOutput;
	protected LinkedList<Integer> opsInsId = new LinkedList<Integer>();
	protected Operand output = null;
	
	//regsiter allocation
	protected Location la = null;//output a
	protected LinkedList<Location> ll = new LinkedList<Location>();//operand b, operand c
	
	//code generation
	protected int assemblyType;
	protected int PC = -1;
	
	//copy propagation
	protected variable retainVar = null;
	protected Location retainAddr = null;
	
	public Instruction(int insType_, Operand o1, Operand o2){
		ops = new ArrayList<Operand>();
		insType = insType_;
		ops.add(o1);opsInsId.add(null);ll.add(null);
		ops.add(o2);opsInsId.add(null);ll.add(null);
		this.locate = curLocate;
		id = insCreated++;
	}
	
	public Instruction(int insType_, Operand o1, Operand o2, Operand o3){
		ops = new ArrayList<Operand>();
		insType = insType_;
		ops.add(o1);opsInsId.add(null);ll.add(null);
		ops.add(o2);opsInsId.add(null);ll.add(null);
		ops.add(o3);opsInsId.add(null);ll.add(null);
		this.locate = curLocate;
		id = insCreated++;
	}	
	
	//private copy constructor, used for deep clone
	private Instruction(Instruction i){
		id = i.id;
		insType = i.insType;
		
		//following copy need assistance from hashmap to corresponding new copy result
		ops = new ArrayList<Operand>();
		ops.add(i.ops.get(0));
		ops.add(i.ops.get(1));		
		if(i.ops.size()>2)
			ops.add(i.ops.get(2));
		locate = i.locate;
		
	}
	
	public boolean isReal(){
		return !((insType == load&&ops.size()==2&&ops.get(1).getType() == opArray)||(insType == kill));
	}
	
	public boolean isArray() {
		return ops.get(0).getType() == opArray;
	}
	
	public void unRealRemove(){
		locate.getInsList().remove(this);
	}
	
	public void setSeqId(int seqId){
		this.seqId = seqId;
	}
	
	public int getSeqId(){
		return seqId;
	}
	
	public int getOutputId() {
		return outputId;
	}

	//output maybe a define for scale or instruction result
	public void setOutputId(int outputId) {
		this.outputId = outputId;
		if((insType == move|| insType == load)&&ops.get(1).getType() == opScale){
			output = ops.get(1);
		}else{
			output = this;
		}
	}
	
	public void setOutputLocation(Location la){
		this.la = la;
	}
	
	public Operand getOutput() {
		return output;
	}
	
	public int getInsType(){
		return insType;
	}
	
	public void setInsType(int insType){
		this.insType = insType;
	}
	
	public int getType(){
		return opIns;
	}
	
	public int getId(){
		return id;
	}

	public LinkedList<Integer> getOpsInsId() {
		return opsInsId;
	}

	public void setOpsInsId(LinkedList<Integer> opsInsId) {
		this.opsInsId = opsInsId;
	}
	
	
	public void setOpSeqId(){
		//override in ssa
	}
	
	public void setOpLoc(){
		//override in ssa
	}
	
	public void resetOpLoc(Location l, int i){
		ll.set(i, l);
	}
	
	public Location getLoc(int i) {
		return ll.get(i);
	}
	
	public Operand getOp(int i){
		return ops.get(i);
	}
	
	public ArrayList<Operand> getOps() {
		return ops;
	}

	public Location getOutputLoc(){
		return la;
	}
	
	public int getAssemblyType() {
		return assemblyType;
	}

	public void setAssemblyType(int assemblyType) {
		this.assemblyType = assemblyType;
	}

	public int getPC() {
		return PC;
	}

	public void setPC(int pC) {
		PC = pC;
	}

	public int getOpSize() {
		return ops.size();
	}
	
	public static void genSSA(){
		irType = irSSA;
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
	public static Instruction genLIR(int insType, Location a, Location b, Location c){
		return new StaticSingleAssignment(insType, a, b, c);
	}
	
	public static Instruction genLIR(int insType, Location a, Operand b, Location c){
		return new StaticSingleAssignment(insType, a, b, c);
	}
	
	public void setBlock(ControlFlowGraph.Block b){
		locate = b;
	}
	
	public static void setCurBlock(ControlFlowGraph.Block b){
		curLocate = b;
	}
	
	public ControlFlowGraph.Block getBlock(){
		return locate;
	}
	
	//set first parameter to be o, used for fix bra which used for if/while statement
	public void fix(Operand o){
		ops.set(1, o);
	}
	
	public variable getRetainVar() {
		return retainVar;
	}

	public void setRetainAddr(){
		retainVar = (variable) ops.get(0);
	}
	
	public Instruction deepCopy(HashMap<Operand, Operand> opMap, HashMap<Block, Block> bMap){
		Instruction copyIns = new Instruction(this);
		opMap.put(this, copyIns);
		copyIns.locate = bMap.get(copyIns.locate);
		return copyIns;
	}
	
	//override by ssa
	public String printVcg(){
		return null;
	}
	
	public String print(){
		String insprint = Integer.toString(id)+"\t";
		if(seqId>=0)
			insprint += Integer.toString(seqId)+"\t";
		insprint+=": " + codeToName(insType);
		insprint +=	"\t";
		for(Operand o : ops){
			if(o != null)
				if(o.getType() == opIns)
					insprint += "(" + Integer.toString(((Instruction)o).getId())+ ") " +"\t";
				else if(o.getType() == opFunc)
					insprint += o.print() + "->" + ((VariableSet.function)o).getBlock().print();
					else
						insprint += o.print()+"\t";
		}
		return insprint;
	}
	
	public String codeToName(int insCode){
		switch(insCode){
		case 	kill	:   return new String("kill");
		case	decl	:	return new String("decl ");
		case	neg		:	return new String("neg ");
		case	add		:	return new String("add ");
		case	sub		:	return new String("sub ");
		case	mul		:	return new String("mul ");
		case	div		:	return new String("div ");
		case	cmp		:	return new String("cmp ");
		
		case	adda	:	return new String("adda ");
		case	load	:	return new String("load ");
		case	store	:	return new String("stro ");
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
		case	write	:	return new String("wrt  ");
		case	writeNL	:	return new String("wrtN ");
		default			:	return new String("Error Ins");
		}
	}
	
	public String asmToName(int asmName){
		switch(asmName){
		case ADD:	return new String("ADD ");
		case SUB:	return new String("SUB ");
		case MUL:	return new String("MUL ");
		case DIV:	return new String("DIV ");
		case CMP:	return new String("CMP ");
		case ADDI:	return new String("ADDI ");
		case SUBI:	return new String("SUBI ");
		case MULI:	return new String("MULI ");
		case DIVI:	return new String("DIVI ");
		case CMPI:	return new String("CMPI ");
		
		case LDW:	return new String("LDW ");
		case LDX:	return new String("LDX ");
		case POP:	return new String("POP ");
		case STW:	return new String("STW ");
		case STX:	return new String("STX ");
		case PSH:	return new String("PSH ");
		case BEQ:	return new String("BEQ ");
		case BNE:	return new String("BNE ");
		case BLT:	return new String("BLT ");
		case BGE:	return new String("BGE ");
		case BLE:	return new String("BLE ");
		case BGT:	return new String("BGT ");
		case BSR:	return new String("BSR ");
		case JSR:	return new String("JSR ");
		case RET:	return new String("RET ");
		case RDD:	return new String("RDD ");
		case WRD:	return new String("WRD ");
		case WRH:	return new String("WRH ");
		case WRL:	return new String("WRL ");
		default:	return new String("Error asm");
		}	
	}
	
	protected static final int opScale 	= 0,
							   opArray  = 1,
							   opIns 	= 2,
							   opFunc	= 4;
	
	
	public static final int nonOutput = -1;
	
	protected static final int
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

	static final int ADD = 0,//ADDI 16
					 SUB = 1,//SUBI 17
					 MUL = 2,//MULI 18
					 DIV = 3,//DIVI 19
					 CMP = 5,//CMPI 21
					 
					 ADDI = 16,
					 SUBI = 17,
					 MULI = 18,
					 DIVI = 19,
					 CMPI = 21,
					 
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


	
}
