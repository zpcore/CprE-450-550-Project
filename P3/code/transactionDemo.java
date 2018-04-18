import org.stellar.sdk.KeyPair;
import java.net.*;
import java.io.*;
import java.util.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.*;


public class transactionDemo{
	public static void main(String args[]){
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");

		KeyPair source = KeyPair.fromSecretSeed("SBO2Q2AFEYQPZLVLW4TMF4IHDITQGLLILAJ5TAVZRNIQ27KUVQPVSECH");
		KeyPair destination = KeyPair.fromAccountId("GDDJZEYBGGSCKEYCS6S24TJTXONKAPAMQLBUCK7AVQN6L7YU736NCGQU");

		// First, check to make sure that the destination account exists.
		// You could skip this, but if the account does not exist, you will be charged
		// the transaction fee when the transaction fails.
		// It will throw HttpResponseException if account does not exist or there was another error.
		try{
			server.accounts().account(destination);
		}catch(IOException ex){
			System.out.println("Cannot find destination.");
		}
		
		try{
			AccountResponse sourceAccount = server.accounts().account(source);
			Transaction transaction = new Transaction.Builder(sourceAccount)
		        .addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), "22").build())
		        // A memo allows you to add your own metadata to a transaction. It's
		        // optional and does not affect how Stellar treats the transaction.
		        .addMemo(Memo.text("Test Transaction"))
		        .build();
		// Sign the transaction to prove you are actually the person sending it.
			transaction.sign(source);

			SubmitTransactionResponse response = server.submitTransaction(transaction);
			  System.out.println("Success!");
			  System.out.println(response);


		}catch(IOException ex){
			System.out.println("Cannot find source account.");
		}catch(Exception e){
			System.out.println("Something went wrong!");
		  	System.out.println(e.getMessage());
		}
	}

}
