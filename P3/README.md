## ISU CprE 450/550 Project 3
# Distributed Coin Flipping Game Using Stellar (applies for phase III)
---
### Preparation:
#### (The code is copied from Stellar offcial website with little revision.)
#### Create the account:
```bash
javac -cp ".:./lib/stellar-sdk.jar" createAccount.java 
java -cp ".:./lib/stellar-sdk.jar" createAccount
```
#### You can run the demo to see how transaction works:
```bash
javac -cp ".:./lib/stellar-sdk.jar" transactionDemo.java  
java -cp ".:./lib/stellar-sdk.jar" transactionDemo
```
#### Check the account accitivity:
```bash
javac -cp ".:./lib/stellar-sdk.jar" receiveCheck.java  
java -cp ".:./lib/stellar-sdk.jar" receiveCheck
```
---
### Steps to run the project:
#### Compile and run the server:
```bash
javac -cp ".:./lib/stellar-sdk.jar:./lib/commons-codec-1.11.jar" TransactionServer.java
java -cp ".:./lib/stellar-sdk.jar:./lib/commons-codec-1.11.jar" TransactionServer
```
#### Compile and run the client, you can run multiple clients to play the game: 
```bash
javac -cp ".:./lib/stellar-sdk.jar:./lib/commons-codec-1.11.jar" TransactionClient.java
java -cp ".:./lib/stellar-sdk.jar:./lib/commons-codec-1.11.jar" TransactionClient
```
#### Check the account accitivity through "receiveCheck.java".