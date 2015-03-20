package com.bulsy.wbtempest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the player's crawler.
 * 
 * @author ugliest
 *
 */
public class Crawler {

	private static final int C_POSES = 6;
	private static final double MAXSPEED = 200;
    static final int CHEIGHT = 10;
    private static final int CHEIGHT_H = CHEIGHT/2; // half height
    private static final int CHEIGHT_HP = (int) (CHEIGHT * 0.6);  // slightly more than half
    private static final int MAX_MISSILES = 6;
    private MainActivity act;
    private double vel =0;
    private double pos=0;
    private boolean visible;
    private ArrayList<Missile> missiles;
    private Board board;
    private int pos_max;
    int[][] coords=new int[9][3];

    public Crawler() {
        missiles = new ArrayList<Missile>();
        visible = true;
    }

    public void init(Board board, MainActivity act) {
        this.board = board;
        this.pos_max = board.getColumns().size() * C_POSES -1;
        this.act = act;
    }

    /**
     * handle crawler movement.  called per tick.
     * @param crawleroffset
     */
    public void move(int crawleroffset, float elapsedTime) {
    	int prevCol = getColumn();
        pos += vel * elapsedTime;

        if (board.isContinuous()){
        	pos %= pos_max;
        	if (pos < 0)
        		pos = pos_max + pos;
        }
        else{
        	if (pos > pos_max)
        		pos = pos_max;
        	else if (pos < 0)
        		pos = 0;
        }

        if (prevCol != getColumn()) {
            // we actually moved columns
            act.playSound(Sound.CRAWLERMOVE);
        }

    }

    /** 
     * Returns the column number where the crawler currently is.
     * @return
     */
    public int getColumn(){
    	return (int)pos / C_POSES;
    }
    
    /**
     * Returns the coordinates to draw the crawler at its current position/pose.
     * 
     * Like everything in this game, the crawler is drawn based on a line
     * connecting a list of points. a few fixed positions are used.
     * 
     * @return
     */
    public List<int[]> getCoords() {
        int colnum = getColumn();
        int pose = (int)pos % C_POSES / 2; // each pose here is doubled for more manageable movement

        if (colnum >= board.getColumns().size())
            pose = board.getColumns().size() * C_POSES - 1;
        Column column = board.getColumns().get(colnum);
        int[] pt1 = column.getFrontPoint1();
        int[] pt2 = column.getFrontPoint2();
        switch (pose)
        {
        	case 0:{
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/3;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/3;
                coords[0][2] = CHEIGHT_H;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/4;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/4;
                coords[4][2] = CHEIGHT_HP;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        	case 1: {
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/3;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/3;
                coords[0][2] = CHEIGHT_H;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])/2;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])/2;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/3;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/3;
                coords[4][2] = CHEIGHT_H;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])/2;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])/2;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        	case 2: {
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[0][2] = CHEIGHT_HP;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])*3/4;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])*3/4;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/3;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/3;
                coords[4][2] = CHEIGHT_H;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])*3/4;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])*2/3;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        }
        coords[1][0] = pt1[0];
        coords[1][1] = pt1[1];
        coords[1][2]=0;
        coords[3][0] = pt2[0];
        coords[3][1] = pt2[1];
        coords[3][2] = 0;
        coords[5][0] = pt2[0] -(pt2[0] - pt1[0])/6;
        coords[5][1] = pt2[1] -(pt2[1] - pt1[1])/6;
        coords[5][2] = 0;
        coords[7][0] = pt1[0] +(pt2[0] - pt1[0])/6;
        coords[7][1] = pt1[1] +(pt2[1] - pt1[1])/6;
        coords[7][2] = 0;
        coords[8] = coords[0];
        return Arrays.asList(coords);
    }

    public ArrayList<Missile> getMissiles() {
        return missiles;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void accel(double factor)
    {
        vel = -factor * MAXSPEED;
        if (vel > MAXSPEED)
            vel = MAXSPEED;
        else if (vel < -MAXSPEED)
            vel = -MAXSPEED;
    }

    public void accelRight(){
    	vel = MAXSPEED;
    }

    public void accelLeft(){
    	vel = -MAXSPEED;
    }

    public void stop() {
        vel = 0;}

    /**
     * fire a missile.
     * @param zoffset - z pos of crawler (if clearing level, crawler will be traveling into board)
     */
    public void fire(int zoffset) {
        if (missiles.size() < MAX_MISSILES) {
            Missile m = Missile.getNewMissile(this.getColumn(), Missile.HEIGHT/3+zoffset, true);
            missiles.add(m);

            // at bottom of board, sound goes haywire
            if (zoffset < Board.BOARD_DEPTH)
                act.playSound(Sound.FIRE);
        }
    }

}

