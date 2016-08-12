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

public class imageURL {
	private HashMap <Integer, String> url =  new HashMap <Integer, String>();
        

	public HashMap<Integer,String> getURLmap() throws FileNotFoundException {
		Scanner sc = new Scanner(new FileReader("C:/Users/Bairong/Desktop/Bar-URL.txt"));
		 while (sc.hasNextLine()) {

       int bar = sc.nextInt();
       String URL = sc.next();
  
      url.put(bar, URL);
	}
		return url;   
	}
}