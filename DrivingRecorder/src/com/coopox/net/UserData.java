package com.coopox.net;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class UserData {
	
	User user = null;
	Context mContext;
	public UserData(Context context){
		mContext = context;
		user = new User();
		initUser();
	}
	
	private void initUser(){
	//	initLoginUserId();
		initUserMid();
	}
	
	public User getUser(){
		return user;
	}
	
	public void initLoginUserId() {
		ContentResolver resolver = mContext.getContentResolver();
		Uri uri = Uri
				.parse("content://com.chelianlian.provider.UserInfo/loginTable");
		Cursor cursor = resolver.query(uri, null, null, null, null);
		if (cursor.getCount() > 0 && cursor.moveToNext()) {
			Log.e("denson", "cursor = "+cursor.toString());
			String mid = cursor.getString(cursor.getColumnIndex("mid"));
			String userName = cursor.getString(cursor
					.getColumnIndex("userName"));
			String loginStatus = cursor.getString(cursor
					.getColumnIndex("loginStatus"));

			Log.e("denson", "mid = " + mid);
			Log.e("denson", "userName = " + userName);
			Log.e("denson", "loginStatus = " + loginStatus);

			user.setmId(mid);
			user.setUserName(userName);
			user.setLoginStatus(loginStatus);
		} else {
			Log.e("denson", "getLoginUserId exception");

		}
		cursor.close();
	}
	
	private String initUserMid() {
		Context con = null;
		String userId = "";
		String userPhone = "";
		try {
			con = mContext.createPackageContext("com.chelianlianland",
					Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		try {
			SharedPreferences sha = con.getSharedPreferences(
					"CheLianLian_Setting", 3);
			userId = sha.getString("uesr_id", "54");
			userPhone = sha.getString("user_phonenumber", "18589025273");
			user.setmId(userId);
			user.setUserPhone(userPhone);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return userId;
	}
}
