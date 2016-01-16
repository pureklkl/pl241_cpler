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
	
	public void close(){
		try {
			freader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			showError("Fail to close file :" + filepath_);
			showError(e.getMessage());
		}
	}
	
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
	
	private void showError(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}
