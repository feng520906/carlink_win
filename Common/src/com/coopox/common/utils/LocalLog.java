package com.coopox.common.utils;

import com.coopox.common.storage.ExternalStorage;
import com.coopox.common.storage.Storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 14/12/28
 */
public class LocalLog {

    public static void write(String log) {
        Storage storage = new ExternalStorage(".Logs", "local.log");
        File file = storage.getFile();

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, true);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日 HH:mm:ss");
            String date = simpleDateFormat.format(new Date());

            fileWriter.write(date + " : " + log);
            fileWriter.write(System.getProperty("line.separator"));
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeStream(fileWriter);
        }
    }

}
