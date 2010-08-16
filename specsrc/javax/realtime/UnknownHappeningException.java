package javax.realtime;

import java.io.Serializable;

public class UnknownHappeningException extends RuntimeException implements
		Serializable {

  public UnknownHappeningException() {
  }
  
  public UnknownHappeningException(String description) {
    super(description);
  }
}
