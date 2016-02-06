package pl241_cpler.ir;

import java.util.HashMap;
import java.util.ArrayList;

public class VariableSet {
	
	private variableScope curScope = null;
	
	public VariableSet(){
		curScope = new variableScope(null);
	}
	
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
				tmp = tmp.parentScope;
				res = tmp.varSet.get(identId);
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
		
		public void setName(String str){
			name = str;
		}
		
		public abstract variable addNew(String addName);
		
		private variableScope locate;
		
		private int id = variableCreated++;
		private String name;
		
	}
	
	public class scale extends variable{
		public int getType(){
			return varScale;
		}
		
		public scale addNew(String addName){
			scale tmp = new scale();
			tmp.setName(addName);
			return tmp;
		}
		
		protected static final int varScale = 0;
		private int value = 0;
	}
	
	public class array extends variable{

		public array(ArrayList<Integer> dims) {
			// TODO Auto-generated constructor stub
			this.dims = dims;
		}
		
		public int getType(){
			return varArray;
		}
		
		public ArrayList<Integer> getDims(){
			return dims;
		}
		
		public array addNew(String addName){
			array tmp = new array(dims);
			tmp.setName(addName);
			return tmp;
		}
		
		private ArrayList<Integer> dims;
		protected static final int varArray = 1;
	}
	
	public class function extends variable{
		
		
		public function addNew(String addName){
			return null;
		}
		public int getType(){
			return varFunction;
		}
		public variable addParam(variable param){
			paramList.add(param);
			return param;
		}
		
		public void setBlockId(int bId){
			blockId = bId;
		}
		public int getBlockId(){
			return blockId;
		}
		
		private int blockId;//indicate the start block of the function
		private ArrayList<variable> paramList;
		protected static final int varFunction = 1;
	}
	
	protected class variableScope {
		public variableScope(variableScope parentScope){
			if(parentScope != null){
				level = parentScope.level+1;
			}
			this.parentScope = parentScope;
		}
		
		public void addChildScope(variableScope childScope){
			this.childScope.add(childScope);
		}
		
		private HashMap<Integer, variable> varSet = new HashMap<Integer, variable>();
		
		private ArrayList<variableScope> childScope = null;
		private int id = scopeCreated++;
		private variableScope parentScope = null; 
		private int level = 0;
	}
	

	
	private static int variableCreated = 0;
	private static int scopeCreated = 0;
}
