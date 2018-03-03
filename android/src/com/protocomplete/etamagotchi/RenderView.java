package com.protocomplete.etamagotchi;

import android.content.Context;
import android.content.res.AssetManager;
import android.annotation.TargetApi;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Build;
import android.util.Log;
import android.util.AttributeSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

public class RenderView extends SurfaceView implements Runnable, OnTouchListener {
    // Process & Activity stuff
    Thread viewThread = null;
    SurfaceHolder holder;
    boolean isThreadActive = true;

    // Graphic loading stuff
    final double TILE_WIDTH = 480/10, TILE_HEIGHT = 384/6, NUM_COLS = 10, NUM_ROWS = 6;
    Bitmap tiles = null, attackImg = null, hostImg = null;
    Bitmap monsters;
    Bitmap bg;
    Sprite monsterSprite = null;
    Sprite bgSprite = null;
    int tileID = (int) Math.floor(Math.random()*((int)NUM_COLS*NUM_ROWS));
    boolean isSpriteLoaded = false;
    Paint paint;

    // P2P
    final String PORT = "9999"; // if hosting, this is the port to use
    BattleThread battleThread = null;
    boolean isInBattle = false;

    // Monster Stuff
    int HP = 1;
    int maxHP = 6;
    int maxDamage = ThreadLocalRandom.current().nextInt(1, 2 + 1); // min = 1, max = 2
    int xpos = 0; // move around inbetween the frame
    int wins = 0;
    int losses = 0;

    public RenderView(Context context) {
      super(context);
      init(context);
    }
    public RenderView(Context context, AttributeSet attributes) {
      super(context, attributes);
      init(context);
    }

    public RenderView(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init(context);
    }

    private Sprite getMonsterSpriteFromID(Bitmap source, int tileID) {
      // tiles are 16 x 16 pixels long
      int row = (int) Math.floor((double)tileID / (double)NUM_COLS);
      int col = tileID % (int) NUM_COLS;

      return new Sprite(this, source, col*(int)TILE_WIDTH, row*(int)TILE_HEIGHT, (int)TILE_WIDTH, (int)TILE_HEIGHT);
      //return new Sprite(this, source);
    }

    public void feedMonster() {
      if(++HP > maxHP) { HP = maxHP; }
    }

    public void init(Context context) {
      monsters = getBitmapFromAsset(context.getAssets(), "monsters.png");
      bg = getBitmapFromAsset(context.getAssets(), "bg.jpg");

      holder = getHolder();

      paint = new Paint();
      paint.setColor(Color.BLACK);
      paint.setStrokeWidth(2);
      paint.setStyle(Paint.Style.STROKE);

      this.setOnTouchListener(this);
    }

    public static Bitmap getBitmapFromAsset(AssetManager assetManager, String filePath) {
      InputStream istr;
      Bitmap bitmap = null;

      try {
          istr = assetManager.open(filePath);
          bitmap = BitmapFactory.decodeStream(istr);
      } catch (IOException e) {
          // TODO: handle exception
          e.printStackTrace();
      }

      return bitmap;
    }

    @Override
    public void run() {
      while(isThreadActive) {
        if(holder.getSurface().isValid()) {

          if(!isSpriteLoaded && monsters != null && bg !=null) {
            monsterSprite = getMonsterSpriteFromID(monsters, tileID);
            bgSprite = new Sprite(this, bg);

            isSpriteLoaded = true;
          }
          Canvas canvas = holder.lockCanvas();
          onDraw(canvas);
          holder.unlockCanvasAndPost(canvas);
        } else {
          Log.d("Android: ", "holder not valid!");
        }
      }
    }

    @Override
    public void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      // draw white bg
      paint.setColor(Color.WHITE);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

      // draw bg
      if(bg != null){
        paint.setAlpha(25); // barely visible
        bgSprite.onDraw(canvas, paint);
        paint.setAlpha(255); // fully opaque
      }

      //if(!isInBattle) {
        int xPos = getWidth();
        // make it move around I guess
        if(HP > 1) {
          xPos += (int)(Math.sin(System.currentTimeMillis()*0.0002)*100.0);
        }

        if(isSpriteLoaded) {
          //monsterSprite.setPosX(0);
          //monsterSprite.setPosY(0);
          monsterSprite.onDraw(canvas);
        }
      //}

      if(HP > 1) {
        paint.setColor(Color.BLACK);
      } else {
        paint.setColor(Color.RED);
      }

      for(int i = 0; i < maxHP; i++) {
        if(i < HP) {
          paint.setStyle(Paint.Style.FILL);
        } else {
          // empty blocks
          paint.setStyle(Paint.Style.STROKE);
        }
        int left = 10*(i+1)+(40*i);
        int right = left + 30;
        int top = 30;
        int bottom = top + 30;
        canvas.drawRect(left, top, right, bottom, paint);
      }
    }

    public void onPause() {
      isThreadActive = false;

      try {
        viewThread.join();
      } catch(InterruptedException e) {
        e.printStackTrace();
      }

      viewThread = null;
    }

    public void onResume() {
      isThreadActive = true;
      viewThread = new Thread(this);
      viewThread.start();
    }

    public boolean onTouch(View v, MotionEvent me) {
      if(monsterSprite == null) {
        return false;
      }

      monsterSprite.setPosX((int)me.getX());
      monsterSprite.setPosY((int)me.getY());

      switch(me.getAction()) {
        case MotionEvent.ACTION_DOWN:
          break;

        case MotionEvent.ACTION_UP:
          break;

        case MotionEvent.ACTION_MOVE:
          break;

        default:
            break;
      }

      return true;
    }
}