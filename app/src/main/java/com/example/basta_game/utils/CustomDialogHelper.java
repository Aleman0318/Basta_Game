package com.example.basta_game.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;

import com.example.basta_game.R;

public class CustomDialogHelper {

    public interface DialogAction {
        void onClick();
    }

    public static class DialogButton {
        String texto;
        int colorFondo;
        int colorTexto;
        boolean cerrarAlPresionar;
        DialogAction accion;

        public DialogButton(String texto, int colorFondo, int colorTexto, boolean cerrarAlPresionar, DialogAction accion) {
            this.texto = texto;
            this.colorFondo = colorFondo;
            this.colorTexto = colorTexto;
            this.cerrarAlPresionar = cerrarAlPresionar;
            this.accion = accion;
        }
    }

    public static void show(
            Context context,
            String titulo,
            String mensaje,
            boolean cancelable,
            DialogButton... botones
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = LayoutInflater.from(context);
        dialog.setContentView(inflater.inflate(R.layout.component_custom_dialog, null));

        dialog.setCancelable(cancelable);

        TextView txtTitulo = dialog.findViewById(R.id.txtDialogTitle);
        TextView txtMensaje = dialog.findViewById(R.id.txtDialogMessage);
        LinearLayout layoutBotones = dialog.findViewById(R.id.layoutDialogButtons);

        txtTitulo.setText(titulo);
        txtMensaje.setText(mensaje);

        layoutBotones.removeAllViews();

        if (botones.length > 2) {
            layoutBotones.setOrientation(LinearLayout.VERTICAL);
        } else {
            layoutBotones.setOrientation(LinearLayout.HORIZONTAL);
        }

        for (DialogButton boton : botones) {
            AppCompatButton btn = new AppCompatButton(context);

            LinearLayout.LayoutParams params;

            if (botones.length > 2) {
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(context, 48)
                );

                params.setMargins(
                        0,
                        dpToPx(context, 8),
                        0,
                        0
                );

            } else {
                params = new LinearLayout.LayoutParams(
                        0,
                        dpToPx(context, 48),
                        1
                );

                params.setMargins(
                        dpToPx(context, 6),
                        0,
                        dpToPx(context, 6),
                        0
                );
            }

            btn.setLayoutParams(params);
            btn.setText(boton.texto);
            btn.setTextSize(15);
            btn.setTextColor(boton.colorTexto);
            btn.setAllCaps(false);

            btn.setTypeface(ResourcesCompat.getFont(context, R.font.lilitaone_regular));

            btn.setBackgroundResource(R.drawable.bg_boton_pildora);
            btn.setBackgroundTintList(ColorStateList.valueOf(boton.colorFondo));

            btn.setPadding(
                    dpToPx(context, 8),
                    0,
                    dpToPx(context, 8),
                    0
            );

            btn.setOnClickListener(v -> {
                if (boton.accion != null) {
                    boton.accion.onClick();
                }

                if (boton.cerrarAlPresionar) {
                    dialog.dismiss();
                }
            });

            layoutBotones.addView(btn);
        }

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int ancho = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.82);
            dialog.getWindow().setLayout(
                    ancho,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}