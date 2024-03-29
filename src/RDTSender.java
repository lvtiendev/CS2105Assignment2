/**
 * RDTSender : Encapsulate a reliable data sender that runs
 * over a unreliable channel that may drop and corrupt packets 
 * (but always delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 12 March 2013
 */
import java.io.*;
import java.util.*;

/**
 * RDTSender receives a byte array from "above", construct a
 * data packet, and send it via UDT.  It also receives
 * ack packets from UDT.
 */
class RDTSender {
	class ResendTask extends TimerTask {

		DataPacket packet;
		UDTSender sender;
		
		ResendTask(UDTSender _sender, DataPacket _packet) {
			sender = _sender;
			packet = _packet;
		}
		
		public void run(){
			try {
				sender.send(packet);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		
	}
	
	UDTSender udt;
	int seqNumber;

	RDTSender(String hostname, int port) throws IOException
	{
		udt = new UDTSender(hostname, port);
		seqNumber = 0;
	}

	/**
	 * send() delivers the given array of bytes reliably and should
	 * not return until it is sure that the packet has been
	 * delivered.
	 */
	void send(byte[] data, int length) throws IOException, ClassNotFoundException
	{
		// send packet
		DataPacket p = new DataPacket(data, length, seqNumber);
		udt.send(p);
		
		// start timer
		long delay = 500;
		Timer timer = new Timer();
		timer.schedule(new ResendTask(udt, p), delay, delay);

		// receive ACK
		while (true) {
			AckPacket ack = udt.recv();
			if (ack.isCorrupted || (ack.ack != seqNumber)) {
				// do nothing
			} else if (!ack.isCorrupted && (ack.ack == seqNumber)) {
				timer.cancel();
				seqNumber = 1 - seqNumber;
				break;
			}
		}
	}
	
	void sendPacket(DataPacket p) {
		
	}

	/**
	 * close() is called when there is no more data to send.
	 * This method creates an empty packet with 0 bytes and
	 * send it to the receiver, to indicate that there is no
	 * more data.
	 * 
	 * This method should not return until it is sure that
	 * the empty packet has been delivered correctly.  It 
	 * catches any EOFException (which signals the receiver
	 * has closed the connection) and close its own connection.
	 */
	void close() throws IOException, ClassNotFoundException
	{
		DataPacket p = new DataPacket(null, 0, seqNumber);
		udt.send(p);
		try {
			AckPacket ack = udt.recv();
		} catch (EOFException e) {
		} 
		finally {
			udt.close();
		}
	}
}
