package com.example.basta_game.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.basta_game.R;
import com.google.android.material.card.MaterialCardView;

public class NotificationHelper {

    public static final int SUCCESS = 1;
    public static final int ERROR = 2;
    public static final int WARNING = 3;
    public static final int INFO = 4;

    public static void show(View notificationView, String message, int type) {

        MaterialCardView card = (MaterialCardView) notificationView;

        View stripe = card.findViewById(R.id.viewNotificationStripe);
        MaterialCardView iconCard = card.findViewById(R.id.cardNotificationIcon);
        ImageView imgIcon = card.findViewById(R.id.img_iconNotificacion);
        TextView txtTitle = card.findViewById(R.id.txtNotificationTitle);
        TextView txtMessage = card.findViewById(R.id.txtNotificationMessage);

        txtMessage.setText(message);

        int mainColor;
        int softColor;
        int icon;
        String title;

        switch (type) {
            case SUCCESS:
                mainColor = Color.parseColor("#1E9B4B");
                softColor = Color.parseColor("#E8F8EE");
                icon = R.drawable.ic_notificacion_succes;
                title = "¡Excelente!";
                break;

            case ERROR:
                mainColor = Color.parseColor("#E63946");
                softColor = Color.parseColor("#FFECEF");
                icon = R.drawable.ic_notificacion_error;
                title = "Algo salió mal";
                break;

            case WARNING:
                mainColor = Color.parseColor("#FFB703");
                softColor = Color.parseColor("#FFF4CC");
                icon = R.drawable.ic_notificacion_warning;
                title = "Aviso del juego";
                break;

            case INFO:
            default:
                mainColor = Color.parseColor("#0B4DFF");
                softColor = Color.parseColor("#EAF2FF");
                icon = R.drawable.ic_notificacion_info;
                title = "Información";
                break;
        }

        card.setCardBackgroundColor(Color.parseColor("#FFFDF6"));
        card.setStrokeColor(mainColor);

        stripe.setBackgroundColor(mainColor);

        iconCard.setCardBackgroundColor(softColor);
        iconCard.setStrokeColor(mainColor);

        imgIcon.setImageResource(icon);
        txtTitle.setText(title);
        txtTitle.setTextColor(mainColor);

        card.setAlpha(0f);
        card.setTranslationY(-25f);
        card.setVisibility(View.VISIBLE);

        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        card.postDelayed(() -> {
            card.animate()
                    .alpha(0f)
                    .translationY(-25f)
                    .setDuration(220)
                    .withEndAction(() -> card.setVisibility(View.GONE))
                    .start();
        }, 3000);
    }
}