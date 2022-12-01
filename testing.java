import java.lang.reflect.Array;
import java.util.ArrayList;

class Access
{
    public static void main(String[] args) {
        ArrayList<Integer> al = new ArrayList<>();
        al.add(1);

        B b = new B(al);
        b.start();

        al.add(2);
        System.out.println("Added");


    }
}

class B extends Thread
{
    ArrayList<Integer> al;

    public B(ArrayList<Integer> al)
    {
        this.al = al;
    }

    @Override
    public void run() {
        try{Thread.sleep(500);}catch(Exception e){}
        for(int a:al)
        {
            System.out.println(a);
        }
    }
}