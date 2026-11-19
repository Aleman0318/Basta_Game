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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.Button;
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
import com.google.firebase.database.Query;
import com.example.basta_game.utils.EstadoSalaPublica;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

public class Unirse_salaFragment extends Fragment {

    private EditText etCodigoSala;
    private FirebaseAuth auth;

    private String letraElegida;
    private CodigoSalaSearch codigoElegidoFiltrado;
    private View unirseSalaNotification;
    private AppCompatButton btnIngresoManual;
    private LinearLayout listadoSalasPublicas;
    private TextView tvEstadoSalasPublicas;
    private Query consultaPublicas;
    private ValueEventListener escuchaIndice;
    private final Map<String, DatabaseReference> referenciasSalasPublicas = new LinkedHashMap<>();
    private final Map<String, ValueEventListener> escuchasSalasPublicas = new LinkedHashMap<>();

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
        btnIngresoManual = btnUnirseSalaFinal;
        listadoSalasPublicas = view.findViewById(R.id.listadoSalasPublicas);
        tvEstadoSalasPublicas = view.findViewById(R.id.tvEstadoSalasPublicas);

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

    @Override public void onStart() {
        super.onStart();
        iniciarListadoPublico();
    }

    @Override public void onStop() {
        cerrarListadoPublico();
        super.onStop();
    }

    @Override public void onDestroyView() {
        cerrarListadoPublico();
        listadoSalasPublicas = null;
        tvEstadoSalasPublicas = null;
        btnIngresoManual = null;
        super.onDestroyView();
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable fondoTarjeta(int color, int borde) {
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(color);
        fondo.setCornerRadius(dp(18));
        fondo.setStroke(dp(2), borde);
        return fondo;
    }

    private TextView texto(String valor, int sp, int color, boolean negrita) {
        TextView tv = new TextView(requireContext());
        tv.setText(valor);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (negrita) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private void iniciarListadoPublico() {
        if (listadoSalasPublicas == null || escuchaIndice != null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvEstadoSalasPublicas.setText("Inicia sesión para ver las salas públicas.");
            return;
        }
        tvEstadoSalasPublicas.setText("Buscando salas públicas…");
        consultaPublicas = FirebaseDatabase.getInstance().getReference("salasPublicas");
        escuchaIndice = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot indice) {
                if (!isAdded() || listadoSalasPublicas == null) return;
                // Nunca dejamos listeners duplicados al actualizar el índice.
                cerrarEscuchasDeSalas();
                listadoSalasPublicas.removeAllViews();
                int total = (int) indice.getChildrenCount();
                tvEstadoSalasPublicas.setText(total == 0 ? "No hay salas públicas todavía. ¡Crea una!" :
                        "Salas recientes (" + total + ") · Actualizando en vivo");
                ArrayList<DataSnapshot> fichas = new ArrayList<>();
                for (DataSnapshot ficha : indice.getChildren()) fichas.add(ficha);
                java.util.Collections.reverse(fichas);
                for (DataSnapshot ficha : fichas) {
                    String idSala = ficha.getKey();
                    String codigo = ficha.child("codigoSala").getValue(String.class);
                    String anfitrion = ficha.child("hostNombre").getValue(String.class);
                    if (idSala == null || codigo == null || codigo.length() != 6) continue;
                    insertarSalaPublica(idSala, codigo, anfitrion == null ? "Anfitrión" : anfitrion);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (tvEstadoSalasPublicas != null) {
                    tvEstadoSalasPublicas.setText("No se pudieron consultar las salas públicas (permisos o conexión).");
                }
            }
        };
        consultaPublicas.addValueEventListener(escuchaIndice);
    }

    /** La interfaz solo lee Firebase; el ingreso sigue utilizando el flujo seguro existente. */
    private void insertarSalaPublica(String idSala, String codigo, String anfitrion) {
        final int azul=Color.rgb(25,57,221), tinta=Color.rgb(29,39,65);
        final int naranja=Color.rgb(236,94,21), verde=Color.rgb(29,119,70);
        // El tablero es una tarjeta de papel del cuaderno, no una pantalla distinta.
        LinearLayout tarjeta=new LinearLayout(requireContext());
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dp(14),dp(14),dp(14),dp(14));
        tarjeta.setBackground(fondoTarjeta(Color.rgb(255,253,249),Color.rgb(38,73,225)));
        tarjeta.setElevation(dp(4));
        LinearLayout.LayoutParams margenes=new LinearLayout.LayoutParams(-1,-2);
        margenes.bottomMargin=dp(14);
        listadoSalasPublicas.addView(tarjeta,margenes);

        LinearLayout cabecera=new LinearLayout(requireContext());
        cabecera.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cabecera.setOrientation(LinearLayout.HORIZONTAL);
        tarjeta.addView(cabecera,new LinearLayout.LayoutParams(-1,-2));
        TextView titulo=texto("SALA  " + codigo,22,azul,true);
        titulo.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.lilitaone_regular));
        titulo.setSingleLine(true);
        titulo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cabecera.addView(titulo,new LinearLayout.LayoutParams(0,-2,1));
        TextView etiqueta=texto("PÚBLICA",11,Color.WHITE,true);
        etiqueta.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.manrope_regular));
        etiqueta.setGravity(android.view.Gravity.CENTER);
        etiqueta.setPadding(dp(10),dp(6),dp(10),dp(6));
        etiqueta.setBackground(fondoTarjeta(naranja,naranja));
        cabecera.addView(etiqueta);

        View separador=new View(requireContext());
        separador.setBackgroundColor(Color.rgb(226,233,252));
        LinearLayout.LayoutParams sepLp=new LinearLayout.LayoutParams(-1,dp(1));
        sepLp.topMargin=dp(10);sepLp.bottomMargin=dp(10);
        tarjeta.addView(separador,sepLp);

        LinearLayout filaDetalles=new LinearLayout(requireContext());
        filaDetalles.setGravity(android.view.Gravity.CENTER_VERTICAL);
        filaDetalles.setOrientation(LinearLayout.HORIZONTAL);
        tarjeta.addView(filaDetalles,new LinearLayout.LayoutParams(-1,-2));
        TextView detalle=texto("ANFITRIÓN  ·  " + anfitrion,13,tinta,true);
        detalle.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.manrope_regular));
        detalle.setSingleLine(true);
        detalle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        filaDetalles.addView(detalle,new LinearLayout.LayoutParams(-1,-2));
        TextView estado=texto("Consultando cupos...",14,azul,true);
        estado.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.manrope_regular));
        LinearLayout.LayoutParams estadoLp=new LinearLayout.LayoutParams(-1,-2);
        estadoLp.topMargin=dp(9); tarjeta.addView(estado,estadoLp);

        android.widget.ProgressBar cupos=new android.widget.ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);
        cupos.setMax(100);cupos.setProgress(0);
        cupos.setProgressTintList(android.content.res.ColorStateList.valueOf(azul));
        cupos.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(218,229,255)));
        LinearLayout.LayoutParams cuposLp=new LinearLayout.LayoutParams(-1,dp(7));
        cuposLp.topMargin=dp(9);tarjeta.addView(cupos,cuposLp);

        AppCompatButton unirse=new AppCompatButton(requireContext());
        unirse.setText("¡UNIRME!");
        unirse.setTextColor(Color.WHITE);
        unirse.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.lilitaone_regular));
        unirse.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
        unirse.setBackground(fondoTarjeta(azul,azul));
        unirse.setEnabled(false);
        LinearLayout.LayoutParams botonLp=new LinearLayout.LayoutParams(-1,dp(49));
        botonLp.topMargin=dp(12);tarjeta.addView(unirse,botonLp);
        unirse.setOnClickListener(v -> {
            if(etCodigoSala==null || btnIngresoManual==null || !unirse.isEnabled()) return;
            etCodigoSala.setText(codigo);btnIngresoManual.performClick();
        });
        DatabaseReference referencia=FirebaseDatabase.getInstance().getReference("salas").child(idSala);
        ValueEventListener escucha=new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                if(!isAdded() || listadoSalasPublicas==null) return;
                if(!sala.exists() || !Boolean.TRUE.equals(sala.child("publica").getValue(Boolean.class))) {
                    listadoSalasPublicas.removeView(tarjeta);return;
                }
                String estadoSala=sala.child("estadoPartida").getValue(String.class);
                Object maximo=sala.child("maxJugadores").getValue();
                int max=maximo instanceof Number?((Number)maximo).intValue():0;
                int actual=(int)sala.child("jugadores").getChildrenCount();
                boolean disponible=EstadoSalaPublica.permiteUnirse(estadoSala,actual,max);
                String textoEstado=EstadoSalaPublica.etiqueta(estadoSala,actual,max);
                estado.setText(actual + " / " + max + " jugadores     •     " + textoEstado);
                estado.setTextColor(disponible?verde:naranja);
                cupos.setProgress(max>0?Math.min(100,Math.round(100f*actual/max)):0);
                cupos.setProgressTintList(android.content.res.ColorStateList.valueOf(disponible?azul:naranja));
                unirse.setEnabled(disponible);unirse.setAlpha(disponible?1f:0.75f);
                unirse.setText(disponible?"¡UNIRME!":EstadoSalaPublica.enLobby(estadoSala)?"SALA LLENA":"EN PARTIDA");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                estado.setText("No se pudo consultar la sala");estado.setTextColor(naranja);
                unirse.setEnabled(false);
            }
        };
        referenciasSalasPublicas.put(idSala,referencia);
        escuchasSalasPublicas.put(idSala,escucha);
        referencia.addValueEventListener(escucha);
    }

    private void cerrarEscuchasDeSalas() {
        for (Map.Entry<String, ValueEventListener> entrada : escuchasSalasPublicas.entrySet()) {
            DatabaseReference referencia = referenciasSalasPublicas.get(entrada.getKey());
            if (referencia != null) referencia.removeEventListener(entrada.getValue());
        }
        referenciasSalasPublicas.clear();
        escuchasSalasPublicas.clear();
    }

    private void cerrarListadoPublico() {
        if (consultaPublicas != null && escuchaIndice != null)
            consultaPublicas.removeEventListener(escuchaIndice);
        escuchaIndice = null;
        consultaPublicas = null;
        cerrarEscuchasDeSalas();
    }
}
