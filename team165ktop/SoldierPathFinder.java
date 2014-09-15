package team165;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.Robot;

public class SoldierPathFinder extends PathFinder {
	boolean waitToMove = false;
	Direction nextMoveDir = null;
	int lastMoveRound = 0;
	
	
	public boolean moveToTarget(boolean needsMoveFlag, boolean sneak) throws GameActionException{
		//if couldn't move in the last step due to ally soldier block and I chose to wait
		if (waitToMove){
			if(rc.canMove(nextMoveDir)){
				rc.move(nextMoveDir);
				waitToMove = false;//resume move
				return true;
			}else{
				//if hasn't moved in the last 20 rounds, reset the state
				if(Clock.getRoundNum()>lastMoveRound + 10){
					reset();
					waitToMove = false;
					return moveRandom(sneak);
				}
				return false;
			}
		}

		nextMoveDir = getNextMoveDir();
//		rc.setIndicatorString(2, "soldier can move? "+rc.isActive()+" and it wants to move in direction "+nextMoveDir);
//      rc.setIndicatorString(1, "current soldier is adjacent to at least to one earlier ally soldier?: "+needsMoveFlag);
//		bug_debug_print(0);
		if(nextMoveDir != Direction.NONE){
			if (rc.canMove(nextMoveDir)) {
				moveType(nextMoveDir, sneak);
		//	    rc.setIndicatorString(2, "soldier move in nextMoveDir: "+nextMoveDir);
				return true;
			} else {
				if (needsMoveFlag){
					//boolean flag = movePreferDir(nextMoveDir);
					//boolean flag = movePreferDir(nextMoveDir, oriDir.opposite());
					boolean flag = moveRandom(sneak);
					//rc.setIndicatorString(2, "move random succeed?"+flag+", prefered direction "+myLoc.directionTo(destination)+", forbidden direction +"+oriDir.opposite());
					//reset();
					//return true;
					return flag;
				}else{
					waitToMove = true;
					lastMoveRound = Clock.getRoundNum();
					return false;
				}
			}
		} else {
//			System.out.println("Robot " + rc.getRobot().getID() + " at round "
//					+ Clock.getRoundNum() + " want to move in direction.NONE");
			return false;
		}
	}

	
	public boolean moveRandom(boolean sneak) throws GameActionException {
 		//simple movement into a movable direction
//		needsMoveFlag = needsToMove();
//		if (needsMoveFlag) {
			for (Direction dir : allDirs) {
				if (rc.canMove(dir)) {	
					moveType(dir, sneak);

					String moveType = sneak?"sneak":"move";
//					rc.setIndicatorString(2, "soldier "+moveType+" in random direction "+dir);
					return true;
				}
			}
//		}
		return false;
	}
	
	private void moveType(Direction dir, boolean sneak) throws GameActionException {
		if (sneak){
			rc.sneak(dir);
		}else{
			rc.move(dir);
		}
	}


	public boolean movePreferDir(Direction chosenDir, boolean sneak) throws GameActionException {
		// simple movement into a movable direction
//		needsMoveFlag = needsToMove();
//		if (needsMoveFlag) {
			int forwardInt = chosenDir.ordinal();
			for (int directionalOffset : directionalLooks) {
				Direction trialDir = allDirs[(forwardInt + directionalOffset + 8) % 8];
				if (rc.canMove(trialDir)) {
					moveType(trialDir, sneak);
					return true;
				}
			}
//		}
		return false;
	}
	
	private boolean movePreferDir(Direction preferDir, Direction prohibitDir, boolean sneak) throws GameActionException {
//		needsMoveFlag = needsToMove();
//		if (needsMoveFlag) {
			int forwardInt = preferDir.ordinal();
//			for (int directionalOffset : directionalLooks) {
			for (int directionalOffset=0; directionalOffset!=8;  ++directionalOffset) {
				Direction trialDir = allDirs[(forwardInt + directionalOffset + 8) % 8];
				if (rc.canMove(trialDir) && !trialDir.equals(prohibitDir)) {
					moveType(trialDir, sneak);
					return true;
				}
			}
//		}
		return false;	
	}
	

	
}
