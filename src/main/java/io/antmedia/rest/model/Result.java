package io.antmedia.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The basic result class")
public class Result {

    /**
     * Gives information about the operation.
     * If it is true, operation is successful
     * if it is false, operation is failed
     */
    @Schema(description = "The result of the operation")
    private boolean success = false;

    /**
     * Message may be filled error or information.
     * It is filled with error when success field
     * is false so that developer may understand what the problem is.
     * It is filled with information if user makes a request, it returns success as true with additional information
     */
    @Schema(description = "The message of the operation result")
    private String message;

    /**
     * If operation is about adding a record, then the below field have the id of the record
     */
    @Schema(description = "The id of the record if operation is about adding a record")
    private String dataId;

    @Schema(description = "The id of error of the operation result")
    private int errorId;

    /**
     * Constructor for the object
     *
     * @param success
     * @param message
     */
    public Result(boolean success, String message) {
        this(success, "", message);
    }

    public Result(boolean success) {
        this(success, "", "");
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