import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Scanner;

import javax.xml.crypto.Data;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.Socket;
import java.security.MessageDigest;

class Main
{
    public static void main(String[] args) 
    {
        // command line input으로부터 port number 얻기
        int port_no = Integer.parseInt(args[0]);
        
        // JOIN 명령어 인식해서 Peer 클래스 만들기
        Scanner s = new Scanner(System.in);
        String join_room = s.nextLine();
        String[] commands = join_room.split(" ");

        if (!commands[0].equals("#JOIN")) 
        {
            System.out.println("Wrong Command!");
            System.exit(1);
        }
        
        Peer peer = new Peer(commands[1], commands[2], port_no, s);
        System.gc();
    
    }
}

@SuppressWarnings("deprecation")
class Peer
{
    public Peer(String chatroom_name, String user_name, int port_no, Scanner sc)
    {

        String address = null;

        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(chatroom_name.getBytes());
            byte[] seq = md.digest();
            int first =  Byte.toUnsignedInt(seq[seq.length-3]);
            int second = Byte.toUnsignedInt(seq[seq.length-2]);
            int third =  Byte.toUnsignedInt(seq[seq.length-1]);
            address =  String.format("%d.%d.%d.%d", 225, first, second, third);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        String ip = address;

        class Receive implements Runnable
        {
            // private final MulticastSocket socket;
            private volatile boolean receive_flag = true;

            public void terminate()
            {
                receive_flag = false;
            }

            public void receive_message(String ip, int port_no, String user_name) throws IOException
            {
                byte[] buffer = new byte[512];
                MulticastSocket socket = new MulticastSocket(port_no);
                
                InetAddress group = InetAddress.getByName(ip);
                socket.joinGroup(group);

                while(receive_flag)
                {
                    // System.out.println("Waiting for multicast message...");
                    DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    // System.out.println("Got Message");
                    String msg = new String(packet.getData(),packet.getOffset(),packet.getLength());
                    if (msg.substring(0, Math.min(19, msg.length())).equals("[netinfo] EXITTED: "))
                    {
                        if (msg.substring(19).equals(user_name))
                        {
                            break;
                        }
                    }
                    else
                    {
                        System.out.println(msg);
                    }
                    
                }

                System.out.println("Finished");

                socket.leaveGroup(group);
                socket.close();
            }

            @Override
            public void run() 
            {
                try
                {
                    receive_message(ip, port_no, user_name);
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        Runnable send = new Runnable() 
        {
            public boolean flag = true;

            public static void send_message(String ip, int port_no, String message) throws IOException
            {
                DatagramSocket ds = new DatagramSocket();
                InetAddress group = InetAddress.getByName(ip);
                byte[] msg = message.getBytes();
                DatagramPacket dp = new DatagramPacket(msg, msg.length, group, port_no);
                // System.out.println("[Sender] Sending Message: "+message);
                ds.send(dp);
                ds.close();
            }

            @Override
            public void run() 
            {
                try
                {
                    while(flag)
                    {

                        String msg = sc.nextLine();
                        if (msg.charAt(0) == '#')
                        {
                            // #EXIT command
                            if(msg.substring(1, 5).equals("EXIT"))
                            {
                                send_message(ip, port_no, "[netinfo] EXITTED: "+user_name);
                                break;
                            }
                        }

                        // else, it is a message, so send it
                        else
                        {
                            send_message(ip, port_no, user_name+": "+msg);
                        }
                    }

                    sc.close();
                    
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        Receive receive = new Receive();
        Thread receive_thread = new Thread(receive);
        Thread send_thread = new Thread(send);

        receive_thread.start();
        try
        {
            send_thread.start();
            send_thread.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // System.out.println("stop");
        receive.terminate();
        // receive.socket.close();
        receive_thread.interrupt();

        System.out.println(receive_thread.isInterrupted());
        System.out.println(receive_thread.getState());
        System.out.println(send_thread.getState());

        // System.exit(0);
    }
}