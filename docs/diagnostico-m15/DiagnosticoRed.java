import java.net.*;
import javax.net.ssl.*;
public class DiagnosticoRed {
 public static void main(String[] args){
  for(String host:new String[]{"www.google.com","firebasestorage.googleapis.com","identitytoolkit.googleapis.com","dl.google.com"}){
   long t=System.currentTimeMillis();try{InetAddress[] ips=InetAddress.getAllByName(host);System.out.println(host+" DNS OK "+ips.length);}catch(Exception e){System.out.println(host+" DNS "+e.getClass().getSimpleName());}
   HttpsURLConnection c=null;try{c=(HttpsURLConnection)new URL("https://"+host+"/").openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestMethod("HEAD");System.out.println(host+" HTTPS="+c.getResponseCode()+" cipher="+c.getCipherSuite());}catch(Exception e){System.out.println(host+" HTTPS "+e.getClass().getSimpleName()+": "+e.getMessage());}finally{if(c!=null)c.disconnect();}
   System.out.println("elapsedMs="+(System.currentTimeMillis()-t));
  }
 }
}
