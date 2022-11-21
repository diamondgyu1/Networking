import java.io.IOException;
import java.util.Scanner;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
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
    
    }
}

@SuppressWarnings("deprecation")
class Peer
{
    public Peer(String chatroom_name, String nickname, int port_no, Scanner sc)
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

            public void receive_message(String ip, int port_no) throws IOException
            {
                byte[] buffer = new byte[512];
                MulticastSocket socket = new MulticastSocket(port_no);
                
                InetAddress group = InetAddress.getByName(ip);
                socket.joinGroup(group);

                while(receive_flag)
                {
                    try
                    {
                        // System.out.println("Waiting for multicast message...");
                        DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        // System.out.println("Got Message");
                        int packets = Integer.parseInt(new String(packet.getData(),packet.getOffset(),packet.getLength()));
                        socket.receive(packet);
                        String nickname = new String(packet.getData(),packet.getOffset(),packet.getLength());
                        System.out.print(nickname+": ");

                        for (int i = 0; i<packets; i++)
                        {
                            socket.receive(packet);
                            System.out.print(new String(packet.getData(),packet.getOffset(),packet.getLength()));
                        }
                        System.out.println();
                    }
                    catch (Exception e)
                    {
                        System.out.println("Interrupted");
                        e.printStackTrace();
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
                    receive_message(ip, port_no);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        class Send implements Runnable
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
                        // Runtime.getRuntime().exec("clear");
                        if (msg.charAt(0) == '#')
                        {
                            // #EXIT command
                            if(msg.substring(1, 5).equals("EXIT"))
                            {
                                break;
                            }
                        }

                        // else, it is a message, so send it
                        else
                        {
                            int packets = msg.length()/512 + 1;
                            send_message(ip, port_no, Integer.toString(packets));
                            send_message(ip, port_no, nickname);
                            for (int i = 0; i<packets; i++)
                            {
                                send_message(ip, port_no, msg.substring(i*512, Math.min((i+1)*512, msg.length())));
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        Thread receive_thread = new Thread(new Receive());
        Thread send_thread = new Thread(new Send());

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

        receive_thread.interrupt();
        sc.close();
        System.exit(0);
    }
    
}