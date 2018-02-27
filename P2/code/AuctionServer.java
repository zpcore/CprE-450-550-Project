import AuctionApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;


class AuctionImpl extends AuctionPOA{
	private ORB orb;

	private boolean hasSeller = false;
	private String itemName = null;
	private int totalUsr = 0;
	private int curBid = 0; // current bid price
	private String curHighestBidder = null;
	private String seller = null;
	public boolean isFinish = false;//may be unaccessable from client
	
	private Map<String,BankAccount> usr2Bank = new HashMap<>();//map username to bankaccount

	public void setORB(ORB orb_val) {
		orb = orb_val; 
	}

	public String view_auction_status(){
		return "Seller: "+ seller+" Item: "+itemName+'\n'+
		"Bidder: "+curHighestBidder+" offer: "+String.valueOf(curBid)+'\n';
	}

	public boolean setUsrName(String name){
		if(usr2Bank.containsKey(name)) return false;
		usr2Bank.put(name,new BankAccount(name));
		return true;
	}

	public boolean checkAccount(String client, String password){
		return usr2Bank.containsKey(client)&&!usr2Bank.get(client).online&&usr2Bank.get(client).checkPassword(password);
	}

	public void setPassword(String client, String password){
		BankAccount ba = usr2Bank.get(client);
		ba.setPassword(password);
	}

	public boolean hasSeller(){
		return hasSeller;
	}

	public boolean deposit(String client, int amount){
		BankAccount ba = usr2Bank.get(client);
		return ba.deposit(amount);
	}

	public String bid(String client, int price){
		//lower price -> cannot bid
		BankAccount ba = usr2Bank.get(client);
		if(ba.balance < price) return "B";
		if(price<curBid) return "C";
		curBid = price;
		curHighestBidder = client;
		return "A";
	}

	public void setBasePrice(int price){
		curBid = price;		
		curHighestBidder = seller+"(seller)";
	}

	public boolean sell(String client, String itemName){
		if(hasSeller) return false;
		hasSeller = true;
		seller = client;	
		this.itemName = itemName;
		return true;
	}

	public void shutdown() {
		orb.shutdown(false);
	}

}

class BankAccount{
	public String usr;
	private String password;
	public int balance;
	public boolean online = false;

	BankAccount(String usr){
		this.usr = usr;
		balance = 0;
	}
	public void setPassword(String password){
		this.password = password;
		online = true;
	}

	public boolean deposit(int money){
		if(money<=0) return false;
		balance += money;
		return true;
	}

	public boolean checkPassword(String password){
		return this.password.equals(password);
	}

	public boolean withdraw(int money){
		if(balance < money) return false;
		balance -= money;
		return true;
	}

}



public class AuctionServer {

	public static void main(String args[]) {
		try{
			// create and initialize the ORB
			ORB orb = ORB.init(args, null);

			// get reference to rootpoa & activate the POAManager
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			// create servant and register it with the ORB
			AuctionImpl AuctionImpl = new AuctionImpl();

			AuctionImpl.setORB(orb);

			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(AuctionImpl);
			Auction href = AuctionHelper.narrow(ref);
			
			// get the root naming context
			org.omg.CORBA.Object objRef =
			orb.resolve_initial_references("NameService");
			// Use NamingContextExt which is part of the Interoperable
			// Naming Service (INS) specification.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			// bind the Object Reference in Naming
			String name = "Auction";
			NameComponent path[] = ncRef.to_name( name );
			ncRef.rebind(path, href);

			System.out.println("AuctionServer ready and waiting ...");

			// wait for invocations from clients
			orb.run();
		} 
		
		catch (Exception e) {
			System.err.println("ERROR: " + e);
			e.printStackTrace(System.out);
		}
		System.out.println("AuctionServer Exiting ...");

	}
}
