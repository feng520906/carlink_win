package com.coopox.net;

/**
 * 
 * @author kuwan 定义一些请求接口的静态常量；
 * 
 */
public class Global {

	/**
	 * 请求的接口地址；
	 */
	public static String REQUEST_URL = "http://test.ilincar.com:2048/";

	/**
	 * 一键分享
	 */
	public static String ONE_KEY_SHARE_URL = "/thinkphp/index.php?m=RidersCircle&a=share";

	/**
	 * 上传文件
	 */
	public static String UPLOAD_FILE_URL = "/thinkphp/index.php?m=RidersCircle&a=uploadFile";

	/**
	 * 上传文件请求的接口地址；
	 */
	public static String UPLOAD_FILE = REQUEST_URL + UPLOAD_FILE_URL;

	/**
	 * 一键分享请求的接口地址；
	 */
	public static String ONE_KEY_SHARE = REQUEST_URL + ONE_KEY_SHARE_URL;
}
