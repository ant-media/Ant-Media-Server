package io.antmedia.rest.model;

public class Result {

	/**
	 * Gives information about the operation. 
	 * If it is true, operation is successfull
	 * if it is false, operation is failed
	 */
	private boolean success = false;

	/**
	 * Message may be filled error or informaiton 
	 * It is filled with error when  success if false so that developer may
	 * understand what the problem is
	 * It is filled with information if user makes a request it returns success as true with additional information
	 */
	private String message;
	
	/**
	 * If operation is about adding a record, then the below field have the id of the record 
	 */
	private String dataId;

	private int errorId;

	/**
	 * Constructor for the object
	 * 
	 * @param success
	 * @param message
	 */
	public Result(boolean success, String message) {
		this(success, null, message);
	}
	
	public Result(boolean success) {
		this(success, null, null);
	}
	
	public Result(boolean success, String dataId, String message) {
		this.success = success;
		this.message = message;
		this.dataId = dataId;
	}

	public Result(boolean result, String message, int errorId) {
		this.success = result;
		this.message = message;
		this.errorId = errorId;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getErrorId() {
		return errorId;
	}

	public void setErrorId(int errorId) {
		this.errorId = errorId;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
}
