package com.example.basta_game.utils;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.basta_game.models.ComodinTienda;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

/** Retained across fragment/activity recreation; no Views or Activity references. */
public final class ComprasViewModel extends ViewModel {
    public static final MutableLiveData<Boolean> enCurso = new MutableLiveData<>(false);
    private static final ControlCompra control=new ControlCompra();
    public static boolean confirmacionAbierta;
    public final MutableLiveData<DataSnapshot> confirmado = new MutableLiveData<>();

    public void comprar(DatabaseReference usuario, ComodinTienda comodin) {
        if (!ConexionJuego.disponible() || usuario == null || comodin == null || !control.iniciar(android.os.SystemClock.elapsedRealtime())) return;
        enCurso.setValue(true);
        final String uid = usuario.getKey(); final long conexion=ConexionJuego.version();
        usuario.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(!ConexionJuego.vigente(conexion)) return Transaction.abort();
                if (datos.getValue() == null) return Transaction.success(datos);
                return CompraComodin.aplicar(datos,comodin.getId(),comodin.getNombre(),comodin.getTipoMoneda(),comodin.getPrecio())
                        ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot datos) {
                if (datos != null && datos.exists()) confirmado.setValue(datos);
                boolean exito=error==null && committed && datos!=null && datos.exists();
                control.terminar(exito,android.os.SystemClock.elapsedRealtime());
                enCurso.setValue(false);
                if (FirebaseAuth.getInstance().getCurrentUser() == null ||
                        !FirebaseAuth.getInstance().getCurrentUser().getUid().equals(uid)) return;
                if (error != null) NotificationHelper.avisoTienda("No se pudo confirmar la compra. Revisa tu conexion",NotificationHelper.ERROR);
                else if (committed && datos != null && datos.exists()) NotificationHelper.compra(uid,comodin.getNombre());
                else NotificationHelper.avisoTienda("No hay saldo suficiente o el inventario no permite esta compra",NotificationHelper.WARNING);
            }
        }, false);
    }
}
