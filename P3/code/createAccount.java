// create a completely new and unique pair of keys.
// see more about KeyPair objects: https://stellar.github.io/java-stellar-sdk/org/stellar/sdk/KeyPair.html
import org.stellar.sdk.KeyPair;
import java.net.*;
import java.io.*;
import java.util.*;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;


public class createAccount{

	public static void main(String agrs[]){
		/*Create public key and private key*/
		KeyPair pair = KeyPair.random();
		System.out.println(new String(pair.getSecretSeed()));
		// SAV76USXIJOBMEQXPANUOQM6F5LIOTLPDIDVRJBFFE2MDJXG24TAPUU7
		System.out.println(pair.getAccountId());
		// GCFXHS4GXL6BVUCXBWXGTITROWLVYXQKQLF4YH5O5JT3YZXCYPAFBJZB	
		/*Create a test account*/
		String friendbotUrl = String.format("https://friendbot.stellar.org/?addr=%s", pair.getAccountId());
		try{
			InputStream response = new URL(friendbotUrl).openStream();
			String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
			System.out.println("SUCCESS! You have a new account :)\n" + body);
		}catch(MalformedURLException ex){
			System.out.println("URL form incorrect.");
		}catch(IOException ex){
			System.out.println("IO Exception.");
		}
		/*Getting the account’s details and checking its balance*/
		Server server = new Server("https://horizon-testnet.stellar.org");
		try{
			AccountResponse account = server.accounts().account(pair);
			System.out.println("Balances for account " + pair.getAccountId());
			for (AccountResponse.Balance balance : account.getBalances()) {
				System.out.println(String.format(
				"Type: %s, Code: %s, Balance: %s",
				balance.getAssetType(),
				balance.getAssetCode(),
				balance.getBalance()));
			}
		}catch(IOException ex){
			System.out.println("IO Exception.");
		}
		
		
		
	}


}

