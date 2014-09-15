package team165;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class PastrPlayer extends BasePlayer {
	
	public PastrPlayer(RobotController myRC){
		super(myRC);
		lastHP = 200;
	}
	
	public void run() {

		while (true) {
			try {
				
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	
}