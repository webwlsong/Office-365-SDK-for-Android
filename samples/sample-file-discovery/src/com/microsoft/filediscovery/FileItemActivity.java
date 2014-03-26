/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.microsoft.assetmanagement.R;
import com.microsoft.filediscovery.adapters.DisplayFileItemAdapter;
import com.microsoft.filediscovery.tasks.SaveFileTask;
import com.microsoft.filediscovery.viewmodel.FileItem;

// TODO: Auto-generated Javadoc
/**
 * The Class FileItemActivity.
 */
public class FileItemActivity extends FragmentActivity {

	/** The m car view item. */
	private FileItem mFileSaveItem;

	String mShareUri = null;
	/** The m application. */
	private AssetApplication mApplication;

	/** The Constant CAMARA_REQUEST_CODE. */
	final static int CAMARA_REQUEST_CODE = 1000;

	/** The Constant SELECT_PHOTO. */
	final static int SELECT_PHOTO = 1001;
	DisplayFileItemAdapter mAdapter;

	BitmapResizer mResizer;

	/**
	 * Sets the car view item.
	 * 
	 * @param carListViewItem
	 *            the new car view item
	 */
	public void setFileViewItem(FileItem fileSaveItem) {
		mFileSaveItem = fileSaveItem;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.file_view_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mApplication = (AssetApplication) getApplication();
		setContentView(R.layout.activity_file_display);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String data = bundle.getString("data");
			if (data != null) {
				JSONObject payload;
				try {
					payload = new JSONObject(data);
					mFileSaveItem = new FileItem();
					mFileSaveItem.ResourceId = payload.getString("resourseId");
					mFileSaveItem.Endpoint = payload.getString("endpoint");
					mShareUri = payload.getString("shareUri");
				} 
				catch (JSONException e) {

					Log.e("Asset", e.getMessage());
				}
			}
		}

		DisplayMetrics displayMetrics = new DisplayMetrics();
		mResizer = new BitmapResizer(displayMetrics);

		if(mShareUri.length() > 0) ShowImageToShare();
	}

	private void ShowImageToShare() {
		final byte[] bytes = getImageData(SELECT_PHOTO, RESULT_OK, new Intent());

		File file = new File(mShareUri);
		FileInputStream fin;
		try {
			fin = new FileInputStream (file);
			byte fileContent[] = new byte[(int)file.length()];
			fin.read(fileContent);

			if (bytes != null) {
				mFileSaveItem.Content = bytes;

				mAdapter = new  DisplayFileItemAdapter(this, bytes);
				ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
				viewPager.setAdapter(mAdapter);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			finish();
			return true;
		}
		case R.id.menu_save_image: {
			hideSoftPad();
			saveAction();
			return true;
		}
		case R.id.menu_file_save: {
			selectPicture();
			return true;
		}
		default:
			return true;
		}
	}

	/**
	 * Select picture.
	 */
	private void selectPicture() {
		final Activity that = this;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				CharSequence[] sources = { "From Library", "From Camera" };
				AlertDialog.Builder builder = new AlertDialog.Builder(that);
				builder.setTitle("Select an option:").setSingleChoiceItems(sources, 0,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								dialog.dismiss();
								openPhotoSource(item);
							}

							private void openPhotoSource(int itemSelected) {
								switch (itemSelected) {
								case 0:
									invokePhotoLibrayIntent();
									break;
								case 1:
									invokeFromCameraIntent();
									break;
								default:
									break;
								}
							}

							private void invokeFromCameraIntent() {
								dispatchTakePictureIntent();
							}

							private void invokePhotoLibrayIntent() {
								Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
								photoPickerIntent.setType("image/*");
								startActivityForResult(photoPickerIntent, SELECT_PHOTO);
							}
						});
				builder.create().show();
			}
		});
	}

	/** The m current photo path. */
	String mCurrentPhotoPath;

	/**
	 * Creates the image file.
	 * 
	 * @return the file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressLint("SimpleDateFormat")
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(imageFileName, /* prefix */
				".jpg", /* suffix */
				storageDir /* directory */
		);

		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	/**
	 * Dispatch take picture intent.
	 */
	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = createImageFile();
			} catch (IOException ex) {
				Log.e("Asset", ex.getMessage());
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
				startActivityForResult(takePictureIntent, CAMARA_REQUEST_CODE);
			}
		}
	}

	/**
	 * Save action.
	 */
	private void saveAction() {
		hideSoftPad();

		mFileSaveItem.Name = ((EditText) findViewById(R.id.textFileName)).getText().toString().trim();// .getText().toString();
		if (mFileSaveItem.Name.length() == 0 || mFileSaveItem.Content == null) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Information");
			builder.setMessage("All fields and a photo are required");
			builder.create().show();
			return;
		}

		new SaveFileTask(this).execute(mFileSaveItem);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		final byte[] bytes = getImageData(requestCode, resultCode, data);

		if (bytes != null) {
			mFileSaveItem.Content = bytes;
			mAdapter = new DisplayFileItemAdapter(this, bytes);

			ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
			viewPager.setAdapter(mAdapter);
		}
	}

	/**
	 * Gets the image data.
	 * 
	 * @param requestCode
	 *            the request code
	 * @param resultCode
	 *            the result code
	 * @param data
	 *            the data
	 * @return the image data
	 */
	private final byte[] getImageData(int requestCode, int resultCode, Intent data) {

		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		switch (requestCode) {
		case SELECT_PHOTO: {
			if (resultCode == RESULT_OK) {

				try {
					Uri selectedImage = data.getData();

					InputStream imageStream = getContentResolver().openInputStream(selectedImage);
					Bitmap bitmap = mResizer.getBitmapFrom(imageStream);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
					return stream.toByteArray();
				} catch (Throwable t) {
					mApplication.handleError(t);
				}
			}
		}
		case CAMARA_REQUEST_CODE: {
			if (resultCode == RESULT_OK) {
				try {
					if (mCurrentPhotoPath != null) {
						Bitmap bitmap = mResizer.getBitmapFrom(mCurrentPhotoPath);
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
						return stream.toByteArray();
					}
				} catch (Throwable t) {
					mApplication.handleError(t);
				}
			}
		}
		default:
			break;
		}
		return null;
	}

	/**
	 * Hide soft pad.
	 */
	private void hideSoftPad() {
		((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).toggleSoftInput(
				InputMethodManager.SHOW_IMPLICIT, 0);
	}

	protected void openFile(String fileName) {
		Intent install = new Intent(Intent.ACTION_VIEW);
		install.setDataAndType(Uri.fromFile(new File(fileName)), "MIME-TYPE");
		startActivity(install);
	}
}
