import java.util.ArrayList;

/**
 * Created by lingduokong on 1/24/15.
 */
public class RequestMessage {

    public String
            method,
            URL,
            version,
            entity_body;
    public ArrayList<HeaderLine> headerLines = new ArrayList<HeaderLine>();
}
