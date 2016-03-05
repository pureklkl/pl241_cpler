package pl241_cpler.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import pl241_cpler.ir.VariableSet.function;

import java.util.ArrayList;

public class VariableSet {
	
	private variableScope curScope = null;
	private variableScope globalScope = null;
	private LinkedList<variableScope> scopeList = new LinkedList<variableScope>();
	private HashMap<Integer, variable> idvarSet = new HashMap<Integer, variable>();
	
	public VariableSet(){
		curScope = new variableScope(null, 0);
		globalScope = curScope;
		scopeList.add(globalScope);
		preDef();
	}
	
	private void preDef(){
		addPredefFunc("OutputNum", 1, OutputNum);
		addPredefFunc("OutputNewLine", 0, OutputNewLine);
		addPredefFunc("OutputNewLine", 0, InputNum);
		addPredefFunc("-callerFunc-", 0, callerFunc);
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
	
	public VariableSet(VariableSet copyFrom){
		HashMap<variableScope, variableScope> mapScope = new HashMap<variableScope, variableScope>(); 
		for(variableScope vs : copyFrom.scopeList){
			variableScope copy = new variableScope(vs);
			mapScope.put(vs, copy);
			scopeList.add(copy);
		}
		globalScope = mapScope.get(copyFrom.globalScope);
		for(variableScope vs : scopeList){
			if(vs.childScope!=null)
				for(int i = vs.childScope.size(); i>=0; i++)
					vs.childScope.set(i, mapScope.get(vs.childScope.get(i)));
			if(vs.parentScope!=null)
				vs.parentScope = mapScope.get(vs);
		}
	}
	
	public variable copyVariable(variable copyFrom){
		switch(copyFrom.getType()){
		case opScale : return new scale(copyFrom);
		case opArray : return new array(copyFrom);
		case opFunc	 : return new function(copyFrom);
		default : return null;
		}
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
		idvarSet.put(var.id, var);
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
		variableScope tmp = new variableScope(this.currentScope(), curScope.level+1);
		this.currentScope().addChildScope(tmp);
		curScope = tmp;
		scopeList.add(curScope);
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
		
		private int id;
		protected String name;
		private int identId;
		
		private variableScope locate;
		private DefUseChain du = new DefUseChain();
		
		private int address = -1;//-1 means unallocated
		
		public variable(){
			id = variableCreated++;
		}
		
		//copy constructor
		//locate, du need fix when move to new CFG
		//address need re-allocate when in new code generation
		public variable(variable copyFrom){
			id = copyFrom.id;
			name = copyFrom.name;
			identId = copyFrom.identId;
			locate = copyFrom.locate;
			du = new DefUseChain(du);
			address = copyFrom.address;
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
		
		public int getId(){
			return id;
		}
		
		public abstract variable addNew(String addName, int id);
		
	}
	
	public class scale extends variable{
		public scale(){
			super();
		}
		
		public scale(variable copyFrom) {
			// TODO Auto-generated constructor stub
		}

		public int getType(){
			return opScale;
		}
		
		public scale addNew(String addName, int id){
			scale tmp = new scale();
			tmp.setName(addName, id);
			return tmp;
		}
		
		public String print(){
			return name;
		}
	}
	
	public class array extends variable{

		public array(ArrayList<Integer> dims) {
			// TODO Auto-generated constructor stub
			super();
			this.dims = dims;
		}
		
		public array(variable copyFrom) {
			// TODO Auto-generated constructor stub
		}

		public int getType(){
			return opArray;
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
		protected static final int opArray = 1;
	}
	
	public class function extends variable{
		
		private HashSet<variable> usedGlobalVar = new HashSet<variable>();
		private ControlFlowGraph.Block blockId;//indicate the start block of the function
		private ArrayList<variable> paramList = new ArrayList<variable>();
		private boolean isReturn_ = false;
		
		public function(variable copyFrom) {
			// TODO Auto-generated constructor stub
		}
		
		//need default constructor because we have added a copy constructor
		public function() {}
		
		public function addNew(String addName, int id){
			return null;
		}
		public int getType(){
			return opFunc;
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
	}
	
	public class variableScope {
		
		private HashMap<Integer, variable> varSet = new HashMap<Integer, variable>();
		private int id;
		private int level = 0;
		//need logical copy
		private variableScope parentScope = null;
		private ArrayList<variableScope> childScope = null;
		
		public variableScope(variableScope parentScope, int level){
			this.level = level;
			this.parentScope = parentScope;
			id = scopeCreated++;
		}
		
		public variableScope(variableScope copyFrom){
			level= copyFrom.level;
			parentScope = copyFrom.parentScope;
			id = copyFrom.id;
			for(Map.Entry<Integer, variable> e : copyFrom.varSet.entrySet()){
				varSet.put(e.getKey(), copyVariable(e.getValue()));
			}
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
	}
	
	private static int variableCreated = 0;
	private static int scopeCreated = 0;
	
	static final int opScale = 0,
					 opArray = 1,
					 opFunc  = 4;
	
	private static final int OutputNum		=	300,
							 OutputNewLine	=	301,
							 InputNum		=	302,//x=InputNum();//OutPutNum(x)
							 callerFunc		=	500;
}
