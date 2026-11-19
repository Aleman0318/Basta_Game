package com.example.basta_game.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import androidx.annotation.NonNull;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

/** Clave determinista case-insensitive; no requiere leer todos los perfiles. */
public final class NombresUnicos {
    private NombresUnicos() {}
    public static String normalizarPresentacion(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }
    public static boolean valido(String nombre) {
        return nombre != null && nombre.length() >= 2 && nombre.length() <= 24
                && nombre.matches("[\\p{L}\\p{N} _-]+");
    }
    public static String clave(String valor) {
        String canonico = java.text.Normalizer.normalize(
                normalizarPresentacion(valor).toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFKC);
        // Hexadecimal permite cualquier carácter Unicode sin caracteres prohibidos por RTDB.
        byte[] bytes = canonico.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        final char[] hex = "0123456789abcdef".toCharArray();
        for (byte b : bytes) {sb.append(hex[(b & 255) >>> 4]);sb.append(hex[b & 15]);}
        return sb.toString();
    }
    public static void soltarReservaAnterior(String uid, String anterior) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("nombresVisibles").child(clave(anterior));
        ref.runTransaction(new Transaction.Handler() {
            @NonNull public Transaction.Result doTransaction(@NonNull MutableData dato) {
                return uid.equals(dato.getValue(String.class)) ? Transaction.success(datoConValorNulo(dato)) : Transaction.abort();
            }
            private MutableData datoConValorNulo(MutableData dato) {dato.setValue(null); return dato;}
            public void onComplete(DatabaseError e, boolean ok, DataSnapshot dato) {}
        }, false);
    }
}
