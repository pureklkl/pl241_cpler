package pl241_cpler.frontend;

import java.util.ArrayList;
import pl241_cpler.ir.*;;

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
	}
	
	public void startParse(){
		next();
		computation();
	}
	
	private void showError(String msg){
		System.out.println("In line "+new Integer(lineNumber).toString()+":"+msg);
	}
	
	private void stateSequence(){
		
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
				stateSequence();
				if(token == endToken){
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
					}else{
						dims.add(val);
					}
					
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
			next();
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
			
			varType.setName(str);
			if(!varSet.add(id, varType)){
				showError("Variable redefined, decleration fail!");
			}

			next();
			while(token == commaToken){
				next();
				if(token == ident){
					if(!varSet.add(id, varType.addNew(str))){
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
				showError("Expected }");
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
			param.setName(str);
			if(varSet.add(id, param)){
				func.addParam(param);
			}else{
				showError("Param redefined, Func decleration fail!");
			}
			next();
			while(token == commaToken){
				next();
				if(token == ident){
					VariableSet.scale moreParam = param.addNew(str);
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
			next();
			if(token == ident){
				VariableSet.function func = varSet.new function();
				if(varSet.add(id, func)){
					showError("Variable redefined, decleration fail!");
				}
				func.setName(str);
				cfg.addNewFuncBlock(func);
				varSet.addAndMoveToNewScope();
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
		}else{
			showError("Expected Identifier in funcDecl");
		}
	}
	
	private void endCFG(){
		
	}
	
	//statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	private boolean isStatement(){
		return   (token == letToken	||
				  token == callToken|| 
				  token == ifToken	||
				  token == whileToken||
				  token == returnToken);
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
	
	// Operand - Value map
	static final int	opScale			= 	0,
						opArray			= 	1,
						opIns			=	2,
						opConstant		=	3;
			
	
}
