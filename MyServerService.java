import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lingduokong on 2/23/15.
 */
public class MyServerService extends Thread {

    private static final String def_file_path = "www/redirect.defs";
    private BufferedReader in;
    private OutputStream socket_output_stream;

    private long timestamp;
    private boolean isStartTimer;
    private boolean shouldStop;

    private Socket origin_socket;

    public MyServerService(Socket origin_socket){
        isStartTimer =false;
        shouldStop = false;
        this.origin_socket = origin_socket;
        try {
            in = new BufferedReader(new InputStreamReader(this.origin_socket.getInputStream()));
            socket_output_stream = this.origin_socket.getOutputStream();
        }catch (IOException e){

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.origin_socket.setKeepAlive(true);
                RequestMessage request_message = receive_message();
                if (request_message == null) {
                    if(isStartTimer){
                        long currentTime = System.currentTimeMillis()/1000;
                        if(Math.abs(currentTime-timestamp)>5){
                            System.out.println("Time out! Persistent socket is close");
                            this.in.close();
                            this.socket_output_stream.close();
                            this.origin_socket.close();
                            break;
                        }
                    }
                    else{
                        isStartTimer = true;
                        timestamp = System.currentTimeMillis()/1000;
                    }
                    continue;
                }
                else {
                    isStartTimer=false;
                }
                if (response_to_client(request_message)!= 0) {
                    origin_socket.close();
                    System.out.println("response_to_client function error");
                    break;
                }
                if(shouldStop){
                    System.out.println("Get a request to close socket");
                    this.in.close();
                    this.socket_output_stream.close();
                    this.origin_socket.close();
                    break;
                }
            }
            catch (SocketException e){
                if(e.getMessage().compareTo("Socket Closed")==0){
                    break;
                }
                // System.out.println(e.getMessage());
                // e.printStackTrace();
                break;
            }catch (IOException e) {
                break;
            }
        }

    }

    private RequestMessage receive_message() throws IOException{

        RequestMessage rm = new RequestMessage();
        String request_line = in.readLine();
        if(request_line==null) {
            return null;
        }
        else {
            System.out.println(request_line);
        }
        String[] str = request_line.split(" ");
        rm.method=str[0];
        rm.URL=str[1];
        rm.version=str[2];

        String inputLine;
        HeaderLine hl;
        while ((inputLine = in.readLine()).compareTo("")!=0) {
            hl = new HeaderLine(inputLine);
            if(hl.field_name.equalsIgnoreCase("CONNECTION") && hl.value.equalsIgnoreCase("CLOSE")){
                shouldStop = true;
            }
            rm.headerLines.add(hl);
        }
        return rm;
    }

    private int response_to_client(RequestMessage request_message) throws IOException {
        ResponseMessage response_message = generate_response_message(request_message);
        String cr = "\r";
        String lf = "\n";
        String output_line;

        output_line = response_message.version + " " + response_message.status_code + " " + response_message.phrase + cr + lf;
        socket_output_stream.write(output_line.getBytes(), 0, output_line.length());
        for (HeaderLine header: response_message.headerLines) {
            output_line = header.field_name + " " + header.value + cr + lf;
            socket_output_stream.write(output_line.getBytes(), 0, output_line.length());
        }

        output_line = cr + lf;
        socket_output_stream.write(output_line.getBytes(), 0, output_line.length());
        socket_output_stream.flush();

        if (response_message.status_code.equals("200") && request_message.method.equals("GET")) {
            // debug
            System.out.println("the local path for requested object is:" + response_message.entity_path);

            BufferedInputStream input_stream = new BufferedInputStream(new FileInputStream(new File(response_message.entity_path)));
            byte[] file_buff = new byte[1024];
            int read_size;
            while ((read_size = input_stream.read(file_buff, 0, file_buff.length)) > 0) {
                socket_output_stream.write(file_buff, 0, read_size);
                socket_output_stream.flush();
            }
            input_stream.close();
        }

        return 0;

    }

    /*
    generate response message (without Entity body, set only entity path) according to request message
    argument:
    RequestMessage request_message // the request message
    return:
    an object of ResponseMessage, the result message, return NULL when request_message does not strictly follows protocol
    author: Junchi Liang
    */
    private ResponseMessage generate_response_message(RequestMessage request_message) {

        ResponseMessage response_message = new ResponseMessage();
        String version_pattern_str = "HTTP/[0-9]+.[0-9]+";
        Pattern version_pattern = Pattern.compile(version_pattern_str);
        Matcher version_pattern_matcher = version_pattern.matcher(request_message.version);


        response_message.version = "HTTP/1.1";
        if (!version_pattern_matcher.find()) { // error in version and return 400 Bad Request
            HeaderLine header_content_length = new HeaderLine("Content-Length: 0");

            response_message.status_code = "400";
            response_message.phrase = "Bad Request";
            response_message.headerLines.add(header_content_length);
            response_message.entity_path = "";
            return response_message;
        }
        else if (request_message.method.equals("GET") || request_message.method.equals("HEAD")) { // a GET or HEAD request

            String requested_URL = local_path(request_message.URL);
            String redirected_path = check_redirect(request_message.URL, def_file_path);


            if (!requested_URL.equals(def_file_path) && check_file_exist(requested_URL)) { // 200 OK

                HeaderLine header_content_type = new HeaderLine("Content-Type: " + get_content_type(requested_URL));
                long file_size = get_file_size(requested_URL);

                HeaderLine header_content_length = new HeaderLine("Content-Length: " + Long.toString(file_size));
                HeaderLine header_connection;
                header_connection = new HeaderLine("Connection: " + "Keep-Alive");
//
//                if(this.shouldStop) {
//                    header_connection = new HeaderLine("Connection: " + "Keep-Alive");
//                }
//                else {
//                    header_connection = new HeaderLine("Connection: " + "Close");
//                }
                response_message.status_code = "200";
                response_message.phrase = "OK";
                response_message.headerLines.add(header_content_type);
                response_message.headerLines.add(header_content_length);
                response_message.headerLines.add(header_connection);
                if (request_message.method.equals("GET"))
                    response_message.entity_path = requested_URL;
                else
                    response_message.entity_path = "";
                return response_message;
            }
            else if (!requested_URL.equals(def_file_path) && redirected_path.length() > 0) { // 301 Moved Permanently
                HeaderLine header_location = new HeaderLine("Location: " + redirected_path);

                response_message.status_code = "301";
                response_message.phrase = "Moved Permanently";
                response_message.headerLines.add(header_location);

                if (request_message.method.equals("GET")) {
                    // ATTENTION: the protocol say we should return something in entity when GET, but I am not sure what it is
                    HeaderLine header_content_length = new HeaderLine("Content-Length: 0");
                    response_message.headerLines.add(header_content_length);
                }
                else {
                    HeaderLine header_content_length = new HeaderLine("Content-Length: 0");
                    response_message.headerLines.add(header_content_length);
                }
                return response_message;
            }
            else { // 404 Not Found
                HeaderLine header_content_length = new HeaderLine("Content-Length: 0");

                response_message.status_code = "404";
                response_message.phrase = "Not Found";
                response_message.headerLines.add(header_content_length);
                response_message.entity_path = "";
                return response_message;
            }
        }
        else { // unexpected request, return 403 Forbidden
            HeaderLine header_content_length = new HeaderLine("Content-Length: 0");

            response_message.status_code = "403";
            response_message.phrase = "Forbidden";
            response_message.headerLines.add(header_content_length);
            response_message.entity_path = "";
        }

        return response_message;
    }

    /*
    check whether an URL will be redirected
    argument:
    String requested_URL // the requested URL
    String def_file // the path for the file redirect.defs
    String redirected_path // the redirected path(if return false, here will be "")
    return:
    true if the file is redirected; false otherwise
    author: Junchi Liang
    */
    private static String check_redirect(String requested_URL, String def_file) {
        String redirected_path = "";
        File redirect_file = new File(def_file);
        if (redirect_file.exists()) {

            try {

                BufferedReader buffered_reader = new BufferedReader(new FileReader(def_file));
                String redirect_str = null;

                while ((redirect_str = buffered_reader.readLine()) != null)
                {
                    String[] redirect_line = redirect_str.split(" ");
                    if (requested_URL.equals(redirect_line[0])) {
                        redirected_path = redirect_line[1];
                    }
                }

                try {
                    buffered_reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                    return redirected_path;
                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                return redirected_path;
            } catch (IOException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                return redirected_path;
            }
            return redirected_path;
        }
        else {
            return redirected_path;
        }
    }

    /*
    check whether the given file exists
    argument:
    String file_path // the path for the checked file
    return:
    true if the file exists; false otherwise
    author: Junchi Liang
    */
    private static boolean check_file_exist(String file_path) {
        File checked_file = new File(file_path);
        if (checked_file.exists() && checked_file.isFile())
            return true;
        else
            return false;
    }

    /*
    get type of file for content-type
    argument:
    String file_path // the path of the file
    return:
    return a string as content-type:
    text/html
    text/plain
    application/pdf
    image/png
    image/jpeg
    author: Junchi Liang
    */
    private static String get_content_type(String file_path) {
        int i = file_path.length() - 1;
        String suffix = null;

        for (; i > 0; i--)
            if (file_path.charAt(i) == '.')
                break;

        suffix = file_path.substring(i + 1);

        if (suffix != null && (suffix.equals("html") || suffix.equals("hml"))) {
            return "text/html";
        }
        else if (suffix != null && suffix.equals("pdf")) {
            return "application/pdf";
        }
        else if (suffix != null && suffix.equals("png")) {
            return "image/png";
        }
        else if (suffix != null && (suffix.equals("jpeg") || suffix.endsWith("jpg"))) {
            return "image/jpeg";
        }
        else
            return "text/plain";
    }

    /*
    get size of file
    argument:
    String file_path // path for the checked file
    return:
    size of file (in byte)
    author: Junchi Liang
    */
    private static long get_file_size(String file_path) {
        File checked_file = new File(file_path);
        return checked_file.length();
    }

    /*
    get local file path
    argument:
    String URL // the URL in request message
    return:
    a string for the local path of this URL
    author: Junchi Liang
    */
    static private String local_path(String URL) {
        if (URL.equals("/") || URL.equals(""))
            return "www/index.html";
        else if (URL == null)
            return "";
        else if (URL.charAt(0) != '/')
            return "www/" + URL;
        else
            return "www" + URL;
    }

}
