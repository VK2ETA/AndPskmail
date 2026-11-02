/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.AndPskmail;

import java.io.*;
import java.util.zip.*;

/**
 *
 * @author rein
 */

public class Unzip {

    public static String Unzip(String inFilePath) throws Exception {
        InputStream fileIn = AppStorage.openInputStream(inFilePath);
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileIn);

        String outFilePath = inFilePath.replace(".gz", "");
        OutputStream out = AppStorage.openOutputStream(outFilePath);

        byte[] buf = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buf)) > 0)
            out.write(buf, 0, len);

        gzipInputStream.close();
        out.close();

        AppStorage.delete(inFilePath);

        return outFilePath;
    }
}
