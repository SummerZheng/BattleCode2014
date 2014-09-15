package team165;

import battlecode.common.MapLocation;

public class MapOperation {
	public static final int maxInt = 1000000; 

	public static MapLocation MapLocation(MapLocation oldLoc) {
		return new MapLocation(oldLoc.x, oldLoc.y);
	}
	
	public static boolean isEqual(MapLocation A, MapLocation B) {
		return (A.x == B.x && A.y==B.y)?true:false;
	}
	
	public static MapLocation mlAdd(MapLocation m1, MapLocation m2) {
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	public static MapLocation mlSubtract(MapLocation m1, MapLocation m2) {
		return new MapLocation(m1.x-m2.x,m1.y-m2.y);
	}
	
	public static MapLocation mldivide(MapLocation m, int d) {
		return new MapLocation(m.x/d, m.y/d);
	}
	
	public static MapLocation mlmultiply(MapLocation m, int d) {
		return new MapLocation(m.x*d, m.y*d);
	}
	
	public static int distanceSq(MapLocation locA, MapLocation locB) {
		return (locA.x-locB.x)*(locA.x-locB.x)+(locA.y-locB.y)*(locA.y-locB.y);
	}
	
	public static int closestDis(MapLocation loc,MapLocation[] nearbyLocs){
		int minDistanceSq = maxInt, curDisSq;
		for(MapLocation targetLoc:nearbyLocs){
			if(targetLoc.equals(null)){
				break;
			}else{curDisSq = loc.distanceSquaredTo(targetLoc);
				if (curDisSq < minDistanceSq){
					minDistanceSq=curDisSq;
				}	
			}
		}
		return minDistanceSq;
	}
	
	public static MapLocation meanLocation(MapLocation[] manyLocs){
		if(manyLocs.length==0)
			return null;
		MapLocation runningTotal = new MapLocation(0,0);
		int ix;
		for(ix=0; ix!=manyLocs.length && manyLocs[ix]!=null; ++ix){
			runningTotal = mlAdd(runningTotal,manyLocs[ix]);
		}
		return mldivide(runningTotal,ix);
	}
}

