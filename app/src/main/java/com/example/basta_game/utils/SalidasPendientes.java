package com.example.basta_game.utils;
import android.content.*;
import com.google.firebase.database.*;
/** Durable local ticket. Never retire a newer match or a different session. */
public final class SalidasPendientes {
 private SalidasPendientes(){}
 private static final java.util.Set<String> enCurso=new java.util.HashSet<>();
 public static void guardar(Context c,String sala,String uid,String partida,String sesion){
  guardar(c,sala,uid,partida,sesion,java.util.Collections.singleton(sesion));
 }
 public static void guardar(Context c,String sala,String uid,String partida,String sesion,java.util.Collection<String> conocidas){
  guardar(c,sala,uid,partida,sesion,conocidas,java.util.Collections.singleton(sesion));
 }
 public static void guardar(Context c,String sala,String uid,String partida,String sesion,java.util.Collection<String> conocidas,java.util.Collection<String> propias){
  try{org.json.JSONObject j=new org.json.JSONObject();j.put("sala",sala);j.put("uid",uid);j.put("partida",partida==null?"":partida);j.put("sesion",sesion);j.put("sesionesConocidas",new org.json.JSONArray(conocidas));j.put("sesionesPropias",new org.json.JSONArray(propias));c.getSharedPreferences("salidasPendientes",0).edit().putString(sala+uid,j.toString()).commit();}catch(org.json.JSONException e){android.util.Log.e("SALIDA","No se pudo guardar la salida",e);}
 }
 public static void reconciliar(Context c){
  android.content.SharedPreferences p=c.getSharedPreferences("salidasPendientes",0);
  for(java.util.Map.Entry<String,?> entrada:p.getAll().entrySet())try{
   org.json.JSONObject j=new org.json.JSONObject((String)entrada.getValue());String uid=j.getString("uid"),partida=j.getString("partida"),sesion=j.getString("sesion");
   com.google.firebase.auth.FirebaseUser user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();if(user==null || !uid.equals(user.getUid()))continue;
   java.util.Set<String> conocidas=new java.util.HashSet<>();conocidas.add(sesion);
   org.json.JSONArray lista=j.optJSONArray("sesionesConocidas");if(lista!=null)for(int i=0;i<lista.length();i++)conocidas.add(lista.getString(i));
   java.util.Set<String> propias=new java.util.HashSet<>();propias.add(sesion);
   org.json.JSONArray ids=j.optJSONArray("sesionesPropias");if(ids!=null)for(int i=0;i<ids.length();i++)propias.add(ids.getString(i));
   final String ticket=(String)entrada.getValue();
   synchronized(enCurso){if(!enCurso.add(ticket))continue;}
   FirebaseDatabase.getInstance().getReference("salas").child(j.getString("sala")).runTransaction(new Transaction.Handler(){
    public Transaction.Result doTransaction(MutableData s){
     if(s.getValue()==null)return Transaction.success(s);
     return TransferenciaHost.abandonarSesion(s,uid,partida,sesion,conocidas,propias)?Transaction.success(s):Transaction.abort();
    }
    public void onComplete(DatabaseError e,boolean ok,DataSnapshot s){
     synchronized(enCurso){enCurso.remove(ticket);}
     if(e==null && ticket.equals(p.getString(entrada.getKey(),null)))p.edit().remove(entrada.getKey()).apply();
    }
   },false);
  }catch(org.json.JSONException e){android.util.Log.e("SALIDA","Ticket inválido",e);}
 }
}
