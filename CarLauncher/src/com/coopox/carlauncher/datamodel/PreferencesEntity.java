package com.coopox.carlauncher.datamodel;

import android.provider.BaseColumns;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/5/17
 * 车机设置存储
 */
@Table(name = "PreferencesModel", id = BaseColumns._ID)
public class PreferencesEntity extends Model {
    @Column(name = "SwitchVoiceNow")
    public boolean swVoiceNow;

    @Column(name = "SwitchSmartKey")
    public boolean swSmartKey;

    @Column(name = "SwitchDrivingRecord")
    public boolean swDrivingRecord;

    @Column(name = "VoiceLanguage")
    public String voiceLanguage;

}
