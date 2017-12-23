package com.AndPskmail;

public class loggingclass {

	@SuppressWarnings("unused")
	private static String Application = "";

	
	public loggingclass(String app) {
		Application = app;
	}

	/**
	 * @param b 
	 * @param e 
	 * @param args
	 */
//JD allow for different log levels
	public static void writelog(String msg, Exception e, boolean b) {
//JD For the moment send this to the terminal
/*		if (e == null) {
			Log.e(Application, msg);
		} else {
			Log.e(Application, msg, e);
		}
*/
		Processor.mainwindow += msg;
		AndPskmail.mHandler.post(AndPskmail.addtoterminal);
	
	}
		
}
