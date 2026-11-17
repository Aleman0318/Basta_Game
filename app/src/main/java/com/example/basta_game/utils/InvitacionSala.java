package com.example.basta_game.utils;

import android.content.*;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import com.example.basta_game.R;

public final class InvitacionSala {
    private InvitacionSala() { }
    public static void colorear(TextView campo,String codigo) {
        if(codigo==null) codigo="";
        SpannableString texto=new SpannableString(codigo);
        for(int i=0;i<codigo.length();i++) if(codigo.charAt(i)=='O') texto.setSpan(new ForegroundColorSpan(Color.rgb(210,28,45)),i,i+1,0);
        campo.setText(texto);campo.setContentDescription(codigo.replace("O"," letra O ").replace("0"," cero "));
    }
    public static void configurar(View vista,String codigo) {
        colorear(vista.findViewById(R.id.tvidSalaLobby),codigo);
        vista.findViewById(R.id.btnCopiarCodigo).setOnClickListener(v -> {
            ClipboardManager portapapeles=(ClipboardManager)v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            portapapeles.setPrimaryClip(ClipData.newPlainText("Invitacion Basta",CodigoSala.invitacion(codigo)));
            NotificationHelper.show(v,"¡Invitación copiada!",NotificationHelper.SUCCESS);
        });
    }
}
