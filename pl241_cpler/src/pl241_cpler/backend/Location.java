package pl241_cpler.backend;

import pl241_cpler.backend.LiveTime.liveRange;

public class Location {
	int type;
	int id;//variable id if assigned to a variable in memory
	
	public Location(int type, int id){
		this.type = type;
		this.id = id;
	}
	//TODO implemented it
	public static void memAllocate(liveRange lr){
		
	}
	
	public static final int MAXREG = 8;
	public static final int REG = 0,//register
							MEM = 1,//variable
							STK = 2;//stack
}
