import AuctionApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
// import java.util.Properties;
// import java.util.Map;
// import java.util.HashMap;
// import java.util.Set;
// import java.util.HashSet;
// import java.util.Timer;
// import java.util.TimerTask;
import java.util.*;

class AuctionTimer {
	private int counter = 0;
	private Timer timer;
	public boolean isEnd = false;//read this value to know timer end or not.
	private TimerTask timerTask;
	public int remainingTime = 0;//read this number for count down remaining.
	private int countDown = 0;
	private boolean isStart = false;

	AuctionTimer(int countDown){
		this.countDown = countDown;
	}

	private void init(){
		timerTask = new TimerTask() {
			@Override
			public void run() {
				counter++;
				remainingTime = countDown - counter;
				if(counter == countDown) endTimer();
			}
		};
	};

	public void startTimer(){
		isEnd = false;
		isStart = true;
		counter = 0;
		init();
		timer = new Timer("AuctionTimer");//create a new Timer
		timer.scheduleAtFixedRate(timerTask, 50, 1000);//1s period
	}

	public void reStart(){
		if(isStart) endTimer();
		startTimer();
	}

	public void endTimer(){
		timer.cancel();
		isEnd = true;
		isStart = false;
	}

}


class AuctionImpl extends AuctionPOA{
	private ORB orb;

	private boolean hasSeller = false;
	private String itemName = null;
	private int totalUsr = 0;
	public int curBid = 0; // current bid price
	public String curHighestBidder = null;
	private String seller = null;
	private AuctionTimer at;
	private final int CountDownMax = 10;//count down timer (seconds)
	private boolean cleared = false;
	
	private Map<String,BankAccount> usr2Bank = new HashMap<>();//map username to bankaccount
	private Set<String> onlineAccount = new HashSet<>();
	private Set<String> readyAccount = new HashSet<>();

	public void setORB(ORB orb_val) {
		orb = orb_val;
		at = new AuctionTimer(CountDownMax);
	}

	public String curHighestBidder(){
		return curHighestBidder;
	}

	public int curBid(){
		return curBid;
	}


	public void clearStatus(){
		at = new AuctionTimer(CountDownMax);
		curBid = 0;
		itemName = null;
		curHighestBidder = null;
		cleared = false;
		seller = null;
		hasSeller = false;
	}

	public String view_auction_status(){
		return "Seller: "+ seller+" Item: "+itemName+'\n'+
		"Bidder: "+curHighestBidder+" offer: "+String.valueOf(curBid)+" (Time Remain:"+at.remainingTime+" sec)"+'\n';
	}

	public boolean setUsrName(String name){
		if(usr2Bank.containsKey(name)) return false;
		usr2Bank.put(name,new BankAccount(name));
		return true;
	}

	public void logoff(String client){
		onlineAccount.remove(client);
		usr2Bank.get(client).online = false;
	}

	public boolean checkAccount(String client, String password){
		if(usr2Bank.containsKey(client)&&!usr2Bank.get(client).online&&usr2Bank.get(client).checkPassword(password)){
			usr2Bank.get(client).online = true;
			onlineAccount.add(client);
			return true;
		}
		return false;
	}

	public void setPassword(String client, String password){
		BankAccount ba = usr2Bank.get(client);
		ba.setPassword(password);
	}

	public boolean roundFinish(){
		return at.isEnd;
	}

	public boolean allReset(String client){
		//check all account is ready for new round.
		if(cleared) return true;
		readyAccount.add(client);
		if(readyAccount.size()!=onlineAccount.size()) return false;
		for(String user:onlineAccount){
			if(!readyAccount.contains(user)) return false;
		}
		cleared = true;
		readyAccount.clear();

		return true;
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
		at.reStart();
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
