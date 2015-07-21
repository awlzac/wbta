package com.bulsy.wbtempest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ugliest on 3/10/15.
 */
public class EntryScreen extends Screen {
    MainActivity act;
    Paint p = new Paint();
    Rect scaledDst = new Rect(); // generic rect for scaling
    Rect playBtnBounds = null;
    Rect exitBtnBounds = null;
    List<Bitmap> screenThumbnails;
    Board sampleBoard;
    Bitmap startscreensBmp = null;
    float screensBmpOffset = 0;
    int thmsize = 0; // thumbnail screen image size, initialized when we load bitmaps for screens
    int scrolly; // where scroller should appear onscreen
    float tvx = 0; // touch velocity along x axis
    long frtime = 0;
    float sqpts[] = null;

    public EntryScreen(MainActivity act) {
        int x = ZMagic.Z_MAX_CACHE; // force load of ZMagic class while showing entry screen
        this.act = act;
        this.screenThumbnails = new ArrayList<Bitmap>();
        sampleBoard = new Board();
    }

    @Override
    public void update(View v) {
        long newtime = System.nanoTime();
        float elapsedTime = (float)(newtime - frtime)/PlayScreen.ONESEC_NANOS;
        frtime = newtime;

        // manually control screen chooser scroll
        screensBmpOffset -= tvx * elapsedTime;
        if (tvx == 0) {
            // we aren't being scrolled -- try to round to nearest screen, within not-too-many ticks
            float rem = screensBmpOffset % thmsize;
            if (rem != 0) {
                if (rem < thmsize / 2) {
                    if (rem > 1)
                        screensBmpOffset -= rem / 3;
                    else
                        screensBmpOffset--;
                } else {
                    rem = rem - thmsize;
                    if (rem < -1)
                        screensBmpOffset -= rem / 3;
                    else
                        screensBmpOffset++;
                }
            }
        }
        if (screensBmpOffset < 0)
            screensBmpOffset = 0;
        else if (screensBmpOffset > thmsize * (act.maxStartLevel - 1))
            screensBmpOffset = thmsize * (act.maxStartLevel - 1);
    }

    @Override
    public void draw(Canvas c, View v) {
        int width = v.getWidth();
        int height = v.getHeight();
        int thmsize_y=0;

        String playmsg = act.getResources().getString(R.string.play);
        String exitmsg = act.getResources().getString(R.string.exitapp);

        // draw the screen
        c.drawRGB(0, 0, 0);

        p.setTypeface(act.getGameFont());
        p.setColor(Color.GREEN);  // dark greenish
        p.setTextSize(act.TS_BIG * 1.5f);
        String txt = "WBT";
        c.drawText(txt, (v.getWidth() - p.measureText(txt)) / 2, height / 4, p);

        // screen chooser
        if (screenThumbnails.size() != act.maxStartLevel) {
            // load bitmaps
            thmsize = width/3;
            scrolly = height/2;
            thmsize_y = thmsize +20;
            for (int i = screenThumbnails.size(); i < act.maxStartLevel; i++) {
                // here, "i" will end up being the screen number, 0 indexed; level num for that screen will be (i+1)
                Bitmap b = Bitmap.createBitmap(thmsize, thmsize_y, Bitmap.Config.ARGB_8888);
                Canvas cvs = new Canvas(b);
                sampleBoard.init(cvs.getWidth() *9/10, cvs.getHeight()*9/10, i+1);
                sampleBoard.draw(cvs, 0, 0, false);
                p.setTextSize(act.TS_NORMAL);
                cvs.drawText(Integer.toString(i+1), cvs.getWidth()/8, cvs.getHeight()/8, p);
                screenThumbnails.add(b);
            }
            if (startscreensBmp != null)
                startscreensBmp.recycle();
            startscreensBmp = Bitmap.createBitmap(thmsize * act.maxStartLevel,
                                                         thmsize_y,
                                                         Bitmap.Config.ARGB_8888);
            Canvas cvs = new Canvas(startscreensBmp);
            for (int i = 0; i < screenThumbnails.size(); i++) {
                Bitmap scrbmp = screenThumbnails.get(i);
                cvs.drawBitmap(scrbmp, thmsize * i, 0, p);
            }
        }
        c.drawBitmap(startscreensBmp, (int) (width - thmsize) / 2 - screensBmpOffset, (int)(height / 2.1), p);
        if (sqpts == null) {
            sqpts = new float[16];
            sqpts[0] = (width - thmsize) / 2;
            sqpts[1] = (int)(height / 2.1);
            sqpts[2] = (width - thmsize) / 2 + thmsize;
            sqpts[3] = sqpts[1];
            sqpts[4] = (width - thmsize) / 2 + thmsize;
            sqpts[5] = sqpts[1];
            sqpts[6] = (width - thmsize) / 2 + thmsize;
            sqpts[7] = (int)(height / 2.1) + thmsize;
            sqpts[8] = (width - thmsize) / 2 + thmsize;
            sqpts[9] = (int)(height / 2.1) + thmsize;
            sqpts[10] = sqpts[0];
            sqpts[11] = sqpts[9];
            sqpts[12] = sqpts[0];
            sqpts[13] = sqpts[9];
            sqpts[14] = sqpts[0];
            sqpts[15] = sqpts[1];
        }
        c.drawLines(sqpts, p);
        if (act.maxStartLevel > 1) {
            p.setTextSize(act.TS_NORMAL);
            c.drawText(act.lblSelectStartScr, (width - p.measureText(act.lblSelectStartScr)) / 2, (int) (height / 2.2), p);
        }

        p.setTextSize(act.TS_BIG);
        if (playBtnBounds == null) {
            // initialize button locations
            p.getTextBounds(playmsg, 0, playmsg.length() - 1, scaledDst);
            // weirdly, and annoyingly, the bounds we create magically move and shrink.
            // so...uh....compensate by making the expected touch area bigger.
            playBtnBounds = new Rect(width/4 - scaledDst.width(),
                    height *3/4 - scaledDst.height(),
                    width/4 + scaledDst.width(),
                    height *3/4 + scaledDst.height());
            p.getTextBounds(exitmsg, 0, exitmsg.length() - 1, scaledDst);
            exitBtnBounds = new Rect(width*3/4 - scaledDst.width(),
                    height *3/4 - scaledDst.height(),
                    width*3/4 + scaledDst.width(),
                    height *3/4 + scaledDst.height());
        }
        c.drawText(playmsg, playBtnBounds.left, playBtnBounds.bottom, p);
        c.drawText(exitmsg, exitBtnBounds.left, exitBtnBounds.bottom, p);

        // version line
        p.setTextSize(act.TS_NORMAL);
        String msg = "v"+ BuildConfig.VERSION_NAME;
        int xTextEnd = (int)(width*.99f);
        c.drawText(msg, xTextEnd-p.measureText(msg), height - 80, p);
        int w1 = scaledDst.width();
        msg = "BULSY GAMES 2015";
        c.drawText(msg, xTextEnd-p.measureText(msg), height - 40, p);
    }

    VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    DisplayMetrics dm = new DisplayMetrics();
    @Override
    public boolean onTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                int idx = e.getActionIndex();
                int pid = e.getPointerId(idx);
                if (e.getY() > scrolly && e.getY() < scrolly + thmsize) {
                    mVelocityTracker.clear();
                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(e);
                }
                else {
                    if (playBtnBounds.contains((int) e.getX(), (int) e.getY())) {
                        int startlevel = (int)(screensBmpOffset+2) / thmsize +1;  // overcome rounding error, and convert screen num to level num
                        act.startGame(startlevel);
                    }
                    if (exitBtnBounds.contains((int) e.getX(), (int) e.getY()))
                        act.exit();

                    // we don't care about followup events for buttons in this screen
                    return false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    pid = e.getPointerId(i);
                    // user is moving scroll
                    mVelocityTracker.addMovement(e);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    tvx = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pid);

                }
                break;

            case MotionEvent.ACTION_UP:
                tvx = 0;
        }

        return true;
    }

}
