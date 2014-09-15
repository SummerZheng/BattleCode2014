package team165;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer extends StatStuff {
	
	public static void run(RobotController rc) throws GameActionException{
		StatStuff.init(rc);
		
		switch (rc.getType()) {
		case SOLDIER:
//			new SoldierPlayer_PathFinderTest(rc).run();
			new SoldierPlayer(rc).run();
			break;
		case HQ:
//			new HeadQuarter_PathFinderTest(rc).run();
			new HeadQuarter(rc).run();
			break;
		case PASTR:
			new PastrPlayer(rc).run();
			break;
		case NOISETOWER: 
			new NoiseTower(rc).run(); 
			break;
		
		default:
			rc.yield();
			break;

		}
		
	}
	

}
