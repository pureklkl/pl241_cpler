package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.Stack;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.VariableSet.function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
public class ControlFlowGraph {
	protected class Block implements Operand{
		
		public Block(ControlFlowGraph cfg_){
			cfg = cfg_;
		}
		
		void addIns(Instruction ins){
			insList.add(ins);
		}
		
		void addInsToHead(Instruction ins){
			insList.add(0, ins);
		}
		
		@Override
		public int getType() {
			// TODO Auto-generated method stub
			
			return opBlock;
		}
		
		public String print(){
			return "Block - " + Integer.toString(id);
		}
		
		public int compareRoute(Block b){
			if(this == b){
				return sameRoute;
			}else{
				int thisRsize	= this.ifElseRoute.size(),
				    bRsize		= b.ifElseRoute.size();
				int cnum = thisRsize<bRsize?thisRsize:bRsize;
				for(int i = cnum - 1;i>=0;i--){
					if(this.ifElseRoute.get(i)!=b.ifElseRoute.get(i)){
						return otherRoute;
					}
				}
				if(thisRsize == bRsize)
					return sameRoute;//function call will split the block
				else if(thisRsize<bRsize&&((bRsize-thisRsize)<=1))
						return reverseDominatorRoute;
					else
						return dominatorRoute;
			}
		}
		
		private void setLoopLevel(){
			loopLevel = 0;
			for(Integer i : ifElseRoute){
				if(i.intValue() == whileRoute){
					loopLevel++;
				}
			}
		}
		
		public int getLoopLevel(){
			return loopLevel;
		}
		
		public VariableSet.function getFunc(){
			return func;
		}
		
		public Block funcHead(){
			return cfg.funcSet.get(func).get(0);
		}
		
		private ControlFlowGraph cfg;
		private VariableSet.function func;
		private Block loopEnd = null;
		private Block loopHead = null;
		private ArrayList<Block> up	= null;
		private ArrayList<Block> down = null;
		private ArrayList<Instruction> insList = new ArrayList<Instruction>();
		private int id = blockCreated++;
		private VariableSet curVarSet;
		private final static int opBlock = 5;
		
		private int loopLevel = 0;
		private Stack<Integer> ifElseRoute;
		
	}
		
	@SuppressWarnings("unchecked")
	public void addNewFuncBlock(VariableSet.function func){
		Block b = new Block(this);
		LinkedList<Block> blockList = new LinkedList<Block>();
		blockList.add(b);
		func.setBlock(b);
		funcSet.put(func, blockList);
		curBlock = b;
		curFunc = func;
		b.func = curFunc;
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
		Instruction.setCurBlock(b);
	}
/*	
	public void addDefaultBlock(VariableSet.function func){
		Block b = new Block();
		LinkedList<Block> blockList = new LinkedList<Block>();
		blockList.add(b);
		funcSet.put(func, blockList);
		curBlock = b;
		curFunc = func;
	}
*/	
	private void linkSeqBlock(Block upB, Block downB){
		if(upB.down == null){
			upB.down = new ArrayList<Block>();
		}
		if(downB.up == null){
			downB.up = new ArrayList<Block>();
		}
		upB.down.add(downB);
		downB.up.add(upB);
	}
	
	@SuppressWarnings("unchecked")
	public void addAndMoveToNextBlock(){
		Block b = new Block(this);
		funcSet.get(curFunc).add(b);
		linkSeqBlock(curBlock, b);
		curBlock = b;
		Instruction.setCurBlock(b);
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
		b.setLoopLevel();
		b.func = curFunc;
	}
	
	@SuppressWarnings("unchecked")
	public void addAndMoveToSingleBlock(){
		Block b = new Block(this);
		funcSet.get(curFunc).add(b);
		curBlock = b;
		Instruction.setCurBlock(b);
		b.ifElseRoute = (Stack<Integer>) curRoute.clone();
		b.setLoopLevel();
		b.func = curFunc;
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
		loopHead.ifElseRoute.pop();
		loopHead.setLoopLevel();
		Instruction.setCurBlock(curBlock);
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
	
	public void addInsToCurBlockHead(Instruction ins){
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
	
	public Stack<Integer> getCurRoute(){
		return curRoute;
	}
	
	public Block getCurBlock(){
		return curBlock;
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
			System.out.println(Integer.toString(i.getId()) + "	:	"+ i.print());
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
	
	HashSet<Block> visited = new HashSet<Block>();
	
	private Block curBlock;
	private VariableSet.function curFunc;
	private Stack<Integer> curRoute;
	
	Stack<Block> stackedBlock = new Stack<Block>();
	Stack<Instruction> stackedIns = new Stack<Instruction>();
	
	private HashMap<VariableSet.function, LinkedList<Block>> funcSet = new HashMap<VariableSet.function, LinkedList<Block>>();
	private HashMap<VariableSet.function, LinkedList<Block>> preDeffuncSet = new HashMap<VariableSet.function, LinkedList<Block>>();
	static private int blockCreated = 0;
	static int curLoopLevel;
	
	static final int bra = Parser.bra;
		
	
	static final int normalRoute 	= 0,
					 ifRoute		= 1,
					 elseRoute		= 2,
					 whileRoute		= 3;
	
	
	/**
	 * same route 		: same block
	 * 					b0	
	 * 				/		\
	 * 				b1		 \
	 * 			/		\	  \	
	 * 			b2		b3	  b5
	 * 			\		/	  /
	 * 				b4		 /
	 * 				\		/
	 * 					b6	
	 *  reverseDominatorRoute	: b1, b2, b3 is reverseDominator to b4 (b4 can get definitions in b1, b2, b3 and blink them to its successor block)
	 *  dominatorRoute		: b0 is dominator to b4	(b4 can get definition in b0 but cannot blink it)
	 *  otherRoute			: b5 is otherRoute to b1, b2, b3, b4, it cannot get definition in b0
	 */
	
	static final int sameRoute		= 0,
					 dominatorRoute  = 1,
					 reverseDominatorRoute  = 2,
					 otherRoute		= 3;
}
