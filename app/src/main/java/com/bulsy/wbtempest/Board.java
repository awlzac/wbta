package com.bulsy.wbtempest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;

import java.util.*;

/**
 * Board/Level. handles board, layout.
 * 
 * @author ugliest
 *
 */
public class Board {
    static final int BOARD_DEPTH = 400;
    static final int FIRST_SPIKE_LEVEL = 4;  // level at which we first see spikes
    static final int MAX_COLS = 40; // max conceivable number of columns
    private static final int BASE_EX_FIRE_BPS = 35;  // bps chance per tick that we fire an ex at player
    public static final int NUMSCREENS = 6;  // number of different screen layouts

    private int levnum;
    private int exesct;
    private float spikespct;
    private List<Column> columns;
    private boolean continuous;
    private boolean exesCanMove;
    private int zpull_x;  // z pull is the point that the z-axis leads to for this level
    private int zpull_y;
    private static Random r = new Random(new java.util.Date().getTime());


    public Board() {
        columns = new ArrayList<Column>();
    }

    public void init(View v, int levnum) {
        int ncols;
        int colsPerSide;
        this.levnum = levnum;

        int cx = v.getWidth()/2;  // center for drawing; not same as where z-axis goes to.
        int cy = v.getHeight() * 24/60;

        zpull_x = v.getWidth()/2;  // where z-axis goes; default z pull to be just low of center.
        zpull_y = v.getHeight() *33/60;

        continuous = false;
        boolean firsttime = true;
        int x, y, oldx=0, oldy=0;
        exesct = 5 + levnum*2;
        exesCanMove = (levnum != 1);
        if (levnum < FIRST_SPIKE_LEVEL)
            spikespct = (float)0;
        else if (levnum < FIRST_SPIKE_LEVEL +1)
            spikespct = (float) 0.5;
        else if (levnum < FIRST_SPIKE_LEVEL*2)
            spikespct = (float) 0.75;
        else spikespct = (float) 1;
        float rad_dist;
        float step;
        int radius = (int)(v.getWidth()/2); // consistent-ish radius for levels that have one
        columns.clear();

        // if we run out of screens....cycle
        int screennum = (levnum-1) % NUMSCREENS;
        //screennum=5;

        switch (screennum) {
            case 0:	// circle
                ncols = 16;
                continuous = true;
                rad_dist = (float) (3.1415927 * 2);
                step = rad_dist/(ncols);
                for (float rads=0; rads < rad_dist+step/2; rads+=step)
                {
                    x = cx - (int)(Math.sin(rads) * radius * .95);
                    y = cy - (int)(Math.cos(rads) * radius);
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                }
                break;

            case 1: // square
                continuous = true;
                ncols = 16;
                colsPerSide = ncols/4;
                radius *= .95;
                // left
                for (x = cx - radius, y = cy-radius; y < cy+radius; y+=(radius*2/colsPerSide)){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                }
                // bottom
                for (x = cx - radius, y = cy+radius; x < cx+radius; x+=(radius*2/colsPerSide)){
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                }
                // right
                for (x = cx + radius, y = cy+radius; y > cy-radius; y-=(radius*2/colsPerSide)){
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                }
                // top
                for (x = cx + radius, y = cy-radius; x >= cx-radius; x-=(radius*2/colsPerSide)){
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                }
                break;

            case 2: // triangle
                continuous = true;
                //radius = 320;
                ncols = 15;
                colsPerSide = ncols/3;
//                cy = v.getHeight()*11/20;
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() *29/60;
                // left
                for (x = cx, y = cy-radius; y < cy+radius*4/5; y+=(radius*4/3/colsPerSide),x-=radius*3/4/colsPerSide){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                }
                // bottom
                firsttime = true;
                int targx = cx + (cx-oldx);
                for (x = oldx, y = oldy; x < targx; x+=(radius*4/3/colsPerSide)){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                }
                // right
                for (; y >= cy-radius; y-=(radius*4/3/colsPerSide),x-=radius*3/4/colsPerSide+4){
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                }
                break;

            case 3: // straight, angled V
                ncols = 16;
                // need a different z-pull for a board using this screen
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() /4;
                for (x = cx/4, y=cy/3; x < cx; x+= cx*2/3/(ncols/2), y+=(cy*2.5)/(ncols)){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                }
                int diffx = x - cx; // we may have a remainder, and on some screen sizes this may screw up rendering
                for (; x <= cx*7/4 + diffx; x+= cx*2/3/(ncols/2), y-=(cy*2.5)/(ncols)){
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                }
                break;

            case 4: // straight line
                ncols = 14;
                // need a different z-pull for a board using this screen
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() /4;
                y = v.getHeight() * 5/7;
                for (x = v.getWidth() *1/(ncols+2); x < v.getWidth() * (1+ncols)/(ncols+2); x+= v.getWidth()/(ncols+2)){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;

                }
                break;

            case 5: // jagged V
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() /5;
                int ycolwidth = 90;
                int xcolwidth = ycolwidth *4/5;
                int ystart;
                x = oldx = cx - (int)(xcolwidth*3.5);
                y = oldy = ystart = cy - ycolwidth;
                y+=ycolwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                while (y < ystart + ycolwidth*4){
                    x+=xcolwidth;
                    columns.add(new Column(oldx, oldy, x, y));
                    oldx=x;
                    y+=ycolwidth;
                    columns.add(new Column(oldx, oldy, x, y));
                    oldy=y;
                }
                while (y > ystart){
                    x+=xcolwidth;
                    columns.add(new Column(oldx, oldy, x, y));
                    oldx=x;
                    y-=ycolwidth;
                    columns.add(new Column(oldx, oldy, x, y));
                    oldy=y;
                }
        }
    }

    public List<Column> getColumns(){
        return columns;
    }

    public int getLevelColor(){
        if (levnum > NUMSCREENS *2)
            return Color.RED;
        return Color.BLUE;
    }

    public int getZPull_X() {
        return zpull_x;
    }

    public int getZPull_Y() {
        return zpull_y;
    }

    public boolean isContinuous(){
        return continuous;
    }

    public boolean exesCanMove(){
        return exesCanMove;
    }

    public int getLevelNumber() {return levnum;}

    public int getNumExes(){
        return this.exesct;
    }

    public int getExFireBPS(){
        return BASE_EX_FIRE_BPS + levnum/2;
    }

    public int getNumSpikes(){
        return (int) (spikespct * columns.size());
    }

    public List<int[]> getBoardFrontCoords(){
        List<int[]> coordList = new ArrayList<int[]>();
        for (int i=0; i<columns.size(); i++){
            Column col = columns.get(i);
            if (i==0)
                coordList.add(col.getFrontPoint1());
            coordList.add(col.getFrontPoint2());
        }
        return coordList;
    }


   // public void addNotify() {
   //     super.addNotify();
   // }


    Paint paint = new Paint();
    /**
     * Draw the actual board, based on the coordinates of the front of the
     * current level.  depth axis is generated.
     *
     * @param c canvas on which to draw
     * @param playerCol column on which player currently is, needed to color the column
     * @param boardpov the pov from which we are to see the board, in z-space
     * @param isSuperzapping is the player superzapping at moment?
     */
    public void draw(Canvas c,int playerCol, int boardpov, boolean isSuperzapping){
    	int oldx = 0, oldy=0, oldbackx=0, oldbacky=0;
    	int boardColor = getLevelColor();
    	if (isSuperzapping) {
    		boardColor = Color.rgb(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    	}
        paint.setColor(boardColor);
        List<int[]> colCoords = getBoardFrontCoords();
    	for (int i=0; i< colCoords.size(); i++)
    	{
    		int[] ftCoords = ZMagic.renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], 0 - boardpov, this);
    		int x=ftCoords[0];
    		int y=ftCoords[1];
    		int[] backCoords = ZMagic.renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], BOARD_DEPTH-boardpov, this);
    		int backx = backCoords[0];
    		int backy = backCoords[1];
    		if (i > 0) {
    			c.drawLine(oldx, oldy, x, y, paint);
  			    c.drawLine(oldbackx, oldbacky, backx, backy, paint);
    			if (i == playerCol || i == playerCol+1){
    		        paint.setColor(Color.YELLOW);
    			}
    			if (i < colCoords.size()-1 || i==playerCol+1 || !isContinuous())
        			c.drawLine(x,  y, backx, backy, paint);
    			if (i == playerCol || i == playerCol + 1){
        	        paint.setColor(boardColor);
    			}
    		}
    		else {
    			if (i == playerCol || i == playerCol+1){
    		        paint.setColor(Color.YELLOW);
    			}
    			c.drawLine(x, y, backx, backy, paint);
    			if (i == playerCol || i == playerCol + 1){
        	        paint.setColor(boardColor);
    			}
    		}
    		oldx=x;
    		oldy=y;
    		oldbackx=backx;
    		oldbacky=backy;
    	}
    }

}

