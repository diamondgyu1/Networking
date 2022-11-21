class A
{
    public static void main(String[] args) {
        R rec = new R();
        Thread t = new Thread(rec);
        t.start();
        System.out.println("thread started");

        try{
            Thread.sleep(5000);
        } catch(Exception e) { e.printStackTrace();}

        System.out.println("thread terminating");
        t.interrupt();

        // rec.flag = false;
    }
}

class R implements Runnable{

    boolean flag = true;

    @Override
    public void run() {
        while(true)
        {
            System.out.println("hi");
            System.out.println(Thread.currentThread().isInterrupted());
            try{
                Thread.sleep(1000);
            } catch(Exception e) { e.printStackTrace();}
            
        }
    }
}