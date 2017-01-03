import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
/**
 * This class stores the packets received from client
 * @author vhineshravi
 *
 */
class pack
{
	byte pkt[];
	public pack(int len,byte [] p)
	{
		pkt = new byte[len];
		System.arraycopy(p, 0, pkt, 0, p.length);
	}
	byte [] getpkt()
	{
		return pkt;
	}
}
/**
 * The server side of reliable data transfer over unreliable protocol.
 * @author vhineshravi
 *
 */
public class udpServer 
{
    int port; // stores port of client
    int q=0;  // decides whether to print miscellaneous info while transferring file 
	int segment; // No of segments to be sent
	long filelength; // length of file to be received
	BufferedWriter bufferedWriter;
	FileOutputStream fos = null;
	File file;
	ArrayList<Integer> window= new ArrayList<Integer>(); // receiver window
	pack p[]; // stores the packets received from client
	public udpServer(String args[])
	{
		extract(args);
		receive_packet();
	}
	/**
	 * This function extracts the command line arguments.
	 * @param args
	 */
	public void extract(String args[])
	{
		for (int i=0;i<args.length;i++)
		{
			if(args[i].equals("-q"))
			{
				q = 1;
			}
			else if(i==(args.length-1))
			{
				port=Integer.parseInt(args[i]);
			}
		}
	}
	/**
	 * This function checks if the packet received is not corrupted 
	 * @param pt packet
	 * @param p length of data in packet
	 * @return
	 */
	public boolean packet_check(byte[] pt,int p)
	{
		byte test[]=new byte[p];
		System.arraycopy(pt, 32, test, 0, 1024);
		MessageDigest digest;
		try 
		{
		digest = MessageDigest.getInstance("md5");
		
		digest.update(test);
		byte hashFunction[] = digest.digest();
		byte t []= new byte[16];
		System.arraycopy(pt, 16, t, 0, 16);
		// comparing the generated checksum with the checksum in the packet
		if(Arrays.equals(t, hashFunction))
		{
			return true;
		}
		else
		{
			return false;
		}
		} catch (NoSuchAlgorithmException e) 
		{
			
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * This function receives the packet from the client
	 */
	public void receive_packet()
	{
        // initializing sockets and preparing to receive
		DatagramSocket serverSocket = null;
		try 
		{
		 serverSocket = new DatagramSocket(port);
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		byte [] pkt = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(pkt,pkt.length);
		DatagramPacket sendclientPacket = null;
		DatagramPacket sendip = null;
		// receiving initial handshake
		try 
		{
		  serverSocket.receive(receivePacket);
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		pkt = receivePacket.getData();
		if(q==0)
		{
		System.out.println(new String(pkt));
		}
		String info[] = ( new String( pkt ).trim() ).split(" ");
		segment = Integer.parseInt(info[1]);
		filelength= Long.parseLong(info[0]);
		int mod = (int)filelength % 1024;
		p = new pack[segment];
		
		InetAddress clientip = receivePacket.getAddress();
		int clientPort       = receivePacket.getPort();
		String s_info ="ok";
		byte[]b=s_info.getBytes();
		// sending response for handshake
		DatagramPacket sendP = new DatagramPacket(b,b.length, clientip ,clientPort);
		try 
		{
			serverSocket.send(sendP);
		} catch (IOException e1) 
		{
			e1.printStackTrace();
		}
		int i=0;
		// receives the packets from client after initial handshake
		while(true)
		{
			int l;
			int c=0;
			byte receiveByte[] = new byte[1024+32];
			DatagramPacket rec_packt = new DatagramPacket( receiveByte , receiveByte.length );
			try 
			{
				serverSocket.receive(rec_packt);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
			String message = new String(Arrays.copyOfRange(receiveByte, 0, 16));
			message = message.trim();
			if(q==0)
			{
			System.out.println("packet: "+ message);
			}
			int t;
			int n = Integer.parseInt(message);
			if( n != segment-1 || mod == 0 )
			{
				l=1024+32;
				t=1024;
			}
			else
			{
				l=mod+32;
				t=mod;
			}
			if(packet_check(receiveByte,t))
			{
				// checks for duplicate packet
				if(window.contains(n))
				{
				 c=1;
			
				}
				else
				{
					p[n]=new pack(l,receiveByte);
					window.add(n);
				}
			
			}
			else
			{
				n=n-1;
			}
			String no;
			if(c==1)
			{
			no=window.get(window.size()-1)+"";	
			}
			else
			{
			no= n+"";
			}
			byte a[]=no.getBytes();
			// sending acknowledgement for received packet
			DatagramPacket sendPacket = new DatagramPacket(a,a.length, clientip ,clientPort);
			try 
			{
				serverSocket.send(sendPacket);
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
			if (window.size()==(segment))
			{
				break;
			}
			i++;
		}
		file = new File("output");
		try 
		{
			fos=new FileOutputStream(file);
		// writes the packets received to the output file
		for( int j = 0; j < segment; j++ )
		{
			int l;
			byte temp[];
			if( j != segment-1 || mod == 0 )
			{
				l=1024;
				temp = new byte[l];
				System.arraycopy(p[j].getpkt(), 32, temp, 0, 1024);
			}
			else
			{
				l=mod;
				temp = new byte[l];
				System.arraycopy(p[j].getpkt(), 32, temp, 0, mod);
			}
			MessageDigest digest = MessageDigest.getInstance("md5");
			digest.update(temp);
			byte hashFunction[] = digest.digest();
			byte t []= new byte[16];
			System.arraycopy(p[j].getpkt(), 16, t, 0, 16);
			//System.out.println("no "+ new String(t));
			fos.write(temp);
		}
		
		fos.flush();
		}
		catch (Exception e) 
		{
			
			e.printStackTrace();
		}
		checksum("output");
		
	}
	/**
	 * This function calculates the checksum of the file generated.
	 * @param filename the filename output file for which the checksum is to be calculated.
	 */
	public void checksum(String filename)
	{
		try
		{
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    File file = new File(filename);
		    FileInputStream fis = new FileInputStream(file);
		    // byte array to store the data read from generated file
		    byte[] data = new byte[(int) file.length()];
		    
		    int nread = 0; 
		    //reads the generated file and stores in data
		    while ((nread = fis.read(data)) != -1) 
		    {
		      md.update(data, 0, nread);
		    };
		 //MD5 checksum for file data
		    byte[] mdchecksum = md.digest();
		   
		    StringBuffer sb = new StringBuffer("");
		 //convert the checksum to hex string   
		    for (int i = 0; i < mdchecksum.length; i++) 
		    {
		    	sb.append(Integer.toString((mdchecksum[i] & 0xff) + 0x100, 16).substring(1));
		    }
		   
		    System.out.println("Checksum in hex : " + sb.toString());
		    
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		    
	}
	
}
