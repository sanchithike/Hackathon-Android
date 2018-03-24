package com.visagetechnologies.visagetrackerdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class WarningActivity extends Activity 
{
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);	
		ShowDialog();
	}
	
	void ShowDialog()
	{
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Warning!");
		alertDialog.setMessage("You are using an unlicensed version of visage|SDK for Android. Tracking stops after one minute.");
		alertDialog.setButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int i)
			{
				finish();
			}
		});
			
		alertDialog.show();
	}
}
