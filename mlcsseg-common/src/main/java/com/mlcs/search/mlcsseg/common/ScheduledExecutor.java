package com.mlcs.search.mlcsseg.common;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;



public class ScheduledExecutor {
	
	static class SegTF implements ThreadFactory{

		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "SegmentScheduledExecutorThread");
			t.setDaemon(true);
			return t;
		}
		
	}
	
	final public static ScheduledExecutorService ScheduledService = Executors.newSingleThreadScheduledExecutor(new SegTF());
	
	
	public static void submit(Runnable cmd, long periodMilliSenconds){
		ScheduledService.scheduleAtFixedRate(cmd, 10l, periodMilliSenconds, TimeUnit.MILLISECONDS);
	}
	
}
