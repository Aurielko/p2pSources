package com.example.p2plib2.common;

import android.content.Context;

import com.example.p2plib2.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

public class FilesLoader {
    /*****************************************************************
     ********** Методы загрузки файла из внешнего источника **********
     *****************************************************************/
    public static String readInputStreamAsString(InputStream in)
            throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while(result != -1) {
            byte b = (byte)result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    /**Download form server files*/
    public String downloadJson(String query) {
        int count;
        HttpsURLConnection connection = null;
        String res = "";
        try {
            connection = new HttpsConnectionServicer().getHttpsConnection(new URL(query));
            if (connection != null) {
                connection.connect();
                long modified = connection.getLastModified();
                System.out.println(query + " " + connection.getResponseCode());
                int lengthOfFile = connection.getContentLength();
                InputStream in = connection.getInputStream();
                res = readInputStreamAsString(in);
                in.close();
            }
            return res;
        } catch (Exception e) {
            System.out.println(query + " " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }


    private static int downloadBinaryFile(String query, File dest) {
        int count;

        OutputStream output;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(query);

            connection = new HttpsConnectionServicer().getHttpsConnection(url);
            connection.connect();
            long modified = connection.getLastModified();

            System.out.println(query + " " + connection.getResponseCode());
            // this will be useful so that you can show a tipical 0-100%
            // progress bar

            int lengthOfFile = connection.getContentLength();
            //LogKES.debug(query + " " + lenghtOfFile);

            byte[] buffer = new byte[1024];
            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), buffer.length);

            // Output stream
            output = new FileOutputStream(dest);

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                output.flush();
            }
            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();
            dest.setLastModified(modified);

            if (dest.length() != lengthOfFile) {
                System.out.println(query + " -2");
                return -2;
            } else {
                System.out.println(query + " 0");
                return 0;
            }
        } catch (Exception e) {
            System.out.println(query + " error code: " + e.getMessage());
            return -1;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    //Метод надо вызывать в отдельном потоке
    public static void downloadFile(final Context cnt, final String url, final String filePath)
    {
        File file = null;
        File tmpFile = new File(filePath);
        if(tmpFile.exists()) {
            tmpFile.delete();
        }
        file = new File(filePath);
        downloadBinaryFile(url, file);
    }

    public static void saveFile(final Context cnt, final String url, Boolean isNeedUnzip, String comment, String downloadID, final String path_to_zip, final String path_to_unzip, String unzip_name) {
        Boolean isFileDownloaded = true;

        String full_url = url; //RestServicer.getUrlToDownloadFile(cnt, url, comment, downloadID);
        Logger.lg("Грузим файл из:" + full_url);
        downloadFile(cnt, full_url, path_to_zip);
        if (isNeedUnzip) {
            isFileDownloaded = unpackZip(path_to_unzip, unzip_name);
        }
        if (isFileDownloaded)
        {
            //Отправляем лог на сервер что файл скачался
        }
    }

    public static boolean unpackZip(String path, String zipname)
    {
        Logger.lg("Unzip file: " + path + zipname);
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(path + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(path + filename);
                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }
                fout.close();
                zis.closeEntry();
            }
            zis.close();
        }
        catch(IOException e)
        {
            Logger.lg("" + e.getMessage());
            return false;
        }
        return true;
    }
}