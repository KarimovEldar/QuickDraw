package com.example.drawingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.drawingapp.databinding.DialogBrushSizeBinding

class BrushSizeDialogFragment : DialogFragment() {

    private var _binding: DialogBrushSizeBinding? = null
    private val binding get() = _binding!!

    var onBrushSizeSelected: ((Float) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogBrushSizeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.smallBrushButton.setOnClickListener {
            onBrushSizeSelected?.invoke(10f)  // Small brush
            dismiss()
        }

        binding.mediumBrushButton.setOnClickListener {
            onBrushSizeSelected?.invoke(20f)  // Medium brush
            dismiss()
        }

        binding.largeBrushButton.setOnClickListener {
            onBrushSizeSelected?.invoke(30f)  // Large brush
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clean up the binding reference
    }
}
