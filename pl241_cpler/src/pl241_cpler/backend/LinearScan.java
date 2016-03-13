package pl241_cpler.backend;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;

import pl241_cpler.backend.LiveTime.liveRange;
import pl241_cpler.backend.LiveTime.liveRange.liveInternal;
import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.Operand;
import pl241_cpler.ir.StaticSingleAssignment;

public class LinearScan {
	//TODO add cost function
	int MAXREG;
	PriorityQueue<liveReg> preSpill = new PriorityQueue<liveReg>(new costCmp());//sorted by cost, now is the liveRange.last liveInternal.to,same to activeSet except use different comparator
	PriorityQueue<liveReg> activeSet = new PriorityQueue<liveReg>(new liveEndCmp());//sorted by allocateTo.to (end of live internal)
	PriorityQueue<liveReg> sortedReg = new PriorityQueue<liveReg>(new liveUntilCmp());//sorted by live until
	PriorityQueue<liveInternal> sortedLR = new PriorityQueue<liveInternal>(new liveBeginCmp());//sorted by liveInternal.from
	LinkedList<liveRange> lr;
	Location p1, p2;//preserved register for spill, move resolution etc.. 
	boolean debug = true;
	
	//auto preserve 2 spill register
	public LinearScan(LinkedList<liveRange> lr, int MAXREG){
		this.lr = lr;
		this.MAXREG = MAXREG;
		p1 = new Location(REG, MAXREG-1);
		p2 = new Location(REG, MAXREG);
	}
	
	public LinkedList<Location> getSp1Sp2(){
		LinkedList<Location> l1 = new LinkedList<Location>();
		l1.add(p1);l1.add(p2);
		return l1;
	}
	
	//register is from 1 -> max-2, preserve last two for spill
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
			lsr.liveUntil.remove(new liveHole(0, lsr.allocateTo));
			Location.memAllocate(lsr.allocateTo.belongTo);
			assignReg(lsr, r);
		}else{
			Location.memAllocate(r.belongTo); 
		}
	}
	
	private void calLiveUntil(liveReg ex, liveInternal r){
		if(r.index!=0){
			LinkedList<liveInternal> liList = r.belongTo.liList;
			ex.liveUntil.push(new liveHole(liList.get(liList.size()-r.index).from, r));//push next start
		}
	}
	
	private void expire(liveInternal r){
		LinkedList<liveReg> recover = new LinkedList<liveReg>();
		while(!activeSet.isEmpty()){
			if(activeSet.peek().allocateTo.to>=r.from)
				break;
			liveReg ex = activeSet.poll();
			//if this r is using this register -> recovery from live time hole
			if(r.belongTo.lc == ex.reg){
				ex.liveUntil.remove(new liveHole(0, r));
				ex.allocateTo = r;
				calLiveUntil(ex, r);
				recover.add(ex);
				continue;
			}
			//the hole need to be recovery by the internal created, leave it in active set if other internal find it first
			if(ex.liveUntil.peek().nextLive>r.from){
				ex.allocateTo = null;
				preSpill.remove(ex);//pay attention when implemented the comparator, it should return 0 when find same internal
				sortedReg.add(ex);
			}else{
				recover.add(ex);
			}
		}
		activeSet.addAll(recover);
	}
	
	private void assignReg(liveReg choose, liveInternal r){
		//essentially the shortest hole, don't need check because of SSA
		choose.allocateTo = r;
		r.belongTo.lc = choose.reg;
		calLiveUntil(choose, r);
		activeSet.add(choose);
		preSpill.add(choose);
	}
	
	//DR to enable debug info for register allocation
	//Allocate register according to linear scan (live time hole) 
	public void allocate(){
		initialREG();
		setQueueu();
		while(sortedLR.size()>0){
			liveInternal r = sortedLR.poll();
			expire(r);
			if(r.belongTo.lc == null){//allocate only when meet new live value
				if(activeSet.size() == MAXREG-2){
					spill(r);
				}else{
					assignReg(sortedReg.poll(), r);
				}
			}
			if(debug){
				showDebug(r);
			}
		}
	}
	
	private void showDebug(liveInternal r){
		System.out.println();
		System.out.print("process :");r.print();System.out.println("from :" + r.belongTo.ins.print());
		System.out.println("Currently active:");
		for(liveReg ar: activeSet){
			System.out.print(ar.reg.print()+" allocate to ");ar.allocateTo.print();System.out.print("from :" + ar.allocateTo.belongTo.ins.print());
			System.out.println();
		}
		System.out.println();
	}
	
	public void regAllocate(ControlFlowGraph cfg){
		for(liveRange iterLr : lr)
			iterLr.ins.setOutputLocation(iterLr.lc);

		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			for(Block b : lb){
				for(Instruction ins : b.getInsList()){
					ins.setOpLoc();
				}
			}
		}
		resolution(cfg);
	}
	
	private Instruction genMoveCopy(phiMap cpy, Location to){
		if(cpy.lfrom!=null)
			return Instruction.genLIR(move, to, cpy.lfrom, to);
		else
			return Instruction.genLIR(move, to, cpy.constfrom, to);
	}
	
	//p2 is used to unlock the circular move
	private LinkedList<Instruction> orderMove(LinkedList<phiMap> mapList){
		LinkedList<Instruction> om = new LinkedList<Instruction>();
		while(!mapList.isEmpty()){
			boolean allLocked = true;
			for(int i = mapList.size()-1; i>=0 ; i--){
				phiMap cpy = mapList.get(i);
				Location to = cpy.to;
				boolean locked = false;
				for(phiMap cpy1 : mapList){
					if(cpy == cpy1)
						continue;
					if(to == cpy1.lfrom){
						locked = true;
						break;
					}
				}
				if(!locked){
					om.add(genMoveCopy(cpy, cpy.to));
					mapList.remove(i);
					allLocked = false;
				}
			}
			if(allLocked){//tested by test029
				phiMap bk = mapList.getFirst();
				om.add(genMoveCopy(bk, p2));
				bk.lfrom = p2;
				bk.constfrom=null;
			}
		}
		return om;
	}
	
	private boolean isBra(int insType){
		return insType==bne||insType == beq||insType==ble||insType==blt||insType==bge||insType==bge||insType==bra;
	}
	
	private int processPhi(LinkedList<Instruction> phiInsList, LinkedList<Block> inBlock, Block phiBlock, LinkedList<Block> funcBl, ControlFlowGraph cfg){
		for(Block b : inBlock){
			LinkedList<phiMap> mapList = new LinkedList<phiMap>();
			for(int i = phiInsList.size()-1; i>=0; i--){
				StaticSingleAssignment ssaIns = (StaticSingleAssignment)phiInsList.get(i);
				LinkedList<Block> bf = ssaIns.getPhiFrom();
				for(int i1 = bf.size() - 1; i1>=0; i1--){
					if(bf.get(i1) == b && (ssaIns.getOpsInsId().get(i1)!=null||ssaIns.getOp(i1).getType() == opConstant)){
//						if(ssaIns.getLoc(i1)==null)//means constant
//							mapList.add(new phiMap(ssaIns.getOp(i1), ssaIns.getOutputLoc()));
//						else 
						if(ssaIns.getLoc(i1)!=ssaIns.getOutputLoc())
							mapList.add(new phiMap(ssaIns.getLoc(i1), ssaIns.getOutputLoc()));
					}
				}
			}
			
			Instruction lastIns = b.getInsList().peekLast();
			if(b.getSuccessor().size()>=2){//need critical edge split
				Block jumpTo = (Block)lastIns.getOp(1);
				Block newB = cfg.new Block();
				newB.setIfElseRoute(phiBlock.getIfElseRoute());
				newB.getInsList().addAll(orderMove(mapList));
				cfg.linkSeqBlock(b, newB);
				b.getSuccessor().remove(phiBlock);
				funcBl.add(funcBl.indexOf(phiBlock), newB);
				if(jumpTo == phiBlock){
					lastIns.getOps().set(1, newB);
					for(Block b1 : inBlock)
						if(b1!=b){
							Instruction lastIns1 = b1.getInsList().peekLast();
							if(lastIns1==null||!isBra(lastIns1.getInsType()))//what if there is a branch to block which is not the phiBlock? seems not possible
								b1.getInsList().addLast(Instruction.genIns(bra, null, phiBlock));
						}
				}
				cfg.linkSeqBlock(newB, phiBlock);
				phiBlock.getPredecessor().remove(b);
			}else{
				if(lastIns!=null&&lastIns.getInsType()==bra)//loop end
					b.getInsList().addAll(b.getInsList().size()-1, orderMove(mapList));
				else
					b.getInsList().addAll(orderMove(mapList));
			}
			mapList.clear();
		}
		return funcBl.indexOf(phiBlock);
	}
	
	private void resolution(ControlFlowGraph cfg){
		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			for(int i=0;i<lb.size();i++){
				Block b = lb.get(i);
				LinkedList<Instruction> phiInsList = new LinkedList<Instruction>();
				for(Instruction ins : b.getInsList()){
					if(ins.getInsType() == phi){
						phiInsList.add(ins);
					}
				}
				if(!phiInsList.isEmpty()){
					i=processPhi(phiInsList, b.getPredecessor(), b, lb, cfg);
				}
			}
		}
	}
	
	public static void main(String[] args){
		int maxReg = 8;
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		LiveTime lt = new LiveTime(cfg);
		cfg.print();
		lt.analysisLiveTime();
		System.out.println();
		lt.print();
		LinearScan allocator = new LinearScan(lt.getLr(), maxReg);
		allocator.allocate();
		StaticSingleAssignment.setShowType(StaticSingleAssignment.showREG);
		System.out.println();
		lt.print();
		allocator.regAllocate(cfg);
		System.out.println();
		cfg.print();
	}
	
	class liveReg{
		Location reg;
		liveInternal allocateTo = null;
		Stack<liveHole> liveUntil = new Stack<liveHole>();
		liveReg(Location reg){
			this.reg = reg;
			liveUntil.push(new liveHole(Integer.MAX_VALUE, null));
		}
	};
	
	class liveHole{
		liveInternal belongTo;
		int nextLive;
		liveHole(int nextLive, liveInternal belongTo){
			this.nextLive = nextLive;
			this.belongTo = belongTo;
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof liveHole){
				liveHole lo = (liveHole)o;
				if(lo.belongTo == null)
					return false;
				return lo.belongTo.belongTo == this.belongTo.belongTo?true:false;//if same value
			}else
				return false;
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
			return o1.liveUntil.peek().nextLive<o2.liveUntil.peek().nextLive?-1:1;
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
	
	class phiMap{
		Location lfrom = null;
		Operand constfrom = null;
		Location to = null;
		
		public phiMap(Operand constfrom, Location to){
			this.constfrom = constfrom;
			this.to = to;
		}
		public phiMap(Location lfrom, Location to){
			this.lfrom = lfrom;
			this.to = to;
		}
	}
	
	static final int opConstant		=	3;
	static final int move	=	43;
	static final int 
					bne	=	20,
					beq	=	21,
					ble	=	25,
					blt	=	23,
					bge	=	22,
					bgt	=	24,
					 bra = 46, 
			         phi = 44;
	public static final int REG = Location.REG,//register
							MEM = Location.MEM,//variable
							STK = Location.STK;//stack
	
}
