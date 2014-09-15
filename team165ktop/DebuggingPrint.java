package team165;

import battlecode.common.MapLocation;

public class DebuggingPrint extends StatStuff{
	
	public static void resetIndicatorString(){
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		rc.setIndicatorString(2, "");
	}
	
	public static void printLocation(int channel, String hintStr, MapLocation loc){
		if (loc==null)
			rc.setIndicatorString(channel, hintStr+"null");
		else
			rc.setIndicatorString(channel, hintStr+"("+loc.x+","+loc.y+")");
	}
	
	public static void printLocation(int channel, MapLocation loc){
		if (loc==null)
			rc.setIndicatorString(channel, "Location "+"null");
		else
			rc.setIndicatorString(channel, "Location ("+loc.x+","+loc.y+")");
	}

}
