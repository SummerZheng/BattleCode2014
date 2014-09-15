package team165;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class HeadQuarter extends BasePlayer {
	private boolean twoHQclose = false;
	private static boolean[] dirSpawnable = new boolean[8];
	private static Direction[] spawnDirs = new Direction[8];
	private static int spawnableDirNum = 0;
	
	private static int nextSodierSquadID = 1;
	private static int nextSodierPostCh = mapMaxSize;
	private static boolean needUpdateSoldier = false;
	private static int soldierDetectableDelay = 1;
	private static int secondPASTRsoldier=0;
	private static int broadcastch = 2*mapMaxSize + 100 * nextSodierSquadID;
	
	private static int rallySetTime = 0;
	private static MapLocation rallyPoint = myHQ;
	private static MapLocation defaultRallyPoint;
	
	private static int buildTimeDelay = 50;
	private static MapLocation nextPASTRLoc = null;
	private static Robot PASTRcandidate = null; 
	
	private static boolean PASTRunderAttack = false;
	private static boolean NTpicked = false;
	private static Robot NTcandidate = null;
	private static int NTestBuildTime = -1;
	private static boolean PASTRpicked = false;
	private static Robot[] PASTRnearbyAllies = null;
	
	private MapLocation[] allyPastr;
	private MapLocation[] enemyPastr;
	private static int allyPastrNum = 0;
	
	private static Robot[] friendfireAllies = null;
	
	private static MapLocation attackTargetLoc = null;
	private static int battleEngageTime = 0;
	
	public HeadQuarter(RobotController myRC) throws GameActionException{
		super(myRC);
		myLoc = myHQ;
		myTask = TASK.HQSPAWN;
		checkTwoHQtooClose();
//		System.out.println("two HQ too close?"+twoHQclose);
		spawnDirDefautRallyPointInit();	
		rallyPoint = defaultRallyPoint;
		curTime = Clock.getRoundNum();
		rallySetTime = curTime;
		broadcastRallyPoint(broadcastch, TASK.ASSEMBLE, rallySetTime, rallyPoint); 
	}
	
	public void run() throws GameActionException {
		//senseCowGrowth();	
		while (true) {
//			rc.setIndicatorString(0, "broadcastch = "+broadcastch+"safety ch = "+rc.readBroadcast(broadcastch + 9));
			try {
				myTask = HQupdateTask();
				switch (myTask) {
				case HQSPAWN:
					HQspawn();
					break;

				case HQATTACK:
					HQattack();
					break;
					
				case HQATTACKPASTR:
					HQattackEnemyPastr();
					break;
					
				case HQMonitorPASTRConstruc:
				    HQmonitorConstruction();
				    break;
					
				default:
					HQmaintenance();
					break;
				}
//				rc.setIndicatorString(0, "detected ally num "+ numAllies);
//				rc.setIndicatorString(0, "detected enemy num "+ numEnemies);
				DebuggingPrint.printLocation(0, "Rally point is", rallyPoint);
//				DebuggingPrint.printLocation(1, "NextPASTR loc is", nextPASTRLoc);
//				DebuggingPrint.printLocation(1, "Default rally point is", defaultRallyPoint);
//				rc.setIndicatorString(1, "my last task is " + lastTask);
				
				rc.setIndicatorString(1, "my task is " + myTask+", next spawn soldier ID is "+nextSodierSquadID+
						"boradcastch is "+broadcastch);
				int NTID = rc.readBroadcast(broadcastch+5);
				int PASTRID = rc.readBroadcast(broadcastch+7);
				boolean safeTag = rc.readBroadcast(2*mapMaxSize+100+9)==0;
				rc.setIndicatorString(2, "NTpicked "+NTpicked+" ID "+NTID+", PASTR picked"+PASTRpicked+
						" ID "+PASTRID+" PASTR safe? "+safeTag);
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected TASK HQupdateTask() throws GameActionException {
		// TODO HQ get global info, judge which team is on the lead
		curTime = Clock.getRoundNum();
		senseGlobalAlly();
		HQsenseNearby();
		enemyPastr = rc.sensePastrLocations(enemyTeam);
		//TODO update my PASTR info more efficiently
		allyPastr = rc.sensePastrLocations(myTeam);
		
		if(allyPastrNum < allyPastr.length){
			//a new PASTR has been constructed, update
			allyPastrNum = allyPastr.length;
		}else if(allyPastrNum > allyPastr.length){
			nextSodierSquadID = 1;
			broadcastch = 2*mapMaxSize+100;
			resetSoldierSquadID();
			clearBroadcastedInfo();
			//at least one PASTR has been defeated, regroup into 1 squad
			if(allyPastr.length==0){
				allyPastrNum = 0;
				//if another PASTR is under construction reset the group to that location
				if (myTask == TASK.HQMonitorPASTRConstruc){
					rallySetTime = curTime;
					broadcastRallyPoint(broadcastch, TASK.BUILDPASTR, curTime, rallyPoint);
				}else{
					//regroup to defautRally point around myHQ
					rallySetTime = curTime;
					rallyPoint = defaultRallyPoint;
					broadcastRallyPoint(broadcastch, TASK.ASSEMBLE, curTime, rallyPoint);
					lastTask = myTask;
					return TASK.HQSPAWN;
				}
			}else{
				allyPastrNum = allyPastr.length;//i.e. 1
				//one PASTR still exists, residue soldier merge with that group
				rallySetTime = curTime;
				rallyPoint = allyPastr[0];
				broadcastRallyPoint(broadcastch, TASK.ASSEMBLE, curTime, rallyPoint);
			}
		}else if(allyPastrNum!=0){
			int squadinCombat = senseDangeredPASTR();
			if(squadinCombat!=0){
				nextSodierSquadID = squadinCombat;
			}else{
				if(secondPASTRsoldier!=0)
					nextSodierSquadID = 2;
			}
		}
		
		if(curTime>1500 && allyPastrNum==0){
			resetSoldierSquadID();
			clearBroadcastedInfo();
			forcedBuildingPASTR(1, 25);
			return myTask;
		}
		
		// if enemy has built a PASTR before I do, attack with all my current soldier
		if (enemyPastr.length!=0 && allyPastr.length == 0){
			//Consider switch task iff neither or both NT and PASTR has picked
			//if either NT or PASTR is already under construction,
			//finish the candidate picking without rush into combat
			if((NTpicked && PASTRpicked) || (!NTpicked && !PASTRpicked)){
				MapLocation newAttackTarget = determineAttackPASTR();
				//this if is only executed when attackTarget 
				if (newAttackTarget != null && !newAttackTarget.equals(attackTargetLoc)) {
					PASTRinfoReset();
					attackTargetLoc = newAttackTarget;
					rallyPoint = attackTargetLoc;
					rallySetTime = curTime;
					lastTask = myTask;
					battleEngageTime = curTime;
					return TASK.HQATTACKPASTR;
				}
			}
		}
		
		if(numEnemies!=0 && numEnemies > friendfireAllies.length*1.5 && myTask!=TASK.HQATTACK){
			lastTask = myTask;
			return TASK.HQATTACK;
		}
		
		switch (myTask) {
		//Once engage in battle
		case HQATTACKPASTR:
			//check if we defeated the PASTR, switch back to HQSPAWN if yes
			if(enemyPastr.length == 0 || !canfind(attackTargetLoc, enemyPastr)){
				attackTargetLoc = null;
				lastTask = myTask;
				if(allyPastr.length!=0){
					rallySetTime = curTime;
					rallyPoint = allyPastr[0];
					broadcastRallyPoint(broadcastch, TASK.ASSEMBLE, curTime, rallyPoint);
				}			
				return TASK.HQSPAWN;
			}else{
			//TODO send two soldiers to build my own PASTRset without escort
				if(curTime> battleEngageTime+400){
					forcedBuildingPASTR(2, 3);
					return myTask;
				}
			}
			break;
			
		case HQMonitorPASTRConstruc:
			//If I'm building the second PASTR, switch back to squad one if 
			//k soldiers have been dispatched for this side construction
			if (allyPastrNum==1){
				//only spawn next k soldiers to construct the second PASTR
				if (needUpdateSoldier && nextSodierSquadID==2){
					secondPASTRsoldier--;
				}
				if(secondPASTRsoldier==0){
					nextSodierSquadID = 1;
				}
			}
			//Both NT and PASTR constructions are complete
			if (rc.readBroadcast(broadcastch+50)!=0 && 	rc.readBroadcast(broadcastch+51)!=0 ){
				PASTRinfoReset();
				lastTask = myTask;
				broadcastch = 2*mapMaxSize+100;
				return TASK.HQSPAWN;
			}
			break;
			
		case HQATTACK:
			//if intruding enemies has been defeated, 
			//or curRound is NOT active for shooting, switch back to last Task
			if (numEnemies == 0 || !rc.isActive()){
				TASK recordedTask = lastTask;
				lastTask = myTask;
				return recordedTask;
			}
			break;
		
		default:
			break;

		}
		return myTask;			
	}
	
	
	private void resetSoldierSquadID() throws GameActionException {
		for(int ch = mapMaxSize+1; rc.readBroadcast(ch)!=0; ch=ch+2){
			if(rc.readBroadcast(ch+1)!=1)
				rc.broadcast(ch+1, 1);
		}
		
	}

	private int senseDangeredPASTR() throws GameActionException {
		if(rc.readBroadcast(2 * mapMaxSize + 100+ 9)==1)
			return 1;
		else if (rc.readBroadcast(2 * mapMaxSize + 200+ 9)==1)
			return 2;
		else
			return 0;
	}

	private void clearBroadcastedInfo() throws GameActionException {
		for (int i=5; i<15; i++){
			rc.broadcast(broadcastch+i, 0);
		}
		rc.broadcast(broadcastch+50, 0);
		rc.broadcast(broadcastch+51, 0);
	}

	private void HQsenseNearby() {
		//sense Global Allies, due to naming issue, just adapt to name nearbyAllies
		numEnemies = 0;
		nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, HQAttackRange, enemyTeam);
		if(nearbyEnemies.length != 0){
			for (Robot r : nearbyEnemies) {
				try {
					RobotInfo info = rc.senseRobotInfo(r);
					nearbyEInfo[numEnemies] = info;
					nearbyELoc[numEnemies] = info.location;
					++numEnemies;
				} catch (Exception e) {
				}
			}
			friendfireAllies = rc.senseNearbyGameObjects(Robot.class, HQAttackRange, myTeam);
		}
	}
	
	private void readMinionReport() throws GameActionException {
		
	}	
	
	////////////////////////////////////////////////////////////////
	//TASK HQATTACKPASTR
	private void HQattackEnemyPastr() throws GameActionException {
		try2spawn();
		int lastBroadcastTime = rc.readBroadcast(broadcastch + 2);		
		//broadcast the targetPastr info if info is NOT up-to-date
		if (rallySetTime != lastBroadcastTime) {
			int recipientSquadID = determineRecipientSquadID();
			broadcastch = 2 * mapMaxSize + 100 * recipientSquadID;
//			// TODO set a rally point before the targetPos, for now just set it to be the targetPos
//			// rallyPoint = targetPastr.add(targetPastr.directionTo(myHQ), soldierSenseRange);
			broadcastRallyPoint(broadcastch, TASK.ATTACK, rallySetTime, rallyPoint);
		}		
	}
	
	//executed only if enemyPastr.length>0
	private MapLocation determineAttackPASTR() throws GameActionException {
		if(!canfind(attackTargetLoc, enemyPastr)){
			MapLocation targetPastr = enemyPastr.length==1? enemyPastr[0]:findClosestEnemyPastr();
			//TODO if enemyPASTR is too close to enemy HQ just give up attack for now
			if(targetPastr.distanceSquaredTo(enemyHQ)<HQAttackRange)
				return null;
			else{
				spawnDirReset(myHQ.directionTo(targetPastr));
				return targetPastr;
			}
		}else{
			return attackTargetLoc;
		}
			
	}

	private boolean canfind(MapLocation broadcastTarget, MapLocation[] enemyPastr) {
		if (broadcastTarget==null)
			return false;
		for (int i=0; i!=enemyPastr.length; ++i){
			if (broadcastTarget.equals(enemyPastr[i]))
				return true;
		}
		return false;
	}

	private MapLocation findClosestEnemyPastr() {
		int minDisSq = mapMaxSize, curDisSq;
		int targetIdx=0;
		for (int i=0; i!=enemyPastr.length; ++i){
			//in future, can change it to find the closet enemy
			//PASTR to a certain ally squad
			curDisSq = myHQ.distanceSquaredTo(enemyPastr[i]);
			if (curDisSq < minDisSq){
				minDisSq = curDisSq;
				targetIdx = i;
			}
		}
		return enemyPastr[targetIdx];
	}
	
	
    //////////////////////////////////////////////////////////////
	//TASK HQSPAWN, the default state for HQ
	private void HQspawn() throws GameActionException {
		try2spawn();
		HQmaintenance();
		considerBuildingPASTR();
	}
	
	private void HQmaintenance() throws GameActionException {
//		boolean needBroadcast = getRallyPoint();
//		if(needBroadcast){
//			
//		}
	}

	//consider >1 squad prioritize rallyPoint
	private boolean getRallyPoint() {

		return false;
	}

	private void try2spawn() throws GameActionException {
		if (rc.isActive()) {
			spawnSoldierPreferedDir();
			needUpdateSoldier = true;
			soldierDetectableDelay = 1;
		}
		if(needUpdateSoldier){
			soldierDetectableDelay--;
			if(soldierDetectableDelay<0){
				int newSoldierID = getnewlySpawnedSoldierID();
				// if (newSoldierID!=0){// &&rc.readBroadcast(nextSodierPostCh-1) != newSoldierID){
				rc.broadcast(++nextSodierPostCh, newSoldierID);
				rc.broadcast(++nextSodierPostCh, nextSodierSquadID);
				needUpdateSoldier = false;
//				rc.setIndicatorString(0, "broadcasting new Soldier ID "
//						+ newSoldierID + " at round " + Clock.getRoundNum());
				// }
			}
		}	
	}

	
	///////////////////////////////////////////////////////////////////
	//TASK HQMonitorPASTRConstruc:
	private void HQmonitorConstruction() throws GameActionException {
		try2spawn();
//		System.out.println("nextPASTRLoc is ("+nextPASTRLoc.x+" , "+nextPASTRLoc.y+")");
		PASTRnearbyAllies = rc.senseNearbyGameObjects(Robot.class, nextPASTRLoc, soldierSenseRange, myTeam);
		//TODO Note!!! myHQ can't sense the enemyRobots around PASTR loc, 
		//thus need the battlefront soldiers to send back boolean representing things like numAllies > 2 * numEnemies
		PASTRunderAttack = rc.readBroadcast(broadcastch + 9)==0? false:true;
		if(PASTRnearbyAllies.length >= 3 && !PASTRunderAttack){
			//TODO check if NT and PASTR candidates are still alive or constructed!!!
			if(NTpicked && rc.readBroadcast(broadcastch+50)==0){
				if(!rc.canSenseObject(NTcandidate)){
					rc.broadcast(broadcastch + 2, curTime);
					rc.broadcast(broadcastch + 5, 0);
					rc.broadcast(broadcastch + 6, 0);
					NTpicked = false;
					NTcandidate = null;
				}
			}
			if(PASTRpicked && rc.readBroadcast(broadcastch+51)==0){
				if(!rc.canSenseObject(PASTRcandidate)){
					rc.broadcast(broadcastch + 2, curTime);
					rc.broadcast(broadcastch + 7, 0);
					rc.broadcast(broadcastch + 8, 0);
					PASTRpicked = false;
					PASTRcandidate = null;
					nextPASTRLoc = null;
				}
			}
						
			if(!NTpicked ||!PASTRpicked){
				checkforConstructCandidate();
			}else{
				
				if (NTestBuildTime>0 && curTime > NTestBuildTime+buildTimeDelay){
					//send a non-zero signal for PASTR building
//					rc.setIndicatorString(2, "HQ send build PASTR t@rd "+curTime+"NTpicked"+NTpicked+"PASTRpicked"+PASTRpicked);
					rc.broadcast(broadcastch+8, curTime);
//					myTask = TASK.HQSPAWN;
//					//reset parameters for next PASTR
//					PASTRinfoReset();
				}
				
			}
		}	
	}

	private void checkforConstructCandidate() throws GameActionException {
		RobotInfo rinfo;
		if(PASTRnearbyAllies.length!=0){
		if (!NTpicked) {
			for (Robot r : PASTRnearbyAllies) {
				rinfo = rc.senseRobotInfo(r);
				if (rinfo.location.isAdjacentTo(nextPASTRLoc)) {
					rc.broadcast(broadcastch + 5, r.getID());
					NTestBuildTime = curTime;
					rc.broadcast(broadcastch + 6, curTime);
					NTpicked = true;
					NTcandidate = r;
//					rc.setIndicatorString(0, "Robot "+r.getID()+" has picked to become NT at location ("
//							+rinfo.location.x + "," + rinfo.location.y+")");
					break;
				}
			}
		}
		if (!PASTRpicked) {
			for (Robot r : PASTRnearbyAllies) {
				rinfo = rc.senseRobotInfo(r);
				if (rinfo.location.equals(nextPASTRLoc)) {
					rc.broadcast(broadcastch + 7, r.getID());
					PASTRpicked = true;
					PASTRcandidate = r;
//					rc.setIndicatorString(1, "Robot "+r.getID()+" has picked to become PASTR at location ("
//							+nextPASTRLoc.x + "," + nextPASTRLoc.y+")");
					break;
				}
			}
		}
		}
	}

	//pick up k newest born soldiers to build PASTR
	private void forcedBuildingPASTR(int sqID, int k) throws GameActionException{
//		if (allyPastr.length < 2 && (rc.readBroadcast(50000) + 100) < Clock.getRoundNum()) {
			PASTRinfoReset();
			rc.broadcast(50000, curTime);
			
			nextPASTRLoc = determinePASTRLoc();
			rallySetTime = curTime;
			rallyPoint = nextPASTRLoc;
			
			nextSodierSquadID = sqID;
			broadcastch = 2 * mapMaxSize + 100 * nextSodierSquadID;
			broadcastRallyPoint(broadcastch, TASK.BUILDPASTR, rallySetTime, rallyPoint);
			secondPASTRsoldier = k;
			//pick up the k latest born soldiers to form the new squad
			//change their squad in their getMySquadIDCh
//			newSquadFormation(squadminNum, SquadID);
//			spawnDirReset(myLoc.directionTo(nextPASTRLoc));
			lastTask = myTask;
			myTask = TASK.HQMonitorPASTRConstruc;
			
//			System.out.println("HQ at round " + curTime + " is forced to build a PASTR due to long battle");
//		}
	}
	
	
	
	// TASK decision making
	// TODO need to fine tune parameters in future
	private void considerBuildingPASTR() throws GameActionException {
		// there must be allies nearby for defense
//		if (numAllies > squadminNum) {
		if (numAllies > (allyPastrNum+1)*squadminNum) {
			// no allied robots can be building a pastr at the same time
			if (allyPastr.length < 2 && (rc.readBroadcast(50000) + 100) < Clock.getRoundNum()) {
				PASTRinfoReset();
				rc.broadcast(50000, curTime);
				
				nextPASTRLoc = determinePASTRLoc();
				int SquadID = allyPastrNum + 1;
				rallySetTime = Clock.getRoundNum();
				rallyPoint = nextPASTRLoc;
				
				nextSodierSquadID = SquadID;
				broadcastch = 2 * mapMaxSize + 100 * SquadID;
				broadcastRallyPoint(broadcastch, TASK.BUILDPASTR, rallySetTime, rallyPoint);
				if(allyPastrNum==1){
					secondPASTRsoldier = 3;
				}
				//pick up the k latest born soldiers to form the new squad
				//change their squad in their getMySquadIDCh
//				newSquadFormation(squadminNum, SquadID);
//				spawnDirReset(myLoc.directionTo(nextPASTRLoc));
				lastTask = myTask;
				myTask = TASK.HQMonitorPASTRConstruc;
				
//				System.out.println("HQ at round " + Clock.getRoundNum() + " decided to build a PASTR");
			}
		}
	}
			
	private void PASTRinfoReset() throws GameActionException {
//		DebuggingPrint.resetIndicatorString();
		NTpicked = false;
		PASTRpicked = false;
		NTestBuildTime = -1;
		PASTRunderAttack = false;
		nextPASTRLoc = null;
		NTcandidate = null;
		PASTRcandidate = null;
		rc.broadcast(broadcastch+50, 0);
		rc.broadcast(broadcastch+51, 0);
	}

	// For now, just determine the k highest ID soldiers to form the new squad
	// TODO Note that they are NOT necessarily all alive, need to give some extra space ?? 
	private void newSquadFormation(int k, int squadID) throws GameActionException {
//		System.out.println("new Squad formed to build new PASTR");
		int curSquadNum = 0;
		int checkCh = nextSodierPostCh;
		while (curSquadNum <= k) {
			rc.broadcast(checkCh, squadID);
			curSquadNum++;
			checkCh -= 2;
		}
	}
	
	private MapLocation determinePASTRLoc() throws GameActionException {
		cowGowthRate = rc.senseCowGrowth();
		MapLocation loc = maxCowGrowthLocCoarseGrain();
		return loc;
	}
	
	////////////////////////////////////////////////////////////////////////
	//TASK HQATTACK, this is executed only when more enemySoldier than my own 
	//soldiers in myHQ attackRange
	private void HQattack() throws GameActionException {
		//Note that HQ attack is a range attack
		if(rc.isActive()){
			MapLocation targetEnemyLoc = lowestHPEnemyLoc(HQAttackRange);
			//Always prioritize with shooting enemies
			if(targetEnemyLoc!=null){
				rc.attackSquare(targetEnemyLoc);
			}
		}
//		else rc.setIndicatorString(0, "I'm "+rc.roundsUntilActive()+" rounds away from active");
		HQmaintenance();
	}

		
	private int getnewlySpawnedSoldierID() {
		Robot[] recentlySpawnedSoldier = rc.senseNearbyGameObjects(Robot.class, 8, myTeam);//diagonal move
//		rc.setIndicatorString(1, "find "+recentlySpawnedSoldier.length+" nearby soldiers");
		if (recentlySpawnedSoldier.length==1){
			return recentlySpawnedSoldier[0].getID();
		}
		else{
			int maxID=0;
			for(Robot r:recentlySpawnedSoldier){
				if(r.getID()>maxID)
					maxID = r.getID();
			}
//			rc.setIndicatorString(2, "maxID find is "+maxID);
			return maxID;
		}
	}


	private int determineRecipientSquadID() {
		return 1;
//		//TODO need to be revised
//		if (numAllies<10){
//			return 1;
//		}else{			
//			return 1;
//		}
	}
	
	private void checkTwoHQtooClose() {
		Direction myHQtoEnemyHQ = myHQ.directionTo(enemyHQ);
		if (myHQtoEnemyHQ.isDiagonal()){
			int compareDim = mapWidth<=mapHeight? mapWidth:mapHeight;
			twoHQclose = myHQ.distanceSquaredTo(enemyHQ) < (compareDim * compareDim*2)/9 ? true : false;
//			System.out.println( myHQ.distanceSquaredTo(enemyHQ));
//			System.out.println((compareDim * compareDim*2)/9 );
		}else{
			int compareDim, twoHQDis;
			if (myHQtoEnemyHQ.equals(Direction.NORTH)||myHQtoEnemyHQ.equals(Direction.SOUTH)){
				compareDim = mapHeight;
				twoHQDis = Math.abs(myHQ.y-enemyHQ.y);
			}else{
				compareDim = mapWidth;
				twoHQDis = Math.abs(myHQ.x-enemyHQ.x);
			}
			twoHQclose = twoHQDis < compareDim/3 ? true : false;
//			System.out.println(twoHQDis);
//			System.out.println(compareDim/3);
		}
	}

	private void spawnDirDefautRallyPointInit() {
		Direction preferSpawnDir = myHQ.directionTo(enemyHQ);
		// if my HQ is too close to enemy HQ, choose to spawn direction in the
		// opposite direction to prevent enemy fire
		preferSpawnDir = twoHQclose? preferSpawnDir.opposite() : preferSpawnDir;
		int preferSpawnDirOrdinal = preferSpawnDir.ordinal();
		int dirOrdinal = 0;
		for (int directionalOffset : directionalLooks) {
			dirOrdinal = (preferSpawnDirOrdinal + directionalOffset + 8) % 8;
			Direction trialDir = allDirs[dirOrdinal];
			if (rc.senseTerrainTile(myHQ.add(trialDir)).ordinal() < 2) {
				//System.out.println("spawnable direction: "+trialDir);
				spawnDirs[spawnableDirNum++] = trialDir;
				dirSpawnable[dirOrdinal] = true;
			}else{
				dirSpawnable[dirOrdinal] = false;
			}
		}

		//TODO could change defaultRallyPoint in the future
		defaultRallyPoint = myHQ.add(spawnDirs[0], 2);
//		defaultRallyPoint = twoHQclose? myHQ.add(spawnDirs[0], 15/spawnableDirNum): 
//			MapOperation.mlAdd(myHQ, (MapOperation.mldivide(MapOperation.mlSubtract(enemyHQ, myHQ), 3)));
		
	}
	
	private void spawnDirReset(Direction preferDir) {
		int forwardInt = preferDir.ordinal();
		forwardInt = forwardInt<8? forwardInt: 0;
		int dirNum = 0;
		for(int directionalOffset:directionalLooks){	
			int dirOrdinal = (forwardInt+directionalOffset+8)%8;
			Direction trialDir = allDirs[dirOrdinal];
			if(dirSpawnable[trialDir.ordinal()]){
				spawnDirs[dirNum++] = trialDir;
			}
		}
	}

	private void spawnSoldierPreferedDir() throws GameActionException {
		if (rc.senseRobotCount() < MAX_NUM_Robots) {
			for(int i=0; i!=spawnableDirNum; ++i){
				if (rc.canMove(spawnDirs[i])){
					rc.spawn(spawnDirs[i]);
					break;
				}
			}
		}
	}
	
}
	
	
//}else{
//	//check if enough ally soldier has arrived at rally point
//	//if it does, change the state from assemble to attack
//	//TODO need to re-download the rally point location
//	//Now let the minions self determine whether to attack or not
//	int currentRound = Clock.getRoundNum();
//	int allyNum = 0;
//	for (int ch = broadcastch+11; rc.readBroadcast(ch)!=0; ch=ch+3){
//		if (rc.readBroadcast(ch+1) >= currentRound-1)
//			++allyNum;
//	}
//	if (allyNum>5){
//		rc.setIndicatorString(2, "Enough soldiers have assembled at rally point, switch to attack mode");
//		rc.broadcast(broadcastch+6, TASK.ATTACK.task);
//	}
//}
