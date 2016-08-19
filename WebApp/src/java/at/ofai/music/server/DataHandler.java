/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ofai.music.server;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

public class DataHandler {
	private HashMap <Integer, String> map =  new HashMap<Integer, String>();
        

	public HashMap<Integer,String> getHashmap() throws FileNotFoundException {
		Scanner sc = new Scanner(new FileReader("C:/Users/Bairong/Desktop/Bar-Per.txt"));
		 while (sc.hasNextLine()) {

       int bar = sc.nextInt();
       int pos = sc.nextInt();
       String per = pos*100/2481 + "%";
  
      map.put(bar, per);
	}
		return map;   
	}
}
