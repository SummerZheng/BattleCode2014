package team165;

import battlecode.common.*;

public abstract class BasePlayer extends StatStuff {
	protected TASK myTask;
	protected TASK lastTask;
	protected static int curTime = 0;
	protected static int squadminNum;
	
	public BasePlayer(RobotController myRC) {
		myTask = null;
		lastTask = null;
		if (mapWidth*mapHeight>3600){
			squadminNum = 5;
		}else{
			squadminNum = 8;
		}
	}
	
	protected void senseNearby(int senseRange) {
		//Note, even if we can set senseRangeSq to be very big,
		//we can sense all the allies in the map, but only sense all the enemy in the sensorRange
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, senseRange);

		numAllies = 0;
		numEnemies = 0;

		for(Robot r : nearbyRobots) {
			try {
				RobotInfo info = rc.senseRobotInfo(r);
				if(info.team == myTeam) {
					nearbyAllies[numAllies] = r;
					nearbyAInfo[numAllies] = info;
					nearbyALoc[numAllies] = info.location;
					++numAllies;
				} else {
					nearbyEnemies[numEnemies] = r;
					nearbyEInfo[numEnemies] = info;
					nearbyELoc[numEnemies] = info.location;
					++numEnemies;
				}
			} catch(Exception e) {
				// sense failed, do nothing
				// this should happen infrequently
			}
		}

		// put a terminator at the end of our arrays.
		nearbyAllies[numAllies] = null;
		nearbyAInfo[numAllies] = null;
		nearbyALoc[numAllies] = null;

		nearbyEnemies[numEnemies] = null;
		nearbyEInfo[numEnemies] = null;
		nearbyELoc[numEnemies] = null;

	}
	
	protected void senseGlobalAlly() {
		// Note, even if we can set senseRangeSq to be very big,
		// we can sense all the allies in the map, but only sense all the enemy
		// in the sensorRange
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, mapMaxSize, myTeam);
		numAllies = 0;
		for (Robot r : nearbyRobots) {
			try {
				RobotInfo info = rc.senseRobotInfo(r);
				nearbyAllies[numAllies] = r;
				nearbyAInfo[numAllies] = info;
				nearbyALoc[numAllies] = info.location;
				++numAllies;
			} catch (Exception e) {
			}
		}

		// put a terminator at the end of our arrays.
		nearbyAllies[numAllies] = null;
		nearbyAInfo[numAllies] = null;
		nearbyALoc[numAllies] = null;
	}
	
	///////////////////////////////////////////////////////////
	//Messaging
	protected void broadcastRallyPoint(int broadcastch, TASK newTask, int time, MapLocation targetPos) throws GameActionException {
		rc.broadcast(broadcastch + 1, newTask.task); 
		rc.broadcast(broadcastch + 2, time); 
		rc.broadcast(broadcastch + 3, targetPos.x);
		rc.broadcast(broadcastch + 4, targetPos.y);
		//rc.broadcast(broadcastch + 5, squadNum); // squad number to change into
		//rc.setIndicatorString(0, "Rally point is set to be (" + targetPos.x	+ "," + targetPos.y + ")");
	}
	
	protected void broadcastAttackPoint(){
		
	}
	
	//////////////////////////////////////////////////////////////////////
	//ATTACK
	//process enemies within senseRange, return the enemy with lowest HP
	//within attackRange
	protected MapLocation lowestHPEnemyLoc(int attackRange) throws GameActionException {		
		double lowestHealth = maxHealth, curHealth;
		MapLocation targetLoc = null;
		for (int i=0; i!=numEnemies; ++i) {
			RobotInfo curEInfo = nearbyEInfo[i];
			if (myLoc.distanceSquaredTo(curEInfo.location) <= attackRange){
			curHealth = curEInfo.health;
			//since maxHealth < enemyHQ.HP, eliminate the useless target
			//enemy HQ, since it's invisible
			if (curHealth < lowestHealth) {
				lowestHealth = curHealth;
				targetLoc = curEInfo.location;
			}
			}
		}
//		 rc.setIndicatorString(1, "find"+nearbyEnemies.length+"nearbyEnemis");
//		 DebuggingPrint.printLocation(2,"decide to target enemy at loc ", targetLoc);
		return targetLoc;		
	}
	
	
	////////////////////////////////////////////////////////////////
	//determine PASTR Location
	protected void senseCowGrowth() {
		cowGowthRate = rc.senseCowGrowth();
		cowGrowthSenseRound = Clock.getRoundNum();
	}
	
	
	protected MapLocation findPASTRlocCloseToHQ(){
		int x = myHQ.x < enemyHQ.x ? 0 : mapWidth -1;
		int y = myHQ.y < enemyHQ.y ? 0 : mapHeight -1;
		MapLocation startLoc=new MapLocation(x, y);
		
		boolean alongx = Math.abs(myHQ.y - enemyHQ.y) > Math.abs(myHQ.x - enemyHQ.x);

        senseCowGrowth();
        return null;	
	}
	
	
	// This is to determine the pastr location on the most abundant cow location
	protected MapLocation maxCowGrowthLocCoarseGrain() {
		double curCowGrowth, maxCowGrowth = 0;
		int curDisSq, minDisSq = mapMaxSize;
		int stepSize = 4;
		MapLocation checkLoc, pastrLoc = null;
		MapLocation[] allyPastrs =rc.sensePastrLocations(myTeam);
		
		for (int y = 2; y < mapHeight-2; y += stepSize) {
			for (int x = 2; x < mapWidth-2; x += stepSize) {
				checkLoc = new MapLocation(x, y);
				// there must not be another pastr nearby
				if (allyPastrs.length == 0 || MapOperation.closestDis(checkLoc, allyPastrs) > GameConstants.PASTR_RANGE * 5) {
					curCowGrowth = cowGowthRate[x][y];
					if (curCowGrowth > maxCowGrowth) {
						// maxCowGrowth = curCowGrowth;
						minDisSq = mapMaxSize;
					}
					// may also need to go finer grain after coarse grain search to guarantee
					// abundant place around and average cow growth is high in
					// this patch && save () place???need terrain info
					if (curCowGrowth >= maxCowGrowth){
//							&& rc.senseTerrainTile(checkLoc).ordinal() < 2) {
						curDisSq = myHQ.distanceSquaredTo(checkLoc);
						//parameter needs to to adjusted!!!!!!
						if (curDisSq < minDisSq && curDisSq+20 < enemyHQ.distanceSquaredTo(checkLoc)) {
							checkLoc = fineGrainTuningPASTRPos(checkLoc);
							if (checkLoc!=null){
								maxCowGrowth = curCowGrowth;
								pastrLoc = checkLoc;
								minDisSq = curDisSq;
							}
						}
					}
				}
			}
		}
		return pastrLoc;
	}
		
	//This is to guarantee that the PASTR loc is 2 dis away from any void
	private MapLocation fineGrainTuningPASTRPos(MapLocation checkLoc) {
		if(rc.senseTerrainTile(checkLoc.add(Direction.NORTH, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.SOUTH, 2);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.NORTH, 2)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.SOUTH, 1);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.SOUTH, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.NORTH, 2);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.SOUTH, 2)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.NORTH, 1);
		}
		
		if(rc.senseTerrainTile(checkLoc.add(Direction.EAST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.WEST, 2);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.EAST, 2)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.WEST, 1);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.WEST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.EAST, 2);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.WEST, 2)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.EAST, 1);
		}
		
		if(rc.senseTerrainTile(checkLoc.add(Direction.NORTH_EAST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.SOUTH_WEST, 1);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.SOUTH_WEST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.NORTH_EAST, 1);
		}
		
		if(rc.senseTerrainTile(checkLoc.add(Direction.NORTH_WEST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.SOUTH_EAST, 1);
		}else if(rc.senseTerrainTile(checkLoc.add(Direction.SOUTH_EAST, 1)).ordinal()>1){
			checkLoc = checkLoc.add(Direction.NORTH_WEST, 1);
		}
		
		if(rc.senseTerrainTile(checkLoc).ordinal()<2){
			return checkLoc;
		}else
			return null;
	}


	protected void debug_print() {
		rc.setIndicatorString(0, myTask.toString());
	}

	protected void yield() {
		debug_print();
		rc.yield();	
	}
	
	protected void senseTerrainTile(){
		int midY = (mapHeight+1)/2;
		for (int j=0; j != midY; ++j){
			for (int i=0; i != mapWidth; ++i){
				mapTerrainTile[i][j] = rc.senseTerrainTile(new MapLocation(i, j)).ordinal();
				mapTerrainTile[mapWidth-1-i][mapHeight-1-j] = mapTerrainTile[i][j];
			}
		}
		terrainInfoComplete = true;
	}
	
	protected void sendTerrainInfoRowMajor() throws GameActionException{
		for (int j=0; j != mapHeight; ++j){
			for (int i=0; i != mapWidth; ++i){
				rc.broadcast(j*mapWidth+i, mapTerrainTile[i][j]);
			}
		}
		rc.broadcast(mapMaxSize, -9999);
	}
	
	protected void downloadTerrainInfoRowMajor() throws GameActionException{
		int ch=0;
		//int midY = (mapHeight+1)/2;
		for (int j=0; j != mapHeight; ++j){
			for (int i=0; i != mapWidth; ++i){
				mapTerrainTile[i][j] = rc.readBroadcast(ch++);
				//mapTerrainTile[mapWidth-1-i][mapHeight-1-j] = mapTerrainTile[i][j];
			}
		}
		terrainInfoComplete = true;
	}
	
	protected static void displayArray(int[][] intArray){
		for(int y = 0;y<intArray.length;y++){
			String line = "";
			for(int x=0;x<intArray[0].length;x++){
				//line+=(voidID[x][y]==-1)?"_":".";
				int i = intArray[x][y];
				if(i== 1){//a path
					line+="-";
				}else if (i==0){//open terrain
					line+=".";
				}else if (i==2){//a void
					line+="X";
				}else{
					line+=i;
				}
			}
			System.out.println(line);
		}
	}
	
	
	

	protected boolean needsToMove(){
		//Just to find if we are adjacent to a nearby ally robot, radiusSquared=3
		//change the name here to distinguish from nearbyAllies etc in the StatStuff
		Robot[] myAdjacentAllies = rc.senseNearbyGameObjects(Robot.class, myLoc, 3, myTeam);
		int myID = rc.getRobot().getID();
		if(myAdjacentAllies.length>0){
			for(Robot thisRobot:myAdjacentAllies){
				if (myID > thisRobot.getID())
					return true;
			}
		}
		return false;
	}
}
