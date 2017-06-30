package chat;

import java.util.Date;

public class Message {

	private Date time;
	private long id;
	private String message;
	private String topic = "default";
	private String user = "anonymous";


	public Message(long id, String message, String topic, String user, Date time) {
		super();
		this.id = id;
		this.message = message;
		this.time = time;
		this.topic = topic;
		this.user = user;
	}


	public long getId() {
		return id;
	}


	public String getMessage() {
		return message;
	}
	
	public Date getTime(){
		return time;
	}
	
	public String getTopic(){
		return topic;
	}
	
	public String getUser(){
		return user;
	}

}
