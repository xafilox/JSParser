package procesador;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;



/**Analizador lexico. Se encarga de leer el texto fuente y obtene los tokens
 *
 */
public class AnalizadorLexico {

	private final String digito= "1234567890";
	private final String letra= "_ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnnopqrstuvwxyz";
	private final String alfanum= digito+letra;
	private String cadena;
	private String numero;


	//*********************************CLASES PRIVADAS**********************************************

	/**Clase que representa un elemento de la matriz de transicion
	 *
	 */
	private class Casilla{
		private int siguienteEstado;
		private String accionSem;
		//constructor
		private Casilla(int siguienteEstado,String accionSem){
			this.siguienteEstado=siguienteEstado;
			this.accionSem=accionSem;
		}
	}


	private class Transicion{
		private char[] simbolos;
		private int nuevoEstado;
		private String accionSemantica;

		private Transicion(String simbolos, int nuevoEstado, String accionSemantica){
			this.nuevoEstado=nuevoEstado;
			this.accionSemantica=accionSemantica;
			
			if(simbolos!=null){
				this.simbolos= simbolos.toCharArray();
			}
			else{
				this.simbolos = new char[0];
			}
		}
	}


	/**Clase que representa una matriz de transicion
	 *
	 */
	private class Matriz{

		private  ArrayList<HashMap<Character,Casilla>> casillaMatriz;


		private Matriz(){
			casillaMatriz = new ArrayList<HashMap<Character,Casilla>>(8);
		}


		/**
		 * Completa la fila "estadoActual" de la matriz de transicion.
		 * @param tr lista de transiciones
		 * @param estadoActual fila de la matriz
		 */
		private  void definirTransiciones(ArrayList<Transicion> tr, int estadoActual){

			HashSet<Character> sinUtilizar = new HashSet<Character>();

			sinUtilizar.add((char)32);
			char aux[]="&()[]{},+*>_".toCharArray();
			for(int i=0; i<aux.length;i++){
				sinUtilizar.add((char) aux[i]);
			}
			for(int i=47; i<=59;i++){
				sinUtilizar.add((char) i);
			}
			for(int i=65; i<=90;i++){
				sinUtilizar.add((char) i);
				sinUtilizar.add((char) (i+32));
			}

			for(Transicion t :  tr){
				casillaMatriz.add(estadoActual,new  HashMap<Character,Casilla>());
				if(t.simbolos.length>0){
					for(int i=0; i<t.simbolos.length;i++){
						casillaMatriz.get(estadoActual).put(t.simbolos[i], new Casilla(t.nuevoEstado,t.accionSemantica));
						sinUtilizar.remove(t.simbolos[i]);
					}
				}
				else if(t.accionSemantica!=null){ //otro caracter
					for(Character s :  sinUtilizar){
						casillaMatriz.get(estadoActual).put(s, new Casilla(t.nuevoEstado,t.accionSemantica));
						sinUtilizar.remove(s);
					}
				}                       
			}
			if(!sinUtilizar.isEmpty()){ //el caracter que no tiene asociado una transicion no es reconocido por el lenguaje
				for(Character s :  sinUtilizar){
					casillaMatriz.add(estadoActual,new  HashMap<Character,Casilla>());
					casillaMatriz.get(estadoActual).put(s, new Casilla(1,"emitirError"));
				}
			}
		}
	}

	//******************************FIN CLASES PRIVADAS**********************************************

	private LinkedList<Character> buffer;
	private int puntero;
	private int estado;

	/**Constructor
	 * 
	 */
	public AnalizadorLexico(File fichero){
		this.estado=0;
		this.puntero=0;

		//Se rellena la matriz de transicion
		Matriz matriz = new Matriz();
		
		//*************estado 0********************
		ArrayList<Transicion> tr0 = new ArrayList<Transicion>();
		tr0.add(new Transicion(letra,  1, "a2"));
		tr0.add(new Transicion(digito,  2, "a3"));
		tr0.add(new Transicion("+", 3, "a4"));
		tr0.add(new Transicion("&", 4, "a5"));
		tr0.add(new Transicion("/", 5, "a6"));
		tr0.add(new Transicion(null, 0, "a1"));
		matriz.definirTransiciones(tr0, 0);
		
		//*************estado 1********************
		ArrayList<Transicion> tr1 = new ArrayList<Transicion>();
		tr1.add(new Transicion(alfanum,  1, "a7"));
		tr1.add(new Transicion(null, 0, "a8"));
		matriz.definirTransiciones(tr1, 1);
		
		//*************estado 2********************
		ArrayList<Transicion> tr2 = new ArrayList<Transicion>();
		tr2.add(new Transicion(digito,  2, "a9"));
		tr2.add(new Transicion(null, 0, "a10"));
		matriz.definirTransiciones(tr2, 2);
		
		//*************estado 3********************
		ArrayList<Transicion> tr3 = new ArrayList<Transicion>();
		tr3.add(new Transicion("+",  0, "a11"));
		tr3.add(new Transicion(null, 0, "a12"));
		matriz.definirTransiciones(tr3, 3);
		
		//*************estado 4********************
		ArrayList<Transicion> tr4 = new ArrayList<Transicion>();
		tr4.add(new Transicion("&",  0, "a13"));
		matriz.definirTransiciones(tr4, 4);
		
		//*************estado 5********************
		ArrayList<Transicion> tr5 = new ArrayList<Transicion>();
		tr5.add(new Transicion("*",  6, "a14"));
		matriz.definirTransiciones(tr5, 5);
		
		//*************estado 6********************
		ArrayList<Transicion> tr6 = new ArrayList<Transicion>();
		tr6.add(new Transicion("*",  7, "a15"));
		tr6.add(new Transicion(null, 6, "a16"));
		matriz.definirTransiciones(tr6, 6);

		//*************estado 7********************
		ArrayList<Transicion> tr7 = new ArrayList<Transicion>();
		tr7.add(new Transicion("/",  0, "a17"));
		tr7.add(new Transicion(null, 6, "a18"));
		matriz.definirTransiciones(tr7, 7);
		
		obtenerChars(fichero);
	}

	/**Con este metodo se obtiene un token por parte del analizador sintáctico
	 * @return siguente token del texto
	 */
	public Token dameToken(){
		return null;
	}

	/*+******************acciones semanticas*******************+*/
	public Token accionSem(String accion){
		Token token=null;
		if(accion=="a1"){

		}
		return token;
	}

	public void emitirError(){
		System.out.println("error, por ahora solo imprimo");
	}



	/*+******************metodos auxiliares*******************+*/

	private void obtenerChars(File fichero){
		File archivo = null;
		FileReader fr = null;
		try {
			fr = new FileReader(archivo);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader b = new BufferedReader(fr);
		try {
			int c;
			while((c=b.read())!=-1){
				buffer.offer((char)c);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
