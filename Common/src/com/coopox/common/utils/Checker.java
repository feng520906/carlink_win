package com.coopox.common.utils;

import android.util.SparseArray;

import com.google.gson.JsonArray;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public final class Checker {

    private Checker() {}

    //********************* Empty Object Validation ***************************

    public static boolean isZero(Long longNum) {
        return longNum == null || longNum == 0;
    }

    public static boolean isZero(Integer integer) {
        return integer == null || integer == 0;
    }

    public static boolean isZero(Double doubleNum) {
        return doubleNum == null || doubleNum == 0;
    }

    public static boolean isEmpty(CharSequence text) {
        return text == null || text.length() <= 0 || "null".equals(text);
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isExistedFile(File file) {
        return file != null && file.exists();
    }

    public static boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(File file) {
        return file == null || !file.exists() || file.length() <= 0;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(SparseArray array) {
        return array == null || array.size() <= 0;
    }

    public static boolean isEmpty(Date date) {
        return date == null || date.getTime() == 0;
    }

    public static boolean isEmpty(JsonArray array){
        return array==null || array.size()==0;
    }

}
