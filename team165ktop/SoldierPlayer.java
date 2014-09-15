package team165;

import battlecode.common.*;

public class SoldierPlayer extends BasePlayer {
	private int mySoldierID;
	private static int myGetSquadIDch=0;

	private static int mySquadID = 0;
	private static int myLastSquadID = -1;
	private static int mySquadCh = 2 * mapMaxSize + 100 * mySquadID;

	protected static SoldierPathFinder bug;
	protected static boolean needsMoveFlag = false;
	
	private static MapLocation rallyPoint = null;
	private static int rallySetTime = -1;

	private static MapLocation attackTargetLoc = null;
    
	// TASK BUILDPASTR
	private static int  NTcandidateID = 0;
	private static int  PASTRcandidateID = 0;
	private static int NTconstructTime = 0;
	private static int buildTimeDelay = 50;
	// TASK STAY
	private static MapLocation enemyCOM = null;
	private static MapLocation defaultECOM = enemyHQ;
	private static MapLocation[] stayLocs = null;
	private static MapLocation stayLoc = null;
	private static int stayLocOrdinal = -1;

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		bug = new SoldierPathFinder();
		mySoldierID = rc.getRobot().getID();
		myTask = TASK.NONE;
		lastTask = TASK.NONE;
		myLoc = rc.getLocation();
		lastHP = 100;
	}

	public void run() {
		while (true) {
			try {
				if (myGetSquadIDch != 0) {
					getUsefulInfo();
				} else {
					get_myGetSquadIDch();
					if (myGetSquadIDch != 0) {
						getUsefulInfo();
					}
				}
				switch (myTask) {
				case ASSEMBLE:
					assemble();
					break;

				case ATTACK:
					performAttack();
					break;

				case BUILDPASTR:
					buildPASTR();
					break;

				case STAY:
					stay();
					// DebuggingPrint.printLocation(2,
					// "my defense location is ", bug.getDestination());
					break;

				default:
					if(rc.isActive()&&rc.canMove(myHQ.directionTo(myLoc)))
						rc.move(myHQ.directionTo(myLoc));
					break;
				}
				
//				DebuggingPrint.printLocation(0, "my default COM is ", defaultECOM);
//				 rc.setIndicatorString(0, "my task is " + myTask);
//				 if(rallyPoint != null){
//					 DebuggingPrint.printLocation(1, "my rally point is ", rallyPoint);
//				 }
//				 rc.setIndicatorString(1, "my getSquadIDch is " + myGetSquadIDch);
//				 rc.setIndicatorString(2, "my Squad ID "+mySquadID+", and my Ch is "+mySquadCh);
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private void get_myGetSquadIDch() throws GameActionException {
		// get mySquadID
		int soldierIDch = mapMaxSize + 1;
		int IDread = rc.readBroadcast(soldierIDch);
		while (IDread != mySoldierID && IDread !=0) {
			soldierIDch += 2;
			IDread = rc.readBroadcast(soldierIDch);
		}
		if(IDread!=0){
			myGetSquadIDch = soldierIDch + 1;
		}
	}

	protected void getUsefulInfo() throws GameActionException {
		myLoc = rc.getLocation();
		curTime = Clock.getRoundNum();
		isAttacked = (rc.getHealth() < lastHP);
		lastHP = rc.getHealth();
		senseNearby(soldierSenseRange);
		mySquadID = rc.readBroadcast(myGetSquadIDch);
		mySquadCh = 2 * mapMaxSize + 100 * mySquadID;
		lastTask = myTask;
		updateSquadInfo();
		// myTask = TASK.getTask(rc.readBroadcast(mySquadCh+1));
	}

	private void updateSquadInfo() throws GameActionException {
		int rallyTime = rc.readBroadcast(mySquadCh + 2);
		if (rallySetTime != rallyTime) {
			if (lastTask == TASK.BUILDPASTR){
				//if I'm already picked for NT or PASTR building, go no where
				if( mySoldierID == NTcandidateID || mySoldierID == PASTRcandidateID){
					return;
				}
			}
			// update myTask and reset state
			myTask = TASK.getTask(rc.readBroadcast(mySquadCh + 1));
			rallySetTime = rallyTime;
			// reset new destination
			rallyPoint = new MapLocation(rc.readBroadcast(mySquadCh + 3),
					rc.readBroadcast(mySquadCh + 4));
			bug.setPath(rallyPoint);
//			DebuggingPrint.printLocation(0, "initialize assemble destination ",rallyPoint);
			//reset parameters for stay condition
			resetSoldierState();
		}
	}

	private void resetSoldierState() {
		enemyCOM = null;
		defaultECOM = enemyHQ;
		stayLocs = null;
		stayLoc = null;
		stayLocOrdinal = -1;
		NTcandidateID = 0;
		PASTRcandidateID = 0;
		NTconstructTime = 0;
	}

	private void stay() throws GameActionException {
		attack();	
		
        MapLocation newEnemyCOM = calculateEnemyCOM();       
//        DebuggingPrint.printLocation(0, "newEnemyCOM is ", newEnemyCOM);
		
        if (!newEnemyCOM.equals(enemyCOM)) {
			enemyCOM = newEnemyCOM;
			Direction dir2Enemy = rallyPoint.directionTo(enemyCOM);		
			determineStayLocs(rallyPoint, dir2Enemy);
			stayLocOrdinal = -1;
			
//			DebuggingPrint.printLocation(0, "enemy COM ", enemyCOM);
		}

		stayLoc = findStayLoc();
//		DebuggingPrint.printLocation(2, "my stayLoc is ", stayLoc);
//		DebuggingPrint.printLocation(2, "defalt enemy COM is updated to ", defaultECOM);
		// enough soldier have reached the stayLoc for defense
		if (stayLoc == null) {
			soldierDefenseCompleteFeedBack();
		}else{		
			if (!bug.getDestination().equals(stayLoc)) {
				bug.setPath(stayLoc);
			}
			if (!myLoc.equals(stayLoc)) {
				if (rc.isActive()){
					needsMoveFlag = needsToMove();
					bug.moveToTarget(needsMoveFlag, false);
				}
			} else {
				////////////////DO NOTHING
			}
		}
	}

	private MapLocation calculateEnemyCOM() throws GameActionException {
		int curTime = Clock.getRoundNum();
		//if another ally soldier sensed more enemies in the last and cur round that what I sensed in the curRound
		//update my enemy info COM info
		if (rc.readBroadcast(mySquadCh+14) > numEnemies &&
				rc.readBroadcast(mySquadCh+11) > curTime-2){
			MapLocation readEnemyCOM = new MapLocation(rc.readBroadcast(mySquadCh+12), rc.readBroadcast(mySquadCh+13));
			if (defaultECOM.equals(enemyHQ))
				defaultECOM = readEnemyCOM;
			return readEnemyCOM;
		}else{
			MapLocation mySensedECOM = mySensedEnemyCOM();
			if(!mySensedECOM.equals(defaultECOM)){
				//broadcast danger Message
				if(rc.readBroadcast(mySquadCh+9)!=1){
					//only send danger Message if out numbered by enemySoldier a lot
					if (numAllies < numEnemies*1.5){
						rc.broadcast(mySquadCh+9, 1);
//						rc.setIndicatorString(1, "broadcasting danger infomation"+curTime);
					}
				}
				broadcastSensedEnemyInfo(mySensedECOM, numEnemies, curTime);
				return mySensedECOM;
			}else{
				//No enemyObserved, reset message to safe IF ONLY it has not been reset
				if(rc.readBroadcast(mySquadCh+9)!=0){
					rc.broadcast(mySquadCh+9, 0);
//					rc.setIndicatorString(1, "broadcasting safe infomation"+curTime);
				}
				//read the last enemy coming direction, and replace it with enemyHQ
				return defaultECOM;
			}
		}
	}

	private MapLocation mySensedEnemyCOM() throws GameActionException {
		// without any enemy in sigh, align perpendicular to direction to enemyHQ
		// the best? estimate of enemy soldier coming direction
		return  (numEnemies==0)? defaultECOM : MapOperation.meanLocation(nearbyELoc);
	}

	
	//only record the most enemy COM loc sensed so far
	//mySquadCh+11 enemySensedRound, +12 enemy COM x, +13 enemy COM y, +14 num of sensed enemies
	private void broadcastSensedEnemyInfo(MapLocation enemyloc, int enemyNum, int curTime) throws GameActionException {
		int ch = mySquadCh+11;
		if (rc.readBroadcast(ch)!=curTime){
			if (!enemyloc.equals(enemyHQ)){
				rc.broadcast(ch, curTime);
				rc.broadcast(ch+1, enemyloc.x);
				rc.broadcast(ch+2, enemyloc.y);
				rc.broadcast(ch+3,  enemyNum);
//				rc.setIndicatorString(0, "broadcasting enemy infomation at new turn "+curTime);
			}
		}else{
			//guaranteed (rc.readBroadcast(ch+3)<num){
			rc.broadcast(ch+1, enemyloc.x);
			rc.broadcast(ch+2, enemyloc.y);
			rc.broadcast(ch+3,  enemyNum);
//			rc.setIndicatorString(0, "broadcasting enemy infomation at time "+curTime);
		}
	}
	
	private MapLocation findStayLoc() throws GameActionException {
		// This if is executed immediately after stayLocs reset
		// if I'm standing in an allowed location don't move to save power
		if (stayLoc != null && myLoc.equals(stayLoc) && stayLocOrdinal == -1) {
			for (MapLocation loc : stayLocs) {
				if (loc == null) {
					break;
				} else if (stayLoc.equals(loc)) {
					return stayLoc;
				}
			}
		}
		
		//This if is only executed if we searched the entire stayLocs last round, 
		//and couldn't find a vacant stayLoc, then reset the search start point
		if(stayLoc == null && stayLocOrdinal > 0) {
			stayLocOrdinal = -1;
		}
		//if we start the stayLoc search
		if(stayLocOrdinal == -1) {
			stayLoc = stayLocs[++stayLocOrdinal];
		}
		while (locOccupiedbyTeamates(stayLoc)) {
			stayLoc = stayLocs[++stayLocOrdinal];
		}
//		DebuggingPrint.printLocation(2, "the first unoccupied stay loc is ", stayLoc);
		return stayLoc;

	}

	private boolean locOccupiedbyTeamates(MapLocation loc) throws GameActionException {
		if (loc == null) {
			return false;
		} else if (myLoc.equals(loc)) {
			return false;
		} else if (rc.canSenseSquare(loc)) {
			Robot r = (Robot) rc.senseObjectAtLocation(loc);
			return (r != null && r.getTeam() == myTeam) ? true : false;
		} else {
			// no occupied soldier or cannot sense it yet, stay optimistic,
			return false;
		}
	}

	private void soldierDefenseCompleteFeedBack() {
		// TODO Auto-generated method stub

	}


//	private void broadcastSensedEnemyInfo() throws GameActionException {
//		int curTime = Clock.getRoundNum();
//		for (int i=0; i!=numEnemies; ++i){
//			int curEID = nearbyEnemies[i].getID();
//			int ch = mySquadCh + 11;
//			int recordedEID = rc.readBroadcast(ch);
//			while(recordedEID!=0){	
//				if (recordedEID == curEID){
//					rc.broadcast(ch+1, curTime);
//					rc.broadcast(ch+2, 1000 * nearbyELoc[i].x + nearbyELoc[i].y);
//					break;
//				}else{
//					ch += 3;
//					recordedEID = rc.readBroadcast(ch);
//				}
//			}
//		}		
//	}


	private void determineStayLocs(MapLocation startPos, Direction dir2Enemy)
			throws GameActionException {
		int[] LtrialDis = { 6, 7, 5, 8 };
//		int[] LtrialDis = { 3, 4, 2, 5 };
		int[] PtrialDis = { 0, 1, -1, 2, -2, 3, -3, 4, -4};
//		int[] PtrialDis = { 0, 1, -1, 2, -2, 3, -3 }; // , 4, -4, 5, -5};
		Direction perDir = dir2Enemy.rotateRight().rotateRight();
		MapLocation candidatePos = null;

		stayLocs = new MapLocation[PtrialDis.length + 1];
		int numLocs = 0;
		MapLocation[] tempStayLocs;
		int tempNumLocs = 0;

		for (int longitudinalDis : LtrialDis) {
			tempStayLocs = new MapLocation[PtrialDis.length + 1];
			tempNumLocs = 0;
			for (int perpendicularDis : PtrialDis) {
				candidatePos = startPos.add(dir2Enemy, longitudinalDis).add(
						perDir, perpendicularDis);
				if (rc.senseTerrainTile(candidatePos).ordinal() < 2)
					tempStayLocs[tempNumLocs++] = candidatePos;
			}
			if (tempNumLocs > numLocs) {
				stayLocs = tempStayLocs;
				numLocs = tempNumLocs;
			}
		}
		stayLocs[numLocs] = null;
//		if (numLocs!=0){	
//			rc.setIndicatorString(1, "possible stay locs are ("+ stayLocs[0].x +
//				" , "+stayLocs[0].y + "), aligned along direction "+perDir);
//		}else{
//			System.out.println("try to find stay location from ("+startPos.x+","+startPos.y+") perpendicular to direction "+ dir2Enemy);
//			System.out.println("cannot find any stayable locations at round "+curTime);
//			rc.resign();
//		}
	}

	private void assemble() throws GameActionException {
		// always prioritize with shooting enemy
		attack();
		if (rallyPoint==null)
			return;
		if (myLoc.distanceSquaredTo(rallyPoint) <= moveScareRange) {
			myTask = TASK.STAY;
		} else if (rc.isActive()) {
			needsMoveFlag = needsToMove();
			bug.moveToTarget(needsMoveFlag, false);
		}
	}

	private void attack() throws GameActionException {
		if (numEnemies != 0) {
			if (rc.isActive()) {
				MapLocation targetEnemyLoc = lowestHPEnemyLoc(soldierAttackRange);
				// Always prioritize with shooting enemies
				if (targetEnemyLoc != null) {
					rc.attackSquare(targetEnemyLoc);
				}
			}
		}
	}

	private void performAttack() throws GameActionException {
		attack();
		if (rc.isActive()) {
			needsMoveFlag = needsToMove();
			bug.moveToTarget(needsMoveFlag, false);
		}
	}

	

	private void buildPASTR() throws GameActionException {
		//always check if there're enemies around, send back danger information
		if(numAllies < numEnemies*1.5){
			//only send danger Message if out numbered by enemySoldier a lot
			rc.broadcast(mySquadCh + 9, 1);
			rc.broadcast(mySquadCh + 10, curTime);
//			rc.setIndicatorString(1, "broadcasting danger information to ch "
//					+ (mySquadCh + 9) + " at round " + curTime);
		}else{
			if(rc.readBroadcast(mySquadCh+9)!=0 && curTime > rc.readBroadcast(mySquadCh+10)+5){
				rc.broadcast(mySquadCh+9, 0);
//				rc.setIndicatorString(1, "broadcasting safe infomation to ch "+(mySquadCh+9)+" at round "+curTime);
			}
		}
		
		if (myLoc.distanceSquaredTo(rallyPoint) > moveScareRange) {
			// if I'm far away from the rallyPoint, just rush to des without
			// having to worry about colliding with NT/PASTR candidate
			if (rc.isActive()) {
				needsMoveFlag = needsToMove();
				bug.moveToTarget(needsMoveFlag, false);
			}
		} else {
			// close to rally point, read NT and PASTR candidate info
			if (NTcandidateID == 0) {
				NTcandidateID = rc.readBroadcast(mySquadCh + 5) == 0 ? 0 : rc.readBroadcast(mySquadCh + 5);
			}
			if (PASTRcandidateID == 0) {
				PASTRcandidateID = rc.readBroadcast(mySquadCh + 7) == 0 ? 0: rc.readBroadcast(mySquadCh + 7);
			}

			if (mySoldierID == NTcandidateID) {
				// As NT or PASTR candidate soldier, construct
				if(!rc.isConstructing() && rc.isActive()){
					if(NTconstructTime == 0){
						NTconstructTime = rc.readBroadcast(mySquadCh+6);
					}
					rc.construct(RobotType.NOISETOWER);	
				}					
				if (rc.isConstructing() && rc.getConstructingRounds()<2)
					rc.broadcast(mySquadCh+50, 1);
//				rc.setIndicatorString(0, "I'm "+rc.getConstructingRounds()+" away from construction complete");
//				rc.setIndicatorString(1, "my NT construction time is "+NTconstructTime);
			} else if (mySoldierID == PASTRcandidateID) {
				//if (!rc.isConstructing() && rc.readBroadcast(mySquadCh + 8) != 0) {
				//cause HQ may switch Task in between, and it may NOT update timer to construct PASTR
				if (NTconstructTime == 0 ){
					NTconstructTime = rc.readBroadcast(mySquadCh+6);
				}else {
					if (!rc.isConstructing() && curTime > NTconstructTime + buildTimeDelay) {
						if (rc.isActive()) {
							rc.construct(RobotType.PASTR);
						}
					}
					if (rc.isConstructing() && rc.getConstructingRounds() < 2) {
						rc.broadcast(mySquadCh + 51, 1);
					}
				}
//				rc.setIndicatorString(1, "my NT construction time is "+NTconstructTime);
//				if(rc.isConstructing()){
//					rc.setIndicatorString(0, "I'm "+rc.getConstructingRounds()+" away from construction complete");
//				}else{
//					rc.setIndicatorString(0, "I'm still waiting to construct PASTR");
//				}
			} else {
				// either NT or PASTR candidate has not been determined,
				// sneak to the rally point for chances of becoming NT or PASTR
				if (NTcandidateID == 0 || PASTRcandidateID == 0) {
					if (rc.isActive()) {
						needsMoveFlag = needsToMoveAroundPastr();
						bug.moveToTarget(needsMoveFlag, true);
					}
				} else {
					// once both NT and PASTR candidate have been picked,
					// other soldiers no longer need to go to rally point,
					// instead switch the TASK to STAY to protect them
					myTask = TASK.STAY;
				}
			}
		}
	}

	protected boolean needsToMoveAroundPastr(){
		//Just to find if we are adjacent to a nearby ally robot, radiusSquared=3
		//change the name here to distinguish from nearbyAllies etc in the StatStuff
		if (mySoldierID == NTcandidateID || mySoldierID == PASTRcandidateID){
			return false;
		}else{
			Robot[] myAdjacentAllies = rc.senseNearbyGameObjects(Robot.class, myLoc, 3, myTeam);
			int myID = rc.getRobot().getID();
			if(myAdjacentAllies.length>0){
			for(Robot thisRobot:myAdjacentAllies){
			    if (myID > thisRobot.getID()){
					return true;
				}
			}
			}
			return false;
		}
	}

	private boolean lowestIDinSquad(int soldierID) {
		for (int i = 0; i != numAllies; ++i) {
			if (nearbyAInfo[i].type == RobotType.SOLDIER
					&& soldierID > nearbyAllies[i].getID()) {
				return false;
			}
		}
		return true;
	}
}