package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.card.MaterialCardView;
import com.example.basta_game.R;

public class PerfilFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        MaterialCardView btnCambiarAvatar = view.findViewById(R.id.btnCambiarAvatar);

        btnCambiarAvatar.setOnClickListener(v -> {

            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_avatars);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            dialog.setCancelable(false);

            AppCompatButton btnCancelar = dialog.findViewById(R.id.btnCancelarAvatar);
            AppCompatButton btnGuardar = dialog.findViewById(R.id.btnGuardarAvatar);

            btnCancelar.setOnClickListener(vCancelar -> dialog.dismiss());

            btnGuardar.setOnClickListener(vGuardar -> {
                dialog.dismiss();
            });

            dialog.show();
        });

        return view;
    }
}