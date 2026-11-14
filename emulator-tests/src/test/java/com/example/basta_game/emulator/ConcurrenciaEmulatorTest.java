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
        m.put("idPartida","partida1"); m.put("rondaActual",2L); m.put("cantidadRondas",5L);
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
                "resuelta",false,"uidObjetivo","h","respuesta","Manzana","categoria","Animal");
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

    /** Expected to fail until presence is fenced per session; run via presenceLimit only. */
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
