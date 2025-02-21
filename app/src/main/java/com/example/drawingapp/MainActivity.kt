package com.example.drawingapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                //process the data
                binding.imageBackgroundImageView.setImageURI(result.data?.data)
            }
        }
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value
                // If permission is granted show a toast and perform operation
                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Perform operation
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    // Read external storage
                    if (perMissionName == READ_EXTERNAL_STORAGE)
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawingView = binding.drawingView

        drawingView!!.setSizeForBrush(5f)

        mImageButtonCurrentPaint = binding.paintColorsLinearLayout.getChildAt(3) as ImageButton

        mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        binding.brushButton.setOnClickListener {
            showBrushSizeDialog()
        }

        binding.galleryButton.setOnClickListener {
            requestStoragePermission()
        }

        binding.undoButton.setOnClickListener {
            drawingView?.onClickUndo()
        }

        binding.saveButton.setOnClickListener {
            showProgressDialog()
            //check if permission is allowed
            lifecycleScope.launch {
                delay(1000)
                val flDrawingView: FrameLayout = binding.drawingViewContainerFlameLayout
                //Save the image to the device
                saveBitmap(flDrawingView)
                cancelProgressDialog()
            }

        }

    }


    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_pressed
                )
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            mImageButtonCurrentPaint = view
        }
    }

    private fun showBrushSizeDialog() {
        val dialog = BrushSizeDialogFragment()

        // Set the callback to handle the selected brush size
        dialog.onBrushSizeSelected = { size ->
            drawingView?.setSizeForBrush(size)
            Toast.makeText(this, "Brush size set to $size", Toast.LENGTH_SHORT).show()
        }

        // Show the dialog
        dialog.show(supportFragmentManager, BrushSizeDialogFragment::class.java.simpleName)

    }

    private fun requestStoragePermission() {
        // Check if the permission was denied and show rationale
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "QuickDraw", "QuickDraw" +
                        "needs to Access Your External Storage"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            )
        }

    }

    // show Dialog
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {

        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private fun saveBitmap(view: View) {
        //launch a coroutine block
        val bitmap = getBitmapFromView(view) // replace with your view
        // save bitmap to gallery based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "DrawingApp_${System.currentTimeMillis() / 1000}.png"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }

            var imageUri: Uri? = null
            resolver.run {
                imageUri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let {
                    openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                    }
                }
            }

            if (imageUri != null) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully: $imageUri",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this@MainActivity,
                "Something went wrong while saving the file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

}