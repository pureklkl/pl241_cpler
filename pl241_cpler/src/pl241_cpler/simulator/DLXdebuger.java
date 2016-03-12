package pl241_cpler.simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DLXdebuger extends DLX {
    static int Rc;
    static int preRbVal;
	public static void debugExecute() throws IOException {
        int origc = 0; // used for F2 instruction RET
        for (int i = 0; i < 32; i++) { R[i] = 0; };
        PC = 0; R[30] = MemSize - 1;

        try {

            execloop:
            while (true) {
                R[0] = 0;
                disassem(M[PC]); // initializes op, a, b, c
                if(format!=3)
                	preRbVal = R[b];// cache Rb' value so that instruction like R1 = R1+1 can be shown correctly
                int nextPC = PC + 1;
                if (format==2) {
                    origc = c; // used for RET
                    Rc = c;// cache Rc for F2, so that it can be shown when debug
                    c = R[c];  // dirty trick what if c>
                }
                switch (op) {
                    case ADD:
                    case ADDI:
                        R[a] = R[b] + c;
                        break;
                    case SUB:
                    case SUBI:
                        R[a] = R[b] - c;
                        break;
                    case CMP:
                    case CMPI:
                        R[a] = R[b] - c; // can not create overflow
                        if (R[a] < 0) R[a] = -1;
                        else if (R[a] > 0) R[a] = 1;
                        // we don't have to do anything if R[a]==0
                        break;
                    case MUL:
                    case MULI:
                        R[a] = R[b] * c;
                        break;
                    case DIV:
                    case DIVI:
                        R[a] = R[b] / c;
                        break;
                    case MOD:
                    case MODI:
                        R[a] = R[b] % c;
                        break;
                    case OR:
                    case ORI:
                        R[a] = R[b] | c;
                        break;
                    case AND:
                    case ANDI:
                        R[a] = R[b] & c;
                        break;
                    case BIC:
                    case BICI:
                        R[a] = R[b] & ~c;
                        break;
                    case XOR:
                    case XORI:
                        R[a] = R[b] ^ c;
                        break;
                    // Shifts: - a shift by a positive number means a left shift
                    //         - if c > 31 or c < -31 an error is generated
                    case LSH:
                    case LSHI:
                        if ((c < -31) || (c >31)) {
                            System.out.println("Illegal value " + c +
                                    " of operand c or register c!");
                            bug(1);
                        }
                        if (c < 0)  R[a] = R[b] >>> -c;
                        else        R[a] = R[b] << c;
                        break;
                    case ASH:
                    case ASHI:
                        if ((c < -31) || (c >31)) {
                            System.out.println("DLX.execute: Illegal value " + c +
                                    " of operand c or register c!");
                            bug(1);
                        }
                        if (c < 0)  R[a] = R[b] >> -c;
                        else        R[a] = R[b] << c;
                        break;
                    case CHKI:
                    case CHK:
                        if (R[a] < 0) {
                            System.out.println("DLX.execute: " + PC*4 + ": R[" + a + "] == " +
                                    R[a] + " < 0");
                            bug(14);
                        } else if (R[a] >= c) {
                            System.out.println("DLX.execute: " + PC*4 + ": R[" + a + "] == " +
                                    R[a] + " >= " + c);
                            bug(14);
                        }
                        break;
                    case LDW:
                    case LDX: // remember: c == R[origc] because of F2 format
                        R[a] = M[(R[b]+c) / 4];
                        break;
                    case STW:
                    case STX: // remember: c == R[origc] because of F2 format
                        M[(R[b]+c) / 4] = R[a];
                        break;
                    case POP:
                        R[a] = M[R[b] / 4];
                        R[b] = R[b] + c;
                        break;
                    case PSH:
                        R[b] = R[b] + c;
                        M[R[b] / 4] = R[a];
                        break;
                    case BEQ:
                        if (R[a] == 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(40);
                        }
                        break;
                    case BNE:
                        if (R[a] != 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(41);
                        }
                        break;
                    case BLT:
                        if (R[a] < 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(42);
                        }
                        break;
                    case BGE:
                        if (R[a] >= 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(43);
                        }
                        break;
                    case BLE:
                        if (R[a] <= 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(44);
                        }
                        break;
                    case BGT:
                        if (R[a] > 0) nextPC = PC + c;
                        if ((nextPC < 0) || (nextPC > MemSize/4)) {
                            System.out.println(4*nextPC + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(45);
                        }
                        break;
                    case BSR:
                        R[31] = (PC+1) * 4;
                        nextPC = PC + c;
                        break;
                    case JSR:
                        R[31] = (PC+1) * 4;
                        nextPC = c / 4;
                        break;
                    case RET:
                        if (origc == 0) break execloop; // remember: c==R[origc]
                        if ((c < 0) || (c > MemSize)) {
                            System.out.println(c + " is no address in memory (0.."
                                    + MemSize + ").");
                            bug(49);
                        }
                        nextPC = c / 4;
                        break;
                    case RDI:
                        System.out.print("?: ");
                        String line = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                        R[a] = Integer.parseInt(line);
                        break;
                    case WRD:
                        System.out.print(R[b] + "  ");
                        break;
                    case WRH:
                        System.out.print("0x" + Integer.toHexString(R[b]) + "  ");
                        break;
                    case WRL:
                        System.out.println();
                        break;
                    case ERR:
                        System.out.println("Program dropped off the end!");
                        bug(1);
                        break;
                    default:
                        System.out.println("DLX.execute: Unknown opcode encountered!");
                        bug(1);
                }
                System.out.println();
                System.out.print( PC*4 + "\t: " +disassembleD(M[PC]));
                System.out.println();
                PC = nextPC;
            }
        	System.out.println("Over!!");
        }
        catch (java.lang.ArrayIndexOutOfBoundsException e ) {
            System.out.println( "failed at " + PC*4 + ",   "  + disassemble( M[PC] ) );
        }

    }
	
	static String regVar(int reg, int val){
		return "R"+reg+"["+val+"]";
	}
	
	static String immediate(int imd){
		return "#"+imd;
	}
	
	static String disassembleD(int instructionWord) {
		
        disassem(instructionWord);
        String line = mnemo[op] + "\t";

        switch (op) {

            case WRL:
                return line += "\n";
            case BSR:
            	return line +=immediate(c*4) ;//branch address is multiplied by 4
            case JSR:
            	return line +=immediate(c);
            case RET:
            	return line +=regVar(c, R[c]);
            case RDI:
                return line += regVar(a, R[a]);
            case WRD:
            case WRH:
                return line += regVar(b, R[b]);
            case CHKI:
            	return line += regVar(a, R[a]) + "\t" + immediate(c);
            case BEQ:
            case BNE:
            case BLT:
            case BGE:
            case BLE:
            case BGT:
            	return line += regVar(a, R[a]) + "\t" + regVar(Rc, c*4);
            case CHK:
                return line += regVar(a, R[a]) + "\t" + regVar(Rc, c);
            case ADDI:
            case SUBI:
            case MULI:
            case DIVI:
            case MODI:
            case CMPI:
            case ORI:
            case ANDI:
            case BICI:
            case XORI:
            case LSHI:
            case ASHI:
            case LDW:
            case POP:
            case STW:
            case PSH:
            	return line+=regVar(a, R[a])+"\t"+regVar(b, preRbVal)+"\t"+immediate(c);
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case CMP:
            case OR:
            case AND:
            case BIC:
            case XOR:
            case LSH:
            case ASH:
            case LDX:
            case STX:
                return line +=regVar(a, R[a])+"\t"+regVar(b, preRbVal) +"\t"+regVar(c, R[c]);
            default:
                return line += "\n";
        }
    }


}
