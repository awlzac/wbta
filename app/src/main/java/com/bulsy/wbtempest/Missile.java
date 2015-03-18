package com.bulsy.wbtempest;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A missile.  If being fired down into the level, it represents a player missile;
 * if fired up (at the player), it represents an enemy missile.
 * 
 * @author ugliest
 *
 */
public class Missile {
	private static final int BASE_SPEED = 500;
	static final int HEIGHT = 8;
	private static final int HEIGHT_H = HEIGHT/2;
    private static final int ENEMY_MISSILE_SPEED_FACTOR = 3;
	private int colnum;
	private int zpos;
	private boolean visible;
	private int speed;
    private static List<Missile> usedMissiles = new LinkedList<Missile>();

    public static Missile getNewMissile(int colnum, int zpos, boolean down){
        Missile m;
        if (usedMissiles.size() > 0) {
            m = usedMissiles.remove(0);
        }
        else
            m = new Missile();
        m.init( colnum, zpos,down);
        return m;
    }

    private Missile(){}

	public void init(int colnum, int zpos, boolean down){
        this.visible = true;
		this.colnum = colnum;
		this.zpos = zpos;
    	if (down)
    		speed = BASE_SPEED;
    	else
    		speed = -BASE_SPEED/ENEMY_MISSILE_SPEED_FACTOR;
	}

    public static void release(Missile m){
        usedMissiles.add(m);
    }

	public int getZPos(){
		return zpos;
	}
	
	public int getColumn(){
		return colnum;
	}

    /**
     * move the missile
     * @param maxz
     * @param elapsedTime time in seconds since last update
     */
	public void move(int maxz, float elapsedTime){
		zpos+=speed * elapsedTime;
		if ((zpos > maxz) || (zpos < 0))
			visible = false;
	}
	
	public boolean isVisible(){
		return visible;
	}
	
	/**
	 * return the points that make up the onscreen missile.
	 * 
	 * @param lev
	 * @return
	 */
	public List<int[]> getCoords(Board lev){
		int[][] coords = new int[5][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		coords[0][0] = p1[0]+(p2[0] - p1[0])*2/5;
		coords[0][1] = p1[1]+(p2[1] - p1[1])*2/5;
		coords[0][2] = zpos-HEIGHT_H;
		coords[1][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[1][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[1][2] = zpos-HEIGHT;
		coords[2][0] = p1[0]+(p2[0] - p1[0])*3/5;
		coords[2][1] = p1[1]+(p2[1] - p1[1])*3/5;
		coords[2][2] = zpos-HEIGHT_H;
		coords[3][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[3][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[3][2] = zpos;
		coords[4] = coords[0];
		
		return Arrays.asList(coords);
	}

	/**
	 * coordinates for the second layer to be drawn; idea is to allow board to draw in 
	 * a different color.
	 * @param lev
	 * @return
	 */
	public List<int[]> getLayerCoords(Board lev){
		int[][] coords = new int[5][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		coords[0][0] = p1[0]+(p2[0] - p1[0])*9/20;
		coords[0][1] = p1[1]+(p2[1] - p1[1])*9/20;
		coords[0][2] = zpos-HEIGHT_H;
		coords[1][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[1][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[1][2] = zpos-HEIGHT*3/5;
		coords[2][0] = p1[0]+(p2[0] - p1[0])*11/20;
		coords[2][1] = p1[1]+(p2[1] - p1[1])*11/20;
		coords[2][2] = zpos-HEIGHT_H;
		coords[3][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[3][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[3][2] = zpos-HEIGHT*2/5;
		coords[4] = coords[0];
		
		return Arrays.asList(coords);
	}

	public void setVisible(boolean b) {
		this.visible = b;
	}
}
