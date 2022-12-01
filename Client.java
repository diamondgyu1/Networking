
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        Socket socket = null;
        try
        {
            socket = new Socket(ip,port1);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        Stop stop = new Stop();
        RecieveThread rt = new RecieveThread(socket, stop);
        rt.start();

        // Loop to get user input
        while(true)
        {
            try
            {
                String input = sc.nextLine();
                String[] input_split = input.split(" ");
                PrintStream ps = new PrintStream(socket.getOutputStream());
                ps.println(input);

                if(input.equals("#EXIT"))
                {
                    System.exit(0);
                }
                if(input_split[0].equals("#PUT"))
                {
                    // System.out.println(System.getProperty("user.dir"));
                    File file = new File(".\\"+input_split[1]);
                    FileInputStream fis = new FileInputStream(file);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF(input_split[1]);
                    dos.writeLong(file.length());
                    int length;
                    byte[] buffer = new byte[64];
                    // System.out.println("Sending File "+input_split[1]);
                    while((length=fis.read(buffer)) != -1)
                    {
                        dos.write(buffer, 0, length);
                        System.out.print("#");
                        dos.flush();
                    }
                    System.out.println();
                    fis.close();
                    // dos.close();
                    System.out.println("Upload Complete");
                    continue;
                }
                if (input_split[0].equals("#GET"))
                {
                    //Server will send file to the client.
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                break;
            }
        }

        sc.close();
    }
}

class RecieveThread extends Thread
{
    Stop stop;
    Socket socket;
    BufferedReader br;

    public RecieveThread(Socket socket, Stop stop)
    {
        try
        {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch(Exception e) {}
        this.socket = socket;
        this.stop = stop;
    }

    @Override
    public void run() 
    {
        //Loop to get data that server sends
        while(!stop.stop)
        {
            try
            {
                //System.out.println("[Receiver] Waiting to get input");
                String msg = br.readLine();
                // System.out.println(msg);
                if (msg.equals("MESSAGE"))
                {
                    msg = br.readLine();
                    System.out.println(msg);
                }
                else // received thing is file
                {
                    System.out.println("Getting File");
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    long datalen = 0;
                    File file = new File(fileName);
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[64];
                    int length;
                    while((length=dis.read(buffer)) != -1)
                    {
                        fos.write(buffer, 0, length);
                        System.out.print("#");
                        datalen += length;
                        if (datalen == fileSize) break;
                        //System.out.println("File is comming");
                    }
                    System.out.println();
                    fos.close();
                    // dis.close();
                    System.out.println("File Saved to local: "+fileName);
                }
            }
            catch(Exception e) { break; }
            
        }
    }


}

class Stop { boolean stop = false; }