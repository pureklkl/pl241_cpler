package pl241_cpler.frontend;

import java.util.ArrayList;
import java.util.HashSet;

import pl241_cpler.ir.*;
import pl241_cpler.ir.VariableSet.variable;

/**
  **********************************EBNF of PL241:*******************************
    letter = “a” | “b” | … | “z”.
    digit = “0” | “1” | … | “9”.
    relOp = “==“ | “!=“ | “<“ | “<=“ | “>“ | “>=“.
    ident = letter {letter | digit}.
    number = digit {digit}.
    designator = ident{ "[" expression "]" }.
    factor = designator | number | “(“ expression “)” | funcCall .
    term = factor { (“*” | “/”) factor}.
    expression = term {(“+” | “-”) term}.
    relation = expression relOp expression .
    assignment = “let” designator “<-” expression.
    funcCall = “call” ident [ “(“ [expression { “,” expression } ] “)” ].
    ifStatement = “if” relation “then” statSequence [ “else” statSequence ] “fi”.
    whileStatement = “while” relation “do” StatSequence “od”.
    returnStatement = “return” [ expression ] .
    statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
    statSequence = statement { “;” statement }.
    typeDecl = “var” | “array” “[“ number “]” { “[“ number “]” }.
    varDecl = typeDecl ident { “,” ident } “;” .
    funcDecl = (“function” | “procedure”) ident [formalParam] “;” funcBody “;” .
    formalParam = “(“ [ident { “,” ident }] “)” .
    funcBody = { varDecl } “{” [ statSequence ] “}”.
    computation = “main” { varDecl } { funcDecl } “{” statSequence “}” “.” .
 */

public class Parser {
	private Scanner s;
	private int token;
	private int val;
	private int id;
	private int lineNumber;
	private String str;
	
	private ControlFlowGraph cfg;
	private VariableSet varSet;
	
	Parser(String filePath){
		s = new Scanner(filePath);
		cfg = new ControlFlowGraph();
		varSet = new VariableSet();
	}
	
	private void next(){
		token = s.GetSym();
		val = s.val;
		lineNumber = s.lineNumber;
		str = s.str;
		id = s.id;
	}
	
	public void startParse(){
		next();
		computation();
	}
	
	private void showError(String msg){
		System.out.println("In line "+new Integer(lineNumber).toString()+":"+msg);
	}
	
	//designator = ident{ "[" expression "]" }.
	//def-use, use-def
	private Operand designator(){
		VariableSet.variable var = varSet.retrieval(id);
		if(var != null){
			next();
			ArrayList<Operand> dims = new ArrayList<Operand>();
			if(var.getType() == opArray){
				while(token == openbracketToken){
					next();
					dims.add(expression());
					if(token == closebracketToken){
						next();
					}else{
						showError("expected ] in designator");
					}
				}
				VariableSet.array var_array = (VariableSet.array)var;
				if(var_array.getDims().size()!=dims.size()){
					showError("dimension size error in designator");
				}
				var_array.setAddr(calArrayLocate(var_array, dims));
			}
			return var;
		}else{
			showError("Unknow identifier in designator");
		}

		return null;
	}
	
	//factor = designator | number | “(“ expression “)” | funcCall .
	private Operand factor(){
		Operand res = null;
		switch(token){
		case ident:				res = designator();
								if(res.getType() == opArray){
									Instruction addrLoad = Instruction.genIns(load, null, ((VariableSet.array)res).getAddr(), res);
									cfg.addInsToCurBlock(addrLoad);
									res = addrLoad;
								}break;
		case number:			res = new Constant(val);next();break;
		case openparenToken: 	next();res = expression();
								if(token == closeparenToken){
									next();
								}
								else{
									showError("expect ) in factor !");
								}break;
		case callToken: 		res = funCall();
								if(res == null){
									showError("Call non return function in factor !");
								}break;
		default:				showError("Unkonw factor !");break;
		}
		return res;
	}
	
	//term = factor { (“*” | “/”) factor}.
	private Operand term(){
		Operand x = factor();
		Operand res = null;
		while(token == timesToken || token == divToken){
			//Auto map by token - instruction
			int arthOp = token;
			next();
			Operand y = factor();
			res = Instruction.genIns(arthOp, x, y);
			cfg.addInsToCurBlock((Instruction)res);
		}
		if(res == null)
			return x;
		else
			return res;
	}
	
	//expression = term {(“+” | “-”) term}.
	private Operand expression(){
		Operand x = term();
		Operand res = null;
		while(token == plusToken || token == minusToken){
			//Auto map by token - instruction
			int arthOp = token;
			next();
			Operand y = term();
			res = Instruction.genIns(arthOp, x, y);
			cfg.addInsToCurBlock((Instruction)res);
		}
		if(res == null)
			return x;
		else
			return res;
	}
	
	//relation = expression relOp expression.
	private void relation(){
		Operand x = expression();
		if(isCompare()){
			//Auto reverse by change map between token and instruction
			int relOp = token;
			next();
			Operand y = expression();
			Operand res = Instruction.genIns(cmp, x, y);
			cfg.addInsToCurBlock((Instruction)res);
			cfg.addAndPushIns(Instruction.genIns(relOp, res, null));
		}
	}
	
	//assignment = “let” designator “<-” expression.
	//???? is global should be stored or use just move - now just move
	private void assignment(){
		next();
		Operand target = designator();
		//Create Phi function here
		if(token == becomesToken){
			next();
			Operand value = expression();
			if(target.getType() == opArray){
				cfg.addInsToCurBlock(Instruction.genIns(store, value, ((VariableSet.array)target).getAddr(), target));
			}else{
				cfg.addInsToCurBlock(Instruction.genIns(move, value, target));
				if(((VariableSet.scale)target).getScopeLevel() == global)
					cfg.curFunc().addGV((VariableSet.scale)target);
			}
			
		}else{
			showError("<- expected!");
		}
	}
	
	//funcCall = “call” ident [ “(“ [expression { “,” expression } ] “)” ].
	//???? seems something happen here for passing parameters
	//???? currently to load return value indicate by function
	//???? pre-compile and later fix the function define should be down
	//???? record use of global variable, store here as parameter
	private Operand funCall(){
		next();
		Operand returnVal = null;
		if(token == ident){
			Operand funcCheck = varSet.retrieval(id);
			int funcId = id;
			VariableSet.function func = null;
			if(funcCheck.getType() != opFunc){
				showError("function name expected");
			}
			else{
				func = (VariableSet.function)funcCheck;
				//if not recursive, copy called function's used global variable to current function
				if(func != cfg.curFunc()&&!(isDefaultFunc(funcId))){
					cfg.curFunc().addGV(func.getUsedGV());
				}
			}
			next();
			if(token == openparenToken){
				int index = 0;
				next();
				//inline for default function with non parameter
				if(isDefaultFunc(funcId)){
					switch(funcId){
					case OutputNewLine	:	cfg.addInsToCurBlock(Instruction.genIns(writeNL, null, null));break;
					case InputNum		: 	returnVal = Instruction.genIns(read, null, null);cfg.addInsToCurBlock((Instruction)returnVal);break;
					}
				}
				if(isExpression()){
					Operand var = expression();
					//inline for default function with one parameter
					if(isDefaultFunc(funcId)){
						switch(funcId){
						case OutputNum		:	cfg.addInsToCurBlock(Instruction.genIns(write, var, null));
												index++;break;
						}
					}else{
						cfg.addInsToCurBlock(Instruction.genIns(store, var, func.getParam(index++)));
					}
					while(token == commaToken){
						next();
						var = expression();
						cfg.addInsToCurBlock(Instruction.genIns(store, var, func.getParam(index++)));
					}
				}
				if(token == closeparenToken){
					next();
				}
				else{
					showError(") expected in funtion call");
				}
				if(index != func.paramNum()){
					showError("expected " + Integer.toString(func.paramNum())+"parameters in function call");
				}
			}
			
			if(isDefaultFunc(funcId)){
					return returnVal;
			}
			
			//store global parameter here
			HashSet<VariableSet.variable> usedGV = cfg.curFunc().getUsedGV();
			for(VariableSet.variable i : usedGV){
				//???? optimize here cancel stored var
				cfg.addInsToCurBlock(Instruction.genIns(store, i, null));
			}
			cfg.addInsToCurBlock(Instruction.genIns(bra, null, func));
			cfg.addAndMoveToNextBlock();
			for(VariableSet.variable i : varSet.getGlobalVar().values()){
				if(i.getType() == opScale){
					cfg.addInsToCurBlock(Instruction.genIns(load, null, i));
				}
			}
			if(func.getReturnState()){
				returnVal = Instruction.genIns(load, func, null);
				cfg.addInsToCurBlock((Instruction)returnVal);
				return returnVal;
			}
		}else{
			showError("identifier expected after function call");
		}
		return null;
	}
	
	//for each if, add phi functions to list, each if gets it phi from end of the list to itself's list
	//recode the if/else route to help find the def of the ssa value
	private void ifStatement(){
		next();
		relation();
		if(token == thenToken){
			cfg.pushCurRoute(ifRoute);
			cfg.addAndMoveToNextBlock();
			next();
			stateSequence();
			if(token == elseToken){
				cfg.changeCurRoute(elseRoute);
				cfg.addAndPushIns(Instruction.genIns(bra, null, null));
				cfg.addAndMoveToSingleBlock();
				cfg.fix2();
				next();
				stateSequence();
			}
			if(token == fiToken){
				cfg.popCurRoute();
				cfg.addAndMoveToNextBlock();
				cfg.fix();
				next();
				//push phi function here
			}else{
				showError("Expect fi in if statement!");
			}
		}else{
			showError("Expect then in if statement");
		}
	}
	
	//whileStatement = “while” relation “do” StatSequence “od”.
	private void whileStatement(){
		next();
		cfg.addAndMoveToNextBlock();
		relation();
		if(token == doToken){
			cfg.pushCurBlock();
			cfg.pushCurRoute(whileRoute);
			cfg.addAndMoveToNextBlock();
			next();
			stateSequence();
			cfg.loopBack();//add bra at the end of loop and link back to while
			//push phi function here
			if(token == odToken){
				cfg.popCurRoute();
				cfg.addAndMoveToNextBlock();
				cfg.fix();//fix bra in while block
				next();
			}else{
				showError("whileStatement expect 'od'");
			}
		}else{
			showError("whileStatement expect 'do'");
		}
	}
	
	private void returnStatement(){
		next();
		cfg.curFunc().setReturnState(true);
		Operand value = expression();
		cfg.addInsToCurBlock(Instruction.genIns(store, value, cfg.curFunc()));
		//means function return - addr will be determined when code generation
		cfg.addInsToCurBlock(Instruction.genIns(bra, null, null));
	}
	
	//statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	private void statement(){
		switch(token){
		case letToken: 		assignment(); 		break;
		case callToken: 	funCall();			break;
		case ifToken:		ifStatement();		break;
		case whileToken:	whileStatement();	break;
		case returnToken: 	returnStatement();	break;
		default:			showError("Unknow statement!");break;
		}
	}
	
	//statSequence = statement { “;” statement }.
	private void stateSequence(){
		statement();
		while(token == semiToken){
			next();
			statement();
		}
	}
	
	/**
	 * computation = “main” { varDecl } { funcDecl } “{” statSequence “}” “.” .
	 * */
	private void computation(){
		if(token == mainToken){
			next();
			while(token == varToken || token == arrayToken){
				varDecl();
			}
			
			while(token == functionToken || token == procToken){
				funcDecl();
			}
			
			if(token == beginToken){
				varSet.addAndMoveToNewScope();
				VariableSet.function func = varSet.new function();
				func.setName("main", id);
				cfg.resetCurRoute();
				cfg.addNewFuncBlock(func);
				next();
				stateSequence();
				if(token == endToken){
					next();
					if(token == periodToken){
						endCFG();
					}else{
						showError("Expected . at end of the program");
					}
				}
				else{
					showError("Expected } before .");
				}
			}else{
				showError("Expected { after main");
			}
			
		}else{
			showError("Expected main at beginning of the program");
		}
	}
	
	/**
	 * typeDecl = “var” | “array” “[“ number “]” { “[“ number “]” }.
	 * */
	private VariableSet.variable typeDecl(){
		VariableSet.variable res = null;
		if(token == varToken){
			res = varSet.new scale();
			next();
			return res;
		}else if(token == arrayToken){
			next();
			ArrayList<Integer> dims = null;
			while(token == openbracketToken){
				next();
				if(token == number){
					if(dims == null){
						dims = new ArrayList<Integer>();
					} 
					dims.add(val);
				}else{
					showError("Expected number after [ in array decleration");
				}
				next();
				if(token == closebracketToken){
					
				}else{
					showError("Expected ] after number in array decleration");
				}
				next();
			}
			if(dims == null){
				showError("Expected [] in array decleration");
			}
			res = varSet.new array(dims);
			return res;
		}
		next();
		return res;
	}
	
	/**
	 * varDecl = typeDecl ident { “,” ident } “;” .
	 * */
	private void varDecl(){
		VariableSet.variable varType = typeDecl();
		if(token == ident){
			
			varType.setName(str, id);
			if(!varSet.add(id, varType)){
				showError("Variable redefined, decleration fail!");
			}

			next();
			while(token == commaToken){
				next();
				if(token == ident){
					if(!varSet.add(id, varType.addNew(str, id))){
						showError("Variable redefined, decleration fail!");
					}
					
				}else{
					showError("Expected Identifier");
				}
				next();
			}
			if(token == semiToken){
				next();
			}else{
				showError("Expected ;");
			}
		}else{
			showError("Expected Identifier");
		}
	}
	
	//funcBody = { varDecl } “{” [ statSequence ] “}”.
	private void funcBody(VariableSet.function func){
		while(token == varToken || token == arrayToken){
			varDecl();
		}
		if(token == beginToken){
			next();
			if(isStatement())
				stateSequence();
			if(token == endToken){
				next();
			}else{
				showError("Expected } in funcBody");
			}
		}else{
			showError("Expected {");
		}
	}
	
	//formalParam = “(“ [ident { “,” ident }] “)” .
	private void formalParam(VariableSet.function func){
		next();
		if(token == ident){
			VariableSet.scale param = varSet.new scale();
			param.setName(str, id);
			if(varSet.add(id, param)){
				func.addParam(param);
			}else{
				showError("Param redefined, Func decleration fail!");
			}
			next();
			while(token == commaToken){
				next();
				if(token == ident){
					VariableSet.scale moreParam = param.addNew(str, id);
					if(varSet.add(id, moreParam)){
						func.addParam(moreParam);
					}
					else{
						showError("Param redefined, Func decleration fail!");
					}
					
				}else{
					showError("Expected Identifier");
				}
			}
		}
		if(token == closeparenToken){
			next();
		}else{
			showError("Expected )");
		}
	}
	
	//funcDecl = (“function” | “procedure”) ident [formalParam] “;” funcBody “;” .
	private void funcDecl(){
		next();
		if(token == ident){
			VariableSet.function func = varSet.new function();
			if(!varSet.add(id, func)){
				showError("Function redefined, decleration fail!");
			}
			func.setName(str, id);
			cfg.resetCurRoute();
			cfg.addNewFuncBlock(func);
			varSet.addAndMoveToNewScope();
			next();
			if(token  == openparenToken)
				formalParam(func);
			if(token == semiToken){
				next();
				funcBody(func);
				if(token == semiToken){
					varSet.returnToParentScope();
					next();
				}
				else{
					showError("Expected ; after function body");
				}
			}
			else{
				showError("Expected ; after function Identifer or Parameters");
			}
		}
		else{
			showError("Expected Identifier in funcDecl");
		}
	}
	
	private void endCFG(){
		
	}
	
	private Instruction calArrayLocate(VariableSet.array a, ArrayList<Operand> dimList){
		ArrayList<Integer> dimSize = a.getDims();
		Instruction prevAddr = null, resAddr = null;
		for(int i = 0; i < dimSize.size(); i++){
			Operand d = dimList.get(i);
			if(dimSize.size() != i+1){
				Constant dS = new Constant(dimSize.get(i+1));
				Instruction curOffset = Instruction.genIns(mul, d, dS);
				cfg.addInsToCurBlock(curOffset);
				if(prevAddr != null){
					prevAddr = Instruction.genIns(add, prevAddr, curOffset);
					cfg.addInsToCurBlock(prevAddr);
				}else{
					prevAddr = curOffset;
				}
			}else{
				if(prevAddr != null){
					Instruction lastDim = Instruction.genIns(add, prevAddr, d);
					cfg.addInsToCurBlock(lastDim);
					resAddr = Instruction.genIns(adda, lastDim, a);
				}else{
					resAddr = Instruction.genIns(adda, d, a);
				}
				cfg.addInsToCurBlock(resAddr);
			}
			
		}
		return resAddr;
	}
	
	//statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	private boolean isStatement(){
		return   (token >= letToken	&& token <= returnToken);
	}
	
	//expression start with ident, number, ( and call
	private boolean isExpression(){
		return (token >= openparenToken && token <= ident)||(token == callToken);
	}
	
	//==, !=, <, >=, <=, >
	private boolean isCompare(){
		return (token >= 20)&&( token <= 25);
	}
	
	private boolean isDefaultFunc(int id){
		return (id>defaultFuncMin)&&(id<defaultFuncMax);
	}
	
	public ControlFlowGraph getCFG(){
		return cfg;
	}
	
	public static void main(String[] args){
		Parser p = new Parser(args[0]);
		p.startParse();
		p.getCFG().print();
	}
	
	// Token - Value map
	private final int	erroToken		=	0, 
						
						timesToken 		= 	1,	//	*
						divToken		=	2,	//	/
						
						plusToken		=	11,	//	+
						minusToken		=	12,	//	-
						
						eqlToken		=	20,	//	==
						neqToken		=	21,	//	!=
						lssToken		=	22,	//	<
						geqToken		=	23,	//	>=
						leqToken		=	24,	//	<=
						gtrToken		=	25,	//	>
						
						periodToken		=	30,	//	.
						commaToken		=	31,	//	,
						openbracketToken=	32,	//	[
						closebracketToken=	34,	//	]
						closeparenToken	=	35,	//	)
						
						becomesToken	=	40,	//	<-
						thenToken		=	41,	//	then
						doToken			=	41,	//	do
						
						openparenToken	=	50,	//	(
						
						number			=	60,	//	number
						ident			=	61,	//	identifier
						
						semiToken		=	70,	//	;
						
						endToken		=	80,	//	}
						odToken			=	81,	//	od
						fiToken			=	82,	// 	fi
						
						elseToken		=	90,	//	else
						
						letToken		=	100,//	let
						callToken		=	101,//	call
						ifToken			=	102,//	if
						whileToken		=	103,//	while
						returnToken		=	104,//	return
						
						varToken		=	110,//	var
						arrayToken		=	111,//	array
						functionToken	=	112,//	function
						procToken		=	113,//	procedure
						
						beginToken		=	150,//	{
						mainToken		=	200,//	main
						eofToken		=	255;//	end of file
	
	//scope
	static final int 	global			=	0,
						local			=	1;
	
	// Operand - Value map
	static final int	opScale			= 	0,
						opArray			= 	1,
						opIns			=	2,
						opConstant		=	3,
						opFunc			=	4,
						opBlock			=	5;
	
	//instruction code
	public static final int
						decl			=	-1,
						neg				= 	0,
						add				=	11,
						sub				=	12,
						mul				=	1,
						div				=	2,
						cmp				=	5,

						adda			=	40,
						load			=	41,
						store			=	42,
						move			=	43,
						phi				=	44,
						end				=	45,
						bra				=	46,
						
						bne				=	20,
						beq				=	21,
						ble				=	25,
						blt				=	23,
						bge				=	22,
						bgt				=	24,
						
						read			=	30,
						write			=	31,
						writeNL			=	32;
	
	static final int 	defaultFuncMin	=	255,
						defaultFuncMax	=	1024,
						OutputNum      	=	300,
						OutputNewLine 	= 	301,//OutPutNum(x)
						InputNum		=	302;//x=InputNum()
	
	static final int normalRoute 	= 0,
			 		 ifRoute		= 1,
			 		 elseRoute		= 2,
					 whileRoute		= 3;
}
