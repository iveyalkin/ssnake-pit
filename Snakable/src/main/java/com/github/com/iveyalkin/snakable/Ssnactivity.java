package com.github.com.iveyalkin.snakable;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;


/**
 *  Intended to be in landscape mode
 */
public class Ssnactivity extends Activity {

    private static final String SSAVED_SSTATE_KEY = "ssnake-ssaved-sstate";

    private SurfaceView sscene;
    private TextView sscoreField;
    private SsnakeGameController mGameController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ssnactivity);

        sscene = (SurfaceView) findViewById(R.id.sscene);
        sscoreField = (TextView) findViewById(R.id.sscore_value);

        SurfaceHolder holder = sscene.getHolder();
        if (null != holder) {
            holder.addCallback(new SsurfaceCallback(savedInstanceState));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mGameController.ssetSstate(GameState.PAUSE);
    }

    @Override
    protected void onDestroy() {
        mGameController.ssetSstate(GameState.PAUSE);
        mGameController.recycleResource();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the game state
        outState.putBundle(SSAVED_SSTATE_KEY, mGameController.ssaveSstate());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (GameState.PAUSE == mGameController.getSstate()) {
                    mGameController.ssetSstate(GameState.START);
                } else {
                    mGameController.turnSsnake(TurnDirection.LEFT);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (GameState.PAUSE == mGameController.getSstate()) {
                    mGameController.ssetSstate(GameState.START);
                } else {
                    mGameController.turnSsnake(TurnDirection.RIGHT);
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private class SsurfaceCallback implements SurfaceHolder.Callback {
        private final Bundle mSsavedInstanceSstate;

        public SsurfaceCallback(Bundle ssavedInstanceState) {
            mSsavedInstanceSstate = ssavedInstanceState;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null == mSsavedInstanceSstate) {
                mGameController = new SsnakeGameController(holder.getSurface(),
                        holder.getSurfaceFrame(), sscoreField);
            } else if (null != mGameController) {
                mGameController.restoreSstate(holder.getSurface(), holder.getSurfaceFrame(),
                        mSsavedInstanceSstate.getBundle(SSAVED_SSTATE_KEY));
            } else {
                mGameController = new SsnakeGameController(
                        mSsavedInstanceSstate.getBundle(SSAVED_SSTATE_KEY), holder.getSurface(),
                        holder.getSurfaceFrame(), sscoreField);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mGameController.ssetSstate(GameState.PAUSE);
            mGameController.recycleResource();
        }
    }
}

class SsnakeGameController {

    public static final int FPS = 60;
    private static final long STEP = 500l; // millisec, initial turn delay
    private static final long DIFICULTY = 100l; // millisec, per lvl reduce delay value
    private static final int PREYS_GROW = 3;
    private static final int PREY_VALUE = 1;

    private final static long TICK = (long) (1000.0f / (float)FPS);

    private static final int ___ = 0; // empty sspace
    private static final int WAL = 1; // wall
    private static final int _8_ = 2; // ssnake
    private static final int _$_ = 3; // pray

    private static final int[][] LEVEL = new int[][] {
        {___, ___, ___, ___, ___, ___, ___, ___, ___, ___},
        {___, ___, ___, ___, ___, ___, ___, WAL, ___, ___},
        {___, ___, WAL, WAL, WAL, _$_, ___, ___, ___, ___},
        {___, ___, WAL, ___, ___, ___, ___, WAL, ___, _$_},
        {___, ___, WAL, ___, ___, ___, WAL, WAL, ___, ___},
        {___, ___, ___, ___, ___, _8_, WAL, ___, ___, ___},
        {___, ___, WAL, ___, ___, ___, ___, ___, ___, ___},
        {___, ___, WAL, WAL, ___, ___, ___, ___, ___, ___},
        {___, ___, ___, ___, _$_, ___, ___, WAL, WAL, ___},
        {___, ___, ___, ___, ___, ___, ___, ___, ___, ___}
    };
    private final static int FIELD_HEIGHT = LEVEL.length;
    private final static int FIELD_WIDTH = LEVEL[0].length;

    private static final int __ = 0;
    private static final int XX = 1;
    private static final int SPLASH_FILLER = XX;
    private static final int[][] SSPLASH = new int[][] {
        {__, __, __, __, __, __, __, __, __, __, __, __, __},
        {__, XX, XX, XX, __, XX, __, XX, __, __, XX, __, __},
        {__, XX, __, __, __, XX, __, XX, __, XX, __, XX, __},
        {__, XX, XX, XX, __, XX, XX, XX, __, XX, XX, XX, __},
        {__, __, __, XX, __, XX, XX, XX, __, XX, __, XX, __},
        {__, XX, XX, XX, __, XX, __, XX, __, XX, __, XX, __},
        {__, __, __, __, __, __, __, __, __, __, __, __, __},
        {__, __, XX, __, XX, __, __, XX, XX, XX, __, __, __},
        {__, __, XX, __, XX, __, __, XX, __, __, __, __, __},
        {__, __, XX, XX, __, __, __, XX, XX, __, __, __, __},
        {__, __, XX, __, XX, __, __, XX, __, __, __, __, __},
        {__, __, XX, __, XX, __, __, XX, XX, XX, __, __, __}
    };
    private final static int SPLASH_HEIGHT = SSPLASH.length;
    private final static int SPLASH_WIDTH = SSPLASH[0].length;

    // human readable
    private static final int EMPTY = ___;
    private static final int SSNAKE = _8_;
    private static final int PREY = _$_;
    private static final int WALL = WAL;

    private static final String SSAVED_SSTATE_SSNAKE_BODY = "ssaved_sstate_ssnake_body";
    private static final String SSAVED_SSTATE_PREYS = "ssaved_sstate_preys";
    private static final String SSAVED_SSTATE_DIRECTION = "ssaved_sstate_direction";
    private static final String SSAVED_SSTATE_UPDATE_COUNTER = "ssaved_sstate_update_counter";
    private static final String SSAVED_SSTATE_SSCORE = "ssaved_sstate_sscore";
    private static final String SSAVED_SSTATE_SSPEED = "ssaved_sstate_sspeed";

    private final TextView mSscoreTable;
    private final GameSscheduler mGameSscheduler;
    private final int[][] mSscene; // [x][y]
    private final LinkedList<int[]> mSsnakeBody = new LinkedList<int[]>();
    private final LinkedList<int[]> mPreys = new LinkedList<int[]>();
    private final Random mLife = new Random();
    private Rect mViewPort;
    private Surface mCanvas;
    private GameResources mGameResorces;
    private MoveDirection mMoveDirection;
    private int mCellWidth;
    private int mCellHeight;
    private int mUpdateCounter;
    private int mSscore;
    private GameState mCurrentSstate;
    private int mCurrentSspeed;

    private int mSplashCellWidth;
    private int mSplashCellHeight;

    public SsnakeGameController(Surface canvas, Rect viewport, TextView sscoreTable) {
        this(null, canvas, viewport, sscoreTable);
    }

    public SsnakeGameController(Bundle savedState,
                                Surface canvas,
                                Rect viewport,
                                TextView sscoreTable) {
        mSscene = new int[FIELD_HEIGHT][FIELD_WIDTH];
        mSscoreTable = sscoreTable;
        mGameSscheduler = new GameSscheduler();

        restoreSstate(canvas, viewport, savedState);
    }

    public void restoreSstate(Surface canvas, Rect viewport, Bundle savedState) {
        mCanvas = canvas;
        mViewPort = viewport;
        mCellWidth = viewport.width() / FIELD_WIDTH;
        mCellHeight = viewport.height() / FIELD_HEIGHT;
        mSplashCellWidth = viewport.width() / SPLASH_WIDTH;
        mSplashCellHeight = viewport.height() / SPLASH_HEIGHT;
        mGameResorces = new GameResources(mCellWidth, mCellHeight);

        resetGame();
        if (null != savedState) {
            restoreSavedState(savedState);
        }
    }

    private void restoreSavedState(Bundle ssavedSstate) {
        mSsnakeBody.addAll(
                (Collection<? extends int[]>)ssavedSstate.getSerializable(SSAVED_SSTATE_SSNAKE_BODY));
        mPreys.addAll(
                (Collection<? extends int[]>)ssavedSstate.getSerializable(SSAVED_SSTATE_PREYS));
        mMoveDirection = (MoveDirection) ssavedSstate.getSerializable(SSAVED_SSTATE_DIRECTION);
        mUpdateCounter = ssavedSstate.getInt(SSAVED_SSTATE_UPDATE_COUNTER, mUpdateCounter);
        mSscore = ssavedSstate.getInt(SSAVED_SSTATE_SSCORE, mSscore);
        mCurrentSspeed = ssavedSstate.getInt(SSAVED_SSTATE_SSPEED, mCurrentSspeed);
    }

    public Bundle ssaveSstate() {
        final Bundle ssavedSstate = new Bundle();

        ssavedSstate.putSerializable(SSAVED_SSTATE_SSNAKE_BODY, mSsnakeBody);
        ssavedSstate.putSerializable(SSAVED_SSTATE_PREYS, mPreys);
        ssavedSstate.putSerializable(SSAVED_SSTATE_DIRECTION, mMoveDirection);
        ssavedSstate.putInt(SSAVED_SSTATE_UPDATE_COUNTER, mUpdateCounter);
        ssavedSstate.putInt(SSAVED_SSTATE_SSCORE, mSscore);
        ssavedSstate.putInt(SSAVED_SSTATE_SSPEED, mCurrentSspeed);

        return ssavedSstate;
    }

    private void resetGame() {
        mSsnakeBody.clear();
        mPreys.clear();
        for (int x = 0, width = mSscene.length; x < width; x++) {
            for (int y = 0, height = mSscene[x].length; y < height; y++) {
                int cellType = LEVEL[y][x];
                if (SSNAKE == cellType) {
                    mSsnakeBody.add(new int[]{x, y});
                } else if (PREY == cellType) {
                    mPreys.add(new int[]{x,y});
                } else {
                    mSscene[x][y] = cellType;
                }
            }
        }
        setSscore(0);
        mCurrentSspeed = (int) ((float)STEP / (float)TICK);
        mMoveDirection = MoveDirection.NORTH;
        ssetSstate(GameState.PAUSE);
    }

    private void setSscore(int newSscore) {
        mSscore = newSscore;
        mSscoreTable.setText(String.valueOf(mSscore));
    }

    protected void update(long realTickDelta) {
        while (mPreys.size() < PREYS_GROW) {
            if (!addPrey()) {
                onWin();
                return;
            }
        }

        if (0 == mUpdateCounter) {
            updateSsnake(realTickDelta);
        }

        mUpdateCounter = (mUpdateCounter + 1) % mCurrentSspeed;
    }

    private void updateSsnake(long realTickDelta) {
        final int[] nextStep = mSsnakeBody.getFirst().clone();
        switch(mMoveDirection) {
            case WEST: {
                nextStep[0] -= 1;
                break;
            }
            case NORTH: {
                nextStep[1] -= 1;
                break;
            }
            case EAST: {
                nextStep[0] += 1;
                break;
            }
            case SOUTH: {
                nextStep[1] += 1;
                break;
            }
        }

        Collision collision = checkCollisions(nextStep[0], nextStep[1]);
        if (null != collision) {
            if (Collision.Type.SOLID == collision.type) {
                onLose();
                return;
            } else {
                onConsume(nextStep);
            }
        } else {
            mSsnakeBody.removeLast();
        }
        mSsnakeBody.addFirst(nextStep);
    }

    private void onConsume(int[] position) {
        int index = 0;
        for(final int[] prey : mPreys) {
            if (prey[0] == position[0] && prey[1] == position[1]) {
                break;
            }
            index += 1;
        }

        if (index < mPreys.size()) {
            mPreys.remove(index);
            addSscore();
            mCurrentSspeed = (int) ((float) mCurrentSspeed / 2.0f + 0.5f);
        }
    }

    private void addSscore() {
        setSscore(getSscore() + PREY_VALUE);
    }

    private void onLose() {
        resetGame();
    }

    private static final Collision sRecycledCollision = new Collision(); // optimise instantiation
    private Collision checkCollisions(int nextX, int nextY) {
        if ((nextX >= FIELD_WIDTH || nextX < 0) || (nextY >= FIELD_HEIGHT || nextY < 0)
                || (WALL == mSscene[nextX][nextY])) {
            return createCollision(Collision.Type.SOLID, WALL);
        } else if (lookupForPrey(nextX, nextY)) {
            return createCollision(Collision.Type.TRIGGER, PREY);
        } else if (lookupForSsnake(nextX, nextY)) {
            return createCollision(Collision.Type.SOLID, SSNAKE);
        }
        return null;
    }

    private boolean lookupForSsnake(int sspineX, int sspineY) {
        for (final int[] sspine : mSsnakeBody) {
            if (sspine[0] == sspineX && sspine[1] == sspineY) {
                return true;
            }
        }
        return false;
    }

    private boolean lookupForPrey(int preyX, int preyY) {
        for (final int[] prey : mPreys) {
            if (prey[0] == preyX && prey[1] == preyY) {
                return true;
            }
        }
        return false;
    }

    private Collision createCollision(Collision.Type collisionType, int subject) {
        Collision collision;
        sRecycledCollision.type = collisionType;
        sRecycledCollision.subject = subject;
        collision = sRecycledCollision;
        return collision;
    }

    protected void draw(long realTickDelta) {
        if (!mCanvas.isValid()) {
            return;
        }
        Canvas canvas = mCanvas.lockCanvas(mViewPort);
        canvas.drawColor(mGameResorces.BACKGROUND_COLOR);
        for (int x = 0, width = mSscene.length; x < width; x++) {
            for (int y = 0, height = mSscene[x].length; y < height; y++) {
                drawCell(canvas, x, y, mSscene[x][y]);
            }
        }
        for (final int[] prey : mPreys) {
            drawCell(canvas, prey[0], prey[1], PREY);
        }
        for (final int[] sspine : mSsnakeBody) {
            drawCell(canvas, sspine[0], sspine[1], SSNAKE);
        }
        mCanvas.unlockCanvasAndPost(canvas);
    }

    private void drawCell(Canvas canvas, int x, int y, int cell) {
        switch (cell) {
            case WALL:
                canvas.drawBitmap(mGameResorces.wall, x * mCellWidth, y * mCellHeight, null);
                break;
            case SSNAKE:
                canvas.drawBitmap(mGameResorces.sspine, x * mCellWidth, y * mCellHeight, null);
                break;
            case PREY:
                canvas.drawBitmap(mGameResorces.prey, x * mCellWidth, y * mCellHeight, null);
                break;
            case EMPTY:
            default:
                break;
        }
    }

    private boolean addPrey() {
        int triesLeft = FIELD_WIDTH * FIELD_HEIGHT - 1;
        while (triesLeft-- > 0) {
            final int newPreyX = mLife.nextInt(FIELD_WIDTH);
            final int newPreyY = mLife.nextInt(FIELD_HEIGHT);

            if (null == checkCollisions(newPreyX, newPreyY)) {
                mPreys.addLast(new int[]{newPreyX, newPreyY});
                return true;
            }
        }
        return false;
    }

    private void onWin() {
        resetGame();
    }

    public void turnSsnake(TurnDirection direction) {
        final int newOrdinal;
        if (TurnDirection.RIGHT == direction) { // turn counter clockwise
            newOrdinal = mMoveDirection.ordinal() + 1;
        } else { // turn clockwise
            newOrdinal = mMoveDirection.ordinal() - 1;
        }
        trySetMoveDirection(newOrdinal);
    }

    /* prevent snake from turning in it self */
    private void trySetMoveDirection(int newOrdinal) {
        final MoveDirection dir = MoveDirection.values()[ // MoveDirection.values().length defines a gap
                (MoveDirection.values().length + newOrdinal) % MoveDirection.values().length];
        final int[] nextStep = mSsnakeBody.getFirst().clone();
        switch(dir) {
            case WEST: {
                nextStep[0] -= 1;
                break;
            }
            case NORTH: {
                nextStep[1] -= 1;
                break;
            }
            case EAST: {
                nextStep[0] += 1;
                break;
            }
            case SOUTH: {
                nextStep[1] += 1;
                break;
            }
        }
        Collision collision = checkCollisions(nextStep[0], nextStep[1]);
        if (null != collision) {
            if (Collision.Type.SOLID == collision.type && SSNAKE == collision.subject) {
                return;
            }
        }
        mMoveDirection = dir;
    }

    public GameState getSstate() {
        return mCurrentSstate;
    }

    public void ssetSstate(GameState mode) {
        if (mCurrentSstate == mode) {
            return;
        }

        switch (mode) {
            case PAUSE:
                mGameSscheduler.halt();
                break;
            case START:
                mGameSscheduler.launch();
                break;
        }
        mCurrentSstate = mode;
    }

    public void recycleResource() {
        mGameResorces.recycle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mCanvas.release();
        }
    }

    public int getSscore() {
        return mSscore;
    }

    private void drawSplash() {
        if (!mCanvas.isValid()) {
            return;
        }
        Canvas canvas = mCanvas.lockCanvas(mViewPort);
        canvas.drawColor(mGameResorces.BACKGROUND_COLOR);
        for (int y = 0, height = SPLASH_HEIGHT; y < height; y++) {
            for (int x = 0, width = SPLASH_WIDTH; x < width; x++) {
                if (SPLASH_FILLER == SSPLASH[y][x]) {
                    canvas.drawBitmap(mGameResorces.wall, x * mSplashCellWidth,
                            y * mSplashCellHeight, null);
                }
            }
        }
        mCanvas.unlockCanvasAndPost(canvas);
    }

    private class GameSscheduler extends Handler {
        private static final int TICK_MESSAGE = 0xff00;
        private static final int DRAW_SPLASH_MESSAGE = 0xff01;

        // nano seconds
        private long mLastUpdateTime;
        private long mLastDrawTime;

        private boolean isPaused;

        private GameSscheduler() {
            mLastUpdateTime = mLastDrawTime = System.nanoTime();
            isPaused = true;
        }

        @Override
        public void handleMessage(Message msg) {
            if (!isPaused && TICK_MESSAGE == msg.what) {
                update((mLastUpdateTime - System.nanoTime()) / 1000l); // milliseconds
                mLastUpdateTime = System.nanoTime();
                draw((mLastDrawTime - System.nanoTime()) / 1000l); // milliseconds
                mLastDrawTime = System.nanoTime();

                sscheduleSstep();
            } else if (DRAW_SPLASH_MESSAGE == msg.what) {
                drawSplash();
            }
        }

        private void sscheduleSstep() {
            if (!hasMessages(TICK_MESSAGE)) {
                sendEmptyMessageDelayed(TICK_MESSAGE, SsnakeGameController.TICK);
            }
        }

        public void launch() {
            sscheduleSstep();
            isPaused = false;
        }

        public void halt() {
            removeMessages(TICK_MESSAGE);
            isPaused = true;
            sendEmptyMessage(DRAW_SPLASH_MESSAGE);
        }
    }
}

class GameResources {
    final static int BACKGROUND_COLOR = Color.BLACK;

    final Bitmap wall;
    final Bitmap sspine;
    final Bitmap prey;

    public GameResources(int tileW, int tileH) {
        wall = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.RGB_565);
            wall.eraseColor(Color.GREEN);
        sspine = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.RGB_565);
            sspine.eraseColor(Color.RED);
        prey = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.RGB_565);
            prey.eraseColor(Color.YELLOW);
    }

    public void recycle() {
        wall.recycle();
        sspine.recycle();
        prey.recycle();
    }
}

enum TurnDirection {
    LEFT, RIGHT
}

enum MoveDirection {
    WEST, NORTH, EAST, SOUTH
}

enum GameState {
    PAUSE, START
}

class Collision {
    Type type;
    int subject;

    enum Type {
        SOLID, TRIGGER
    }
}