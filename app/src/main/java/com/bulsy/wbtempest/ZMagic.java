package com.bulsy.wbtempest;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handles rendering and conversion from x,y,z space to actual
 * x,y space on screen.  Fallout from porting.
 *
 * Created by ugliest on 3/13/15.
 */
public class ZMagic {
    private static double ZSTRETCH = 100; // lower = more stretched on z axis
    private static Paint paint=new Paint();

    static final int Z_MAX_CACHE = Board.BOARD_DEPTH * 3;
    private static double[] zfactMap = new double[Z_MAX_CACHE];
    static {
        for (int i=0; i < Z_MAX_CACHE; i++) {
            zfactMap[i] = getRawZFact(i);
        }
    }

    /**
     * This is how we achieve the 3D effect.  The z-axis is assumed to
     * point "into" the screen, away from the player.  interpolation is
     * done toward this center to simulate a z-axis -- but it can't be
     * done linearly, or depth perception is ruined.  this function
     * creates a "z factor", based on the
     * z position, which asymptotically approaches but never hits the
     * board center (which represents z-infinity). this z-factor can then
     * be used by the caller as a percentage of the way to the center.
     *
     * @param z the inpassed z position
     * @return
     */
    private static double getRawZFact(int z) {
        return 1.0-(ZSTRETCH/((double)z+ZSTRETCH) );
    }

    // the downside to the curve used to represent the z-axis, is that
    // it goes to infinity quickly for negative Z values.  to get around this,
    // a line is used to continue the slope manageably for negative z.
    private static final double ZFACT_TAIL_SLOPE = 2*(getRawZFact(1)- getRawZFact(0)) / (1-0);
    private static final int Z_SLOPE_CUTOFF = -Ex.HEIGHT;

    private static double getZFact(int z) {
        double zfact;
        if (z < Z_SLOPE_CUTOFF) // switch to a constant slope to avoid math oblivion for negative z
            zfact = (double)z * ZFACT_TAIL_SLOPE;
        else
            zfact = getRawZFact(z);
        return zfact;
    }

    /**
     * given a point in (x,y,z) space, return the real (x,y) coords needed to
     * display the point.
     *
     * @param x
     * @param y
     * @param z
     * @return int array holding x and y coords.
     */
    static int[] renderFromZ(int x, int y, int z, Board board, int[] effcoords){
        double zfact;
        if (z >= 0 && z < Z_MAX_CACHE)
            zfact = zfactMap[z];
        else
            zfact = getZFact(z);
        effcoords[0] = x + (int)(zfact * (board.zpull_x-x));
        effcoords[1] = y + (int)(zfact * (board.zpull_y-y));
        return effcoords;
    }


    /**
     * draw the inpassed object; inpassed coords are 3D.
     * @param c
     * @param color
     * @param coords
     */
    public static void drawObject(Canvas c, int color, List<int[]> coords, Board board, int boardpov){
        drawObject(c, color, coords, board, boardpov, 0);
    }

    private static final int MAX_POINTS = 20;  // max points in a drawable object
    private static float pts[] = new float[4 * MAX_POINTS];
    private static int effcoords[] = new int[2];  // reusable coords for z-rendering of drawn object
    /**
     * Draw the object inpassed, which is assumed to be a set of coordinates
     * of dots to connect.  inpassed coords are 3d (x, y, z space).
     *
     * The goal here is to emulate the vector graphics style of tempest, where
     * everything is a combination of drawn lines.
     *
     * @param c - the canvas
     * @param color - which color to use to render the object.
     * @param coords
     * @param zoffset
     */
    public static void drawObject(Canvas c, int color, List<int[]> coords, Board board, int boardpov, int zoffset){
        int idx = 0;
        paint.setColor(color);
        int nLines = coords.size()-1;
        for (int i=0; i<coords.size(); i++)
        {
            int[] cd = coords.get(i);
            effcoords = renderFromZ(cd[0], cd[1], cd[2]-boardpov+zoffset, board, effcoords);

            pts[idx++]=effcoords[0];
            pts[idx++]=effcoords[1];
            if (i > 0 && i < nLines) {
                pts[idx++]=effcoords[0];
                pts[idx++]=effcoords[1];
            }
        }
        c.drawLines(pts, 0, idx, paint);
    }
}
