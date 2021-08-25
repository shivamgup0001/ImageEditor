package com.example.imageeditor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Button button;
    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
      init();
    }
    private static final int REQUEST_PERMISSIONS=1234;
    private static final String[] PERMISSIONS={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT =2;

    @SuppressLint("NewApi")
    private boolean notPermissions(){
        for(int i=0;i<PERMISSIONS_COUNT;i++){
            if(checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    static {
        System.loadLibrary("imageEditor");
    }

    private static native void blackAndWhite (int [] pixels,int width,int height);

    @Override
    protected void onResume()
    {
        super.onResume();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&notPermissions()){
            requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_PERMISSIONS&& grantResults.length>0){
            if(notPermissions()){
                ((ActivityManager)this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }
        }
    }
    private static final int REQUEST_PICK_IMAGE=12345;
    private ImageView imageView;

    private void init()
    {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder =new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        imageView=findViewById(R.id.imageView);
        if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            findViewById(R.id.button1).setVisibility(View.GONE);
        }
        button=findViewById(R.id.button);
        button1=findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent =new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                final Intent pickIntent=new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                final Intent chooserIntent = Intent.createChooser(intent,"Select image");
                startActivityForResult(chooserIntent,REQUEST_PICK_IMAGE);
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent takePictureIntent =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(getPackageManager())!=null){
                    final File photoFile =createImageFile();
                    imageUri=Uri.fromFile(photoFile);
                    final SharedPreferences myPrefs=getSharedPreferences(appID,0);
                    myPrefs.edit().putString("path",photoFile.getAbsolutePath()).apply();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                }else {
                    Toast.makeText(MainActivity.this, "Your Camera is not compatible", Toast.LENGTH_SHORT).show();
                }
            }
        });
        final Button blackAndWhiteButton =findViewById(R.id.button2);
        blackAndWhiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run(){
                       blackAndWhite(pixels,width,height);
                        bitmap.setPixels(pixels,0,width,0,0,width,height);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });
        final Button saveImageButton =findViewById(R.id.button4);
        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogClickListener= new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which==DialogInterface.BUTTON_POSITIVE){
                            final File outFile=createImageFile();
                            try(FileOutputStream out =new FileOutputStream(outFile)){
                                bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
                                imageUri=Uri.parse("file://"+outFile.getAbsolutePath());
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
                                Toast.makeText(MainActivity.this,"The Image was Saved",Toast.LENGTH_SHORT).show();

                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Save current photo to Gallery ?").setPositiveButton("yes",
                        dialogClickListener).setNegativeButton("no",dialogClickListener).show();
            }
        });
        final EditText editText=findViewById(R.id.editText);
        final TextView textView=findViewById(R.id.textView2);
        final ImageButton imageButton=findViewById(R.id.imageButton);
        final ImageButton imageButton2=findViewById(R.id.imageButton2);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s= editText.getText().toString();
                if(!TextUtils.isEmpty(s)) {
                    Bitmap b=drawTextToBitmap(MainActivity.this,s);
                   check=1;
                   imageView.setImageBitmap(bitmap);
                    s=null;

                }
                else
                    Toast.makeText(MainActivity.this, "Please Enter something", Toast.LENGTH_SHORT).show();
            }
        });
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check=1;
            }
        });
    }

    private int check=0;
    private String s;
    Rect rect;
    Rect bounds;
    public Bitmap doodle(float x1,float y1){
        Canvas canvas=new Canvas(bitmap);
        Paint p1=new Paint();
        p1.setColor(Color.BLACK);
        canvas.drawCircle(x1,y1,30,p1);
        Log.d("shivam","Error");
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }
    private final int TEXT_SIZE=120;
    public Bitmap drawTextToBitmap(Context gContext,
                                   String gText) {



        Canvas canvas = new Canvas(bitmap);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.RED);
        // text size in pixels
        paint.setTextSize(TEXT_SIZE);
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        // draw text to the Canvas center
         bounds = new Rect();
        rect=new Rect();
        Paint paint1=new Paint();
        paint1.setColor(Color.YELLOW);
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/4;

        rect.set(x,y-TEXT_SIZE,x+bounds.width(),y);
            canvas.drawRect(rect, paint1);
            canvas.drawText(gText, x, y, paint);

        return bitmap;

    }
    private static final String appID="imageEditor";

    private static final int REQUEST_IMAGE_CAPTURE =1012;

    private Uri imageUri;

    private File createImageFile(){
        final String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
       final String imageFileName="/JPEG_"+ timeStamp +".jpg";
        final File storageDir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir+imageFileName);
    }

    private boolean editMode=false;
    private Bitmap bitmap;
    private int width =0;
    private int height =0;
    private static final int MAX_PIXEL_COUNT=2048;

    private int[] pixels;
    private int pixelsCount=0;

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK){
            return;
        }
        if(requestCode==REQUEST_IMAGE_CAPTURE){
            if(imageUri==null){
                final SharedPreferences p=getSharedPreferences(appID,0);
                final String path =p.getString("path","");
                if(path.length()<1){
                    recreate();
                    return;
                }
                imageUri=Uri.parse("file://"+path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
        }else if(data==null){
            recreate();
            return;
        }else if(requestCode==REQUEST_PICK_IMAGE){
            imageUri=data.getData();
        }

        final ProgressDialog dialog =ProgressDialog.show(MainActivity.this,"Loading",
                "Please Wait",true);
        editMode=true;
        findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);

        new Thread(){
            public void run(){
                bitmap=null;
                final BitmapFactory.Options bmpOptions =new BitmapFactory.Options();
                bmpOptions.inBitmap=bitmap;
                bmpOptions.inJustDecodeBounds=true;
                try(InputStream input=getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);

                }catch (IOException e){
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds=false;
                width= bmpOptions.outWidth;
                height=bmpOptions.outHeight;
                int resizeScale =1;
                if(width>MAX_PIXEL_COUNT){
                    resizeScale=width/MAX_PIXEL_COUNT;
                }else if(height>MAX_PIXEL_COUNT){
                    resizeScale=height/MAX_PIXEL_COUNT;
                }
                if(width/resizeScale>MAX_PIXEL_COUNT||height/resizeScale>MAX_PIXEL_COUNT){
                    resizeScale++;
                }
                bmpOptions.inSampleSize=resizeScale;
                InputStream input =null;
                try {
                    input=getContentResolver().openInputStream(imageUri);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap=BitmapFactory.decodeStream(input,null,bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            imageView.setImageBitmap(bitmap);
                            dialog.cancel();

                    }
                });
                width=bitmap.getWidth();
                height=bitmap.getHeight();
                bitmap= bitmap.copy(Bitmap.Config.ARGB_8888,true);

                pixelsCount=width*height;
                pixels=new int[pixelsCount];
                bitmap.getPixels(pixels,0,width,0,0,width,height);
            }
        }.start();


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x=event.getX();
        float y =event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:

                   if( check==1)
                   {
                       doodle(x,y);
                   }
                   return true;
                //    rect.left= (int) x;
                  //  rect.right=rect.left+bounds.width();
                    //rect.bottom= (int) y;
                   // rect.top=rect.bottom-50;
                    //drawTextToBitmap(MainActivity.this,s);


        }
        return super.onTouchEvent(event);
    }
}