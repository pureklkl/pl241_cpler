package pl241_cpler.core;

import java.io.IOException;
import java.util.LinkedList;

import pl241_cpler.backend.DLXCodeGeneration;
import pl241_cpler.backend.LinearScan;
import pl241_cpler.backend.LiveTime;
import pl241_cpler.backend.Location;
import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.StaticSingleAssignment;
import pl241_cpler.ir.VariableSet;
import pl241_cpler.simulator.DLX;
import pl241_cpler.simulator.DLXCode;
import pl241_cpler.simulator.DLXdebuger;

public class CLI {
	static void run(String[] args) throws IOException{
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		VariableSet varSet = p.getVarSet();
		LiveTime lt = new LiveTime(cfg);
		lt.analysisLiveTime();
		int maxReg = 8;
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
		
		DLXCode exe =new DLXCode(codeGen);
		exe.asmToBinary();
		
		int[] program = exe.getProgram();
		
		//DLXdebuger.load(program);
		//DLXdebuger.debugExecute();
		DLX.load(program);
		DLX.execute();
	}
	//TODO add compiler options
	public static void main(String[] args) throws IOException{
		run(args);
	}
}
