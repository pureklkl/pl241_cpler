package pl241_cpler.ir;

public class Constant implements Operand {

	public Constant(int value){
		val = value;
	}
	public int getType(){
		return opIns;
	}
	public int getValue(){
		return val;
	}
	
	public String print(){
		return "#" + Integer.toString(val) + "\t";
	}
	
	private int val;
	private static int opIns = 3;
	
	
}
