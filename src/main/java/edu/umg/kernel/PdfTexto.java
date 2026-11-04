package edu.umg.kernel;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

/** PDF aut?nomo: tama?o carta, m?rgenes de una pulgada, Times de 12 puntos
 * e interlineado doble. Las tablas y bloques ASCII usan Courier para alinearse.
 * Las posiciones xref se calculan sobre bytes, no sobre caracteres Unicode.
 */
public final class PdfTexto {
    private static final Charset CODIFICACION=Charset.forName("windows-1252");
    private PdfTexto() { }
    public static byte[] crear(List<String> texto){
        List<String> lineas=new ArrayList<>();for(String original:texto){for(String linea:original.replace('\r',' ').split("\n",-1)){if(linea.isEmpty())lineas.add("");for(int i=0;i<linea.length();i+=78)lineas.add(linea.substring(i,Math.min(i+78,linea.length())));}}
        int paginas=Math.max(1,(lineas.size()+26)/27);List<byte[]> objetos=new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));StringBuilder hijos=new StringBuilder();for(int i=0;i<paginas;i++)hijos.append(4+i*2).append(" 0 R ");objetos.add(bytes("<< /Type /Pages /Kids ["+hijos+"] /Count "+paginas+" >>"));objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman /Encoding /WinAnsiEncoding >>"));
        for(int i=0;i<paginas;i++){
            int contenido=5+i*2;objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R /F2 << /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >> >> >> /Contents "+contenido+" 0 R >>"));
            StringBuilder flujo=new StringBuilder("BT /F1 12 Tf 24 TL 72 720 Td\n");for(int j=i*27;j<Math.min((i+1)*27,lineas.size());j++)flujo.append(lineas.get(j).contains("|") || lineas.get(j).contains("+--") ? "/F2 9 Tf " : "/F1 12 Tf ").append('(').append(escapar(lineas.get(j))).append(") Tj T*\n");flujo.append("ET");byte[] datos=bytes(flujo.toString());ByteArrayOutputStream obj=new ByteArrayOutputStream();obj.writeBytes(bytes("<< /Length "+datos.length+" >>\nstream\n"));obj.writeBytes(datos);obj.writeBytes(bytes("\nendstream"));objetos.add(obj.toByteArray());
        }
        ByteArrayOutputStream salida=new ByteArrayOutputStream();salida.writeBytes(bytes("%PDF-1.4\n"));List<Integer> posiciones=new ArrayList<>();
        for(int i=0;i<objetos.size();i++){posiciones.add(salida.size());salida.writeBytes(bytes((i+1)+" 0 obj\n"));salida.writeBytes(objetos.get(i));salida.writeBytes(bytes("\nendobj\n"));}
        int xref=salida.size();salida.writeBytes(bytes("xref\n0 "+(objetos.size()+1)+"\n0000000000 65535 f \n"));for(int posicion:posiciones)salida.writeBytes(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",posicion)));salida.writeBytes(bytes("trailer\n<< /Size "+(objetos.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return salida.toByteArray();
    }
    private static String escapar(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}
    private static byte[] bytes(String s){return s.getBytes(CODIFICACION);}
}
