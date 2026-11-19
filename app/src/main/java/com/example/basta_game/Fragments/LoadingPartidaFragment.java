package com.example.basta_game.Fragments;

import android.os.*;
import android.view.*;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.basta_game.R;
import com.example.basta_game.utils.PlazoRonda;
import com.example.basta_game.utils.Sounds;
import com.google.firebase.database.*;

/** Welcome follows the persisted preparation deadline, never a per-device ten-second delay. */
public class LoadingPartidaFragment extends Fragment {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DatabaseReference sala,offset;
    private ValueEventListener listener,listenerOffset;
    private DataSnapshot estado;
    private TextView contador;
    private long baseServidor,baseMonotona;
    private boolean relojListo,abierta,sonando;
    private final Runnable tick=this::actualizar;
    @Override public View onCreateView(LayoutInflater i,ViewGroup p,Bundle b) { return i.inflate(R.layout.fragment_loading_partida,p,false); }
    @Override public void onViewCreated(View v,Bundle b) {
        contador=v.findViewById(R.id.txtCuentaRegresiva);contador.setText("Preparando...");
        Bundle datos=getArguments(); if(datos==null) return;
        sala=FirebaseDatabase.getInstance().getReference("salas").child(datos.getString("IDSALA",""));
        offset=FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        listenerOffset=offset.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                Object n=s.getValue();relojListo=n instanceof Number;
                if(relojListo) {baseServidor=System.currentTimeMillis()+((Number)n).longValue();baseMonotona=SystemClock.elapsedRealtime();}
                actualizar();
            }
            @Override public void onCancelled(DatabaseError e) {relojListo=false;contador.setText("Reconectando...");}
        });
        listener=sala.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {estado=s;actualizar();}
            @Override public void onCancelled(DatabaseError e) {contador.setText("No se pudo cargar la partida");}
        });
    }
    private long numero(DataSnapshot s,long defecto) {return s.getValue() instanceof Number ? ((Number)s.getValue()).longValue() : defecto;}
    private void actualizar() {
        handler.removeCallbacks(tick);
        if(!isAdded() || getView()==null || abierta || estado==null || !relojListo) return;
        String partida=getArguments().getString("ID_PARTIDA");
        String actual=estado.child("idPartida").getValue(String.class);if(actual==null) return;
        if(!actual.equals(partida)) getArguments().putString("ID_PARTIDA",actual);
        long inicio=numero(estado.child("inicioRonda"),-1); if(inicio<0) return;
        long fin=PlazoRonda.inicioRespuestas(inicio,(int)numero(estado.child("rondaActual"),1),numero(estado.child("preparacionInicialMs"),0));
        long restante="jugando".equals(estado.child("fase").getValue()) ? fin-(baseServidor+SystemClock.elapsedRealtime()-baseMonotona) : 0;
        if(restante<=0) {
            if(getParentFragmentManager().isStateSaved()) {handler.postDelayed(tick,200);return;}
            abierta=true;Sounds.detenerLoadingPartida();
            PartidaFragment f=new PartidaFragment();f.setArguments(new Bundle(getArguments()));
            getParentFragmentManager().beginTransaction().replace(R.id.sala_fragment_container,f).commit();return;
        }
        if(!sonando) {sonando=true;Sounds.playLoadingPartida();}
        String valor=String.valueOf((restante+999)/1000);
        if(!valor.contentEquals(contador.getText())) {
            contador.setText(valor);contador.setScaleX(1.3f);contador.setScaleY(1.3f);
            contador.animate().scaleX(1).scaleY(1).setDuration(150).start();
        }
        handler.postDelayed(tick,100);
    }
    @Override public void onDestroyView() {
        handler.removeCallbacks(tick);Sounds.detenerLoadingPartida();
        if(sala!=null && listener!=null) sala.removeEventListener(listener);
        if(offset!=null && listenerOffset!=null) offset.removeEventListener(listenerOffset);
        super.onDestroyView();
    }
}