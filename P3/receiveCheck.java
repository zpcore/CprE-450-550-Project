import org.stellar.sdk.KeyPair;
import java.net.*;
import java.io.*;
import java.util.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.*;
import org.stellar.sdk.responses.operations.*;
import org.stellar.sdk.requests.*;


public class receiveCheck{
	public static String myToken = null;
	public static void main(String args[])throws Exception{
		Server server = new Server("https://horizon-testnet.stellar.org");

		KeyPair source = KeyPair.fromSecretSeed("SBS72QGLQO4YMLNDNBDX3WH3TP5424XDZ7KBDBUX3ACIFP4VS3UFMVTD");
		AccountResponse sourceAccount = server.accounts().account(source);
		KeyPair account = KeyPair.fromAccountId("GCT3DYSYYWCM36O6PRQEJJSKOMEVQZOTXUW3CH75LPIGEPWGR5MYH6HS");

		// Create an API call to query payments involving the account.
		PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(account);

		// If some payments have already been handled, start the results from the
		// last seen payment. (See below in `handlePayment` where it gets saved.)
		String lastToken = loadLastPagingToken();
		if (lastToken != null) {
			paymentsRequest.cursor(lastToken);
		}

		// `stream` will send each recorded payment, one by one, then keep the
		// connection open and continue to send you new payments as they occur.
		paymentsRequest.stream(new org.stellar.sdk.requests.EventListener<OperationResponse>() {
			@Override
			public void onEvent(OperationResponse payment) {
		    // Record the paging token so we can start from here next time.
				savePagingToken(payment.getPagingToken());

				// The payments stream includes both sent and received payments. We only
				// want to process received payments here.
				if (payment instanceof PaymentOperationResponse) {
				if (((PaymentOperationResponse) payment).getTo().equals(account)) {
				return;
				}

				String amount = ((PaymentOperationResponse) payment).getAmount();

				Asset asset = ((PaymentOperationResponse) payment).getAsset();
				String assetName;
				if (asset.equals(new AssetTypeNative())) {
				assetName = "lumens";
				} else {
				StringBuilder assetNameBuilder = new StringBuilder();
				assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
				assetNameBuilder.append(":");
				assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId());
				assetName = assetNameBuilder.toString();
				}

				StringBuilder output = new StringBuilder();
				output.append(amount);
				output.append(" ");
				output.append(assetName);
				output.append(" from ");
				output.append(((PaymentOperationResponse) payment).getFrom().getAccountId());
				System.out.println(output.toString());
				}

			}
		});

	}
	protected static void savePagingToken(String pagingToken) {
        myToken = pagingToken;
        System.out.println(String.format("myToken is %s",myToken));
    }
    private static String loadLastPagingToken() {
        return myToken;
    }

}

