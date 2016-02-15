package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.Stack;

import pl241_cpler.frontend.Parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
public class ControlFlowGraph {
	protected class Block implements Operand{
		
		void addIns(Instruction ins){
			insList.add(ins);
		}
		
		@Override
		public int getType() {
			// TODO Auto-generated method stub
			
			return opBlock;
		}
		
		public String print(){
			return "Block - " + Integer.toString(id) + " ";
		}
		
		Block loopEnd = null;
		Block loopHead = null;
		ArrayList<Block> up;
		ArrayList<Block> down;
		private ArrayList<Instruction> insList;
		private int id = blockCreated++;
		private VariableSet curVarSet;
		private final static int opBlock = 5;

	}
	
	public void addNewFuncBlock(VariableSet.function func){
		Block b = new Block();
		LinkedList<Block> blockList = new LinkedList<Block>();
		blockList.add(b);
		funcSet.put(func, blockList);
		curBlock = b;
		curFunc = func;
	}
	
	private void linkSeqBlock(Block upB, Block downB){
		upB.down.add(downB);
		downB.up.add(upB);
	}
	
	public void addAndMoveToNextBlock(){
		Block b = new Block();
		funcSet.get(curFunc).add(b);
		linkSeqBlock(curBlock, b);
		curBlock = b;
	}
	
	public void addAndMoveToSingleBlock(){
		Block b = new Block();
		funcSet.get(curFunc).add(b);
		curBlock = b;
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
		curBlock.loopHead = loopHead;
		curBlock.addIns(Instruction.genIns(bra, null, loopHead));
		curBlock = loopHead;
	}
	
	//the jump target block have not yet created, so push current ins
	public void addAndPushIns(Instruction ins){
		curBlock.addIns(ins);
		stackedIns.push(ins);
	}
	//the jump ins to current block have not created yet, so push current block, used for loop
	public void pushCurBlock(){
		stackedBlock.push(curBlock);
	}
	
	public void addInsToCurBlock(Instruction ins){
		curBlock.addIns(ins);
		ins.setBlock(curBlock);
	}
	
	public VariableSet.function curFunc(){
		return curFunc;
	}
	
	private void printBlock(Block b){
		System.out.println(b.print()+" [ ");
		for(Instruction i : b.insList){
			System.out.println(i.print());
		}
		System.out.print(" ] ");
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
	
	HashSet<Block> visited = new HashSet<Block>();
	
	private Block curBlock;
	private VariableSet.function curFunc;
	
	Stack<Block> stackedBlock = new Stack<Block>();
	Stack<Instruction> stackedIns = new Stack<Instruction>();
	
	private HashMap<VariableSet.function, LinkedList<Block>> funcSet;
	static private int blockCreated = 0;
	static private int insCreated = 0;
	static final int bra = Parser.bra;
}
