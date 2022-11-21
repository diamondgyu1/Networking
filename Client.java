import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client 
{
    public static void main(String[] args) 
    {
        String ip = args[0];
        int port1 = Integer.parseInt(args[1]);
        int port2 = Integer.parseInt(args[2]);

        Scanner sc = new Scanner(System.in);

        while(true)
        {
            try
            {
                String input = sc.nextLine();
                String[] input_split = input.split(" ");

                Socket socket = new Socket("localhost",port1);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream ps = new PrintStream(socket.getOutputStream());
                
                // System.out.println(input);
                ps.println(input);
                System.out.println("Sent Message");

                String msg = br.readLine();
                System.out.println("MSG: "+msg);

                socket.close();
                br.close();
                
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        //sc.close();
    }
}