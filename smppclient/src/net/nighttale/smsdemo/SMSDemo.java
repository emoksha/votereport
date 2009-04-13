package smsdemo;

import ie.omk.smpp.BadCommandIDException;
import ie.omk.smpp.Connection;
import ie.omk.smpp.event.ConnectionObserver;
import ie.omk.smpp.event.ReceiverExitEvent;
import ie.omk.smpp.event.SMPPEvent;
import ie.omk.smpp.message.SMPPPacket;
import ie.omk.smpp.message.SubmitSM;
import ie.omk.smpp.message.SubmitSMResp;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.URL;

public class SMSDemo extends Thread implements ConnectionObserver {
	

	private int KEYWORD_LEN = 5 ;// Fair
	private int MSG_MAX_LEN  = 105; // Need to know whether this needs to be enforced for our backend
	
	private final static String APP_URL_PREFIX = "http://www.freefairelections.com/ushahidi/frontlinesms/?key=RAISRBE7&";
	private final static String SENDER_KEY = "s="; // Key + =
	private final static String MESG_KEY = "&m="; // Separator + key + =
	
	private Connection conn;
	private boolean exit = false;
	
	
	private static SMSDemo instance;
	
	private SMSDemo() {
	}
	
	public static SMSDemo getInstance() {
		if (instance == null)
			instance = new SMSDemo();
		return instance;
	}
	
	private void connect() {
		try {
			//conn = new Connection("localhost", 2775, true);
			// TODO : Read connection details from External Config
			conn = new Connection("66.179.181.44", 2775, true);
			conn.addObserver(this);
		} catch (UnknownHostException uhe) {
			System.exit(0);
		}
		
		boolean retry = false;
		
		while (!retry) {
			try {
				//conn.bind(Connection.TRANSCEIVER, "sysId", "secret", null);
				// TODO : Read connection details from config
				// Bind as a receiver for now
				conn.bind(Connection.RECEIVER, "munish", "mun567", null);
				retry = true;
			} catch (IOException ioe) {
				try {
					sleep(60 * 1000);
				} catch (InterruptedException ie) {
				}
			}
		}
		
	}
	
	public void run() {
		while (!exit) {
			connect();
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException ie) {
				}
			}
		}
	}
	
	public void update(Connection conn, SMPPEvent ev) {
		if (ev.getType() == SMPPEvent.RECEIVER_EXIT && ((ReceiverExitEvent)ev).isException()) {
			synchronized(this) {
				notify();
			}
		}
	}

	public void packetReceived(Connection conn, SMPPPacket pack) {
		switch(pack.getCommandId()) {
			case SMPPPacket.DELIVER_SM : 
				try {
					
//					SubmitSM response = processRequest(pack);
//					SubmitSMResp smr = (SubmitSMResp)conn.sendRequest(response);
					processMessage(pack);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case SMPPPacket.BIND_TRANSCEIVER_RESP :
				System.out.println(" Packet Recieved with switch : " + SMPPPacket.BIND_TRANSCEIVER_RESP +"(SMPPPacket.BIND_TRANSCEIVER_RESP)");
				if (pack.getCommandStatus() != 0) {
					System.out.println("Error binding: " + pack.getCommandStatus());
					exit = true;
					synchronized(this) {
						notify();
					}
				} else {
					System.out.println("Bounded");
				}
		}
	}
	
	private SubmitSM processRequest(SMPPPacket request) throws BadCommandIDException {
		SubmitSM sm = (SubmitSM)conn.newInstance(SMPPPacket.SUBMIT_SM);
		sm.setDestination(request.getSource());
		String[] parts = request.getMessageText().split(" ");
		logPacket(request, "IN");
		if (parts[0].equalsIgnoreCase("balance")) {
			User user = User.findByPhone(request.getSource().getAddress());
			if (user == null)
				sm.setMessageText("Your phone number is not registered in our database! Please contact one of our offices");
			else if (!user.getAccountNumber().equalsIgnoreCase(parts[1]))
				sm.setMessageText("Account number that you have entered is not correct! Please try again");
			else
				sm.setMessageText("Balance on your account is " + user.getBalance() + "$");
		} else {
			sm.setMessageText("Wrong message format! Please send BALANCE <ACCOUNT_NUMBER>");
		}
		logPacket(sm, "OUT");
		return null;
	}
	
	private void submitMessage(String sender, String message){
		try {
			String url = APP_URL_PREFIX;
			url += SENDER_KEY + URLEncoder.encode(sender, "UTF-8");
			url += MESG_KEY + URLEncoder.encode(message, "UTF-8");
			System.out.println("URL = " + url);
			URL appURL = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(appURL
					.openStream()));

			String inputLine;
			
			while ((inputLine = in.readLine()) != null)
				System.out.println("X =" + inputLine);
			in.close();
			
		} catch (Exception e){
			System.err.println(e.getMessage());
		}
		
		}
	private void processMessage(SMPPPacket message) {
		if(message != null){
			
			String messageText = message.getMessageText();
			String sender		= message.getSource().getAddress();
			
			if (messageText != null){
				
				int len = messageText.length();
				
				// There has to be a message
				// What can be a minimum length, for now allow a code
				if(len > KEYWORD_LEN + 2) {
					String actMesg = messageText.substring(KEYWORD_LEN,MSG_MAX_LEN); 
					System.out.println(" Sender - " + sender + " : " + actMesg);
					submitMessage(sender,actMesg);
				} else {
					// Develop robust logging here
					System.out.println("Undecipherable Complaint \""+ messageText + "\" from " + message.getSource());
				}
			} else {
				// How can this happen?
				System.out.println("SMMPPPacket was null. Start reading the API ....");
			}
			
		} else {
			// throw exception (what kind)
			// This is just to prevent the daemon from crashing. Need to know when this can happen
		}
		
	}
	
	
	
	private void logPacket(SMPPPacket packet, String direction) {
		String phone;
		if (direction.equals("OUT"))
			phone = packet.getDestination().getAddress();
		else
			phone = packet.getSource().getAddress();
		System.out.println(direction + ": " + phone +  " - " + packet.getMessageText());
	}
	
	public Connection getConnection() {
		return conn;
	}	
	
	public static void main(String args[]) {
		Runtime.getRuntime().addShutdownHook(new Hook());
		SMSDemo demo = SMSDemo.getInstance();
		demo.start();
	}	

}
