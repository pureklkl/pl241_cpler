package pl241_cpler.backend;

import pl241_cpler.backend.LiveTime.liveRange;
import pl241_cpler.ir.VariableSet.variable;

public class Location {
	int type;
	int id;//variable id if assigned to a variable in memory
	static int stackSlot = 0;
	
	public Location(int type, int id){
		this.type = type;
		this.id = id;
	}
	
	public int getLocType(){
		return type;
	}
	
	public String print(){
		String lcString = "";
		switch(type){
		case REG:	lcString += "REG_";break;
		case MEM:	lcString += "MEM_";break;
		case STK:	lcString += "STK_";break;
		case CON:	lcString += "Constant_";break;
		}
		lcString += Integer.toString(id);
		return lcString;
	}
	
	//TODO stack reuse
	public static void memAllocate(liveRange lr){
		if(lr.ins.getOutput().getType() == opScale)
			lr.lc =new Location(MEM, ((variable)lr.ins.getOutput()).getId());
		else
			lr.lc = new Location(STK, stackSlot++);
	}
	
	public int getId() {
		return id;
	}
	
	public static final int opScale = 0,
			   				opIns   = 2;
	public static final int MAXREG = 8;
	public static final int REG = 0,//register
							MEM = 1,//variable
							STK = 2,//stack
							CON = 3;//Constant
}
