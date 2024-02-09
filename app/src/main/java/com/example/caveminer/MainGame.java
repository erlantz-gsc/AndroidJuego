package com.example.caveminer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainGame extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener{

    //Añadir Highscore
    //Muscia de fondo
    //Escena GameOver
    
    private int corx, cory;
    private Lienzo fondo;
    private List<Shape> shapes;
    private Handler handler;
    private Runnable runnable;
    private boolean isGameOver;
    private int score;
    private Button retryButton;

    private Context _context;
    private MediaPlayer mp;
    private MediaPlayer mp2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        corx = 0;
        cory = 0;
        ConstraintLayout layout1 = findViewById(R.id.layout1);
        fondo = new Lienzo(this);
        fondo.setOnTouchListener(this);
        layout1.addView(fondo);

        shapes = new ArrayList<>();
        handler = new Handler();
        isGameOver = false;
        score = 0;


        startGame();
    }

    private void showGameOver() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void startGame() {
        new Thread(() -> {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            while (!isGameOver) {
                mainHandler.post(() -> {
                    if (!isGameOver) {
                        generateShape();
                        moveShapes();
                        removeOffScreenShapes();
                        checkCollision();
                        fondo.invalidate();
                    } else {
                        showGameOver();
                    }
                });

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(isGameOver)
            {
                showGameOver();
            }
        }).start();
    }

    private void generateShape() {
        Random random = new Random();
        int width = Math.max(1, Math.abs(fondo.getWidth()));
        int x = random.nextInt(width);
        int shapeType = random.nextInt(50);

        if (shapeType >= 1 && shapeType < 11) {
            shapes.add(new Triangle(x, 0));
        } else if(shapeType == 0){
            shapes.add(new Coin(x, 0));
        }
    }

    private void moveShapes() {
        for (Shape shape : shapes) {
            shape.move();
        }
    }

    private void removeOffScreenShapes() {
        List<Shape> shapesToRemove = new ArrayList<>();
        for (Shape shape : shapes) {
            if (shape.getY() > fondo.getHeight()) {
                shapesToRemove.add(shape);
            }
        }
        shapes.removeAll(shapesToRemove);
    }

    private void checkCollision() {
        for (Shape shape : shapes) {
            if (shape instanceof Coin && shape.intersects(corx, cory)) {
                // Increase score when a coin is collected
                score++;
                shapes.remove(shape);
                if(mp == null)
                    mp = MediaPlayer.create(this,R.raw.coin);
                else
                    mp.seekTo(0);
                mp.start();

                break;
            } else if (shape instanceof Triangle && shape.intersects(corx, cory)) {
                // Game over if there is a collision with a triangle
                if(mp2 == null)
                    mp2 = MediaPlayer.create(this,R.raw.death);
                else
                    mp2.seekTo(0);
                mp2.start();
                isGameOver = true;
                break;
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(isGameOver){
            return false;
        }

        corx = (int) event.getX();
        cory = (int) event.getY();
        fondo.invalidate();
        return true;
    }

    @Override
    public void onClick(View v) {

    }

    class Lienzo extends View {

        private Drawable playerDrawable;
        private Drawable backGroundDrawable;
        private Paint scorePaint;

        public Lienzo(Context context) {
            super(context);
            _context = context;  // Move this line to the beginning
            playerDrawable = _context.getResources().getDrawable(R.drawable.helmet);
            backGroundDrawable = _context.getResources().getDrawable(R.drawable.background);
            scorePaint = new Paint();
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(50); // Adjust text size as needed
            scorePaint.setTextAlign(Paint.Align.RIGHT);
        }

        protected void onDraw(Canvas canvas) {

            int ancho = canvas.getWidth();
            int alto = canvas.getHeight();

            backGroundDrawable.setBounds(0,0,ancho,alto);
            backGroundDrawable.draw(canvas);

            if (corx == 0 || cory == 0) {
                corx = ancho / 2;
                cory = alto - 100;
            }

            int playerWidth = playerDrawable.getIntrinsicWidth();
            int playerHeight = playerDrawable.getIntrinsicHeight();

            float scale = 0.05f;

            int width = (int) (playerWidth * scale);
            int height = (int) (playerHeight * scale);

            playerDrawable.setBounds(corx - width/2, cory -height/2 , corx + width/2, cory + height/2);

            playerDrawable.draw(canvas);

            for (Shape shape : shapes) {
                shape.draw(canvas);
            }

            String scoreText = "Coins: " + score;
            canvas.drawText(scoreText, ancho - 10, 60, scorePaint);
        }
    }

    abstract class Shape {
        protected int x;
        protected int y;
        protected int speed;

        public Shape(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public abstract void move();

        public abstract void draw(Canvas canvas);

        public abstract boolean intersects(int cx, int cy);
    }

    class Triangle extends Shape {

        private Drawable triangleDrawable = _context.getResources().getDrawable(R.drawable.spike);
        public Triangle(int x, int y) {
            super(x, y);
            this.speed = 10; // Falling speed for triangles
        }

        @Override
        public void move() {
            y += speed;
        }

        @Override
        public void draw(Canvas canvas) {
            int spikeWidth = triangleDrawable.getIntrinsicWidth();
            int spikeHeight = triangleDrawable.getIntrinsicHeight();

            float scale = 0.1f;

            int width = (int) (spikeWidth * scale);
            int height = (int) (spikeHeight * scale);

            triangleDrawable.setBounds(x - width/2, y-height/2, x + width/2, y + height/2);

            triangleDrawable.draw(canvas);
        }

        @Override
        public boolean intersects(int cx, int cy) {
            return cx > x && cx < x + 50 && cy > y && cy < y + 50;
        }
    }

    public class Coin extends Shape {
        private Drawable coinDrawable;
        private int imageIndex = 0;

        public Coin( int x, int y) {
            super(x, y);
            this.speed = 8; // Falling speed for coins
            this.coinDrawable = _context.getResources().getDrawable(R.drawable.coin1); // Use your first image
            startAnimation();
        }

        private void startAnimation() {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updateImage();
                    move();

                    // Repeat the animation
                    handler.postDelayed(this, 16); // Adjust the delay based on your desired frame rate
                }
            });
        }

        private void updateImage() {
            imageIndex++;
            if (imageIndex > 6) {
                imageIndex = 0;
            }

            int resId = _context.getResources().getIdentifier("coin" + (imageIndex + 1), "drawable", _context.getPackageName());
            coinDrawable = _context.getResources().getDrawable(resId);
        }

        @Override
        public void move() {
            y += speed;
        }

        @Override
        public void draw(Canvas canvas) {
            coinDrawable.setBounds(x - 20, y - 20, x + 20, y + 20);
            coinDrawable.draw(canvas);
        }

        @Override
        public boolean intersects(int cx, int cy) {
            float distance = (float) Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
            return distance < 20;
        }
    }
}