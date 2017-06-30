package chat;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class Score {
	private String user;
	private int score;
	public Score(String user, int value) {
		this.user = user;
		this.score = value;
	}
	
	
}
