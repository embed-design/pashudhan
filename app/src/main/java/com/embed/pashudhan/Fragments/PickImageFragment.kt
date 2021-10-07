package com.embed.pashudhan.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.embed.pashudhan.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_pick_image.*
import java.io.Serializable

class PickImageFragment : BottomSheetDialogFragment() {
    companion object {
        var TAG = "UserUploadedPosts==>"
        fun newInstance(pickImage: (() -> Unit), capture: (() -> Unit)) =
            PickImageFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("pickImage", pickImage as Serializable)
                    putSerializable("capture", capture as Serializable)
                }
            }
    }

    private lateinit var pickImageFun: () -> Unit
    private lateinit var captureFun: () -> Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        pickImageFun = arguments?.getSerializable("pickImage") as () -> Unit
        captureFun = arguments?.getSerializable("capture") as () -> Unit
        return inflater.inflate(R.layout.fragment_pick_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        picker_cameraButton.setOnClickListener {
            captureFun()
            this.dismiss()
        }
        picker_galleryButton.setOnClickListener {
            pickImageFun()
            this.dismiss()
        }

        photosCloseButton.setOnClickListener {
            this.dismiss()
        }

    }

}