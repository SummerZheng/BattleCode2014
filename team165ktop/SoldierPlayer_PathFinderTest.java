package team165;

import java.util.Random;

import team165.BasePlayer;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class SoldierPlayer_PathFinderTest extends BasePlayer {
	private int mySoldierID;
	private static int mySquadID = 1;
	private static int mySquadCh = mapMaxSize;
	
	protected static SoldierPathFinder bug;
	protected static boolean needsMoveFlag=false;
	
	private static MapLocation attackTargetLoc = null;
	private static MapLocation rallyPoint = null;
	private static MapLocation targetLoc = null;
	private static TASK myTask = TASK.ASSEMBLE;
	private static int myState = STATE.MOVE.state;
	private static int targetSetTime = -1;
	private static int rallySetTime = -1;
	
	private MapLocation targetEnemyLoc = null;
	
	private static final int soldierAttackRange = rc.getType().attackRadiusMaxSquared;
	private static final int soldierSenseRange = rc.getType().sensorRadiusSquared;

	private static double cowThreshold = 1000;
	static Random myRand = new Random();

	public SoldierPlayer_PathFinderTest(RobotController rc) {
		super(rc);
		bug  = new SoldierPathFinder();
		mySoldierID = rc.getRobot().getID();
	}

	public void run(){
//		rc.setIndicatorString(0, "Current active robot is " + mySoldierID);
//		System.out.println("target loc is ("+target.x+" , "+target.y+")");

		while (true) {
			try {
				myLoc = rc.getLocation();
				switch (myTask) {
				case ASSEMBLE:
					assemble();
					break;

				case ATTACK:
					performAttack();
					break;
				default:
					break;
				}
				
//				if(mySoldierID == 105){
//					int time = Clock.getRoundNum();
//					if(time==1288 ||time==1290 ||time==1340||time==1345 ||time==1392)//||time==1185||time==1186||time==1207)
//					bug.printPath();
//				}
				
				yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	private void performAttack() {
		// TODO Auto-generated method stub
		
	}

	private void assemble() throws GameActionException {
		updateAssemblePoint(mySquadCh);
		if(rc.isActive()){
			moveTo(rallyPoint);
		}
	}

	private void moveTo(MapLocation targetLoc) throws GameActionException{
		if(myLoc.distanceSquaredTo(targetLoc)<=10){
			myState = STATE.STAY.state;
			//also Update the sensed enemies, so that whenever the enemy emergency 
			//is alleviated, HQ can determine to swithc the state from Assemble to Attack
			//broadcastMyLoc();
			bug.moveRandom(false);		
		}else if(myState == STATE.STAY.state){
//			//System.out.println("robot" + rc.getRobot().getID() + "first move in and then outside rally point");
			//broadcastMyLoc();
			bug.movePreferDir(myLoc.directionTo(targetLoc), false);
		}else{
			myState = STATE.MOVE.state;	
//			MapLocation loc = bug.getDestination();
//			System.out.println("bug destination is ("+loc.x+","+loc.y+")");
			needsMoveFlag = needsToMove();
			bug.moveToTarget(needsMoveFlag, false);					
		}
		
	}

	private void updateAssemblePoint(int channel) throws GameActionException {
		int rallyTime = rc.readBroadcast(channel + 2);
		if (rallySetTime != rallyTime) {
			// reset new destination
			myState = STATE.MOVE.state;
			rallySetTime = rallyTime;
			rallyPoint = new MapLocation(rc.readBroadcast(channel + 3), rc.readBroadcast(channel + 4));
			mySquadID = rc.readBroadcast(channel + 5);
			//TODO  reset mySquad Channel
			mySquadCh = mapMaxSize + (mySquadID-1)*100;
			bug.setPath(rallyPoint);
			DebuggingPrint.printLocation(0, "initialize assemble destination ", rallyPoint);
		}
	}

	private MapLocation ProcessEnemyInfoAndBroadcast() throws GameActionException {
		DebuggingPrint.printLocation(0, "An more urgent enemy squad in sight, reset rally point to be myLoc ", rallyPoint);
		double lowestHealth = maxHealth, curHealth;
		RobotInfo curRobotInfo = null;
		MapLocation targetLoc = null, curLoc;
		MapLocation[] nearbyEnemyLoc = new MapLocation[nearbyEnemies.length];
		for (int i=0; i!=nearbyEnemies.length; ++i) {
			curRobotInfo = rc.senseRobotInfo(nearbyEnemies[i]);
			curLoc = curRobotInfo.location;
			nearbyEnemyLoc[i] = curLoc;
			if (myLoc.distanceSquaredTo(curLoc) <= soldierAttackRange){
			curHealth = curRobotInfo.health;
			if (curHealth < lowestHealth) {
				lowestHealth = curHealth;
				targetLoc = curLoc;
			}
			}
		}
		rallyPoint = MapOperation.meanLocation(nearbyEnemyLoc);
		bug.setPath(rallyPoint);
		myState = STATE.MOVE.state;	
		rc.broadcast(mySquadCh + 6, TASK.ASSEMBLE.task);
		rc.broadcast(mySquadCh + 7, Clock.getRoundNum());
		//TODO could retract the rally point towards my HQ a little bit
		rc.broadcast(mySquadCh + 8, rallyPoint.x);
		rc.broadcast(mySquadCh + 9, rallyPoint.y);
		//TODO could update the enemy number in ch+10
		rc.broadcast(mySquadCh + 10, nearbyEnemies.length);
		
		return targetLoc;
	}

	//process enemies within senseRange, return the enemy with lowest HP
	//within attackRange
	private MapLocation ProcessEnemyInfo() throws GameActionException {		
		double lowestHealth = maxHealth, curHealth;
		RobotInfo curRobotInfo = null;
		MapLocation targetLoc = null;
		for (Robot nbEnemy : nearbyEnemies) {
			curRobotInfo = rc.senseRobotInfo(nbEnemy);
			if (myLoc.distanceSquaredTo(curRobotInfo.location) <= soldierAttackRange){
			curHealth = curRobotInfo.health;
			//since maxHealth < enemyHQ.HP, eliminate the useless target
			//enemy HQ, since it's invisible
			if (curHealth < lowestHealth) {
				lowestHealth = curHealth;
				targetLoc = curRobotInfo.location;
			}
			}
		}
		// rc.setIndicatorString(0, "find"+nearbyEnemies.length+"nearbyEnemis");
		// rc.setIndicatorString(1,"decide to target enemy "+targetEnemy.getID());
		return targetLoc;		
	}

	private void broadcastMyLoc() throws GameActionException {
		int allyRobotID, allyRobotTime, allyRobotLoc;
		boolean inSquad = false;
		//TODO later need to exploit the ally report time to replenish the killed soldier
		//allyRobotTime = rc.readBroadcast(ch+1);
		int ch;
		for (ch = mySquadCh+11; rc.readBroadcast(ch)!=0; ch=ch+3){
			allyRobotID = rc.readBroadcast(ch);
			if (allyRobotID == mySoldierID){
				//update my report time and location
				rc.broadcast(ch+1, Clock.getRoundNum());
				rc.broadcast(ch+2, 100*myLoc.x+myLoc.y);
				rc.broadcast(mySquadCh+10, nearbyEnemies.length);
				inSquad = true;
				break;
			}	
		}
		if(!inSquad){
			rc.broadcast(ch, mySoldierID);
			rc.broadcast(ch+1, Clock.getRoundNum());
			rc.broadcast(ch+2, 100*myLoc.x+myLoc.y);
			rc.broadcast(mySquadCh+10, nearbyEnemies.length);
		}
		rc.setIndicatorString(2, "robot close to rally point, send its info to squad channel "+ch);
	    //"Robot" + mySoldierID+ " at round "+Clock.getRoundNum()+" reported its location to squad channel");
	}



	private void shootLowestHealthEnemy(Robot[] nearbyEnemies) throws GameActionException {
		double lowestHealth = maxHealth, curHealth;
		Robot targetEnemy = null;
		for (Robot nbEnemy : nearbyEnemies) {
			curHealth = rc.senseRobotInfo(nbEnemy).health;
			if (curHealth < lowestHealth) {
				lowestHealth = curHealth;
				targetEnemy = nbEnemy;
			}
		}
		// rc.setIndicatorString(0, "find"+nearbyEnemies.length+"nearbyEnemis");
		// rc.setIndicatorString(1,
		// "decide to target enemy "+targetEnemy.getID());
		attackEnemy(targetEnemy);
	}

	private boolean isValideTarget(Robot[] nearbyEnemies) throws GameActionException {
		if (nearbyEnemies.length == 1)
			if (rc.senseRobotInfo(nearbyEnemies[0]).type == RobotType.HQ)
				return false;
		return true;
	}

	private boolean isGoodPastrLoc(MapLocation myLoc) throws GameActionException {
		MapLocation[] allyPASTRs = rc.sensePastrLocations(myTeam);
		// rc.setIndicatorString(0,
		// "find "+allyPASTRs.length+" PASTRs on my team");
		// rc.setIndicatorString(1,
		// "decide to target enemy "+targetEnemy.getID());
		if (allyPASTRs.length > 5)
			return false;
		// rc.setIndicatorString(1,
		// "my documented location is ("+myLoc.x+" , "+myLoc.y+")");
		// rc.setIndicatorString(2,
		// "my current location is ("+rc.getLocation().x+" , "+rc.getLocation().y+")");
		if (rc.senseCowsAtLocation(myLoc) < cowThreshold)
			return false;
		if (allyPASTRs.length != 0
				&& MapOperation.closestDis(myLoc, allyPASTRs) < 100)
			return false;
		return true;
	}
	
	private void convert2pastr() throws GameActionException {
		if (rc.isActive()) {
			rc.construct(RobotType.PASTR);
			numAllies += (pastrWeight - soldierWeight);
		}
	}

	private static void attackEnemy(Robot targetEnemy) throws GameActionException {
		if (rc.isActive())
			rc.attackSquare(rc.senseRobotInfo(targetEnemy).location);
	}
	
	protected void debug_print() {
		bug.bug_debug_print(1);
	}
	
//	private boolean disperseAroundHQPreferedDir(Direction dir) throws Exception {
//	boolean needsMove = false;
//    if (myLoc.distanceSquaredTo(myHQ)<35)
//    	needsMove =true;
//    if (needsMove) {
//		bug.movePreferDir(dir);
//	}
//    return needsMove;
//}
//
//private boolean disperseAroundHQ() throws Exception {
//	rc.setIndicatorString(1, "after move, bytecode used is "+Clock.getBytecodeNum());
//	boolean needsMove = false;
//    if (myLoc.distanceSquaredTo(myHQ)<35)
//    	needsMove =true;
//    if (needsMove) {
//		bug.movePreferDir(myHQ.directionTo(myLoc));
//	}
//    return needsMove;
//    //rc.setIndicatorString(2, "after move, bytecode used is "+Clock.getBytecodeNum());
//}
}
