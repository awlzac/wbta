package com.bulsy.wbtempest;

/**
 * Represents a column of the game board.  Convenience class.
 * @author ugliest
 *
 */
public class Column {
	private int[] point1;
	private int[] point2;
	
	public Column(int p1x, int p1y, int p2x, int p2y){
		point1 = new int[2];
		point1[0] = p1x;
		point1[1] = p1y;
		point2 = new int[2];
		point2[0] = p2x;
		point2[1] = p2y;
	}
		
	public int[] getFrontPoint1(){
		return point1;
	}

	public int[] getFrontPoint2(){
		return point2;
	}

	public void setFrontPoint2(int[] point) {
		point2 = point;
	}
}
