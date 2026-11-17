package com.example.basta_game.emulator;

import com.example.basta_game.utils.*;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.*;
import static org.junit.Assert.*;

/** Real RTDB Emulator transactions. No Android lifecycle or deployed rules are tested. */
public class ConcurrenciaEmulatorTest {
    @Test public void admisionCasosABCD() throws Exception {
        for(int caso=0;caso<4;caso++){
            Map<String,Object> d=sala();d.put("maxJugadores",5);d.put("fase",caso==0?"lobby":"jugando");d.put("estadoPartida",caso==0?"En espera":"En juego");
            if(caso<2){Map<String,Object> j=new HashMap<>((Map<String,Object>)d.get("jugadores"));j.put("quinto",Map.of("estaConectado",true));d.put("jugadores",j);}
            semilla("sala",d);String uid=caso==3?"b":"nuevo";
            Resultado r=esperar(transaccion(ref(a,"sala"),x -> IngresoSala.ingresar(x,uid,"Nombre","foto",System.currentTimeMillis(),90000),new AtomicInteger(),null));
            assertEquals("Caso "+caso,caso==3,r.committed());assertEquals(caso<2?5:4,leer(ref(seed,"sala/jugadores")).getChildrenCount());
        }
    }
    @Test public void categoriaAutomaticaConcurrenteTieneUnSoloPlazoFinal() throws Exception {
        Map<String,Object> datos=sala();datos.put("categoriasActivas",List.of("Animal"));datos.put("letraActual","A");
        datos.put("respuestas",Map.of("ronda_2",Map.of("h",Map.of("Animal","Abeja"),"d",Map.of("Animal",""),"b",Map.of("Animal","abeja"),"c",Map.of("Animal","Ox"))));
        datos.remove("validaciones");datos.remove("revisionCategorias");semilla("sala",datos);
        var x=transaccion(ref(a,"sala"),d -> RelojRevision.coordinar(d,"partida1",2,"b",System.currentTimeMillis()),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),d -> RelojRevision.coordinar(d,"partida1",2,"c",System.currentTimeMillis()),new AtomicInteger(),null);
        assertEquals(1,(esperar(x).committed()?1:0)+(esperar(y).committed()?1:0));
        DataSnapshot r=leer(ref(seed,"sala/revisionCategorias/ronda_2/reloj"));assertEquals("resultado",r.child("modo").getValue());assertEquals(3000L,r.child("duracionMs").getValue());
        long inicio=((Number)r.child("inicio").getValue()).longValue();
        assertFalse(esperar(transaccion(ref(a,"sala"),d -> RelojRevision.coordinar(d,"partida1",2,"b",inicio+2999),new AtomicInteger(),null)).committed());
        x=transaccion(ref(a,"sala"),d -> RelojRevision.coordinar(d,"partida1",2,"b",inicio+3000),new AtomicInteger(),null);
        y=transaccion(ref(b,"sala"),d -> RelojRevision.coordinar(d,"partida1",2,"c",inicio+3000),new AtomicInteger(),null);
        assertEquals(1,(esperar(x).committed()?1:0)+(esperar(y).committed()?1:0));
        assertEquals(true,leer(ref(seed,"sala/revisionCategorias/ronda_2/completa")).getValue());
    }
    @Test public void expirarSeleccionYCrearVotoConcurrentesSonExcluyentes() throws Exception {
        Map<String,Object> datos=sala();datos.put("categoriasActivas",List.of("Animal"));
        datos.put("revisionCategorias",Map.of("ronda_2",Map.of("indiceActual",0,"reloj",Map.of(
                "idPartida","partida1","indice",0,"categoria","Animal","turno",2,"modo","propuestas","inicio",1000))));
        semilla("sala",datos);
        Map<String,Object> propuesta=Map.of("idVotacion","manual","claveVotacion",ClaveRevision.de(2,"h","Animal","Abeja"),
                "activa",true,"uidObjetivo","h","categoria","Animal","respuesta","Abeja","turnoRevision",2);
        var fin=transaccion(ref(a,"sala"),s -> RelojRevision.coordinar(s,"partida1",2,"c",6000),new AtomicInteger(),null);
        var voto=transaccion(ref(b,"sala"),s -> VotacionRevision.crear(s,"partida1",2,"b",propuesta,5999),new AtomicInteger(),null);
        assertNotEquals(esperar(fin).committed(),esperar(voto).committed());
        DataSnapshot resultado=leer(ref(seed,"sala"));
        assertNotEquals("resultado".equals(resultado.child("revisionCategorias/ronda_2/reloj/modo").getValue()),
                Boolean.TRUE.equals(resultado.child("votacionActiva/ronda_2/activa").getValue()));
    }
    @Test public void ultimaPlazaSoloAdmiteUnUIDYReconexionNoDuplicaCupo() throws Exception {
        Map<String,Object> datos=sala();datos.put("fase","lobby");datos.put("estadoPartida","En espera");datos.put("maxJugadores",5);
        semilla("sala",datos);
        var x=transaccion(ref(a,"sala"),s -> IngresoSala.ingresar(s,"x","Diana","foto"),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> IngresoSala.ingresar(s,"y","Diego","foto"),new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        assertEquals(5,leer(ref(seed,"sala/jugadores")).getChildrenCount());
        assertTrue(esperar(transaccion(ref(a,"sala"),s -> IngresoSala.ingresar(s,"h","Host","foto"),new AtomicInteger(),null)).committed());
        assertEquals(5,leer(ref(seed,"sala/jugadores")).getChildrenCount());
    }
    @Test public void hostAusenteDosClientesPublicanResumenUnaVezYContinuan() throws Exception {
        Map<String,Object> datos=sala();datos.put("cantidadRondas",5);datos.put("categoriasActivas",List.of("Animal"));
        datos.put("revisionCategorias",Map.of("ronda_2",Map.of("completa",true)));
        datos.put("validaciones",Map.of("ronda_2",Map.of("b",Map.of("Animal",Map.of("puntos",10)))));
        semilla("sala",datos);ref(seed,"sala/jugadores/h/estaConectado").setValueAsync(false).get();
        var x=transaccion(ref(a,"sala"),s -> CierreRevision.aplicar(s,"partida1",2,"b"),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> CierreRevision.aplicar(s,"partida1",2,"c"),new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        assertEquals(10L,leer(ref(seed,"sala/jugadores/b/puntajePartida")).getValue());
        assertTrue(esperar(transaccion(ref(a,"sala"),TransferenciaHost::reconciliar,new AtomicInteger(),null)).committed());
        assertEquals("b",leer(ref(seed,"sala/hostUID")).getValue());
        ref(seed,"sala/revisionLista/ronda_2").setValueAsync(Map.of("b",true,"c",true,"d",true)).get();
        assertTrue(esperar(transaccion(ref(b,"sala"),s -> InicioPartida.avanzar(s,"b","partida1",2,Map.of("rondaActual",3,"fase","jugando")),new AtomicInteger(),null)).committed());
        ref(seed,"sala/jugadores/h/estaConectado").setValueAsync(true).get();
        esperar(transaccion(ref(a,"sala"),TransferenciaHost::reconciliar,new AtomicInteger(),null));
        assertEquals("b",leer(ref(seed,"sala/hostUID")).getValue());assertEquals(3L,leer(ref(seed,"sala/rondaActual")).getValue());
    }
    @Test public void dosClientesNoRepitenUnaRuletaConfirmada() throws Exception {
        Map<String,Object> datos=sala();datos.put("fase","revision_puntuaciones");
        datos.put("puntajesRondas",Map.of("ronda_2",Map.of("b",20,"c",10,"d",15,"h",5)));
        semilla("sala",datos);
        Cambio efecto=s -> EfectoRuleta.aplicar(s,2,"h","Host","ruleta-unica",0,.1,.2);
        var x=transaccion(ref(a,"sala"),efecto,new AtomicInteger(),null);var y=transaccion(ref(b,"sala"),efecto,new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        assertEquals(12L,leer(ref(seed,"sala/puntajesRondas/ronda_2/b")).getValue());
        assertTrue(leer(ref(seed,"sala/comodinesResultados/ronda_2")).hasChild("ruleta-unica"));
    }
    @Test public void dosSolicitudesDeRespuestasDiferentesSoloAbrenUnDialogo() throws Exception {
        Map<String,Object> datos=sala();datos.put("categoriasActivas",List.of("Animal"));
        datos.put("revisionCategorias",Map.of("ronda_2",Map.of("indiceActual",0,"reloj",Map.of(
                "idPartida","partida1","indice",0,"categoria","Animal","turno",2,"modo","propuestas","inicio",1000))));
        semilla("sala",datos);
        Map<String,Object> pa=Map.of("idVotacion","a","claveVotacion",ClaveRevision.de(2,"h","Animal","Abeja"),
                "activa",true,"uidObjetivo","h","categoria","Animal","respuesta","Abeja","turnoRevision",2);
        Map<String,Object> pb=Map.of("idVotacion","b","claveVotacion",ClaveRevision.de(2,"b","Animal","Ardilla"),
                "activa",true,"uidObjetivo","b","categoria","Animal","respuesta","Ardilla","turnoRevision",2);
        var x=transaccion(ref(a,"sala"),s -> VotacionRevision.crear(s,"partida1",2,"h",pa,1500),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> VotacionRevision.crear(s,"partida1",2,"b",pb,1500),new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        assertEquals("votos",leer(ref(seed,"sala/revisionCategorias/ronda_2/reloj/modo")).getValue());
        assertEquals(1000L,leer(ref(seed,"sala/revisionCategorias/ronda_2/reloj/seleccionInicio")).getValue());
    }
    @Test public void expulsionConcurrenteConEntradaNoPermiteReaparicion() throws Exception {
        Map<String,Object> datos=sala();datos.put("estadoPartida","En espera");datos.put("fase","espera");datos.put("maxJugadores",8);
        semilla("sala",datos);
        var entrada=transaccion(ref(a,"sala"),s -> IngresoSala.ingresar(s,"b","Diego","foto"),new AtomicInteger(),null);
        var salida=transaccion(ref(b,"sala"),s -> InicioPartida.expulsarEnEspera(s,"h","b"),new AtomicInteger(),null);
        esperar(entrada);assertTrue(esperar(salida).committed());
        assertFalse(esperar(transaccion(ref(a,"sala"),s -> IngresoSala.ingresar(s,"b","Diego","vieja"),new AtomicInteger(),null)).committed());
        assertFalse(leer(ref(seed,"sala/jugadores")).hasChild("b"));
        assertEquals(true,leer(ref(seed,"sala/expulsados/b")).getValue());
    }
    @Test public void saltarinesConcurrentesMismoUsoNoDuplicanTransferencia() throws Exception {
        Map<String,Object> datos=sala();datos.put("fase","revision_puntuaciones");
        datos.put("puntajesRondas",Map.of("ronda_2",Map.of("b",3,"c",10,"d",0,"h",50)));
        semilla("sala",datos);
        Cambio efecto=s -> PuntosSaltarines.aplicar(s,2,"h","Host","uso",0,0);
        var x=transaccion(ref(a,"sala"),efecto,new AtomicInteger(),null);var y=transaccion(ref(b,"sala"),efecto,new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        assertEquals(0L,leer(ref(seed,"sala/puntajesRondas/ronda_2/b")).getValue());
        assertEquals(13L,leer(ref(seed,"sala/puntajesRondas/ronda_2/c")).getValue());
        assertEquals(3L,leer(ref(seed,"sala/comodinesResultados/ronda_2/uso/cantidad")).getValue());
    }
    @Test public void dosPublicacionesDeDueloCompartenUnaSeleccionSinRepetir() throws Exception {
        Map<String,Object> datos=sala();datos.put("fase","revision_puntuaciones");datos.put("ultimoMinijuego","tres_en_raya");
        semilla("sala",datos);
        var x=transaccion(ref(a,"sala"),s -> SeleccionMinijuego.publicar(s,2,"h","b","H","B",0,100,"a"),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> SeleccionMinijuego.publicar(s,2,"c","d","C","D",1,200,"b"),new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());
        Object tipo=leer(ref(a,"sala/minijuegoActual/tipoJuego")).getValue();
        assertNotEquals("tres_en_raya",tipo);assertEquals(tipo,leer(ref(b,"sala/minijuegoActual/tipoJuego")).getValue());
        assertTrue(leer(ref(seed,"sala/minijuegoActual/anuncioDesde")).getValue() instanceof Number);
    }
    @Test public void colisionRealDeCodigoSoloReservaUnaSalaYNoLiberaOtra() throws Exception {
        semilla("codigos",Map.of("conservar",true));
        CompletableFuture<Resultado> uno=transaccion(ref(a,"codigos"),d -> ReservaCodigo.reservar(d.child("ABC123"),"ABC123","salaA"),new AtomicInteger(),null);
        CompletableFuture<Resultado> dos=transaccion(ref(b,"codigos"),d -> ReservaCodigo.reservar(d.child("ABC123"),"ABC123","salaB"),new AtomicInteger(),null);
        Resultado x=esperar(uno), y=esperar(dos);assertNull(x.error());assertNull(y.error());
        assertEquals(1,(x.committed()?1:0)+(y.committed()?1:0));
        String ganadora=leer(ref(seed,"codigos/ABC123/idSala")).getValue(String.class);
        String perdedora="salaA".equals(ganadora) ? "salaB" : "salaA";
        assertFalse(esperar(transaccion(ref(b,"codigos"),d -> ReservaCodigo.liberar(d.child("ABC123"),perdedora),new AtomicInteger(),null)).committed());
        assertTrue(esperar(transaccion(ref(a,"codigos"),d -> ReservaCodigo.activar(d.child("ABC123"),ganadora),new AtomicInteger(),null)).committed());
        assertEquals(ganadora,leer(ref(seed,"codigos/ABC123/idSala")).getValue());
        assertEquals(true,leer(ref(seed,"codigos/conservar")).getValue());
    }
    @Test public void bastaConcurrentePublicaUnaMarcaServidorYDiezSegundos() throws Exception {
        long ahora=System.currentTimeMillis();
        semilla("sala",Map.of("idPartida","p","rondaActual",1,"fase","jugando","inicioRonda",ahora-41000,
                "preparacionInicialMs",10000,"tiempoRonda",60,"jugadores",Map.of("a",Map.of("estaConectado",true),"b",Map.of("estaConectado",true))));
        CompletableFuture<Resultado> uno=transaccion(ref(a,"sala"),d -> PlazoRonda.aceptarBasta(d,"p",1,"a",System.currentTimeMillis()),new AtomicInteger(),null);
        CompletableFuture<Resultado> dos=transaccion(ref(b,"sala"),d -> PlazoRonda.aceptarBasta(d,"p",1,"b",System.currentTimeMillis()),new AtomicInteger(),null);
        assertEquals(1,(esperar(uno).committed()?1:0)+(esperar(dos).committed()?1:0));
        DataSnapshot s=leer(ref(seed,"sala"));long marca=((Number)s.child("bastaTimeStamp").getValue()).longValue();
        assertEquals(marca+10000,PlazoRonda.calcularFin(ahora-1000,60,marca));
    }
    private static final String PROJECT = "demo-basta-concurrency";
    private static final String URL = "https://demo-basta-concurrency-default-rtdb.firebaseio.com";
    private static final List<FirebaseApp> APPS = new ArrayList<>();
    private static FirebaseDatabase seed, a, b;
    private String run;

    @BeforeClass public static void iniciar() {
        assertEquals("Refusing any nonlocal database", "127.0.0.1:9000",
                System.getenv("FIREBASE_DATABASE_EMULATOR_HOST"));
        assertTrue(PROJECT.startsWith("demo-"));
        String project = System.getenv("GCLOUD_PROJECT");
        if (project != null) assertEquals(PROJECT, project);
        seed = cliente("fixture", true);
        a = cliente("cliente-a", false);
        b = cliente("cliente-b", false);
    }

    private static FirebaseDatabase cliente(String nombre, boolean admin) {
        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setProjectId(PROJECT).setDatabaseUrl(URL)
                .setCredentials(GoogleCredentials.create(new AccessToken("owner", new Date(System.currentTimeMillis()+3600000))));
        // Null override deliberately removes admin privileges: local fixture rules apply.
        if (!admin) options.setDatabaseAuthVariableOverride(null);
        FirebaseApp app = FirebaseApp.initializeApp(options.build(), nombre);
        APPS.add(app);
        return FirebaseDatabase.getInstance(app);
    }

    @Before public void aislarCaso() { run = UUID.randomUUID().toString(); }
    private DatabaseReference ref(FirebaseDatabase db, String path) {
        return db.getReference("runs/"+run+"/"+path);
    }
    @After public void limpiarSoloCasoLocal() throws Exception {
        a.goOnline(); b.goOnline();
        seed.getReference("runs/"+run).removeValueAsync().get(15, TimeUnit.SECONDS);
        seed.getReference("denied/"+run).removeValueAsync().get(15, TimeUnit.SECONDS);
    }
    @AfterClass public static void cerrar() { for (FirebaseApp app : APPS) app.delete(); }

    private static DataSnapshot leer(DatabaseReference ref) throws Exception {
        CompletableFuture<DataSnapshot> f = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) { f.complete(s); }
            @Override public void onCancelled(DatabaseError e) { f.completeExceptionally(e.toException()); }
        });
        return f.get(15, TimeUnit.SECONDS);
    }

    private interface Cambio { boolean aplicar(MutableData datos); }
    private record Resultado(DatabaseError error, boolean committed, DataSnapshot snapshot) { }
    private static CompletableFuture<Resultado> transaccion(DatabaseReference ref, Cambio cambio,
                                                            AtomicInteger intentos, Consumer<MutableData> antes) {
        CompletableFuture<Resultado> f = new CompletableFuture<>();
        ref.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                intentos.incrementAndGet();
                if (datos.getValue() == null) return Transaction.success(datos); // Same cold-cache handling as Android.
                if (antes != null) antes.accept(datos);
                return cambio.aplicar(datos) ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e, boolean committed, DataSnapshot s) {
                f.complete(new Resultado(e, committed, s));
            }
        }, false);
        return f;
    }
    private static Resultado esperar(CompletableFuture<Resultado> f) throws Exception {
        return f.get(20, TimeUnit.SECONDS);
    }
    private static Map<String,Object> usuario() {
        Map<String,Object> m = new HashMap<>();
        m.put("uid","u"); m.put("monedas",20L); m.put("partidasJugadas",2L);
        m.put("partidasGanadas",1L); m.put("puntosTotalesHistoricos",80L);
        m.put("avatarID","avatar_01"); m.put("campoAnterior",Map.of("dato","conservar"));
        m.put("inventarioComodines",Map.of("congelador",Map.of("cantidad",2L)));
        return m;
    }
    private static Map<String,Object> historial() {
        return Map.of("idPartida","partida1","miUid","u","monedas",100L,"miTotal",50L,
                "miPosicion",1L,"fecha",ServerValue.TIMESTAMP);
    }
    private static Cambio recompensa() { return datos -> RecompensasPartida.aplicar(datos,"partida1",historial()); }
    private void semilla(String path, Object value) throws Exception {
        ref(seed,path).setValueAsync(value).get(15,TimeUnit.SECONDS);
    }
    private void comprobarUsuarioUnaVez(DataSnapshot u) {
        assertEquals(120L,u.child("monedas").getValue());
        assertEquals(3L,u.child("partidasJugadas").getValue());
        assertEquals(2L,u.child("partidasGanadas").getValue());
        assertEquals(130L,u.child("puntosTotalesHistoricos").getValue());
        assertEquals(true,u.child("recompensasAplicadas/partida1").getValue());
        assertEquals(1L,u.child("historialPartidas").getChildrenCount());
        assertEquals("avatar_01",u.child("avatarID").getValue());
        assertEquals("conservar",u.child("campoAnterior/dato").getValue());
        assertEquals(2L,u.child("inventarioComodines/congelador/cantidad").getValue());
    }
    private static Map<String,Object> sala() {
        Map<String,Object> m = new HashMap<>();
        m.put("letraActual","A");m.put("idPartida","partida1"); m.put("rondaActual",2L); m.put("cantidadRondas",5L);
        m.put("inicioRonda",1000L); m.put("fase","jugando"); m.put("estadoPartida","En juego");
        m.put("puntajesRondas",Map.of("ronda_1",Map.of("b",50L)));
        m.put("campoAnterior",Map.of("dato","conservar"));
        Map<String,Object> jugadores = new HashMap<>();
        for (String uid : List.of("h","b","c","d")) jugadores.put(uid,
                Map.of("uid",uid,"nombre","Nombre "+uid,"estaConectado",true,"esHost",uid.equals("h")));
        m.put("jugadores",jugadores); m.put("hostUID","h"); m.put("hostUid","h"); m.put("hostNombre","Nombre h");
        return m;
    }
    private void comprobarCorona(DataSnapshot s, String uid) {
        assertEquals(uid,s.child("hostUID").getValue());
        assertEquals(uid,s.child("hostUid").getValue());
        int coronas=0;
        for (DataSnapshot j:s.child("jugadores").getChildren()) if (Boolean.TRUE.equals(j.child("esHost").getValue())) {
            coronas++; assertEquals(uid,j.getKey());
        }
        assertEquals(1,coronas);
        assertEquals("partida1",s.child("idPartida").getValue());
        assertEquals(2L,s.child("rondaActual").getValue());
        assertEquals(1000L,s.child("inicioRonda").getValue());
        assertEquals("jugando",s.child("fase").getValue());
        assertEquals(50L,s.child("puntajesRondas/ronda_1/b").getValue());
        assertEquals("conservar",s.child("campoAnterior/dato").getValue());
    }

    @Test(timeout=45000) public void dosClientesNoPaganDosVecesYReentradaNoPaga() throws Exception {
        semilla("usuarios/u",usuario());
        AtomicInteger ca=new AtomicInteger(),cb=new AtomicInteger();
        var fa=transaccion(ref(a,"usuarios/u"),recompensa(),ca,null);
        var fb=transaccion(ref(b,"usuarios/u"),recompensa(),cb,null);
        Resultado ra=esperar(fa),rb=esperar(fb);
        assertNull(ra.error()); assertNull(rb.error()); assertNotEquals(ra.committed(),rb.committed());
        comprobarUsuarioUnaVez(leer(ref(seed,"usuarios/u")));
        assertFalse(esperar(transaccion(ref(a,"usuarios/u"),recompensa(),new AtomicInteger(),null)).committed());
        comprobarUsuarioUnaVez(leer(ref(seed,"usuarios/u")));
        System.out.println("reward attempts a="+ca+" b="+cb);
    }

    @Test(timeout=45000) public void eleccionConcurrenteDejaUnaSolaCorona() throws Exception {
        Map<String,Object> sala=sala(); sala.remove("hostUID");sala.remove("hostUid");sala.remove("hostNombre");
        @SuppressWarnings("unchecked") Map<String,Object> js=(Map<String,Object>)sala.get("jugadores");
        js.remove("h");
        semilla("salas/s",sala);
        var fa=transaccion(ref(a,"salas/s"),TransferenciaHost::reconciliar,new AtomicInteger(),null);
        var fb=transaccion(ref(b,"salas/s"),TransferenciaHost::reconciliar,new AtomicInteger(),null);
        Resultado ra=esperar(fa),rb=esperar(fb);
        assertNull(ra.error());assertNull(rb.error());assertNotEquals(ra.committed(),rb.committed());
        comprobarCorona(leer(ref(seed,"salas/s")),"b");
    }

    @Test(timeout=45000) public void dosAbandonosConcurrentesConservanSalaYPuntos() throws Exception {
        semilla("salas/s",sala());
        var fa=transaccion(ref(a,"salas/s"),s -> TransferenciaHost.abandonar(s,"h"),new AtomicInteger(),null);
        var fb=transaccion(ref(b,"salas/s"),s -> TransferenciaHost.abandonar(s,"b"),new AtomicInteger(),null);
        assertTrue(esperar(fa).committed()); assertTrue(esperar(fb).committed());
        DataSnapshot actual=leer(ref(seed,"salas/s"));
        comprobarCorona(actual,"c"); assertEquals(2L,actual.child("jugadores").getChildrenCount());
        assertFalse(esperar(transaccion(ref(b,"salas/s"),s -> TransferenciaHost.abandonar(s,"h"),new AtomicInteger(),null)).committed());
    }

    @Test(timeout=45000) public void conflictoRealReejecutaYConservaOtraEscritura() throws Exception {
        semilla("usuarios/u",usuario());
        CountDownLatch leyendo=new CountDownLatch(1),continuar=new CountDownLatch(1);
        AtomicBoolean bloquear=new AtomicBoolean(true);AtomicInteger validos=new AtomicInteger();
        CompletableFuture<Resultado> f=transaccion(ref(a,"usuarios/u"),recompensa(),new AtomicInteger(),datos -> {
            validos.incrementAndGet();
            if (bloquear.getAndSet(false)) {
                leyendo.countDown();
                try { if (!continuar.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("No release"); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
            }
        });
        try {
            assertTrue(leyendo.await(10,TimeUnit.SECONDS));
            ref(b,"usuarios/u/avatarID").setValueAsync("avatar_02").get(10,TimeUnit.SECONDS);
        } finally { continuar.countDown(); }
        assertTrue(esperar(f).committed()); assertTrue("No hubo retry real",validos.get()>=2);
        DataSnapshot actual=leer(ref(seed,"usuarios/u"));
        assertEquals("avatar_02",actual.child("avatarID").getValue());
        assertEquals(120L,actual.child("monedas").getValue());
        assertEquals(true,actual.child("recompensasAplicadas/partida1").getValue());
        System.out.println("nonempty server transaction attempts="+validos);
    }

    @Test(timeout=45000) public void operacionPendienteOfflineNoDuplicaAlReconectar() throws Exception {
        semilla("usuarios/u",usuario());
        leer(ref(a,"usuarios/u"));
        a.goOffline();
        var pendiente=transaccion(ref(a,"usuarios/u"),recompensa(),new AtomicInteger(),null);
        try {
            assertTrue(esperar(transaccion(ref(b,"usuarios/u"),recompensa(),new AtomicInteger(),null)).committed());
            assertFalse("La operacion offline no debe confirmarse",pendiente.isDone());
        } finally { a.goOnline(); }
        Resultado reconectado=esperar(pendiente);
        assertNull(reconectado.error()); assertFalse(reconectado.committed());
        comprobarUsuarioUnaVez(leer(ref(seed,"usuarios/u")));
    }

    @Test(timeout=45000) public void reglaLocalDenegadaNoPublicaHistorialMarcaNiMonedas() throws Exception {
        DatabaseReference admin=seed.getReference("denied/"+run);
        admin.setValueAsync(usuario()).get(10,TimeUnit.SECONDS);
        Resultado r=esperar(transaccion(a.getReference("denied/"+run),recompensa(),new AtomicInteger(),null));
        assertNotNull(r.error());assertEquals(DatabaseError.PERMISSION_DENIED,r.error().getCode());assertFalse(r.committed());
        DataSnapshot actual=leer(admin);
        assertEquals(20L,actual.child("monedas").getValue());
        assertNull(actual.child("historialPartidas").getValue());
        assertNull(actual.child("recompensasAplicadas").getValue());
    }

    @Test(timeout=45000) public void onDisconnectRealRegistraAusenciaYNoDevuelveCorona() throws Exception {
        semilla("salas/s",sala());
        DatabaseReference jugador=ref(a,"salas/s/jugadores/h");
        jugador.onDisconnect().updateChildrenAsync(Map.of("estaConectado",false,
                "desconectadoDesde",ServerValue.TIMESTAMP)).get(10,TimeUnit.SECONDS);
        a.goOffline();
        DataSnapshot actual=null;
        try {
            long fin=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            do {
                actual=leer(ref(seed,"salas/s/jugadores/h"));
                if (Boolean.FALSE.equals(actual.child("estaConectado").getValue())) break;
                Thread.sleep(100);
            } while (System.nanoTime()<fin);
            assertEquals(false,actual.child("estaConectado").getValue());
            long desde=((Number)actual.child("desconectadoDesde").getValue()).longValue();
            // Deadline is advanced as input; we test the server callback, not a 30s wall-clock sleep.
            assertTrue(esperar(transaccion(ref(b,"salas/s"),s -> TransferenciaHost.expirar(s,desde+30000L,30000L),new AtomicInteger(),null)).committed());
        } finally { a.goOnline(); }
        DataSnapshot s=leer(ref(seed,"salas/s"));
        comprobarCorona(s,"b"); assertFalse(s.child("jugadores").hasChild("h"));
    }

    @Test(timeout=45000) public void plazoCompartidoNoDependeDelClienteQueLee() throws Exception {
        semilla("salas/s",Map.of("inicioRonda",ServerValue.TIMESTAMP,"tiempoRonda",60L));
        DataSnapshot sa=leer(ref(a,"salas/s")),sb=leer(ref(b,"salas/s"));
        long inicioA=((Number)sa.child("inicioRonda").getValue()).longValue();
        long inicioB=((Number)sb.child("inicioRonda").getValue()).longValue();
        assertEquals(inicioA,inicioB);
        assertEquals(PlazoRonda.calcularFin(inicioA,60,null),PlazoRonda.calcularFin(inicioB,60,null));
    }

    @Test(timeout=45000) public void segundaPartidaAvanzaUnaVezConDosYTresJugadores() throws Exception {
        for (int cantidad : new int[]{2, 3}) {
            Map<String,Object> datos=sala();
            datos.put("idPartida", "partida2");
            datos.put("fase", "revision_puntuaciones");
            @SuppressWarnings("unchecked") Map<String,Object> jugadores=(Map<String,Object>)datos.get("jugadores");
            jugadores.remove("d");
            if (cantidad==2) jugadores.remove("c");
            Map<String,Object> listos=new HashMap<>();
            for (String uid:jugadores.keySet()) listos.put(uid,true);
            datos.put("revisionLista",Map.of("ronda_2",listos));
            semilla("salas/s",datos);
            Map<String,Object> cambios=Map.of("rondaActual",3,"fase","jugando");
            Cambio avanzar=s -> InicioPartida.avanzar(s,"h","partida2",2,cambios);
            var fa=transaccion(ref(a,"salas/s"),avanzar,new AtomicInteger(),null);
            var fb=transaccion(ref(b,"salas/s"),avanzar,new AtomicInteger(),null);
            Resultado ra=esperar(fa),rb=esperar(fb);
            assertNull(ra.error()); assertNull(rb.error()); assertNotEquals(ra.committed(),rb.committed());
            assertFalse(esperar(transaccion(ref(a,"salas/s"),avanzar,new AtomicInteger(),null)).committed());
            assertFalse(esperar(transaccion(ref(a,"salas/s"),
                    s -> InicioPartida.avanzar(s,"h","partida1",3,cambios),new AtomicInteger(),null)).committed());
            DataSnapshot actual=leer(ref(seed,"salas/s"));
            assertEquals(3L,actual.child("rondaActual").getValue());
            assertEquals("partida2",actual.child("idPartida").getValue());
            assertEquals(50L,actual.child("puntajesRondas/ronda_1/b").getValue());
        }
    }

    @Test(timeout=45000) public void salidaDuranteEsperaPermiteAvanzarYResumenNoReabreFinal() throws Exception {
        Map<String,Object> datos=sala();
        datos.put("fase","revision_puntuaciones");
        datos.put("revisionLista",Map.of("ronda_2",Map.of("h",true,"b",true)));
        semilla("salas/s",datos);
        Cambio avanzar=s -> InicioPartida.avanzar(s,"h","partida1",2,Map.of("rondaActual",3,"fase","jugando"));
        assertFalse(esperar(transaccion(ref(a,"salas/s"),avanzar,new AtomicInteger(),null)).committed());
        for (String uid:List.of("c","d"))
            assertTrue(esperar(transaccion(ref(b,"salas/s"),s -> TransferenciaHost.abandonar(s,uid),new AtomicInteger(),null)).committed());
        assertTrue(esperar(transaccion(ref(a,"salas/s"),avanzar,new AtomicInteger(),null)).committed());
        assertTrue(esperar(transaccion(ref(b,"salas/s"),s -> TransferenciaHost.abandonar(s,"b"),new AtomicInteger(),null)).committed());
        assertFalse(esperar(transaccion(ref(a,"salas/s"),s -> InicioPartida.publicarResumen(s,"h","partida1",3),new AtomicInteger(),null)).committed());
        assertEquals("resultados_finales",leer(ref(seed,"salas/s")).child("fase").getValue());
    }

    @Test(timeout=45000) public void reentradaDeIdentidadNoBorraEstadoConcurrente() throws Exception {
        semilla("salas/s",sala());
        var fa=transaccion(ref(a,"salas/s/jugadores/h"),s -> IdentidadJugador.aplicar(s,"h","Diego",null),new AtomicInteger(),null);
        ref(b,"salas/s/jugadores/h/puntajePartida").setValueAsync(75).get(10,TimeUnit.SECONDS);
        assertTrue(esperar(fa).committed());
        DataSnapshot jugador=leer(ref(seed,"salas/s/jugadores/h"));
        assertEquals("Diego",jugador.child("nombre").getValue());
        assertEquals(true,jugador.child("esHost").getValue());
        assertEquals(75L,jugador.child("puntajePartida").getValue());
    }

    @Test(timeout=45000) public void votacionConcurrenteVotoUnicoYCierreIdempotente() throws Exception {
        semilla("salas/s",sala());
        Map<String,Object> propuesta=Map.of("idVotacion","v","claveVotacion","k","activa",true,
                "resuelta",false,"uidObjetivo","h","respuesta","Abeja","categoria","Animal");
        Cambio crear=s -> VotacionRevision.crear(s,"partida1",2,"h",propuesta);
        var abrirA=transaccion(ref(a,"salas/s"),crear,new AtomicInteger(),null);
        var abrirB=transaccion(ref(b,"salas/s"),crear,new AtomicInteger(),null);
        Resultado ca=esperar(abrirA),cb=esperar(abrirB);
        assertNull(ca.error()); assertNull(cb.error()); assertNotEquals(ca.committed(),cb.committed());
        Cambio votar=s -> VotacionRevision.registrar(s,"partida1",2,"v","b","si");
        var votoA=transaccion(ref(a,"salas/s"),votar,new AtomicInteger(),null);
        var votoB=transaccion(ref(b,"salas/s"),votar,new AtomicInteger(),null);
        Resultado va=esperar(votoA),vb=esperar(votoB);
        assertNull(va.error()); assertNull(vb.error()); assertNotEquals(va.committed(),vb.committed());
        for (String uid:List.of("h","c","d")) assertTrue(esperar(transaccion(ref(a,"salas/s"),
                s -> VotacionRevision.registrar(s,"partida1",2,"v",uid,"si"),new AtomicInteger(),null)).committed());
        Map<String,Object> cambios=new HashMap<>();
        cambios.put("votacionActiva/ronda_2",null);
        cambios.put("historialVotaciones/ronda_2/v/puntosFinales",10);
        cambios.put("resultadosVotacionPalabras/ronda_2/k/resuelta",true);
        Object votos=leer(ref(seed,"salas/s/votacionActiva/ronda_2/votos")).getValue();
        Cambio cerrar=s -> VotacionRevision.resolver(s,"partida1",2,"v","h",votos,cambios);
        var cierreA=transaccion(ref(a,"salas/s"),cerrar,new AtomicInteger(),null);
        var cierreB=transaccion(ref(b,"salas/s"),cerrar,new AtomicInteger(),null);
        Resultado ra=esperar(cierreA),rb=esperar(cierreB);
        assertNull(ra.error()); assertNull(rb.error()); assertNotEquals(ra.committed(),rb.committed());
        assertFalse(esperar(transaccion(ref(a,"salas/s"),votar,new AtomicInteger(),null)).committed());
        assertFalse(esperar(transaccion(ref(a,"salas/s"),crear,new AtomicInteger(),null)).committed());
        DataSnapshot actual=leer(ref(seed,"salas/s"));
        assertEquals(1L,actual.child("historialVotaciones/ronda_2").getChildrenCount());
        assertEquals(10L,actual.child("historialVotaciones/ronda_2/v/puntosFinales").getValue());
        assertEquals(50L,actual.child("puntajesRondas/ronda_1/b").getValue());
    }

    @Test(timeout=45000) public void comprasConcurrentesDeAmbasMonedasNoPierdenInventario() throws Exception {
        Map<String,Object> datos=usuario(); datos.put("monedas",500L); datos.put("gemas",20L);
        datos.put("recompensasAplicadas",Map.of("anterior",true));
        semilla("usuarios/u",datos);
        Cambio moneda=s -> CompraComodin.aplicar(s,"congelador","Congelador","monedas",250);
        Cambio gema=s -> CompraComodin.aplicar(s,"marca","Marca","gemas",10);
        var fa=transaccion(ref(a,"usuarios/u"),moneda,new AtomicInteger(),null);
        var fb=transaccion(ref(b,"usuarios/u"),moneda,new AtomicInteger(),null);
        var fc=transaccion(ref(a,"usuarios/u"),gema,new AtomicInteger(),null);
        var fd=transaccion(ref(b,"usuarios/u"),gema,new AtomicInteger(),null);
        for(var f:List.of(fa,fb,fc,fd)) { Resultado r=esperar(f); assertNull(r.error()); assertTrue(r.committed()); }
        DataSnapshot u=leer(ref(seed,"usuarios/u"));
        assertEquals(0L,u.child("monedas").getValue()); assertEquals(0L,u.child("gemas").getValue());
        assertEquals(4L,u.child("inventarioComodines/congelador/cantidad").getValue());
        assertEquals(2L,u.child("inventarioComodines/marca/cantidad").getValue());
        assertEquals(true,u.child("recompensasAplicadas/anterior").getValue());
        assertFalse(esperar(transaccion(ref(a,"usuarios/u"),moneda,new AtomicInteger(),null)).committed());
    }

    @Test(timeout=45000) public void relojCompartidoCincoSegundosRealesYCierreConcurrenteConEmpate() throws Exception {
        Map<String,Object> datos=sala();datos.put("categoriasActivas",List.of("Animal","Fruta"));
        datos.put("respuestas",Map.of("ronda_2",Map.of("h",Map.of("Animal","Abeja"),"b",Map.of("Animal",""),"c",Map.of("Animal",""),"d",Map.of("Animal",""))));
        datos.put("validaciones",Map.of("ronda_2",Map.of("h",Map.of("Animal",Map.of("puntos",10,"correcta",true)))));
        semilla("salas/s",datos);
        Cambio iniciarA=s -> RelojRevision.coordinar(s,"partida1",2,"h",System.currentTimeMillis());
        Cambio iniciarB=s -> RelojRevision.coordinar(s,"partida1",2,"b",System.currentTimeMillis());
        var fa=transaccion(ref(a,"salas/s"),iniciarA,new AtomicInteger(),null);
        var fb=transaccion(ref(b,"salas/s"),iniciarB,new AtomicInteger(),null);
        assertNotEquals(esperar(fa).committed(),esperar(fb).committed());
        for(String uid:List.of("h","b","c","d")) assertTrue(esperar(transaccion(ref(a,"salas/s"),
                s -> RelojRevision.preparado(s,"partida1",2,0,1,uid),new AtomicInteger(),null)).committed());
        assertTrue(esperar(transaccion(ref(b,"salas/s"),iniciarB,new AtomicInteger(),null)).committed());
        long revision=((Number)leer(ref(a,"salas/s/revisionCategorias/ronda_2/reloj/inicio")).getValue()).longValue();
        assertEquals(revision,((Number)leer(ref(b,"salas/s/revisionCategorias/ronda_2/reloj/inicio")).getValue()).longValue());
        Thread.sleep(Math.max(0,revision+3500-System.currentTimeMillis()+20));
        assertTrue(esperar(transaccion(ref(b,"salas/s"),iniciarB,new AtomicInteger(),null)).committed());
        assertEquals("propuestas",leer(ref(seed,"salas/s/revisionCategorias/ronda_2/reloj/modo")).getValue());
        String clavePropuesta=ClaveRevision.de(2,"h","Animal","Abeja");
        Map<String,Object> propuesta=Map.of("idVotacion","compartida","claveVotacion",clavePropuesta,"activa",true,
                "uidObjetivo","h","categoria","Animal","respuesta","Abeja","turnoRevision",2);
        assertTrue(esperar(transaccion(ref(a,"salas/s"),s -> VotacionRevision.crear(s,"partida1",2,"h",propuesta,System.currentTimeMillis()),new AtomicInteger(),null)).committed());
        DataSnapshot v=leer(ref(seed,"salas/s/votacionActiva/ronda_2"));
        String id=v.child("idVotacion").getValue(String.class),clave=v.child("claveVotacion").getValue(String.class);
        long inicio=((Number)leer(ref(seed,"salas/s/revisionCategorias/ronda_2/reloj/inicio")).getValue()).longValue();
        long fin=inicio+5000;
        for(String[] voto:List.of(new String[]{"h","si"},new String[]{"b","no"},new String[]{"c","abstenerse"}))
            assertTrue(esperar(transaccion(ref(a,"salas/s"),s -> VotacionRevision.registrar(s,"partida1",2,id,voto[0],voto[1],System.currentTimeMillis()),new AtomicInteger(),null)).committed());
        Object votos=leer(ref(seed,"salas/s/votacionActiva/ronda_2/votos")).getValue();
        Map<String,Object> cambios=new HashMap<>();cambios.put("votacionActiva/ronda_2",null);
        cambios.put("historialVotaciones/ronda_2/"+id+"/puntosFinales",10);
        cambios.put("validaciones/ronda_2/h/Animal/puntos",5); // A mistaken old caller cannot turn a tie into +5.
        cambios.put("resultadosVotacionPalabras/ronda_2/"+clave+"/resuelta",true);
        Cambio cerrarA=s -> VotacionRevision.resolver(s,"partida1",2,id,"h",votos,cambios,System.currentTimeMillis());
        Cambio cerrarB=s -> VotacionRevision.resolver(s,"partida1",2,id,"b",votos,cambios,System.currentTimeMillis());
        assertFalse(esperar(transaccion(ref(a,"salas/s"),cerrarA,new AtomicInteger(),null)).committed());
        Thread.sleep(Math.max(0,fin-System.currentTimeMillis()+30));
        assertFalse(esperar(transaccion(ref(b,"salas/s"),s -> VotacionRevision.registrar(s,"partida1",2,id,"d","no",System.currentTimeMillis()),new AtomicInteger(),null)).committed());
        var ca=transaccion(ref(a,"salas/s"),cerrarA,new AtomicInteger(),null);var cb=transaccion(ref(b,"salas/s"),cerrarB,new AtomicInteger(),null);
        assertNotEquals(esperar(ca).committed(),esperar(cb).committed());
        DataSnapshot actual=leer(ref(seed,"salas/s"));
        assertEquals(10L,actual.child("validaciones/ronda_2/h/Animal/puntos").getValue());
        assertEquals(1L,actual.child("historialVotaciones/ronda_2").getChildrenCount());
        assertEquals("resultado",actual.child("revisionCategorias/ronda_2/reloj/modo").getValue());
    }
    @Test(timeout=45000) public void cierreM16DosClientesRetrasoYListenerRecreado() throws Exception {
        Map<String,Object> datos=sala();datos.put("tiempoRonda",60);datos.put("categoriasActivas",List.of("Animal","Fruta"));
        datos.put("jugadores",Map.of("h",Map.of("estaConectado",true),"b",Map.of("estaConectado",true)));
        datos.put("respuestas",Map.of("ronda_2",Map.of("h",Map.of("Animal","Abeja","Fruta",""),"b",Map.of("Animal","Avestruz","Fruta",""))));
        semilla("sala",datos);
        DatabaseReference rb=ref(b,"sala");
        CompletableFuture<DataSnapshot> observado=new CompletableFuture<>();
        ValueEventListener listener=new ValueEventListener(){
            public void onDataChange(DataSnapshot s){if(s.child("revisionCategorias/ronda_2/reloj").exists())observado.complete(s);}
            public void onCancelled(DatabaseError e){observado.completeExceptionally(e.toException());}
        };
        rb.addValueEventListener(listener);rb.removeEventListener(listener);b.goOffline();
        assertTrue(esperar(transaccion(ref(a,"sala"),s -> CierreRespuestas.aplicar(s,"partida1",2,"h",61000),new AtomicInteger(),null)).committed());
        b.goOnline();rb.addValueEventListener(listener);
        DataSnapshot recuperado=observado.get(15,TimeUnit.SECONDS);rb.removeEventListener(listener);
        assertEquals(0L,recuperado.child("revisionCategorias/ronda_2/indiceActual").getValue());
        assertEquals(leer(ref(a,"sala/revisionCategorias/ronda_2")).getValue(),recuperado.child("revisionCategorias/ronda_2").getValue());
        assertFalse(esperar(transaccion(rb,s -> CierreRespuestas.aplicar(s,"partida1",2,"b",80000),new AtomicInteger(),null)).committed());
        assertEquals("Abeja",leer(ref(seed,"sala/respuestas/ronda_2/h/Animal")).getValue());
    }
    @Test(timeout=45000) public void cierreM16ConcurrenteSinHostYConRespuestasAusentes() throws Exception {
        Map<String,Object> datos=sala();datos.put("tiempoRonda",60);datos.put("categoriasActivas",List.of("Animal"));
        datos.put("sesiones",Map.of("h",Map.of("host",Map.of("conectada",false,"desconectadoDesde",1000))));
        semilla("sala",datos);a.goOffline();
        assertFalse(esperar(transaccion(ref(b,"sala"),s -> CierreRespuestas.aplicar(s,"partida1",2,"b",65999),new AtomicInteger(),null)).committed());
        var uno=transaccion(ref(b,"sala"),s -> CierreRespuestas.aplicar(s,"partida1",2,"b",66000),new AtomicInteger(),null);
        var dos=transaccion(ref(seed,"sala"),s -> CierreRespuestas.aplicar(s,"partida1",2,"c",66000),new AtomicInteger(),null);
        assertNotEquals(esperar(uno).committed(),esperar(dos).committed());
        a.goOnline();
        assertEquals(leer(ref(a,"sala/revisionCategorias/ronda_2")).getValue(),leer(ref(b,"sala/revisionCategorias/ronda_2")).getValue());
        assertFalse(leer(ref(seed,"sala/respuestas")).exists());
        assertEquals(50L,leer(ref(seed,"sala/puntajesRondas/ronda_1/b")).getValue());
    }
    @Test(timeout=45000) public void presenciaM16OnDisconnectRealYLimite90Segundos() throws Exception {
        Map<String,Object> datos=sala();datos.put("sesiones",Map.of("h",Map.of("s1",Map.of("conectada",true))));semilla("sala",datos);
        DatabaseReference sesion=ref(a,"sala/sesiones/h/s1");
        sesion.onDisconnect().updateChildrenAsync(Map.of("conectada",false,"desconectadoDesde",ServerValue.TIMESTAMP)).get(10,TimeUnit.SECONDS);
        a.goOffline();DataSnapshot caida=null;long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        do{caida=leer(ref(seed,"sala/sesiones/h/s1"));if(Boolean.FALSE.equals(caida.child("conectada").getValue()))break;Thread.sleep(50);}while(System.nanoTime()<limite);
        assertEquals(false,caida.child("conectada").getValue());long desde=((Number)caida.child("desconectadoDesde").getValue()).longValue();
        esperar(transaccion(ref(b,"sala"),s -> TransferenciaHost.expirar(s,desde+89999,90000),new AtomicInteger(),null));
        assertTrue(leer(ref(seed,"sala/jugadores/h")).exists());
        assertEquals("b",leer(ref(seed,"sala/hostUID")).getValue());
        assertTrue(esperar(transaccion(ref(b,"sala"),s -> TransferenciaHost.expirar(s,desde+90000,90000),new AtomicInteger(),null)).committed());
        assertFalse(leer(ref(seed,"sala/jugadores/h")).exists());assertFalse(leer(ref(seed,"sala/sesiones/h")).exists());
        a.goOnline();assertFalse(esperar(transaccion(ref(a,"sala"),s -> IngresoSala.ingresar(s,"h","Host","foto",desde+90001,90000),new AtomicInteger(),null)).committed());
    }
    @Test(timeout=45000) public void presenciaM16ReconexionCompiteConExpiracionSinBorrarSesionValida() throws Exception {
        Map<String,Object> datos=sala();datos.put("sesiones",Map.of("h",Map.of("s1",Map.of("conectada",false,"desconectadoDesde",1000))));
        semilla("sala",datos);
        // Capture before the boundary: transaction retries revalidate the reservation against shared data.
        var reconectar=transaccion(ref(a,"sala"),s -> {
            MutableData j=s.child("jugadores/h");
            if(!j.hasChildren() || !IngresoSala.reservaVigente(j.child("estaConectado").getValue(),j.child("desconectadoDesde").getValue(),s.child("sesiones/h").getValue(),90999,90000)) return false;
            s.child("sesiones/h/s2/conectada").setValue(true);PresenciaSala.actualizar(s);return true;
        },new AtomicInteger(),null);
        var expirar=transaccion(ref(b,"sala"),s -> TransferenciaHost.expirar(s,91000,90000),new AtomicInteger(),null);
        Resultado r=esperar(reconectar);esperar(expirar);
        DataSnapshot fin=leer(ref(seed,"sala"));assertEquals(r.committed(),fin.child("jugadores/h").exists());
        if(r.committed()) {
            assertEquals(true,fin.child("sesiones/h/s2/conectada").getValue());
            assertFalse(esperar(transaccion(ref(b,"sala"),s -> TransferenciaHost.abandonarSesion(s,"h","partida1","s1",List.of("s1")),new AtomicInteger(),null)).committed());
        }
    }
    @Test(timeout=45000) public void presenciaM16SalidaReconciliadaEsUnicaYConservaOtraPartida() throws Exception {
        Map<String,Object> datos=sala();datos.put("sesiones",Map.of("h",Map.of("vieja",Map.of("conectada",false),"actual",Map.of("conectada",true))));
        semilla("sala",datos);List<String> conocidas=List.of("vieja","actual");
        var x=transaccion(ref(a,"sala"),s -> TransferenciaHost.abandonarSesion(s,"h","partida1","actual",conocidas),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> TransferenciaHost.abandonarSesion(s,"h","partida1","actual",conocidas),new AtomicInteger(),null);
        assertNotEquals(esperar(x).committed(),esperar(y).committed());assertFalse(leer(ref(seed,"sala/jugadores/h")).exists());
        datos.put("idPartida","partida2");semilla("sala",datos);
        assertFalse(esperar(transaccion(ref(a,"sala"),s -> TransferenciaHost.abandonarSesion(s,"h","partida1","actual",conocidas),new AtomicInteger(),null)).committed());
        assertTrue(leer(ref(seed,"sala/jugadores/h")).exists());
    }
    @Test(timeout=45000) public void presenciaM16AbandonoDuranteRecreacionRetiraSusDosSesiones() throws Exception {
        Map<String,Object> datos=sala();datos.put("sesiones",Map.of("h",Map.of("anterior",Map.of("conectada",false),"pendiente",Map.of("conectada",true))));semilla("sala",datos);
        var x=transaccion(ref(a,"sala"),s -> TransferenciaHost.abandonarSesion(s,"h","partida1","pendiente",List.of("anterior","pendiente"),List.of("anterior","pendiente")),new AtomicInteger(),null);
        var y=transaccion(ref(b,"sala"),s -> TransferenciaHost.expirar(s,System.currentTimeMillis(),90000),new AtomicInteger(),null);
        assertTrue(esperar(x).committed());esperar(y);
        assertFalse(leer(ref(seed,"sala/jugadores/h")).exists());assertEquals(3,leer(ref(seed,"sala/jugadores")).getChildrenCount());
        assertFalse(leer(ref(seed,"sala/sesiones/h")).exists());
    }
    @Test(timeout=45000) public void presenciaM16UltimoJugadorCierraUnaVezSinBonos() throws Exception {
        Map<String,Object> datos=sala();datos.put("jugadores",Map.of("h",Map.of("estaConectado",true),"b",Map.of("estaConectado",true)));
        datos.put("sesiones",Map.of("h",Map.of("s",Map.of("conectada",false,"desconectadoDesde",1000))));semilla("sala",datos);
        assertTrue(esperar(transaccion(ref(b,"sala"),s -> TransferenciaHost.expirar(s,91000,90000),new AtomicInteger(),null)).committed());
        DataSnapshot finalizado=leer(ref(seed,"sala"));assertEquals(1,finalizado.child("jugadores").getChildrenCount());
        assertEquals("resultados_finales",finalizado.child("fase").getValue());assertEquals("b",finalizado.child("hostUID").getValue());
        assertEquals(true,finalizado.child("rondasProcesadas/ronda_2").getValue());
        assertFalse(esperar(transaccion(ref(a,"sala"),s -> TransferenciaHost.expirar(s,92000,90000),new AtomicInteger(),null)).committed());
        assertEquals(50L,leer(ref(seed,"sala/puntajesRondas/ronda_1/b")).getValue());
    }
    @Test(timeout=45000) public void presenciaM16EntradaConcurrenteConAbandonoNoRecreaJugador() throws Exception {
        semilla("sala",sala());
        var entrada=transaccion(ref(a,"sala"),s -> InicioPartida.registrarEntrada(s,"partida1",2,"h"),new AtomicInteger(),null);
        var salida=transaccion(ref(b,"sala"),s -> TransferenciaHost.abandonar(s,"h"),new AtomicInteger(),null);
        esperar(entrada);assertTrue(esperar(salida).committed());
        assertFalse(esperar(transaccion(ref(a,"sala"),s -> InicioPartida.registrarEntrada(s,"partida1",2,"h"),new AtomicInteger(),null)).committed());
        DataSnapshot fin=leer(ref(seed,"sala"));assertFalse(fin.child("jugadores").hasChild("h"));assertFalse(fin.child("entradaPartida/ronda_2").hasChild("h"));
    }

    /** Expected to fail for the obsolete player-level presence protocol; run via presenceLimit only. */
    @Test(timeout=45000) public void conexionAnteriorNoDebeDesconectarOtraSesionActiva() throws Exception {
        semilla("salas/s",sala());
        DatabaseReference antigua=ref(a,"salas/s/jugadores/h");
        antigua.onDisconnect().updateChildrenAsync(Map.of("estaConectado",false,
                "desconectadoDesde",ServerValue.TIMESTAMP)).get(10,TimeUnit.SECONDS);
        ref(b,"salas/s/jugadores/h/estaConectado").setValueAsync(true).get(10,TimeUnit.SECONDS);
        a.goOffline();
        DataSnapshot presencia=null;
        try {
            long fin=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            do {
                presencia=leer(ref(seed,"salas/s/jugadores/h"));
                if (Boolean.FALSE.equals(presencia.child("estaConectado").getValue())) break;
                Thread.sleep(100);
            } while (System.nanoTime()<fin);
            Object conectadaB=leer(b.getReference(".info/connected")).getValue();
            System.out.println("Second connection alive="+conectadaB+", shared presence="+
                    presencia.child("estaConectado").getValue());
            assertEquals(true,conectadaB);
            assertEquals("A disconnected old connection must not mark another active connection offline",
                    true,presencia.child("estaConectado").getValue());
        } finally { a.goOnline(); }
    }
}
