package io.antmedia.webrtc.datachannel.event;

public class RoleEvent extends ControlEvent {

	private String role;
	
	public RoleEvent(String streamId) {
		super(streamId);
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

}
