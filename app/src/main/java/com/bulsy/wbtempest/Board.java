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
    private static final int BASE_EX_FIRE_BPS = 2000;  // bps chance per sec that an ex fires at player
    public static final int NUMSCREENS = 9;  // number of different screen layouts

    private int levnum;
    private int exesct;
    private float spikespct;
    private List<Column> columns;
    private boolean continuous;
    private boolean exesCanMove;
    int zpull_x;  // z pull is the point that the z-axis leads to for this level
    int zpull_y;
    private static Random r = new Random(new java.util.Date().getTime());


    public Board() {
        columns = new ArrayList<Column>();
    }

    public void init(View v, int levnum) {
        int ncols;
        int colsPerSide;
        this.levnum = levnum;

        int cx = v.getWidth()/2;  // center for drawing; not same as where z-axis goes to.
        int cy = v.getHeight() * 25/60;  // buttons at bottom of screen, so "center" is  usually a little high of actual screen center

        zpull_x = v.getWidth()/2;  // where z-axis goes; default z pull to be just low of center.
        zpull_y = v.getHeight() *33/60;

        continuous = false;
        boolean firsttime = true;
        int x=0, y=0, oldx=0, oldy=0;
        exesct = 6 + (int)(1.5*levnum);
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
        int radius = (int)(v.getWidth()*0.48); // consistent-ish radius for levels that have one
        columns.clear();

        // if we run out of screens....cycle
        int screennum = (levnum-1) % NUMSCREENS;
        //screennum=7;

        switch (screennum) {
            case 0:	// circle
                ncols = 16;
                continuous = true;
                rad_dist = (float) (Math.PI * 2);
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

            case 2: // plus
                continuous = true;
                int colwidth = v.getWidth()/5;
                zpull_x=cx;
                zpull_y= cy+(int)(colwidth*0.8);
                x = oldx = cx-colwidth;
                y = oldy = cy - colwidth;
                x-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                y+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                x+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                x+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                x+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                x+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                y-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                x-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                x-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                x-= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldx = x;
                y+= colwidth;
                columns.add(new Column(oldx, oldy, x, y));
                oldy = y;
                break;

            case 3: // infinitybinocularthing
                ncols = 8;
                zpull_y = cy;//*41/40;
                continuous = true;
                rad_dist = (float) (Math.PI * 2);
                step = rad_dist/(ncols);
                int origx=0;
                int cxc = (int)((float)cx * .44);
                //radius = (int)((cx - cxc)*0.81f);
                radius = (int)((cx - cxc)*0.89f);
                int i = 0;
                for (float rads=-1.5f*step; rads < rad_dist-step*1.5; rads+=step)
                {
                    x = cxc - (int)(Math.sin(rads) * radius * .90);
                    y = cy - (int)(Math.cos(rads) * radius);
                    if (i == 0 || i == 7){
                        y = cy - (int)(Math.cos(rads) * radius * 1.3);
                        x = cxc - (int)(Math.sin(rads) * radius * .85);
                    }
                    if (i==0){
                        origx = x;
                    }
                    else {
                        Column col = new Column(oldx, oldy, x, y);
                        columns.add(col);
                    }
                    oldx = x;
                    oldy = y;
                    i++;
                }
                cxc = (int)((float)cx * 1.56);
                i = 0;
                for (float rads=+2.5f*step; rads < rad_dist+1.5*step; rads+=step)
                {
                    x = cxc - (int)(Math.sin(rads) * radius * .90);
                    y = cy - (int)(Math.cos(rads) * radius);
                    if (i == 0 || i == 7){
                        y = cy - (int)(Math.cos(rads) * radius * 1.3);
                        x = cxc - (int)(Math.sin(rads) * radius * .85);
                    }
                    Column col = new Column(oldx, oldy, x, y);
                    columns.add(col);
                    oldx = x;
                    oldy = y;
                    i++;
                }
                columns.add(new Column(oldx, oldy, origx, y));
                break;

            case 4: // triangle
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

            case 5: // straight, angled V
                ncols = 16;
                // need a different z-pull for a board using this screen
                zpull_x = cx;
                zpull_y = v.getHeight() /4;
                int yhi = cy/3;
                int ylow = cy*5/3;
                int ydist = ylow-yhi;
                int xdist = cx * 3/2;  // 1/4 of cx on each side
                for (x = 0, y = ylow; y >= yhi; x+= xdist/(ncols), y-=ydist/(ncols/2)){
                    if (firsttime){
                        firsttime = false;
                    }
                    else {
                        columns.add(0, new Column(cx-x, y, cx-oldx, oldy));
                        columns.add(new Column(cx+oldx, oldy, cx+x, y));
                    }
                    oldx = x;
                    oldy = y;
                }
                break;

            case 6: // jagged V
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() /5;
                int total_cols = 15;  // must be ODD
                int xcolwidth = (int) Math.floor(v.getWidth()/(total_cols/2 + 1)); // should be enough
                int ycolwidth = xcolwidth * 5/4;
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
                break;

            case 7:	// U
                // arc portion
                ncols = 8;
                radius *= .9;
                rad_dist = (float) (Math.PI); // half circ
                step = rad_dist/(ncols);
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight()*17/28;
                int xradius=0, orgy=0, straightstepdist=0;
                for (double rads=0; rads < Math.PI+step/2; rads+=step)
                {
                    x = cx - (int)(Math.cos(rads) * radius * .95);
                    y = cy * 12/10 + (int)(Math.sin(rads) * radius);
                    if (firsttime){
                        firsttime = false;
                        xradius = cx - x;
                        orgy = y;
                        straightstepdist = (int)(Math.sin(step) * radius);
                    }
                    else {
                        columns.add(new Column(oldx, oldy, x, y));
                    }
                    oldx = x;
                    oldy = y;
                }
                for (int j=0; j<3; j++)
                    columns.add(0, new Column(cx-xradius, orgy-straightstepdist*(j+1), cx-xradius, orgy-straightstepdist*j));
                for (int j=0; j<3; j++)
                    columns.add(new Column(cx+xradius, orgy-straightstepdist*j, cx+xradius, orgy-straightstepdist*(j+1)));
                break;

            case 8: // straight line
                ncols = 14;
                // need a different z-pull for a board using this screen
                zpull_x = v.getWidth()/2;
                zpull_y = v.getHeight() /4;
                y = v.getHeight() * 4/7;
                for (x = v.getWidth() *1/(ncols+1); x < v.getWidth() * (1+ncols)/(ncols+2); x+= v.getWidth()/(ncols+2)){
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
        }
    }

    public List<Column> getColumns(){
        return columns;
    }

    public int getBoardColor(){
        switch ((levnum-1)/NUMSCREENS) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.RED;
            case 2:
                return Color.YELLOW;
            case 3:
                return Color.CYAN;
            case 4:
                return Color.BLACK;
            default:
                return Color.GREEN;
        }
    }

    public int getCrawlerColor(){
        switch ((levnum-1)/NUMSCREENS) {
            case 0:
                return Color.YELLOW;
            case 1:
                return Color.GREEN;
            case 2:
                return Color.BLUE;
            case 3:
                return Color.BLUE;
            case 4:
                return Color.YELLOW;
            default:
                return Color.RED;
        }
    }

    public int getExColor(){
        switch ((levnum-1)/NUMSCREENS) {
            case 0:
                return Color.RED;
            case 1:
                return Color.MAGENTA;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.GREEN;
            case 4:
                return Color.RED;
            default:
                return Color.YELLOW;
        }
    }

    public int getExPodColor(){
        switch ((levnum-1)/NUMSCREENS) {
            case 0:
                return Color.MAGENTA;
            case 1:
                return Color.BLUE;
            case 2:
                return Color.CYAN;
            case 3:
                return Color.MAGENTA;
            case 4:
                return Color.MAGENTA;
            default:
                return Color.MAGENTA;
        }
    }

    public int getSpikeColor(){
        switch ((levnum-1)/NUMSCREENS) {
            case 0:
                return Color.GREEN;
            case 1:
                return Color.CYAN;
            case 2:
                return Color.RED;
            case 3:
                return Color.RED;
            case 4:
                return Color.GREEN;
            default:
                return Color.BLUE;
        }
    }

    //public int getZPull_X() {
    //    return zpull_x;
    //}

    //public int getZPull_Y() {
    //    return zpull_y;
    //}

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

    /**
     * bps chance per second that we will fire
     * @return
     */
    public int getExFireBPS(float elapsedTime){
        return (int)(elapsedTime * (BASE_EX_FIRE_BPS + levnum*15));
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

    Paint paint = new Paint();
    int[] fntCoords = new int[2];  // reusable, VERY NOT multithreadable coordinate storage
    int[] backCoords = new int[2];  // reusable, VERY NOT multithreadable coordinate storage
    float[] pcLinePts = new float[8];
    float[] boardLinePts = new float[3 * 4 * MAX_COLS];
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
    	int oldfntx = 0, oldfnty=0, oldbackx=0, oldbacky=0;
        int idx=0;
        int playerColCoord2 = (playerCol + 1);// % columns.size();
    	int boardColor = getBoardColor();
    	if (isSuperzapping) {
    		boardColor = Color.rgb(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    	}
        paint.setColor(boardColor);
        List<int[]> colCoords = getBoardFrontCoords();
    	for (int i=0; i< colCoords.size(); i++)
    	{
    		fntCoords = ZMagic.renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], 0 - boardpov, this, fntCoords);
    		backCoords = ZMagic.renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], BOARD_DEPTH-boardpov, this, backCoords);
            if (i == playerCol) {
                pcLinePts[0] = fntCoords[0];
                pcLinePts[1] = fntCoords[1];
                pcLinePts[2] = backCoords[0];
                pcLinePts[3] = backCoords[1];
            }
            else if (i == playerColCoord2) {
                pcLinePts[4] = fntCoords[0];
                pcLinePts[5] = fntCoords[1];
                pcLinePts[6] = backCoords[0];
                pcLinePts[7] = backCoords[1];
            }
            if (i < colCoords.size()-1 || !isContinuous()) {
                boardLinePts[idx++] = fntCoords[0];
                boardLinePts[idx++] = fntCoords[1];
                boardLinePts[idx++] = backCoords[0];
                boardLinePts[idx++] = backCoords[1];
            }
    		if (i > 0) {
                boardLinePts[idx++] = oldfntx;
                boardLinePts[idx++] = oldfnty;
                boardLinePts[idx++] = fntCoords[0];
                boardLinePts[idx++] = fntCoords[1];
                boardLinePts[idx++] = oldbackx;
                boardLinePts[idx++] = oldbacky;
                boardLinePts[idx++] = backCoords[0];
                boardLinePts[idx++] = backCoords[1];
    		}
    		oldfntx=fntCoords[0];
    		oldfnty=fntCoords[1];
    		oldbackx=backCoords[0];
    		oldbacky=backCoords[1];
    	}
        c.drawLines(boardLinePts, 0, idx, paint);
        paint.setColor(getCrawlerColor());
        c.drawLines(pcLinePts, paint);
    }
}

