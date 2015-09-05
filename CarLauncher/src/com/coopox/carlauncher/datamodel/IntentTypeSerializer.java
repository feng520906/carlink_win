package com.coopox.carlauncher.datamodel;

import android.content.Intent;
import com.activeandroid.serializer.TypeSerializer;

import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-15
 */
public class IntentTypeSerializer extends TypeSerializer {
    @Override
    public Class<?> getDeserializedType() {
        return Intent.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return String.class;
    }

    @Override
    public String serialize(Object data) {
        return ((Intent) data).toUri(Intent.URI_INTENT_SCHEME);
    }

    @Override
    public Intent deserialize(Object data) {
        try {
            return Intent.parseUri((String)data, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
