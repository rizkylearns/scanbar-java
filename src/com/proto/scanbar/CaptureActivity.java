package com.proto.scanbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.proto.scanbar.zxing.CaptureActivityHandler;
import com.proto.scanbar.zxing.camera.CameraManager;

import java.io.IOException;
import java.util.*;

public class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private boolean hasSurface;

    private CaptureActivityHandler handler;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;

    private Result savedResultToShow, lastResult;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.capturecode);

        hasSurface = false;

    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        if(hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() is called to init the camera.
            surfaceHolder.addCallback(this);
            //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.i(TAG, "initCamera -- initializing camera");

        if(surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if(cameraManager.isOpen()) {
            //Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try{
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException
            if(handler == null) {
                Log.i(TAG,"starting capture activity handler");
                decodeFormats = EnumSet.of(
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.RSS_14,
                        BarcodeFormat.RSS_EXPANDED,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.ITF,
                        BarcodeFormat.CODABAR
                        );
                decodeHints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);

                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, null, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch(IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch(RuntimeException e) {
            // Barcode Scanner original application has seen crashes in the wild that reaches this stack:
            // java.lang.RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    @Override
    protected void onPause() {
        if(handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        cameraManager.closeDriver();
        if(!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            default: return true;
            case KeyEvent.KEYCODE_BACK:
                restartPreviewAfterDelay(0L);
                return true;
        }
    }

    private void restartPreviewAfterDelay(long delayMs) {
        if(handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMs);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        //menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    private void displayFrameworkBugMessageAndExit(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        final Activity me = this;
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                me.finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                me.finish();
            }
        });
        builder.show();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(surfaceHolder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave s a null surface!");
        }
        if(!hasSurface) {
            hasSurface = true;
            initCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        hasSurface = false;
    }

    public CaptureActivityHandler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void handleDecode(Result obj, Bitmap barcode, float scaleFactor) {
        lastResult = obj;
        if(lastResult != null) {
            Log.d(TAG, lastResult.getText());
            saveDecodedResult(lastResult.getText());
        }
    }


    // TODO: Remove the button that calls this listener and remove this method
    private int incr = 0;
    public void doSimulateIntent(View view) {
        saveDecodedResult("Increment Value " + incr++ );
    }

    // Send Decoded Result to History Lizt
    private void saveDecodedResult(String decodedResult) {
        if(decodedResult == null || decodedResult.trim().length() == 0) {
            return;
        }
        Intent saveResultIntent = new Intent(this, ResultListActivity.class);
        saveResultIntent.putExtra("message", decodedResult);
        startActivity(saveResultIntent);
    }
}
