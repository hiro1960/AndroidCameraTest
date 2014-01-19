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

	// �J����
	private Camera myCamera;
	
	// �J�����p�T�[�t�F�X�r���[
	private SurfaceView mySurfaceView;
	
	// �n���C�E�����x�Z���T�[ 
//    private SensorManager mySensor;
    
    // �n���C�E�����x�Z���T�[���
//    private static final int MATRIX_SIZE = 16;
    private static final int DIMENSION = 3;
//    private float[] magneticValues = new float[DIMENSION];
//    private float[] accelerometerValues = new float[DIMENSION];
    private float[] orientationValues = new float[DIMENSION];
    
    /**
     * �J�����̃C�x���g����
     */
    private PictureCallback mPictureListener = 
        new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                // �f�[�^�𐶐�����
                Bitmap tmp_bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                int width = tmp_bitmap.getWidth();
                int height = tmp_bitmap.getHeight();

                // �摜�f�[�^����]����
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
	
                // �M�������[�ɕۑ�
                String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.JAPAN).format(new Date()) + ".jpg";
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, name, null);		// /sdcard/DCIM/Camera�ȉ��ɕۑ�
                // �A����L�̂܂܂ł̓t�@�C���́h�^�C�g���h��name���g���邾���ŁA���ۂ̃t�@�C������method�̕��Ń��j�[�N�ɐ������Ă��܂��B
                // ���Ȃ݂ɑ�4�����́A�摜�̏ڍא����̕�����ɂȂ�炵���B
                // �t�ɂ����Ɠǂ݂��������t�@�C�����w��ł͂Ȃ����f�B�A��p�̃f�[�^�x�[�X�ցh�^�C�g���h���L�[�ɂ���getContentResolover().query()���\�b�h�ɂ��A�ǂ݂����Ă���炵���B
                
                Toast.makeText(SimpleCameraSampleActivity.this, "�ۑ����܂����B", Toast.LENGTH_SHORT).show();
                
                // �J�������ĊJ
                myCamera.startPreview();
            }
        };
 
    /**
     * �J�����p�T�[�t�F�C�X�̃C�x���g����
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
                myCamera = Camera.open(0); // 1:�O�ʃJ�������J���Aopen()�Ɉ���������ɂ�API=9�ȏ��v�������
                // 0 or �����Ȃ��F�w�ʃJ�����A���邢��HW�̃f�t�H���g���w�肷��
                // �A������Nexus7�͑O�ʃJ���������Ȃ����߂��A"0"���w�肷��K�v������B
                try {
                	// Preview���R�����g�A�E�g����ƁA�ʐ^���Ƃ�Ȃ��B
                	// ����surfaceChanged()�ɂĉ�ʃp�����[�^�̐ݒ�����Ă���̂ŁA�T�C�Y�A�I�[�g�t�H�[�J�X���������Ȃ��B
                    myCamera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                myCamera.stopPreview();
                
                Camera.Parameters parameters = myCamera.getParameters();
            
                // ��ʂ̌�����ݒ�
                boolean portrait = isPortrait();
                if (portrait) {
                    myCamera.setDisplayOrientation(90);
                } else {
                    myCamera.setDisplayOrientation(0);
                }
                
                // �Ή�����v���r���[�T�C�Y�E�ۑ��T�C�Y���擾����
                List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
                List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                
                // �ő�𑜓x��I��
                Size pictureSize = getOptimalPictureSize(pictureSizes);
                // �ʐ^�T�C�Y�ɋ߂��čł��傫���v���r���[�T�C�Y��I������
                Size previewSize = getOptimalPreviewSize(previewSizes, pictureSize.width, pictureSize.height);

                parameters.setPreviewSize(previewSize.width, previewSize.height);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
                
                // �T�[�t�F�C�X�̃T�C�Y���J�����̃v���r���[�T�C�Y�Ɠ����䗦�ɐݒ�
                android.view.ViewGroup.LayoutParams layoutParams = mySurfaceView.getLayoutParams();
                double preview_raito = (double)previewSize.width / (double)previewSize.height;
                if (width > height) {
                    // ����
                    int new_height = (int)(width / preview_raito);
                    if (new_height <= height) {
                        layoutParams.height = height;
                    } else {
                        int new_width = (int)(height * preview_raito); 
                        layoutParams.width = new_width;
                    }
                } else {
                    // �c��
                    int new_width = (int)(height / preview_raito);
                    if (new_width <= width) {
                        layoutParams.width = new_width;
                    } else {
                        int new_height = (int)(width * preview_raito); 
                        layoutParams.height = new_height;
                    }
                }
                mySurfaceView.setLayoutParams(layoutParams);

                // �p�����[�^��ݒ肵�ăJ�������ĊJ
                myCamera.setParameters(parameters);
                myCamera.startPreview();
            }
        };
         
    /**
     * �I�[�g�t�H�[�J�X�̃C�x���g����
     */
    private AutoFocusCallback mAutoFocusListener = 
        new AutoFocusCallback() {           
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
            }
        }; 	
       
	/**
	 * �Z���T�[����̃C�x���g����
	 */
    // ���������ȒP�ȃJ�����E�A�v���ɒn���C������x�Z���T�̒l�͕K�v�Ȃ��Bcomment out���Ă����Ȃ��B
    /*
	private SensorEventListener mSensorEventListener =
	    new SensorEventListener() {
	        
	        @Override
	        public void onSensorChanged(SensorEvent event) {
	            
	            // Nexus7�ł͏��SENSOR_STATUS_UNRELIABLE�ɂȂ�̂Ń`�F�b�N���Ȃ��@���@Nexus7�ȊO�ł͗L���ɂ��Ă����Ȃ��ƘI�o�����������Ȃ�I
	        	// �A���A����Nexus7�ł�SensorManager.SENSOR_STATUS_ACCURACY_HIGH=3��Ԃ��Ă���B
	        	// Android 4.x�ȍ~��SensorManager���̂��񐄏��ɂȂ����炵���A����ȍ~�Ɏ������s��ꂽ�V����Android�@��ł�UNRELIABLE��Ԃ��Ă���炵���B
	        	// Galaxy S2, XperiaAero����UNRELIABLE�Ƃ����񍐂����������A������Galaxy S2��SENSOR_STATUS_ACCURACY_HIGH�������B
	            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
	                return;

	            
	            switch (event.sensor.getType()) {
	                case Sensor.TYPE_MAGNETIC_FIELD:
	                    // �n���C�Z���T
	                    magneticValues = event.values.clone();
	                    break;
	                case Sensor.TYPE_ACCELEROMETER:
	                    // �����x�Z���T(Nexus7�ł̓T�|�[�g�O�H)
	                    accelerometerValues = event.values.clone();
	                    break;
	            }
                
	            if (magneticValues != null && accelerometerValues != null) {
                    float[] rotationMatrix = new float[MATRIX_SIZE];
                    float[] inclinationMatrix = new float[MATRIX_SIZE];
                    float[] remapedMatrix = new float[MATRIX_SIZE];
                    
                    // �����x�Z���T�ƒn���C�Z���^�����]�s����擾
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
		
		// �J�����v���r���[�̐ݒ�
        mySurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(mSurfaceListener);
        
        // �Z���T�[���擾����
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
        
        // �n���C�Z���T
//        mySensor.registerListener(mSensorEventListener,
//                                    mySensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
//                                    SensorManager.SENSOR_DELAY_UI);
//        
//        // �����x�Z���T
//        mySensor.registerListener(mSensorEventListener,
//                                    mySensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                                    SensorManager.SENSOR_DELAY_UI);
        
        // �{����onPause()�ŃJ�����E���\�[�X��������A�ēx�擾��������Preview�̐ݒ�����邽�߂�holder��scope���ɂȂ��̂łł��Ȃ��B
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
        // �{����onpause()�ŃJ�����E���\�[�X����U������i���̃A�v���̂��߂Ɂj�AonResume()�ōēx�擾���ׂ��Ǝv���B
        // ������onResume��Preview�̐ݒ肪�ł��Ȃ����A���ɉ�����Ȃ��Ă����Ȃ��݂����B
//        if (myCamera != null) {
//	        myCamera.stopPreview();
//	        myCamera.release();
//	        myCamera = null;
//        }
    }
	
    /**
     * ��ʂ̌������擾����(�c�Ȃ�true)
     */
    private boolean isPortrait() {
        return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }
    
    /**
     * �傫�����Ȃ��ʐ^�T�C�Y��I������(300����f�ȉ��A4:3�̂���)
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
     * �ʐ^�T�C�Y�ɋ߂��čł��傫���v���r���[�T�C�Y��I������
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
     * ���W�A���Ōv�������p�x���A��������x�ɕϊ�����
     */
    private int radianToDegree(float rad) {
        return (int)Math.floor(Math.toDegrees(rad));
    }
    
    /**
     * ��ʃ^�b�`���ŃI�[�g�t�H�[�J�X
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
     * �ʐ^�ۑ�
     */
    protected void buttonSave_onClick() {
        myCamera.takePicture(null, null, mPictureListener);
    }

}
