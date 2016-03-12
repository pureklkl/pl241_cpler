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
import pl241_cpler.ir.VCGCreator;
import pl241_cpler.ir.VariableSet;
import pl241_cpler.optimization.CSE;
import pl241_cpler.optimization.CopyPropagation;
import pl241_cpler.simulator.DLX;
import pl241_cpler.simulator.DLXCode;
import pl241_cpler.simulator.DLXdebuger;

public class CLI {
	
	static boolean cp = false, cse = false, simulator = false;
	
	static void parseOption(String[] args){
		for(int i = 1; i<args.length;i++){
			if(args[i].compareToIgnoreCase("cp")==0)
				cp = true;
			if(args[i].compareToIgnoreCase("cse")==0)
				cse = true;
			if(args[i].compareToIgnoreCase("sim")==0)
				simulator = true;
		}
	}
	
	static void run(String[] args) throws IOException{
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		VariableSet varSet = p.getVarSet();
		VCGCreator normVcg = new VCGCreator(args[0], cfg);
		normVcg.run();
		if(cp){
			CopyPropagation g = new CopyPropagation(p);
			g.runCP();
			p.getCFG().print();
			VCGCreator cpVcg = new VCGCreator(args[0], cfg, "CP");
			cpVcg.run();
		}
		if(cse){
			CSE cse = new CSE(p.getCFG());
			cse.runCSE();
			p.getCFG().print();
			VCGCreator cseVcg = new VCGCreator(args[0], cfg, "CSE");
			cseVcg.run();
		}
		LiveTime lt = new LiveTime(cfg);
		lt.analysisLiveTime();
		int maxReg = 8;
		LinearScan allocator = new LinearScan(lt.getLr(), maxReg);
		allocator.allocate();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showREG);
		allocator.regAllocate(cfg);
		//cfg.print();
		VCGCreator regVCG = new VCGCreator(args[0], cfg, true);
		regVCG.run();
		if(simulator){
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
	}
	//TODO add compiler options
	public static void main(String[] args) throws IOException{
		parseOption(args);
		run(args);
	}
}
