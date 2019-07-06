import java.io.Serializable;
/**
 * This is a basic auctioning program using RMI
 * @author James Brindley
 *
 */
public class ItemSale implements Serializable{
	/**
	 * Stores the start price of the sale
	 */
	private int startPrice;
	/**
	 * Stores the miniumum price of the sale
	 */
	private int minPrice; 
	/**
	 * Stores the description of the sale
	 */
	private String description;
	/**
	 * Stores the highest bid
	 */
	private int highestbid = 0; 
	/**
	 * Stores the highest bidder ID
	 */
	private String highestbidID = "";
	/**
	 * Stores the sellers ID
	 */
	private String sellerID; 
	/**
	 * Stores the auction ID
	 */
	private int auctionID;
	/**
	 * Stores the status of the sale
	 */
	private boolean active;
	/**
	 * Stores the highest bidders email
	 */
	private String highestbidemail = "";	
	/**
	 * Consturctor for the item sale
	 * @param startPrice - The starting price
	 * @param minPrice - The reserve price
	 * @param description - The description for the item
	 * @param sellerID - The sellers username
	 * @param auctionID - The auction ID
	 */
	public ItemSale(int startPrice, int minPrice, String description, String sellerID, int auctionID) {
		setstartPrice(startPrice);
		setminPrice(minPrice);
		setDescription(description);
		setsellerID(sellerID);
		setauctionID(auctionID);
		setactive(true);
	}
	/**
	 *Method to place a new bid on the item
	 * //1 means successful
	 * //2 means there is a big higher or the bid placed was bellow starting price 
	 * @param bid - The bid
	 * @param bidderID - The bidders ID
	 * @param email - The bidders email
	 * @return - The error code to display if succesfull
	 */
	public int placebid(int bid, String bidderID, String email) {
		int check = checkbid(bid);
		if (check == 1) {
			sethighestbid(bid);
			sethighestbidID(bidderID);
			sethighestbidemail(email);
			System.out.println("Bid succesfully placed");
		}
		else if (check == 2) {
			System.out.println("Bid for "+getauctionID()+" was unsuccessful as there is a bid that is higher than or equal to £ "+ bid);
		}
		return check;
	}
	/**
	 * Checks if the bid given is higher than the current bid and higher than the start price
	 * @param bid - The bid
	 * @return returns 1 if it is higher and 2 if the bid is less than
	 */
	public int checkbid(int bid) {
		if (bid > gethighestbid() && bid >= getstartPrice()) {
			return 1;
		}
		else {
			return 2;
		}
	}
	/**
	 * closes the auction and sets active to false
	 * @return returns true when completed
	 */
	public boolean close() {
		setactive(false);
		return true;
	}
	/**
	 * 
	 * Getter for the start price of an item
	 * @return start price 
	 */
	public int getstartPrice() {
		return startPrice;
	}
	/**
	 * Setter for the start price of an item
	 * @param value
	 */
	public void setstartPrice(int value) {
		 this.startPrice = value;
	}
	/**
	 * Getter for the minimum price of an item
	 * @return minimum price
	 */
	public int getminPrice() {
		return minPrice;
	}
	/**
	 * Setter for the minimum price of an item
	 * @param value
	 */
	public void setminPrice(int value) {
		 this.minPrice = value;
	}
	/**
	 * Getter for the description of an item
	 * @return item description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * Setter for the item description
	 * @param value
	 */
	public void setDescription(String value) {
		 this.description = value;
	}
	/**
	 * Getter for the highest bid 
	 * @return highest bid
	 */
	public int gethighestbid() {
		return highestbid;
	}
	/**
	 * Setter for the highest bid
	 * @param value
	 */
	public void sethighestbid(int value) {
		 this.highestbid = value;
	}
	/**
	 * Getter for the highest bidder id
	 * @return highest bidder id
	 */
	public String gethighestbidID() {
		return highestbidID;
	}
	/**
	 * Setter for the highest bidders id
	 * @param value
	 */
	public void sethighestbidID(String value) {
		 this.highestbidID = value;
	}
	/**
	 * Getter for the highest bidder's email
	 * @return highest bidders email
	 */
	public String gethighestbidemail() {
		return highestbidemail;
	}
	/**
	 * Setter for the highest bidder's email
	 * @param value
	 */
	public void sethighestbidemail(String value) {
		 this.highestbidemail = value;
	}
	/**
	 * Getter for the sellers ID
	 * @return
	 */
	public String getsellerID() {
		return sellerID;
	}
	/**
	 * Setter for the seller ID
	 * @param value
	 */
	public void setsellerID(String value) {
		 this.sellerID = value;
	}
	/**
	 * Getter for the auction ID
	 * @return
	 */
	public int getauctionID() {
		return auctionID;
	}
	/**
	 * Setter for the auction ID
	 * @param value
	 */
	public void setauctionID(int value) {
		 this.auctionID = value;
	}
	/**
	 * Getter for the status of the auction
	 * @return
	 */
	public boolean getstatus() {
		return active;
	}
	/**
	 * Setter for the status of the auction
	 * @param value
	 */
	public void setactive(boolean value) {
		 this.active = value;
	}
	
}
