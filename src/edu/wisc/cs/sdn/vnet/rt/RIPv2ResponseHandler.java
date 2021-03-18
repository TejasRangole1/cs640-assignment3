package edu.wisc.cs.sdn.vnet.rt;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.RIPv2Entry;
import net.floodlightcontroller.packet.UDP;

public class RIPv2ResponseHandler implements Runnable {


	private Router router;
   /** Table to store rip entries */
	private List<RIPv2Entry> ripTable;
	
	private Thread responseThread;

	private ReentrantLock lock;
	
	public RIPv2ResponseHandler(Router router, List<RIPv2Entry> someTable, ReentrantLock lock){
		this.router = router;
		this.ripTable = someTable;
		RIPv2 request = createRipPacket(RIPv2.COMMAND_REQUEST);
		floodRIPv2Packet(request);
		this.lock = lock;
		responseThread = new Thread(this);
		responseThread.start();
	}
    /**
	 * Sends a RIP packet out all interfaces
	 * @param ripPacket
	 */
	public void floodRIPv2Packet(RIPv2 ripPacket){
		for(Iface i : router.getInterfaces().values()) {
			Ethernet etherPacket = encapsulateRIPv2Packet(ripPacket, i);
			router.sendPacket(etherPacket, i);
		}
	}
    /**
	 * Send RIP packet to a specific node
	 * @param ripPacket
	 * @param i
	 */
	public void sendRIPv2Packet(RIPv2 ripPacket, Iface i){
		Ethernet etherPacket = encapsulateRIPv2Packet(ripPacket, i);
		router.sendPacket(etherPacket, i);
	}

	public Ethernet encapsulateRIPv2Packet(RIPv2 ripPacket, Iface i){
		UDP udpPacket = new UDP();
		udpPacket.setSourcePort((short) 520);
		udpPacket.setDestinationPort((short) 520);
		udpPacket.setPayload(ripPacket);
		IPv4 ipPacket = new IPv4();
		ipPacket.setPayload(udpPacket);
		ipPacket.setSourceAddress(i.getIpAddress());
		ipPacket.setDestinationAddress("224.0.0.9");
		ipPacket.setProtocol(IPv4.PROTOCOL_UDP);
		ipPacket.setTtl((byte) 2); 
		Ethernet etherPacket = new Ethernet();
		etherPacket.setPayload(ipPacket);
		etherPacket.setSourceMACAddress(i.getMacAddress().toBytes());
		etherPacket.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");
		etherPacket.setEtherType(Ethernet.TYPE_IPv4);
		return etherPacket;
	}

	public RIPv2 createRipPacket(byte command){
		RIPv2 ripPacket = new RIPv2();
		ripPacket.setCommand(command);
		if(command == RIPv2.COMMAND_RESPONSE){
			ripPacket.setEntries(ripTable);
		}
		return ripPacket;
	}

	public void handleResponse(){
		try{
			lock.lock();

		}
	}
	
	/*
	 * Sends an unsolicted RIPv2 response every 10 seconds
	 */
	@Override
	public void run(){
		// TODO Auto-generated method stub
		while(true){
			try{
				// sleep for 10 seconds
				responseThread.sleep(10000);
			} catch (InterruptedException e){
				break;
			}
			RIPv2 response = createRipPacket(RIPv2.COMMAND_RESPONSE);
			floodRIPv2Packet(response);
		}
	}
	
}
