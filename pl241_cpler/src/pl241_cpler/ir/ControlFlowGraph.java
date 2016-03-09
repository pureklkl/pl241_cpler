package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.Stack;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.VariableSet.function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
public class ControlFlowGraph {
	
	HashSet<Block> visited = new HashSet<Block>();
	
	private Block curBlock;
	private VariableSet.function curFunc;
	private Stack<Integer> curRoute;
	
	Stack<Block> stackedBlock = new Stack<Block>();
	Stack<Instruction> stackedIns = new Stack<Instruction>();
	
	private HashMap<VariableSet.function, dominatorTree> funcSetT = new HashMap<VariableSet.function, dominatorTree>();
	private HashMap<VariableSet.function, LinkedList<Block>> funcSet = new HashMap<VariableSet.function, LinkedList<Block>>();
	private HashMap<VariableSet.function, LinkedList<Block>> preDeffuncSet = new HashMap<VariableSet.function, LinkedList<Block>>();
	static private int blockCreated = 0;
			
	@SuppressWarnings("unchecked")
	public void addNewFuncBlock(VariableSet.function func){
		Block b = new Block();
		LinkedList<Block> blockList = new LinkedList<Block>();
		blockList.add(b);
		func.setBlock(b);
		funcSet.put(func, blockList);
		curBlock = b;
		curFunc = func;
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
		Instruction.setCurBlock(b);
	}

	private void linkSeqBlock(Block upB, Block downB){
		if(upB.down == null){
			upB.down = new LinkedList<Block>();
		}
		if(downB.up == null){
			downB.up = new LinkedList<Block>();
		}
		upB.down.add(downB);
		downB.up.add(upB);
	}
	
	@SuppressWarnings("unchecked")
	public void addAndMoveToNextBlock(){
		Block b = new Block();
		funcSet.get(curFunc).add(b);
		linkSeqBlock(curBlock, b);
		curBlock = b;
		Instruction.setCurBlock(b);
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
	}
	
	@SuppressWarnings("unchecked")
	public void addAndMoveToSingleBlock(){
		Block b = new Block();
		funcSet.get(curFunc).add(b);
		curBlock = b;
		Instruction.setCurBlock(b);
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
	}
	//link top of stacked ins to current block
	public void fix(){
		Instruction sI = stackedIns.pop();
		sI.fix(curBlock);
		linkSeqBlock(sI.getBlock(), curBlock);
	}
	//link second top of stacked ins to current block - used for else
	public void fix2(){
		Instruction tmp = stackedIns.pop();
		fix();
		stackedIns.push(tmp);
	}
	//add a bra to stacked ins' block to while block - used for loop
	public void loopBack(){
		Block loopHead = stackedBlock.pop();
		loopHead.loopEnd = curBlock;
		loopHead.isLH = true;
		curBlock.loopHead = loopHead;
		linkSeqBlock(curBlock, loopHead);
		curBlock.addIns(Instruction.genIns(bra, null, loopHead));
		curBlock = loopHead;
	}
	
	//the jump target block have not yet created, so push current ins
	public void addAndPushIns(Instruction ins){
		curBlock.addIns(ins);
		stackedIns.push(ins);
	}
	
	public void retCheck() {
		if( curBlock.insList.isEmpty())
			addInsToCurBlock(Instruction.genIns(bra, null, null));
		else{
			Instruction lastIns = curBlock.insList.getLast();
			if(!(lastIns.insType == bra && lastIns.ops.get(0)==null && lastIns.ops.get(1)==null))
				addInsToCurBlock(Instruction.genIns(bra, null, null));
		}
	}
	
	//the jump ins to current block have not created yet, so push current block, used for loop
	public void pushCurBlock(){
		stackedBlock.push(curBlock);
	}
	
	public void addInsToCurBlock(Instruction ins){
		curBlock.addIns(ins);
		ins.setBlock(curBlock);
	}
	
	public void addInsToCurBlockHead(Instruction ins) {
		curBlock.addInsToHead(ins);
		ins.setBlock(curBlock);
	}
	
	public VariableSet.function curFunc(){
		return curFunc;
	}
	
	public void resetCurRoute(){
		curRoute = new Stack<Integer>();
		curRoute.push(normalRoute);
	}
	
	public void pushCurRoute(int newRoute){
		curRoute.push(newRoute);
	}
	
	public void changeCurRoute(int newRoute){
		 curRoute.pop();
		 curRoute.push(newRoute);
	}
	
	public void popCurRoute(){
		curRoute.pop();
	}
	
	public HashMap<VariableSet.function, LinkedList<Block>> getFuncSet(){
		return funcSet;
	}
	
	public void putFuncDTree(VariableSet.function func, dominatorTree t){
		funcSetT.put(func, t);
	}
	
	public HashMap<VariableSet.function, dominatorTree> getFuncDTree(){
		return funcSetT;
	}
	
	private void printBlock(Block b){
		System.out.print("Route :");
		for(Integer i : b.ifElseRoute){
			switch(i.intValue()){
			case normalRoute 	: System.out.print("normal ");break;
			case ifRoute		: System.out.print("if ");break;
			case elseRoute		: System.out.print("else ");break;
			case whileRoute		: System.out.print("while ");break;
			}
		}
		System.out.println();
		System.out.println(b.print()+" [ ");
		for(Instruction i : b.insList){
			System.out.println(i.print());
		}
		System.out.println(" ] ");
	}
	
	private void printFunc(LinkedList<Block> blockList){
		for(Block b : blockList){
			printBlock(b);
		}
	}
	
	public void print(){
		for(VariableSet.function func : funcSet.keySet()){
			System.out.println(func.print() + "start-> ");
			printFunc(funcSet.get(func));
		}
	}
	
	public class Block implements Operand{
		
		private Block loopEnd = null;
		private Block loopHead = null;
		private boolean isLH = false;//is loop head
		private LinkedList<Block> up	= null;
		private LinkedList<Block> down = null;
		private LinkedList<Instruction> insList = new LinkedList<Instruction>();
		private int id;
		private VariableSet curVarSet;
		private final static int opBlock = 5;
		private Stack<Integer> ifElseRoute;
		
		private HashSet<Integer> livedIn;//operand live from end to this block(after the definition block)
		private HashSet<Integer> liveOut;//operand live from start to this block(before the last usage block)
		
		
		void addIns(Instruction ins){
			insList.add(ins);
		}
		
		void addInsToHead(Instruction ins){
			insList.add(0, ins);
		}
		
		public Block(){
			id = blockCreated++;
		}
		
		@Override
		public int getType() {
			return opBlock;
		}
		
		public int getFirstInsSeqId(){
			return insList.getFirst().seqId;
		}
		
		public int getLastInsSeqId(){
			return insList.getLast().seqId;
		}
		
		public boolean isLoopHead(){
			return isLH;
		}
		
		public Block getLoopEnd(){
			return loopEnd;
		}
		
		public LinkedList<Block> getSuccessor(){
			return down;
		}
		
		public LinkedList<Block> getPredecessor(){
			return up;
		}
		
		public LinkedList<Instruction> getInsList(){
			return insList;
		}
		
		public HashSet<Integer> getLivedIn() {
			return livedIn;
		}

		public void setLivedIn(HashSet<Integer> livedIn) {
			this.livedIn = livedIn;
		}
		
		public int getId(){
			return id;
		}
		
		public String print(){
			return "[" + Integer.toString(id) + "]";
		}
	}
	
	static final int bra = Parser.bra;
	
	static final int normalRoute 	= 0,
					 ifRoute		= 1,
					 elseRoute		= 2,
					 whileRoute		= 3;
	
}
