package io.antmedia.ipcamera;

public class ArchivedVideo {

	private String url, date, camName;

	public ArchivedVideo(String camName, String date, String url) {

		this.camName = camName;
		this.date = date;
		this.url = url;
	}

	public String getCamName() {
		return camName;
	}

	public void setCamName(String camName) {
		this.camName = camName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
