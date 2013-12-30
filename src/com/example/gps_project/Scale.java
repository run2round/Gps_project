package com.example.gps_project;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

/*
 This activity is used to display the photo with full resolution
 */
public class Scale extends Activity{

	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.imagepreview);
    	Bundle extra = getIntent().getExtras();
    	Bitmap tem;
    	ImageView preview = (ImageView)findViewById(R.id.preview); 
		tem = BitmapFactory.decodeFile(extra.getString("path"));
    	preview.setImageBitmap(tem);
	}

}
