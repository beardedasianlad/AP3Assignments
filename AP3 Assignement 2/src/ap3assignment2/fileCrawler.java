package ap3assignment2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

public class fileCrawler {
	static boolean processDone = false;
	
	/*
	 * routine to convert bash pattern to regex pattern
	 * 
	 * e.g. if bashpat is "*.c", pattern generated is "^.*\.c$"
	 *      if bashpat is "a.*", pattern generated is "^a\..*$"
	 *
	 * i.e. '*' is converted to ".*"
	 *      '.' is converted to "\."
	 *      '?' is converted to "."
	 *      '^' is put at the beginning of the regex pattern
	 *      '$' is put at the end of the regex pattern
	 *
	 * assumes 'pattern' is large enough to hold the regular expression
	 */
	
	static String convertPattern(String bashpat){
		StringBuilder pat = new StringBuilder();
		int start, length;
		pat.append('^');
		if (bashpat.charAt(0) == '\'') {	// double quoting on Windows
			start = 1;
			length = bashpat.length() - 1;
		} else {
			start = 0;
			length = bashpat.length();
		}
		for (int i = start; i < length; i++) {
			switch(bashpat.charAt(i)) {
			case '*': pat.append('.'); pat.append('*'); break;
			case '.': pat.append('\\'); pat.append('.'); break;
			case '?': pat.append('.'); break;
			default:  pat.append(bashpat.charAt(i)); break;
			}
		}
		pat.append('$');
		return pat.toString();
		
	}
	
	/*
	 * recursively opens directory files
	 *
	 * if the directory is successfully opened, it is added to the linked list
	 *
	 * for each entry in the directory, if it is itself a directory,
	 * processDirectory is recursively invoked on the fully qualified name
	 *
	 * if there is an error opening the directory, an error message is
	 * printed on stderr, and 1 is returned, as this is most likely indicative
	 * of a protection violation
	 *
	 * if there is an error duplicating the directory name, or adding it to
	 * the linked list, an error message is printed on stderr and 0 is returned
	 *
	 * if no problems, returns 1
	 */
	
	public static void processDirectory(String dirName,Queue q){
		synchronized(q){
		try {
			 File file = new File(dirName);	// create a File object
			 if (file.isDirectory()) {	// a directory - could be symlink
				 String entries[] = file.list();
				 if (entries != null) {	// not a symlink
	               //System.out.println(dirName);// print out the name
					 if(q.add(dirName) == false){
						 System.out.println("Error adding " + dirName + " to the queue");
					 	}
					 q.notifyAll(); // notify all worker threads
		       for (String entry : entries ) {
	                  if (entry.compareTo(".") == 0)
	                     continue;
	                  if (entry.compareTo("..") == 0)
	                     continue;
			  processDirectory(dirName+"/"+entry, q);
		       }
		    }
		 }
	      } catch (Exception e) {
	         System.err.println("Error processing "+dirName+": "+e);
	      }
		}
	}
	
	/*
	 * comparison function between strings
	 *
	 * need this shim function to match the signature in treeset.h
	 */
	
	public int scmp(String a, String b){
		return a.compareTo(b);
	}
	
	/*
	 * applies regular expression pattern to contents of the directory
	 *
	 * for entries that match, the fully qualified pathname is inserted into
	 * the treeset
	 */
	
	
	public boolean getProcessDone(){
		return processDone;
	}
	
	public static void main(String[] argv){
		
		long start = System.currentTimeMillis();
		//checks if parameters are correct
		if(argv.length < 2){
			System.out.println("Usage: java fileCrawler [pattern] [dir] ...");
			return;
		}
		
		int numOfThreads;
		if(System.getenv("CRAWLER_THREADS") == null){
			numOfThreads = 10;
		}	
		else{
			numOfThreads = Integer.parseInt(System.getenv("CRAWLER_THREADS"));
		}
			
		
		//convert bash expression to regular expression and compile	
		Pattern pattern;
		pattern = Pattern.compile(convertPattern(argv[0]));
		System.out.println(argv[0] +"--->"+pattern);

		ConcurrentLinkedQueue workQueue = new ConcurrentLinkedQueue();
		
		
		//Declare resultsQueue
		TreeSet<String> resultsTree = new TreeSet<String>();
		
		
		ArrayList<Thread> threadArray = new ArrayList<>();
		
		CrawlerThread job = new CrawlerThread(workQueue, pattern, resultsTree);
		for(int i = 0; i < numOfThreads; i++){
			Thread workerThread = new Thread(job);
			threadArray.add(workerThread);
			threadArray.get(i).start();
		}
		
		//Worker thread retrieves scans the entires in the directory
		//populate workQueue
		processDirectory(argv[1], workQueue);//producer role
		
		//wait for processing to be finished
		synchronized(resultsTree){
			while(resultsTree.isEmpty()){
				try {
					resultsTree.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		
		for(int i = 0; i < numOfThreads; i++){
			threadArray.get(i).interrupt();
		}
		
		//create iterator to traverse files matching pattern in sorted order

		for(String s : resultsTree){
			System.out.println(s);
		}
			
		
		//System.out.println(resultsQueue.size());
		long end = System.currentTimeMillis();
		System.out.println("Elapsed time: " + (end - start) + " milliseconds");
		//String value = System.getenv("CRAWLER_THREADS");
		//System.out.format("%s%n", value);
	}
	
	
	
	
	
	
	
	
	
}
