/*
 * HowOpenSource.com
 * Copyright (C) 2015 admin@howopensource.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package chat;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/chat")
public class TopicChatServlet extends HttpServlet {

	private AtomicLong counter = new AtomicLong();
	private boolean running;

	// Keeps all open connections from browsers
	private Map<String, AsyncContext> asyncContexts = new ConcurrentHashMap<>();

	// Temporary store for messages when arrived
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

	// Keep last messages
	//private List<Message> messageStore = new CopyOnWriteArrayList<>();

	private Map<String, List<Message>> topicToMessageMap = new ConcurrentHashMap<>();
	private Map<AsyncContext, String> AsyncContextToTopicMap = new ConcurrentHashMap<>();
	
	public TopicChatServlet(){
		topicToMessageMap.put("default", new CopyOnWriteArrayList<>());
		Map<String, AsyncContext> asyncContexts = new ConcurrentHashMap<>();
	}

	// Thread that waits for new message and then redistribute it
	private Thread notifier = new Thread(new Runnable() {

		@Override
		public void run() {
			while (running) {
				try {
					// Waits until a message arrives
					Message message = messageQueue.take();
					List<Message> messageStore = topicToMessageMap.get("default");
					String topic = message.getTopic();
					if(topic != null ){
						if(topicToMessageMap.get(topic) == null){					
							System.out.println("put new message to topic: " + topic + " text: " + message.getTopic());
							topicToMessageMap.put(topic, new CopyOnWriteArrayList<>());
						}
						messageStore = topicToMessageMap.get(topic);	
						
					}
					// Put a message in a store
					messageStore.add(message);

					// Keep only last 100 messages
					if (messageStore.size() > 100) {
						messageStore.remove(0);
					}
					
					
					System.out.println("number of context: " + asyncContexts.values().size());
					// Sends the message to all the AsyncContext's response
					for (AsyncContext asyncContext : asyncContexts.values()) {
						try {
							
							if(AsyncContextToTopicMap.get(asyncContext).equals(topic)){
								sendMessage(asyncContext.getResponse().getWriter(), message);
							}
							else{
								String tmpTopic = AsyncContextToTopicMap.get(asyncContext);
								System.out.println("async: " + asyncContext + " has not informed, it's topic is: " + tmpTopic );
							}
						} catch (Exception e) {
							// In case of exception remove context from map
							asyncContexts.values().remove(asyncContext);
						}
					}
				} catch (InterruptedException e) {
					// Log exception, etc.
				}
			}
		}
	});


	@Override
	public void destroy() {
		// Stops thread and clears queue and stores
		running = false;
		asyncContexts.clear();
		messageQueue.clear();
		//messageStore.clear();
	}


	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		running = true;
		notifier.start();
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// This is for loading home page when user comes for the first time
		if (request.getAttribute("index") != null) {
			List<Message> messageStore = topicToMessageMap.get("default");
			request.setAttribute("messages", messageStore);
			request.getRequestDispatcher("/chat.jsp").forward(request, response);
			return;
		}

		// Check that it is SSE request
		if ("text/event-stream".equals(request.getHeader("Accept"))) {

			// This a Tomcat specific - makes request asynchronous
			request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
			
			List<Message> messageStore = topicToMessageMap.get("default");
			String topic = request.getHeader("topic");
			if(topic != null ){
				if(topicToMessageMap.get(topic) != null){
					messageStore = topicToMessageMap.get(topic);	
				}
			}
			else{
				topic = "default";
			}
			
			
			// Set header fields
			response.setContentType("text/event-stream");
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Connection", "keep-alive");
			response.setCharacterEncoding("UTF-8");

			// Parse Last-Event-ID header field which should contain last event received
			String lastMsgId = request.getHeader("Last-Event-ID");
			if ((lastMsgId != null) && !lastMsgId.trim().isEmpty()) {
				long lastId = 0;
				try {
					lastId = Long.parseLong(lastMsgId);
				} catch (NumberFormatException e) {
					// Do nothing as we have default value
				}
				if (lastId > 0) {
					// Send all messages that are not send - e.g. with higher id
					for (Message message : messageStore) {
						if (message.getId() > lastId) {
							sendMessage(response.getWriter(), message);
						}
					}
				}
			} else {
				long lastId = 0;
				try {
					lastId = messageStore.get(messageStore.size() - 1).getId();
				} catch (Exception e) {
					// Do nothing as this just gets the last id
				}
				if (lastId > 0) {
					// Send some ping with the last id. Idea is browser to be informed
					// which is the last event id. Also tell the browser if connection
					// fails to reopen it after 1000 milliseconds
					response.getWriter().println("retry: 1000\n");
					Message message = new Message(lastId, "reconnecting...", "default", new Date());
					sendMessage(response.getWriter(), message);
				}
			}

			// Generate some unique identifier used to store context in map
			final String id = UUID.randomUUID().toString();

			// Start asynchronous context and add listeners to remove it in case of errors
			
			final AsyncContext ac = request.startAsync();
			ac.addListener(new AsyncListener() {

				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					//System.out.println("listener complte: " + id); 
					asyncContexts.remove(id);
				}

				@Override
				public void onError(AsyncEvent event) throws IOException {
					//System.out.println("listener error: " + id);
					asyncContexts.remove(id);
				}

				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
					// Do nothing
				}

				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					//System.out.println("listener time out, remove: " + id);
					asyncContexts.remove(id);
				}
			});

			// Put context in a map
			asyncContexts.put(id, ac);
			System.out.println("bind: " + ac.toString() + " to topic" + topic );
			AsyncContextToTopicMap.put(ac, topic);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("do post: " + request.getHeader("user"));
		// Sets char encoding - should not be done here, better in filter
		request.setCharacterEncoding("UTF-8");

		// Gets message from request
		String message = request.getParameter("msg");
		String topic = request.getHeader("topic");
		if(topic == null || topic.trim().isEmpty()){
			topic = "default";
		}
		else{
			System.out.println("get new topic: " + topic);
			System.out.println();
		}

		// Do some verification and save message into DB, etc.
		if ((message != null) && !message.trim().isEmpty()) {
			try {
//				if (message.equals("#clear")) {
//					messageQueue.clear();
//					messageStore.clear();
//					return;
//				}
				Message msg = new Message(counter.incrementAndGet(), message.trim(), topic, new Date());
				// Put message into messageQueue
				messageQueue.put(msg);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	}

//	private void sendMessage(PrintWriter writer, Message message) {
//		writer.print("id: ");
//		writer.println(message.getId());
//		writer.print("data: ");
//		writer.println(message.getMessage());
//		writer.println();
//		writer.flush();
//	}
	
	private void sendMessage(PrintWriter writer, Message message) {
		writer.println("data: {");
		writer.println("data: \"id\": "+ message.getId()+",");
		writer.println("data: \"topic\": \"" + message.getTopic()+"\",");
		writer.println("data: \"msg\": \"" + message.getMessage()+"\"");
		writer.println("data: }");
		writer.println();
		writer.flush();
	}

}
