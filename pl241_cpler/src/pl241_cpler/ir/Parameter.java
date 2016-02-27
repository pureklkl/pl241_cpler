package pl241_cpler.ir;

import pl241_cpler.ir.VariableSet.function;

public class Parameter implements Operand {

	public Parameter(function func, int index){
		func_ = func;
		index_ = index;
	}
	
	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return opParam;
	}

	@Override
	public String print() {
		// TODO Auto-generated method stub
		return new String(func_.print()+": par "+Integer.toString(index_));
	}

	private function func_;
	private int index_;
	private static final int opParam	=	6;
}
