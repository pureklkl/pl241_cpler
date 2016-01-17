package pl241_cpler.frontend;

import java.io.IOException;
import java.util.HashMap;

public class Scanner {
	private final int STARTSTAT		= 0,
					  SPECIALSTAT 	= 1,
					  IDENTSTAT		= 2,
					  NUMBERSTAT	= 3,
					  ENDSTAT		= 4,
					  
					  IDSTART		= 1024;
	
	private FileReader fReader;
	private char inputSym;
	private int token;
	private int accumId;
	private HashMap<String, Integer> identMap;
	
	public int val;
	public int id;
	public int lineNumber;
	
	//pre-populate keyword
	private void StoreKeyword(){
		identMap = new HashMap<String, Integer>();
		identMap.put("then", thenToken);
		identMap.put("do", doToken);
		identMap.put("od", odToken);
		identMap.put("fi", fiToken);
		identMap.put("else", elseToken);
		identMap.put("let", letToken);
		identMap.put("call", callToken);
		identMap.put("if", ifToken);
		identMap.put("while", whileToken);
		identMap.put("return", returnToken);
		identMap.put("var", varToken);
		identMap.put("array", arrayToken);
		identMap.put("function", functionToken);
		identMap.put("procedure", procToken);
		identMap.put("main", mainToken);
	}
	
	public Scanner(String filepath){
		fReader = new FileReader(filepath);
		accumId = IDSTART;
		StoreKeyword();
		Next();
	}
	
	private int GetNextId(){
		return accumId++;
	}
	
	private void Next(){
		try {
			inputSym = fReader.GetSym();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			ShowError(e.getMessage());
		}
	}
	
	public int GetSym(){
		token = erroToken;
		int state = STARTSTAT;
		String st = "";
		while(state != ENDSTAT){
			switch(state){
			case STARTSTAT		:if(Character.isWhitespace(inputSym)){
									if(inputSym == '\n'){
										lineNumber++;
									}
									Next();
								  }
								  else if(Character.isLetter(inputSym)){
									  st += inputSym;
									  id = GetNextId();
									  state = IDENTSTAT;
									  Next();
								  }
								  else if(Character.isDigit(inputSym)){
									  st += inputSym;
									  state = NUMBERSTAT;
									  Next();
								  }
								  else{
									  st += inputSym;
									  switch(inputSym){
								  
									  case 0xff : token = eofToken; state = ENDSTAT; break;
									  
									  case '*'	: token	= timesToken; state = ENDSTAT; Next(); break;
									  case '/'  : token	= divToken; state = ENDSTAT; Next(); break;
									  case '+'	: token	= plusToken; state = ENDSTAT; Next(); break;
									  case '-'	: token	= minusToken; state = ENDSTAT; Next(); break;
									
									  //These four character will move state to SPECALSTATE
									  //Special Token are ==, !=, <, <=, >, >= and <-
									  case '='	:  
									  case '!'	: 
									  case '<'	: 
									  case '>'	: state = SPECIALSTAT; Next(); break;
										  
									  case '.'	: token	= periodToken; state = ENDSTAT; Next(); break;
									  case ','	: token	= commaToken; state = ENDSTAT; Next(); break;
									  case '['	: token	= openbracketToken; state = ENDSTAT; Next(); break;
									  case ']'	: token	= closebracketToken; state = ENDSTAT; Next(); break;
									  case ')'	: token	= closeparenToken; state = ENDSTAT; Next(); break;
									  case '('	: token	= openparenToken; state = ENDSTAT; Next(); break;
									  
									  case ';'	: token	= semiToken; state = ENDSTAT; Next(); break;
									  
									  case '{'	: token	= beginToken; state = ENDSTAT; Next(); break;
									  case '}'	: token	= endToken; state = ENDSTAT; Next(); break;
									  }
								  }break;
			
			case SPECIALSTAT	: switch(inputSym){
									case '-'	: if(st.equals("<")){
												  	st += inputSym;
												  	token = becomesToken;
												  	Next();
												  }break;
												  
									case '='	: if(st.equals("<")){
													token = leqToken;
												  }else if(st.equals(">")){
													token = geqToken;
												  }else if(st.equals("=")){
													token = eqlToken;
												  }else if(st.equals("!")){
													token = neqToken;
												  }
												  Next();
												  st += inputSym;break;
												  
									default		: if(st.equals("<")){
													token = lssToken;
												  }else if(st.equals(">")){
													token = gtrToken;
												  }break;
								  }
								  state = ENDSTAT;break;
								  
			case IDENTSTAT		: if(Character.isLetter(inputSym)||Character.isDigit(inputSym)){
									st += inputSym;
									Next();
								  }else{
								    Integer tmp = identMap.get(st);
								    if(tmp!=null){
								    	token = tmp.intValue();
								    }else{
								    	token = ident;
								    }
									state = ENDSTAT;  
								  }break;
								  
			case NUMBERSTAT		: if(Character.isDigit(inputSym)){
									st += inputSym;
									Next();
								  }else{
									try{
									val = Integer.parseInt(st);
									token = number;
									}catch(NumberFormatException e){
										System.out.println("parse number fail!");
										ShowError(e.getMessage());
									}
									state = ENDSTAT;
								  }break;
								  
			case ENDSTAT		: break;
			default				: break;
			}
		}
		return token;
	}
	
	private void ShowError(String errorMsg)
	{
		System.out.println("file read error in line :" + Integer.toString(lineNumber));
		System.out.println(errorMsg);
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
}
