package com.coopox.carlauncher.misc;

import com.coopox.common.utils.StreamUtils;

import java.io.*;

public class SerializationHelper {

    public static <T> byte[] serialize(T object) {
        if (object == null) throw new RuntimeException("Cannot serialize a null object.");

        byte[] byteArray = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            byteArray = bos.toByteArray();

            oos.flush();
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtils.closeStream(bos);
            StreamUtils.closeStream(oos);
        }

        return byteArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] byteArray) {
        T object = null;

        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;

        try {
            bis = new ByteArrayInputStream(byteArray);
            ois = new ObjectInputStream(bis);
            object = (T) ois.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            // Mostly because Class def is changed, return null
            e.printStackTrace();
        } finally {
            StreamUtils.closeStream(bis);
            StreamUtils.closeStream(ois);
        }

        return object;
    }
}
