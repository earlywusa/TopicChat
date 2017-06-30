package chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import javax.servlet.AsyncContext;



public class ClientNotify {
	private BlockingQueue<Message> messageQueue;
	private Queue<Message> storage;
	private Map<String, AsyncContext> asyncContexts;
	private boolean running = true;
	Thread sendHelper;
	public ClientNotify(){
		sendHelper = new Thread(new Runnable(){

			@Override
			public void run() {
				while(running){
					try {
						//TimeUnit.SECONDS.sleep(5);
						// Waits until a message arrives
//						Message messageDummy = new Message();
//						messageDummy.time(new Date());
//						messageDummy.text("some text");
						
						// Waits until a message arrives
						Message message = messageQueue.take();

						// Put a message in a store
						storage.add(message);
//						storage.add(messageDummy);

						// Keep only last 100 messages
						if (storage.size() > 10) {
							storage.remove(0);
						}
						System.out.println("number of connection: " + asyncContexts.size());
						for(AsyncContext asyncContext : asyncContexts.values()){
							ClientNotify.send(asyncContext.getResponse().getWriter(), message);
						}
					} catch (InterruptedException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		});
		
	}
	
	public void start(){
		sendHelper.start();
	}
	
	public void destroy(){
		running = false;
	}

	public BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
	}


	public void setMessageQueue(BlockingQueue<Message> messageQueue) {
		this.messageQueue = messageQueue;
	}


	public Queue<Message> getStorage() {
		return storage;
	}


	public void setStorage(Queue<Message> storage) {
		this.storage = storage;
	}


	public Map<String, AsyncContext> getAsyncContexts() {
		return asyncContexts;
	}


	public void setAsyncContexts(Map<String, AsyncContext> asyncContexts) {
		this.asyncContexts = asyncContexts;
	}
	
	public static void send(PrintWriter writer, Message message){
		System.out.println("sending message: " + message.getTopic() + " " + message.getMessage());
		writer.print("topic: " + message.getTopic() + "\n");
		writer.print("text: " + message.getMessage() + "\n");
		writer.println();
		writer.flush();
	}
}
