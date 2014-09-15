package team165;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class NoiseTower extends BasePlayer {
	private static int NTsenseRange = RobotType.NOISETOWER.sensorRadiusSquared;
	private static MapLocation defaultECOM;
	
	private static int NTattackRange = RobotType.NOISETOWER.attackRadiusMaxSquared;
	private static int attackCardinalMax = (int) Math.sqrt(NTattackRange);
	private static int attackDiagonalMax = (int) Math.sqrt(NTattackRange/2);
	
	private static int NTScareRangeLargeSq = GameConstants.NOISE_SCARE_RANGE_LARGE; 
	private static int scareLargeDis = (int) Math.sqrt(NTScareRangeLargeSq);
	private static int diagonalScareLargeDis = (int) Math.sqrt(NTScareRangeLargeSq/2);	
	
	private static int NTScareRangeSmallSq = GameConstants.NOISE_SCARE_RANGE_SMALL; 
	private static int scareSmallDis = (int) Math.sqrt( NTScareRangeSmallSq);
	private static int diagonalScareSmallDis = (int) Math.sqrt( NTScareRangeSmallSq/2);

	private static int dirIdx = 0;
	private static Direction attackDir = allDirs[dirIdx];
	private static int attackDis = attackCardinalMax;
	private static int attackDisSq2PASTR;
	private static int attackDisSq2NT;
	private static MapLocation attackLoc = null;
	private static MapLocation PASTRLoc =null;
	private static int cardinalMax;
	private static int cardinalMin;
	private static int diagonalMax;
	private static int diagonalMin;
	
	private static boolean attackLight = false;
	private static boolean enoughCowHerd = false;
	
	private static int mySquadID = 1;
	private static int mySquadCh = 2 * mapMaxSize + 100 * mySquadID;
	
	
	public NoiseTower(RobotController myRC) throws GameActionException {
		super(myRC);
		myLoc = rc.getLocation();
		myTask = TASK.NTherdCow;
		lastHP = 100;
		attackTypeBasedonTerrain();
	}

	private void attackTypeBasedonTerrain() {
		// TODO Auto-generated method stub
		// if PASTR site not blocked by wall
		attackLight = false;
		cardinalMax = attackCardinalMax;
		cardinalMin = scareLargeDis;
		diagonalMax = attackDiagonalMax;
		diagonalMin = diagonalScareLargeDis;
	}


	public void run() {
		while (true) {
			try{
				if(PASTRLoc == null){
					MapLocation[] PASTRs = rc.sensePastrLocations(myTeam);
					if (PASTRs.length!=0){
						for(MapLocation p:PASTRs){
							if(p.isAdjacentTo(myLoc)){
								PASTRLoc = p;
								mySquadID = PASTRs.length;
								mySquadCh = 2 * mapMaxSize + 100 * mySquadID;
							}
						}
					}else{
						//PASTRs.length==0, must still under construction
						//otherwise, if it's been destructed, also destruct the NT
						
					}
				}
				MapLocation[] PASTRs = rc.sensePastrLocations(myTeam);
				mySquadID = PASTRs.length;
				mySquadCh = 2 * mapMaxSize + 100 * mySquadID;
				
				if(rc.isActive()){
					if(!enoughCowHerd){
						if(PASTRLoc == null){
							NTattack();
						}else if(PASTRLoc!= null && rc.senseObjectAtLocation(PASTRLoc)!=null){
							NTattackAroundPASTR();
						}else{
							NTselfDestruct();
						}
					}
				}
				senseNearby(NTsenseRange);
				if(numEnemies!=0 && rc.readBroadcast(mySquadCh+9)==0){
					MapLocation mySensedECOM = mySensedEnemyCOM();
					broadcastSensedEnemyInfo(mySensedECOM, numEnemies, curTime);
				}
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void NTselfDestruct() throws GameActionException {
		//need to test is this create COW damage!!!
		//also test if this need isActive() to perform self Destruction
		rc.selfDestruct();		
	}


	private void NTattack() throws GameActionException {
		attackDir = allDirs[dirIdx];
		attackLoc = myLoc.add(attackDir, attackDis);
		if(!attackLight){
			rc.attackSquare(attackLoc);
		}else{
			rc.attackSquareLight(attackLoc);
		}
		attackDis--;
		if (attackDis < diagonalMin && attackDir.isDiagonal()){
			dirIdx++;
			dirIdx = dirIdx>7?0:dirIdx;	
			attackDir = allDirs[dirIdx];
			attackDis = cardinalMax;
		}
		
		if (attackDis < cardinalMin && !attackDir.isDiagonal()){
			dirIdx++;
			dirIdx = dirIdx>7?0:dirIdx;	
			attackDir = allDirs[dirIdx];
			attackDis = diagonalMax;
		}
		
//		DebuggingPrint.printLocation(0, "attack position is ", attackLoc);
	}
	
	private void NTattackAroundPASTR() throws GameActionException {
		attackDir = allDirs[dirIdx];
		attackLoc = PASTRLoc.add(attackDir, attackDis);	
		
		attackDisSq2PASTR = attackLoc.distanceSquaredTo(PASTRLoc);
		attackDisSq2NT = attackLoc.distanceSquaredTo(myLoc);
		if (attackDisSq2PASTR < NTScareRangeLargeSq){
			if(attackDir.isDiagonal()){
				dirIdx++;
				dirIdx = dirIdx > 7 ? 0 : dirIdx;
				attackDir = allDirs[dirIdx];
				attackDis = cardinalMax;
				attackLoc = PASTRLoc.add(attackDir, attackDis);
				attackDisSq2NT = attackLoc.distanceSquaredTo(myLoc);
			}else{
				dirIdx++;
				dirIdx = dirIdx>7?0:dirIdx;	
				attackDir = allDirs[dirIdx];
				attackDis = diagonalMax;
				attackLoc = PASTRLoc.add(attackDir, attackDis);
				attackDisSq2NT = attackLoc.distanceSquaredTo(myLoc);
			}
		}

		while(attackDisSq2NT > NTattackRange ){
			attackDis--;
			attackLoc = PASTRLoc.add(attackDir, attackDis);
			attackDisSq2NT = attackLoc.distanceSquaredTo(myLoc);
		}
		
		if(!attackLight){
			rc.attackSquare(attackLoc);
		}else{
			rc.attackSquareLight(attackLoc);
		}
		
		attackDis--;

		DebuggingPrint.printLocation(0, "attack position is ", attackLoc);
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
			}
		}else{
			//guaranteed (rc.readBroadcast(ch+3)<num){
			rc.broadcast(ch+1, enemyloc.x);
			rc.broadcast(ch+2, enemyloc.y);
			rc.broadcast(ch+3,  enemyNum);
		}
	}
	
}
