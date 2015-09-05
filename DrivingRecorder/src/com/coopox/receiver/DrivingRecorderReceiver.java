package com.coopox.receiver;

import com.coopox.DrivingRecorder.DrivingRecordService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class DrivingRecorderReceiver extends BroadcastReceiver{

	
	public static final String CAR_ACTION = "carsignal";
	public static final String CAR_MESSAGE = "carmode";
	public static final String car_poweron_working = "car_poweron_working";
	public static final String car_powerdown_working = "car_powerdown_working";
	public static final String car_powerdown_suspend = "car_powerdown_suspend";
	
	public static final String ACTION_BACK_CAMERA_SIGNAL = "backcamsignal";
	public static final String MSG_BACKCAMERA_DOWN = "backcam_powerdown";
	public static final String MSG_BACKCAMERA_ON = "backcam_poweron";
	
	public static final String CAR_UPLOAD_FILE_MP4 = "car_upload_file_mp4";
	public static final String ACTION_TO_UPLOAD_FILE = "action_msg_to_upload_file";
	
	private Handler mHandler;
	public DrivingRecorderReceiver(Handler handler){
		this.mHandler = handler;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if (action.equals(CAR_ACTION)) {
			String message = intent.getStringExtra(CAR_MESSAGE);
			if (message == null) 
				return;
			if (message.equals(car_poweron_working)) {
				mHandler.sendEmptyMessage(DrivingRecordService.MSG_ON_DRIVING);
			}else if(message.equals(car_powerdown_working)){
				mHandler.sendEmptyMessage(DrivingRecordService.MSG_OFF_DRIVING);
			}else if(message.equals(CAR_UPLOAD_FILE_MP4)){
				mHandler.sendEmptyMessage(DrivingRecordService.MSG_UPLOAD_FILE);
			}
		}
		if(action.equals(ACTION_BACK_CAMERA_SIGNAL)){
			String message = intent.getStringExtra(ACTION_BACK_CAMERA_SIGNAL);
			if(message == null)
				return;
			if(message.equals(MSG_BACKCAMERA_ON)){
				mHandler.sendEmptyMessage(DrivingRecordService.MSG_SWITCH_CAMERA_ONE);
			}else if(message.equals(MSG_BACKCAMERA_DOWN)){
				mHandler.sendEmptyMessage(DrivingRecordService.MSG_SWITCH_CAMERA_ZERO);
			}
		}
		
	}	
}
