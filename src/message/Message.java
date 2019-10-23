package message;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Message implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private String message;
		private int number;
		private Set<Integer> shifts;
		public String time;
		
		public Message(String m, int n) {
			message = m;
			number = n;
			Date d = new Date();
			this.time = d.toString();
			String[] timeArgs = this.time.split(" ");
			this.time = timeArgs[1] + " " + timeArgs[2] + " " + timeArgs[3];
		}
		
		public String getMessage() {
			return time + " " + message;
		}
		
		public int getNumber() {
			return number;
		}
		
		public String toString() {
			return "Message: [" + message + "] Number: [" + number + "]";
		}
		
		public void cipher() {
			this.shifts = new HashSet<Integer>();
			char[] newMessage = new char[this.message.length()];
			for(int i = 0; i < this.message.length(); i++) {
				Character c = this.message.charAt(i);
				if((c > 64 && c < 91) || (c > 96 && c < 123)) {
					newMessage[i] = (char) (c + 2);
					this.shifts.add(i);
				} else {
					newMessage[i] = c;
				}
			}
			this.message = new String(newMessage);
		}
		
		public void reverseCipher() {
			StringBuilder realMessage = new StringBuilder(this.message);
			for(Integer curr : this.shifts) {
				realMessage.setCharAt(curr, (char) (realMessage.charAt(curr) - 2));
			}
			this.message = realMessage.toString();
		}
	}