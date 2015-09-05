package com.coopox.carlauncher.datamodel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.activeandroid.serializer.TypeSerializer;

import java.io.ByteArrayOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-14
 */
public final class BitmapTypeSerializer extends TypeSerializer {
    @Override
    public Class<?> getDeserializedType() {
        return Bitmap.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return byte[].class;
    }

    @Override
    public Object serialize(Object data) {
        if (null == data) return null;
        Bitmap bitmap = (Bitmap)data;
        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream(bitmap.getRowBytes() * bitmap.getHeight());
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public Bitmap deserialize(Object data) {
        if (null == data) return null;
        byte[] blob = (byte[])data;
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }
}
