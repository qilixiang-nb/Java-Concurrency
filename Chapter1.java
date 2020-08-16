/**
 * 线程创建与运行
 */
//1.继承Thread类的实现
public class ThreadTest {
    //继承Thread类并重写run方法
    public static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("I am a child thread");
        }
    }
    public static void main(String[] args) {
        //创建线程
        MyThread thread = new MyThread();
        //启动线程
        thread.start();
    }
}

//2.实现Runnable接口的run方法
public static class RunableTask implements Runnable {  //RunableTask可以继承其他类
    @Override
    public void run(){
        System.out.println("I am a child thread");
    }
}
public static void main(String[] args) throws InterruptedException {
    RunableTask task = new RunnableTask();
    new Thread(task).start();
    new Thread(task).start();  //两个线程共用一个task代码逻辑
}

//3.FutureTask可以拿到任务的返回结果

//创建任务类，类似Runable
public static class CallerTask implements Callable<String> {
    @Override
    public String call() throws Exception {
        return "hello";
    }
}
public static void main(String[] args) throws InterruptedException {
    //创建异步任务
    FutureTask<String> futureTask = new FutureTask<>(new CallerTask());
    //使用创建的FutrueTask对象作为任务创建一个线程并启动线程
    new Thread(futureTask).start();
    try{
        //等待任务执行完毕，并返回结果
        String result = futureTask.get();
        System.out.println(result);
    }catch(ExecutionException e){
        e.printStackTrace();
    }
}

/**
 * 线程等待与等待
 * wait()函数：
 * 当一个线程调用一个共享变量的wait()方法时，该调用线程会被阻塞挂起，直到
 * 1)其他线程调用了该共享变量的notify()或者notifyAll()方法
 * 2)其他线程调用了该线程的interrupt()方法，该线程抛出InterruptedException异常返回
 * 
 * 调用wait()方法的线程需获取共享变量的监视器锁，否则调用wait()时会抛出IllegalMonitorStateException异常返回
 * 1) 执行synchronized同步代码块时，使用该共享变量作为参数
 *    synchronized(共享变量){ //do something  }
 * 2) 调用共享变量的方法，且该方法使用synchronized修饰
 *    synchronized void add(int a,int b){ //do something}
 */

//生产线程
synchronized(queue) {
    //消费队列满，则等待队列空闲
    while(queue.size() == MAX_SIZE) {
        try {
            //挂起当前线程，并释放通过同步获取到的queue上的锁，让消费者线程可以获取该锁，然后获取队列中的元素
            queue.wait();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    //空闲则生成元素，并通知消费者线程
    queue.add(ele);
    queue.notifyAll();
}
//消费者线程
synchronized (queue) {
    //消费队列为空
    while(queue.size() == 0){
        try{
            //挂起当前线程，并释放通过同步获取到的queue上的锁，让生产者线程可以获取该锁，将生产元素放入队列
            queue.wait();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    //消费元素，并通知唤醒生产者线程
    queue.take();
    queue.notifyAll();
}

//当前线程调用共享变量的wait()方法后只会释放当前共享变量上的锁，若当前线程还持有其他共享变量的锁，则这些锁是不会被释放的

//创建资源
private static volatile Object resourceA = new Object();
private static volatile Object resourceB = new Object();

public static void main(String[] args) throws InterruptedException {
    //创建线程
    Thread threadA = new Thread(new Runnable(){
        public void run() {
            try {
                //获取resourceA共享资源的监视器锁
                synchronized(resourceA){
                    System.out.println("threadA get resourceA lock");
                    //获取resourceB共享资源的监视器锁
                    synchronized(resourceB){
                        System.out.println("threadA get resourceB lock");
                        //线程A阻塞，并释放获取到的resourceA上的锁
                        System.out.println("threadA release resourceA lock");
                        resourceA.wait(); 
                    } 
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    });

    //创建线程
    Thread threadB = new Thread(new Runnable(){
        public void run(){
            try{
                //休眠1s,为了让线程A先获取到锁
                Thread.sleep(1000);
                //获取resourceA共享资源的监视器锁
                synchronized(resourceA){
                    System.out.println("threadB get resourceA lock");
                    System.out.println("threadB try get resourceB lock...");

                    //获取resourceB共享资源的监视器锁(会被阻塞，线程A挂起自己后并没有释放获取到的resourceB上的锁)
                    synchronized(resourceB){
                        System.out.println("threadB get resourceB lock");
                        //线程B阻塞，并释放获取到的resourceA的锁
                        System.out.println("threadB release resoureA lock");
                        resourceA.wait();
                    }
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    });

    //启动线程
    threadA.start();
    threadB.start();
    //等待两个线程结束
    threadA.join();
    threadB.join();

    System.out.println("main over");
}

//当一个线程调用共享对象的wait()方法被阻塞挂起后，如果其他线程中断了该线程，则该线程会抛出InterruptedException异常并返回
public class WaitNotifyInterupt{
    static Object obj = new Object();
    public static void main(String[] args) throws InterruptedException {
        Thread threadA = new Thread(new Runable(){
            public void run(){
                try{
                    System.out.println("---begin---");
                    //阻塞当前线程
                    synchronized(obj){
                        obj.wait();
                    }
                    System.out.println("---end---");
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        threadA.start();
        Thread.sleep(1000);
        System.out.println("---begin interrupt threadA---");
        threadA.interrupt();
        System.out.println("---end interrupt threadB---");
    }
}
//中断后threadA在obj.wait()出抛出java.lang.InterruptedException异常而返回并终止


/**
 * notify()函数
 * 一个线程调用共享变量的notify()方法后，会唤醒一个在该共享变量上调用wait系列方法后被挂起的线程
 * notifyAll()函数则会唤醒所有在该共享变量上由于调用wait系列方法而被挂起的线程。
 */
private static volatile Object resourceA = new Object();
public static void main(String[] args) throws InterruptedException{
    Thread threadA = new Thread(new Runnable(){
        public void run(){
            synchronized(resourceA){
                System.out.println("threadA get resourceA lock");
                try{
                    System.out.println("threadA begin wait");
                    resourceA.wait();
                    System.out.println("threadA end wait");
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    });

    Thread threadB = new Thread(new Runnable(){
        public void run(){
            synchronized (resourceA){
                System.out.println("threadB get resourceA lock");
                try{
                    System.out.println("threadB begin wait");
                    resourceA.wait();
                    System.out.println("threadB end wait");
                }catch(InterruptedException e){
                    e.printStackTrace();
                }

            }
        }
    });

    Thread threadC =new Thread(new Runnable(){
        public void run(){
            synchronized (resourceA){
                System.out.println("threadC begin notify");
                resourceA.notify();
            }
        }
    });

    threadA.start();
    threadB.start();

    Thread.sleep(1000); //启用线程C前首先调用sleep()方法让主线程休眠，这样做的目的是让线程A和线程B全部执行到调用wait()后在调用线程C的notify()方法
    threadC.start();
    //等待线程结束
    threadA.join();
    threadB.join();
    threadC.join();
    System.out.println("main over");
} 

/**
 * 等待线程执行终止的join方法
 * 多个线程加载资源，需要等待多个线程全部加载完毕后再汇总处理
 * 线程A调用线程B的join方法后会被阻塞当，当其他线程调用了线程A的interrupt()方法中断了线程A时，线程A会抛出IntertuptedException异常而返回
 */

 public static void main(String[] args) throws InterruptedException {
     Thread threadOne = new Thread(new Runnable(){
         @Override
         public void run(){
             System.out.println("threadOne begin run!");
             for(;;){}
         }
     })；
     //获取主线程
     final Thread mainThread = Thread.currentThread();
     Thread threadTwo = new Thread(new Runnable(){
         @Override
         public void run(){
             try{
                 Thread.sleep(1000);
             }catch(InterruptedException e){
                 e.printStackTrace();
             }
             //中断主线程
             mainThread.interrupt();
         }
     });
     threadOne.start();
     threadTwo.start();
     try{//等待线程1执行结束
        threadOne.join();
     }catch(InterruptedException e){
         System.out.println("main thread:"+e);
     }
 }


//线程死锁
public class DeadLockTest2 {
    //创建资源
    private static Object resourceA = new Object();
    private static Object resourceB = new Object();

    public static void main(String[] args){
        Thread threadA = new Thread(new Runnable(){
            public void run() {
                synchronized(resourceA){
                    System.out.println(Thread.currentThread()+"get resourceA");
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread()+"waiting get resourceB");
                    synchronized(resourceB){
                        System.out.println(Thread.currentThread()+"get resourceB");
                    }
                }
            }
        });

        Thread threadB = new Thread(new Runnable(){
            public void run() {
                synchronized(resourceB){
                    System.out.println(Thread.currentThread()+"get resourceA");
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread()+"waiting get resourceA");
                    synchronized(resourceA){
                        System.out.println(Thread.currentThread()+"get resourceA");
                    }
                }
            }
        });

        threadA.start();
        threadB.start();
    }
}
