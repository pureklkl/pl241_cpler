package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.HashMap;
public class ControlFlowGraph {
	protected class Block{
		
		void addIns(Instruction ins){
			
		}
		
		ArrayList<Block> up;
		ArrayList<Block> down;
		private ArrayList<Instruction> insList;
		private int id = blockCreated++;
		private VariableSet curVarSet;
	}
	
	public void addNewFuncBlock(VariableSet.function func){
		Block b = new Block();
		funcSet.put(func, b);
	}
	
	private HashMap<VariableSet.function, Block> funcSet;
	static private int blockCreated = 0;
	static private int insCreated = 0;
}
