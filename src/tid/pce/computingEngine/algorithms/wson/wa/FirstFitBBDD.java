package tid.pce.computingEngine.algorithms.wson.wa;

import java.net.Inet4Address;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import tid.pce.tedb.IntraDomainEdge;

public class FirstFitBBDD {
		
	    /*
	     * Par�metros necesarios para la conexi�n: servidor (m�quina y puerto),
	     * usuario, contrase�a y base de datos a utilizar. Estos se podr�an pasar
	     * por l�nea de comandos o leer de un fichero de configuraci�n.
	     */

	    // Servidor (m�quina)
	    String host;

	    // Servidor (puerto): el puerto por defecto es el 3306
	    String port;

	    // Usuario
	    String user;

	    // Contrase�a
	    String passwd;

	    // Base de datos a utilizar
	    String db;
	    
	    private Logger log;

	    // Representa una conexi�n a la base de datos
	    Connection conn = null;

	    // Permite ejecutar una sentencia o comando en la base de datos
	    Statement stmt = null;

	    // Representa a los resultados de ejecutar una consulta en la base de datos
	    ResultSet rs = null;
	    
	    public FirstFitBBDD(String host, String port, String user, String passwd, String db){
	    	
	    	this.host = host;
	    	this.port= port;
	    	this.user = user;
	    	this.passwd = passwd;
	    	this.db = db;
	    	log = Logger.getLogger("PCEServer");
	    }
	    
//		public static void main(String args[]){
//
//			DDBB_PCE_Test2 objeto1 = new DDBB_PCE_Test2("autoslocos","root","josy1","strongest_topo");
//			
//			System.out.println("Assigned Lambda: "+objeto1.queryLambdaFromNodes());
//
//		}

		public int queryLambdaFromNodes(List<IntraDomainEdge> edge_list){
			
		    int AssignedLambda =0;
			
			try {
	        		log.info("Intentando conectar driver \n");
	                // Cargamos din�micamente la clase del driver
	                Class.forName("com.mysql.jdbc.Driver").newInstance();
	                log.info("El driver se carg� satisfactoriamente \n");

	                // Abrimos la conexi�n a la base de datos
	                String url = "jdbc:mysql://" + host + ":" + port + "/" + db;
	                //String url = "jdbc:mysql://127.0.0.1:3306/ejemplo";
	                System.out.print("intentando abrir conexi�n a: '"+url+"'");
	                conn = DriverManager.getConnection(url, user, passwd);
	                                 

	                // Queremos ejecutar comandos sobre la base de datos,
	                // as� que necesitamos un objeto Statement
	                stmt = conn.createStatement();

	                // executeUpdate: para ejecutar INSERT, DELETE, DROP, ALTER, UPDATE
	                // Creamos una tabla
	                //int result = stmt.executeUpdate("CREATE TABLE tabla_ejemplo (id INT, nombre VARCHAR(25), primary key(id))");

	                // Insertamos dos entradas
	                //result = stmt.executeUpdate("INSERT INTO tabla_ejemplo VALUES(1, 'Un nombre cualquiera')");
	                //result = stmt.executeUpdate("INSERT INTO tabla_ejemplo VALUES(2, 'Otro nombre')");

	                // executeQuery: para ejecutar SELECT
	                // Leemos todos los datos de la tabla
	                //rs = stmt.executeQuery("SELECT * FROM network_topology");
	                
	                // 1.- Tenemos que identificar en la BBDD el origen y destino del salto para poder ver qu� lambdas de ese enlace est�n libres
	                // 2.- Habr� que suponer conocido el n�mero de saltos y los Origen y Destino de cada salto
	                // 3.- Para cada salto, localizar el Origen y Destino
	                // 4.- Para cada salto localizado, leer las lambdas y almecenar el entero en un vector de enteros
	                // 5.- Cuando temine de recorrer los saltos, hacer el OR con las lambdas y devolver una lambda �til
	                
	                //*******************************************************//
	                //**************  2
	                log.info("Edge list size"+edge_list.size());
	                ArrayList<Integer> Dato_Origen= new ArrayList<Integer>(edge_list.size());
	                ArrayList<Integer> Dato_Destino= new ArrayList<Integer>(edge_list.size());
	                
	                int i;
	                for (i=0;i<edge_list.size();i++){
	                	Dato_Origen.add((((Inet4Address)edge_list.get(i).getSource()).getAddress())[3]&0xFF );
	                	Dato_Destino.add((((Inet4Address)edge_list.get(i).getTarget()).getAddress())[3]&0xFF);
	                }
	            
	                int sector =1;
	                int rellenar_con_ceros =0;
	                int Dato_MaxNumero_Lambdas =31;
	                int Dato_Max_Lambda_Enabled = 2147483647; // no puede haber m�s de 31 lambdas por segmento
	                int Dato_Num_saltos = edge_list.size();
//	                int[] Dato_Origen = {101 , 104, 103};
//	                int[] Dato_Destino = {104 , 103, 102};
	                int OperarLambdas = 0;  // inicializamos la variable
	                int[]lambdas_region1 = new int[Dato_Num_saltos]; // Array de enteros para almacenar lambdas
	                int[]lambdas_region2 = new int[Dato_Num_saltos]; // Array de enteros para almacenar lambdas
	                int[]lambdas_region3 = new int[Dato_Num_saltos]; // Array de enteros para almacenar lambdas
	                
	                for(i=0; i< Dato_Num_saltos; i++){
	                //for(int i=0; i<= Dato_Num_saltos; i++){	
	                	System.out.println("dame el valor de la i =" + i);	
	                	//**************  3
	                	rs = stmt.executeQuery("SELECT * FROM network_topology");
	                	while (rs.next()) {
		                	//System.out.println("id=" + rs.getInt("id") + " origen=" + rs.getInt("source")+ " destino=" + rs.getInt("destination"));
		                	//System.out.println("lambdas_3=" + rs.getInt("use_lambdas_3") + " lambdas_2=" + rs.getInt("use_lambdas_2")+ " lambdas_1=" + rs.getInt("use_lambdas_1"));
		                	int Origen = rs.getInt("source");
		                	int Destino = rs.getInt("destination");
		                	             
		                	if ((Origen == Dato_Origen.get(i).intValue())){
		                		if(Destino == Dato_Destino.get(i).intValue()){
		                			//**************  4 Si entra aqu� lo que tenemos que hacer es leer las lambdas y guardarlas
		                			lambdas_region1[i] = rs.getInt("use_lambdas_1");
		                			lambdas_region2[i] = rs.getInt("use_lambdas_2");
		                			lambdas_region3[i] = rs.getInt("use_lambdas_3");
		                			System.out.println("Se ha localizado uno de los saltos");
		                			
		                			//Una vez localizado el salto, necesitamos saber si podremos utilizar una
		                			//lambda de la 0-->31 (Use_lambdas_1),de la 32-->63 (use_lambdas_2) o de la
		                			//64-->80 (use_lambdas_3)
		                			
		                			
		                			//Ahora necesito saber qu� lambdas est�n disponibles en los distintos sectores. Como
			                		// criterio, se asigna la primera lambda que est� libre.
		              
		                			switch (sector){
		                			
		                			case 1:
		                				
		                				OperarLambdas = (OperarLambdas | lambdas_region1[i]);
			                			System.out.println("Lambdas de 0 --> 30 formato entero calculada =" + OperarLambdas);
			                			//En caso de que haya lambdas disponibles....
			                			String OperarLambdasBinario = Integer.toBinaryString(OperarLambdas);
			                			// asegurarse que la longitud es de 31 lambdas en el segmento
			                			if(Dato_MaxNumero_Lambdas > OperarLambdasBinario.length())
			                				{
			                				rellenar_con_ceros = Dato_MaxNumero_Lambdas - OperarLambdasBinario.length();
			                				for (int j=1;j<=rellenar_con_ceros;j++)
			                					{
			                					OperarLambdasBinario = "0" + OperarLambdasBinario; // Completar la cadena hasta 31
			                					}
			                				}
			                			if (OperarLambdas < Dato_Max_Lambda_Enabled)
			                			{
			                				System.out.println("Hay lambdas disponibles de la 0 --> 30");
			                				System.out.println("Mapa de bits de lambdas: " + OperarLambdasBinario);
				                			int F = OperarLambdasBinario.lastIndexOf("0"); // Que ser� la primera lambda libre
				                			System.out.println("lambda libre del sector 1: " + (Dato_MaxNumero_Lambdas-F-1));
				                			AssignedLambda = Dato_MaxNumero_Lambdas-F-1;
				                			sector =1;
			                			}
			                			else // Si entra aqu� dentro es que no hab�a ninguna lambda libre 0-->31
			                			{
			                				System.out.println("No hay lambdas libres de la 0 --> 30");
			                				i=0;
			                				OperarLambdas = 0;
			                				sector = 2;
			                			}
		                				
		                				
		                				
		                				break;
		                				
		                			case 2:
		                				
		                				OperarLambdas = (OperarLambdas | lambdas_region2[i]);
		                				System.out.println("Lambdas de 31 --> 62 formato entero calculadas =" + OperarLambdas);
			                			//En caso de que haya lambdas disponibles....
		                				OperarLambdasBinario = Integer.toBinaryString(OperarLambdas);
		                				// asegurarse que la longitud es de 31 lambdas en el segmento
			                			if(Dato_MaxNumero_Lambdas > OperarLambdasBinario.length())
			                				{
			                				rellenar_con_ceros = Dato_MaxNumero_Lambdas - OperarLambdasBinario.length();
			                				for (int j=1;j<=rellenar_con_ceros;j++)
			                					{
			                					OperarLambdasBinario = "0" + OperarLambdasBinario; // Completar la cadena hasta 31
			                					}
			                				}
			                			if (OperarLambdas < Dato_Max_Lambda_Enabled)
			                			{
			                				System.out.println("Hay lambdas disponibles de la 31 --> 62");
			                				// asegurarse que la longitud es de 31 lambdas en el segmento
					                		OperarLambdasBinario = Integer.toBinaryString(OperarLambdas);
					                		if(Dato_MaxNumero_Lambdas > OperarLambdasBinario.length())
				                				{
				                				rellenar_con_ceros = Dato_MaxNumero_Lambdas - OperarLambdasBinario.length();
				                				for (int j=1;j<=rellenar_con_ceros;j++)
				                					{
				                					OperarLambdasBinario = "0" + OperarLambdasBinario; // Completar la cadena hasta 31
				                					}
				                				}
					                			
				                				System.out.println("Resultado lambdas en binario: " + OperarLambdasBinario);
				                				int F = OperarLambdasBinario.lastIndexOf("0"); // Que ser� la primera lambda libre
				                				System.out.println("lambda libre del sector 2: " + (2*Dato_MaxNumero_Lambdas-F-1));
				                				AssignedLambda = 2*Dato_MaxNumero_Lambdas-F-1;
				                				if (i<= Dato_Num_saltos-1)
				                					sector =2;
				                				else
				                					sector =1;
				                				
			                			}
			                			else // Si entra aqu� dentro es que no hab�a ninguna lambda libre 0-->31
			                			{
			                				System.out.println("No hay lambdas libres de la 31 --> 62");
			                				i=0;
			                				OperarLambdas = 0;
			                				sector = 3;
			                			}
		                				
		                				break;
		                				
		                				
		                			case 3:
		                				
		                				OperarLambdas = (OperarLambdas | lambdas_region3[i]);
		                				System.out.println("Lambdas de 63 --> 80 formato entero calculadas =" + OperarLambdas);
			                			//En caso de que haya lambdas disponibles....
		                				OperarLambdasBinario = Integer.toBinaryString(OperarLambdas);
		                				// asegurarse que la longitud es de 31 lambdas en el segmento
			                			if(Dato_MaxNumero_Lambdas > OperarLambdasBinario.length())
			                				{
			                				rellenar_con_ceros = Dato_MaxNumero_Lambdas - OperarLambdasBinario.length();
			                				for (int j=1;j<=rellenar_con_ceros;j++)
			                					{
			                					OperarLambdasBinario = "0" + OperarLambdasBinario; // Completar la cadena hasta 31
			                					}
			                				}
			                			if (OperarLambdas < Dato_Max_Lambda_Enabled)
			                			{
			                				System.out.println("Hay lambdas disponibles de la 63 --> 80");
			                				// asegurarse que la longitud es de 31 lambdas en el segmento
					                		OperarLambdasBinario = Integer.toBinaryString(OperarLambdas);
					                		if(Dato_MaxNumero_Lambdas > OperarLambdasBinario.length())
				                				{
				                				rellenar_con_ceros = Dato_MaxNumero_Lambdas - OperarLambdasBinario.length();
				                				for (int j=1;j<=rellenar_con_ceros;j++)
				                					{
				                					OperarLambdasBinario = "0" + OperarLambdasBinario; // Completar la cadena hasta 31
				                					}
				                				}
					                			
				                				System.out.println("Resultado lambdas en binario: " + OperarLambdasBinario);
				                				int F = OperarLambdasBinario.lastIndexOf("0"); // Que ser� la primera lambda libre
				                				System.out.println("lambda libre del sector 3: " + (3*Dato_MaxNumero_Lambdas-F-1));
				                				AssignedLambda = 3*Dato_MaxNumero_Lambdas-F-1;
				                				if (i<= Dato_Num_saltos-1)
				                					sector =3;
				                				else
				                					sector =1;
				                				
			                			}
			                			else // Si entra aqu� dentro es que no hab�a ninguna lambda libre 0-->31
			                			{
			                				System.out.println("NO HAY LAMBDAS DISPONIBLES");
			                				System.err.println("No hay lambdas disponibles");
			                				i=Dato_Num_saltos +1;  // Para forzar error
			                				OperarLambdas = 0;
			                				sector = 1;
			                			}
		                				
		                				
		                				break;
		                			
		                			}
		                			
		                		}
		                	}
		                	
	                	}
	                	
	                }
	              //  return AssignedLambda;
	                
	        }
	        catch (Exception e) {
	                e.printStackTrace();
	        }
	        finally {
	                // Cerramos todo para liberar recursos de la base de datos
	                try {
	                        if (rs != null) rs.close();
	                        if (stmt != null) stmt.close();
	                        if (conn != null) conn.close();
	                }
	                catch (SQLException e) {
	                        System.out.println("Error al liberar recursos");
	                }
	        }
			return AssignedLambda;
			
		}
		
		

}
