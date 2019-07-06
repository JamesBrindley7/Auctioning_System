import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.rmi.RemoteException;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
/**
 * This is a Advanced auctioning system implementaton using RMI and JGroups
 * @author James Brindley
 *
 */
public class ServerReplica {
	/**
	 * Auction counter to be used as the auction ID, 1 is added each time a new acution is created
	 */
	private static int auctioncounter = 1; 
	/**
	 * Hashtable to store the auction items in with the ID as a key
	 */
	public static Hashtable<Integer, ItemSale> ItemHash = new Hashtable<Integer, ItemSale>();
	/**
	 * Hashtable to store the removed items in
	 */
	public static Hashtable<Integer, ItemSale> ItemRemovedHash = new Hashtable<Integer, ItemSale>();
	/**
	 * Hashtable to store the usernames of the clients and the encrypted passwords
	 */
	public static Hashtable<String, SealedObject> UserHash = new Hashtable<String, SealedObject>(); 
	
	/**
	 * The key generator to generate the public and private keys for the server
	 */
	private KeyPairGenerator keyGen;
	/**
	 * The pair of public and private keys for the server
	 */
	private KeyPair pair;
	/**
	 * The servers private key
	 */
	private static PrivateKey privateKey;
	/**
	 * The servers public key
	 */
	private static PublicKey publicKey;
	/**
	 * The key hashtable where the clients username is the key 
	 */
	private Hashtable<String, PublicKey> keyHash = new Hashtable<String, PublicKey>();
	/**
	 * The lock used by the replica to make sure it doesnt enter the critical section with different processes
	 */
	private lock lock = new lock();
	/**
	 * The current directory the replica is being run from
	 */
	String current = System.getProperty("user.dir");
	
	public static void main(String[] args) throws Exception
    {
        new ServerReplica().startup();
    }
	/**
	 * Method to start the server, joins the ServerReplicas channel and creates anew dispacher to send messages. If the public and private keys are in the file 
	 * location then load them otherwise create new keys. Then send a request to all other replicas to get all data, if none are sent back then load from files instead
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void startup() throws Exception{
		RpcDispatcher dispactch = null;
		try {
		Channel chan = new JChannel();
		chan.connect("ServerReplicas");
		chan.setDiscardOwnMessages(true);
        dispactch = new RpcDispatcher(chan, this);
        Address address = chan.getAddress();
		}
		catch(Exception E){
			System.out.println("Problem connecting to server replicas");
		}
        if (new File(current+"/ServerKeys/Server_publicKey.pub").isFile() && new File(current+"/ServerKeys/Server_privateKey.key").isFile()) {
			loadkeysReplica(); 
		}
		else {
			try {
				GenerateKeysReplica();
				createKeysReplica();
				saveserverkeysReplica(); 
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Error creating keys");
			} catch (IOException e) {
				System.out.println("Error saving the keys");
			}
		}
		RequestOptions options = new RequestOptions(ResponseMode.GET_ALL,5000, false);
		RspList<?> responcelist = dispactch.callRemoteMethods(null,"GetEverything",new Object[]{},new Class[]{}, options);
		List<?> results = responcelist.getResults();
		if(results.size() == 0) {
			System.out.println("I am the first server");
			if(new File(current+"/ServerKeys/userID's.txt").isFile()) {
				loadallkeysReplica();
			}
			else {
				System.out.println("Can't find user data...Creating new files");
			}
		}
		else {
		System.out.println("There are "+(results.size())+" other servers up");
		System.out.println("Loaded data from another server");
		Hashtable<?, ?> responcetable = (Hashtable<?, ?>) results.get(0);
		
		Object auctioncounterOB = responcetable.get("auctioncounterOB"); 
		Object ItemHashOB = responcetable.get("ItemHashOB"); 
		Object ItemRemovedHashOB = responcetable.get("ItemRemovedHashOB");
		Object UserHashOB = responcetable.get("UserHashOB");
		Object keyHashOB = responcetable.get("keyHashOB");
		
		auctioncounter = (int) auctioncounterOB;
		ItemHash = (Hashtable<Integer, ItemSale>)ItemHashOB;
		ItemRemovedHash =(Hashtable<Integer, ItemSale>)ItemRemovedHashOB;
		UserHash = (Hashtable<String, SealedObject>)UserHashOB;
		keyHash = (Hashtable<String, PublicKey>)keyHashOB;
		System.out.println("Data Loaded...");
		}
	}
	/**
	 * Saves everything from its hash tables and sends it back, called by other replicas
	 * @return
	 */
	 public Hashtable<String, Object> GetEverything()
	    {
		 Hashtable<String, Object> Everything = new Hashtable<>();

		 	Hashtable<Integer, ItemSale> TempItemHash = new Hashtable<Integer, ItemSale>(); //Hashtable to store the auctions
			Hashtable<Integer, ItemSale> TempItemRemovedHash = new Hashtable<Integer, ItemSale>(); //Hashtable to store expired auctions4
			Hashtable<String, SealedObject> TempUserHash = new Hashtable<String, SealedObject>(); //Hashtable to store the user ID and passwords
			Hashtable<String, PublicKey> TempkeyHash = new Hashtable<String, PublicKey>(); //Hashtable to store the user ID and passwords
			
			TempItemHash.putAll(ItemHash);
			TempItemRemovedHash.putAll(ItemRemovedHash);
			TempUserHash.putAll(UserHash);
			TempkeyHash.putAll(keyHash);
			
	        Everything.put("auctioncounterOB", auctioncounter);
	        Everything.put("ItemHashOB", TempItemHash);
	        Everything.put("ItemRemovedHashOB", TempItemRemovedHash);
	        Everything.put("UserHashOB", TempUserHash);
	        Everything.put("keyHashOB", TempkeyHash);
	        return Everything;
	    }
	/**
	 * Method to respond to any challanges called by a client
	 * two random numbers are generated by the server and put in a array
	 * these numbers are then stored in the hash table to keep track of challanges being envoked by clients
	 * creates a cipher using RSA and sets it to encrypt mode using the key found in the keyhash table (Clients public key)
	 * encrypts the array with the cipher and stores it in a sealed object, this is then sent back to the user
	 * 
	 */
	public int challengeReplica(String username) {
		try {
			return 1312;
		}
		catch( Exception E) {
			
		}
		return 0;
		
	}
	/**
	 * Accepts the clients challange and signs a number with its private key, this is then sent back to the user
	 * @param so
	 * @param userID
	 * @return
	 * @throws InterruptedException
	 */
	public SignedObject acceptchallangeReplica(int so, String userID) throws InterruptedException {
		try {
			lock.getlock();
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initSign(privateKey);
			
			SignedObject SignOb = new SignedObject(so, privateKey, sig);
			lock.unlock();
			return SignOb;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error: Cant get signature isntance");
		
		} catch (InvalidKeyException e) {
			System.out.println("Error: Invalid Key");
		}catch (IOException e) {
			System.out.println("Error: Can't find key");
		} catch (SignatureException e) {
			System.out.println("Error: Signature Exception");
		}
		return null;
	}
	/**
	 * Save all users passwords to a file, if the file exists then go to the next one
	 */
	public void saveobjectReplica() {
		
		try {
			Enumeration<SealedObject> e = UserHash.elements();
			Enumeration<String> u = keyHash.keys();
			while (e.hasMoreElements()) {
				SealedObject s = e.nextElement();
				String username = u.nextElement();
				File f = new File(current+"/ServerKeys"+username+"_Sealedobject");
				if(!f.isFile()) {
					f.createNewFile();
					ObjectOutputStream userstream = new ObjectOutputStream(new FileOutputStream(current+"/ServerKeys/"+username+"_Sealedobject"));
					userstream.writeObject(s);
					userstream.close();
					System.out.println(username+ "s password has been encrypted and has been saved sucesfully");
				}
				else {
					System.out.println("Object already saved");
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Error: Cant find password file");
		} catch (IOException e) {
			System.out.println("Error: Cant load password file");
		}
	}
	/**
	 * Saves the users password, if already saved then skip
	 * @param UserId
	 */
 public void saveouserbjectReplica(String UserId) {
		try {
			SealedObject s = UserHash.get(UserId);
			File f = new File(current+"/ServerKeys"+UserId+"_Sealedobject");
			if(!f.isFile()) {
				f.createNewFile();
				ObjectOutputStream userstream = new ObjectOutputStream(new FileOutputStream(current+"/ServerKeys/"+UserId+"_Sealedobject"));
				userstream.writeObject(s);
				userstream.close();
				System.out.println(UserId+ " password has been encrypted and has been saved sucesfully");
			}
			else {
				System.out.println("Object has already been saved");
			}
		} catch (FileNotFoundException e) {
			System.out.println("Error: Cant find password file");
		} catch (IOException e) {
			System.out.println("Error: Cant load password file");
		}
	}
	/**
	 * Is called by the user once they ahve received and decrypted the challange sucesfully
	 * Creates a cipher instance uisng RSA and sets it to decypt using its own private key (the servers)
	 * This cipher is then used to decrpt the answer and is casted to an int
	 * This answer is then testetd against the numbers that it sent to the client
	 * IF correct it returns true, if not then return false.
	 * @throws InterruptedException 
	 */
	public boolean answerReplica(SignedObject signedanswer, String userID) throws NoSuchAlgorithmException, NoSuchPaddingException, InterruptedException {
		try {
			lock.getlock();
			Signature sig = Signature.getInstance("SHA256withRSA");
			boolean rightsig = signedanswer.verify(keyHash.get(userID), sig);
			lock.unlock();
			if(rightsig) {
				System.out.println(userID+" signiture has been verified");
				return true;
			}
			else {
				return false;
			}
		} catch (InvalidKeyException e) {
			System.out.println("Error: Invalid Key");
		} catch (SignatureException e) {
			System.out.println("Error: Cant get signature isntance");
		}
		return false;
		
	}
	/**
	 * Generates a pair of RSA keys with the lengh of 2048
	 */
	public void GenerateKeysReplica() throws NoSuchAlgorithmException {
		int keylength = 2048;
		this.keyGen = KeyPairGenerator.getInstance("RSA");
		this.keyGen.initialize(keylength);
	}
	/**
	 * Returns the servers public key
	 */
	public PublicKey getpublicReplica() {
		return publicKey;
	}
	/**
	 * gets a clients public key and stores it in the keyhash table
	 * @throws InterruptedException 
	 */
	public boolean givepublicReplica(PublicKey k, String userID) throws InterruptedException {
		lock.getlock();
		if(keyHash.get(userID) == null) {
			keyHash.put(userID, k);
		}
		lock.unlock();
		return true;
	}
	
	/**
	 * Seperates the key pairs into the private and public key
	 */
	public void createKeysReplica() {
		this.pair = this.keyGen.generateKeyPair();
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();
	}
	/**
	 * Saves all the public keys in the hashtables to files, if there is no file with that name then create one, otherwise check the next
	 */
	public void saveallkeysReplica() {
		try {
		FileOutputStream userstream = new FileOutputStream("./ServerKeys/userID's.txt");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(userstream));
		Enumeration<PublicKey> e = keyHash.elements();
		Enumeration<String> u = keyHash.keys();
		while (e.hasMoreElements()) {
			PublicKey s = e.nextElement();
			String username = u.nextElement();
			File f = new File(current+"/ServerKeys"+username+"_publicKey(server).pub");
			if(!f.isFile()) {
			f.createNewFile();
			FileOutputStream stream = new FileOutputStream(current+"/ServerKeys/"+username+"_publicKey(server).pub");
			byte[] keybytes = s.getEncoded();
			stream.write(keybytes);
			writer.write(username);
			writer.newLine();
			System.out.println(username+" has been saved sucesfully");
			stream.close();
			}
			else {
				System.out.println("Object has already been saved");
			}
		}
		writer.close();
		userstream.close();
		saveserverkeysReplica();
		}catch(Exception e){
			System.out.println("Failed to save user keys");
		}
	}
	/**
	 * Method to save a new users key
	 * Sees if userID is a file, if not then created a new one
	 * Sees if the user has already had its key saved by one of the other repilcas, if not then create one and wrte to it
	 * @param UserID
	 */
	public void saveruserkeyReplica(String UserID) {
		
		try {
			File f = new File(current+"/ServerKeys/userID's.txt");
			if(!f.isFile()) {
				f.createNewFile();
			}
				File f2 = new File(current+"/ServerKeys"+UserID+"_publicKey(server).pub");
				if(!f2.isFile()) {
					f2.createNewFile();
					FileWriter writer = new FileWriter(f.getAbsoluteFile(), true);
					BufferedWriter buffer = new BufferedWriter(writer);
					PublicKey s = keyHash.get(UserID);
					FileOutputStream stream = new FileOutputStream(current+"/ServerKeys/"+UserID+"_publicKey(server).pub");
					byte[] keybytes = s.getEncoded();
					stream.write(keybytes);
					buffer.write(UserID);
					buffer.newLine();
					System.out.println(UserID+" has been saved sucesfully");
					stream.close();
					buffer.close();
					writer.close();
				}
			else {
				System.out.println("Object is already created");
			}
			}catch(Exception e){
				System.out.println("Failed to save user keys");
			}
	}
	/**
	 * Saves the servers public and private keys to a file in the key directory
	 * @throws IOException
	 */
	public void saveserverkeysReplica()throws IOException {
		byte[] keybytes = publicKey.getEncoded();
	    FileOutputStream stream = new FileOutputStream(current+"/ServerKeys/Server_publicKey.pub");
	    stream.write(keybytes);
	    stream.close();
	    
	    byte[] pkeybytes2 = privateKey.getEncoded();
	    FileOutputStream pstream = new FileOutputStream(current+"/ServerKeys/Server_privateKey.key");
	    pstream.write(pkeybytes2);
	    pstream.close();
	}
	/**
	 * Method to load all the keys in the server key directory, only run if its the first server activated
	 * Gets a userID file and reads it line by line
	 * for each user then it loads the public key and puts it in the keyhash 
	 * then loads the users encyrpted password and puts it in the userhash
	 */
	public void loadallkeysReplica(){
		try {
			if(new File(current+"/ServerKeys/userID's.txt").isFile()) {
			BufferedReader reader = new BufferedReader(new FileReader(current+"/ServerKeys/userID's.txt"));
			String username = reader.readLine();
			while (username != null) {
				byte[] bytes =  Files.readAllBytes(Paths.get(current+"/ServerKeys/"+username+"_publicKey(server).pub"));
				X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
				KeyFactory factory = KeyFactory.getInstance("RSA");
				PublicKey userpublickey = factory.generatePublic(spec);
				keyHash.put(username, userpublickey);
				ObjectInputStream userstream = new ObjectInputStream(new FileInputStream(current+"/ServerKeys/"+username+"_Sealedobject"));
				SealedObject so = (SealedObject) userstream.readObject();
				userstream.close();
				UserHash.put(username, so);
				System.out.println(username+ " keys and password has been loaded sucesfully");
				username = reader.readLine();
			}
			reader.close();
			}
		} catch (InvalidKeySpecException e) {
			System.out.println("Somethigs gone wrong while loading the public keys, please check to see if the key file is still aviablable to load");
		}
		catch (NoSuchAlgorithmException e1) {
			System.out.println("Somethigs gone wrong while loading the public keys, please check to see if the key file is still aviablable to load");
		}
		catch (IOException e) {
			System.out.println("Somethigs gone wrong while loading the public keys, please check to see if the key file is still aviablable to load");
		} catch (ClassNotFoundException e) {
			System.out.println("Somethigs gone wrong while loading the public keys, please check to see if the key file is still aviablable to load");
		}
		
	}
	/**
	 * Calls each of the load functions for the public and private keys
	 */
	public void loadkeysReplica() {
		loadprivateReplica();
		loadpublicReplica();
	}
	/**
	 * Loads the private key from a file, reads all bytes from a file into an array of bytes
	 * a sepc for PKCS8 encoding is then made using the bytes
	 *  Creates a new isntance of RSA
	 *  Using the isntance a private key is created from the spec
	 */
	public void loadprivateReplica() {
		try {
			byte[] bytes =  Files.readAllBytes(Paths.get(current+"/ServerKeys/Server_privateKey.key"));
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			privateKey = factory.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			System.out.println("Somethigs gone wrong while loading the private key, please check to see if the key file is still aviablable to load");
		}
		catch (NoSuchAlgorithmException e1) {
			System.out.println("Somethigs gone wrong while loading the private key, please check to see if the key file is still aviablable to load");
		}
		catch (IOException e) {
			System.out.println("Somethigs gone wrong while loading the private key, please check to see if the key file is still aviablable to load");
		}
	}
	/**
	 * Loads the public key from a file, reads all bytes from a file into an array of bytes
	 * a sepc for X509 encoding is then made using the bytes
	 *  Creates a new isntance of RSA
	 *  Using the isntance a public key is created from the spec
	 */
	public void loadpublicReplica() {
		try {
			byte[] bytes =  Files.readAllBytes(Paths.get(current+"/ServerKeys/Server_publicKey.pub"));
			X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			publicKey = factory.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			System.out.println("Somethigs gone wrong while loading the public key, please check to see if the key file is still aviablable to load");
		}
		catch (NoSuchAlgorithmException e1) {
			System.out.println("Somethigs gone wrong while loading the public key, please check to see if the key file is still aviablable to load");
		}
		catch (IOException e) {
			System.out.println("Somethigs gone wrong while loading the public key, please check to see if the key file is still aviablable to load");
		}
		
	}
	/**
	 * Method to create a listing, takes all the values needed for the auction and then creates an instance of the ItemSale class containing them
	 * This is then put into the hash table with the auction ID as the key. The auction ID is then sent back to the user
	 * @param startPrice - The starting price of the auction
	 * @param minPrice - The reserve price
	 * @param description - The items description
	 * @param sellerID - The sellers ID
	 * @return AuctionID - The auction ID
	 * @throws InterruptedException 
	 */
	public int createlistingReplica(int startPrice, int minPrice, String description, String sellerID) throws RemoteException, InterruptedException {
		lock.getlock();
		ItemSale sale = new ItemSale(startPrice, minPrice, description, sellerID, auctioncounter);
		ItemHash.put(auctioncounter, sale);
		auctioncounter++;
		lock.unlock();
		System.out.println("Auction ID: "+ (auctioncounter-1)+" has been created");
		return auctioncounter-1;
	}
	/**
	 * Method to get all the listings from the server, called by the client
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings
	 * @return listofitemdata - List of listings  
	 */
	public ArrayList<ArrayList<String>> getlistingsReplica() throws RemoteException {
		int amount = 0;
		ArrayList<ArrayList<String>> listofitemdata = new ArrayList<>();
		Enumeration<ItemSale> e = ItemHash.elements();
		while (e.hasMoreElements()) {
			ItemSale s = e.nextElement();
			ArrayList<String> itemvalues = new ArrayList<>();
			itemvalues.add(Integer.toString(s.getauctionID()));
			itemvalues.add(Integer.toString(s.getstartPrice()));
			itemvalues.add(Integer.toString(s.gethighestbid()));
			itemvalues.add(s.getDescription());
			itemvalues.add(String.valueOf(s.getstatus()));
			amount++;
			listofitemdata.add(itemvalues);
		}
		if (amount == 0) {
			System.out.println("No listing found");
			return null;
		}
		else {
			return listofitemdata;
		}
	}
	/**
	 * Checks to see if the username is avaible or if its already been taken
	 * @param sellerID
	 * @return
	 * @throws InterruptedException
	 */
	public boolean checkusernameReplica(String sellerID) throws InterruptedException {
		System.out.println("Checking username");
		lock.getlock();
		SealedObject t = UserHash.get(sellerID);
		lock.unlock();
		if(t == null) {
			return true;
		}
		return false;
	}
	/**
	 * Registers the client ID into the User List
	 * @param SellerID - The users ID
	 * @throws InterruptedException 
	 */
	public int registerclientIDReplica(String sellerID, SealedObject Password) throws RemoteException, InterruptedException {
		System.out.println("Registering client");
		lock.getlock();
		SealedObject t = UserHash.get(sellerID);
		if(t == null) {
			UserHash.put(sellerID, Password);
			System.out.println(sellerID+" has succesfully been registered in the client list");
			saveruserkeyReplica(sellerID);	
			saveouserbjectReplica(sellerID);
			System.out.println(sellerID+" has been backed up");
			lock.unlock();
			return 1;
		}
		System.out.println(sellerID+" is already registered in the client list");	
		lock.unlock();
		return 0;
	}
	/**Checks the users login details to see if the detials entered are right
	 * @param SellerID - The users ID
	 * @param Password - The users Password
	 * @throws InterruptedException 
	 */
	public int checklogindetailsReplica(String sellerID, SealedObject Password) throws RemoteException, InterruptedException {
		System.out.println("Checking details");
		lock.getlock();
		SealedObject passwordcheck = UserHash.get(sellerID);
		Cipher DEScipher;
		String answer = "";
		String actual = "";
		try {
			DEScipher = Cipher.getInstance("RSA");
			DEScipher.init(Cipher.DECRYPT_MODE, privateKey);
			answer = (String) passwordcheck.getObject(DEScipher);
			actual = (String) Password.getObject(DEScipher);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error: Cant detectalgorithm RSA");
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			System.out.println("Padding Error");
		} catch (InvalidKeyException e) {
			System.out.println("Error: Invalid Key");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Invalid Class");
		} catch (IllegalBlockSizeException e) {
			System.out.println("Block Size Error");
		} catch (BadPaddingException e) {
			System.out.println("Padding Error");
		} catch (IOException e) {
			System.out.println("Error getting object");
		}
		lock.unlock();
		if(answer != null) {
			if(answer.equals(actual)) {
				System.out.println("Entered password for "+sellerID+" is correct");	
				return 2;
			}
			else {
				System.out.println("Entered password for "+sellerID+" is incorrect");	
				return 1;
			}
		}
		System.out.println("There is no username registered with "+sellerID);	
		return 0;
	}
	/**
	 * Method to get the listings of a client. Takes the user ID as a parameter
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Checks if the userID supplied by the client is the same of as the auction.
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings
	 * @param UserID - The usersID
	 * @throws InterruptedException 
	 */
	public ArrayList<ArrayList<String>> getmylistingsReplica(String userID) throws RemoteException, InterruptedException {
		lock.getlock();
		int amount = 0;
		ArrayList<ArrayList<String>> listofitemdata = new ArrayList<ArrayList<String>>();
		Enumeration<ItemSale> e = ItemHash.elements();
		while (e.hasMoreElements()) {
			ItemSale s = e.nextElement();
			System.out.println(s.getsellerID()+" has retreived their listings");
			if(s.getsellerID().equals(userID)) {
				ArrayList<String> itemvalues = new ArrayList<>();
				itemvalues.add(Integer.toString(s.getauctionID()));
				itemvalues.add(Integer.toString(s.getstartPrice()));
				itemvalues.add(Integer.toString(s.gethighestbid()));
				itemvalues.add(s.getDescription());
				itemvalues.add(String.valueOf(s.getstatus()));
				amount++;
				listofitemdata.add(itemvalues);
			}
		}
		lock.unlock();
		if (amount == 0) {
			System.out.println("No listing found with user ID: ("+userID+")");
			return null;
		}
		else {
			return listofitemdata;
		}
	}
	/**
	 * Method to allow the user to bid on an item. Takes the auction ID, bid, UserID and the users email as parameters
	 * Uses the get method in the hastable to retrieve the item with that auctionID, if it returns null then return error code 3
	 * if not then place the bid using the placebid method. Returns a error message which is translated client side to text.
	 * If it fails the try statement then catch and return 0
	 * @param auctionID - The auction ID to bid on
	 * @param bid - The bidding amount
	 * @param bidderID - The bidders ID
	 * @param bidderemail - The bidders email
	 * @return ErrorCode
	 */
	public int bidReplica(int auctionID, int bid, String bidderID, String bidderemail) throws RemoteException {
		try {
			lock.getlock();
			ItemSale item = ItemHash.get(auctionID);
			lock.unlock();
			if (item == null) {
				System.out.println("No listing the requested auctionID ("+auctionID+")");
				return 3;
			}
			if(item.getsellerID().equals(bidderID)) {
				System.out.println("Bidder is also the seller");
				return 4;
			}
			int check = item.placebid(bid, bidderID, bidderemail);
			return check;
		}catch(Exception e){
			System.out.println("Server Error: Can't retreive item");
		}
		return 0;
	}
	/**
	 * Method to get the users bids.
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Checks if the userID supplied by the client is the same as the higehst bidder in the auction.
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings with the users bids
	 * @param userID - The users ID
	 * @return listofitemdata - The list of items with the users Bids on
	 * @throws InterruptedException 
	 */ 
	public ArrayList<ArrayList<String>> getbidsReplica(String userID) throws RemoteException, InterruptedException {
		int amount = 0;
		ArrayList<ArrayList<String>> listofitemdata = new ArrayList<ArrayList<String>>();
		lock.getlock();
		Enumeration<ItemSale> e = ItemHash.elements();
		while (e.hasMoreElements()) {
			ItemSale s = e.nextElement();
			System.out.println(s.gethighestbidID());
			if(s.gethighestbidID().equals(userID)) {
				ArrayList<String> itemvalues = new ArrayList<>();
				itemvalues.add(Integer.toString(s.getauctionID()));
				itemvalues.add(Integer.toString(s.getstartPrice()));
				itemvalues.add(Integer.toString(s.gethighestbid()));
				itemvalues.add(s.getDescription());
				itemvalues.add(String.valueOf(s.getstatus()));
				amount++;
				listofitemdata.add(itemvalues);
			}
		}
		lock.unlock();
		if (amount == 0) {
			System.out.println("No bids found with user ID: ("+userID+")");
			return null;
		}
		else {
			return listofitemdata;
		}
	}
	/**
	 * Method to search by auction ID
	 * uses the hashtable get function to retreive the itemsale class using the auction ID
	 * if it is null then return null to the users else get the auctionID, start price, highest bid, Description and status
	 * @param auctionID - The ID of the auction wanted
	 * @return itemvalues - The auctions values to be printed out to the user
	 * @throws InterruptedException 
	 */
	public ArrayList<String> searchauctionReplica(int auctionID) throws RemoteException, InterruptedException {
		lock.getlock();
		ItemSale s = ItemHash.get(auctionID);
		lock.unlock();
		if(s != null) {
			ArrayList<String> itemvalues = new ArrayList <String>();
			itemvalues.add(Integer.toString(s.getauctionID()));
			itemvalues.add(Integer.toString(s.getstartPrice()));
			itemvalues.add(Integer.toString(s.gethighestbid()));
			itemvalues.add(s.getDescription());
			itemvalues.add(String.valueOf(s.getstatus()));
			return itemvalues;
		}
		else {
			return null;
		}
	}
	/**
	 * Method to close a auction
	 * Searches the auctionID supplied, if it returns null then return error code 5 as there is no auction with that ID
	 * Then checks if the sellerID in them item instance is the same as the userID trying to remove it, if so start the closing process
	 * Calls the close function in the item instance which sets the auction to close and returns a boolean value if succesfull
	 * Then try's to remove the auction from the Item hashtable and adds it to the closed item hashtable. 
	 * If the highest bid is less than the reserve price then return error code 4 otherwise return 1 which indicates the auction winner
	 * @param userID - The users ID
	 * @param auctionID - The wanted auctions ID
	 * @return ErrorCode
	 * @throws InterruptedException 
	 */
	public int closebidReplica(int auctionID, String userID) throws RemoteException, InterruptedException {
		lock.getlock();
		ItemSale item = ItemHash.get(auctionID);
		lock.unlock();
		if (item == null) {
			System.out.println("No auction with that ID (Highest ID: "+ItemHash.size());
			return 5;
		}
		if(item.getsellerID().equals(userID)) {
			System.out.println("Seller confirmed");
			boolean result = item.close();
			if (result) {
				System.out.println(item.getauctionID()+ " Has ended");
				try {
					ItemRemovedHash.put(auctionID, item);
					ItemHash.remove(auctionID);
					System.out.println(ItemRemovedHash.get(auctionID).getauctionID()+ " has been moved to past auctions and removed from current succesfully");
					
					if (item.gethighestbid() >= item.getminPrice()) {
						System.out.println("Highest bidder has bid above the minimum price (High bid: "+item.gethighestbid()+")");
						return 1;
					}
					else {
						System.out.println("Highest bidder bid under the minimum price (High bid: "+item.gethighestbid()+") (Min bid: "+item.getminPrice()+")");
						return 4;
					
					}
					
				}catch (Exception e) {
					System.out.println("An error has occured moving item from live auctions to past auctions");
					return 2;
				}
			}
			else {
				System.out.println("Failed to close auctionID: "+item.getauctionID());
				return 2;
			}
		}
		else {
			System.out.println("User trying to remove the auction is not the seller (ID: "+userID+") (Seller ID:"+item.getsellerID()+")");
			return 3;
		}
	}
	/**
	 * Method to get data from a clossed auction.
	 * Searchs for the auctionID in the removed hash table , if it finds one then send a list to the user containing the auctionID, strat price, bid , description
	 * @param auctionID - The auctions ID
	 * @return itemvalues - A list of values of the searched auction to be displayed to the user
	 * @throws InterruptedException 
	 */
	public ArrayList<String> getcloseddetailsReplica(int auctionID) throws RemoteException, InterruptedException {
		lock.getlock();
		ItemSale s = ItemRemovedHash.get(auctionID);
		lock.unlock();
		if(s != null) {
			ArrayList<String> itemvalues = new ArrayList <String>();
			itemvalues.add(Integer.toString(s.getauctionID()));
			itemvalues.add(Integer.toString(s.getstartPrice()));
			itemvalues.add(Integer.toString(s.gethighestbid()));
			itemvalues.add(s.getDescription());
			itemvalues.add(String.valueOf(s.getstatus()));
		
			return itemvalues;
		}
		else {
			return null;
		}
	}
	/**
	 * Method is called by the client after removing a auction from open to closed
	 * Searches for the auctionID just moved and returns a list containing the highest bidders details
	 * @param auctionID - The auctions ID
	 * @return itemvalues - The values the user requested (higehst bidder details)
	 * @throws InterruptedException 
	 */
	public ArrayList<String> getbuyerdetailsReplica(int auctionID) throws RemoteException, InterruptedException {
		lock.getlock();
		ItemSale s = ItemRemovedHash.get(auctionID);
		lock.unlock();
		if(s != null) {
			ArrayList<String> itemvalues = new ArrayList <String>();
			itemvalues.add(Integer.toString(s.gethighestbid()));
			itemvalues.add(s.gethighestbidID());
			itemvalues.add(s.gethighestbidemail());
			return itemvalues;
		}
		else {
			return null;
		}
	}
	/**
	 * Checks the connection and returns true if is connected
	 */
	public boolean checkconnected() throws RemoteException {
		return true;
	}
	

}
