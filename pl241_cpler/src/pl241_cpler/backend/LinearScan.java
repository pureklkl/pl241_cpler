package pl241_cpler.backend;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import pl241_cpler.backend.LiveTime.liveRange;
import pl241_cpler.backend.LiveTime.liveRange.liveInternal;
import pl241_cpler.ir.ControlFlowGraph;

public class LinearScan {
	//TODO add cost function
	PriorityQueue<liveReg> preSpill = new PriorityQueue<liveReg>(new costCmp());//sorted by cost, now is the liveRange.last liveInternal.to,same to activeSet except use different comparator
	PriorityQueue<liveReg> activeSet = new PriorityQueue<liveReg>(new liveEndCmp());//sorted by allocateTo.to (end of live internal)
	PriorityQueue<liveReg> sortedReg = new PriorityQueue<liveReg>(new liveUntilCmp());//sorted by live until
	PriorityQueue<liveInternal> sortedLR = new PriorityQueue<liveInternal>(new liveBeginCmp());//sorted by liveInternal.from
	LinkedList<liveRange> lr;
	
	public LinearScan(LinkedList<liveRange> lr){
		this.lr = lr;
	}
	
	//register is from 1 -> max-2, preserve 0, last two for spill
	private void initialREG(){
		for(int i = 0; i<MAXREG-2; i++){
			Location gR = new Location(REG, i+1);
			sortedReg.add(new liveReg(gR));
		}
	}
	private PriorityQueue<liveInternal> setQueueu(){
		for(liveRange iterLr : lr){
			for(liveInternal iterLi : iterLr.liList)
				sortedLR.add(iterLi);
		}
		return sortedLR;
	}
	private void spill(liveInternal r){
		liveReg lsr = preSpill.peek();
		if(lsr.allocateTo.belongTo.lastLive>r.belongTo.lastLive){
			lsr = preSpill.poll();
			activeSet.remove(lsr);//pay attention when implemented the comparator
			Location.memAllocate(lsr.allocateTo.belongTo);
			assignReg(lsr, r);
		}else{
			Location.memAllocate(r.belongTo); 
		}
	}
	private void expire(liveInternal r){
		for(int i=0; i<activeSet.size(); i++){
			if(activeSet.peek().allocateTo.to>=r.from)
				return;
			liveReg ex = activeSet.poll();
			preSpill.remove(ex);//pay attention when implemented the comparator, it should return 0 when find same internal
			if(ex.allocateTo.index == 0)
				ex.liveUntil = Integer.MAX_VALUE;
			else{
				LinkedList<liveInternal> li = ex.allocateTo.belongTo.liList;
				ex.liveUntil = li.get(li.size()-ex.allocateTo.index).from;
			}
			ex.allocateTo = null;
			sortedReg.add(ex);
		}
	}
	
	private void assignReg(liveReg choose, liveInternal r){
		//essentially the shortest hole, don't need check because of SSA
		choose.allocateTo = r;
		r.belongTo.lc = choose.reg;
		activeSet.add(choose);
		preSpill.add(choose);
	}
	
	public void allocate(LinkedList<liveRange> lr){
		initialREG();
		setQueueu();
		for(int i=0;i<sortedLR.size();i++){
			liveInternal r = sortedLR.poll();
			expire(r);
			if(r.belongTo.lc != null){
				if(r.belongTo.lc.type == REG){
					assignReg(sortedReg.poll(), r);
				}
				continue;
			}
			if(activeSet.size() == MAXREG){
				spill(r);
			}else{
				assignReg(sortedReg.poll(), r);
			}
		}
	}
	
	private void resolution(ControlFlowGraph cfg, LinkedList<liveRange> lr){
		
	}
	
	class liveReg{
		Location reg;
		liveInternal allocateTo = null;
		int liveUntil = Integer.MAX_VALUE;
		liveReg(Location reg){
			this.reg = reg;
		}
	};
	
	class liveBeginCmp implements Comparator<liveInternal>{

		@Override
		public int compare(liveInternal o1, liveInternal o2) {
			return o1.from<o2.from?-1:1;
		}
		
	}
	
	class liveUntilCmp implements Comparator<liveReg>{

		@Override
		public int compare(liveReg o1, liveReg o2) {
			return o1.liveUntil<o2.liveUntil?-1:1;
		}
		
	}
	
	class liveEndCmp implements Comparator<liveReg>{

		@Override
		public int compare(liveReg o1, liveReg o2) {
			if(o1 == o2)
				return 0;
			else 
				return o1.allocateTo.to<o2.allocateTo.to?-1:1;
		}
		
	}
	
	class costCmp implements Comparator<liveReg>{

		@Override
		public int compare(liveReg o1, liveReg o2) {
			if(o1==o2)
				return 0;
			else 
				return o1.allocateTo.belongTo.lastLive>o2.allocateTo.belongTo.lastLive?-1:1;
		}
		
	}
	
	static final int MAXREG = Location.MAXREG;
	public static final int REG = Location.REG,//register
							MEM = Location.MEM,//variable
							STK = Location.STK;//stack
	
}
