package io.antmedia.rest.model;

public class Result {

	/**
	 * Gives information about the operation. 
	 * If it is true, operation is successfull
	 * if it is false, operation is failed
	 */
	private boolean success = false;

	/**
	 * Message may be filled when error happens so that developer may
	 * understand what the problem is
	 */
	private String message;

	private int errorId;

	/**
	 * Constructor for the object
	 * 
	 * @param success
	 * @param message
	 */
	public Result(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	
	public Result(boolean success) {
		this.success = success;
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
}
