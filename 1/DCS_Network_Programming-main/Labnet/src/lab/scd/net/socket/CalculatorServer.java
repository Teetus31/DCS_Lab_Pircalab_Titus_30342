package lab.scd.net.socket;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;



public class CalculatorServer {
  public static void main(String[] args) throws IOException{

    ServerSocket ss=null;
    Socket s=null;

    try{
      String line="";
      ss = new ServerSocket(1900); //create a serversocket object
      System.out.println("Server is waiting for connections...");
      s = ss.accept(); //start waiting for connections at port 1900
      //When a client connects, `ss.accept()` returns.
      //a Socket object that identifies the connection

      //Create the input and output streams.
      BufferedReader in = new BufferedReader(
            new InputStreamReader(s.getInputStream()));

      PrintWriter out = new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(
              s.getOutputStream())),true);

      //extract the IP address and port from which the client connected
      InetSocketAddress remoteadr = (InetSocketAddress)s.getRemoteSocketAddress();
      String remotehost = remoteadr.getHostName();
      int remoteport = remoteadr.getPort();

      System.out.println("Client nou conectat: "+remotehost+":"+remoteport);

      //read requests until the client sends END or closes the connection
      while((line = in.readLine()) != null && !line.equals("END")){
        System.out.println("Server a receptionat: "+line);
        String response = compute(line);
        System.out.println("Server trimite: "+response);
        out.println(response); //send the result to the client
      }

      System.out.println("Aplicatie server gata.");

    }catch(Exception e){e.printStackTrace();}
     finally{
      if(s!=null) s.close();
      if(ss!=null) ss.close();
     }
  }

  //Request format: "<number1> <op> <number2>", e.g. "12 + 30"
  //Reply: "RESULT <value>" or "ERROR <message>"
  private static String compute(String request){
    String[] parts = request.trim().split("\\s+");
    if(parts.length != 3){
      return "ERROR Expected format: <number> <op> <number>";
    }

    double a, b;
    try{
      a = Double.parseDouble(parts[0]);
      b = Double.parseDouble(parts[2]);
    }catch(NumberFormatException e){
      return "ERROR Invalid number";
    }

    switch(parts[1]){
      case "+": return "RESULT " + (a + b);
      case "-": return "RESULT " + (a - b);
      case "*": return "RESULT " + (a * b);
      case "/":
        if(b == 0) return "ERROR Division by zero";
        return "RESULT " + (a / b);
      default:  return "ERROR Unknown operation: " + parts[1];
    }
  }
}
