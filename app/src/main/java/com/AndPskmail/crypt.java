/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.AndPskmail;

import java.math.BigInteger;
import java.util.Random;
import java.security.*;


/**
 *
 * @author rein
 */


public class crypt  {

    BigInteger p;  // prime
    BigInteger g;  // base
    BigInteger mypublickey;
    static String mypubkey;
    BigInteger myprivatekey;
    static String hiskey;
    BigInteger hispublickey;
    BigInteger mysecret;
    static String prk = "";
    String strkey;  // secret key
    final static String[] nrs = {"563","587","719","839","863","887","983","1019",
        "1187","1283","1307","1319","1367","1439","1487","1523","1619","1823",
        "1907","2027","2039","2063","2099","2207","2447","2459","2579","2819",
        "2879","2903"};

//    public void crypt () {
    public crypt () {
        ;
    }

    public void set_p (String prime) {
        BigInteger pp = new BigInteger(prime);
        p = pp;
    }

    public String dh (String otherpublickey){
        // generate private key
        int inx = Integer.parseInt(Processor.connectsecond);
        p = new BigInteger(nrs[inx/2]);
        g = new BigInteger("5");

        hispublickey = new BigInteger(otherpublickey);
        
        Random r = new Random();

        int myrd = Integer.parseInt(Processor.hispubkey);

        prk = Integer.toString(r.nextInt(myrd));

        myprivatekey = new BigInteger(prk);  // my secret

        mypublickey = g.modPow(myprivatekey,p); // send to server

        String pubkey = mypublickey.toString();

        mysecret = hispublickey.modPow(myprivatekey, p);

        Processor.strkey = mysecret.toString();

        return (pubkey);
    }

    public String encrypt (String key, String txt) {
//System.out.println("MSG:" + Main.strkey);
              mypubkey = this.dh (key);
              String rd = "";
//System.out.println("STRKEY:" + Main.strkey);
            try {
               MessageDigest md=MessageDigest.getInstance("MD5");
               md.reset();
               md.update(Processor.strkey.getBytes());
               byte[] rawData = md.digest();
               StringBuilder secr = new StringBuilder();
                 for (int i=0;i<rawData.length;i++) {
                    String hex = Integer.toHexString(0xFF & rawData[i]);
                    if(hex.length()==1)
                        secr.append('0');

                    secr.append(hex);
                 }
                rd = secr.toString();
                rd += rd;
                rd += rd;
                rd += rd;
                
//  System.out.println(rd);
            }
            catch (NoSuchAlgorithmException e) {
                ;
            }

             byte[] at = txt.getBytes();
             int l = at.length;
             String as = "";
//System.out.println("TXTlength:" + l);
             for ( int i = 0; i < l; i++) {
                 int v = at[i] & 0xff;
                 as += Integer.toHexString(v);
             }
             
             byte[] a = as.getBytes();
             byte[] b = rd.getBytes();
             int c = 0;
             int ba = 0;
             int bb = 0;
             String d = "";
             String e = "0";

             for (int i = 0; i < a.length; i++) {
                 ba = conv (a[i]);
                 bb = conv (b[i]);
                c =  (byte) (ba ^ bb) ;

                e = HEX_STR_TABLE[c];
                d += e;
// System.out.println("PUBLENGTH:" + d.length());
             }
             
            return mypubkey + "," + d;
    }

        static final byte[] HEX_CHAR_TABLE = {
            (byte)'0', (byte)'1', (byte)'2', (byte)'3',
            (byte)'4', (byte)'5', (byte)'6', (byte)'7',
            (byte)'8', (byte)'9', (byte)'a', (byte)'b',
            (byte)'c', (byte)'d', (byte)'e', (byte)'f'
          };

        static final String[] HEX_STR_TABLE = {
            "0", "1", "2", "3",
            "4", "5", "6", "7",
            "8", "9", "a", "b",
            "c", "d", "e", "f"
          };

        private int conv (byte x) {
            @SuppressWarnings("unused")
			int y = 0;
            int z = (int) x;

            for (int i = 0; i < 16; i++){
                if ((int) HEX_CHAR_TABLE[i] == z)
                    return i;
            }
            return 0;
        }


}
