import java.util.Scanner;
/**
 * This program provides reliability over an unreliable UDP protocol.
 * 
 * @author vhineshravi
 */

public class fcntcp 
{
	/**
	 * This program calls the client or sever based on command line input.
	 * @param args command line args
	 */
	public static void main(String[] args) 
	{
		Scanner sc = new Scanner(System.in);
		if(args[0].equals("-s"))
		{
			udpServer server = new udpServer(args);
		}
		else
		{
			udpClient client = new udpClient(args);
		}
	}

}
