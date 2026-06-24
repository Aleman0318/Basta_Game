package com.example.basta_game.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.basta_game.R;
import com.example.basta_game.utils.Sounds;

public class LoadingPartidaFragment extends Fragment {

    private Bundle datosPartida;
    private Handler handler;

    public LoadingPartidaFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            datosPartida = getArguments();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_loading_partida, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Sounds.playLoadingPartida();

        TextView txtCuentaRegresiva = view.findViewById(R.id.txtCuentaRegresiva);

        handler = new Handler(Looper.getMainLooper());

        final int[] segundos = {10};

        txtCuentaRegresiva.setText(String.valueOf(segundos[0]));

        Runnable contador = new Runnable() {
            @Override
            public void run() {

                segundos[0]--;

                if (segundos[0] >= 0) {

                    txtCuentaRegresiva.setText(String.valueOf(segundos[0]));

                    txtCuentaRegresiva.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .setDuration(150)
                            .withEndAction(() ->
                                    txtCuentaRegresiva.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(150)
                            );

                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.postDelayed(contador, 1000);

        handler.postDelayed(() -> {

            if (!isAdded()) {
                return;
            }

            PartidaFragment partida = new PartidaFragment();
            partida.setArguments(datosPartida);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.sala_fragment_container, partida)
                    .commit();

        }, 10000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}