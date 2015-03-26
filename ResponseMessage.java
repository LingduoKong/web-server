import java.util.ArrayList;

/**
 * Created by lingduokong on 1/24/15.
 */
public class ResponseMessage {

    public String version, status_code, phrase;
    public ArrayList<HeaderLine> headerLines = new ArrayList<HeaderLine>();
    public String entity_path;

}


