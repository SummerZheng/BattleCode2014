package team165;

import battlecode.common.*;

public abstract class StatStuff {
	public static final double maxHealth = 1000.0; //to distinguish between HQ and other RobotType
	
	public static final int mapMaxSize = 10000;
	
	public static Direction[] allDirs = { Direction.NORTH, Direction.NORTH_EAST,
		Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
		Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
		
	protected static RobotController rc;
	
	protected static Team myTeam;
	protected static Team enemyTeam;

	protected static MapLocation myHQ;
	protected static MapLocation enemyHQ;
	
	protected static int mapHeight;
	protected static int mapWidth;
	protected static int[][] mapTerrainTile; // = new int[mapWidth][mapHeight];
	protected static boolean terrainInfoComplete = false;	
	
	protected static MapLocation myLoc;
	protected static RobotType myType;
	
	public static int hqWeight = RobotType.HQ.count;
	public static final int soldierWeight = RobotType.SOLDIER.count;
	public static final int pastrWeight = RobotType.PASTR.count;
	protected static final int MAX_NUM_Robots = GameConstants.MAX_ROBOTS;
	protected static final int soldierAttackRange = RobotType.SOLDIER.attackRadiusMaxSquared;
	protected static final int soldierSenseRange = RobotType.SOLDIER.sensorRadiusSquared;
	protected static final int HQAttackRange = RobotType.HQ.attackRadiusMaxSquared;
	protected static final int HQSenseRange = RobotType.HQ.sensorRadiusSquared;
	protected static final int moveScareRange = GameConstants.MOVEMENT_SCARE_RANGE;  // 9
	
	protected static int numAllies = 0;//non-weighted
	protected static int numEnemies = 0;//non-weighted
	protected static Robot[] nearbyAllies;
	protected static Robot[] nearbyEnemies;
	protected static RobotInfo[] nearbyAInfo;
	protected static RobotInfo[] nearbyEInfo;
	protected static MapLocation[] nearbyALoc;
	protected static MapLocation[] nearbyELoc;
	protected static Robot[] allAllies;
	protected static RobotInfo[] allAInfo;
	protected static MapLocation[] allALoc;
	
    protected static boolean isAttacked;
	protected static double lastHP;
	
	protected static double[][] cowGowthRate;
	protected static int cowGrowthSenseRound;
	private static double cowThreshold = 1000;
	
	public static void init(RobotController myRC) {
		rc = myRC;
		myTeam = rc.getTeam();
		enemyTeam = rc.getTeam().opponent();
	    
		mapWidth = rc.getMapWidth();
	    mapHeight = rc.getMapHeight();  
	    
	    myHQ = rc.senseHQLocation();
	    enemyHQ = rc.senseEnemyHQLocation();
	    
	    myType = rc.getType();
		numAllies = rc.senseRobotCount();		
		
		mapTerrainTile = new int[mapWidth][mapHeight];
		
		int arraylength = MAX_NUM_Robots+1; //reserve last one for null
		nearbyAllies = new Robot[arraylength];
		nearbyEnemies = new Robot[arraylength];
		
		nearbyAInfo = new RobotInfo[arraylength];
		nearbyEInfo = new RobotInfo[arraylength];		
		
		nearbyALoc =  new MapLocation[arraylength] ;
		nearbyELoc =  new MapLocation[arraylength] ;
	}

}
