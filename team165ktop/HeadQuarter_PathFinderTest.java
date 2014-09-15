package team165;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants ;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class HeadQuarter_PathFinderTest extends BasePlayer {
	private static boolean[] dirSpawnable = new boolean[8];
	private static Direction[] spawnDirs = new Direction[8];
	private static int spawnableDirNum = 0;

	public static int[] directionalLooks =  new int[]{0,1,-1,2,-2,3,-3,4};
	private static int broadcastch = mapMaxSize;
	private static MapLocation rallyPoint = myHQ;
	private static MapLocation defaultRallyPoint;
	private MapLocation[] enemyPastr;
	private static MapLocation attackTargetLoc = myHQ;
	private static int squadNum = 1;
	
	public HeadQuarter_PathFinderTest(RobotController myRC){
		super(myRC);
		spawnDirDefautRallyPointInit();	
	}



	public void run() {
			while (true) {
				try{
					if (myTeam == Team.A){
						if(Clock.getRoundNum()<1000 && rallyPoint != defaultRallyPoint){
							broadcastRallyPoint(broadcastch, TASK.ASSEMBLE.task, Clock.getRoundNum(), defaultRallyPoint, squadNum);
						}else if(Clock.getRoundNum()>=1000 && rallyPoint != enemyHQ){
							broadcastRallyPoint(broadcastch, TASK.ASSEMBLE.task, Clock.getRoundNum(), enemyHQ, squadNum);
						}
						if (rc.isActive()){//&&rc.senseRobotCount()<1){
						spawnSoldierPreferedDir();
						}	
					}
					yield();
				}catch (Exception e) {
					e.printStackTrace();
				}
			// System.out.println("task assemble is "+ TASK.ASSEMBLE.task);
			// System.out.println("byte code penalty is "+GameConstants.BYTECODE_PENALTY);
			// System.out.println("map size is ("+ mapWidth + ","+mapHeight+")");
			// System.out.println("enemy base location cal is ("+ enemyHQ.x+","+enemyHQ.y+")");
			}
	}
		//senseCowGrowth();
/*
		broadcastRallyPoint(broadcastch, TASK.ASSEMBLE.task, Clock.getRoundNum(), defaultRallyPoint, squadNum);
		
//		MapLocation pastrLoc = null;
		while (true) {
			try {
				if (rc.isActive()){
					spawnSoldierPreferedDir();
				}				
				enemyPastr = rc.sensePastrLocations(enemyTeam);
				if(enemyPastr.length>0){
					if(!canfind(attackTargetLoc, enemyPastr)){
						MapLocation targetPastr = findClosestEnemyPastr();
						spawnDirReset(myHQ.directionTo(targetPastr));
						broadcastAttackPoint(broadcastch, TASK.ATTACK.task, Clock.getRoundNum(), targetPastr, squadNum);
						//broadcastRallyPoint(broadcastch, 1, Clock.getRoundNum(), targetPastr, squadNum);
					}else{
						//check if enough ally soldier has arrived at rally point
						//if it does, change the state from assemble to attack
						//TODO need to re-download the rally point location
						//Now let the minions self determine whether to attack or not
						int currentRound = Clock.getRoundNum();
						int allyNum = 0;
						for (int ch = broadcastch+11; rc.readBroadcast(ch)!=0; ch=ch+3){
							if (rc.readBroadcast(ch+1) >= currentRound-1)
								++allyNum;
						}
						if (allyNum>5){
							rc.setIndicatorString(2, "Enough soldiers have assembled at rally point, switch to attack mode");
							rc.broadcast(broadcastch+6, TASK.ATTACK.task);
						}
					}
				}else if (!rallyPoint.equals(defaultRallyPoint)){
					//regroup the troop to default Rally Point
					//rallyPoint = new MapLocation(rc.readBroadcast(broadcastch+3),rc.readBroadcast(broadcastch+4));
				    spawnDirReset(myHQ.directionTo(defaultRallyPoint));
				    broadcastRallyPoint(broadcastch, TASK.ASSEMBLE.task , Clock.getRoundNum(),  defaultRallyPoint, squadNum);
				    
				}
				
//				else if (!terrainInfoComplete){
//					 if( rc.readBroadcast(mapMaxSize) == -9999){
//						int t1 = Clock.getRoundNum();
//						downloadTerrainInfoRowMajor();
//						int t2 = Clock.getRoundNum();
//						System.out.println("it take "+(t2-t1)+" rounds for the HQ to download terrain info");
//						//displayArray(mapTerrainTile);
//					}
//				}else if(terrainInfoComplete && pastrLoc == null){
//					int b1 = Clock.getBytecodeNum();
//					pastrLoc = coarsePastrLocDetermination();
//					int b2 = Clock.getBytecodeNum();
//					System.out.println("Best Pastr Location determined is ("+ pastrLoc.x+"," + pastrLoc.y+")");
//					System.out.println("it takes "+(b2-b1)+ " bytecode to compute pastr location");
//				}
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
			}

	}
*/
	private void broadcastAttackPoint(int broadcastch, int task, int roundNum, MapLocation targetPos, int squadNum) {
		attackTargetLoc = targetPos;
		//TODO set a rally point before the targetPos, for now just set it to be the targetPos
		rallyPoint = MapOperation.mlAdd(defaultRallyPoint, MapOperation.mldivide
				(MapOperation.mlmultiply(MapOperation.mlSubtract(attackTargetLoc, defaultRallyPoint),2), 3));
		try {
			rc.broadcast(broadcastch + 1, task);//task attack(200)	
			rc.broadcast(broadcastch + 2, Clock.getRoundNum()); //time, in order to keep update
			rc.broadcast(broadcastch + 3, attackTargetLoc.x);
			rc.broadcast(broadcastch + 4, attackTargetLoc.y);
			rc.broadcast(broadcastch + 5, 1); // current squad number
			rc.setIndicatorString(0, "Attack target position ("+ attackTargetLoc.x+","+attackTargetLoc.y+")");	
			
			rc.broadcast(broadcastch + 6, TASK.ASSEMBLE.task);//current state, task assemble at rally point (500)	
			rc.broadcast(broadcastch + 7, Clock.getRoundNum()); //time, in order to keep update
			rc.broadcast(broadcastch + 8, rallyPoint.x);
			rc.broadcast(broadcastch + 9, rallyPoint.y);
			rc.broadcast(broadcastch + 10, 0); // current ally soldier around rally point
			rc.setIndicatorString(1, "Rally point is set to be ("+ rallyPoint.x+","+rallyPoint.y+")");	
			
		} catch (GameActionException e) {
			e.printStackTrace();
		} 
	}



	private void broadcastRallyPoint(int broadcastch, int task, int roundNum, MapLocation targetPos, int squadNum) {
		rallyPoint = targetPos;
		try {
			rc.broadcast(broadcastch + 1, task);//task assemble at rally point (500)	
			rc.broadcast(broadcastch + 2, Clock.getRoundNum()); //time, in order to keep update
			rc.broadcast(broadcastch + 3, rallyPoint.x);
			rc.broadcast(broadcastch + 4, rallyPoint.y);
			rc.broadcast(broadcastch + 5, 1); // current squad number
			rc.setIndicatorString(0, "Rally point is set to be ("+ rallyPoint.x+","+rallyPoint.y+")");		 
//			rc.setIndicatorString(1, "broadcast time is "+ rc.readBroadcast(mapMaxSize+2));
		} catch (GameActionException e) {
			e.printStackTrace();
		} 
		
	}


	private void spawnDirDefautRallyPointInit() {
		Direction preferSpawnDir = myHQ.directionTo(enemyHQ);
		//System.out.println("direction of my HQ to enemyHQ: "+preferSpawnDir);
		
		boolean disperseFlag = myHQ.distanceSquaredTo(enemyHQ) < (mapWidth* mapWidth*2)/9 ;	
		// if my HQ is too close to enemy HQ, choose to spawn direction in the
		// opposite direction to prevent enemy fire
		preferSpawnDir = disperseFlag? preferSpawnDir.opposite() : preferSpawnDir;
		//TODO could change defaultRallyPoint in the future
		//defaultRallyPoint = MapOperation.mlAdd(myHQ, (MapOperation.mldivide(MapOperation.mlSubtract(enemyHQ, myHQ), 3)));
		//defaultRallyPoint = myHQ.add(spawnDirs[0], 10);
		//defaultRallyPoint = disperseFlag? myHQ:  new MapLocation(mapWidth/2, mapHeight/2);
		defaultRallyPoint = disperseFlag? myHQ:  MapOperation.mlAdd(myHQ, (MapOperation.mldivide(MapOperation.mlSubtract(enemyHQ, myHQ), 3)));
		
		//System.out.println("Initialization prefered spawn direction: "+preferSpawnDir);
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

	private boolean canfind(MapLocation broadcastTarget, MapLocation[] enemyPastr) {
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

	private void spawnSoldierPreferedDir() throws GameActionException {
		if (rc.senseRobotCount() < MAX_NUM_Robots) {
			for(int i=0; i!=spawnableDirNum; ++i){
				if (rc.canMove(spawnDirs[i])){
					rc.spawn(spawnDirs[i]);
					//numAllies += soldierWeight;
					break;
				}
			}
		}
	}
	
	//This is to determine the pastr location on the most abundant cow ori
	//based on initial cow growth rate sensed
	protected MapLocation coarsePastrLocDetermination() {
		double curCowGrowth, maxCowGrowth = 0;
		int curDisSq, minDisSq = mapMaxSize;
		int stepSize = 4;
		MapLocation myLoc, pastrLoc=null;
		for (int y=0; y<mapHeight; y+=stepSize){
			for(int x=0; x<mapWidth; x+=stepSize){
				curCowGrowth = cowGowthRate[x][y];
				if (curCowGrowth > maxCowGrowth){
					//maxCowGrowth = curCowGrowth;
					minDisSq = mapMaxSize;
				}
				if (curCowGrowth >= maxCowGrowth && mapTerrainTile[x][y]<2){
					myLoc = new MapLocation(x, y);
					curDisSq = myHQ.distanceSquaredTo(myLoc);
					if (curDisSq < minDisSq && curDisSq < enemyHQ.distanceSquaredTo(myLoc)){
						maxCowGrowth = curCowGrowth;
						pastrLoc = myLoc;
						minDisSq = curDisSq;
					}
				}
			}		
		}
		return pastrLoc;
	}
	
	
}
