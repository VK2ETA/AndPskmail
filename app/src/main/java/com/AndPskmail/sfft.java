/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.AndPskmail;

/**
 *
 * @author rein
 */
public class sfft {
    
//    static private Complex[] vrot;
//    static private Complex[] delay;
//    static private Complex[] bins;
//VK2ETA Debug FIX: NOT STATIC as there can be multiple instances (THOR)
    private double[] vrotR;
    private double[] vrotI;
    private double[] delayR;
    private double[] delayI;
    public double[] binsR;
    public double[] binsI;
    
    public int fftlen;
    public int first;
    private int last;
    private int ptr;
    double k2;
    private final double M_PI = (Math.PI);
    private final double K1 = 0.99999999999;
    
//    public void sfft(int len, int first1, int last1)
    public sfft(int len, int first1, int last1)
    {
    	//        System.out.println(len);
    	//        System.out.println(first1);
    	//        System.out.println(last1);

    	vrotR  = new double[len];
    	vrotI  = new double[len];
    	delayR = new double[len];
    	delayI = new double[len];
    	binsR  = new double[len];
    	binsI  = new double[len];
    	fftlen = len;
    	first = first1;
    	last = last1;
    	ptr = 0;
    	double phi = 0.0, tau = 2.0 * M_PI/ len;
    	k2 = 1.0;
    	for (int i = 0; i < len; i++) {
    		vrotR[i] = K1 * Math.cos (phi);
    		vrotI[i] = K1 * Math.sin (phi);
    		phi += tau;
    		delayR[i] = 0;
    		delayI[i] = 0;
    		binsR[i] = 0;
    		binsI[i] = 0;
    		k2 *= K1;
    	}
    }

    public void run (double inputR, double inputI){
//    	Complex z;
//    	Complex y;
    	double zR, zI;
    	double yR, yI;
    	double xR, xI;
    	
//    	z.setReal(input.getReal() - k2 * delay[ptr].getReal()) ;
//    	z.setImag(input.getImag() - k2 * delay[ptr].getImag()) ;
    	zR = inputR - k2 * delayR[ptr];
    	zI = inputI - k2 * delayI[ptr];

//    	delay[ptr] = input;
    	delayR[ptr] = inputR;
    	delayI[ptr] = inputI;

    	ptr = (ptr + 1) % fftlen;

    	for (int i = first; i < last; i++) {
//    		y = bins[i].plus(z);
//VK2ETA Debug fix: shift input as well
    		yR = binsR[i - first] + zR;
    		yI = binsI[i - first] + zI;
//    		bins[i - first] = y.multi(vrot[i]);
    		xR = vrotR[i];
    		xI = vrotI[i];
    		binsR[i - first] = yR * xR - yI * xI; //real*r.real - imag*r.imag
    		binsI[i - first] = yR * xI + yI * xR; //real*r.imag + imag*r.real
    		//                System.out.println(i);
    		//                System.out.println(bins[i - first]);
    	}
    }
}
