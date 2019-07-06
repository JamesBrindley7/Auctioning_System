/**
 * Simple lock to only allow one process into the critical section at one point
 * @author James Brindley
 *
 */
public class lock {
	
	public boolean locked = false;
	/**
	 * Method to aquire a lock so only one process can run at a time in the critical section
	 * @throws InterruptedException
	 */
	public synchronized void getlock() throws InterruptedException {
		while(locked){
            wait();
        }
		locked = true;
	}
	/**
	 * Method to unlock the lock so other processes can enter the critical section
	 * @throws InterruptedException
	 */
	public synchronized void unlock() {
	    locked = false;
	    notify();

	}
}
