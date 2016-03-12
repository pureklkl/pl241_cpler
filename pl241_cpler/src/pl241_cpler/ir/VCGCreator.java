package pl241_cpler.ir;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import pl241_cpler.backend.LiveTime;
import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.dominatorTree.treeNode;

public class VCGCreator {
	
	String sourceName;
	ControlFlowGraph cfg;
	PrintWriter writer;
	
	public VCGCreator(String sourceName, ControlFlowGraph cfg){
		String[] sp = sourceName.split("[\\/.]");
		this.sourceName = sp[sp.length-2];
		this.cfg = cfg;
	}
	
	public VCGCreator(String sourceName, ControlFlowGraph cfg, String addedName){
		String[] sp = sourceName.split("[\\/.]");
		this.sourceName = sp[sp.length-2]+addedName;
		this.cfg = cfg;
	}
	
	private void printEdge(int from, int to){
        writer.println("edge: { sourcename: \"" + from + "\"");
        writer.println("targetname: \"" + to + "\"");
        writer.println("color: blue");
        writer.println("}");
	}
	
	private void printBlock(Block b){
        writer.println("node: {");
        writer.println("title: \"" + b.getId() + "\"");
        writer.println("label: \"" + b.getId() + "[");
        for(Instruction ins:b.getInsList()){
        	 writer.println(ins.printVcg());
        }
        writer.println("]\"");
        writer.println("}");
	}
	
	private void printCFG(){
        writer.println("graph: { title: \"Control Flow Graph\"");
        writer.println("layoutalgorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("smanhattan_edges: yes");
		for(LinkedList<Block> bl: cfg.getFuncSet().values()){
			for(Block b:bl){
				printBlock(b);
		        if(b.getSuccessor()!=null)
			        for(Block sb:b.getSuccessor()){
			        	printEdge(b.getId(), sb.getId());
			        }
			}
		}
		writer.println("}");
	}
	
	private void printNode(treeNode t){
		printBlock(t.getBlock());
	}
	
	private void printDT(){
        writer.println("graph: { title: \"Control Flow Graph\"");
        writer.println("layoutalgorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("smanhattan_edges: yes");
		for(dominatorTree tl: cfg.getFuncDTree().values()){
			for(treeNode t:tl.getNodeList()){
				printNode(t);
				if(t.getChild()!=null)
					for(treeNode c:t.getChild()){
						printEdge(t.getBlock().getId(), c.getBlock().getId());
					}
			}
		}
		
		writer.println("}");
	}
	
	public void run() throws IOException{
        writer = new PrintWriter(new FileWriter("vcg/" + sourceName + ".vcg"));
        printCFG();
        writer.close();
        writer = new PrintWriter(new FileWriter("vcg/" + sourceName + "DT.vcg"));
        printDT();
        writer.close();
	}
	
	public static void main(String args[]) throws IOException{
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		LiveTime lt = new LiveTime(cfg);
		cfg.print();
		lt.analysisLiveTime();
		VCGCreator vcg = new VCGCreator(args[0], cfg);
		vcg.run();
	}
}
