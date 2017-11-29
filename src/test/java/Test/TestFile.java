package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestFile {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeFile1");
			System.setProperty("AppName", "GrapeFile1");
			booter.start(1006);
		} catch (Exception e) {
			nlogger.logout(e);
		}
	}
}
