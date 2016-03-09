package pl241_cpler.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import pl241_cpler.backend.Location;
import pl241_cpler.ir.VariableSet.function;
import pl241_cpler.ir.VariableSet.variable;

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
		func.setVscope(curScope);
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
	
	public variableScope getCurScope() {
		return curScope;
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
	
	public variable findById(int id) {
		return idvarSet.get(id);
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
		
		private int addrOffset;
		
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
			addrOffset = copyFrom.addrOffset;
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
		
		public int getAddrOffset() {
			return addrOffset;
		}

		public void setAddrOffset(int addrOffset) {
			this.addrOffset = addrOffset;
		}

		public abstract variable addNew(String addName, int id);
		
	}
	
	public class scale extends variable{
		private boolean par = false;//>=0 means the par'th parameter
		
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
			idvarSet.put(id, tmp);
			return tmp;
		}
		
		public String print(){
			return name;
		}
	}
	
	public class array extends variable{

		private Instruction curAddr;
		private ArrayList<Integer> dims;
		private int size = 1;
		
		public array(ArrayList<Integer> dims) {
			// TODO Auto-generated constructor stub
			super();
			this.dims = dims;
			for(Integer d:dims){
				size*=d;
			}
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
		
		public int getArraySize(){
			return size;
		}
		
		public array addNew(String addName, int id){
			array tmp = new array(dims);
			tmp.setName(addName, id);
			idvarSet.put(id, tmp);
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
		
		protected static final int opArray = 1;
	}
	
	public class function extends variable{
		
		private HashSet<variable> usedGlobalVar = new HashSet<variable>();
		private ControlFlowGraph.Block blockId;//indicate the start block of the function
		private ArrayList<scale> paramList = new ArrayList<scale>();
		private boolean isReturn_ = false;
		private variableScope vscope = null;
		
		//for code generation
		//Integer corresponding to index in usedSTKREG
		private HashMap<Location, Integer> locToAddr = new HashMap<Location, Integer>();
		//used REG is only for save the caller's register
		private Stack<Location> usedSTKREG = new Stack<Location>();
		int variableSize=0;//used as base address for used stacked temporary and stored caller register
		int stackEnd = 0;//used for main function used for initialize the sp register,initial address for caller register
		
		public function(variable copyFrom) {
			// TODO Auto-generated constructor stub
		}
		
		//need default constructor because we need copy constructor
		public function() {}
		
		public function addNew(String addName, int id){
			return null;
		}
		public int getType(){
			return opFunc;
		}
		public variable addParam(scale param){
			param.par = true;
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
		
		public void setVscope(variableScope vscope){
			this.vscope = vscope;
			vscope.func = this;
		}
		
		public variableScope getVscope() {
			return vscope;
		}

		public int getStackEnd() {
			return stackEnd;
		}
		
		public Stack<Location> getUsedSTKREG() {
			return usedSTKREG;
		}

		public int getVariableSize() {
			return variableSize;
		}
		
		public int assignVar(int varOffset, int parOffset) {
			HashMap<Integer, variable> varSet = vscope.varSet;
			int base = 0;
			for(variable v : varSet.values()){
				if(v.getType() == opScale && !((scale)v).par){
					v.addrOffset = (base+varOffset)*4;
					base++;
				}else if(v.getType() == opArray){
					v.addrOffset = (base+varOffset)*4;
					base+=((VariableSet.array)v).getArraySize();
				}
			}
			variableSize = base;
			base = 0;
			for(int i = paramList.size()-1; i>=0; i--){
				paramList.get(i).setAddrOffset((base+parOffset)*4);
				base--;
			}
			return variableSize+varOffset;
		}
		
		public void assignSTKREG(HashSet<Location> usedSTKREG, int offset){
			Stack<Location> usedREG = new Stack<Location>();
			for(Location l : usedSTKREG){
				if(l.getLocType() == STK){
					locToAddr.put(l, (this.usedSTKREG.size()+offset)*4);
					this.usedSTKREG.push(l);
				}else if(l.getLocType() == REG){
					usedREG.push(l);
				}
			}
			stackEnd = (this.usedSTKREG.size()+offset-1)*4;//offset is start from 1
			for(Location l : usedREG){
				locToAddr.put(l, (this.usedSTKREG.size()+offset)*4);
				this.usedSTKREG.push(l);
			}
		}
		
		public int findLoc(Location loc) {
			return locToAddr.get(loc);
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
		
		private function func = null;
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
	
	public void printLayout(){
		System.out.println();
		for(variableScope vs:scopeList){
			System.out.print("Level : "+Integer.toString(vs.level)+"\t");
			if(vs.func!=null)
				System.out.println(vs.func.print());
			else
				System.out.println();
			for(variable v:vs.varSet.values()){
				System.out.println(v.print()+" at "+Integer.toString(v.addrOffset));
			}
			if(vs.func!=null)
				for(Location l:vs.func.usedSTKREG){
					System.out.println(l.print()+" at "+Integer.toString(vs.func.findLoc(l)));
				}
			System.out.println();
		}
		function mainFunc = (function) globalScope.getVarSet().get(mainToken);
		System.out.println(mainFunc.print());
		for(Location l:mainFunc.usedSTKREG){
			System.out.println(l.print()+" at "+Integer.toString(mainFunc.findLoc(l)));
		}
		System.out.println();
	}
	
	private static int variableCreated = 0;
	private static int scopeCreated = 0;
	
	static final int opScale = 0,
					 opArray = 1,
					 opFunc  = 4;
	static final int mainToken	=	200;
	private static final int OutputNum		=	300,
							 OutputNewLine	=	301,
							 InputNum		=	302,//x=InputNum();//OutPutNum(x)
							 callerFunc		=	500;
	public static final int REG = 0,//register
							MEM = 1,//variable
							STK = 2;//stack


}
