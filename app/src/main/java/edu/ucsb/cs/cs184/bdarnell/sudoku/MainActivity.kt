package edu.ucsb.cs.cs184.bdarnell.sudoku

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import java.io.File
import org.opencv.android.LoaderCallbackInterface
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import androidx.fragment.app.FragmentActivity
import org.opencv.android.BaseLoaderCallback


class MainActivity : AppCompatActivity() {

    var fotoapparat: Fotoapparat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabCamera.setOnClickListener { takePhoto() }

        // Create fotoapparat:
        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(logcat()),
            cameraErrorCallback = { error ->
                println("Recorder errors: $error")
            }
        )
        OpenCVLoader.initDebug()
        /*
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, object: BaseLoaderCallback(this){
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.d("hello", "loaded successfully")
                    }
                    else -> {
                        super.onManagerConnected(status)

                    }
                }
            }
        })
         */
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
    }

    override fun onStart() {
        super.onStart()
        if (hasNoPermissions()) {
            requestPermission()
        } else {
            fotoapparat?.start()
        }
    }

    val filename = "sudoku.png"
    val storageDirectory = Environment.getExternalStorageDirectory()
    val destination = File(storageDirectory, filename)

    private fun takePhoto() {
        if (hasNoPermissions()) {
            requestPermission()
        } else {
            //fotoapparat?.takePicture()?.saveToFile(destination)
            Vision.analyzeImage()
        }
    }

    val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }
}
