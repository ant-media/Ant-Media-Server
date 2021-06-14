package io.antmedia.console.rest;

public class SupportRequest {
	private String name;
	private String email;
	private String title;
	private String description;
	private boolean sendSystemInfo;

	public boolean isSendSystemInfo() {
		return sendSystemInfo;
	}
	public void setSendSystemInfo(boolean sendSystemInfo) {
		this.sendSystemInfo = sendSystemInfo;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
}
