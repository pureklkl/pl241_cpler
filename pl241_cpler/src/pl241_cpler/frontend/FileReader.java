package pl241_cpler.frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
	private BufferedReader bufReader;
	private java.io.FileReader fReader;
	private String filePath_;
	
	public final char ERROCHAR = 0x00,
					  EOFCHAR  = 0x03;//uncertain
	
	//Error in open file will cause the program to exit
	FileReader(String filePath) {
		filePath_ = filePath;
		try {
			fReader = new java.io.FileReader(filePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			ShowError("Fail to open file :" + filePath);
			ShowError(e.getMessage());
			System.exit(0);
		}
		bufReader = new BufferedReader(fReader);
	}
	
	//Don't care error in close file
	public void Close(){
		try {
			fReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			ShowError("Fail to close file :" + filePath_);
			ShowError(e.getMessage());
		}
	}
	
	//Throw to Scanner which will give more information
	public char GetSym() throws IOException{
		char readChar = ERROCHAR;
		int intchar = bufReader.read();
		if(intchar == -1){
			readChar = EOFCHAR;
		}
		else{
			readChar = (char) intchar;
		}
		return readChar;
	}
	
	public void ShowTest() throws IOException{
		char showChar=GetSym();
		while(showChar!=EOFCHAR){
			System.out.print(showChar);
			showChar = GetSym();
		}
	}
	
	private void ShowError(String errorMsg)
	{
		System.out.println(errorMsg);
	}
	
	//main function for test this class
	public static void main(String[] args){
		String filename = args[0];
		FileReader freader= new FileReader(filename);
		try {
			freader.ShowTest();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		freader.Close();
	}
}
