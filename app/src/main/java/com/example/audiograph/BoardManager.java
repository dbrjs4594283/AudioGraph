package com.example.audiograph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ShortBuffer;

class BoardManager extends SurfaceView implements SurfaceHolder.Callback{
    int ScreenWidth,ScreenHeight,BoardWidth,BoardHeight;
    int BoardStartX,BoardStartY,BoardMiddleWidth,
            BoardMiddleHeight,BoardEndX,BoardEndY;

    Canvas canvas;

    double RatioX,RatioY;
    int TimeDiv,MaxHeight,SamplingRate,VRange;
    //AudioRecord로 부터 받은 데이터의 수
    int DataLength;
    ShortBuffer Buffer,tempBuffer,ReadBuffer;
    SurfaceHolder mHolder;

    RecordManager recordManager;
    boolean isData;

    public BoardManager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardManager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public BoardManager(Context context) {
        super(context);
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder=holder;
        getScreenInfo();
        canvas=holder.lockCanvas();
        drawBoard();
        holder.unlockCanvasAndPost(canvas);
        recordManager=new RecordManager(this);
        recordManager.setBoardManager(this);
        recordManager.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    void init(){
        SurfaceHolder mHolder=getHolder();
        mHolder.addCallback(this);

        SamplingRate=44100;
        Buffer=ShortBuffer.allocate(SamplingRate);
        tempBuffer=ShortBuffer.allocate(SamplingRate);

        //화면에 표시할 시간
        TimeDiv=1000;
        //표시할 최대값
        MaxHeight=32767;
    }

    void start(){
        recordManager.onStart();
    }

    void Pause(){
        recordManager.onPause();
    }

    void Stop(){
        recordManager.onStop();
    }

    void getScreenInfo(){
        //화면의 크기를 얻어옴
        ScreenWidth=getWidth();
        ScreenHeight=getHeight();

        //박스의 크기를 화면의 90%크기로 설정
        BoardWidth=(int)(ScreenWidth*0.9);
        BoardHeight=(int)(ScreenHeight*0.9);

        //박스의 그릴 지점 설정
        BoardStartX=(ScreenWidth-BoardWidth)/2;
        BoardStartY=(ScreenHeight-BoardHeight)/2;
        BoardEndX=BoardStartX+BoardWidth;
        BoardEndY=BoardStartY+BoardHeight+2;

        //박스의 중앙선
        BoardMiddleHeight=BoardHeight/2;
        BoardMiddleWidth=BoardWidth/2;

        //표시 배율
        RatioY=(BoardHeight-2)/((6-VRange)*MaxHeight*2.0f);
        RatioX=(BoardWidth-2)*1000/(double)(TimeDiv);

        isData=false;
    }

    void drawBoard(){
        Paint paint=new Paint();

        //배경화면 힌색으로
        paint.setColor(Color.WHITE);
        canvas.drawRect(1,1,ScreenWidth,ScreenHeight,paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

        //보드 외곽선
        canvas.drawRect(BoardStartX-1,BoardStartY,BoardEndX+2,BoardEndY,paint);

        //보드 십자선
        paint.setStrokeWidth(1);
        canvas.drawLine(BoardStartX+BoardMiddleWidth,BoardStartY,
                BoardStartX+BoardMiddleWidth,BoardEndY,paint);
        canvas.drawLine(BoardStartX,BoardStartY+BoardMiddleHeight
                ,BoardEndX,BoardStartY+BoardMiddleHeight,paint);

    }

    void setData(ShortBuffer readBuffer,int dataLength){
        this.ReadBuffer=readBuffer;
        this.DataLength=dataLength;

        canvas=mHolder.lockCanvas();
        drawBoard();
        drawData();
        mHolder.unlockCanvasAndPost(canvas);
        isData=true;
    }

    void drawData(){
        double data, Stime, Ttime;
        int x, y, i;
        //AudioRecord로 부터 입력된 데이터가 있을 때만 그림
        if(isData==true){
            Paint paint=new Paint();
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(5);

            tempBuffer.position(0);
            tempBuffer.put(Buffer.array(),0,SamplingRate-1);//버퍼 복사
            Buffer.position(0);
            //앞부분을 readBuffer의 내용으로 채움
            Buffer.put(ReadBuffer.array(),0,DataLength-1);
            //readBuffer의 내용 뒷부분을 원래의 값으로 채움-> 쉬프트
            Buffer.put(tempBuffer.array(),0,SamplingRate-DataLength-1);

            //샘플링 한주기의 시간을 구함
            Stime=1.0f/SamplingRate;
            //그려야할 전체 시간(1초) - TimeDiv/1000
            Ttime=TimeDiv/(Stime*1000);

            //데이터를 읽어와서 화면에 출력
            Buffer.position(0);
            for (i=0;i<(int)Ttime;i++) {
                //그려질 x 좌표 구하기
                x = (int) ((i + 1) * Stime * RatioX) + BoardStartX + 1;

                //y값 구하고 역상만들기 및 아래로 내리기
                data = -RatioY * Buffer.get(i);
                y = BoardMiddleHeight + (int) data + BoardStartY;

                //그리기
                canvas.drawPoint(x, y, paint);
            }
            isData=false;
        }
    }
}