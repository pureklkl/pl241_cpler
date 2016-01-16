package pl241_cpler.frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
	private BufferedReader bufreader;
	private java.io.FileReader freader;
	private String filepath_;
	
	public final char ERROCHAR = 0x00,
					  EOFCHAR  = 0xff;
	
	//Error in open file will cause the program to exit
	FileReader(String filepath) {
		filepath_ = filepath;
		try {
			freader = new java.io.FileReader(filepath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			showError("Fail to open file :" + filepath);
			showError(e.getMessage());
			System.exit(0);
		}
		bufreader = new BufferedReader(freader);
	}
	
	//Don't care error in close file
	public void close(){
		try {
			freader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			showError("Fail to close file :" + filepath_);
			showError(e.getMessage());
		}
	}
	
	//Throw to Scanner which will give more information
	public char getSym() throws IOException{
		char readchar = ERROCHAR;
		int intchar = bufreader.read();
		if(intchar == -1){
			readchar = EOFCHAR;
		}
		else{
			readchar = (char) intchar;
		}
		return readchar;
	}
	
	public void showTest() throws IOException{
		char showchar=getSym();
		while(showchar!=EOFCHAR){
			System.out.print(showchar);
			getSym();
		}
	}
	
	private void showError(String errorMsg)
	{
		System.out.println(errorMsg);
	}
	
	//main function for test this class
	public static void main(String[] args){
		String filename = args[0];
		FileReader freader= new FileReader(filename);
		try {
			freader.showTest();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		freader.close();
	}
}
