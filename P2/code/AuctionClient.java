import AuctionApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;

import java.io.*;
import java.util.concurrent.TimeUnit;

enum status{
	logOrRegister, checkPassword, setName, setPassword, chooseSeller, selling, bidding, godView, waitNewRound, finish;
}

public class AuctionClient
{
	static Auction auctionImpl;
	static status bidStatus;
	public static void main(String args[])
	{
		try{
		// create and initialize the ORB
			ORB orb = ORB.init(args, null);
			// get the root naming context
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			// Use NamingContextExt instead of NamingContext. This is
			// part of the Interoperable naming Service.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			String name = "Auction";
			auctionImpl = AuctionHelper.narrow(ncRef.resolve_str(name));
			System.out.println("Obtained a handle on server object: " + auctionImpl);
			AuctionProcess(auctionImpl);
		} 
		catch (Exception e) {
			System.out.println("ERROR : " + e) ;
			e.printStackTrace(System.out);
		}
	}


	public static int getNumInput()throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String s = br.readLine();
		int choice = -1;
		try{
			choice = Integer.valueOf(s);
		}catch(NumberFormatException e){
			System.out.println("Input is not a number. Try again.");
			return choice;
		}
		if(choice<0){
			System.out.println("Negative input. Try again.");
			choice = -1;
		}
		return choice;
	}

	public static String getStringInput() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String s = br.readLine();
		return s;
	}

	public static void AuctionProcess(Auction auc)throws IOException{
		bidStatus = status.logOrRegister;
		String usr = null;//use this as authorization of which client
		BufferedReader br;
		String s = "";
		String password;
		int choice = -1;
		int price = 0;
		int amount;
		while(bidStatus!=status.finish){
			switch(bidStatus){
				case logOrRegister:
					System.out.println("Choose one operation:");
					System.out.println(
						"[1]: Log in\n"+
						"[2]: Register new Account"
						);
					choice = getNumInput();
					if(choice == 1) bidStatus = status.checkPassword;
					else if(choice == 2) bidStatus = status.setName;
					break;
				case checkPassword:
					System.out.println("UserName:");
					String username = getStringInput();
					System.out.println("Password:");
					password = getStringInput();
					if(auc.checkAccount(username,password)){
						usr = username;
						if(auc.hasSeller()) bidStatus = status.bidding;
						else bidStatus = status.chooseSeller;
					}
					else{
						System.out.println("Login error due to: 1. User already online 2. UserName not exists 3. password Incorrect.");
						bidStatus = status.logOrRegister;
					}
					break;
				case setName:
					System.out.println("Type your nickname:");
					s = getStringInput();
					if(s.length()<3){
						System.out.println("UserName should be more than 2 characters. Try again.");
						break;
					}
					boolean setName = auc.setUsrName(s);
					if(setName){
						usr = s;
						bidStatus = status.setPassword;
					}
					else System.out.println("Name already exists, choose another one.");
					break;
				case setPassword:
					System.out.println("Set your password:");
					password = getStringInput();
					if(password.length()<6){
						System.out.println("Password should be more than 5 characters. Try again.");
						break;
					}
					auc.setPassword(usr,password);
					System.out.println("Initial Deposit amount: ");
					amount = getNumInput();
					if(!auc.deposit(usr,amount)) System.out.println("Deposit Fail! Amount should be positive.");
					else System.out.println("Deposit Successfully!");
					bidStatus = status.logOrRegister;
					break;
				case chooseSeller:
					System.out.println(usr+", do you want to be a seller? Y or N.");
					s = getStringInput();
					auc.clearStatus();
					if(s.equalsIgnoreCase("Y")){
						bidStatus = status.selling;
					}else if(s.equalsIgnoreCase("N")){
						bidStatus = status.bidding;
						System.out.println("Wait for seller...");
					}else System.out.println("Incorrect choice, choose again.");
					break;
				case selling:
					System.out.println("Choose one operation:");
					System.out.println(
						"[1]: view status\n"+
						"[2]: confirm sell"
						);
					choice = getNumInput();
					switch(choice){
						case 1:
							System.out.println(auc.view_auction_status());
							break;
						case 2:
							System.out.println("Type item name:");
							br = new BufferedReader(new InputStreamReader(System.in));
							s = br.readLine();
							boolean sellSuccess = auc.sell(usr, s);
							if(!sellSuccess){
								System.out.println("Seller already exists. You will be bidder.");
								bidStatus = status.bidding;
								break;
							}
							System.out.println("Type base price:");
							price = getNumInput();
							if(price!=-1){							
								auc.setBasePrice(price);
								bidStatus = status.godView;
							}
					}
					break;
				case godView:
					if(auc.roundFinish()){
						System.out.println("Bidding finish, winner:"+auc.curHighestBidder()+" ("+auc.curBid()+").");
						bidStatus = status.waitNewRound;
						break;
					}
					System.out.println("Choose one operation:");
					System.out.println(
						"[1]: view status\n"+
						"[2]: (do nothing)"
						);
					choice = getNumInput();
					switch(choice){
						case 1:
							System.out.println(auc.view_auction_status());
							break;
						case 2:

					}
					break;
				case bidding:
					if(auc.roundFinish()){
						System.out.println("Bidding finish, winner:"+auc.curHighestBidder()+" ("+auc.curBid()+").");
						bidStatus = status.waitNewRound;
						break;
					}
					System.out.println("Choose one operation:");
					System.out.println(
						"[1]: view status\n"+
						"[2]: offer price\n"+
						"[3]: log off"
						);
					choice = getNumInput();
					switch(choice){
						case 1:
							System.out.println(auc.view_auction_status());
							break;
						case 2:		
							System.out.println("How much you want to bid?");					
							price = getNumInput();
							if(price == -1) break;
							String bidResult = auc.bid(usr,price);
							if(bidResult.equals("C")) System.out.println("Offer too low. Bid fail.");
							else if(bidResult.equals("B")){
								System.out.println("Not enough bank balance. Do you want to deposit to your bank account? Y or N.");
								s = getStringInput();
								if(s.equalsIgnoreCase("Y")){
									System.out.println("Deposit amount: ");
									amount = getNumInput();
									if(!auc.deposit(usr,amount)) System.out.println("Deposit Fail! Amount should be positive.");
									else System.out.println("Deposit Successfully!");
									break;
								}else if(s.equalsIgnoreCase("N")){
									bidStatus = status.bidding;
									break;
								}else System.out.println("Incorrect choice, choose again.");
							}
							else System.out.println("Bid Successfully!");
							break;
						case 3:
							System.out.println("Logging off...");
							auc.logoff(usr);
							bidStatus = status.logOrRegister;
							break;
						default:
							System.out.println("Choice not exist.");
					}
					break;
				case waitNewRound:
					if(auc.allReset(usr)){
						System.out.println("Starting a new round...");
						bidStatus = status.chooseSeller;
					}
					try{
						TimeUnit.SECONDS.sleep(2); //check every 2 seconds.
					}catch(InterruptedException ex){}
					
					break;
			}
			
		}
			
	}
	
}

	