package com.bulsy.wbtempest;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Represents a spike, which may appear in a column of the board, and may have a spinner
 * which can grow the spike and shott missiles at the player.
 * 
 * @author ugliest
 *
 */
public class Spike {
	private static int IMPACT_DAMAGE_LENGTH = 15;
	static int SPIKE_SCORE = 2;
	static double SPINNER_SPIN_SPEED = .3;
	static int SPINNER_SPEED = 3;
	static int SPINNER_SCORE = 50;
	private static Random r = new Random(new java.util.Date().getTime());
	private int length;
	private int colnum;
	private int spinnerz;
	private double spinnerangle;
	private int spinnerDir = 1;
	private boolean visible;
	private boolean spinnerVisible;
    private static List<Spike> usedSpikes = new LinkedList<Spike>();

    private Spike (){}

	private void init(int colnum) {
		this.colnum = colnum;
		length = r.nextInt(Board.BOARD_DEPTH *3/4)+Board.BOARD_DEPTH /10;
		spinnerz = Board.BOARD_DEPTH - r.nextInt(length);
		spinnerangle = r.nextDouble();
		visible = true;
		spinnerVisible = true;
	}

    public static Spike getNewSpike(int colnum){
        Spike s;
        if (usedSpikes.size() > 0) {
            s = usedSpikes.remove(0);
        }
        else {
            s = new Spike();
        }
        s.init(colnum);
        return s;
    }

    public static void release(Spike s) {
        usedSpikes.add(s);
    }
	public int getColumn() {
		return colnum;
	}
	
	public int getLength() {
		return length;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isSpinnerVisible() {
		return spinnerVisible;
	}
	
	public void setSpinnerVisible(boolean v) {
		spinnerVisible = v;
	}
	
	public int getSpinnerZ() {
		return spinnerz;
	}

	public void move() {
		spinnerz += spinnerDir * SPINNER_SPEED;
		spinnerangle += SPINNER_SPIN_SPEED;
		if (spinnerz > Board.BOARD_DEPTH){
			spinnerz = Board.BOARD_DEPTH;
			spinnerDir = -1; // go up
		}
		else if (spinnerz < Board.BOARD_DEPTH - length) {
			// we're at top of spike; grow spike or flip dir
			if (length < Board.BOARD_DEPTH - Crawler.CHEIGHT*2
					&& r.nextInt(2) > 0) {
				length+= IMPACT_DAMAGE_LENGTH;
			}
			else
				spinnerDir = 1; // go down
		}
	}
	
	/**
	 * a missile has hit this spike.  handle it.
	 */
	public void impact(){
		length -= IMPACT_DAMAGE_LENGTH;
		if (length < IMPACT_DAMAGE_LENGTH)
			visible = false;
	}
	
	public List<int[]> getSpinnerCoords(Board lev){
		int nCoords = 16;
		int[][] coords=new int[nCoords][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		int[] mp = new int[2];
		mp[0] = p1[0] + (p2[0]-p1[0])/2;
		mp[1] = p1[1] + (p2[1]-p1[1])/2;
		int colWidth = (int)Math.sqrt(Math.pow((p2[0]-p1[0]),2) + Math.pow((p2[1]-p1[1]),2));
		int origRadius = colWidth/3;
		int radius = origRadius;
		float rad_dist = (float) (3.1415927 * 2)*3;
		float step = rad_dist/(nCoords);
		int ct = 0;
		for (double rads=spinnerangle; ct < nCoords; rads+=step, ct++)
		{
			coords[ct][0] = mp[0] - (int)(Math.sin(rads) * radius * .85);
			coords[ct][1] = mp[1] - (int)(Math.cos(rads) * radius);
			coords[ct][2] = spinnerz;
			radius = origRadius *ct/nCoords; 
		}
    	return Arrays.asList(coords);
	}

	public List<int[]> getCoords(Board lev){
		int[][] coords=new int[2][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		coords[0][0]=p1[0] + (p2[0] - p1[0])/2;
		coords[0][1]=p1[1] + (p2[1] - p1[1])/2;
		coords[0][2]=Board.BOARD_DEPTH;
		coords[1][0]=coords[0][0];
		coords[1][1]=coords[0][1];
		coords[1][2]=Board.BOARD_DEPTH - length;
    	return Arrays.asList(coords);
	}
	
	
  
}
