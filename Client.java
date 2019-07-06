import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.rmi.Naming;
import java.rmi.RemoteException;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

/**
 * This is a Advanced auctioning system implementaton using RMI and JGroups
 * @author James Brindley
 *
 */
public class Client {
	
	/**
	 * Scanner to read info from client
	 */
	public static Scanner scanner = new Scanner(System.in);
	/**
	 * Stores the clients username
	 */
	private static String userID;
	/**
	 * Stores the password
	 */
	private static String Password;
	/**
	 * Stores the keygen
	 */
	private static KeyPairGenerator keyGen;
	/**
	 * Stores the keypair of the public and private
	 */
	private static KeyPair pair;
	/**
	 * Stores the clients private key
	 */
	private static PrivateKey privateKey;
	/**
	 * Stores the clients public key
	 */
	private static PublicKey publicKey;
	/**
	 * Stores the servers public key
	 */
	public static PublicKey serverpublic;
	/**
	 * Gets the current directory the program is being run in
	 */
	static String current = System.getProperty("user.dir");
	
	/**
	 * Method to start the client, able's the user to either log in or create a new account
	 * Then enters an infinity loop for the options
	 * @param args
	 */
public static void main(String[] args) {
	try {
		try {
			ServerInterface connection = (ServerInterface) Naming.lookup("rmi://localhost/Auction");
			try {
				connection.checkconnected();
			}catch(RemoteException e) {
				System.out.println("Error Connecting to server:"+e);
				System.exit(0);
			}finally{
				System.out.println("Sucessfully connected to auction server");
			}
			System.out.println("Do you wish to: ");
			System.out.println("Create a new account - N");
			System.out.println("Login to excisting account - L");
			boolean login = true;
			boolean chosen = true;
			while(chosen) {
				switch (readstring()) {
					case "n":
					case "N":
						while(login) {
							
							System.out.println("Please enter a username you wish to use");
							userID = readstring();
							System.out.println("Please enter a password you wish to use");
							Password = readstring();
							System.out.println("Creating account...");
							boolean check = connection.checkusername(userID);
							if (!check) {
								System.out.println("Error: Username "+userID+" is already in use, please enter another one");
							}
							else {
								GenerateKeys();
								createKeys();
								sendpublic(connection); 
								saveserverkey();
								SealedObject so = encryptpassword(Password);
								int result = connection.registerclientID(userID, so); 
								boolean check2 = sendrequest(connection);
								if (result == 1 & check2 == true) {
									login = false;
									chosen = false;
									System.out.println("You have succesfully created an account");
								}
								else {
									login = false;
									System.out.println("Account creation unsucesfull");
									System.out.println("Please enter either N - to create a new account or L - to login");
								}
							}
						}
						break;
					case "L":
					case "l":
						while(login) {
							System.out.println("Please enter your username");
							userID = readstring();
							System.out.println("Please enter your password");
							Password = readstring();
							boolean loaded = loadkeys();
							if (loaded) {	
							boolean check = sendrequest(connection);
							if (check == true) {
								System.out.println("Loging you in...");
								SealedObject so = encryptpassword(Password);
								int result = connection.checklogindetails(userID, so); //fucks here
								if (result == 0 ) {
									System.out.println("Error: There is no account with the username "+userID);
									System.out.println("Please enter either N - to create a new account or L - to login");
									login = false;
								}
								else if (result == 1 ){
									System.out.println("Error: The password you entered is incorrect");
									System.out.println("Please enter either N - to create a new account or L - to login");
									login = false;
								}
								else {
									System.out.println("You have succesfully logged in");
									login = false;
									chosen = false;
								}
							}
							else {
								System.out.println("Login has been unsucesfull");
								System.out.println("Please enter either N - to create a new account or L - to login");
								login = true;
							}
							}
							else {
								System.out.println("Unable to load the keys, returning to main menu");
								System.out.println("Please enter either N - to create a new account or L - to login");
								login = true;
							}
						}
						break;
					default:
						System.out.println("Please enter either N - to create a new account or L - to login");
						break;
						
				}
			}
			System.out.println("Please choose an option from below:");
			printoptions();
			
			while(true) {
				switch (readstring()) {
					case "n":
					case "N":
						createlisting(connection);
						break;
					case "l":
					case "L":
						showmylistings(connection);
						break;
					case "s":
					case "S":
						showalllistings(connection);
						break;
					case "h":
					case "H":
						printoptions();
						break;
					case "m":
					case "M":
						showbids(connection);
						break;
					case "q":
					case "Q":
						System.out.println("Quiting...");
						System.exit(0);
					case "a":
					case "A":
						searchbyauction(connection);
						break;
					case "r":
					case "R":
						removeauction(connection);
						break;
					case "o":
					case "O":
						searchclosedauctions(connection);
						break;
					case "b":
					case "B":
						bid(connection, userID);
						break;
					default:
						System.out.println("Your input was not recognised, (H - Main Menu), please try again...");
						break;
				}
			}
		} catch (Exception e) {
			System.out.println("Error:" +e);
		}
	}catch (Exception e) {
		System.out.println("Server Error: Can't connect to server");
	}
}

/**
 * Generates a key pair using RSA  with a key lengh of 2048
 * @throws NoSuchAlgorithmException
 */
public static void GenerateKeys() throws NoSuchAlgorithmException {
	int keylength = 2048;
	keyGen = KeyPairGenerator.getInstance("RSA");
	keyGen.initialize(keylength);
}
/**
 * Encrypts the password with the servers public key
 * @param password
 * @return
 */
public static SealedObject encryptpassword(String password) {
	try {
		Cipher DEScipher = Cipher.getInstance("RSA");
		DEScipher.init(Cipher.ENCRYPT_MODE, serverpublic);
		SealedObject sealedpassword;
		sealedpassword = new SealedObject(password,DEScipher);
		return sealedpassword;
	} catch (InvalidKeyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IllegalBlockSizeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (NoSuchPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null;
}
/**
 * Converts the keys into bytes and saves them into a file which can later be loaded
 * @param k Public Key
 * @param p Private Key
 * @throws IOException
 */
public static void savekeys(PublicKey k, PrivateKey p) throws IOException {
	byte[] keybytes = k.getEncoded();
    FileOutputStream stream = new FileOutputStream(current+"/"+userID+"_publicKey.pub");
    stream.write(keybytes);
    stream.close();
    
    byte[] pkeybytes2 = p.getEncoded();
    FileOutputStream pstream = new FileOutputStream(current+"/"+userID+"_privateKey.key");
    pstream.write(pkeybytes2);
    pstream.close();
}
/**
 * Saves the servers public key so it can be loaded later
 * @throws IOException
 */
public static void saveserverkey() throws IOException {
	 byte[] keybytesserver = serverpublic.getEncoded();
	    FileOutputStream streamserver = new FileOutputStream(current+"/"+"Server_publicKey.pub");
	    streamserver.write(keybytesserver);
	    streamserver.close();
}
/**
 * Calls the different load keys for the public key and the private key
 * @return 
 */
public static boolean loadkeys() {
	boolean loaded2 = loadprivate();
	boolean loaded1 = loadpublic();
	if (loaded2 == true && loaded1 == true) {
	return true;
	} else {
		return false;
	}
}
/**
 * Loads the private key from a file, reads all bytes from a file into an array of bytes
 * a sepc for PKCS8 encoding is then made using the bytes
 *  Creates a new isntance of RSA
 *  Using the isntance a private key is created from the spec
 */
public static boolean loadprivate() {
	try {
		byte[] bytes =  Files.readAllBytes(Paths.get(current+"/"+userID+"_privateKey.key"));
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		privateKey = factory.generatePrivate(spec);
		if(privateKey != null)
		return true;
	} catch (InvalidKeySpecException e) {
		System.out.println("Somethigs gone wrong while loading the private key, please check to see if the key file is still aviablable to load");
	}
	catch (NoSuchAlgorithmException e1) {
		System.out.println("Somethigs gone wrong while loading the private key, please check to see if the key file is still aviablable to load");
	}
	catch (IOException e) {
		System.out.println("You Currently have no private key associated with that username");
	}
	return false;
}
/**
 * Loads the public key from a file, reads all bytes from a file into an array of bytes
 * a sepc for X509 encoding is then made using the bytes
 *  Creates a new isntance of RSA
 *  Using the isntance a public key is created from the spec
 */
public static boolean loadpublic() {
	try {
		byte[] bytes =  Files.readAllBytes(Paths.get(current+"/"+userID+"_publicKey.pub"));
		byte[] bytesserver =  Files.readAllBytes(Paths.get(current+"/"+"Server_publicKey.pub"));
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		X509EncodedKeySpec specserver = new X509EncodedKeySpec(bytesserver);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		publicKey = factory.generatePublic(spec);
		serverpublic = factory.generatePublic(specserver);
		if(publicKey != null)
			return true;
	} catch (InvalidKeySpecException e) {
		System.out.println("Somethigs gone wrong while loading the public key, please check to see if the key file is still aviablable to load");
	}
	catch (NoSuchAlgorithmException e1) {
		System.out.println("Somethigs gone wrong while loading the public key, please check to see if the key file is still aviablable to load");
	}
	catch (IOException e) {
		System.out.println("You Currently have no public key associated with that username");
	}
	return false;
}
/**
 * Gets the publci key of the server and then sends the clients public key along with the username
 * @param connection
 * @throws Exception 
 */
public static void sendpublic(ServerInterface connection) throws Exception {
	try {
		serverpublic = connection.getpublic();
		boolean success = connection.givepublic(publicKey, userID);
	} catch (RemoteException e) {
		System.out.println("Failed to connect to the server to retrieve the public key");
	}
}	

/**
 * Splits the pair of keys into a private and public key
 * Then saves the keys to a file
 * @throws IOException
 */

public static void createKeys() throws IOException {
	pair = keyGen.generateKeyPair();
	privateKey = pair.getPrivate();
	publicKey = pair.getPublic();
	savekeys(publicKey, privateKey);
}
/**
 * Method to send a challange request to the server, starts the challange by sending the user id, this then returns a sealed object
 * A cipher is created using RSA and is then used to decrypt the sealed object using the clients privatekey
 * The answer is then calculated using the two numbers and mulitplying them together. 
 * This answer is then encrypted using the servers public key  and sent to back to the server
 * If the answer is true then the user is identified
 * 
 * Then the next stage is where the client challanges the server, this is done in the same way
 * The client now generates two random numbers and encrypts them using the servers public key
 * This is then sent to the server to be tested
 * If the server reterns the correct answer then the server is identified and the user will then send the password to log in
 * @param connection
 * @return
 * @throws NoSuchAlgorithmException
 * @throws NoSuchPaddingException
 * @throws InvalidKeyException
 */
public static boolean sendrequest(ServerInterface connection) {
	try {
		System.out.println("Challaging the server");
		int random = 0;
		if (serverpublic != null) {
			random = (int )(Math.random() * 5000 + 1);
			SignedObject signedanswer = connection.acceptchallange(random, userID);
			
			Signature sig = Signature.getInstance("SHA256withRSA");
			boolean rightsig = signedanswer.verify(serverpublic, sig);
			if (rightsig) {
				System.out.println("Server keys have been succesfuly Identified");
				int random2 = connection.challenge(userID);
			
				Signature mysig = Signature.getInstance("SHA256withRSA");
				mysig.initSign(privateKey);
				
				SignedObject SignOb = new SignedObject(random2, privateKey, sig);
				
					boolean clientauth = connection.answer(SignOb, userID);
					if(clientauth) {
						System.out.println("Your keys have been succesfuly Identified");
						return true;
					}
					else {
						System.out.println("Your identification has been unsuccesfuly");
						return false;
					}
					
				}
			}
			else {
				System.out.println("Warning: Server you are connected to has no been Identified");
				return false;
			}
		}catch(Exception e) {
			System.out.println("Error: Cant connect to the server");
		}
	return false;
}


/**
 * Method to print out the values of a auction items. 
 * ID stored in index 0, start price in index 1, high bid in index 2, description in index 3 and status in index 4.
 * @param item - List of string varaibles from the item sale class
 */
public static void printdatatoscreen(ArrayList<String> item) {
	System.out.println("AuctionID: "+item.get(0)+"| Start Price: " +item.get(1)+ "| Highest Bid: "+item.get(2));
	System.out.println("Description: "+item.get(3));
	if (item.get(4).equals("false")) {
		System.out.println("Auction status: Closed");
	}
	else if (item.get(4).equals("true")){
		System.out.println("Auction status: Open");
	}
	else {
		System.out.println("Auction status: Null");
	}
	System.out.println("--------------------------------------------------------------------------");
}
/**
 * Method to print out all options available to the user with the correct input key to activate them
 */
public static void printoptions() {
	System.out.println("N - New listing");
	System.out.println("S - Show all listings");
	System.out.println("L - Show my listings");
	System.out.println("M - Show my bids");
	System.out.println("A - To search using an auction ID");
	System.out.println("R - To remove an auction listing");
	System.out.println("O - To search a closed auction");
	System.out.println("B - To place a bid");
	System.out.println("Q - Quit");
	System.out.println("H - To see this list again");
}
/**
 * Method to read integers from the line, will return a number format error if something other than an integer is entered.
 * will only accept it when the value is not empty
 * @return integer value that was read from the scanner
 */
public static int readint() {
	int value = 0;
		try {
        	value = Integer.parseInt(scanner.nextLine());
        	if (value == 0) {
    			System.out.println("Please enter a value to continue (H - Main Menu)");
    		}
        	return value;
    	} catch (NumberFormatException e) {
        	System.out.println("The value entered is not an integer, please try again.");
    	}catch(NoSuchElementException e){
    		
    	}
	return value;
}
/**
 * Method to read characters from the line.
 * will only accept it when the value is not empty
 * @return string value that was read from the scanner
 */
public static String readstring() {
	try {
	String value = "";
		value = scanner.nextLine();
		if (value.trim().isEmpty()) {
			System.out.println("Please enter a value to continue (H - Main Menu)");
		}
	return value;
	}
	catch(NoSuchElementException e){
		
	}
	return "";
}
/**
 * Method for the user to bid. The user is prompted to enter the auction ID, the bid they wish to place and then the email to be contacted on after the sale
 * this is then passed to the server and a integer is returned which is compared to error codes in the checkbidnum method.
 * @param connection the connection to the RMI server
 * @param userID the users ID entered at the start of the application
 * @throws Exception 
 */
public static void bid(ServerInterface connection, String userID) throws Exception {
	System.out.println("Please enter the auction ID you wish to bid on");
	int auctionID = readint();
	System.out.println("Please enter your bid");
	int bid = readint();
	System.out.println("Please enter your email you wish to be connacted on after the auctions completion");
	String bidderemail = readstring();
	int check = 0;
	try {
		check = connection.bid(auctionID, bid, userID, bidderemail);
	} catch (RemoteException e) {
		System.out.println("Server Error: Can't connect to server");
	}
	checkbidnum(check);
}
/**
 * Method to translate the integer message given by the server during the closing auction stage to a readable format for the user.
 * @param value - Takes the error code provided for the server 
 */
public static void checkclosenum(int value) {
	if (value == 1) {
		System.out.println("Auction has been removed succesfully");
	}else if (value == 2) {
		System.out.println("Closing Error: ");
	}else if (value == 3) {
		System.out.println("Server Rejection: Your userID is not that which was used to create the auction");
	}else if (value == 4) {
		System.out.println("The bid that was placed is under the reserve price that was stated upon listing therefore the item is returned to you");
	}else if (value == 5) {
		System.out.println("Closing Error: There is no auction with that ID");
	}
	else if (value == 0) {
		System.out.println("Closing Error: ");
	}
	
}
/**
 * Method to translate the integer message given by the server during the bidding stage to a readable format for the user.
 * @param value - The error message passed from the servers bidding function
 */
public static void checkbidnum(int value) {
	if (value == 1) {
		System.out.println("Bid was sucessfully placed");
	}else if (value == 2) {
		System.out.println("Bid Error: There is either a higher bid already placed or the bid entered is below the starting price...Returning you to the main menu");
	}else if (value == 3) {
		System.out.println("Bid Error: There is no auction with that ID");
	}else if (value == 4) {
		System.out.println("Bid Error: You can't bid on auctions you created");
	}
	else if (value == 0) {
		System.out.println("Bid Error: ");
	}
	
}
/**
 * Method to check if the user would like to place a bid, called after every search for a auction or listing
 * @param connection - The connection to the server
 * @param userID - The users ID
 * @throws Exception 
 */
public static void bidchoice(ServerInterface connection, String userID) throws Exception {
	System.out.println("Would you like to make a bid? Y for yes / N for no");
	switch (readstring()) {
	case "y":
	case "Y":
		bid(connection, userID);
		break;
	case "n":
	case "N":
		System.out.println("Returning to the main menu, press H to show help...");
		break;
	default:
		System.out.println("Error: You have entered an invialid character, returning you to the main menu...");
		break;
	}
}
/**
 * Method to check with the user if the listing information they have supplied is right, called by the createlisting function
 * returns a boolean depending on the answer
 * @param connection - The connection to the server
 */
public static boolean yesorno(ServerInterface connection) {
	System.out.println("Is this listing correct? Yes(Y) or No(N)");
	switch (readstring()) {
	case "y":
	case "Y":
		return false;
	case "n":
	case "N":
		return true;
	default:
		System.out.println("Error: You have entered an invialid character");
		return true;
	}
}
/**
 * Method to create a listing
 * Takes user input for the starting price, the reserve price and then a description for the item
 * Passes all the varaibles and the userID to the server which returns the auction ID for the item created
 * @param connection The connection to the server
 */
public static void createlisting(ServerInterface connection) {
	System.out.println("Creating listing...");
	boolean correct = true;
	boolean morethan = true;
	int startPrice = 0;
	int minPrice = 0;
	String description = "";
	while(correct) {
		while(morethan) {
			System.out.println("Please enter the starting price: ");
			startPrice = readint();
			System.out.println("Please enter the minimum price you are willing to accept: ");
			minPrice = readint();
			if(minPrice > startPrice) {
				morethan = false;
			}
			else {
				System.out.println("Error: The reserve price is lower than the starting price, please try again.");
			}
		}
		System.out.println("Please enter a description: ");
		description = readstring();
		System.out.println("--------------------------------------------------------------------------");
		System.out.println("Start Price: " +startPrice+ "| Reserve Price: "+minPrice);
		System.out.println("Description: "+description);
		System.out.println("--------------------------------------------------------------------------");
		correct = yesorno(connection);
	}
	try {
		int auctionnum = connection.createlisting(startPrice, minPrice, description, userID);
		System.out.println("Listing succesfully created, your auction ID number is: "+auctionnum);
	}catch (RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
}
/**
 * Method to show the user all the listings they have created 
 * Gets an arraylist in an arraylist containing the auction ID, start price, highest bid, auctions status and description for each listing
 * returns null if there are no listings
 * @param connection The connection to the server
 */
public static void showmylistings(ServerInterface connection) {
	System.out.println("Showing your listings...");
	try {
		ArrayList<ArrayList<String>> mylistings = null;
		mylistings = connection.getmylistings(userID);
		if (mylistings == null) {
			System.out.println("You curerntly have no listings");
		}
		else {
			for(int i = 0; i < mylistings.size(); i++) {
				printdatatoscreen(mylistings.get(i));
			}
		}
	}catch(RemoteException e){
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
}
/**
 * Shows the user all the avaible listings on the server
 * Gets an arraylist in an arraylist containing the auction ID, start price, highest bid, auctions status and description for all listings
 * @param connection The connection to the sever
 */
public static void showalllistings(ServerInterface connection) {
	System.out.println("Showing all available listings...");
	try {
		ArrayList<ArrayList<String>> alllistings = null;
		alllistings = connection.getlistings();
		
		if (alllistings == null) {
			System.out.println("There are currently no listings");
		}
		else {
			for(int i = 0; i < alllistings.size(); i++) {
				printdatatoscreen(alllistings.get(i));
			}
			bidchoice(connection, userID);
		}
	}catch(RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
}
/**
 * Method to show the user all their current bids
 * returns null if there are no bids but if there are returns the auction ID, start price, highest bid and the description
 * @param connection The connection to the server
 */
public static void showbids(ServerInterface connection) {
	System.out.println("Loading your bids...");
	try {
		ArrayList<ArrayList<String>> mybids = null;
		mybids = connection.getbids(userID);
		if (mybids == null) {
			System.out.println("You curerntly have no bids");
		}
		else {
			for(int i = 0; i < mybids.size(); i++) {
				printdatatoscreen(mybids.get(i));
			}
		}
	}catch(RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
}
/**
 * Method to allow the user to search by an already known auction ID. Returns null if there are no auctions with that ID
 * Returns the auction ID, start price, highest bid and the description of that auction
 * @param connection The connection to the server
 */
public static void searchbyauction(ServerInterface connection) {
	System.out.println("Please enter the auction ID...");
	try {
		ArrayList<String> auctiondata = connection.searchauction(readint());
		if(auctiondata == null) {
			System.out.println("There is no auction with that auction ID");
		}
		else {
			printdatatoscreen(auctiondata);
			bidchoice(connection, userID);
		}
	}catch(RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
	
}
/**
 * Method to search through closed auctions by the auction ID
 * Returns the auction ID, start price, highest bid and the description of that auction
 * @param connection The connection to the server
 * @throws Exception 
 */
public static void searchclosedauctions(ServerInterface connection) throws Exception {
	System.out.println("Please enter the auction ID");
	int auctionID = readint();
	try{
		ArrayList<String> auctiondata = connection.getcloseddetails(auctionID);
		if(auctiondata == null) {
			System.out.println("There is no closed auction with that auction ID");
		}
		else {
			printdatatoscreen(auctiondata);
		}
	}catch(RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
}
/**
 * Method to remove a auction, passes auction ID and user ID to the server
 * Server returns a integer which can be translated to an error message using the checkclosenum method
 * @param connection 	The connection to the server
 */
public static void removeauction(ServerInterface connection) {
	System.out.println("Please enter the auction ID you wish to end...");
	try {
		int auctiontoclose = readint();
		int check = connection.closebid(auctiontoclose, userID);
		checkclosenum(check);
		if (check == 1) {
			List<String> closeditem = connection.getbuyerdetails(auctiontoclose);
			System.out.println("The highest bidder for auction "+auctiontoclose+" is "+closeditem.get(1)+" with an email of " +closeditem.get(2)+" and was bought for £"+closeditem.get(0));
		}
	}catch(RemoteException e) {
		System.out.println("Server Error: There was a problem connecting, please try again: ");
	}
	catch(Exception e) {
		System.out.println("Error: Something went wrong, please try again ");
	}
}
}