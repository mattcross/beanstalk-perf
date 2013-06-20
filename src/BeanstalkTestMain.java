import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.beanstalk.BeanstalkClient;
import com.trendrr.beanstalk.BeanstalkException;
import com.trendrr.beanstalk.BeanstalkJob;


public class BeanstalkTestMain {
  protected static Log log = LogFactory.getLog(BeanstalkTestMain.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//test: create and load 3000 tubes with 600 jobs each
		//loadQueues(makeTubes(3000),600);
		
		//test: create and load 1 tube with 500k jobs.  then unload
		//enqueueDequeue(makeTubes(1),500000);
		
		//test: create and load 3000 tubes with 600 jobs each.  then create and load 1 tube with 500k jobs.  then unload 500k job tube.
		//loadQueues(makeTubes(3000),600);
		//enqueueDequeue(makeTubes(1),500000);
		
		//test: create and load 3000 tubes with 600 jobs each.  then unload
		enqueueDequeue(makeTubes(3000),600);
	}
	
	private static List<BeanstalkClient> makeTubes(int tubeCount) {
		long start = System.nanoTime();
		List<BeanstalkClient> connections = new ArrayList<BeanstalkClient>(tubeCount);
		for(int i=0;i<tubeCount;i++) {
			BeanstalkClient bc = new BeanstalkClient("localhost",11300,String.format("tube_%d",i));
			try {
				bc.put(1024, 0, 60, "This is a job".getBytes());
			} catch (BeanstalkException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				connections.add(bc);
			}
		}
		
		long tubeSetupTime = System.nanoTime() - start;
		log.error(String.format("Tube setup took. %d nanos", tubeSetupTime));
		return connections;
	}
	
	private static void enqueueDequeue(final List<BeanstalkClient> queues, final int itemCount) {
		long start = System.nanoTime();
		
		loadQueues(queues,itemCount);
		emptyQueues(queues,itemCount);
		
		long enqueueDequeueTime = System.nanoTime() - start;
		log.error(String.format("EnqueueDequeue took. %d nanos", enqueueDequeueTime));
	}
	
	private static void emptyQueues(final List<BeanstalkClient> queues, final int itemCount) {
		final ExecutorService threadPool = Executors.newFixedThreadPool(20);
		final CompletionService<Void> pool = new ExecutorCompletionService<Void>(threadPool);
		
		long start = System.nanoTime();
		
		for(final BeanstalkClient queue : queues) {
		   pool.submit(new Callable<Void> () {
			   public Void call() {
				   int count = 0;
				   while(count < itemCount) {
					   try {
						BeanstalkJob job = queue.reserve(20);
						if(job != null) {
							queue.deleteJob(job);
							count++;
						}
					} catch (BeanstalkException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				   }
				return null;
			   }
		   });
		}
		
		for(int i=0;i<queues.size();i++) {
			try {
				pool.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		long enqueueTime = System.nanoTime() - start;
		log.error(String.format("Enqueue took. %d nanos", enqueueTime));
		
		threadPool.shutdown();
	}
	
	private static void loadQueues(final List<BeanstalkClient> queues, final int itemCount) {
		final ExecutorService threadPool = Executors.newFixedThreadPool(20);
		final CompletionService<Void> pool = new ExecutorCompletionService<Void>(threadPool);
		
		long start = System.nanoTime();
		
		for(final BeanstalkClient queue : queues) {
		   pool.submit(new Callable<Void> () {
			   public Void call() {
				   for(int i=0;i<itemCount;i++) {
					   try {
							queue.put(1024, 0, 60, "This is a job".getBytes());
						} catch (BeanstalkException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				   }
				return null;
			   }
		   });
		}
		
		for(int i=0;i<queues.size();i++) {
			try {
				pool.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		long enqueueTime = System.nanoTime() - start;
		log.error(String.format("Enqueue took. %d nanos", enqueueTime));
		
		threadPool.shutdown();
	}
}
