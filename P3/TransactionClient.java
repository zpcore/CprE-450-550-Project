import java.io.*;
import java.net.*;
import org.stellar.sdk.KeyPair;
import java.util.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.*;
import java.security.*;
import org.apache.commons.codec.digest.DigestUtils;

enum State{
	stellar_log_in, send_deposit, send_encrypted_guess, wait_for_other, send_guess, wait_for_result, exit_or_again;
}

public class TransactionClient {
	static String guess, seed;

	public static void stellarLogin(BufferedReader dis, DataOutputStream dos){
		BufferedReader br;
		String idString;
		while(true){
			System.out.println("Type in your Stellar private key ID: ");
			br = new BufferedReader(new InputStreamReader(System.in));
			try{
				idString = br.readLine();
				dos.writeBytes(idString+"\n");
				String received = dis.readLine();
				if(!received.equals("T")){
					System.out.println("Incorrect private key ID, try again...");
					continue;
				}
			}catch(Exception e){}

			System.out.println("Type in your Stellar public key ID: ");
			br = new BufferedReader(new InputStreamReader(System.in));
			try{
				idString = br.readLine();
				dos.writeBytes(idString+"\n");
				String received = dis.readLine();
				if(received.equals("T")) return;
			}catch(Exception e){}	
			System.out.println("Stellar log in error. Please try again...");
		}		
	}

	public static void sendDeposit(BufferedReader dis, DataOutputStream dos){
		while(true){
			System.out.println("How much you want to bet: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try{
				String idString = br.readLine();
				dos.writeBytes(idString+"\n");
				System.out.println("Waiting for server response...");
				String received = dis.readLine();
				if(received.equals("T")){
					System.out.println("Success! Deposit reveived by server.");
					return;
				}
			}catch(Exception e){}			
			System.out.println("Action error. Please try again...");
		}		
	}

	public static void sendEncryptedGuess(BufferedReader dis, DataOutputStream dos){
		BufferedReader br;
		int i = 0, j = 0;
		MessageDigest messageDigest;
		while(true){
			System.out.println("Type guessing number 0 or 1:");
			br = new BufferedReader(new InputStreamReader(System.in));
			try{guess = br.readLine();}
			catch(IOException e){
				System.out.println("IOException. Please try again...");
				continue;
			}
			if(!guess.equals("0")&&!guess.equals("1")){
				System.out.println("Incorrect guess. Please try again...");
				continue;
			}
			System.out.println("Privide a random seed number:");
			br = new BufferedReader(new InputStreamReader(System.in));
			try{seed = br.readLine();}
			catch(IOException e){
				System.out.println("IOException. Please try again...");
				continue;
			}
			try{
				j = Integer.valueOf(seed);
			}catch(Exception e){
				System.out.println("Input should be a number, Please try again...");
				continue;
			}
			
			String stringToEncrypt = guess+seed;
			try{messageDigest = MessageDigest.getInstance("SHA-256");}
			catch(NoSuchAlgorithmException nae){
				continue;
			}
			String encryptedString = new DigestUtils(messageDigest).digestAsHex(guess+seed);
			System.out.println("Hashing: "+encryptedString);
			try{
				dos.writeBytes(encryptedString+"\n");
				String received = dis.readLine();
				if(received.equals("T")){
					System.out.println("Encrypted guess and seed reveived by server.");
					return;
				}
			}catch(Exception e){}			
			System.out.println("Send error. Please try again...");
		}	
	}

	public static void waitAllReady(BufferedReader dis){
		try{
			String received = dis.readLine();
			if(received.equals("ALLREADY")){
				System.out.println("All participants have sent their guess hash.");
				return;
			}
		}catch(Exception e){}
		System.out.println("Server is down!!! The server swallowed your deposit!!!");
		System.exit(1);
	}

	public static void sendGuess(BufferedReader dis, DataOutputStream dos){
		while(true){
			System.out.println("Automatically sending guess and seed number for verification...");
			try{
				dos.writeBytes(guess+" "+seed+"\n");
				String received = dis.readLine();
				if(received.equals("T")){
					System.out.println("Success! String hash matches record.");
					return;
				}
			}catch(Exception e){}			
			System.out.println("Error. Please try again...");
		}		
	}

	public static void waitResult(BufferedReader dis){
		while(true){
			System.out.println("Waiting for bet result...");
			try{
				String received = dis.readLine();
				if(received.equals("F")){
					System.out.println("You lose!");
					return;
				}else{//
					System.out.println("You Win! Prize "+received+" has been sent to your Stellar account.");
					return;
				}
			}catch(Exception e){}			
			System.out.println("Error. Please try again...");
			System.exit(1);
		}		
	}


	public static void main(String[] args) throws IOException {
		State state = State.stellar_log_in;
		try
		{
			InetAddress ip = InetAddress.getByName("localhost");
			Socket s = new Socket(ip, 6789);
			// obtaining input and out streams
			BufferedReader dis = new BufferedReader(new InputStreamReader(s.getInputStream()));
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			
			while (true) 
			{

				switch(state){
					case stellar_log_in:
						stellarLogin(dis,dos);
						state = State.send_deposit;
						break;
					case send_deposit:
						sendDeposit(dis,dos);
						state = State.send_encrypted_guess;
						break;
					case send_encrypted_guess:
						sendEncryptedGuess(dis,dos);
						state = State.wait_for_other;
						break;
					case wait_for_other:
						waitAllReady(dis);
						state = State.send_guess;
						break;
					case send_guess:
						sendGuess(dis,dos);
						state = State.wait_for_result;
						break;
					case wait_for_result:
						waitResult(dis);
						System.out.println("Start a new round.");
						state = State.send_deposit;
						break;
					case exit_or_again:
						break;
					default:
						break;
				}
			}
			 
			// closing resources
			// dis.close();
			// dos.close();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
	}
}