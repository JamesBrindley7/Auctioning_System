import java.rmi.RemoteException;

import java.security.PublicKey;
import java.security.SignedObject;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SealedObject;

import org.jgroups.*;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
/**
 * This is a Advanced auctioning system implementaton using RMI and JGroups
 * @author James Brindley
 *
 */

public class ServerImpl implements ServerInterface {
	/**
	 * The options used to send messages
	 */
	private RequestOptions options;
	/**
	 * The dispatcher to send the messages
	 */
    private RpcDispatcher dispacher;
    /**
     * The channel address
     */
    Address address;
    /**
     * The number of Replica servers running
     */
    public int ServerCount;
	
	/**
	 * Constructor for the server implementation, creates a dispatcher and gets the options being used
	 * @param chan
	 */
	public ServerImpl(Channel chan) {
		super();
		dispacher = new RpcDispatcher(chan, this);
	    address = chan.getAddress();
	    options = new RequestOptions(ResponseMode.GET_ALL, 5000, false);
	}
	/**
	 * Method to respond to any challanges called by a client
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @throws Exception z	
	 * 
	 */
	public synchronized int challenge(String username) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"challengeReplica",new Object[]{username}, new Class[]{String.class},options);
		List results = ResponceList.getResults();
		int server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
	
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
		
		
	}
	/**
	 * Method to respond to return a signed object after being called by a client
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @throws Exception z	
	 * 
	 */
	public synchronized SignedObject acceptchallange(int so, String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"acceptchallangeReplica",new Object[]{so, userID}, new Class[]{int.class, String.class},options);
		List results = ResponceList.getResults();
		SignedObject server_Response = (SignedObject) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Method to see if the signiture from the client is the correct user
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @throws Exception z	
	 * 
	 */
	public synchronized boolean answer(SignedObject signedanswer, String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"answerReplica",new Object[]{signedanswer, userID}, new Class[]{SignedObject.class, String.class},options);
		List results = ResponceList.getResults();
		boolean server_Response = (boolean) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
	   
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return false;
        }
		
	}
	/**
	 * Method to return the servers public key
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @throws Exception z	
	 * 
	 */
	public PublicKey getpublic() throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getpublicReplica",null, null,options);
		List results = ResponceList.getResults();
		PublicKey server_Response = (PublicKey) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return null;
        }
	}
	/**
	 * Method to store the cleints public key and store it in the keyhash
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @throws Exception z	
	 * 
	 */
	public synchronized boolean givepublic(PublicKey k, String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"givepublicReplica",new Object[]{k, userID}, new Class[]{PublicKey.class, String.class},options);
		List results = ResponceList.getResults();
		
		boolean server_Response = (boolean) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
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
	 * @throws Exception 
	 */
	@Override
	public synchronized int createlisting(int startPrice, int minPrice, String description, String sellerID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"createlistingReplica",new Object[]{startPrice, minPrice, description, sellerID}, new Class[]{int.class, int.class,String.class, String.class},options);
		List results = ResponceList.getResults();
		int server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Method to get all the listings from the server, called by the client
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @return listofitemdata - List of listings  
	 * @throws Exception 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<String>> getlistings() throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getlistingsReplica",null, null,options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<ArrayList<String>> server_Response = (ArrayList<ArrayList<String>>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
	}
	/**Method to check the username to see if its avaiblable
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 */
	public synchronized boolean checkusername(String sellerID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"checkusernameReplica",new Object[]{sellerID}, new Class[]{String.class},options);
		List results = ResponceList.getResults();
		boolean server_Response = (boolean) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Registers the client ID into the User List
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @param SellerID - The users ID
	 * @throws Exception 
	 */
	@Override
	public synchronized int registerclientID(String sellerID, SealedObject Password) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"registerclientIDReplica",new Object[]{sellerID, Password}, new Class[]{String.class, SealedObject.class},options);
		List results = ResponceList.getResults();
		int server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**Checks the users login details to see if the detials entered are right
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @param SellerID - The users ID
	 * @param Password - The users Password
	 * @throws Exception 
	 */
	@Override
	public synchronized int checklogindetails(String sellerID, SealedObject Password) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"checklogindetailsReplica",new Object[]{sellerID, Password}, new Class[]{String.class, SealedObject.class},options);
		List results = ResponceList.getResults();
		int server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Method to get the listings of a client. Takes the user ID as a parameter
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Checks if the userID supplied by the client is the same of as the auction.
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @param UserID - The usersID
	 * @throws Exception 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<String>> getmylistings(String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getmylistingsReplica",new Object[]{userID}, new Class[]{String.class},options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<ArrayList<String>>  server_Response = (ArrayList<ArrayList<String>>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
	}
	/**
	 * Method to allow the user to bid on an item. Takes the auction ID, bid, UserID and the users email as parameters
	 * Uses the get method in the hastable to retrieve the item with that auctionID, if it returns null then return error code 3
	 * if not then place the bid using the placebid method. Returns a error message which is translated client side to text.
	 * If it fails the try statement then catch and return 0
	 * Sends the request to all replicas in the network and gets the result, sends back the first responce depending on the sanity check
	 * A sanity check is there to check if all the responces are the same
	 * @param auctionID - The auction ID to bid on
	 * @param bid - The bidding amount
	 * @param bidderID - The bidders ID
	 * @param bidderemail - The bidders email
	 * @return ErrorCode
	 * @throws Exception 
	 */
	@Override
	public synchronized int bid(int auctionID, int bid, String bidderID, String bidderemail) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"bidReplica",new Object[]{auctionID, bid, bidderID, bidderemail}, new Class[]{int.class, int.class, String.class,String.class},options);
		List results = ResponceList.getResults();
		int  server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Method to get the users bids.
	 * Creates a list of a list to store the values called by the user in. Uses a enumeration of the hashtable to cycle through all of the auctions
	 * Checks if the userID supplied by the client is the same as the higehst bidder in the auction.
	 * Gets the auctionID, price, highest bid, description and status of the auction and stores it in a list which is then stored in another list 
	 * The list then gets sent to the user where the client side can print out this informaiton. Returns null if there are no listings with the users bids
	 * @param userID - The users ID
	 * @return listofitemdata - The list of items with the users Bids on
	 * @throws Exception 
	 */ 
	@Override
	@SuppressWarnings("unchecked")
	public ArrayList<ArrayList<String>> getbids(String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getbidsReplica",new Object[]{userID}, new Class[]{String.class},options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<ArrayList<String>>  server_Response = (ArrayList<ArrayList<String>>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
	}
	/**
	 * Method to search by auction ID
	 * uses the hashtable get function to retreive the itemsale class using the auction ID
	 * if it is null then return null to the users else get the auctionID, start price, highest bid, Description and status
	 * @param auctionID - The ID of the auction wanted
	 * @return itemvalues - The auctions values to be printed out to the user
	 * @throws Exception 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ArrayList<String> searchauction(int auctionID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"searchauctionReplica",new Object[]{auctionID}, new Class[]{int.class},options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<String>  server_Response = (ArrayList<String>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
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
	 * @throws Exception 
	 */
	@Override
	public synchronized int closebid(int auctionID, String userID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"closebidReplica",new Object[]{auctionID, userID}, new Class[]{int.class, String.class},options);
		List results = ResponceList.getResults();
		int  server_Response = (int) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
	}
	/**
	 * Method to get data from a clossed auction.
	 * Searchs for the auctionID in the removed hash table , if it finds one then send a list to the user containing the auctionID, strat price, bid , description
	 * @param auctionID - The auctions ID
	 * @return itemvalues - A list of values of the searched auction to be displayed to the user
	 * @throws Exception 
	 */
	 @SuppressWarnings("unchecked")
	@Override
	public ArrayList<String> getcloseddetails(int auctionID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getcloseddetailsReplica",new Object[]{auctionID}, new Class[]{int.class},options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<String>  server_Response = (ArrayList<String>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
	}
	/**
	 * Method is called by the client after removing a auction from open to closed
	 * Searches for the auctionID just moved and returns a list containing the highest bidders details
	 * @param auctionID - The auctions ID
	 * @return itemvalues - The values the user requested (higehst bidder details)
	 * @throws Exception 
	 */
	 
	 @SuppressWarnings("unchecked")
	public ArrayList<String> getbuyerdetails(int auctionID) throws Exception {
		RspList ResponceList = dispacher.callRemoteMethods(null,"getbuyerdetailsReplica",new Object[]{auctionID}, new Class[]{int.class},options);
		List results = ResponceList.getResults();
		if(results.size() > 0){
		ArrayList<String>  server_Response = (ArrayList<String>) results.get(0);
		checkServerStatus(ResponceList);
		
	    boolean sanity = checkSanity(ResponceList);
		
	    if (sanity) {
            System.out.println("Sanity check passed");
            return server_Response;
        } else
        {
            System.out.println("Sanity check failed");
            return server_Response;
        }
        }
        return null;
	}
	/**
	 * Checks the connection and returns true if is connected
	 */
	@Override
	public boolean checkconnected() throws RemoteException {
		return true;
	}
	/**
	 * Method to check the sanity of a list of responses. This checks to see if theres any errors returned, wiull return true if all the values are the same
	 * False if there are more than 1 distince values
	 * @param rspList
	 * @return
	 */
	public boolean checkSanity(RspList rspList) {

        List list_results = rspList.getResults();
        if (list_results.stream().distinct().count() <= 1) {
        	return true;
        }
        else {
        	return false;
        }
        
    }
	/**
	 * Checks all server statuses, sees the size of the list returned by the replicas. If its the same as the server count from the last request then print 
	 * all are function, if there is any change in the server count then print out how many more or less
	 * @param Responce_List
	 */
	private void checkServerStatus(RspList Responce_List) {
		//If the saved number of servers is the same as the number of responce
		//received then no servers have faileds
        if(ServerCount == (Responce_List.size()))
        {
            System.out.println("All servers registered are functioning ("+ServerCount+")");
        }
        //If the server count is larger than the responce number then some
        //servers have failed
        if (ServerCount > (Responce_List.size()))
        {
        	int numberfailed = ServerCount - Responce_List.size();
        	ServerCount = (Responce_List.size()); //Sets the server count to the new number of servers
            System.out.println(numberfailed+" servers have stoped functioning since last check");
        }
        if (ServerCount < (Responce_List.size()))
        {
        	int numberstarted = Responce_List.size() - ServerCount;
        	ServerCount = (Responce_List.size()); //Sets the server count to the new number of servers
            System.out.println(numberstarted+" servers have started making "+ServerCount+" in total");
        }
        
        //If there are no servers registered
        if (ServerCount == 0) 
        {
        	int numberstarted = Responce_List.size();
        	ServerCount = Responce_List.size();
            System.out.println(numberstarted+" servers have been started when 0 were avaiable");
        }
    }
	

}
