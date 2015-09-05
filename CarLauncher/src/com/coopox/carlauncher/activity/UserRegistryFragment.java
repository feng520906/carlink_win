/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coopox.carlauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.view.PickerView;
import com.coopox.common.Constants;
import com.coopox.common.utils.AssetUtils;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.ThreadManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/7
 */
public class UserRegistryFragment extends Fragment implements View.OnClickListener, PickerView.onSelectListener, RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "UserRegistryFragment";

    public interface RegistrySubmitListener {
        void onSubmit();
        void onSkip();
    }

    private static final Map<String, String> PROVINCE_SHORT_NAME = new HashMap<String, String>();
    static {
        PROVINCE_SHORT_NAME.put("北京", "京");
        PROVINCE_SHORT_NAME.put("上海", "沪");
        PROVINCE_SHORT_NAME.put("安徽省", "皖");
        PROVINCE_SHORT_NAME.put("甘肃省", "甘");
        PROVINCE_SHORT_NAME.put("广西省", "桂");
        PROVINCE_SHORT_NAME.put("海南省", "琼");
        PROVINCE_SHORT_NAME.put("河南省", "豫");
        PROVINCE_SHORT_NAME.put("湖北省", "鄂");
        PROVINCE_SHORT_NAME.put("吉林省", "吉");
        PROVINCE_SHORT_NAME.put("江西省", "赣");
        PROVINCE_SHORT_NAME.put("内蒙古", "蒙");
        PROVINCE_SHORT_NAME.put("青海省", "青");
        PROVINCE_SHORT_NAME.put("山西省", "晋");
        PROVINCE_SHORT_NAME.put("四川省", "川");
        PROVINCE_SHORT_NAME.put("新疆", "新");
        PROVINCE_SHORT_NAME.put("天津", "津");
        PROVINCE_SHORT_NAME.put("重庆", "渝");
        PROVINCE_SHORT_NAME.put("福建省", "闽");
        PROVINCE_SHORT_NAME.put("广东省", "粤");
        PROVINCE_SHORT_NAME.put("贵州省", "贵");
        PROVINCE_SHORT_NAME.put("河北省", "冀");
        PROVINCE_SHORT_NAME.put("黑龙江省", "黑");
        PROVINCE_SHORT_NAME.put("湖南省", "湘");
        PROVINCE_SHORT_NAME.put("江苏省", "苏");
        PROVINCE_SHORT_NAME.put("辽宁省", "辽");
        PROVINCE_SHORT_NAME.put("宁夏省", "宁");
        PROVINCE_SHORT_NAME.put("山东省", "鲁");
        PROVINCE_SHORT_NAME.put("陕西省", "陕");
        PROVINCE_SHORT_NAME.put("西藏", "藏");
        PROVINCE_SHORT_NAME.put("云南省", "云");
        PROVINCE_SHORT_NAME.put("浙江省", "浙");
    }

    private static final String DEFAULT_PROVINCE = "北京";
    private TextView mPlateLabel;
    private String mShortName;
    private RadioGroup mGenderRadioGroup;
    private EditText mNameEdit;
    private EditText mEngineCodeEdit;
//    private EditText mMilEdit;
    private EditText mPlateEdit;
    private EditText mFrameEdit;
    private int mGender = -1;
    private String mProvince;
    RegistrySubmitListener mListener;
    private Map<String, List<String>> mBrandTable;
    private PickerView mBrandPicker;
    private PickerView mFamilyPicker;
    private PickerView mProvincePicker;
    private String mCarBrand;
    private String mCarFamily;

    private PickerView.onSelectListener mBrandOnSelectListener =
            new PickerView.onSelectListener() {
        @Override
        public void onSelect(String text) {
            if (!TextUtils.isEmpty(text)) {
                mCarBrand = text;
                Log.d(TAG, "Choose Car brand " + mCarBrand);

                List<String> family = mBrandTable.get(text);
                if (!Checker.isEmpty(family)) {
                    mFamilyPicker.setData(family);
                    mFamilyPicker.setSelected(family.size() / 2);
                } else {
                    Log.w(TAG, "No car family data!");
                }
            }
        }
    };

    private PickerView.onSelectListener mFamilyOnSelectListener =
            new PickerView.onSelectListener() {
        @Override
        public void onSelect(String text) {
            if (!TextUtils.isEmpty(text)) {
                mCarFamily = text;
                Log.d(TAG, "Choose Car family " + mCarFamily);
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof RegistrySubmitListener) {
            mListener = (RegistrySubmitListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_registry, container, false);
        rootView.findViewById(R.id.btn_ok).setOnClickListener(this);
        rootView.findViewById(R.id.btn_skip).setOnClickListener(this);
        mGenderRadioGroup = (RadioGroup)rootView.findViewById(R.id.radio_gender);
        mGenderRadioGroup.setOnCheckedChangeListener(this);
        mNameEdit = (EditText)rootView.findViewById(R.id.input_name);
        mEngineCodeEdit = (EditText)rootView.findViewById(R.id.input_engine_code);
//        mMilEdit = (EditText)findViewById(R.id.input_mileage);
        mPlateEdit = (EditText)rootView.findViewById(R.id.input_plate);
        mFrameEdit = (EditText)rootView.findViewById(R.id.input_frame_num);

        mPlateLabel = (TextView) rootView.findViewById(R.id.label_plate);
        List<String> provinces = new ArrayList<String>();
        provinces.addAll(PROVINCE_SHORT_NAME.keySet());
        mProvincePicker = (PickerView) rootView.findViewById(R.id.province_picker);
        mProvincePicker.setData(provinces);
        mProvincePicker.setOnSelectListener(this);
        mProvincePicker.setSelected(DEFAULT_PROVINCE);
        onSelect(DEFAULT_PROVINCE);

        mBrandPicker = (PickerView) rootView.findViewById(R.id.brand_picker);
        mFamilyPicker = (PickerView) rootView.findViewById(R.id.family_picker);

        ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                mBrandTable = loadCarBrandInfo();
                if (Checker.isEmpty(mBrandTable.keySet())) {
                    return;
                }

                ThreadManager.INSTANCE.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mBrandPicker && null != mFamilyPicker) {

                            ArrayList<String> brandList =
                                    new ArrayList<String>(mBrandTable.keySet());
                            mBrandPicker.setData(brandList);
                            int midIndex = brandList.size() / 2;
                            mCarBrand = brandList.get(midIndex);
                            mBrandPicker.setSelected(midIndex);

                            List<String> family = mBrandTable.get(mCarBrand);
                            if (!Checker.isEmpty(family)) {
                                mFamilyPicker.setData(family);
                            }
                        }

                        // 车型数据加载后才加载用户注册信息并显示
                        loadUserData();
                    }
                });
            }
        });

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ok:
                if (checkInputData()) {
                    saveInputData();
                    if (null != mListener) {
                        mListener.onSubmit();
                    }
                }
                break;
            case R.id.btn_skip:
                if (null != mListener) {
                    mListener.onSkip();
                }
                break;
        }
    }

    @Override
    public void onSelect(String text) {
        if (!TextUtils.isEmpty(text)) {
            mProvince = text;
            mShortName = PROVINCE_SHORT_NAME.get(text);
            if (null != mShortName) {
                mPlateLabel.setText(getString(R.string.plate) + mShortName);
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.radio_male:
                mGender = 1;
                break;
            case R.id.radio_female:
                mGender = 0;
                break;
        }
    }

    private Map<String, List<String>> loadCarBrandInfo() {
        String jsonStr = AssetUtils.loadJSONFromAsset(getActivity(), "carbrand.json");
        return new Gson().fromJson(jsonStr, new TypeToken<Map<String, List<String>>>() {
        }.getType());
    }

    private boolean checkInputData() {
        Toast toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
/*        if (mNameEdit.getText().toString().trim().isEmpty()) {
            toast.setText(R.string.pls_input_name);
            toast.show();
            return false;
        }
        if (-1 == mGender) {
            toast.setText(R.string.pls_select_gender);
            toast.show();
            return false;
        }*/

/*        String milStr = mMilEdit.getText().toString().trim();
        if (!TextUtils.isDigitsOnly(milStr)) {
            toast.setText(R.string.invalid_mileage);
            toast.show();
            return false;
        }*/

        if (Checker.isEmpty(mCarBrand) || Checker.isEmpty(mCarFamily)) {
            toast.setText(R.string.pls_choose_car_info);
            toast.show();
            return false;
        }

        String plate = mPlateEdit.getText().toString().trim();
        if (plate.isEmpty()) {
            toast.setText(R.string.pls_input_plate);
            toast.show();
            return false;
        }
        // 检查车牌号的合法性
        Matcher matcher = Pattern.compile("^[A-Za-z0-9\\-]+$").matcher(plate);
        if (!matcher.find()) {
            toast.setText(R.string.invalid_plate);
            toast.show();
            return false;
        }

        String frame_num = mFrameEdit.getText().toString().trim();
/*        if (frame_num.isEmpty()) {
            toast.setText(R.string.pls_input_frame_num);
            toast.show();
            return false;
        }*/
        if (!frame_num.isEmpty()) {
            // 检查车架号的合法性
            matcher = Pattern.compile("^[A-Za-z0-9\\-]+$").matcher(frame_num);
            if (!matcher.find()) {
                toast.setText(R.string.invalid_frame_num);
                toast.show();
                return false;
            }
        }

        String engineCode = mEngineCodeEdit.getText().toString().trim();
        if (engineCode.isEmpty()) {
            toast.setText(R.string.pls_input_engine_code);
            toast.show();
            return false;
        }
        // 检查车架号的合法性
        matcher = Pattern.compile("^[A-Za-z0-9\\-]+$").matcher(engineCode);
        if (!matcher.find()) {
            toast.setText(R.string.invalid_engine_code);
            toast.show();
            return false;
        }

        return true;
    }

    private void saveInputData() {
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = sp.edit();
//        edit.putString(Constants.KEY_USER_NAME, mNameEdit.getText().toString().trim());
//        edit.putInt(Constants.KEY_USER_GENDER, mGender);
        edit.putString(Constants.KEY_CAR_BRAND, mCarBrand);
        edit.putString(Constants.KEY_CAR_FAMILY, mCarFamily);
        edit.putString(Constants.KEY_CAR_PLATE, mPlateEdit.getText().toString().trim());
        edit.putString(Constants.KEY_CAR_PROVINCE, mProvince);
        edit.putString(Constants.KEY_PROVINCE_SHORT, mShortName);
        edit.putString(Constants.KEY_ENGINE_CODE, mEngineCodeEdit.getText().toString().trim());
        edit.putString(Constants.KEY_FRAME_NUM, mFrameEdit.getText().toString().trim());

        edit.commit();
    }

    /**
     * 加载用户注册信息（如果有的话）并在界面上回显 */
    private void loadUserData() {
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        mCarBrand = sp.getString(Constants.KEY_CAR_BRAND, null);
        mCarFamily = sp.getString(Constants.KEY_CAR_FAMILY, null);
        if (null != mCarBrand) {
            if (mBrandPicker.setSelected(mCarBrand)) {
                List<String> family = mBrandTable.get(mCarBrand);
                if (!Checker.isEmpty(family)) {
                    mFamilyPicker.setData(family);
                }

                // 汽车品牌设置成功才继续设置型号，防止以后更新车型数据时可能出现对不上的情况。
                if (null != mCarFamily) {
                    mFamilyPicker.setSelected(mCarFamily);
                }
            }
        }
        // 为了上面可以直接设置汽车品牌车型（避免受回调逻辑影响），将回调的注册时机延迟到这里执行
        mBrandPicker.setOnSelectListener(mBrandOnSelectListener);
        mFamilyPicker.setOnSelectListener(mFamilyOnSelectListener);

        String carPlate = sp.getString(Constants.KEY_CAR_PLATE, null);
        if (null != carPlate) {
            mPlateEdit.setText(carPlate);
        }

        mProvince = sp.getString(Constants.KEY_CAR_PROVINCE, null);
        if (null != mProvince) {
            mProvincePicker.setSelected(mProvince);
        }

        mShortName = sp.getString(Constants.KEY_PROVINCE_SHORT, null);
        if (null != mShortName) {
            mPlateLabel.setText(getString(R.string.plate) + mShortName);
        }

        String engineCode = sp.getString(Constants.KEY_ENGINE_CODE, null);
        if (null != engineCode) {
            mEngineCodeEdit.setText(engineCode);
        }

        String frameNum = sp.getString(Constants.KEY_FRAME_NUM, null);
        if (null != frameNum) {
            mFrameEdit.setText(frameNum);
        }
    }
}
