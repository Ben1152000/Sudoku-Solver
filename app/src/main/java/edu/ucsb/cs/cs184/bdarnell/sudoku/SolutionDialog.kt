package edu.ucsb.cs.cs184.bdarnell.sudoku

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import kotlinx.android.synthetic.main.image_dialog.view.*
import java.io.File


class SolutionDialog(val destination: File) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.image_dialog, container, false)

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeFile(destination.toString(), options)
        root.imageView.setImageBitmap(bitmap)

        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}