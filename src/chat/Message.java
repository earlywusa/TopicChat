package chat;

import java.util.Date;

public class Message {

	private Date time;
	private long id;
	private String message;
	private String topic = "default";


	public Message(long id, String message, String topic,  Date time) {
		super();
		this.id = id;
		this.message = message;
		this.time = time;
		this.topic = topic;
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

}
