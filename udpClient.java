import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class stores the data in packets
 * @author vhineshravi
 */
class packet
{
	byte pkt[];
	public packet(int len,byte [] p)
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
 * This program is the client side reliable transfer protocol.
 * @author vhineshravi
 */
public class udpClient 
{
	String fileloc; // Stores the filename/file location
	long time = 1000; // stores the timeout delay 
	int q=0; // decides whether to print miscellaneous info while transferring file 
	InetAddress serverAddress; // ip address of server to connect to
	int serverPort; // port of server to connect to 
	int segment; // No of segments to be sent 
	FileInputStream fileInputStream; // to read the input file
    long fileLength;  // length of input file
    File file; 
	byte sendData[]; // stores handshake info to be sent
	int check;
	ArrayList<Integer> window= new ArrayList<Integer>(); // stores the packet no that is to be sent 
	FileOutputStream fos = null;
	packet p[]; // stores packets to be sent
	DatagramSocket clientsocket; 
	public udpClient(String[] args)
	{
		try 
		{
			clientsocket = new DatagramSocket();
		} 
		catch (SocketException e) 
		{
			
			e.printStackTrace();
		}
		extract(args);
		
	}
	/**
	 * The timer class which manages the timeout and resend the packet if timeout occurs.
	 */
	class timer implements Runnable
	{
        
		@Override
		public void run() 
		{
			
			try 
			{
				while(true)
				{
					Thread.sleep(time);
					if(check==0)
					{
						Thread.sleep(time);
					    sendData = ( fileLength + " " + segment).getBytes();
						DatagramPacket sendInformation = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
						clientsocket.send(sendInformation);
					}
				/**
				 * Resends the packet in window if time out occurs
				 */
				if(window.isEmpty())
				{
					
				}
				else
				{
					int  i = window.get(0).intValue();
					DatagramPacket sendPacket = new DatagramPacket(p[i].getpkt(), p[i].getpkt().length,serverAddress ,serverPort);
					clientsocket.send(sendPacket);
				}
				}
			} 
			catch (Exception e) 
			{
				
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * This fuction calculates the MD5 checksum of the packet.
	 * @param filename name of file for which the checksum is to be calculated.
	 */
	public void checksum(String filename)
	{
		try
		{
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    File file = new File(filename);
		    FileInputStream fis = new FileInputStream(file);
		    byte[] data = new byte[(int) file.length()]; // stores the file data in bytes
		    
		    int nread = 0; 
		    //reads the input file and stors in data
		    while ((nread = fis.read(data)) != -1) 
		    {
		      md.update(data, 0, nread);
		    };
            // MD5 checksum for file data
		    byte[] mdchecksum = md.digest();
		   
		    StringBuffer sb = new StringBuffer("");
		    // Representing MD5 checksum in hex decimal String
		    for (int i = 0; i < mdchecksum.length; i++) 
		    {
		    	sb.append(Integer.toString((mdchecksum[i] & 0xff) + 0x100, 16).substring(1));
		    }
		   
		    System.out.println("Checksum in hex: " + sb.toString());
		    
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		    
	}
	/**
	 * This function sends the packets to server
	 */
	public void client_send()
	{
	  FileInputStream fis = null;
	  File file = new File(fileloc);
	  byte[] file_array = new byte[(int) file.length()];
	  try 
	  {
	  fis = new FileInputStream(file);
	  fis.read(file_array);
	  fis.close();
      
	  checksum(fileloc);
	    // intial Checksum
	    byte rec[]=new byte[512];
	    sendData = ( fileLength + " " + segment).getBytes();
		DatagramPacket sendInformation = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
		clientsocket.send(sendInformation);
		// starts the timer for the packet
		Thread k = new Thread(new timer());
		k.start();
		DatagramPacket receive = new DatagramPacket(rec, rec.length);
		clientsocket.receive(receive);
		rec = receive.getData();
		if(q==0)
		{
		System.out.println(new String(rec));
		}
		check=1;
		if(k.isAlive())
		{
			k.stop();
		}
		// sends the packet in the send window
	    for (int i=0;i<segment;i++)
	    {
	    byte [] receivedata = new byte[512];
		window.add(i);
		DatagramPacket sendPacket = new DatagramPacket(p[i].getpkt(), p[i].getpkt().length,serverAddress ,serverPort);
		clientsocket.send(sendPacket);
		Thread tk = new Thread(new timer());
		tk.start();
		DatagramPacket receivePacket = new DatagramPacket(receivedata, receivedata.length);
		clientsocket.receive(receivePacket);
		receivedata = receivePacket.getData();
		window.remove(0);
		if(q==0)
		{
		System.out.println("ack :"+new String(receivedata).trim() );
		}
		i = Integer.parseInt(new String(receivedata).trim());
		if(tk.isAlive())
		{
			tk.stop();
		}
	    }
	  }
	  catch (Exception e) 
	  {
			e.printStackTrace();
	  }
	}
	/**
	 * This function reads the file and makes it into packets to be sent.
	 */
	private void packetformation()
	{
		int mod = (int)fileLength % 1024;
		segment = (int)fileLength / 1024;
		if( mod == 0 )
		{
			p = new packet[segment];
		}
		else
		{
			segment = segment + 1;
			p = new packet[segment];
		}
		byte pkt[];
		for( int i = 0; i < segment; i++ )
		{
			int temp;
			int l;
			if( i != segment-1 || mod == 0 )
			{
				temp = 1024;
				pkt = new byte[temp+32];
				l=temp+32;
			}
			else
			{
				temp = mod;
				pkt = new byte[temp+32];
				l=temp+32;
			}
			try
			{   // storing data,checksum,packet no in the packet
				byte tempByte[] = new byte[temp]; 
				fileInputStream.read(tempByte, 0, temp);
				System.arraycopy(tempByte, 0, pkt, 32, tempByte.length);
				MessageDigest digest = MessageDigest.getInstance("md5");
				digest.update(tempByte);
				byte hashFunction[] = digest.digest();
				System.arraycopy(hashFunction, 0, pkt, 16, hashFunction.length);
				byte segmentNumber[] = (i + "").getBytes();
				System.arraycopy(segmentNumber, 0, pkt, 0, segmentNumber.length);
				byte check[]= new byte[1024+32];
				System.arraycopy(pkt, 16, check, 0, 32);
				p[i]=new  packet(l,pkt);
				byte t []= new byte[16];
				System.arraycopy(p[i].getpkt(), 16, t, 0, 16);
				if(Arrays.equals(t, hashFunction))
				{
					
				}
				
				
			}
			catch(Exception e)
			{
				
			}
			
			
		}
		
	}
	/**
	 * This function extracts the command line arguments 
	 * @param args
	 */
	public void extract(String args[])
	{
		for (int i=0;i<args.length;i++)
		{
			if(args[i].equals("-f"))
			{
				fileloc = args[i+1];
			}
			else if(args[i].equals("-t"))
			{
				time = Integer.parseInt(args[i+1]);
			}
			else if(args[i].equals("-q"))
			{
				q = 1;
			}
			else if(i==(args.length-2))
			{
				try 
				{
					serverAddress = InetAddress.getByName(args[i]);
				} catch (UnknownHostException e) 
				{
				
					e.printStackTrace();
				}
			}
			else if(i==(args.length-1))
			{
				serverPort=Integer.parseInt(args[i]);
			}
		}
		try
		{
			file = new File(fileloc);
			fileInputStream = new FileInputStream(file);
		} 
		catch (FileNotFoundException e) 
		{
			
			e.printStackTrace();
		}
		fileLength = file.length();
		
		packetformation();
		client_send();
	}
}
