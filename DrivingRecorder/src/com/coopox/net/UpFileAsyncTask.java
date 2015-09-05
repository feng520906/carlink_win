package com.coopox.net;

import java.io.File;

import com.coopox.DrivingRecorder.R;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UpFileAsyncTask extends AsyncTask<File, Integer, ReqPojo>{
	
	private Context mContext;
	private UserData userData;
	private User user;

	public UpFileAsyncTask(Context context){
		this.mContext = context;
		userData = new UserData(context);
		user = userData.getUser();
	}
	
	@Override
	protected ReqPojo doInBackground(File... params) {
		// TODO Auto-generated method stub
		return DrivingRecorderInterface.uploadFile(user.getmId(), params[0]);
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		Toast.makeText(mContext,
				mContext.getResources().getString(R.string.now_upload_file),
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onPostExecute(ReqPojo result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if(result != null){
			if(result.getStatus().equals("200")){
				Toast.makeText(mContext,
						"this is result is 200",Toast.LENGTH_LONG).show();
			}else if(result.getStatus().equals("404")){
				Toast.makeText(mContext,"this is result is 404",Toast.LENGTH_LONG).show();
			}else{
				String temp = "this is result = " + result.getStatus() + "; message = " + result.getMessage().toString();
				Toast.makeText(mContext,temp,Toast.LENGTH_LONG).show();
			}
		}else{
			Toast.makeText(mContext,"result == null",Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
		Log.d("zoufeng", "values[0] = " + values[0]);
		Toast.makeText(mContext,"zoufeng  values[0] = " + values[0] ,Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onCancelled(ReqPojo result) {
		// TODO Auto-generated method stub
		super.onCancelled(result);
	}

	@Override
	protected void onCancelled() {
		// TODO Auto-generated method stub
		super.onCancelled();
	}
	
	
}
