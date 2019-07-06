import java.io.IOException;
import java.rmi.*;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignedObject;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
/**
 * This is a Advanced auctioning system implementaton using RMI and JGroups
 * @author James Brindley
 *
 */
public interface ServerInterface extends java.rmi.Remote{
	/**
	 * Interface to send back a challange for the client to sign
	 * @param username
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws Exception
	 */
	public int challenge(String username)
			throws java.rmi.RemoteException, Exception;
	/**
	 * nterface to check the clients username
	 * @param username
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws Exception
	 */
	public boolean checkusername(String username)
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to return the public key
	 * @return
	 * @throws java.rmi.RemoteException
	 */
	public PublicKey getpublic() 
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to add the users public key to the hash table
	 * @param k
	 * @param Username
	 * @return 
	 * @throws java.rmi.RemoteException
	 */
	public boolean givepublic(PublicKey k, String Username) 
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to check the answer given by the clients response
	 * @param signOb
	 * @param userID
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public boolean answer(SignedObject signOb, String userID)
			throws java.rmi.RemoteException, NoSuchAlgorithmException, NoSuchPaddingException , Exception;
	/**
	 * Interface for creating a listing
	 * @param startPrice
	 * @param minPrice
	 * @param description
	 * @param sellerID
	 * @return ErrorCodes
	 * @throws java.rmi.RemoteException
	 */
	int createlisting(int startPrice, int minPrice, String description, String sellerID)
		throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to get all listings
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<ArrayList<String>> getlistings()
		throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to register client ID when new account is created
	 * @param SellerID
	 * @param Password
	 * @return ErrorCodes
	 * @throws java.rmi.RemoteException
	 */
	int registerclientID(String SellerID, SealedObject Password)
		throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to check the login detials submitted
	 * @param SellerID
	 * @param Password
	 * @return ErrorCodes
	 * @throws java.rmi.RemoteException
	 */
	int checklogindetails(String SellerID, SealedObject Password)
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to get a certain users listings
	 * @param SellerID
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<ArrayList<String>> getmylistings(String SellerID)
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to place a bid
	 * @param auctionID
	 * @param bid
	 * @param bidderID
	 * @param bidderemail
	 * @return ErrorCodes
	 * @throws java.rmi.RemoteException
	 */
	int bid(int auctionID, int bid, String bidderID, String bidderemail) 
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to get listings in which the user has bidded on
	 * @param userID
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<ArrayList<String>> getbids(String userID) 
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to search for an auction with the auction ID supplied by the user
	 * @param auctionID
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<String> searchauction(int auctionID) 
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to close an auction
	 * @param auctionID
	 * @param userID
	 * @return ErrorCode
	 * @throws java.rmi.RemoteException
	 */
	int closebid(int auctionID, String userID)
		throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to check that the user is connected
	 * @return
	 * @throws java.rmi.RemoteException
	 */
	boolean checkconnected()
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to get details for closed auction
	 * @param auctionID
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<String> getcloseddetails(int auctionID)
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to get the buyer details for an auction
	 * @param auctionID
	 * @return ItemValues
	 * @throws java.rmi.RemoteException
	 */
	ArrayList<String> getbuyerdetails(int auctionID)
			throws java.rmi.RemoteException , Exception;
	/**
	 * Interface to send a signed object back to the client
	 * @param random
	 * @param userID
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws Exception
	 */
	public SignedObject acceptchallange(int random, String userID)
			throws java.rmi.RemoteException, Exception;
}
