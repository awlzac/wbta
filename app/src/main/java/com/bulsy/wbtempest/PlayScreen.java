package com.bulsy.wbtempest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Represents the main screen of play for the game.
 *
 * Created by ugliest on 12/29/14.
 */
public class PlayScreen extends Screen {
    private static final long serialVersionUID = -1467405293079888602L;
    private static final String HISCORE_FILENAME = "wbt.hi";
    private static Random r = new Random(new java.util.Date().getTime());
    static final int START_LIVES = 3;
    private static final int SPEED_LEV_ADVANCE = 265;  // speed at which we fly to the next board after clearing a level
    private static final int GAME_OVER_BOARDSPEED = 2300;  // speed at which the game recedes when player loses
    static final long ONESEC_NANOS = 1000000000L;
    private static final long DEATH_PAUSE_NANOS = ONESEC_NANOS*2/3;  // time to pause on crawler death
    private static final long SUPERZAPPER_NANOS = ONESEC_NANOS/3; // how long does a superzap last?
    private static final long FIRE_COOLDOWN_NANOS = ONESEC_NANOS / 30; // max firing rate, per sec
    private static final int NUM_STARS = 100; // number of stars when entering a level
    private static final int MAX_VEL = 2500; // spin-controlling at this pace is "fast"
    private static final int INIT_LEVELPREP_POV = Board.BOARD_DEPTH * 2;  // start level-intro zoom from this distance
    private static final int PER_LEV_CLEAR_BONUS = 100;  // level clear bonus is this *level
    private static final int EXTRA_LIFE_SCORE = 20000;  // score at which player gets an extra life

    private Crawler crawler;
    private ArrayList<Ex> exes;
    private ArrayList<Missile> enemymissiles;
    private ArrayList<Spike> spikes;
    private boolean clearboard=false;
    private int levelnum = 1;
    private Board board;
    private boolean pause = false;
    private boolean levelcleared = false;
    private boolean levelprep = false;  // not dealt with by initLevel() - have to set it explicitly to handle level advance visual
    private boolean gameover = false;
    private int lives;
    private int score;
    private int hiscore=0;
    private float scorertx = -1;
    private int boardpov;
    private int crawlerzoffset;
    private long deathPauseTime = 0;
    private int buttonLimitLine = 0;
    private Rect btnFire1Bounds;
    private Rect btnFire2Bounds;
    private Rect btnSuperzapBounds;
    private long fireReadyTime = 0;
    private volatile boolean fireMissile = false;
    private volatile boolean fireSuperzapper = false;
    private long superzapperOverTime = 0;  // time at which the zap color display will end
    private int superzaps = 1;  // how many superzaps does the player have available?
    private int nZapKillsPerTick;
    private boolean crawlerSpiked;
    private List<int[]> starList = null;  // will be created and populated first time around
    int[] xycoords = new int[2];
//    float[] starpts = new float[NUM_STARS*2];
    Paint starpaint = new Paint();
    private int height, width, halfheight, halfwidth;
    private int rhstextoffset;
    private int statstextheight, statstextheight2;
    private String info= "";
    private String bonustxt;
    private int nextLife = 0;
    private int levchgStreamID = 0;
    boolean hasSpike[] = new boolean[Board.MAX_COLS]; // most columns any screen will have

    private Paint p=new Paint();
    private long frtime = 0;
    private float fps;
    private Rect scaledDst = new Rect();
    private MainActivity act = null;
    private boolean gamestarting = true;  // flag to init level from within update, so that we have screen coords

    private String lblLives;
    private String lblHigh;
    private String lblLevel;
    private String lblFire;
    private String lblZap;
    private String lblZapRechg;
    private String lblAvoidSpikes;
    private String lblExit;
    private String lblGameOver;
    private String lblLevelClearBonus;
    String lblSelectStartScr;

    public PlayScreen(MainActivity act) {
        this.act = act;
        exes = new ArrayList<Ex>();
        enemymissiles = new ArrayList<Missile>();
        spikes = new ArrayList<Spike>();
        board = new Board();
        crawler = new Crawler();

        try {
            BufferedReader f = new BufferedReader(new FileReader(act.getFilesDir() + HISCORE_FILENAME));
            hiscore = Integer.parseInt(f.readLine());
            f.close();
        } catch (Exception e) {
            Log.d(MainActivity.LOG_ID, "ReadHiScore", e);
        }

        lblLevel = act.getResources().getString(R.string.level)+": ";
        lblHigh = act.getResources().getString(R.string.high)+": ";
        lblLives = act.getResources().getString(R.string.lives)+": ";
        lblFire = act.getResources().getString(R.string.fire);
        lblZap = act.getResources().getString(R.string.zap);
        lblZapRechg = act.getResources().getString(R.string.zaprechg);
        lblAvoidSpikes = act.getResources().getString(R.string.avoidspikes);
        lblGameOver = act.getResources().getString(R.string.gameover);
        lblExit = act.getResources().getString(R.string.exit);
        lblLevelClearBonus = act.getResources().getString(R.string.lvlclrbonus);
        act.lblSelectStartScr = act.getResources().getString(R.string.selectstartscr);
    }

    /**
     * Initialize the game.
     */
    public void startGame(int startlevel)
    {
        lives=START_LIVES;
        gameover=false;
        score = 0;
        levelnum = startlevel;
        gamestarting = true;
        frtime = 0;
        nextLife = EXTRA_LIFE_SCORE;
    }

    /**
     * initialize a level for play
     */
    private void initLevel(View v){
        board.init(v.getWidth(), v.getHeight(), levelnum);
        crawler.init(board, act);
        exes.clear();
        int ncols = board.getColumns().size();
        for (int i=0; i<board.getNumExes(); i++ ) {
            exes.add(Ex.getNewEx(r.nextInt(ncols),
                    levelnum > 1 ? r.nextBoolean() : false,
                    board));
        }
        for (Spike s : spikes) {
            Spike.release(s);
        }
        spikes.clear();
        if (board.getNumSpikes() > 0) {
            for (int i=0; i<ncols;i++)
                hasSpike[i]=true;
            for (int i=0; i<(ncols-board.getNumSpikes());) {
                int sc = r.nextInt(ncols);
                if (hasSpike[sc]) {
                    hasSpike[sc] = false;
                    i++;
                }
            }
            for (int i=0; i<ncols;i++)
                if (hasSpike[i])
                    spikes.add(Spike.getNewSpike(i));

        }
        superzaps = 1;  // superzapper recharges at start of every level
        fireSuperzapper = false;

        //also do whatever we need when replaying a level
        replayLevel();
    }

    /**
     * Reattempt current level.  retains state of level after player death.
     */
    public void replayLevel() {
        levelcleared = false;
        clearboard = false;
        boardpov = crawlerzoffset = 0;
        crawlerSpiked = false;
        enemymissiles.clear();
        if (exes.size() == 0) {
            // need at least one ex
            exes.add(Ex.getNewEx(r.nextInt(board.getColumns().size()),
                    r.nextBoolean(),
                    board));
        }
        if (exes.size() == 1)
            exes.get(0).resetZ((int)(board.BOARD_DEPTH *3.0f/2.0f));
        else { // reposition all the exes in the center spawning area
            for (Ex ex : exes )
                ex.resetZ((int)(r.nextInt((int)(board.BOARD_DEPTH * (2.0f+exes.size()/3.0f))) + board.BOARD_DEPTH * 5.0f/4.0f));
        }
    }

    private boolean isPlayerDead(){
        return (clearboard && !levelcleared) || crawlerSpiked;
    }


    private void playerDeath() {
        lives--;
        clearboard = true;
        deathPauseTime = System.nanoTime() + DEATH_PAUSE_NANOS;
        act.playSound(Sound.CRAWLERDEATH);
    }

    private void writeDataFiles() {
        try {
            BufferedWriter f = new BufferedWriter(new FileWriter(act.getFilesDir() + HISCORE_FILENAME));
            f.write(Integer.toString(hiscore)+"\n");
            f.close();
            if (levelnum > MainActivity.LOW_LEVEL_THRESHOLD || levelnum > act.maxStartLevel) {
                act.maxStartLevel = levelnum - 1;
                f = new BufferedWriter(new FileWriter(act.getFilesDir() + MainActivity.STARTLEVEL_FILENAME));
                f.write(Integer.toString(act.maxStartLevel) + "\n");
                f.close();
            }
        } catch (Exception e) { // if we can't write the hi score or start level file...oh well.
            Log.d(MainActivity.LOG_ID, "WriteDataFiles", e);
        }
    }

    @Override
    public void update(View v) {
        if (pause)
            return; // if we're on pause, don't do anything.

        long newtime = System.nanoTime();
        float elapsedTime = (float)(newtime - frtime)/ONESEC_NANOS;
        if (frtime == 0) {
            // first time through -- no elapsed time
            elapsedTime = 0;
        }
        frtime = newtime;
        fps = 1/elapsedTime;

        if (gamestarting) {
            gamestarting = false;
            initLevel(v);
        }

        if (btnFire1Bounds == null) {
            // init button locs
            width = v.getWidth();
            halfwidth = width/2;
            height = v.getHeight();
            halfheight = height/2;
            buttonLimitLine = (int)(height * .8f);
            int spacer = 10;
            int halfspacer = spacer>>1;
            int x_sep1 = (int)(width * .4f);
            int x_sep2 = (int)(width * .6f);
            btnFire1Bounds = new Rect(spacer, buttonLimitLine, x_sep1 - halfspacer, height-spacer);
            btnSuperzapBounds = new Rect(x_sep1 +halfspacer, buttonLimitLine, x_sep2 - halfspacer, height-spacer);
            btnFire2Bounds = new Rect(x_sep2+halfspacer, buttonLimitLine, v.getWidth()-spacer, height-spacer);
            p.setTextSize(act.TS_NORMAL);
            p.setTypeface(act.getGameFont());
            String t = lblLives+": 9";
            rhstextoffset = (int)p.measureText(t);
            p.getTextBounds(t, 0, t.length()-1, scaledDst);
            statstextheight = (int)(scaledDst.height() +8);
            statstextheight2 = statstextheight * 2;
        }

        if (starList == null) {
            // set up stars for inter-level flow
            int xadj, yadj, fieldlen;
            if (v.getHeight() > v.getWidth()) {
                fieldlen = v.getHeight();
                yadj = 0;
                xadj = -(v.getHeight()-v.getWidth()) / 2;
            }
            else {
                fieldlen = v.getWidth();
                xadj = 0;
                yadj = -(v.getWidth()-v.getHeight()) / 2;
            }

            starList = new ArrayList<int[]>();
            for (int i=0; i< NUM_STARS; i++){
                int[] starcoords = new int[3];

                starcoords[0] = r.nextInt(fieldlen) + xadj;
                starcoords[1] = r.nextInt(fieldlen) + yadj;
                starcoords[2] = -r.nextInt(INIT_LEVELPREP_POV * 2 / 3); // concentrate stars close-ish to playing board
                starList.add(starcoords);
            }
        }

        // if player died, they don't get to move crawler or superzap
        if (!isPlayerDead()){
            if (fireSuperzapper && superzaps > 0){
                superzapperOverTime = frtime + SUPERZAPPER_NANOS;
                fireSuperzapper = false;
                // compute appx how many exes to kill per tick, for a staggered "most-of-them" kill effect
                // assume worstcase fps of half of current, and assume half the exes on board can spawn
                nZapKillsPerTick = (int)(Ex.exesOnBoard(exes)*1.5/(fps/2 * SUPERZAPPER_NANOS/ONESEC_NANOS));
                superzaps--;
            }
            crawler.move(crawlerzoffset, elapsedTime);
            if (fireMissile & frtime > fireReadyTime) {
                crawler.fire(crawlerzoffset);
                fireReadyTime = frtime + FIRE_COOLDOWN_NANOS;
            }
        }

        if (clearboard)	{
            // if we're clearing the board, updating reduces to the boardclear animations
            if (crawlerSpiked) {
                if (frtime >= deathPauseTime) {
                    if (lives > 0)
                        replayLevel();
                    else
                        crawlerSpiked = false; // damage has been done; other case will now handle game over.
                }
            }
            else if (levelcleared)
            {   // player passed level.
                // pull board out towards screen until player leaves far end of board.
                float incr = elapsedTime * SPEED_LEV_ADVANCE;
                // speed should increase as we go
                float grad = ((float)boardpov + incr)/(board.BOARD_DEPTH/2);
                if (grad < 1)
                    grad = 1;
                incr = incr * grad;
                boardpov += incr;
                if (crawlerzoffset < board.BOARD_DEPTH)
                    crawlerzoffset+= incr;

                if (boardpov > board.BOARD_DEPTH * 5/4)
                {
                    score += levelnum * PER_LEV_CLEAR_BONUS;
                    bonustxt = lblLevelClearBonus + " " + levelnum*PER_LEV_CLEAR_BONUS;
                    levelnum++;
                    initLevel(v);
                    boardpov = -INIT_LEVELPREP_POV;
                    levelprep=true;
                }
            }
            else if (frtime >= deathPauseTime) {
                if (lives > 0) {
                    // player died but not out of lives.
                    // suck crawler down and restart level
                    crawlerzoffset += elapsedTime * SPEED_LEV_ADVANCE * 2;  // twice as fast as level adv
                    if (crawlerzoffset > board.BOARD_DEPTH)
                        replayLevel();
                } else {
                    // player died and game is over.
                    // advance everything along z away from player.
                    if (boardpov > -board.BOARD_DEPTH * 5)
                        boardpov -= elapsedTime * GAME_OVER_BOARDSPEED;
                    else {
                        gameover = true;
                        writeDataFiles();
                    }
                }
            }
            // ...otherwise, we died and are waiting for death pause to pass
        }

        if (lives > 0)
        {
            if (levelprep)
            {   // just cleared a level and we're prepping the new one
                // advance POV onto board, and then allow normal play
                boardpov += elapsedTime * SPEED_LEV_ADVANCE*3/2;
                if (boardpov >= 0)
                {
                    boardpov = 0;
                    levelprep = false;
                }
            }

            if (!isPlayerDead())
            {   // if the player is alive, the exes and spikes can move and shoot
                for (int i = 0; i < exes.size(); i++) {
                    Ex ex = (Ex) exes.get(i);
                    if (ex.isVisible())
                    {
                        ex.move(levelnum, crawler.getColumn(), elapsedTime);
                        if (ex.getZ() <= 0) {
                            if (ex.isPod()) {
                                // we're at the top of the board; split the pod
                                exes.add(ex.spawn());
                                ex.setPod(false);
                            }
                        }
                        if ((ex.getZ() < board.BOARD_DEPTH)
                                && (r.nextInt(10000) < board.getExFireBPS(elapsedTime)))
                        { // this ex fires a missile
                            enemymissiles.add(Missile.getNewMissile(ex.getColumn(), ex.getZ(), false));
                            act.playSound(Sound.ENEMYFIRE);
                        }
                    }
                    else {
                        Ex.release(exes.remove(i));
                    }
                }

                for (Spike s : spikes) {
                    if (s.isVisible()) {
                        if (s.isSpinnerVisible()) {
                            s.move(elapsedTime);
                            if ((s.getSpinnerZ() < board.BOARD_DEPTH)
                                    && (r.nextInt(10000) < board.getExFireBPS(elapsedTime)/4))
                            { // with 1/4 the frequency of an ex, this spinner fires a missile
                                enemymissiles.add(Missile.getNewMissile(s.getColumn(), s.getSpinnerZ(), false));
                                act.playSound(Sound.ENEMYFIRE);
                            }
                        }
                    }
                }
            }

            // update ex missiles
            for (int i = 0; i < enemymissiles.size(); i++) {
                Missile exm = (Missile) enemymissiles.get(i);
                if (exm.isVisible())
                    exm.move(board.BOARD_DEPTH, elapsedTime);
                else {
                    Missile.release(enemymissiles.remove(i));
                }
            }

            if (!isPlayerDead())
                checkCollisions();

            // update player missile positions -- after collission checkm in case we just fired a missile, and ex is next door
            ArrayList<Missile> ms = crawler.getMissiles();
            for (int i = 0; i < ms.size(); i++) {
                Missile m = (Missile) ms.get(i);
                if (m.isVisible())
                    m.move(board.BOARD_DEPTH, elapsedTime);
                else
                    Missile.release(ms.remove(i));
            }

            // did player clear level?
            if (exes.size() <= 0 && !crawlerSpiked && !levelcleared)
            {
                levelcleared = true;
                clearboard = true;
                levchgStreamID = act.playSound(Sound.LEVCHG);
            }
        }
    }

    /**
     * draw the board, and everything on it.
     *
     * @param c
     * @param v
     */
    @Override
    public void draw(Canvas c, View v) {
        try {
            if (pause)
            {
                p.setColor(Color.WHITE);
                p.setTypeface(act.getGameFont());
                p.setTextSize(act.TS_BIG);
                drawCenteredText(c, "PAUSED", v.getHeight() / 2, p, 0);
                return;
            }

            c.drawRGB(0, 0, 0);  // color the screen black

            if (!gameover) {
                int color;
                // draw this level's board
                board.draw(c, crawler.getColumn(), boardpov, (frtime < superzapperOverTime));

                // draw crawler
                if (crawler.isVisible()){
                    if (frtime < deathPauseTime)
                        color = Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255));
                    else
                        color = board.getCrawlerColor();
                    ZMagic.drawObject(c, color, crawler.getCoords(), board, boardpov, crawlerzoffset);
                }

                if (boardpov < -Crawler.CHEIGHT) {
                    // pov shows game level board in the distance; add stars for fun
                    starpaint.setColor(Color.BLUE);
                    starpaint.setStrokeCap(Paint.Cap.ROUND);
                    for (int[] s : starList) {
                        int zdist = s[2] - boardpov;
                        xycoords = ZMagic.renderFromZ(s[0], s[1], zdist, board, xycoords);
                        if (levelnum >= Board.NUMSCREENS) // sparkle them as of last blue level
                            starpaint.setARGB(255, r.nextInt(255),r.nextInt(255),r.nextInt(255));
                        if (zdist < 200)
                            starpaint.setStrokeWidth(5);
                        else if (zdist < 500)
                            starpaint.setStrokeWidth(4);
                        else
                            starpaint.setStrokeWidth(2);

                        c.drawPoint(xycoords[0], xycoords[1], starpaint);
                    }
                }

                // draw crawler's missiles
                int missileColors[] = {Color.BLUE, Color.RED, Color.GREEN};
                for (Missile m : crawler.getMissiles()) {
                    if (m.isVisible()) {
                        ZMagic.drawObject(c, Color.YELLOW, m.getCoords(board), board, boardpov);
                        ZMagic.drawObject(c, missileColors[r.nextInt(missileColors.length)], m.getLayerCoords(board), board, boardpov);
                    }
                }

                // draw exes
                int podc = board.getExPodColor();
                int exc = board.getExColor();
                for (Ex ex : exes) {
                    if (ex.isVisible())
                        if (ex.isPod())
                            ZMagic.drawObject(c, podc, ex.getCoords(board), board, boardpov, crawlerzoffset);
                        else
                            ZMagic.drawObject(c, exc, ex.getCoords(board), board, boardpov, crawlerzoffset);
                    else {
                        // not visible but still in list means just killed
                        ZMagic.drawObject(c, Color.WHITE, ex.getDeathCoords(board), board, boardpov);
                    }
                }

                // draw enemy missiles
                for (Missile exm : enemymissiles) {
                    if (exm.isVisible()) {
                        ZMagic.drawObject(c, Color.GRAY, exm.getCoords(board), board, boardpov);
                        ZMagic.drawObject(c, Color.RED, exm.getLayerCoords(board), board, boardpov);
                    }
                    else if (exm.getZPos() > 0){
                        // not visible but still in list means just killed - draw explosion, snagged from Ex
                        ZMagic.drawObject(c, Color.WHITE, Ex.getDeathCoords(board, exm.getColumn(), exm.getZPos()), board, boardpov);
                    }
                }

                // draw spikes and spinnythings
                int spikecol = board.getSpikeColor();
                for (Spike s : spikes) {
                    if (s.isVisible()) {
                        List<int[]> spikeCoords = s.getCoords(board);
                        ZMagic.drawObject(c, spikecol, spikeCoords, board, boardpov);
                        spikeCoords.set(0, spikeCoords.get(1)); // add white dot at end
                        ZMagic.drawObject(c, Color.WHITE, spikeCoords, board, boardpov);
                        if (s.isSpinnerVisible()) {
                            List<int[]> spinCoords = s.getSpinnerCoords(board);
                            ZMagic.drawObject(c, spikecol, spinCoords, board, boardpov);
                        }
                    }
                }

                // other crudethings?  vims, for extra lives?
            }

            p.setColor(Color.GREEN);
            p.setTypeface(act.getGameFont());
            p.setTextSize(act.TS_BIG);
            if (scorertx < 0) {
                scorertx = p.measureText("100000");  // right align of score should just be enough for 6 digits
            }
//		g2d.drawString("SCORE:", 5, 15);
            if (score >= nextLife) {
                lives++;
                nextLife += EXTRA_LIFE_SCORE;
                act.playSound(Sound.EXLIFE);  // need a real sound here haha
            }
            String scorestr = Integer.toString(score);
            if (score < 100000)
                scorestr = scorestr + " ";
            c.drawText(scorestr, scorertx-p.measureText(scorestr), statstextheight2, p);
            if (score > hiscore)
                hiscore = score;
            p.setTextSize(act.TS_NORMAL);
            drawCenteredText(c, lblHigh + hiscore, statstextheight, p, 0);
            drawCenteredText(c, lblLevel+levelnum, statstextheight2, p, 0);
            c.drawText(lblLives+lives, width-rhstextoffset, statstextheight, p);

//            // onscreen dbg info
//            c.drawText(info, 50, 150, p);
//            c.drawText("fps:"+fps, 50, 100, p);

            // draw fire buttons
            p.setARGB(255, 170, 0, 0);
            c.drawRect(btnFire1Bounds, p);
            c.drawRect(btnFire2Bounds, p);
            p.setARGB(200, 100, 80, 80);
            c.drawRect(btnSuperzapBounds, p);
            p.setTextSize(act.TS_BIG);
            p.setColor(Color.BLACK);
            c.drawText(lblFire, getCenteredBtnX(lblFire, btnFire1Bounds), getCenteredBtnY(lblFire, btnFire1Bounds), p);
            c.drawText(lblFire, getCenteredBtnX(lblFire, btnFire2Bounds), getCenteredBtnY(lblFire, btnFire2Bounds), p);
            if (superzaps > 0)
                p.setColor(Color.RED);
            else
                p.setColor(Color.BLACK);
            c.drawText(lblZap, getCenteredBtnX(lblZap, btnSuperzapBounds), getCenteredBtnY(lblZap, btnSuperzapBounds), p);

            if (levelprep){
                p.setTextSize(act.TS_NORMAL);
                p.setColor(Color.BLUE);
                drawCenteredText(c, bonustxt, height * 1/3, p, 0);
                drawCenteredText(c, lblZapRechg, height * 2/3, p, 0);
            }

            if (levelcleared && levelnum == Board.FIRST_SPIKE_LEVEL) {
                p.setTextSize(act.TS_NORMAL);
                p.setColor(Color.WHITE);
                drawCenteredText(c, lblAvoidSpikes, height>>1, p, 0);
            }

            if (gameover) {
                p.setColor(Color.GREEN);
                p.setTextSize(act.TS_BIG);
                drawCenteredText(c, lblGameOver, v.getHeight() / 2, p, 0);
                p.setTextSize(act.TS_NORMAL);
                drawCenteredText(c, lblExit, v.getHeight() * 3/4, p, 0);
            }
        } catch (Exception e) {
            Log.d(act.LOG_ID, "draw", e);
        }
    }

    private int getCenteredBtnX(String s, Rect r) {
        return (int)(r.left + (r.right - r.left - p.measureText(s))/2);
    }
    private int getCenteredBtnY(String s, Rect r) {
        p.getTextBounds(s, 0, s.length()-1, scaledDst);
        return (int)(r.top + (r.bottom - r.top - scaledDst.height())/2);
    }

    /**
     * check for relevant in-game collisions
     */
    public void checkCollisions() {
        int cCol = crawler.getColumn();

        if (clearboard && levelcleared && !crawlerSpiked) {
            // check spike/player
            for (Spike s : spikes) {
                if (s.isVisible() && s.getColumn() == cCol && ((board.BOARD_DEPTH -s.getLength()) < crawlerzoffset)) {
                    act.stopSound(levchgStreamID); // if we got spiked, that means the levelclear sound is playing....stopSound it
                    playerDeath();
                    crawlerSpiked = true;
                    levelcleared = false;
                    break;
                }
            }
        }
        else {
            // check ex/player
            for (Ex ex : exes) {
                if (ex.isVisible() && (ex.getColumn() == cCol) && (ex.getZ() < Crawler.CHEIGHT)) {
                    //Log.d(act.LOG_ID, "ex got crawler:"+ex);
                    playerDeath();
                    ex.resetState();
                    return;  // we died...round is over, no need to continue checking any other collisions
                }
            }

            // check exes' missiles / player
            for (Missile exm : enemymissiles) {
                if (exm.isVisible()
                        && (exm.getColumn() == crawler.getColumn()) && (exm.getZPos() < Crawler.CHEIGHT))
                {
                    playerDeath();
                    exm.setVisible(false);
                    break;
                }
            }

        }

        // while not really a collision, the superzapper acts more or less like a
        // collision with all on-board non-pod exes, so it goes here.
        if (frtime < superzapperOverTime) {
            // loop through exes old school style, in case we have to add a spawn and don't
            // want a concurrentmodificationexception
            for (int ix = 0; ix < exes.size(); ix++){
                Ex ex = exes.get(ix);
                int kills = 0;
                if (ex.isVisible() && ex.getZ() < board.BOARD_DEPTH){
                    if (!ex.isPod()) {
                        // normal ex.  kill it.
                        ex.setVisible(false);
                        act.playSound(Sound.ENEMYDEATH);
                        kills++;
                    }
                    else {
                        // pod.  split it like normal.
                        ex.setPod(false);
                        exes.add(ex.spawn());
                        //don't count against kills
                    }
                    if (kills >= nZapKillsPerTick)
                        break;  // each tick, kill a few, for a slight stagger effect
                }
            }
        }

        // check player's missiles vs everything
        int ncols = board.getColumns().size();
        for (Missile m : crawler.getMissiles()) {
            // vs exes:
            Ex newEx = null; // if this missile hits a pod, we may spawn a new ex
            for (Ex ex : exes) {
                // check for normal missile/ex collision, also ex adjacent to crawler
                if (m.isVisible()
                        && (m.getColumn() == ex.getColumn() && (Math.abs(m.getZPos() - ex.getZ())< Ex.HEIGHT))
                        || ((m.getColumn() == crawler.getColumn())
                            && (m.getZPos() <= Missile.HEIGHT) // if we JUST fired the missile and ex is adjacent...
                            && (ex.getZ() <= 0)
                            && (!ex.isJumping())
                            && ( // adjacent fire depends on if the screen is continuous
                                (((ex.getColumn() +1) == crawler.getColumn()) || ((crawler.getColumn()+1) == ex.getColumn())) ||
                                ((((ex.getColumn() +1)%ncols == crawler.getColumn()) || ((crawler.getColumn()+1)%ncols == ex.getColumn())) && board.isContinuous())
                               )
                           )){
                    //Log.d(act.LOG_ID, "ex hit,  m:"+m.getZPos() + " "+m+" ex:"+ex);
                    if (ex.isPod()) {
                        // this ex is a pod; split into normal exes
                        score += Ex.PODSCOREVAL;
                        m.setVisible(false);
                        ex.setPod(false);
                        newEx = ex.spawn(true);
                        act.playSound(Sound.ENEMYDEATH);
                        break;
                    }
                    else {
                        // player hit ex
                        m.setVisible(false);
                        ex.setVisible(false);
                        score += Ex.SCOREVAL;
                        act.playSound(Sound.ENEMYDEATH);
                        break;
                    }
                }
            }
            if (newEx != null)
                exes.add(newEx);

            // vs exmissiles:
            if (m.isVisible()) {
                for (Missile exm : enemymissiles) {
                    if ((m.getColumn() == exm.getColumn())
                            && (exm.getZPos() - m.getZPos() < Missile.HEIGHT)) {
                        exm.setVisible(false);
                        m.setVisible(false);
                    }
                }
            }

            // vs spikes
            if (m.isVisible()) {
                for (Spike s : spikes) {
                    if (s.isVisible()
                            && m.getColumn() == s.getColumn()
                            && ((board.BOARD_DEPTH - s.getLength()) < m.getZPos())) {
                        s.impact();
                        m.setVisible(false);
                        score += Spike.SPIKE_SCORE;
                        if (s.isSpinnerVisible() &&
                                Math.abs(s.getSpinnerZ() - m.getZPos()) < Missile.HEIGHT * 2) {
                            s.setSpinnerVisible(false);
                            score += Spike.SPINNER_SCORE;
                        }
                    }
                }
            }
        }
    }


    /**
     * draw text onscreen, centered, at the inpassed height.  last param can be used to shift
     * text horizontally.
     *
     * @param c
     * @param msg
     * @param height
     * @param p
     * @param shift pixels to horizontally shift text.  normally 0
     */
    private void drawCenteredText(Canvas c, String msg, int height, Paint p, int shift) {
        p.getTextBounds(msg, 0, msg.length() - 1, scaledDst);
        c.drawText(msg, (c.getWidth() - scaledDst.width()) / 2 + shift, height, p);
    }

    VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    List<Integer> fireList = new LinkedList<Integer>();
    DisplayMetrics dm = new DisplayMetrics();
    @Override
    public boolean onTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (gameover) {
                    act.leaveGame();
                    return false;
                }
                int idx = e.getActionIndex();
                int pid = e.getPointerId(idx);
                if (e.getY(idx) > buttonLimitLine) {
                    // user pressing btns, not movement
                    //Log.d(act.LOG_ID, "BTN DOWN: "+e.getActionMasked()+", "+e.getX(idx)+","+e.getY(idx)+"; idx:"+idx+" pid:"+pid);
                    if (btnFire1Bounds.contains((int)e.getX(idx), (int)e.getY(idx)) || btnFire2Bounds.contains((int)e.getX(idx), (int)e.getY(idx))) {
                        // fire!
                        fireList.add(pid);
                        fireMissile = true;
                    }
                    else if (btnSuperzapBounds.contains((int)e.getX(idx), (int)e.getY(idx))) {
                        // superzap
                        fireSuperzapper = true;
                        return false; // no followups...though...we might get them anyway if this isn;t the original pointer.   this really seems overcomplicated.
                    }
                }
                else {
                    // crawler movement
                    mVelocityTracker.clear();
                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(e);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    pid = e.getPointerId(i);
                    if (!fireList.contains(pid)) {
                        // user is moving crawler
                        //Log.d(act.LOG_ID, "MOVE, dial; pid " + pid);
                        mVelocityTracker.addMovement(e);
                        float effx = e.getX(i) - halfwidth;
                        float effy = e.getY(i) - halfheight;

                        // come up with magnitude of portion of movement vector in same direction
                        // as unit vector normal to position vector relative to center.
                        // damn, that was a lot of words.
                        // basically, "control movement like on an iPod"
                        double posmag = Math.sqrt(effx*effx + effy*effy);
                        if (Math.abs(posmag) > 100) { // ignore if too close to center
                            double posdir = Math.atan2(effy, effx);
                            double posnormdir = posdir + Math.PI / 2;
                            mVelocityTracker.computeCurrentVelocity(1000);
                            float tvx = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pid);
                            float tvy = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pid);

                                if (tvx == 0)
                                    tvx = 0.001f;
                                double velmag = Math.sqrt(Math.pow(tvx, 2) + Math.pow(tvy, 2));
                                double veldir = Math.atan(tvy / tvx);
                                if (tvx < 0)
                                    veldir += Math.PI;
                                double alignedComponentFactor = Math.cos(veldir - posnormdir);
                                double fact = alignedComponentFactor * velmag / MAX_VEL;
                                crawler.accel(fact);
//                                info = String.format("acf:%.2f veld:%.2f\tposnd:%.2f\tvelmag:%d",
//                                        (float) alignedComponentFactor, (float) veldir, (float) posnormdir, (int) velmag);
                        }
/*
                        // alternative screen control - cheaper but feels more spastic
                        float xfact = -effy/halfheight;
                        float yfact = effx/halfwidth;
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float tvx = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pid);
                        float tvy = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pid);
                        double fact = (tvx*xfact + tvy*yfact)/(MAX_VEL/4);
                        crawler.accel(fact);
*/

                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (!fireList.contains(e.getPointerId(e.getActionIndex()))){
                    crawler.stop();
                    //info = "stopped by release";
                }
                else
                { // was a button press
                    int lidx = fireList.lastIndexOf(e.getPointerId(e.getActionIndex()));
                    if (lidx > -1) {
                        fireList.remove(lidx);
                    }
                    if (fireList.size() == 0)
                        fireMissile = false;
                }
                break;
        }

        return true;
    }
}
