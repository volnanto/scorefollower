package at.ofai.music.match;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

public class DataStorage {
	private HashMap <Integer, Integer> map =  new HashMap<Integer, Integer>();

	public HashMap<Integer,Integer> getHashmap() throws FileNotFoundException {
		Scanner sc = new Scanner(new FileReader("C:/Users/Bairong/Desktop/Sos.txt"));
		 while (sc.hasNextLine()) {

       int timing= sc.nextInt();
       int bar = sc.nextInt();
  
      map.put(timing, bar);
	}
		return map;   
	}
}

