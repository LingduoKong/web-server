import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by lingduokong on 2/23/15.
 */
public class MySingleServer extends Thread{

    int port;
    String server_type;

    public MySingleServer(int port, String server_type){
        this.port = port;
        this.server_type = server_type;
    }
    
    @Override
    public void run() {
        if (server_type.compareTo("http") == 0) { // http
            ServerSocket server_socket = null;
            try {
                server_socket = new ServerSocket(port);
            } catch (IOException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            while (true) {
                try {
                    Socket connection_socket = server_socket.accept();
		            System.out.println("HTTP server get a new connection");
                    MyServerService my_server_service = new MyServerService(connection_socket);
                    my_server_service.start();
                }catch (IOException ex) {
                    Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        else { // https

	  
            char[] our_pw = "nptwopw".toCharArray();
            String key_store_file = "server.jks";
            try {
                SSLContext ssl_context;
                ssl_context = SSLContext.getInstance("TLS");
                KeyStore key_store = KeyStore.getInstance("JKS");

                FileInputStream key_file_input = new FileInputStream(key_store_file);
                key_store.load(key_file_input, our_pw);
                KeyManagerFactory key_manager_factory = KeyManagerFactory.getInstance("SunX509");
                key_manager_factory.init(key_store, our_pw);
                TrustManagerFactory trust_manager_factory = TrustManagerFactory.getInstance("SunX509");
                trust_manager_factory.init(key_store);

                ssl_context.init(key_manager_factory.getKeyManagers(), null, null);
                SSLServerSocketFactory socket_factory = ssl_context.getServerSocketFactory();
                SSLServerSocket server_socket = (SSLServerSocket) socket_factory.createServerSocket(port);

                while (true) {

                    Socket connection_socket = server_socket.accept();
    		        System.out.println("HTTPS server get a new connection");
                    MyServerService my_server_service = new MyServerService(connection_socket);
                    my_server_service.start();
                }

            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyStoreException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (CertificateException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnrecoverableKeyException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyManagementException ex) {
                Logger.getLogger(MySingleServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }

}
