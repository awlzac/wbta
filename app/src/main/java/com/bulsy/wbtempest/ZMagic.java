package com.bulsy.wbtempest;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.List;

/**
 * This handles rendering and conversion from x,y,z space to actual
 * x,y space on screen.  Fallout from porting.
 *
 * Created by ugliest on 3/13/15.
 */
public class ZMagic {
    private static double ZSTRETCH = 100; // lower = more stretched on z axis
    private static Paint paint=new Paint();

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
        return 1.0-(ZSTRETCH/(z+ZSTRETCH) );
    }

    // the downside to the curve used to represent the z-axis, is that
    // it goes to infinity quickly for negative Z values.  to get around this,
    // a line is used to continue the slope manageably for negative z.
    private static final double ZFACT_TAIL_SLOPE = 2*(getRawZFact(1)- getRawZFact(0)) / (1-0);

    private static double getZFact(int z) {
        double zfact = getRawZFact(z);
        if (z<-Ex.HEIGHT) // switch to a constant slope to avoid math oblivion for negative z
            zfact = z * ZFACT_TAIL_SLOPE;
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
    public static int[] renderFromZ(int x, int y, int z, Board board){
        double zfact = getZFact(z);
        int eff_x = x + (int)(zfact * (board.getZPull_X()-x));
        int eff_y = y + (int)(zfact * (board.getZPull_Y()-y));
        int[] effcoords = {eff_x, eff_y};
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
        int oldx = 0, oldy=0;
        paint.setColor(color);
        for (int i=0; i<coords.size(); i++)
        {
            int x=coords.get(i)[0];
            int y=coords.get(i)[1];
            int z=coords.get(i)[2];
            int[] eff_coords = renderFromZ(x, y, z-boardpov+zoffset, board);

            if (i > 0) {
                c.drawLine(oldx, oldy, eff_coords[0], eff_coords[1], paint);
            }
            oldx=eff_coords[0];
            oldy=eff_coords[1];
        }
    }
}
