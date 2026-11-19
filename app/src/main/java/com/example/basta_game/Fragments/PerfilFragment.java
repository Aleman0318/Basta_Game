package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.content.Intent;
import com.example.basta_game.LoginActivity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import com.bumptech.glide.Glide;
import com.example.basta_game.Adapters.AvatarAdapter;
import com.example.basta_game.models.Avatar;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.example.basta_game.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;

public class PerfilFragment extends Fragment {

    private Avatar avatarSeleccionado = null;
    private List<Avatar> listaAvatares;
    private ImageView ivAvatarActual;
    private AvatarAdapter adapter;
    private RecyclerView rvListaAvatares;
    private View perfilNotification;
    private TextView tvUsername, tvCorreo, tvPartidasJugadas, tvPartidasGanadas, tvPromedioPuntos;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        ivAvatarActual = view.findViewById(R.id.ivAvatarActual);
        MaterialCardView btnCambiarAvatar = view.findViewById(R.id.btnCambiarAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);
        ImageButton btnEditarNombre = view.findViewById(R.id.btnEditarNombre);
        btnEditarNombre.setOnClickListener(v -> mostrarDialogoEditarNombre(btnEditarNombre));
        tvCorreo = view.findViewById(R.id.tvCorreo);
        tvPartidasJugadas = view.findViewById(R.id.tvPartidasJugadas);
        tvPartidasGanadas = view.findViewById(R.id.tvPartidasGanadas);
        tvPromedioPuntos = view.findViewById(R.id.tvPromedioPuntos);
        perfilNotification = view.findViewById(R.id.perfilNotification);
        MaterialButton btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);



        Animations.animacionAparicion(tvCorreo);
        Animations.animacionAparicion(tvUsername);
        Animations.animacionAparicion(tvPartidasGanadas);
        Animations.animacionAparicion(tvPartidasJugadas);
        Animations.animacionAparicion(tvPromedioPuntos);
        Animations.animacionAparicion(btnCerrarSesion);

        btnCerrarSesion.setOnClickListener(v -> {
            Animations.animacionZoom(v);

            if (getActivity() instanceof com.example.basta_game.HomeActivity)
                ((com.example.basta_game.HomeActivity) getActivity()).confirmarCerrarSesion();
        });


        inicializarOpcionesAvatares();
        cargarPerfilUsuario();
        btnCambiarAvatar.setOnClickListener(v -> {

            if (getContext() == null) return;

            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_avatars);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);

            rvListaAvatares = dialog.findViewById(R.id.rvListaAvatares);
            AppCompatButton btnCancelar = dialog.findViewById(R.id.btnCancelarAvatar);
            AppCompatButton btnGuardar = dialog.findViewById(R.id.btnGuardarAvatar);


            rvListaAvatares.setLayoutManager(new GridLayoutManager(getContext(), 3));

            adapter = new AvatarAdapter(listaAvatares, avatar -> {
                avatarSeleccionado = avatar;
                btnGuardar.setEnabled(true);
            });
            rvListaAvatares.setAdapter(adapter);

            btnCancelar.setOnClickListener(vCancelar -> {
                avatarSeleccionado = null;
                dialog.dismiss();
            });

            btnGuardar.setOnClickListener(vGuardar -> {
                if (avatarSeleccionado != null) {
                    btnGuardar.setEnabled(false);
                    guardarAvatarEnFirebase(dialog);
                }
            });
            if (dialog.getWindow() != null) {
                int ancho = (int) (getResources().getDisplayMetrics().widthPixels * 0.99);
                int alto = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
                dialog.getWindow().setLayout(ancho, alto);
            }

            dialog.show();
        });

        return view;
    }

    /** Dialogo personalizado en el estilo del juego. El correo y la clave nunca se modifican. */
    private void mostrarDialogoEditarNombre(View boton) {
        if (!isAdded() || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_editar_nombre);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);
        EditText campo = dialog.findViewById(R.id.etNuevoNombrePerfil);
        TextView error = dialog.findViewById(R.id.tvErrorNombrePerfil);
        com.google.android.material.button.MaterialButton cancelar = dialog.findViewById(R.id.btnCancelarNombrePerfil);
        com.google.android.material.button.MaterialButton guardar = dialog.findViewById(R.id.btnGuardarNombrePerfil);
        campo.setText(tvUsername.getText());
        campo.setSelection(campo.length());
        campo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        cancelar.setOnClickListener(v -> dialog.dismiss());
        guardar.setOnClickListener(v -> {
            String nombre = com.example.basta_game.utils.NombresUnicos.normalizarPresentacion(
                    campo.getText().toString());
            if (!com.example.basta_game.utils.NombresUnicos.valido(nombre)) {
                error.setText("Usa entre 2 y 24 caracteres, letras, números o espacios");
                error.setVisibility(View.VISIBLE);
                return;
            }
            guardar.setEnabled(false);
            error.setVisibility(View.GONE);
            // Reserva exclusiva por clave normalizada (Roberto == roberto).
            // Si la ruta aun no tiene reglas autorizadas, el cambio se rechaza, nunca se guarda duplicado.
            DatabaseReference usuarios = FirebaseDatabase.getInstance().getReference("usuarios");
            DatabaseReference nuevoIndice = FirebaseDatabase.getInstance()
                    .getReference("nombresVisibles").child(com.example.basta_game.utils.NombresUnicos.clave(nombre));
            nuevoIndice.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull @Override public com.google.firebase.database.Transaction.Result doTransaction(
                        @NonNull com.google.firebase.database.MutableData actual) {
                    String dueño = actual.getValue(String.class);
                    if (dueño != null && !uid.equals(dueño)) return com.google.firebase.database.Transaction.abort();
                    actual.setValue(uid);
                    return com.google.firebase.database.Transaction.success(actual);
                }
                @Override public void onComplete(DatabaseError problema, boolean reservado, DataSnapshot snap) {
                    if (!isAdded() || !dialog.isShowing()) return;
                    if (problema != null || !reservado) {
                        guardar.setEnabled(true);
                        error.setText(problema == null ? "Este nombre ya está ocupado" :
                                "No pudimos comprobar el nombre. Revisa tu conexión o las reglas Firebase");
                        error.setVisibility(View.VISIBLE);
                        return;
                    }
                    usuarios.child(uid).child("nombre").get().addOnCompleteListener(lectura -> {
                        if (!lectura.isSuccessful() || lectura.getResult() == null) {
                            guardar.setEnabled(true);
                            error.setText("No pudimos consultar tu perfil");
                            error.setVisibility(View.VISIBLE);
                            // No retiramos una reserva ya perteneciente a este usuario sin comprobar el perfil.
                            return;
                        }
                        String anterior = lectura.getResult().getValue(String.class);
                        usuarios.child(uid).child("nombre").setValue(nombre).addOnCompleteListener(escritura -> {
                            if (!isAdded() || !dialog.isShowing()) return;
                            guardar.setEnabled(true);
                            if (escritura.isSuccessful()) {
                                tvUsername.setText(nombre);
                                if (anterior != null && !com.example.basta_game.utils.NombresUnicos.clave(anterior)
                                        .equals(com.example.basta_game.utils.NombresUnicos.clave(nombre))) {
                                    com.example.basta_game.utils.NombresUnicos.soltarReservaAnterior(uid, anterior);
                                }
                                dialog.dismiss();
                                NotificationHelper.show(perfilNotification, "Nombre actualizado", NotificationHelper.SUCCESS);
                            } else {
                                error.setText("Error guardando el nombre. Intenta otra vez");
                                error.setVisibility(View.VISIBLE);
                                // No borramos reservas en reintentos inciertos de red; se pueden reutilizar por el mismo UID.
                            }
                        });
                    });
                }
            }, false);
        });
        dialog.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);
        if (dialog.getWindow() != null) {
            int ancho = (int) (getResources().getDisplayMetrics().widthPixels * .88);
            dialog.getWindow().setLayout(ancho, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void inicializarOpcionesAvatares() {
        listaAvatares = new ArrayList<>();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        List<String> nombresArchivos = new ArrayList<>();
        listaAvatares.add(new Avatar("avatar1.png", "Chico one"));
        listaAvatares.add(new Avatar("avatar2.png", "Chico two"));
        listaAvatares.add(new Avatar("avatar3.png", "Ingeniero"));
        listaAvatares.add(new Avatar("avatar4.png", "Chica one"));
        listaAvatares.add(new Avatar("avatar5.png", "nossaJ"));
        listaAvatares.add(new Avatar("avatar6.png", "Chica two"));
        listaAvatares.add(new Avatar("avatar7.png", "Chica three"));
        listaAvatares.add(new Avatar("avatar8.png", "anairA"));
        listaAvatares.add(new Avatar("helicoptero.png", "Helicoptero Apache"));
        listaAvatares.add(new Avatar("hipopotama.png", "Flaca asintomatica"));
        listaAvatares.add(new Avatar("therian.png", "Therian"));

        for (Avatar avatar : listaAvatares) {

            StorageReference storageRef = storage.getReference().child("avatares/" + avatar.getNombreArchivo());
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                avatar.setUrlDescarga(uri.toString());
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(e -> {
                if (getContext() != null) {
                    NotificationHelper.show(
                            perfilNotification,
                            "Error al enlazar: " + avatar.getNombreArchivo(),
                            NotificationHelper.SUCCESS
                    );
                    Sounds.playNotificaciones();
                }
            });
        }
    }
    private void guardarAvatarEnFirebase(Dialog dialog) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || getContext() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        String avatarId = avatarSeleccionado.getNombreArchivo().replace(".png", "");
        String urlDescarga = avatarSeleccionado.getUrlDescarga();

        java.util.HashMap<String, Object> actualizaciones = new java.util.HashMap<>();
        actualizaciones.put("avatarID", avatarId);
        actualizaciones.put("fotoPerfilUrl", urlDescarga);


        userRef.updateChildren(actualizaciones)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.show(
                            perfilNotification,
                            "¡Avatar actualizado con exito!",
                            NotificationHelper.SUCCESS
                    );
                    Sounds.playNotificaciones();

                    Glide.with(PerfilFragment.this)
                            .load(urlDescarga)
                            .into(ivAvatarActual);

                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    NotificationHelper.show(
                            perfilNotification,
                            "Error al guardar avatar.",
                            NotificationHelper.ERROR
                    );
                    Sounds.playNotificaciones();
                    dialog.dismiss();
                });
    }

    private void cargarPerfilUsuario() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || getContext() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null || !snapshot.exists()) return;

                String nombre = snapshot.child("nombre").getValue(String.class);
                String correo = snapshot.child("correo").getValue(String.class);

                Long jugadas = snapshot.child("partidasJugadas").getValue(Long.class);
                Long ganadas = snapshot.child("partidasGanadas").getValue(Long.class);
                Long promedio = snapshot.child("promedioPts").getValue(Long.class);
                String fotoUrl = snapshot.child("fotoPerfilUrl").getValue(String.class);

                tvUsername.setText(nombre != null ? nombre : "Jugador");
                tvCorreo.setText(correo != null ? correo : "");
                tvPartidasJugadas.setText(String.valueOf(jugadas != null ? jugadas : 0));
                tvPartidasGanadas.setText(String.valueOf(ganadas != null ? ganadas : 0));
                tvPromedioPuntos.setText(String.valueOf(promedio != null ? promedio : 0));

                if (fotoUrl != null && !fotoUrl.trim().isEmpty()) {
                    Glide.with(PerfilFragment.this)
                            .load(fotoUrl)
                            .dontAnimate()
                            .into(ivAvatarActual);
                } else {
                    ivAvatarActual.setImageResource(R.drawable.outline_person_24);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    ivAvatarActual.setImageResource(R.drawable.outline_person_24);
                }
            }
        });
    }
}
