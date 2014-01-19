package com.example.androidcameratest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SimpleCameraSampleActivity extends Activity {

	// カメラ
	private Camera myCamera;
	
	// カメラ用サーフェスビュー
	private SurfaceView mySurfaceView;
	
	// 地磁気・加速度センサー 
//    private SensorManager mySensor;
    
    // 地磁気・加速度センサー情報
//    private static final int MATRIX_SIZE = 16;
    private static final int DIMENSION = 3;
//    private float[] magneticValues = new float[DIMENSION];
//    private float[] accelerometerValues = new float[DIMENSION];
    private float[] orientationValues = new float[DIMENSION];
    
    /**
     * カメラのイベント処理
     */
    private PictureCallback mPictureListener = 
        new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                // データを生成する
                Bitmap tmp_bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                int width = tmp_bitmap.getWidth();
                int height = tmp_bitmap.getHeight();

                // 画像データを回転する
                int rad_y = radianToDegree(orientationValues[2]);
                Matrix matrix = new Matrix();
                if ((rad_y > -45 && rad_y <= 0) || (rad_y > 0 && rad_y <= 45)) {
                    matrix.setRotate(90);
                } else if (rad_y > 45 && rad_y <= 135) {
                    matrix.setRotate(180);
                } else if ((rad_y > 135 && rad_y <= 180) || (rad_y >= -180 && rad_y <= -135)) {
                    matrix.setRotate(-90);
                } else if (rad_y > -135 && rad_y <= -45) {
                    matrix.setRotate(0);
                }
                Bitmap bitmap = Bitmap.createBitmap(tmp_bitmap, 0, 0, width, height, matrix, true);
	
                // ギャラリーに保存
                String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.JAPAN).format(new Date()) + ".jpg";
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, name, null);		// /sdcard/DCIM/Camera以下に保存
                // 但し上記のままではファイルの”タイトル”にnameが使われるだけで、実際のファイル名はmethodの方でユニークに生成してしまう。
                // ちなみに第4引数は、画像の詳細説明の文字列になるらしい。
                // 逆にいうと読みだす時もファイル名指定ではなくメディア専用のデータベースへ”タイトル”をキーにしてgetContentResolover().query()メソッドにより、読みだしてくるらしい。
                
                Toast.makeText(SimpleCameraSampleActivity.this, "保存しました。", Toast.LENGTH_SHORT).show();
                
                // カメラを再開
                myCamera.startPreview();
            }
        };
 
    /**
     * カメラ用サーフェイスのイベント処理
     */
    private SurfaceHolder.Callback mSurfaceListener = 
        new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                myCamera.stopPreview();
                myCamera.release();
                myCamera = null;
            }
            
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                myCamera = Camera.open(0); // 1:前面カメラを開く、open()に引数を入れるにはAPI=9以上を要求される
                // 0 or 引数なし：背面カメラ、あるいはHWのデフォルトを指定する
                // 但し初代Nexus7は前面カメラしかないためか、"0"を指定する必要がある。
                try {
                	// Previewをコメントアウトすると、写真がとれない。
                	// 下のsurfaceChanged()にて画面パラメータの設定をしているので、サイズ、オートフォーカス等が動かない。
                    myCamera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                myCamera.stopPreview();
                
                Camera.Parameters parameters = myCamera.getParameters();
            
                // 画面の向きを設定
                boolean portrait = isPortrait();
                if (portrait) {
                    myCamera.setDisplayOrientation(90);
                } else {
                    myCamera.setDisplayOrientation(0);
                }
                
                // 対応するプレビューサイズ・保存サイズを取得する
                List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
                List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                
                // 最大解像度を選択
                Size pictureSize = getOptimalPictureSize(pictureSizes);
                // 写真サイズに近くて最も大きいプレビューサイズを選択する
                Size previewSize = getOptimalPreviewSize(previewSizes, pictureSize.width, pictureSize.height);

                parameters.setPreviewSize(previewSize.width, previewSize.height);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
                
                // サーフェイスのサイズをカメラのプレビューサイズと同じ比率に設定
                android.view.ViewGroup.LayoutParams layoutParams = mySurfaceView.getLayoutParams();
                double preview_raito = (double)previewSize.width / (double)previewSize.height;
                if (width > height) {
                    // 横長
                    int new_height = (int)(width / preview_raito);
                    if (new_height <= height) {
                        layoutParams.height = height;
                    } else {
                        int new_width = (int)(height * preview_raito); 
                        layoutParams.width = new_width;
                    }
                } else {
                    // 縦長
                    int new_width = (int)(height / preview_raito);
                    if (new_width <= width) {
                        layoutParams.width = new_width;
                    } else {
                        int new_height = (int)(width * preview_raito); 
                        layoutParams.height = new_height;
                    }
                }
                mySurfaceView.setLayoutParams(layoutParams);

                // パラメータを設定してカメラを再開
                myCamera.setParameters(parameters);
                myCamera.startPreview();
            }
        };
         
    /**
     * オートフォーカスのイベント処理
     */
    private AutoFocusCallback mAutoFocusListener = 
        new AutoFocusCallback() {           
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
            }
        }; 	
       
	/**
	 * センサー制御のイベント処理
	 */
    // そもそも簡単なカメラ・アプリに地磁気や加速度センサの値は必要ない。comment outしても問題ない。
    /*
	private SensorEventListener mSensorEventListener =
	    new SensorEventListener() {
	        
	        @Override
	        public void onSensorChanged(SensorEvent event) {
	            
	            // Nexus7では常にSENSOR_STATUS_UNRELIABLEになるのでチェックしない　→　Nexus7以外では有効にしておかないと露出がおかしくなる！
	        	// 但し、初代Nexus7ではSensorManager.SENSOR_STATUS_ACCURACY_HIGH=3を返してくる。
	        	// Android 4.x以降はSensorManager自体が非推奨になったらしく、それ以降に実装が行われた新しいAndroid機器ではUNRELIABLEを返してくるらしい。
	        	// Galaxy S2, XperiaAero等でUNRELIABLEという報告があったが、自分のGalaxy S2はSENSOR_STATUS_ACCURACY_HIGHだった。
	            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
	                return;

	            
	            switch (event.sensor.getType()) {
	                case Sensor.TYPE_MAGNETIC_FIELD:
	                    // 地磁気センサ
	                    magneticValues = event.values.clone();
	                    break;
	                case Sensor.TYPE_ACCELEROMETER:
	                    // 加速度センサ(Nexus7ではサポート外？)
	                    accelerometerValues = event.values.clone();
	                    break;
	            }
                
	            if (magneticValues != null && accelerometerValues != null) {
                    float[] rotationMatrix = new float[MATRIX_SIZE];
                    float[] inclinationMatrix = new float[MATRIX_SIZE];
                    float[] remapedMatrix = new float[MATRIX_SIZE];
                    
                    // 加速度センサと地磁気センタから回転行列を取得
                    SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerValues, magneticValues);
                    
                    SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix);
                    SensorManager.getOrientation(remapedMatrix, orientationValues);
                }
            }
            
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
	*/
                
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_camera_sample);
		
		// カメラプレビューの設定
        mySurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(mSurfaceListener);
        
        // センサーを取得する
//        mySensor = (SensorManager)getSystemService(SENSOR_SERVICE);

        Button buttonOK = (Button)this.findViewById(R.id.buttonSave);
        buttonOK.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                buttonSave_onClick();
            }
        });
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_camera_sample, menu);
		return true;
	}
	
	@Override
    public void onResume() {

        super.onResume();
        
        // 地磁気センサ
//        mySensor.registerListener(mSensorEventListener,
//                                    mySensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
//                                    SensorManager.SENSOR_DELAY_UI);
//        
//        // 加速度センサ
//        mySensor.registerListener(mSensorEventListener,
//                                    mySensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                                    SensorManager.SENSOR_DELAY_UI);
        
        // 本当はonPause()でカメラ・リソースを解放し、再度取得したいがPreviewの設定をするためのholderがscope内にないのでできない。
//        if (myCamera == null) {
//            myCamera = Camera.open(0); 
//            try {
//                myCamera.setPreviewDisplay(holder);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
	
	@Override
    public void onPause() {
        super.onPause();
//        mySensor.unregisterListener(mSensorEventListener);
        // 本当はonpause()でカメラ・リソースを一旦解放し（他のアプリのために）、onResume()で再度取得すべきと思う。
        // しかしonResumeでPreviewの設定ができないし、特に解放しなくても問題ないみたい。
//        if (myCamera != null) {
//	        myCamera.stopPreview();
//	        myCamera.release();
//	        myCamera = null;
//        }
    }
	
    /**
     * 画面の向きを取得する(縦ならtrue)
     */
    private boolean isPortrait() {
        return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }
    
    /**
     * 大きすぎない写真サイズを選択する(300万画素以下、4:3のもの)
     */
    private Size getOptimalPictureSize(List<Size> sizes) {

        double targetRatio = (double)4 / 3;
        
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            int gasosu = size.width * size.height;
            if (gasosu < (300 * 10000) && Math.abs(ratio - targetRatio) < 0.1) {
                return size;
            }
        }
        
        return sizes.get(0);
    }
    
    /**
     * 写真サイズに近くて最も大きいプレビューサイズを選択する
     */
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {

        if (sizes == null) {
            return null;
        }

        double targetRatio = (double) w / h;
        
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) < 0.1) {
                return size;
            }
        }

        return sizes.get(0);
    }

    /**
     * ラジアンで計測した角度を、相当する度に変換する
     */
    private int radianToDegree(float rad) {
        return (int)Math.floor(Math.toDegrees(rad));
    }
    
    /**
     * 画面タッチ時でオートフォーカス
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Camera.Parameters params = myCamera.getParameters();
            if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_FIXED)) {
                myCamera.autoFocus(mAutoFocusListener);
            }
        }
        return true;
    }
    
    /**
     * 写真保存
     */
    protected void buttonSave_onClick() {
        myCamera.takePicture(null, null, mPictureListener);
    }

}
