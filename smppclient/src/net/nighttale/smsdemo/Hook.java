package net.nighttale.smsdemo;

import ie.omk.smpp.Connection;

public class Hook extends Thread {
	
	public void run() {
		System.out.println("Unbinding");
		Connection conn = SMSDemo.getInstance().getConnection();
		if(conn != null && conn.isBound())
			conn.force_unbind();
	}
}
