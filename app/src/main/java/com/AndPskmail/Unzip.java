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
 
public static String Unzip(String inFilePath) throws Exception 
{	
    GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(inFilePath));
 
    String outFilePath = inFilePath.replace(".gz", "");
    OutputStream out = new FileOutputStream(outFilePath);
 
    byte[] buf = new byte[1024];
    int len;
    while ((len = gzipInputStream.read(buf)) > 0)
        out.write(buf, 0, len);
 
    gzipInputStream.close();
    out.close();
 
    new File(inFilePath).delete();
 
    return outFilePath;
}
}
