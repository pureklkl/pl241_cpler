package pl241_cpler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph.Block;;

public class dominatorTree {

	public class treeNode{
		public treeNode(Block b){
			b_ = b;
			nodeList.add(this);
		}
		public void addChild(Block c){
			if(childs == null)
				childs = new LinkedList<treeNode>();
			treeNode newC = new treeNode(c);
			newC.parent = this;
			childs.add(newC);
		}
		public LinkedList<treeNode> getChild(){
			return childs;
		}
		
		public Block getBlock(){
			return b_;
		}
		
		Block b_;
		LinkedList<treeNode> childs;
		treeNode parent;
	}
	
	public dominatorTree(LinkedList<Block> bList){
		bList_ = bList;
		treeGenerator();
	}
	
	private void copyToHashSet(HashSet<Block> hashBlockSet){
		for(Block b : bList_)
			hashBlockSet.add(b);
	}
	
	private void treeGenerator(){
		dominatorSet();
		generateDominatorTree();
	}
	
	private void dfs(Block b, HashSet<Block> dominators, Block blinked){
		visited.add(b);
		LinkedList<Block> successors = b.getSuccessor();
		if(successors!=null){
			for(Block s : successors){
				if(s!=blinked&&(!visited.contains(s))){
					dominators.remove(s);
					dfs(s, dominators, blinked);
				}
			}
		}
	}
	
	private void dominatorSet(){
		for(Block b : bList_){
			HashSet<Block> dominators = new HashSet<Block>();
			copyToHashSet(dominators);
			visited.clear();
			if(b != bList_.get(0)){
				dominators.remove(bList_.get(0));
				dfs(bList_.get(0), dominators, b);
			}
			doSet.put(b, dominators);
			rdoSet.put(b, new HashSet<Block>());
		}
		for(Block b : doSet.keySet()){
			for(Block b1 : doSet.get(b)){
				rdoSet.get(b1).add(b);
			}
		}
	}
	
	private void recursiveBuild(treeNode r, HashMap<Block, HashSet<Block>> copyDoSet){
		//System.out.println(r.b_.print());
		for(Iterator<Map.Entry<Block, HashSet<Block>>>  i = copyDoSet.entrySet().iterator();
				i.hasNext();){
			Map.Entry<Block, HashSet<Block>> e = i.next();
			if(e.getKey() != r.b_ && e.getKey() != root.b_){
				e.getValue().remove(r.b_);
				if(e.getValue().size()==1){
					r.addChild(e.getKey());
					i.remove();
				}
			}
		}
		if(r.getChild()!=null)
			for(treeNode c : r.getChild()){
				recursiveBuild(c, copyDoSet);
			}
	}
	
	private HashMap<Block, HashSet<Block>> deepCopyDoSet(HashMap<Block, HashSet<Block>> src){
		HashMap<Block, HashSet<Block>> res = new HashMap<Block, HashSet<Block>>();
		for(Map.Entry<Block, HashSet<Block>> e : src.entrySet()){
			HashSet<Block> s = new HashSet<Block>();
			for(Block b : e.getValue()){
				s.add(b);
			}
			res.put(e.getKey(), s);
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	private treeNode generateDominatorTree(){
		root = new treeNode(bList_.get(0));
		recursiveBuild(root, deepCopyDoSet(rdoSet));
		return null;
	}
	
	public void printTree(){
		for(treeNode n : nodeList){
			System.out.print(n.b_.print() + "have childs :");
			if(n.childs!=null){
				for(treeNode c : n.childs)
					System.out.print(c.b_.print());
			}
			System.out.println();
		}
	}
	
	public void printSet(){
		for(Block b : doSet.keySet()){
			System.out.println(b.print() + "dominates :");
			for(Block b1: doSet.get(b)){
				System.out.print(b1.print());
			}
			System.out.println();
			System.out.println();
		}
	}
	
	public void printRSet(){
		for(Block b : rdoSet.keySet()){
			System.out.println(b.print() + "is dominated by :");
			for(Block b1: rdoSet.get(b)){
				System.out.print(b1.print());
			}
			System.out.println();
			System.out.println();
		}
	}
	
	public treeNode getTreeRoot(){
		return root;
	}
	
	public HashMap<Block, HashSet<Block>> getRDSet(){
		return rdoSet;
	}
	
	public LinkedList<treeNode> getNodeList(){
		return nodeList;
	}
	
	private treeNode root;
	private LinkedList<treeNode> nodeList = new LinkedList<treeNode>();
	private HashMap<Block, HashSet<Block>> rdoSet =  new HashMap<Block, HashSet<Block>>();
	private HashMap<Block, HashSet<Block>> doSet = new HashMap<Block, HashSet<Block>>();
	private HashSet<Block> visited = new HashSet<Block>();
	private LinkedList<Block> bList_;
	
	public static void main(String[] args){
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		cfg.print();
		for(VariableSet.function f : cfg.getFuncSet().keySet()){
			System.out.println(f.print()+" : ");
			dominatorTree t = new dominatorTree(cfg.getFuncSet().get(f));
			t.printRSet();
			t.printTree();
		}
	}
}
