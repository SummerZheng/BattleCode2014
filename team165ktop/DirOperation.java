package team165;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class DirOperation {
	public static Direction getOptDir(MapLocation startOff, MapLocation destination) {
		if (startOff.equals(destination))
			return Direction.OMNI;
		if (startOff.x == destination.x) {
			if (destination.y > startOff.y)
				return Direction.SOUTH;
			else
				return Direction.NORTH;
		}
		double tan1 = 0.4142, tan2 = 2.4142;
		int delta_y = destination.y - startOff.y;
		int delta_x = destination.x - startOff.x;
		double tanAngle = (double) delta_y / (double) delta_x;
		//System.out.println(""+tanAngle);
		if (tanAngle > tan1 && tanAngle <= tan2) {
			return delta_x > 0 ? Direction.SOUTH_EAST : Direction.NORTH_WEST;
		} else if (tanAngle < -tan1 && tanAngle >= -tan2) {
			return delta_x > 0 ? Direction.NORTH_EAST : Direction.SOUTH_WEST;
		} if (Math.abs(tanAngle)<tan1) {//can re-write to further reduce bytecode
			return delta_x > 0 ? Direction.EAST : Direction.WEST;
		} else {
			return delta_y > 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}

	public static int dirInnerProduct(Direction dir1, Direction dir2) {
		MapLocation unitVec1 = dir2Vec(dir1);
		MapLocation unitVec2 = dir2Vec(dir2);
		return innerProduct(unitVec1, unitVec2);
	}

	public static int innerProduct(MapLocation dirVec1, MapLocation dirVec2) {
		return dirVec1.x*dirVec2.x+dirVec1.y*dirVec2.y;
	}

	// ordinal direction only!!!
	public static MapLocation dir2Vec(Direction dir) {
		switch(dir){
		case NORTH:
			return new MapLocation(0, -1);
		case SOUTH:
			return new MapLocation(0, 1);
		case EAST:
			return new MapLocation(1, 0);
		case WEST:
			return new MapLocation(-1, 0);
		case NORTH_EAST:
			return new MapLocation(1, -1);
		case NORTH_WEST:
			return new MapLocation(-1, -1);
		case SOUTH_EAST:
		    return new MapLocation(1, 1);
		case SOUTH_WEST:
			return new MapLocation(-1, 1);
		default:
			return new MapLocation(0, 0);
		}
	}
}
