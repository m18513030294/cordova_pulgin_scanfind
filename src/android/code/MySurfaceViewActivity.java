package com.CarOCR;

import java.io.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Camera;
import android.hardware.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.ActivityInfo;
import io.ionic.starter.R;
public class MySurfaceViewActivity extends Activity {

    private final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private final long ANIMATION_DELAY = 80L;
    private final int POINT_SIZE = 6;
    private static final int CURRENT_POINT_OPACITY = 0xA0;

    private Bitmap resultBitmap;
    //    private Bitmap pictureBitmap = null;
    private Paint paint;
    private int maskColor;
    private int resultColor;
    private int frameColor;
    private int laserColor;
    private int resultPointColor;
    private int scannerAlpha;
    public android.hardware.Camera mCamera;
    private int m_nDisplayWidth = 0;
    private int m_nDisplayHeight = 0;
    private Button myIntent;//确定按钮
    private Button da;
    private Button btnCamera;
    private Button xiao;
    private SeekBar mSeekBar;
    private TextView lblRecogResult;
    private Preview mPreview;
    private Boolean bClick = false;
    public static String result = null;//车牌具体信息

    private int mMAX;//相机可以放大的最大值
    private android.hardware.Camera.Parameters mParameters;
    CarOCREngine myCarOCREngine;
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
          if(MySurfaceViewActivity.this!=null){
            //实现自动对焦
            mCamera.autoFocus(new android.hardware.Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, android.hardware.Camera camera) {
                    if (success) {
                        camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。

                    }
                }
            });
            handler.postDelayed(this, 4000);
          }else{
            handler.removeCallbacks(runnable);
          }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        myCarOCREngine = new CarOCREngine();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//设置横屏


        mPreview = new Preview(this);
        setContentView(mPreview);

        DrawOnTop mDraw = new DrawOnTop(this);
        addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WindowManager manager = (WindowManager) MySurfaceViewActivity.this.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        m_nDisplayWidth = display.getWidth();
        m_nDisplayHeight = display.getHeight();

        ResolutionSet._instance.setResolution(m_nDisplayWidth, m_nDisplayHeight);

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.main, null);
        this.addContentView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        ResolutionSet._instance.iterateChild(findViewById(R.id.rlBack));
        //加载控件
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        da = (Button) findViewById(R.id.da);
        myIntent = (Button) findViewById(R.id.myIntent);
        xiao = (Button) findViewById(R.id.xiao);
        btnCamera = (Button) findViewById(R.id.btnShutter);
        lblRecogResult = (TextView) findViewById(R.id.lblRecogResult);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //拍照点击事件
                if (!bClick) {
                    //车牌信息像是完成才可继续点击
                    bClick = true;
                    mPreview.captureImage();
                } else {
                    Toast.makeText(MySurfaceViewActivity.this, "正在解析车牌号请稍后！", Toast.LENGTH_LONG).show();
                }
            }
        });
        //放大
        da.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = mSeekBar.getProgress();
                if (progress >= mMAX - 7) {
                    addZoom(mMAX);
                } else {
                    addZoom(progress + 7);
                }
            }
        });
        //缩小
        xiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = mSeekBar.getProgress();
                if (progress <= 7) {
                    addZoom(0);
                } else {
                    addZoom(progress - 7);
                }
            }
        });
              initIonicActivity();
//        //获取车牌号完成跳转
//        myIntent.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(result != null) {//获取到车牌号
//                    //执行跳转
//                }
//
//            }
//        });
        scannerAlpha = 0;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;

        myCarOCREngine.init();
        loadAssets();


    }

  private void initIonicActivity() {
    myIntent.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(result != null) {//获取到车牌号
          //给ionic传车牌号
          Intent intent = new Intent("didShow");
          Bundle b = new Bundle();
          b.putString( "userdata","{ data: '"+result+"'}" );
          intent.putExtras(b);
          LocalBroadcastManager.getInstance(MySurfaceViewActivity.this).sendBroadcastSync(intent);
          handler.removeCallbacks(runnable);
          finish();
        }
      }
    });
  }

  private void loadAssets() {
        int size = 0;
        String assetNames = "mPcaLda.dic";
        byte[] pDicData;
        try {
            InputStream is = getAssets().open(assetNames);
            size = is.available();
            pDicData = new byte[size];
            is.read(pDicData);
            is.close();
            myCarOCREngine.loadDiction(pDicData, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class DrawOnTop extends View {
        public DrawOnTop(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int nRealX = 0;
            int nRealY = 0;
            //设置扫描框的大小
            nRealX = m_nDisplayWidth * 5 / 20;
            nRealY = m_nDisplayHeight * 5 / 14;

            Rect frame = new Rect(nRealX, nRealY, m_nDisplayWidth * 15 / 20, m_nDisplayHeight * 9 / 14);
            if (frame == null) {
                return;
            }
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            paint.setColor(resultBitmap != null ? resultColor : maskColor);
            canvas.drawRect(0, 0, width, frame.top, paint);
            canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
            canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
            canvas.drawRect(0, frame.bottom + 1, width, height, paint);

            if (resultBitmap != null) {
                paint.setAlpha(CURRENT_POINT_OPACITY);
                canvas.drawBitmap(resultBitmap, null, frame, paint);
            } else {
                paint.setColor(frameColor);
                canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
                canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
                canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
                canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

                paint.setColor(laserColor);
                paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
                scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
                int middle = frame.height() / 2 + frame.top;
                canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
            }

            postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE, frame.top - POINT_SIZE, frame.right + POINT_SIZE, frame.bottom + POINT_SIZE);

            super.onDraw(canvas);
        }
    }

    //自定义相机
    class Preview extends SurfaceView implements SurfaceHolder.Callback {
        public SurfaceHolder mHolder;

        android.hardware.Camera.PictureCallback rawCallback;
        android.hardware.Camera.ShutterCallback shutterCallback;
        android.hardware.Camera.PictureCallback jpegCallback;

        public Preview(Context context) {
            this(context, null);

            setDrawingCacheEnabled(false);

            rawCallback = new android.hardware.Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                }
            };

            shutterCallback = new android.hardware.Camera.ShutterCallback() {
                public void onShutter() {
                }
            };

            jpegCallback = new android.hardware.Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                    File tempDir = Environment.getExternalStorageDirectory();
                    tempDir = new File(tempDir.getAbsolutePath() + "/CarOCR/");
                    if (!tempDir.exists()) {
                        tempDir.mkdir();
                    }
                    String strPath = tempDir + "/CarNumber.jpg";

                    FileOutputStream outStream = null;
                    try {
                        outStream = new FileOutputStream(String.format(
                                strPath, System.currentTimeMillis()));
                        outStream.write(data);
                        outStream.close();

                        char resultString[] = new char[100];
                        myCarOCREngine.recogpageFile(strPath, resultString);
                        String resultStr = "";
                        int i;
                        for (i = 0; i < 100; i++) {
                            if (resultString[i] == 0x0)
                                break;
                            resultStr += resultString[i];
                        }
                        if(!resultStr.equals("")){
                            result = resultStr;
                        }
                        //解析车牌完毕 显示车牌信息

                        lblRecogResult.setText(resultStr);
                        bClick = false;//车牌没有显示点击无效


                        mCamera.stopPreview();
                        mCamera.startPreview();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    } finally {
                    }
                }
            };

            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public Preview(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public Preview(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public void captureImage() {
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCamera = android.hardware.Camera.open();
        //    mCamera.setDisplayOrientation(90);//竖屏添加代码

            mParameters = mCamera.getParameters();
            mMAX = mParameters.getMaxZoom();
            mSeekBar.setMax(mMAX);
//            android.hardware.Camera.Parameters param;
//            param = mCamera.getParameters();
//            //modify parameter
//            param.setPreviewFrameRate(20);
//            param.setPreviewSize(m_nDisplayWidth, m_nDisplayHeight);
//            mCamera.setParameters(param);

            //相机启动  开始自动聚焦
            handler.postDelayed(runnable, 2000);//每两秒执行一次runnable.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.stopPreview();
            mCamera = null;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            try {
                android.hardware.Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(w, h);
                mCamera.startPreview();
            } catch (Exception e) {
            }
        }

    }

    //相机放大缩小
    public void addZoom(int zoomValue) {
        // 是否支持变焦
        if (mMAX == 0)
            return;
        if (zoomValue <= mMAX - 1) {//目前是放大
            mParameters.setZoom(zoomValue);
            mCamera.setParameters(mParameters);
            mCamera.startSmoothZoom(zoomValue);
        } else {
            //TODO: toast
        }
        mSeekBar.setProgress(zoomValue);
    }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    handler.removeCallbacks(runnable);
  }
}
