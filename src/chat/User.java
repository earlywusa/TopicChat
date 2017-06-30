package chat;

import javax.servlet.AsyncContext;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class User {
	private String id;
	private AsyncContext context;
	private int score;
	private String topic;
	
	public User(String id, AsyncContext context, String topic) {
		this.id = id;
		this.context = context;
		this.topic = topic;
	}
}
