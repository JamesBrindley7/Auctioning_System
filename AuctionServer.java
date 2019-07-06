import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

import org.jgroups.Channel;
import org.jgroups.JChannel;
/**
 * This is a basic auctioning program using RMI
 * @author James Brindley
 *
 */
public class AuctionServer {
	/**
	 * Constructor for the auction server
	 * Creates a new instance of server implmentation, registers the rmi name and rebinds it to the server
	 */
	public AuctionServer() {
		try {
			//Creates a new jgroup channel and connects to "ServerReplicas", this is then passed to the server implementation
			Channel chan = new JChannel();
            chan.connect("ServerReplicas");
            
			ServerInterface Impl = new ServerImpl(chan);
			ServerInterface Server = (ServerInterface) UnicastRemoteObject.exportObject(Impl, 0);
			Naming.rebind("rmi://localhost/Auction", Server);
			System.out.println("Server running...");
		}
		catch (Exception e) {
			System.out.println("Server Error: Failed to start" + e);
		}
	}
	/**
	 * Creates a new instance of the Auction Server
	 * @param args
	 */
	public static void main(String args[]) {
		new AuctionServer();
	}
}
