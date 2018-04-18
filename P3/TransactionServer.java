import java.text.*;
import org.stellar.sdk.KeyPair;
import java.net.*;
import java.io.*;
import java.util.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.*;
import org.stellar.sdk.responses.operations.*;
import org.stellar.sdk.requests.*;
import java.security.*;
import org.apache.commons.codec.digest.DigestUtils;


class TransactionServer {
	public static void main(String argv[]) throws Exception {
		ServerSocket ss = new ServerSocket(6789);
		// StateHandle sh = new StateHandle();
		Server server = new Server("https://horizon-testnet.stellar.org");//use the stellar test server for demo
		Network.useTestNetwork();
		KeyPair serverAccount =  KeyPair.fromAccountId("GDDJZEYBGGSCKEYCS6S24TJTXONKAPAMQLBUCK7AVQN6L7YU736NCGQU");
		KeyPair serverAccount_private = KeyPair.fromSecretSeed("SBS72QGLQO4YMLNDNBDX3WH3TP5424XDZ7KBDBUX3ACIFP4VS3UFMVTD");
		try{
			server.accounts().account(serverAccount);
		}catch(IOException ex){
			System.out.println("Fail to log into server Stellar account. Exit.");
			return;
		}
		System.out.println("Server start. Waiting for user join the game...");
		Thread sh = new StateHandler();
		sh.start();
		// Keep adding new client handler when new client joint in the game
		while (true) {
			Socket s = ss.accept();
			String socketAddress = s.getRemoteSocketAddress().toString();
			System.out.println("New clinent ("+socketAddress+") join in.");		
			DataOutputStream outToClient = new DataOutputStream(s.getOutputStream());
			Thread t = new ClientHandler(server, s, outToClient, serverAccount, serverAccount_private);			
			t.start();
			
		}
	}
}


// supervisor of all the client
class StateHandler extends Thread{
	@Override
	public void run() 
	{
		while(true){
			// use two lock steps to control the state of client
			if(ClientHandler.startNewRound && ClientHandler.clientWaitforPlay == ClientHandler.totalClientHandler && ClientHandler.clientWaitforPlay>=1){
				ClientHandler.startNewRound = false;
				ClientHandler.startPlay = true;
				ClientHandler.clientWaitforNewRound = 0;
			}

			if(ClientHandler.startPlay && ClientHandler.clientWaitforNewRound == ClientHandler.totalClientHandler && ClientHandler.clientWaitforNewRound>=1){
				ClientHandler.startPlay = false;
				ClientHandler.startNewRound = true;
				ClientHandler.clientWaitforPlay = 0;
				ClientHandler.clientAlreadySendHash = 0;	
				System.out.println("<<<<<<<<<<Start a new round>>>>>>>>>>");			
			}
			try{
				Thread.sleep(2000); //supervise every 2 seconds
			}catch(InterruptedException e){
				System.out.println("Supervisor is down! The program has to exit.");
				System.exit(1);
			}
			
		}
		
	}


}


//ClientHandler class
class ClientHandler extends Thread {

	static int totalClientHandler = 0;
	static int clientAlreadySendHash = 0;
	public static boolean startNewRound = true;
	public static boolean startPlay = false;
	public static int clientWaitforPlay = 0;
	public static int clientWaitforNewRound= 0;
	static int totalSeed = 0;
	static float totalDepositGuess0 = 0;
	static float totalDepositGuess1 = 0;
	BufferedReader dis;
	int choice;
	final DataOutputStream dos;
	final Socket s;
	final String address;
	State state;
	Server server;
	AccountResponse sourceAccount;
	float deposit;
	KeyPair source;
	KeyPair source_pub;
	KeyPair serverAccount;
	KeyPair serverAccount_private;
	String encryptedString;
	String guess = null, seed = null;
	public enum State{
		stellar_log_in, take_deposit, receive_hash, wait_user_guess, receive_guess_info, start_game, wait_start_game, wait_new_round;
	}

	// Constructor
	public ClientHandler(Server server, Socket s, DataOutputStream dos, KeyPair serverAccount, KeyPair serverAccount_private) 
	{
		address = s.getRemoteSocketAddress().toString();
		System.out.println(address+" log into the server.");
		this.server = server;
		choice = -1;
		this.s = s;
		this.dos = dos;
		this.serverAccount = serverAccount;
		this.serverAccount_private = serverAccount_private;
		state = State.stellar_log_in;
		totalClientHandler ++;
	}


	public boolean stellarLogin(){
		while(true){
			String received = null;
			try{
				received = dis.readLine();	
			}catch(Exception e){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
			try{
				source = KeyPair.fromSecretSeed(received);
				sourceAccount = server.accounts().account(source);
				dos.writeBytes("T"+"\n");
			}catch(Exception e){
				try{
					dos.writeBytes("F"+"\n");//log in incorrect
				}catch(Exception ee){
					System.out.println("Client: "+address+" connection Lost.");
					return false;
				}
				System.out.println("Client: "+address+" log in error. Wait for another try...");
				continue;
			}


			try{
				received = dis.readLine();	
			}catch(Exception e){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
			try{
				source_pub = KeyPair.fromAccountId(received);
				try{
					server.accounts().account(source_pub);
				}catch(IOException ex){
					System.out.println("Client public ID incorrect. Wait for another try...");
					continue;
				}
				dos.writeBytes("T"+"\n");
				return true;
			}catch(Exception e){
				try{
					dos.writeBytes("F"+"\n");//log in incorrect
				}catch(Exception ee){
					System.out.println("Client: "+address+" connection Lost.");
					return false;
				}
				System.out.println("Client: "+address+" log in error. Wait for another try...");
			}
		}
	}


	public boolean getDeposit(){
		while(true){
			String received = null;
			try{
				received = dis.readLine();	

			}catch(Exception e){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
			try{
				deposit = Float.valueOf(received);//check whether the input is a number
				try{
					Transaction transaction = new Transaction.Builder(sourceAccount)
						.addOperation(new PaymentOperation.Builder(serverAccount, new AssetTypeNative(), received).build())
						// A memo allows you to add your own metadata to a transaction. It's
						// optional and does not affect how Stellar treats the transaction.
						.addMemo(Memo.text("Test Transaction"))
						.build();

					// Sign the transaction to prove you are actually the person sending it.
					transaction.sign(source);
					SubmitTransactionResponse response = server.submitTransaction(transaction);
					System.out.println("Success! "+address+" deposit: "+received+". Stellar Response: "+response);
					dos.writeBytes("T"+"\n");
					return true;

				}catch(IOException ex){
					System.out.println("Cannot find source account.");
					return false;
				}catch(Exception e){
					System.out.println(e);
					try{
					dos.writeBytes("F"+"\n");//input format incorrect
					}catch(Exception ee){
						System.out.println("Client: "+address+" connection Lost.");
						return false;
					}
					System.out.println("Client: "+address+" error. Wait for another try...");
				}
			}catch(Exception e){
				try{
					dos.writeBytes("F"+"\n");//input format incorrect
				}catch(Exception ee){
					System.out.println("Client: "+address+" connection Lost.");
					return false;
				}
				System.out.println("Client: "+address+" deposit error. Wait for another try...");
			}
		}
	}

	public boolean receiveHash(){
		while(true){
			String received = null;
			try{
				received = dis.readLine();	
			}catch(Exception e){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
			try{
				encryptedString = received;
				dos.writeBytes("T"+"\n");
				clientAlreadySendHash ++;
				return true;
			}catch(Exception e){
				try{
					dos.writeBytes("F"+"\n");//log in incorrect
				}catch(Exception ee){
					System.out.println("Client: "+address+" connection Lost.");
					return false;
				}
				System.out.println("Client: "+address+" hash process error. Wait for another try...");
			}
		}
	}

	public boolean checkReadyUser(){
		while(true){
			try{
				if(clientAlreadySendHash == totalClientHandler && clientAlreadySendHash>=1){
					try{				
						dos.writeBytes("ALLREADY"+"\n");
						return true;
					}catch(Exception e){}
				}
				Thread.sleep(1000); // Check ready user every 1 second.
			} 
			catch(InterruptedException ex){
				Thread.currentThread().interrupt();
			}
		}
		
	}

	public boolean receiveGuess(){
		MessageDigest messageDigest;
		while(true){
			String received = null;
			try{
				received = dis.readLine();
			}catch(Exception e){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
			String split[] = received.split("\\s+");
			guess = split[0];
			seed = split[1];
			try{
				try{messageDigest = MessageDigest.getInstance("SHA-256");}
				catch(NoSuchAlgorithmException nae){continue;}
				String encryptedString_2nd = new DigestUtils(messageDigest).digestAsHex(guess+seed);
				if(encryptedString_2nd.equals(encryptedString)){
					System.out.println("Client: "+address+" Guess and Seed match with hashing.");
					dos.writeBytes("T"+"\n");
					totalSeed += Integer.valueOf(seed);
					if(guess.equals("0")) totalDepositGuess0+=deposit;
					else totalDepositGuess1+=deposit;
					clientWaitforPlay ++;
					return true;
				}else{
					System.out.println("Client: "+address+" unmatch with hashing! Kick him out!");
					clientAlreadySendHash--;
					return false;
				}
			}catch(Exception e){
				try{
					dos.writeBytes("F"+"\n");//log in incorrect
				}catch(Exception ee){
					System.out.println("Client: "+address+" connection Lost.");
					return false;
				}
				System.out.println("Client: "+address+" hash process error. Wait for another try...");
			}
		}
	}

	public boolean playGame(){
		float totalDeposit = totalDepositGuess0+totalDepositGuess1;
		float prizePool = totalDeposit*0.95f;
		int rem = totalSeed%2;
		float totalDepositRightGuess = (rem==0?totalDepositGuess0:totalDepositGuess1);
		if(rem == Integer.valueOf(guess)){// win the bet
			float bouns = prizePool*(deposit/totalDepositRightGuess);
			
///////////////////////////////////////////send back the bouns to user account.
			try{
				AccountResponse serverAccount_private_res = server.accounts().account(serverAccount_private);
				Transaction transaction = new Transaction.Builder(serverAccount_private_res)
					.addOperation(new PaymentOperation.Builder(source_pub, new AssetTypeNative(), String.valueOf(bouns)).build())
					// A memo allows you to add your own metadata to a transaction. It's
					// optional and does not affect how Stellar treats the transaction.
					.addMemo(Memo.text("Test Transaction"))
					.build();

				// Sign the transaction to prove you are actually the person sending it.
				transaction.sign(serverAccount_private);
				SubmitTransactionResponse response = server.submitTransaction(transaction);
				System.out.println("Success! Send money: "+String.valueOf(bouns)+" to "+address+". Stellar Response: "+response);
			}catch(IOException ex){
				System.out.println("Cannot find source account.");
				return false;
			}catch(Exception e){
				System.out.println(e);
				System.out.println("Client: "+address+" error. Wait for another try...");
				return false;
			}
////////////////////////////////////////////
			try{
				dos.writeBytes(String.valueOf(bouns)+"\n");//log in incorrect
			}catch(Exception ee){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}

		}else{
			try{
				dos.writeBytes("F"+"\n");//log in incorrect
			}catch(Exception ee){
				System.out.println("Client: "+address+" connection Lost.");
				return false;
			}
		}
		return true;
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
					
				switch(state){
					case stellar_log_in:
						if(stellarLogin()==true) state = State.take_deposit;
						else{							
							totalClientHandler --;
							return;
						}
						break;
					case take_deposit:
						if(getDeposit()==true) state = State.receive_hash;
						else{
							totalClientHandler --;
							return;
						}
						break;
					case receive_hash:
						if(receiveHash()==true) state = State.wait_user_guess;
						else{							
							totalClientHandler --;
							return;
						}
					case wait_user_guess:
						if(checkReadyUser()==true) state = State.receive_guess_info;
						else{
							totalClientHandler --;
							return;
						}
						break;
					case receive_guess_info:
						if(receiveGuess()==true){
							state = State.wait_start_game;
						}
						else{							
							totalClientHandler --;
							return;
						}
						break;
					case wait_start_game:
						if(startPlay) state = State.start_game;		
						break;				
					case start_game:
						if(playGame()==true){
							clientWaitforNewRound++;
							System.out.println(clientWaitforNewRound);					
							state = State.wait_new_round;
						}
						else{					
							totalClientHandler --;
							return;
						}
						break;
					case wait_new_round:
						if(startNewRound){
							
							state = State.take_deposit;
						}
					default:
						break;
				}

			} catch (Exception e) {
				System.out.println("Exception: possibly lost connection from: "+address+".");				
				totalClientHandler --;
				return;
			}
			try{
				Thread.sleep(1000); 
			}catch(InterruptedException e){
				System.out.println("Thread error.");
				totalClientHandler--;
			}
		}
		 
		// try
		// {
		// 	dis.close();
		// 	dos.close();		
		// 	totalClientHandler --;
		// 	return;

		// }catch(IOException e){
		// 	e.printStackTrace();		
		// 	totalClientHandler --;
		// 	return;
		// }
	}
}