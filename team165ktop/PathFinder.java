package team165;
//This PathFinder only take into account the static map terrain info
//a higher level move fun (that also takes into account the moving structure, like soldier, etc)
//is in SoldierPathFinder

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

enum BUGSTATE {
	FLOCKING, BUGGING, RETRIEVING
};

public class PathFinder extends StatStuff {
	
	public static int forwardDirectionalLooks[] = new int[]{0,1,-1, 2, -2};
	public static Direction defaultDir = Direction.NONE;
	
	// path vector: used to store info of turning points on the path from A to
	// B, info format 1000000*turningpoint.x + 1000*turningpoint.y + facing_dir.ordinal;
	public int[] path = new int[mapMaxSize];

	// Whenever we encounter a dead end, we retrieve back to the last bugging
	// site. If the other (less prior) bugging direction has not been explored, explore;
	// otherwise retrieve to the second last bugging site, etc. if already
	// explored both bugging direction for each bugging site, and path is still
	// not found, return NO PATH!!!
	
	// bugDirRecord: 10000*is_the_other_bugging_direction_explored +
	// 1000*lessPrior_buggingDir+100*lessPrior_bugging_exitDir + 
	// 10*prior_buggingDir + prior_bugging_exitDir
	
	// If it's just a turning point along flocking, no bugging needed, in order
	// to keep in step with path data, Record -1
	public int[] bugDirRecord = new int[mapMaxSize];

	public static MapLocation destination = null;
	protected MapLocation retrieveNode = null;
	public BUGSTATE bugstate = BUGSTATE.FLOCKING;

	public Direction curDir = defaultDir;
	public Direction optDir = defaultDir;
	public Direction bugExitDir = defaultDir;
	//whether we're bugging along a right-hand-sided wall
	public boolean rhsBug = false; 
	
	// return the next MOVABLE direction
	public Direction getNextMoveDir() throws GameActionException {
//		DebuggingPrint.resetIndicatorString();
		if (!myLoc.equals(destination)) {
			switch (bugstate) {
			case BUGGING:
				//TODO need to do something to prevent back tracking!!!!
				if (noBlockingTerrain(bugExitDir)) { 
					int pathIdx = getNextPathNodeIdx();
					setPathNode(myLoc, bugExitDir, pathIdx);
					bugDirRecord[pathIdx] = -1;
					// TODO Need further check of bugging EXIT condition in function facingTarget(dir)
					// Only change the state to flocking if inner product of the optDir and bugExitDir are >0 ?												
					if (facingTarget(bugExitDir)) {
						bugstate = BUGSTATE.FLOCKING;
						curDir = bugExitDir;
//						rc.setIndicatorString(0,"Robot exit bugging along direction "+ curDir);
					} else {
						// keep bugging, and keep rhsBug chirality
						// Also, in order to re-calculate the new bugging dir, set curDir = defaultDir as a marker 
						Direction recordedDir = bugExitDir;
						// cal new bugging dir and update info for next round
						findSingleBuggingDirection(myLoc.add(bugExitDir), bugExitDir.opposite());
						rc.setIndicatorString(1,"Robot will exit current bugging along direction "+ recordedDir);
						return recordedDir; //guaranteed no blocking wall in the bug ExitDir
					}
				} else if (noBlockingTerrain(curDir)) { // keep bugging along the same direction
				
//					DebuggingPrint.resetIndicatorString();
//					rc.setIndicatorString(0, "Robot keep bugging along direction " + curDir);

				} else {
					// Scenario: cannot exit bugging and cannot move along the previous bugging dir
					// TODO what if prioritized action fails? prevent back tracking
					// If we hit a wall or OFF_MAP, try to select between keep bugging and retrieve
					// if (bugBlockTT.ordinal()>=2)
					TerrainTile bugBlockTT = rc.senseTerrainTile(myLoc.add(curDir));
											
					// If we hit a wall, prioritize with keep bugging along the wall
					if (bugBlockTT == TerrainTile.VOID) {
						// note that path, bugDireRecord, curDir, bugExitDir
						// have been updated internally in findSBD function
						// also, this action does not change rhsBug chirality
						findSingleBuggingDirection(myLoc, curDir);
						
//						DebuggingPrint.resetIndicatorString();
//						rc.setIndicatorString(0,"Robot find a new bugging direction "+ curDir);
//						rc.setIndicatorString(1,"Now bugging exit direction " + bugExitDir);
					} else if(bugBlockTT == TerrainTile.OFF_MAP){
						// If we hit OFF_MAP, prioritize with retrieve
						bugstate = BUGSTATE.RETRIEVING;
						curDir = curDir.opposite();
						if (noBlockingTerrain(curDir)) {
							//since we only use retrieveNode info in retrieving scenario, 
							//and we don't keep update rhsBug flag, even if here it's possible
							//instead, we set the rhsBug whenever we re-enter a bugging state
							//rhsBug = !rhsBug;
							
							retrieveNode = getLastPathNodePos();

//							DebuggingPrint.resetIndicatorString();
//							rc.setIndicatorString(0,"Robot hit OFF_MAP, and will retrieve along dir " + curDir);
//							rc.setIndicatorString(1, "Retireve to node ("+ retrieveNode.x + " , "+ retrieveNode.y + ")");
						}
					}
				}
				break;
			case FLOCKING:
				// this is like the tangent move, cal the best flocking moving dir for each round
				optDir = myLoc.directionTo(destination);
				bugstate = resetState();

//				rc.setIndicatorString(0, "destination is (" + destination.x + " , " + destination.y + ")");
//				rc.setIndicatorString(1, "Robot's optimal direction to the target is " + optDir);
//				rc.setIndicatorString(2, "Robot is switched from flocking state to " + state);
				
				if (bugstate == BUGSTATE.FLOCKING) {
					if (optDir != curDir) {
						curDir = optDir;
						setFlockTurningNode();
					}				
//					DebuggingPrint.resetIndicatorString();
//					rc.setIndicatorString(1, "Robot is flocking along direction " + curDir);
				} else if (bugstate == BUGSTATE.BUGGING) {
					//even though dual bugging search might only end up with a single bugging dir
					//or even result in retrieving, we denote it the bi-hand-side search anyway
					//also note that path, bugDireRecord, curDir, bugExitDir have been updated internally
					findDualBuggingDirection(optDir);

//					DebuggingPrint.resetIndicatorString();
//					rc.setIndicatorString(0, "Robot start bugging along dir " + curDir);
//					rc.setIndicatorString(1, "Robot bugging exit direction is "	+ bugExitDir);					
				}
				break;
			case RETRIEVING:
				//keep retrieving until the retrieve Node is reached
				//note that we only use reach retrieveNode as RETRIEVING exit condition.
				//the rhsBug info is not up-to-date, and thus is NOT usable
				if(!myLoc.equals(retrieveNode)){
//					DebuggingPrint.resetIndicatorString();
//					rc.setIndicatorString(0, "robot keep retrieving along direction "+curDir);
				}else{
					//retrievingNode reached
//					DebuggingPrint.resetIndicatorString();
//					rc.setIndicatorString(0, "robot arrived at the last retrieve node and will reset state");
					resetRetrievePathNode();
				}
				break;
			default:
				System.out.println("path finder state not resolved");
				rc.resign();
				break;
			}
		} else {
//			System.out.println("destination reached");
			return defaultDir;
		}
		return curDir;
	}
	
	private boolean facingTarget(Direction dir) {
		MapLocation unitVec = DirOperation.dir2Vec(dir);
		//MapLocation vec2des = DirOperation.dir2Vec(myLoc.directionTo(destination));
		MapLocation vec2des = MapOperation.mlSubtract(destination, myLoc);
		//DebuggingPrint.printLocation(0, "unit vector ", unitVec);
		//DebuggingPrint.printLocation(1, "Vector to destination ", vec2des);
		//rc.setIndicatorString(2, ""+DirOperation.innerProduct(unitVec, vec2des));
		return (DirOperation.innerProduct(unitVec, vec2des) > 0)? true:false;
	}

	private void resetRetrievePathNode() throws GameActionException {
		int idx = getNextPathNodeIdx();
		int bugInfo = bugDirRecord[--idx];
		//If only one of the bugging direction have been visited
		//check the other bugging direction
		if(bugInfo>=100 && bugInfo<10000){
			//change curDir to the less prior bugging direction
			curDir = allDirs[bugInfo/1000];
			//It's possible other things have been moved here/blocked, re-check the moving possibility
			//now clump this possibility into the main getNextMove final canMove(curDir) check before returnint
			//mark that now the two bugging directions have both been explored
			bugDirRecord[idx] += 10000; 
			//path[idx]; keep the current pathNode along the path
			bugstate = BUGSTATE.BUGGING;
			bugExitDir = allDirs[(bugInfo/100)%10];
			//determine bugging chirality afresh, 
			//cause multiple retrieving might messed up the chirality  
			rhsBug = bugExitDir==curDir.rotateRight()?true : false;
		}else{ 
		//The node is a single dir bugging(<100) or flocking(-1) turning point
		//Or both bugging directions of the buggind node have been visited(>10000)
		//further retrieve one level up
			//erase current retrieveNode from the path
			path[idx] = 0;
			if(idx>0){
				bugInfo = bugDirRecord[--idx];
				int pathInfo = path[idx];
				curDir = allDirs[pathInfo%10].opposite();
				retrieveNode = getLastPathNodePos();
			}else{
				//If we retrieve back to the starting point, reset
				reset();
			}
		}			
	}

	public void setPath(MapLocation end) {
		destination = end;
		//System.out.println("bug destination is set to be ("+destination.x+","+destination.y+")");
		reset();
	}

	protected void reset() {
//		System.out.println("Robot " + rc.getRobot().getID() + " at round " + Clock.getRoundNum() + " reset the state. ");
		bugstate = BUGSTATE.FLOCKING;
		path = new int[mapMaxSize];
		bugDirRecord = new int[mapMaxSize];
		curDir = defaultDir;
		optDir = defaultDir;
		bugExitDir = defaultDir;
	}

	private MapLocation getLastPathNodePos() {
		int idx = getNextPathNodeIdx();
		int pathNodeInfo = path[--idx];
		//rc.setIndicatorString(2, ""+pathNodeInfo);
		int x = pathNodeInfo / 1000000;
		int y = (pathNodeInfo / 1000) - 1000 * x;
		return new MapLocation(x, y);
	}

	private BUGSTATE resetState() {
		BUGSTATE nextMoveState;
		if (noBlockingTerrain(optDir)) {
			nextMoveState = BUGSTATE.FLOCKING;
			// TODO Need to consider the returning case
		} else {
			nextMoveState = BUGSTATE.BUGGING;
		}
		return nextMoveState;
	}

	// This issues whenever when need to change flocking direction
	// or we need to change bugging direction along bugging (along the wall)
	private void setFlockTurningNode() {
		int pathIdx = getNextPathNodeIdx();
		setPathNode(myLoc, curDir, pathIdx);
		bugDirRecord[pathIdx] = -1;
	}

	private void setPathNode(MapLocation curLoc, Direction dir, int pathIdx) {
		path[pathIdx] = 1000000 * curLoc.x + 1000 * curLoc.y + dir.ordinal();
	}

	// get the node idx to write the next path turning/ bugging point
	private int getNextPathNodeIdx() {
		int idx = 0;
		while (path[idx] != 0)
			idx++;
		return idx;
	}
	
	// This function is executed whenever we need to change bugging dir along bugging from loc
	// including just jump out of a bugging state and enter a new state
	private void findSingleBuggingDirection(MapLocation loc, Direction startDir) throws GameActionException {
		Direction recordDir= curDir;
		curDir = startDir;
		// If we're moving and will continue to move along the rhs wall
		if (rhsBug) {
			 do{
				curDir = curDir.rotateLeft();
			}while (!noBlockingTerrain(loc, curDir));
			bugExitDir = curDir.rotateRight();
		} else { // If we're moving along the left-hand-side wall
			do{
				curDir = curDir.rotateRight();
			} while (!noBlockingTerrain(loc, curDir));
			bugExitDir = curDir.rotateLeft();
		}
		// If only dir to go is retrieving, e.g. bugging into the end of a narrow tunnel
		if(curDir == recordDir.opposite()){
			bugstate = BUGSTATE.RETRIEVING;
			resetRetrievePathNode();
		}else{ //it's a turning point, keep the node record
			int idx = getNextPathNodeIdx();
			bugDirRecord[idx] = 10 * curDir.ordinal() + bugExitDir.ordinal();
			setPathNode(loc, curDir, idx);	
		}
	}

	//Function only executed whenever we switch from state flocking to bugging
	// it's guaranteed it's the first time we visit the bugging site
	//bugDirRecord: 10000*is the other bugging direction explored + 1000*lessPrior_buggingDir+
	//100*lessPrior_bugging_exitDir + 10*prior_buggingDir + prior_bugging_exitDir
	private void findDualBuggingDirection(Direction startDir) throws GameActionException {
		Direction backStepDir = defaultDir;
		if (curDir!=defaultDir)
			backStepDir = curDir.opposite(); 
		Direction bugDir = startDir;  //which is optDir
		Direction clockWiseDir= defaultDir;
		Direction clockWise_bug_exitDir= defaultDir;
		Direction counterClockWiseDir= defaultDir; 
		Direction counterClockWise_bug_exitDir= defaultDir;
		
		//get the nodeIdx to write node information
		int pathIdx = getNextPathNodeIdx();		
		do{
			bugDir = bugDir.rotateRight();
		}while (!noBlockingTerrain(bugDir));
		if(bugDir!=backStepDir){
			clockWiseDir = bugDir;
			clockWise_bug_exitDir = clockWiseDir.rotateLeft();
		}
		// System.out.println("first clockwise bugging direction is "+ clockWiseDir);
		bugDir = startDir;
		do{
			bugDir = bugDir.rotateLeft();
		}while (!noBlockingTerrain(bugDir));
		if(bugDir!=backStepDir){
			counterClockWiseDir = bugDir;
			counterClockWise_bug_exitDir = counterClockWiseDir.rotateRight();
		}
		// System.out.println("first counter clockwise bugging direction is "+counterClockWiseDir);
		if(clockWiseDir != defaultDir && counterClockWiseDir!= defaultDir){
			int clockWiseDis = MapOperation.distanceSq(myLoc.add(clockWiseDir),destination);
			int counterClockWiseDis = MapOperation.distanceSq(myLoc.add(counterClockWiseDir), destination);

			if (clockWiseDis <= counterClockWiseDis) {
				curDir = clockWiseDir;
				rhsBug = false;
				bugExitDir = clockWise_bug_exitDir;
				bugDirRecord[pathIdx] = 10 * clockWiseDir.ordinal()
						+ clockWise_bug_exitDir.ordinal() + 1000
						* counterClockWiseDir.ordinal() + 100
						* counterClockWise_bug_exitDir.ordinal();
			} else {
				curDir = counterClockWiseDir;
				bugExitDir = counterClockWise_bug_exitDir;
				rhsBug = true;
				bugDirRecord[pathIdx] = 1000 * clockWiseDir.ordinal() + 100
						* clockWise_bug_exitDir.ordinal() + 10
						* counterClockWiseDir.ordinal()
						+ counterClockWise_bug_exitDir.ordinal();
			}
			setPathNode(myLoc, curDir, pathIdx);
		}else if(clockWiseDir != defaultDir){
			curDir = clockWiseDir;
			rhsBug = false;
			bugExitDir = clockWise_bug_exitDir;
			bugDirRecord[pathIdx] = 10 * curDir.ordinal() + bugExitDir.ordinal();
			setPathNode(myLoc, curDir, pathIdx);			
		}else if(counterClockWiseDir!= defaultDir){
			curDir = counterClockWiseDir;
			rhsBug = true;
			bugExitDir = counterClockWise_bug_exitDir;
			bugDirRecord[pathIdx] = 10 * curDir.ordinal() + bugExitDir.ordinal();
			setPathNode(myLoc, curDir, pathIdx);	
		}else{
			//retrieve e.g. flocking into the end of a narrow tube
			if (backStepDir!=defaultDir){
				bugstate = BUGSTATE.RETRIEVING;
				resetRetrievePathNode();
			}else{
				DebuggingPrint.resetIndicatorString();
				rc.setIndicatorString(0, "robot cannot move to any direction!!!");
				System.out.println("robot "+rc.getRobot().getID()+ " at round " + Clock.getRoundNum() + " cannot move to any direction!!!");
				rc.yield();
			}
		}
		// System.out.println("prioritized bugging direction chosen is "+curDir);	
	}
	
	//Definitely forbidden region
	//void or wall
	//region too close to enemyHQ
	private boolean noBlockingTerrain(MapLocation loc, Direction dir) {
		if (rc.senseTerrainTile(loc.add(dir)).ordinal() > 1) {
			return false;
		} else if(loc.add(dir).equals(myHQ)){
			return false;
		}else{
			return loc.add(dir).distanceSquaredTo(enemyHQ) <= HQAttackRange ? false : true;
		}
//		return rc.senseTerrainTile(loc.add(dir)).ordinal()<2? true: false;
	}

	private boolean noBlockingTerrain(Direction dir) {		
		if (rc.senseTerrainTile(myLoc.add(dir)).ordinal() > 1) {
			return false;
		} else if(myLoc.equals(myHQ)){
			return false;
		}else{
			return (myLoc.add(dir)).distanceSquaredTo(enemyHQ) <= HQAttackRange ? false : true;
		}
//		return rc.senseTerrainTile(myLoc.add(dir)).ordinal()<2? true: false;
	}
	
	public MapLocation getDestination(){
		return destination;
	}
	
	public void bug_debug_print(int row){
		String bugChirality = rhsBug?"rhs bugging":"lfh bugging";
		String str = (bugstate==BUGSTATE.BUGGING)? bugChirality+", bugging exit state is "+bugExitDir:"";
		rc.setIndicatorString(row, "Bug in state "+bugstate+", optDir = "+optDir+", current dir: "+ curDir + str);
	}
	
	public void printPath() {
		int i;
		for (i = 0; path[i] != 0; ++i)
			System.out.print(" " + path[i]);
		System.out.print("\n");
		int ilast = i;
		for (i = 0; i != ilast; ++i)
			System.out.print(" " + bugDirRecord[i]);
		System.out.print("\n");
	}
	
}