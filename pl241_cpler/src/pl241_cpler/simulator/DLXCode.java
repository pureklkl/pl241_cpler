package pl241_cpler.simulator;

import java.util.ArrayList;
import java.util.LinkedList;

import pl241_cpler.backend.DLXCodeGeneration;
import pl241_cpler.backend.Location;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.Instruction;

public class DLXCode {
	int[] binaryCode;
	int maxPC;
	
	private ControlFlowGraph cfg;
	
	public DLXCode(DLXCodeGeneration asm){
		maxPC = asm.getMaxPC();
		cfg = asm.getCfg();
		binaryCode = new int[maxPC];
	}
	public void asmToBinary(){
		ArrayList<Integer> args = new ArrayList<Integer>();
		for(LinkedList<Block> bl : cfg.getFuncSet().values()){
			for(Block b : bl)
				for(Instruction ins : b.getInsList()){
					args.clear();
					Location la = ins.getOutputLoc(), lb = ins.getLoc(0), lc = ins.getLoc(1);
					if(la!=null)
						args.add(la.getId());
					if(lb!=null)
						args.add(lb.getId());
					if(lc!=null)
						args.add(lc.getId());
					switch(args.size()){
					case 0:binaryCode[ins.getPC()]=DLX.assemble(ins.getAssemblyType());break;
					case 1:binaryCode[ins.getPC()]=DLX.assemble(ins.getAssemblyType(), args.get(0));break;
					case 2:binaryCode[ins.getPC()]=DLX.assemble(ins.getAssemblyType(), args.get(0), args.get(1));break;
					case 3:binaryCode[ins.getPC()]=DLX.assemble(ins.getAssemblyType(), args.get(0), args.get(1), args.get(2));break;
					}
				}
		}
	}
	public int[] getProgram(){
		return binaryCode;
	}
}
