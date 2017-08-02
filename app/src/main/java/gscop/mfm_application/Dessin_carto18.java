package gscop.mfm_application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Alison.rl on 30/07/2017.
 */

public class Dessin_carto18 extends View implements View.OnClickListener {

    private Paint paint ;

    private static final String TAG = "gscop.mfm_application";

    private Boolean  animStop = false;

    private ArrayList<ArrayList<Float>> completedTabsX = new ArrayList<>();
    private ArrayList<ArrayList<Float>> completedTabsY = new ArrayList<>();
    private HashMap<Integer,Float> animCurrentPos = new HashMap();

    private ArrayList<Long> durationTimes = new ArrayList<>();
    private ArrayList<Long> eventDownTimes = new ArrayList<>();
    private ArrayList<Long> eventUpTimes = new ArrayList<>();
    private ArrayList<Long> animTimesInit = new ArrayList<>();

    private ArrayList<Path> paths = new ArrayList<>();
    private HashMap<Integer,Path> animPaths = new HashMap<>();

    private HashMap<Integer,Boolean> animRunning = new HashMap<>();
    private HashMap<Integer,PathMeasure> animPathsMeasure = new HashMap<>();
    private HashMap<Integer,Long> animLastUpdated = new HashMap<>();


    public Dessin_carto18(Context context) {
        super(context);
        init();
    }

    public Dessin_carto18(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Dessin_carto18(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);

        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        for(int j = 0; j < completedTabsX.size(); j++ ){
            animRunning.put(j,false);
            animPathsMeasure.put(j,null);
        }
        for(int j = 0; j < eventDownTimes.size(); j++){
            durationTimes.add(eventUpTimes.get(j) - eventDownTimes.get(j));
            Log.d(TAG," DURATION : " + j + durationTimes.get(j));
        }
        startAnimation();
    }

    public void startAnimation(){
        setPaths();
        animRunning.put(0,true);
        invalidate();
    }

    public void setPaths(){
        Log.d(TAG, " NUMBERS OF PATHS : " + completedTabsX.size());
        for(int j = 0; j < completedTabsX.size(); j++) {
            Log.d(TAG, " TABLEAU : " + completedTabsX.get(j).size());
            Path path = new Path();
            for (int i = 0; i < completedTabsX.get(j).size(); i++) {
                Log.d(TAG, " I : " + i);
                if (i == 0) {
                    path.moveTo(completedTabsX.get(j).get(i), completedTabsY.get(j).get(i));
                } else {
                    path.lineTo(completedTabsX.get(j).get(i), completedTabsY.get(j).get(i));
                }
            }
            paths.add(path);
        }
        Log.d(TAG, " PATHS LOADED ! ");
    }


    @Override
    protected void onDraw(Canvas canvas) {

        if(animStop){
            for(int j = 0; j < paths.size(); j++){
                canvas.drawPath(paths.get(j),paint);
            }
        } else {
            for (int j = 0; j < paths.size(); j++) {
                Log.d(TAG, " J : " + j);
                isTime(j);
                if (animRunning.get(j)) {drawAnim(canvas, j);}
                else {drawStatic(canvas, j);}
            }
            super.onDraw(canvas);
            invalidate();
        }
    }

    public void isTime(int j){
        if (j != 0) {
            if(animTimesInit.size() == j) {
                long time = System.currentTimeMillis() - animTimesInit.get(j - 1);
                long timeBetPath = eventDownTimes.get(j) - eventDownTimes.get(j - 1);

                Log.d(TAG, " TIME : " + time);
                Log.d(TAG, " TIME BET : " + timeBetPath);


                if (time > timeBetPath) {
                    animRunning.put(j, true);
                }
            }
        }
    }

    public void drawAnim(Canvas canvas, int j){

        if(animPathsMeasure.get(j) == null){
            // Set the first point of the Path
            PathMeasure animPathMesure = new PathMeasure(paths.get(j),false);
            animPathMesure.nextContour();
            Path animPath = new Path();
            animCurrentPos.put(j,0.0f);
            animLastUpdated.put(j,System.currentTimeMillis());
            animTimesInit.add(System.currentTimeMillis());
            animPathsMeasure.put(j,animPathMesure);
            animPaths.put(j,animPath);
            Log.d(TAG, " INIT DRAW PATH ");
            Log.d(TAG, " PATH LENGTH: " + animPathMesure.getLength());

        } else {
            // Get time since last frame
            long now = System.currentTimeMillis();
            long timeSinceLast = now - animLastUpdated.get(j);
            Log.d(TAG, " TIME SINCE LAST : " + timeSinceLast);

            float animLength = (animPathsMeasure.get(j).getLength()*(timeSinceLast))/durationTimes.get(j);
            float newPos =  animCurrentPos.get(j) + animLength;
            boolean moveTo = (animCurrentPos.get(j) == 0.0f);
            Log.d(TAG, " LENGTH PASS " + animLength);
            Log.d(TAG, " NEW POS " + newPos);

            if(newPos < animPathsMeasure.get(j).getLength()){
                animPathsMeasure.get(j).getSegment(animCurrentPos.get(j), newPos, animPaths.get(j), moveTo);
                animCurrentPos.put(j,newPos);
                animLastUpdated.put(j,now);
            } else {
                animCurrentPos.put(j,(newPos - animLength));
                animPathsMeasure.get(j).getSegment(animCurrentPos.get(j), animPathsMeasure.get(j).getLength(), animPaths.get(j), moveTo);
                animCurrentPos.put(j,0.0f);
                Log.d(TAG, " DONE! ");
                Log.d(TAG, " ERROR TIME : " + ((System.currentTimeMillis() - animTimesInit.get(j)) - durationTimes.get(j)));
                animRunning.put(j, false);
                if(j == (paths.size()-1)){
                    Log.d(TAG, " ANIM STOP ");
                    animStop = true;
                }
            }

            // Draw Path
            canvas.drawPath(animPaths.get(j),paint);

        }

    }

    public void drawStatic(Canvas canvas, int j){
        if(animPathsMeasure.get(j) != null){
            canvas.drawPath(paths.get(j),paint);
        }
    }

    public void getTabX(ArrayList<ArrayList<Float>> tabsX) {
        completedTabsX = tabsX;
    }

    public void getTabY(ArrayList<ArrayList<Float>> tabsY) {
        completedTabsY = tabsY;
    }

    public void getEventUpTimes(ArrayList<Long> event_up_times) {eventUpTimes = event_up_times;}

    public void getEventDownTimes(ArrayList<Long> event_times) {
        eventDownTimes = event_times;
    }

}