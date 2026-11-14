package com.example.basta_game.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.basta_game.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayDeque;
import java.util.WeakHashMap;

public class NotificationHelper {

    public static final int SUCCESS = 1;
    public static final int ERROR = 2;
    public static final int WARNING = 3;
    public static final int INFO = 4;

    private static final long DURACION_ENTRADA_MS = 220;
    private static final long DURACION_VISIBLE_MS = 3000;
    private static final long DURACION_SALIDA_MS = 220;

    private static final WeakHashMap<View, EstadoNotificacion> estados = new WeakHashMap<>();

    private static class MensajeNotificacion {
        String message;
        int type;

        MensajeNotificacion(String message, int type) {
            this.message = message;
            this.type = type;
        }
    }

    private static class EstadoNotificacion {
        ArrayDeque<MensajeNotificacion> cola = new ArrayDeque<>();
        boolean mostrando = false;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnableSalida;
    }

    public static void show(View notificationView, String message, int type) {
        if (notificationView == null) {
            return;
        }

        notificationView.post(() -> {
            EstadoNotificacion estado = estados.get(notificationView);

            if (estado == null) {
                estado = new EstadoNotificacion();
                estados.put(notificationView, estado);
            }

            estado.cola.offer(new MensajeNotificacion(message, type));

            if (!estado.mostrando) {
                mostrarSiguiente(notificationView, estado);
            }
        });
    }

    private static void mostrarSiguiente(View notificationView, EstadoNotificacion estado) {
        if (notificationView == null) {
            estado.mostrando = false;
            return;
        }

        MensajeNotificacion siguiente = estado.cola.poll();

        if (siguiente == null) {
            estado.mostrando = false;
            return;
        }

        estado.mostrando = true;

        if (estado.runnableSalida != null) {
            estado.handler.removeCallbacks(estado.runnableSalida);
            estado.runnableSalida = null;
        }

        MaterialCardView card = (MaterialCardView) notificationView;

        card.animate().cancel();
        card.clearAnimation();

        aplicarDatosNotificacion(card, siguiente.message, siguiente.type);

        card.bringToFront();
        card.setTranslationZ(50f);

        card.setAlpha(0f);
        card.setTranslationY(-25f);
        card.setVisibility(View.VISIBLE);

        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(DURACION_ENTRADA_MS)
                .withEndAction(() -> {
                    estado.runnableSalida = () -> ocultarActual(card, estado);
                    estado.handler.postDelayed(estado.runnableSalida, DURACION_VISIBLE_MS);
                })
                .start();
    }

    private static void ocultarActual(MaterialCardView card, EstadoNotificacion estado) {
        if (card == null) {
            estado.mostrando = false;
            return;
        }

        card.animate().cancel();
        card.clearAnimation();

        card.animate()
                .alpha(0f)
                .translationY(-25f)
                .setDuration(DURACION_SALIDA_MS)
                .withEndAction(() -> {
                    card.setVisibility(View.GONE);
                    card.setAlpha(1f);
                    card.setTranslationY(0f);

                    estado.mostrando = false;
                    estado.runnableSalida = null;

                    if (!estado.cola.isEmpty()) {
                        mostrarSiguiente(card, estado);
                    }
                })
                .start();
    }

    private static void aplicarDatosNotificacion(MaterialCardView card, String message, int type) {
        View stripe = card.findViewById(R.id.viewNotificationStripe);
        MaterialCardView iconCard = card.findViewById(R.id.cardNotificationIcon);
        ImageView imgIcon = card.findViewById(R.id.img_iconNotificacion);
        TextView txtTitle = card.findViewById(R.id.txtNotificationTitle);
        TextView txtMessage = card.findViewById(R.id.txtNotificationMessage);

        if (txtMessage != null) {
            txtMessage.setText(message);
        }

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

        if (stripe != null) {
            stripe.setBackgroundColor(mainColor);
        }

        if (iconCard != null) {
            iconCard.setCardBackgroundColor(softColor);
            iconCard.setStrokeColor(mainColor);
        }

        if (imgIcon != null) {
            imgIcon.setImageResource(icon);
        }

        if (txtTitle != null) {
            txtTitle.setText(title);
            txtTitle.setTextColor(mainColor);
        }
    }
}