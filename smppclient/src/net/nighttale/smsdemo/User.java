package net.nighttale.smsdemo;

public class User {

	private String phoneNumber;
	private String accountNumber;
	private float balance;
	
	public User(String phoneNumber, String accountNumber, int balance) {
		this.phoneNumber = phoneNumber;
		this.accountNumber = accountNumber;
		this.balance = balance;
	}
	
	
	public static User findByPhone(String phoneNumber) {
		if (phoneNumber.equals("063123456"))
			return new User(phoneNumber, "123456", 10000);
		else
			return null;
	}
	
	public String getAccountNumber() {
		return accountNumber;
	}
	
	public float getBalance() {
		return balance;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	
	public void setBalance(float balance) {
		this.balance = balance;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

}
