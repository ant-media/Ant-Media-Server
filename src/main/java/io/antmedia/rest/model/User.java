package io.antmedia.rest.model;

public class User {
	
	public String email;
	public String password;
	public int userType;
	public String newPassword;
	public String fullName;
	
	public User(String email, String password, int userType) {
		this.email = email;
		this.password = password;
		this.userType = userType;
	}
	
	
	public User() {
		
	}
	
	
	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public int getUserType() {
		return userType;
	}


	public void setUserType(int userType) {
		this.userType = userType;
	}


	public String getNewPassword() {
		return newPassword;
	}


	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}




	
}