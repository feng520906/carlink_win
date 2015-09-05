package com.coopox.net;

public class ReqPojo {
	private String status;
	private String message;
	private String result;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	@Override
	public String toString() {
		return "PeqUploadFile [status=" + status + ", message=" + message
				+ ", result=" + result + "]";
	}
}
