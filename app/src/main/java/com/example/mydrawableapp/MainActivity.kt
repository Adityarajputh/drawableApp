package com.example.mydrawableapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawableView? = null
    private var mCurrentPaintButton: ImageButton? = null
    private var mGalleryButton: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val ivImageBackground: ImageView = findViewById(R.id.fl_iv_background)

                ivImageBackground.setImageURI(result.data?.data)
            //result.data is location of the image adding .data will give us the actual image data
                //in the URI form.

            }
        }

    private val permissionLauncher: ActivityResultLauncher <Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
                permissions ->
                permissions.entries.forEach {
                    val name = it.key
                    val status = it.value
                    if(status){
                        if(name == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(applicationContext,"Storage Permission Granted",
                               Toast.LENGTH_LONG).show()
                            val pickIntent = Intent(Intent.ACTION_PICK,
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            galleryLauncher.launch(pickIntent)
                        }else{
                        Toast.makeText(applicationContext,"Storage Permission Granted",
                            Toast.LENGTH_LONG).show()
                    }
                    }
                    else{
                        if(name == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(applicationContext,"Storage Permission Denied",
                                Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(applicationContext,"Storage Permission Denied",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_color_selector)
        mCurrentPaintButton = linearLayoutPaintColors[1] as ImageButton


        drawingView = findViewById(R.id.uiView)
        drawingView?.setBrushSize(20.toFloat())



        mGalleryButton = findViewById(R.id.ib_gallery)
        mGalleryButton?.setOnClickListener {
                requestPermission()
        }

        val undoIcon: ImageButton = findViewById(R.id.ib_undo)
        undoIcon.setOnClickListener {
                drawingView?.onUndoClicked()
        }

        val redoIcon: ImageButton = findViewById(R.id.ib_redo)
        redoIcon.setOnClickListener {
            drawingView?.onRedoClicked()
        }

        val brushIcon: ImageButton = findViewById(R.id.ib_brush)
        brushIcon.setOnClickListener{
            showBrushSizeSelectorDialog()
        }

        val saveIcon: ImageButton = findViewById(R.id.ib_save)
        saveIcon.setOnClickListener {
            if(checkPermissionGranted()){
                showCustomProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmap(getBitmapFromView(flDrawingView))

                }
            }

        }
    }

    private fun checkPermissionGranted(): Boolean{
        val result = ActivityCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Request Storage Access",
                "would you Grant the App to access your Storage to set" +
                        "the background image of the canvas?")
        }else{
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(
        title : String,
        message : String){
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.setIcon(R.drawable.ic_alert)
        dialog.setPositiveButton("YES") { digitalInterface, _ ->
//            Toast.makeText(applicationContext, "Access Granted", Toast.LENGTH_LONG).show();
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            digitalInterface.dismiss()
        }
        dialog.setNegativeButton("NO") { digitalInterface, _ ->
            Toast.makeText(applicationContext, "Access Denied", Toast.LENGTH_LONG).show();
            digitalInterface.dismiss()
        }
        val alertDialog: AlertDialog = dialog.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showBrushSizeSelectorDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.button_resource)
        brushDialog.setTitle("Brush Size: ")

        val btnSmall = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        btnSmall.setOnClickListener{
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val btnMedium = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        btnMedium.setOnClickListener {
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }

        val btnLarge = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        btnLarge.setOnClickListener {
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun colorClicked(view: View){
        if(view !== mCurrentPaintButton){
            val imageButton = view as ImageButton
            val colorSelected = imageButton.tag.toString()

            drawingView?.setColor(colorSelected)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.color_palette_pressed)
            )

            mCurrentPaintButton?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.color_palette_resource)
            )

            mCurrentPaintButton = view
        }
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val resultBitmap = Bitmap.createBitmap(view.width,view.height,
                            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(resultBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return resultBitmap
    }

    private suspend fun saveBitmap(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawingApp" + System.currentTimeMillis()/1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelCustomProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "Image is stored at:$result",Toast.LENGTH_SHORT).show()
                            shareMedia(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                                "There was an Error while trying to store the image",
                                  Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showCustomProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.progess_dialog)
        customProgressDialog?.show()
    }

    private fun cancelCustomProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareMedia(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))

        }
    }

}