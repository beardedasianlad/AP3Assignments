package ap3assignment2;

import java.io.File;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerThread implements Runnable{
	ConcurrentLinkedQueue workQueue;
	Pattern reg;
	TreeSet results;
	boolean finishProcessing = false;
	public CrawlerThread(ConcurrentLinkedQueue workQueue, Pattern reg, TreeSet results){
		this.workQueue = workQueue;
		this.reg = reg;
		this.results = results;
	}
	@Override
	public void run() {
			this.processApplyRe(workQueue, reg, results);//consume		
	}
	
	public void processApplyRe(Queue workQueue , Pattern reg, TreeSet results){//consumer function
		while(true){
		synchronized(workQueue){
		while(workQueue.isEmpty()){
			try {
				workQueue.wait();
			} catch (InterruptedException e) {
				if(!workQueue.isEmpty()){
					// TODO Auto-generated catch block
					Matcher m; 
				    // open the directory
					String dir = (String)workQueue.poll();
					//System.out.println(dir);
				    File dd = new File(dir);
				    File[] files = dd.listFiles();
				    //for each entry in the directory
				    for(File file : files){
				    	//see if filename matches regular expression
				    	m = reg.matcher(file.getName());
				    	//System.out.println(file.getName());
				    	if(m.matches()){
				    		//duplicate fully qualified pathname for insertion into treeset
				    		results.add(dir+"/"+file.getName());		
				    		synchronized(results){
				    			results.notifyAll();
				    		}
				    		//System.out.println(file.getName());
				    	}
				    	
				    } 
					}
				
				return;
			}
		}
		Matcher m; 
	    // open the directory
		String dir = (String)workQueue.poll();
		//System.out.println(dir);
	    File dd = new File(dir);
	    File[] files = dd.listFiles();
	    //for each entry in the directory
	    for(File file : files){
	    	//see if filename matches regular expression
	    	m = reg.matcher(file.getName());
	    	//System.out.println(file.getName());
	    	if(m.matches()){
	    		//duplicate fully qualified pathname for insertion into treeset
	    		results.add(dir+"/"+file.getName());	
	    		synchronized(results){
	    			results.notifyAll();
	    		}
	    		//System.out.println(file.getName());
	    	}
	    }
	}
		}
}
	
	
	public void endThread(){
		finishProcessing = true;
	}
}
