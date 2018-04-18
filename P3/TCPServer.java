import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

class TCPServer {
	public static void main(String argv[]) throws Exception {
		ServerSocket ss = new ServerSocket(6789);
		while (true) {
			Socket s = ss.accept();
			System.out.println("Connect to Client: "+s.getRemoteSocketAddress().toString());		
			DataOutputStream outToClient = new DataOutputStream(s.getOutputStream());
		  	Thread t = new ClientHandler(s, outToClient);
            t.start();
		}
	}
}

//ClientHandler class
class ClientHandler extends Thread 
{
	
    DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
    DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
    BufferedReader dis;
    final DataOutputStream dos;
    final Socket s;
     
    // Constructor
    public ClientHandler(Socket s, DataOutputStream dos) 
    {

        this.s = s;
        this.dos = dos;
		
    }
 
    @Override
    public void run() 
    {
        String received;
        String toreturn;
        try{
        	dis = new BufferedReader(new InputStreamReader(s.getInputStream()));   
        }catch(IOException e){

        }
        while (true)
        {
            try {
 				
                // Ask user what he wants
                dos.writeBytes("What do you want?[Date | Time].."+
                            "Type Exit to terminate connection."+"\n");
                 
                // receive the answer from client   
                System.out.println("WWWWW");       
                received = dis.readLine();
                if(received.equals("Exit"))
                { 
                    System.out.println("Client " + this.s + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.s.close();
                    System.out.println("Connection closed");
                    break;
                }
                 
                // creating Date object
                Date date = new Date();
                 
                // write on output stream based on the
                // answer from the client
                switch (received) {
                 
                    case "Date" :
                        toreturn = fordate.format(date);
                        System.out.println(">>>>>"+toreturn);
                        dos.writeBytes(toreturn+"\n");
                        break;
                         
                    case "Time" :
                        toreturn = fortime.format(date);
                        System.out.println(">>>>>:::"+toreturn);
                        dos.writeBytes(toreturn+"\n");
                        break;
                         
                    default:
                        dos.writeBytes("Invalid input\n");
                        break;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("connection lose");
            }
        }
         
        try
        {
            // closing resources
            dis.close();
            dos.close();
             
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}