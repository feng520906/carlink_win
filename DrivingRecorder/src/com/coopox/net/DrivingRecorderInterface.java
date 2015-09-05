package com.coopox.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.FileBody;

import org.json.JSONObject;
import android.util.Log;

public class DrivingRecorderInterface {
	
	private static final int TIMEOUT_IN_MILLIONS = 5000;
	public static  String userIdKey = "mid";
	public static  String descriptionKey = "description";
	public static  String fileKey = "file";

	/**
	 *  一键分享接口
	 * @param userid		//登录用户名
	 * @param file			//上传的文件（视频/图片）
	 * @param description	//文字内容
	 */
	public static ReqPojo oneKeyShrar(String userid,File file,String description){
		ReqPojo reqOneKeyShare = null;
		try {
			// 创建http连接
			HttpClient httpClient = new DefaultHttpClient();
			// 连接地址
			HttpPost httpPostReq = new HttpPost(Global.ONE_KEY_SHARE);
			
			// 创建一个MultipartEntity
			MultipartEntity entity = new MultipartEntity();
			
			entity.addPart(userIdKey, new StringBody(userid));
			entity.addPart(descriptionKey, new StringBody(description));
			
			if (file != null && file.exists()) {
				entity.addPart(fileKey, new FileBody(file));
			}
			
			httpPostReq.setEntity(entity);
			// 提交请求
			HttpResponse resp = httpClient.execute(httpPostReq);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(resp.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String inputLine = null;

				while ((inputLine = reader.readLine()) != null) {
					result.append(inputLine);
				}

				JSONObject oneKeyShareResult = new JSONObject(result.toString());
				
				reqOneKeyShare = new ReqPojo();
				
				reqOneKeyShare.setMessage(oneKeyShareResult.getString("state"));
				reqOneKeyShare.setResult(oneKeyShareResult.getString("message"));
				reqOneKeyShare.setStatus(oneKeyShareResult.getString("result"));
				
			}
			httpPostReq.abort();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reqOneKeyShare;
	}
	
	public static ReqPojo uploadFile(String userid,File file){
		ReqPojo peqUploadFile = null;
		try {
			// 创建http连接
			HttpClient httpClient = new DefaultHttpClient();
			// 连接地址
			HttpPost httpPostReq = new HttpPost(Global.UPLOAD_FILE);
			
			// 创建一个MultipartEntity
			MultipartEntity entity = new MultipartEntity();
			
			entity.addPart(userIdKey, new StringBody(userid));
			
			if (file != null && file.exists()) {
				entity.addPart(fileKey, new FileBody(file,"video/mp4"));
			}
			
			httpPostReq.setEntity(entity);
			Log.e("zoufeng", "httpPostReq.getParams() = "+httpPostReq.getRequestLine());;
			// 提交请求
			HttpResponse resp = httpClient.execute(httpPostReq);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(resp.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String inputLine = null;

				while ((inputLine = reader.readLine()) != null) {
					result.append(inputLine);
				}

				JSONObject updeteFileResult = new JSONObject(result.toString());	
				peqUploadFile = new ReqPojo();
				peqUploadFile.setMessage(updeteFileResult.getString("state"));
				peqUploadFile.setResult(updeteFileResult.getString("message"));
				peqUploadFile.setStatus(updeteFileResult.getString("result"));
			}
			httpPostReq.abort();
		} catch (Exception e) {
			Log.d("zoufeng", "this is error = " + e.toString());
			e.printStackTrace();
		}
		return peqUploadFile;
	}
	
	public static String doPost(String url, String param) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection) realUrl
					.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setUseCaches(false);
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
			conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);

			if (param != null && !param.trim().equals("")) {
				// 获取URLConnection对象对应的输出流
				out = new PrintWriter(conn.getOutputStream());
				// 发送请求参数
				out.print(param);
				// flush输出流的缓冲
				out.flush();
			}
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
}
