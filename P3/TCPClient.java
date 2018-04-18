import java.io.*;
import java.net.*;
import java.util.Scanner;
 
// Client class
public class TCPClient 
{
    public static void main(String[] args) throws IOException 
    {
        try
        {
            Scanner scn = new Scanner(System.in);
             
            // getting localhost ip
            InetAddress ip = InetAddress.getByName("localhost");
     
            // establish the connection with server port 5056
            Socket s = new Socket(ip, 6789);
     
            // obtaining input and out streams
            BufferedReader dis =
			new BufferedReader(new InputStreamReader(s.getInputStream()));
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
     
            // the following loop performs the exchange of
            // information between client and client handler
            while (true) 
            {
                System.out.println(dis.readLine());
                String tosend = scn.nextLine();
                dos.writeBytes(tosend+"\n");
                 
                // If client sends exit,close this connection 
                // and then break from the while loop
                String received = dis.readLine();
                if(tosend.equals("Exit"))
                {
                    System.out.println("Closing this connection : " + s);
                    s.close();
                    System.out.println("Connection closed");
                    break;
                }
                 
                // printing date or time as requested by client
                
                System.out.println(received);
            }
             
            // closing resources
            scn.close();
            dis.close();
            dos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}