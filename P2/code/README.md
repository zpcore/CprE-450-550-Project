## ISU CprE 450/550 Project 2
# Auction System using Corba
## The code applies for Phase III directly. 
---
### Steps to run the code:

### 1) Compile
#### Compile the IDL Interface .idl.
```bash
idlj -fall Auction.idl
```

####  Compile java Program.
```bash
javac --add-modules java.se.ee *.java AuctionApp/*.java
```

### 2) Server Side:
#### Initiate the Port on Server Machine.
```bash
orbd -ORBInitialPort 1050&
```
#### Run Server Code.
```bash
java --add-modules java.se.ee AuctionServer -ORBInitialPort 1050 -ORBInitialHost localhost&
```
### 3) Client Side:
#### Run Client Code. You can initiate more than one clients.
```bash
java --add-modules java.se.ee AuctionClient -ORBInitialPort 1050 -ORBInitialHost localhost
```