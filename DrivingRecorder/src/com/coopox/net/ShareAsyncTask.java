package com.coopox.net;

import java.io.File;

import com.coopox.DrivingRecorder.R;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ShareAsyncTask extends AsyncTask<File, Integer, ReqPojo>{

	private Context mContext;
	private UserData userData;
	private User user;
	public ShareAsyncTask(Context context){
		mContext = context;
		userData = new UserData(context);
		user = userData.getUser();
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
	protected ReqPojo doInBackground(File... params) {
		// TODO Auto-generated method stub
		if (!user.getLoginStatus().equals("1")) {
			Toast.makeText(mContext,
					mContext.getResources().getString(R.string.no_user_id),
					Toast.LENGTH_SHORT).show();
			return null;
		}
		return DrivingRecorderInterface.oneKeyShrar(user.getmId(), params[0],
				"description test");
	}

	@Override
	protected void onPostExecute(ReqPojo result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}
	
	
}
