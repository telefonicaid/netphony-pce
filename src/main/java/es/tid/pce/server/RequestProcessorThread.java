package es.tid.pce.server;

import java.util.LinkedList;

public class RequestProcessorThread extends Thread{
	
	private LinkedList queue;
	
	public RequestProcessorThread(LinkedList queue){
		this.queue=queue;
	}
		
	public void run(){
		Runnable r;

        while (true) {
            synchronized(queue) {
                while (queue.isEmpty()) {
                    try
                    {
                        queue.wait();
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }

                r = (Runnable) queue.removeFirst();
            }

            // If we don't catch RuntimeException, 
            // the pool could leak threads
            try {
                r.run();
            }
            catch (RuntimeException e) {
                // You might want to log something here
            }
        }
    }

}
