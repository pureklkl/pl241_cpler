package pl241_cpler.backend;

import java.util.HashSet;
import java.util.LinkedList;

import pl241_cpler.frontend.Parser;
import pl241_cpler.ir.ControlFlowGraph;
import pl241_cpler.ir.ControlFlowGraph.Block;
import pl241_cpler.ir.Instruction;
import pl241_cpler.ir.StaticSingleAssignment;

public class LiveTime {
	
	private int seqId = 0;
	private int outputId = 0;
	private ControlFlowGraph cfg = null;
	private LinkedList<liveRange> lr = new LinkedList<liveRange>();
	
	public LiveTime(ControlFlowGraph cfg){
		this.cfg = cfg;
		assignId();
	}
	
	//judge the instruction i has a output
	public static boolean hasOutPut(Instruction i){
		int insType = i.getInsType();
		return insType == neg	|| insType == add	|| insType == sub	|| insType == mul	|| insType == div	||
			   insType == cmp	|| insType == read  ||
			   insType == adda	|| insType == load ||
			   insType == move	|| (insType == phi	&& !i.isArray());// phi for array has no output
	}
	
	//assign seqId for instructions, assign outputId for output of instruction(if it has output)
	//only instruction which has output can generate live range
	//operand used by instruction either be a constant(don't need register) or an output of another instruction(need register)
	public void assignId(){
		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			boolean prevPhi = false;
			for(int i = 0; i<lb.size(); i++){
				Block b = lb.get(i);
				LinkedList<Instruction> li = b.getInsList();
				for(int i1 = 0; i1<li.size(); i1++){
					Instruction ins = li.get(i1);
					if(!ins.isReal()){// kill and load array as definition is not real ins
						ins.unRealRemove();
						i1--;
						continue;
					}
					if(ins.getInsType() == phi){
						ins.setSeqId(seqId);
						prevPhi = true;
					}
					else{
						if(prevPhi){
							prevPhi = false;
							seqId++;
						}
						ins.setSeqId(seqId++);
					}
					if(hasOutPut(ins)){
						ins.setOutputId(outputId++);
						lr.add(new liveRange(ins));
					}
				}
				if(prevPhi){//test009 when nested if, two join block may be connected lead to phi in different block has same seq id 
					prevPhi = false;
					seqId++;
				}
			}
		}
	}
	
	//add phi's operand to current live
	private void processPhi(HashSet<Integer> live, Block b, Block sb){
		LinkedList<Instruction> li = sb.getInsList();
		for(int i = li.size() - 1; i >= 0 ; i--){
			Instruction ins = li.get(i);
			if(ins.getInsType() == phi){
				StaticSingleAssignment ssaIns = (StaticSingleAssignment)ins;
				ssaIns.setOpSeqId();//when process while, define id need be gotten before actual process this phi 
				LinkedList<Block> bf = ssaIns.getPhiFrom();
					for(int i1 = bf.size() - 1; i1>=0; i1--){
						// phi for array has no output
						if(bf.get(i1) == b && ssaIns.getOpsInsId().get(i1)!=null){
							Integer opd = ins.getOpsInsId().get(i1);
							live.add(ssaIns.getOpsInsId().get(i1));
						}
					}
			}
		}
	}
	
	//analysis live time
	public void analysisLiveTime(){
		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			for(int i = lb.size() - 1; i >= 0; i--){
				Block b = lb.get(i);
				HashSet<Integer> live = new HashSet<Integer>();
				if(b.getSuccessor()!=null)
					for(Block sb : b.getSuccessor()){
						if(sb.getLivedIn()!=null)
							live.addAll(sb.getLivedIn());
						processPhi(live, b, sb);
					}
				for(Integer opd : live){
					if(!b.getInsList().isEmpty())
						lr.get(opd).addRange(b.getFirstInsSeqId(), b.getLastInsSeqId());
				}
				LinkedList<Instruction> li = b.getInsList();
				for(int i1 = li.size() - 1; i1 >= 0 ; i1--){
					Instruction ins = li.get(i1);
					ins.setOpSeqId();
					if(ins.getOutputId()>nonOutput){
						lr.get(ins.getOutputId()).setFrom(ins.getSeqId());
						live.remove(ins.getOutputId());
					}
					for(int i2 = ins.getOpsInsId().size()-1 ; i2>=0; i2--){
						Integer opd = ins.getOpsInsId().get(i2);
						int insType = ins.getInsType();
						if(opd!=null 
						   && !(ins.getOps().size()==2&&(insType == move||insType == load) && i2 == 1)//this is output, move scale/kill->load
						   && insType!=phi){//phi is process separately
							if(b.getFirstInsSeqId()<=ins.getSeqId()-1)
								lr.get(opd).addRange(b.getFirstInsSeqId(), ins.getSeqId()-1);//
							live.add(opd);
						}
					}
				}
				if(b.isLoopHead()){
					Block lpEnd = b.getLoopEnd();
					for(Integer opd : live)
						lr.get(opd).addRange(b.getFirstInsSeqId(), lpEnd.getLastInsSeqId());
				}
				b.setLivedIn(live);
			}
		}
	}
	
	public LinkedList<liveRange> getLr() {
		return lr;
	}
	
	public void print(){
		for(liveRange iterLr : lr){
			iterLr.print();
			System.out.println();
		}
	}
	
	public void sprint(){
		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			for(int i = 0; i<lb.size(); i++){
				Block b = lb.get(i);
				LinkedList<Instruction> li = b.getInsList();
				for(int i1 = 0; i1<li.size(); i1++){
					Instruction ins = li.get(i1);
					if(ins.getOutputId()>nonOutput){
						 lr.get(ins.getOutputId()).print();
						 System.out.println();
					}
					else
						System.out.println(ins.print());
				}
			}
		}
	}
	
	public void gprint(){
		String tableHead = "";
		for(int i=0;i<baseTable;i++){
			tableHead+="\t";
		}
		for(int i=0;i<lr.size();i++){
			tableHead+=Integer.toString(i)+"\t";
		}
		System.out.println(tableHead);
		int sec = 0;
		for(LinkedList<Block> lb : cfg.getFuncSet().values()){
			for(int i = 0; i<lb.size(); i++){
				Block b = lb.get(i);
				LinkedList<Instruction> li = b.getInsList();
				for(int i1 = 0; i1<li.size(); i1++){
					sec++;
					if(sec%10 == 0){
						System.out.println(tableHead);
					}
					String insLive = "";
					Instruction ins = li.get(i1);
					insLive+=ins.print()+"\t";
					for(int i2=0;i2<lr.size();i2++){
						if(lr.get(i2).contain(ins.getSeqId()))
							insLive+="|\t";
						else
							insLive+="\t";
					}
					System.out.println(insLive);
				}
				System.out.println();
			}
		}
	}
	
	public static void main(String[] args){
		Instruction.genSSA();
		Parser p = new Parser(args[0]);
		p.startParse();
		ControlFlowGraph cfg = p.getCFG();
		LiveTime lt = new LiveTime(cfg);
		cfg.print();
		lt.analysisLiveTime();
		System.out.println();
		System.out.println();
		lt.sprint();
		System.out.println();
		System.out.println();
		lt.print();
	}
	
	public class liveRange {
		
		Instruction ins;//liveRange for output of the instruction
		Location lc = null;
		int lastLive;
		LinkedList<liveInternal> liList = new LinkedList<liveInternal>();
		
		public liveRange(Instruction ins){
			this.ins = ins;
		}
		
		public void print() {
			System.out.print(ins.print()+" : ");
			for(liveInternal lt : liList){
				lt.print();
			}
			if(lc != null){
				System.out.print(lc.print());
			}
		}
		
		private void addRange(int from, int to){
			if(!liList.isEmpty()){
				if(liList.getFirst().from <= to+1 && liList.getFirst().from> from)//overlap from -> this.from -> to-> this.to
					liList.getFirst().from = from;
				else if(liList.getFirst().to+1>= from && liList.getFirst().to<=to){
					int lastTo = to;
					for(int i = 1; i<liList.size(); i++){
						if(liList.get(i).from<=to+1){
							lastTo = lastTo>liList.get(i).to?lastTo:liList.get(i).to;
							liList.remove(i);
							i--;
						}else{
							break;
						}
					}
					liList.getFirst().to = lastTo;
					liList.getFirst().index = liList.size()-1;
				}
				else if(liList.getFirst().from > to+1){
					liList.add(0, new liveInternal(from, to, this, liList.size()));
				}
			}	
			else{
				liList.add(0, new liveInternal(from, to, this, liList.size()));
				lastLive = to;
			}
		}
		
		private void setFrom(int from){
			if(liList.isEmpty()){
				liList.add(0, new liveInternal(from, from, this, liList.size()));
				lastLive = from;
			}
			liList.getFirst().from = from;
		}
		
		// TODO need change to binary search
		public boolean contain(int i){
			for(liveInternal lt : liList){
				if(i>=lt.from&&i<=lt.to){
					return true;
				}
			}
			return false;
		}
		
		public class liveInternal{
			
			liveRange belongTo;
			int index;
			int from, to;
			
			public liveInternal(int from, int to, liveRange belongTo, int index){
				this.from	= from;
				this.to		= to;
				this.belongTo = belongTo;
				this.index = index;
			}

			public void print() {
				System.out.print(Integer.toString(from)+" -> " + Integer.toString(to) +"\t");
			}
			
		}
	};
	
	static private final int baseTable = 7;
	
	static private final int 

	neg				= 	0,
	add				=	11,
	sub				=	12,
	mul				=	1,
	div				=	2,
	cmp				=	5,
	
	read			=	30,

	adda			=	40,
	load			=	41,
	move			=	43,
	phi				=	44;
	
	public static final int nonOutput = -1;
}
