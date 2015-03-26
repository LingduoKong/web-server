import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;


/**
 * Created by lingduokong on 1/24/15.
 */
public class WebServer {
    /*
    get the port number from command argument
    author: Junchi Liang
     */
        
    public static void main(String[] args) throws IOException {
		
        int i, http_port = -1, ssl_port = -1;
        String http_port_pattern_str = "--serverPort=[0-9]+";
        String ssl_port_pattern_str = "--sslServerPort=[0-9]+";
        Pattern http_port_pattern = Pattern.compile(http_port_pattern_str);
        Pattern ssl_port_pattern = Pattern.compile(ssl_port_pattern_str);
			
        for (i = 0; i < args.length; i++)
        {
            Matcher port_matcher = http_port_pattern.matcher(args[i]);
            if (port_matcher.find())
            {
                try
                {
                    http_port = Integer.parseInt(args[i].substring(13));
                } catch (NumberFormatException e)
                {
                    break;
                }
            }
        }

        for (i = 0; i < args.length; i++)
        {
            Matcher port_matcher = ssl_port_pattern.matcher(args[i]);
            if (port_matcher.find())
            {
                try
                {
                    ssl_port = Integer.parseInt(args[i].substring(16));
                } catch (NumberFormatException e)
                {
                    break;
                }
            }
        }

	System.out.println("http port:" + http_port + " SSL port:" + ssl_port);
        
        if (http_port < 0 && ssl_port < 0) {
            System.out.println("please use --serverPort option or --sslServerPort option (or both)");
            return;
        }
        
        if (http_port >= 0) {
            MySingleServer http_server = new MySingleServer(http_port, "http");
            System.out.println("run a HTTP server");
            http_server.start();
        }
        
        if (ssl_port >= 0) {
            MySingleServer ssl_server = new MySingleServer(ssl_port, "ssl");
            System.out.println("run a HTTPS server");
            ssl_server.start();
        }
        
        while (true) {
            String user_input;
            Scanner scanner = new Scanner(System.in);
            user_input = scanner.next();
            if (user_input.compareTo("exit") == 0)
                break;
        }

        System.out.println("server end");

    }


}


