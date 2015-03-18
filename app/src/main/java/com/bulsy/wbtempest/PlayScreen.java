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
    private static final int SPEED_LEV_ADVANCE = 300;  // speed at which we fly to the next board after clearing a level
    private static final int GAME_OVER_BOARDSPEED = 2300;  // speed at which the game recedes when player loses
    static final long ONESEC_NANOS = 1000000000L;
    private static final long DEATH_PAUSE_NANOS = ONESEC_NANOS*2/3;  // time to pause on crawler death
    private static final long SUPERZAPPER_NANOS = ONESEC_NANOS/3; // how long does a superzap last?
    private static final long FIRE_COOLDOWN_NANOS = ONESEC_NANOS / 30; // max firing rate, per sec
    private static final int NUM_STARS = 100; // number of stars when entering a level
    private static final int MAX_VEL = 2000; // spin-controlling at this pace is "fast"
    private static final int INIT_LEVELPREP_POV = Board.BOARD_DEPTH * 2;  // start level-intro zoom from this distance

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
    private boolean crawlerSpiked;
    private List<int[]> starList = null;  // will be created and populated first time around
    int[] xycoords = new int[2];
    float[] starpts = new float[NUM_STARS*2];
    Paint starpaint = new Paint();
    private int height, width;
    private String info= "";
    private int levchgStreamID = 0;
    boolean hasSpike[] = new boolean[Board.MAX_COLS]; // most columns any screen will have


    static final int TS_NORMAL = 30; // normal text size
    static final int TS_BIG = 70; // large text size

    private Paint p=new Paint();
    private long frtime = 0;
    private float fps;
    private Rect scaledDst = new Rect();
    private MainActivity act = null;
    private boolean gamestarting = true;  // flag to init level from within update, so that we have screen coords

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

        startGame();
    }

    /**
     * Initialize the game.
     */
    public void startGame()
    {
        lives=START_LIVES;
        gameover=false;
        score = 0;
        levelnum = 1;
        gamestarting = true;
    }

    /**
     * initialize a level for play
     */
    private void initLevel(View v){
        board.init(v, levelnum);
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
            exes.get(0).resetZ(board.BOARD_DEPTH *3/2);
        else { // reposition all the exes in the center spawning area
            for (Ex ex : exes )
                ex.resetZ(r.nextInt(board.BOARD_DEPTH *2 + board.BOARD_DEPTH * board.getNumExes()/5) + board.BOARD_DEPTH *5/4);
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
            buttonLimitLine = v.getHeight() *4/5;
            int spacer = 10;
            int x_sep1 = v.getWidth() *2/5;
            int x_sep2 = v.getWidth() *3/5;
            btnFire1Bounds = new Rect(spacer, buttonLimitLine, x_sep1 - spacer/2, v.getHeight()-spacer);
            btnSuperzapBounds = new Rect(x_sep1 +spacer/2, buttonLimitLine, x_sep2 - spacer/2, v.getHeight()-spacer);
            btnFire2Bounds = new Rect(x_sep2+spacer/2, buttonLimitLine, v.getWidth()-spacer, v.getHeight()-spacer);
            width = v.getWidth();
            height = v.getHeight();
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
                starcoords[2] = -r.nextInt(INIT_LEVELPREP_POV / 2); // concentrate stars close-ish to playing board
                starList.add(starcoords);
            }
        }

        // if player died, they don't get to move crawler or superzap
        if (!isPlayerDead()){
            if (fireSuperzapper && superzaps > 0){
                superzapperOverTime = frtime + SUPERZAPPER_NANOS;
                fireSuperzapper = false;
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
                boardpov += elapsedTime * SPEED_LEV_ADVANCE;
                if (crawlerzoffset < board.BOARD_DEPTH)
                    crawlerzoffset+= elapsedTime * SPEED_LEV_ADVANCE;

                if (boardpov > board.BOARD_DEPTH * 5/4)
                {
                    levelnum++;
                    initLevel(v);
                    boardpov = -INIT_LEVELPREP_POV;
                    levelprep=true;
                }
            }
            else if (lives > 0)
            {   // player died but not out of lives.
                // pause, then suck crawler down and restart level
                if (frtime >= deathPauseTime) {
                    crawlerzoffset += elapsedTime * SPEED_LEV_ADVANCE * 2;  // twice as fast as level adv
                    if (crawlerzoffset > board.BOARD_DEPTH)
                        replayLevel();
                }
            }
            else
            { // player died and game is over.  advance everything along z away from player.
                if (boardpov > -board.BOARD_DEPTH *5)
                    boardpov -= elapsedTime * GAME_OVER_BOARDSPEED;
                else
                    gameover=true;
            }
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
                        ex.move(v.getWidth(), crawler.getColumn(), elapsedTime);
                        if (ex.getZ() <= 0) {
                            if (ex.isPod()) {
                                // we're at the top of the board; split the pod
                                exes.add(ex.spawn());
                                ex.setPod(false);
                            }
                        }
                        if ((ex.getZ() < board.BOARD_DEPTH)
                                && (r.nextInt(10000) < board.getExFireBPS()))
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
                                    && (r.nextInt(10000) < board.getExFireBPS()/4))
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
                p.setTextSize(TS_BIG);
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
                    color = Color.YELLOW;
                    if (frtime < deathPauseTime)
                        color = Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255));
                    ZMagic.drawObject(c, color, crawler.getCoords(), board, boardpov, crawlerzoffset);
                }

                if (boardpov < -Crawler.CHEIGHT) {
                    // pov shows game level board in the distance; add stars for fun
                    starpaint.setColor(Color.BLUE);
                    starpaint.setStrokeCap(Paint.Cap.ROUND);
                    starpaint.setStrokeWidth(2);
                    for (int[] s : starList) {
                        xycoords = ZMagic.renderFromZ(s[0], s[1], s[2]-boardpov, board);
                        if (levelnum > Board.NUMSCREENS)
                            starpaint.setARGB(255, r.nextInt(255),r.nextInt(255),r.nextInt(255));
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
                for (Ex ex : exes) {
                    if (ex.isVisible())
                        if (ex.isPod())
                            ZMagic.drawObject(c, Color.MAGENTA, ex.getCoords(board), board, boardpov, crawlerzoffset);
                        else
                            ZMagic.drawObject(c, Color.RED, ex.getCoords(board), board, boardpov, crawlerzoffset);
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
                for (Spike s : spikes) {
                    if (s.isVisible()) {
                        List<int[]> spikeCoords = s.getCoords(board);
                        ZMagic.drawObject(c, Color.GREEN, spikeCoords, board, boardpov);
                        spikeCoords.set(0, spikeCoords.get(1)); // add white dot at end
                        ZMagic.drawObject(c, Color.WHITE, spikeCoords, board, boardpov);
                        if (s.isSpinnerVisible()) {
                            List<int[]> spinCoords = s.getSpinnerCoords(board);
                            ZMagic.drawObject(c, Color.GREEN, spinCoords, board, boardpov);
                        }
                    }
                }

                // other crudethings?  vims, for extra lives?
            }

            p.setColor(Color.GREEN);
            p.setTypeface(act.getGameFont());
            p.setTextSize(TS_BIG);
//		g2d.drawString("SCORE:", 5, 15);
            c.drawText(Integer.toString(score), 100, 55, p);
            if (score > hiscore)
                hiscore = score;
            p.setTextSize(TS_NORMAL);
            drawCenteredText(c, "HIGH: " + hiscore, 30, p, 0);
            drawCenteredText(c, "LEVEL: "+levelnum, 55, p, 0);
            c.drawText("LIVES:", v.getWidth()-130, 30, p);
            c.drawText(Integer.toString(lives), v.getWidth()-40, 30, p);

//            // onscreen dbg info
//            c.drawText(info, 50, 150, p);
            c.drawText("fps:"+fps, 50, 100, p);

            // draw fire buttons
            p.setARGB(255, 170, 0, 0);
            c.drawRect(btnFire1Bounds, p);
            c.drawRect(btnFire2Bounds, p);
            p.setARGB(200, 100, 80, 80);
            c.drawRect(btnSuperzapBounds, p);
            p.setTextSize(TS_BIG);
            String txt = "FIRE";
            p.setColor(Color.BLACK);
            c.drawText(txt, getCenteredBtnX(txt, btnFire1Bounds), getCenteredBtnY(txt, btnFire1Bounds), p);
            c.drawText(txt, getCenteredBtnX(txt, btnFire2Bounds), getCenteredBtnY(txt, btnFire2Bounds), p);
            txt = "ZAP";
            if (superzaps > 0)
                p.setColor(Color.RED);
            else
                p.setColor(Color.BLACK);
            c.drawText(txt, getCenteredBtnX(txt, btnSuperzapBounds), getCenteredBtnY(txt, btnSuperzapBounds), p);

            if (levelprep){
                p.setTextSize(TS_NORMAL);
                p.setColor(Color.BLUE);
                drawCenteredText(c, "SUPERZAPPER RECHARGE", v.getHeight() *2/3, p, 0);
            }

            if (levelcleared && levelnum == Board.FIRST_SPIKE_LEVEL) {
                p.setTextSize(TS_NORMAL);
                p.setColor(Color.WHITE);
                drawCenteredText(c, "AVOID  SPIKES", v.getHeight()/2, p, 0);
            }


            if (gameover) {
                p.setColor(Color.GREEN);
                p.setTextSize(TS_BIG);
                drawCenteredText(c, "GAME OVER", v.getHeight() / 2, p, 0);
                p.setTextSize(TS_NORMAL);
                drawCenteredText(c, "TOUCH TO EXIT", v.getHeight() * 3/4, p, 0);

                try {
                    BufferedWriter f = new BufferedWriter(new FileWriter(act.getFilesDir() + HISCORE_FILENAME));
                    f.write(Integer.toString(hiscore)+"\n");
                    f.close();
                } catch (Exception e) { // if we can't write the hi score file...oh well.
                    Log.d(MainActivity.LOG_ID, "WriteHiScore", e);
                }
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
            int perTickKill = (int)(exes.size()/(10 * SUPERZAPPER_NANOS/ONESEC_NANOS)); // assume worst frame rate 10
            for (Ex ex : exes){
                int kills = 0;
                if (ex.isVisible() && ex.getZ() < board.BOARD_DEPTH && !ex.isPod()) {
                    ex.setVisible(false);
                    act.playSound(Sound.ENEMYDEATH);
                    kills++;
                    if (kills >= perTickKill)
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
                        && (((ex.getColumn() +1)%ncols == crawler.getColumn())
                        || ((crawler.getColumn()+1)%ncols == ex.getColumn())))){
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

    VelocityTracker mVelocityTracker = null;
    List<Integer> mvmtPrtList = new LinkedList<Integer>();
    List<Integer> fireList = new LinkedList<Integer>();
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
                    mvmtPrtList.add(pid);
                    //Log.d(act.LOG_ID, "DIAL DOWN: "+e.getActionMasked()+", "+e.getX(idx)+","+e.getY(idx)+"; idx:"+idx+" pid:"+pid);
                    if (mVelocityTracker == null) {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }

                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(e);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    pid = e.getPointerId(i);
                    if (mvmtPrtList.contains(pid)) {
                        //Log.d(act.LOG_ID, "MOVE, dial; pid " + pid);
                        mVelocityTracker.addMovement(e);

                        // come up with magnitude of portion of movement vector in same direction
                        // as unit vector normal to position vector relative to center.
                        // damn, that was a lot of words.
                        // basically, "control movement like on an iPod"
                        float effx = e.getX(i) - width / 2;
                        float effy = e.getY(i) - height / 2;

                        double posmag = Math.sqrt(Math.pow(effx, 2) + Math.pow(effy, 2));
                        if (Math.abs(posmag) > 100) { // ignore if too close to center
                            double posdir = Math.atan2(effy, effx);
                            double posnormdir = posdir + Math.PI / 2;
                            mVelocityTracker.computeCurrentVelocity(1000);
                            float tvx = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pid);
                            float tvy = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pid);
                            double velmag = Math.sqrt(Math.pow(tvx, 2) + Math.pow(tvy, 2));
                            if (tvx != 0) { // skip those brief instants where computation gets ruined
                                double veldir = Math.atan(tvy / tvx);
                                if (tvx < 0)
                                    veldir += Math.PI;
                                double alignedComponentFactor = Math.cos(veldir - posnormdir);
                                double fact = alignedComponentFactor * velmag / MAX_VEL;
                                crawler.accel(alignedComponentFactor * velmag / MAX_VEL);
                                info = String.format("acf:%.2f veld:%.2f\tposnd:%.2f\tvelmag:%d",
                                        (float) alignedComponentFactor, (float) veldir, (float) posnormdir, (int) velmag);
                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int lidx = mvmtPrtList.lastIndexOf(e.getPointerId(e.getActionIndex()));
                if (lidx > -1) {
                    mvmtPrtList.remove(lidx);
                    crawler.stop();
                    info = "stopped by release";
                    mVelocityTracker.recycle();
                }
                else
                { // was a button press
                    lidx = fireList.lastIndexOf(e.getPointerId(e.getActionIndex()));
                    if (lidx > -1) {
                        fireList.remove(lidx);
                        fireMissile = false;
                    }
                }
                break;

        }

        return true;
    }

}
