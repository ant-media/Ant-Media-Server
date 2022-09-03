package io.antmedia.datastore.db.types;

public class Licence {
	
	private String licenceId;
	
	private String startDate;
	
	private String endDate;
	
	private String type;
	
	private String licenceCount;
	
	private String owner;
	
	private String status;
	
	private String hourUsed;

	public Licence() {
		
		this.type = "regular";
	}

	public String getLicenceId() {
		return licenceId;
	}

	public void setLicenceId(String licenceId) {
		this.licenceId = licenceId;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLicenceCount() {
		return licenceCount;
	}

	public void setLicenceCount(String licenceCount) {
		this.licenceCount = licenceCount;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getHourUsed() {
		return hourUsed;
	}

	public void setHourUsed(String hourUsed) {
		this.hourUsed = hourUsed;
	}

}
