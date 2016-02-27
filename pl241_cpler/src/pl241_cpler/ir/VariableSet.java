package pl241_cpler.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import pl241_cpler.ir.VariableSet.function;

import java.util.ArrayList;

public class VariableSet {
	
	private variableScope curScope = null;
	private variableScope globalScope = null;
	
	public VariableSet(){
		curScope = new variableScope(null);
		globalScope = curScope;
		addPredefFunc("OutputNum", 1, OutputNum);
		addPredefFunc("OutputNewLine", 0, OutputNewLine);
		addPredefFunc("OutputNewLine", 0, InputNum);
		addPredefFunc("&callerFunc&", 0, callerFunc);
	}
	
	private void addPredefFunc(String name, int varnum, int id){
		VariableSet.function func = new function();
		func.setName(name, id);
		add(id, func);
		addAndMoveToNewScope();
		for(int i = 0; i < varnum; i++){
			func.addParam(new scale()).setName("v"+Integer.toString(i), -1);
		}
		returnToParentScope();
		
	}
	
	public HashMap<Integer, variable> getGlobalVar(){
		return globalScope.varSet;
	}
	
	public variableScope getGlobalScope(){
		return globalScope;
	}
	
	//???? make sure the variable id
	public boolean add(int identId, variable var){
		if(curScope.varSet.containsKey(identId)){
			return false;
		}
		var.locate=curScope;
		curScope.varSet.put(identId, var);
		return true;
	}
	
	public variable retrieval(int identId){
		variable res = curScope.varSet.get(identId);
		if(res == null){
			variableScope tmp = curScope.parentScope;
			while(res == null && tmp != null){
				res = tmp.varSet.get(identId);
				tmp = tmp.parentScope;
			}
		}
		return res;
	}
	
	public void addAndMoveToNewScope(){
		variableScope tmp = new variableScope(this.currentScope());
		this.currentScope().addChildScope(tmp);
		curScope = tmp;
	}
	
	public void returnToParentScope(){
		curScope = curScope.parentScope;
	}
	
	variableScope currentScope(){
		return curScope;
	}
	
	variableScope parentScope(){
		return curScope.parentScope;
	}
	
	public abstract class variable implements Operand{
		
		public variable(){
		}
		
		public abstract int getType();
		
		public void setName(String str , int identId_){
			name = str;
			identId = identId_;
		}
		
		public int getScopeLevel(){
			return locate.level;
		}
		
		public DefUseChain getDU(){
			return du;
		}
		
		public int getIdentId(){
			return identId;
		}
		
		public abstract variable addNew(String addName, int id);
		
		private int identId;
		private DefUseChain du = new DefUseChain();
		private variableScope locate;
		private int id = variableCreated++;
		protected String name;
		
	}
	
	public class scale extends variable{
		public scale(){
			super();
		}
		
		public int getType(){
			return varScale;
		}
		
		public scale addNew(String addName, int id){
			scale tmp = new scale();
			tmp.setName(addName, id);
			return tmp;
		}
		
		public String print(){
			return name;
		}
		
		protected static final int varScale = 0;
		private int value = 0;
	}
	
	public class array extends variable{

		public array(ArrayList<Integer> dims) {
			// TODO Auto-generated constructor stub
			super();
			this.dims = dims;
		}
		
		public int getType(){
			return varArray;
		}
		
		public ArrayList<Integer> getDims(){
			return dims;
		}
		
		public array addNew(String addName, int id){
			array tmp = new array(dims);
			tmp.setName(addName, id);
			return tmp;
		}
		
		public Instruction getAddr(){
			return curAddr;
		}
		
		public void setAddr(Instruction itemAddr){
			curAddr = itemAddr;
		}
		
		public String print(){
			return name + "[]";
		}
		
		private Instruction curAddr;
		private ArrayList<Integer> dims;
		protected static final int varArray = 1;
	}
	
	public class function extends variable{
		
		
		public function addNew(String addName, int id){
			return null;
		}
		public int getType(){
			return varFunction;
		}
		public variable addParam(variable param){
			paramList.add(param);
			return param;
		}
		
		public void setBlock(ControlFlowGraph.Block bId){
			blockId = bId;
		}
		public ControlFlowGraph.Block getBlock(){
			return blockId;
		}
		
		public int paramNum(){
			return paramList.size();
		}
		
		public variable getParam(int i){
			return paramList.get(i);
		}
		
		public void setReturnState(boolean isReturn){
			isReturn_ = isReturn;
		}
		
		public boolean getReturnState(){
			return isReturn_;
		}
		
		public HashSet<variable> getUsedGV(){
			return usedGlobalVar;
		}
		
		public void addGV(scale gv){
			usedGlobalVar.add(gv);
		}
		
		public void addGV(HashSet<variable> gvl){
			usedGlobalVar.addAll(gvl);
		}
		
		public String print(){
			return name + "()";
		}
		
		private HashSet<variable> usedGlobalVar = new HashSet<variable>();
		private ControlFlowGraph.Block blockId;//indicate the start block of the function
		private ArrayList<variable> paramList = new ArrayList<variable>();
		private boolean isReturn_ = false;
		protected static final int varFunction = 4;
	}
	
	public class variableScope {
		public variableScope(variableScope parentScope){
			if(parentScope != null){
				level = parentScope.level+1;
			}
			this.parentScope = parentScope;
		}
		
		public void addChildScope(variableScope childScope){
			if(this.childScope == null){
				this.childScope = new ArrayList<variableScope>();
			}
			this.childScope.add(childScope);
		}
		
		public int getLevel(){
			return level;
		}
		
		public ArrayList<variableScope> getChildScope(){
			return childScope;
		}
		
		public HashMap<Integer, variable> getVarSet(){
			return varSet;
		}
		
		private HashMap<Integer, variable> varSet = new HashMap<Integer, variable>();
		
		private ArrayList<variableScope> childScope = null;
		private int id = scopeCreated++;
		private variableScope parentScope = null; 
		private int level = 0;
	}
	
	private static int 		OutputNum		=	300,
							OutputNewLine	=	301,
							InputNum		=	302,//x=InputNum();//OutPutNum(x)
							callerFunc		=	500;
	
	private static int variableCreated = 0;
	private static int scopeCreated = 0;
	
	
}
