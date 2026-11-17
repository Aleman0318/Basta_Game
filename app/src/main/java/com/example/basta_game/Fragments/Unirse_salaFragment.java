package com.example.basta_game.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.R;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.CodigoSalaSearch;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.example.basta_game.utils.IdentidadJugador;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class Unirse_salaFragment extends Fragment {

    private EditText etCodigoSala;
    private FirebaseAuth auth;

    private String letraElegida;
    private CodigoSalaSearch codigoElegidoFiltrado;
    private View unirseSalaNotification;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unirse_sala, container, false);

        ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStack();
            }
        });

        etCodigoSala = view.findViewById(R.id.etSalaId);

        etCodigoSala.setFilters(new InputFilter[] {
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(6)
        });

        unirseSalaNotification = view.findViewById(R.id.notificacionUnirseSala);
        auth = FirebaseAuth.getInstance();

        AppCompatButton btnUnirseSalaFinal = view.findViewById(R.id.btnUnirseSalaFinal);

        Animations.animacionAparicion(etCodigoSala);
        Animations.animacionAparicion(btnUnirseSalaFinal);

        btnUnirseSalaFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!com.example.basta_game.utils.ConexionJuego.disponible()) return;
                final long conexion=com.example.basta_game.utils.ConexionJuego.version();
                if(auth != null && VerificacionesAuth.VerificarInicioSesion(auth)){
                    if(!com.example.basta_game.utils.CargaBoton.iniciar(btnUnirseSalaFinal))return;
        final Object operacion=com.example.basta_game.utils.CargaBoton.token(btnUnirseSalaFinal);
                    String codigoSalaIngresado = etCodigoSala.getText().toString();

                    DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("codigosSalas");
                    nodoSalas.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(!isAdded() || getView()==null || !com.example.basta_game.utils.CargaBoton.activa(btnUnirseSalaFinal,operacion))return;
                            String codigoSalaUsuario = etCodigoSala.getText().toString();
                            boolean encontrado = false;
                            for (DataSnapshot hijo: snapshot.getChildren()) {
                                if(hijo!=null){
                                    CodigoSalaSearch codigoElegido = hijo.getValue(CodigoSalaSearch.class);
                                    if(codigoElegido.getCodigoSala().equals(codigoSalaUsuario) && codigoElegido.getEstaActiva()){
                                        encontrado = true;
                                        codigoElegidoFiltrado = codigoElegido;
                                    }
                                }
                            }

                            if(!encontrado){
                                com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion);
                                NotificationHelper.show(
                                        unirseSalaNotification,
                                        "No se encontro la sala requerida",
                                        NotificationHelper.ERROR
                                );
                                Sounds.playNotificaciones();
                            }else{
                                DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala());
                                nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(!isAdded() || getView()==null || !com.example.basta_game.utils.CargaBoton.activa(btnUnirseSalaFinal,operacion))return;
                                        Sala sala = snapshot.getValue(Sala.class);
                                        if(sala==null){com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion);NotificationHelper.showGlobal("La sala ya no está disponible",NotificationHelper.WARNING);return;}

                                        DatabaseReference nodoJugadoresSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala()).child("jugadores");
                                        nodoJugadoresSala.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(!isAdded() || getView()==null || !com.example.basta_game.utils.CargaBoton.activa(btnUnirseSalaFinal,operacion))return;
                                                ArrayList<JugadorSala> jugadoresEnSala = new ArrayList<>();

                                                for (DataSnapshot jugadorSnapshot : snapshot.getChildren()) {
                                                    JugadorSala jugadorSala = jugadorSnapshot.getValue(JugadorSala.class);

                                                    if (jugadorSala != null) {
                                                        jugadoresEnSala.add(jugadorSala);
                                                    }
                                                }

                                                if (snapshot.hasChild(auth.getCurrentUser().getUid()) ||
                                                        jugadoresEnSala.size() < sala.getMaxJugadores()) {
                                                    String uidUsuario = auth.getCurrentUser().getUid();
                                                    DatabaseReference nodoUsuarios = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario);
                                                    nodoUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(!isAdded() || getView()==null || !com.example.basta_game.utils.CargaBoton.activa(btnUnirseSalaFinal,operacion))return;
                                                            Usuario user = snapshot.getValue(Usuario.class);
                                                            if (!isAdded() || getView() == null) return;
                                                            if (user == null || !IdentidadJugador.nombreValido(user.getNombre())) {
                                                                com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion);
                                                                NotificationHelper.show(unirseSalaNotification,
                                                                        "Cargando perfil. No se pudo confirmar tu identidad; reintenta al conectar",
                                                                        NotificationHelper.WARNING);
                                                                return;
                                                            }
                                                            letraElegida = sala.getLetraActual();
                                                            DatabaseReference nodoJugadorSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala());
                                                            nodoJugadorSala.runTransaction(new Transaction.Handler() {
                                                                @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData jugador) {
                                                                    if(!com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                                                                    if(jugador.getValue()==null) return Transaction.success(jugador);
                                                                    return com.example.basta_game.utils.IngresoSala.ingresar(jugador, uidUsuario, user.getNombre(), user.getFotoPerfilUrl(),com.example.basta_game.utils.ConexionJuego.ahoraServidor(),90000)
                                                                            ? Transaction.success(jugador) : Transaction.abort();
                                                                }
                                                                @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot jugador) {
                                                                if(!com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion))return;
                                                                if(error == null && committed && isAdded()){



                                                                    Intent intent = new Intent(requireContext(), SalaActivity.class);
                                                                    Bundle bundle = new Bundle();

                                                                    bundle.putBoolean("ES_ANFITRION", false);
                                                                    bundle.putStringArrayList("CATEGORIAS", sala.getCategoriasActivas());
                                                                    bundle.putInt("TIEMPO", sala.getTiempoRonda());
                                                                    bundle.putInt("JUGADORES", sala.getMaxJugadores());
                                                                    bundle.putInt("RONDAS", sala.getCantidadRondas());
                                                                    bundle.putBoolean("CREADOR", false);
                                                                    bundle.putBoolean("ANEXO", true);
                                                                    bundle.putString("CODIGO_SALA", sala.getCodigoSala());
                                                                    bundle.putString("IDSALA", sala.getIdSala());
                                                                    bundle.putString("LETRA", letraElegida);

                                                                    intent.putExtra("DATOS_SALA", bundle);

                                                                    com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion);
                                                                    startActivity(intent);
                                                                } else if (isAdded()) {
                                                                    NotificationHelper.show(unirseSalaNotification,
                                                                            "No se pudo recuperar tu lugar en la sala. Reintenta al conectar",
                                                                            NotificationHelper.ERROR);
                                                                }
                                                                }
                                                            }, false);
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                            if(com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion))NotificationHelper.showGlobal("No se pudo consultar la sala. Revisa tu conexión o sesión",NotificationHelper.ERROR);

                                                        }
                                                    });
                                                } else {
                                                    com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion);
                                                    NotificationHelper.show(
                                                            unirseSalaNotification,
                                                            "La sala está llena",
                                                            NotificationHelper.ERROR
                                                    );
                                                    Sounds.playNotificaciones();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                            if(com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion))NotificationHelper.showGlobal("No se pudo consultar la sala. Revisa tu conexión o sesión",NotificationHelper.ERROR);

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                            if(com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion))NotificationHelper.showGlobal("No se pudo consultar la sala. Revisa tu conexión o sesión",NotificationHelper.ERROR);

                                    }
                                });
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if(com.example.basta_game.utils.CargaBoton.terminar(btnUnirseSalaFinal,operacion))NotificationHelper.showGlobal("No se pudo consultar la sala. Revisa tu conexión o sesión",NotificationHelper.ERROR);

                        }
                    });
                }
            }
        });

        return view;
    }
}
