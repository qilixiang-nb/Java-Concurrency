/**
 * ThreadLocal是JDK包提供的，它提供了线程本地变量
 * 如果你创建了一个ThreadLocal变量，那么访问这个变量的每个线程都会有这个变量的一个本地副本
 * 当多个线程操作这个变量时，实际操作的是自己本地内存里面的变量，从而避免了线程安全问题
 */
public class ThreadLocalTest{
    static void print(String str){
        //打印当前线程本地内存中localVariable变量的值
        System.out.println(str + ":" + localVariable.get());
        //清除当前线程本地内存中的localVariable变量
        localVariable.remove();
    }

    //创建ThreadLocal变量
    static ThreadLocal<String> localVariable = new ThreadLocal<>();
    public static void main(String[] args){
        Thread threadOne = new Thread(new Runnable(){
            public void run(){
                //设置线程1中本地变量localVariable的值
                localVariable.set("threadone local variable");
                print("threadOne");
                System.out.println("threadOne remove after"+":"+localVariable.get());
            }
        });

        Thread threadTwo = new Thread(new Runnable(){
            public void run(){
                //设置线程1中本地变量localVariable的值
                localVariable.set("threadTwo local variable");
                print("threadTwo");
                System.out.println("threadTwo remove after"+":"+localVariable.get());
            }
        });

        threadOne.start();
        threadTwo.start();
    }
}

//ThreadLocal的实现原理
//Thread类中有一个threadLocals和一个inheritableThreadLocals，均为ThreadLocalMap类型的变量
//每个线程的本地变量不是存放在ThreadLocal实例中，而是存放在调用线程的threadLocals变量里面，即ThreadLocal类型的本地变量存放在具体的线程内存空间中
//分析ThreadLocal的set,get,remove方法的实现逻辑

//1.void set(T value)
public void  set(T value){
    Thread t = Thread.currentThread();
    //将当前线程作为key，去查找对应的线程变量，找到则设置
    ThreadLocalMap map = getMap(t);
    if(map!=null){
        map.set(this,value);
    }
    else{
        //第一次调用就创建当前线程的HashMap
        createMap(t,value);
    }

ThreadLocalMap getMap(Thread t){
    return t.threadLocals;
}

void createMap(Thread t,T firstValue){
    t.threadLocals = new ThreadLocalMap(this,firstValue);
}

//2.T get()
public T get(){
    Thread t = Thread.currentThread();
    //获取当前线程的threadLocals变量
    ThreadLocalMap map = getMap(t);
    if(map!=null){
        ThreadLocalMap.Entry e = map.getEntry(this);
        if(e!=null){
            @SuppressWarning("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    //threadLocals为空则初始化当前线程的threadLocals成员变量
    return setInitialValue();
}

private T setInitialValue(){
    T value = initialValue();
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if(map!=null){
        map.set(this,value);
    }
    else{
        createMap(t,value);
    }
    return value;
}

protected T initialValue(){
    return null;
}

3.void remove()
public void remove(){
    ThreadLocalMap m=getMap(Thread.currentThread());
    if(m!=null){
        m.remove(this);
    }
}

//总结:在每个线程内部都有一个名为threadLocals的成员变量(变量类型为HashMap),其中key为我们定义的ThreadLocal变量的this引用
//value则为我们使用set方法设置的值

//ThreadLocal不支持继承性
public class TestThreadLocal {
    public static ThreadLocal<String> threadLocal = new ThreadLocal<String>();
    public void main(String[] args){
        threadLocal.set("hello world");
        //启动子线程
        Thread thread = new Thread(new Runnable(){
            public void run(){
                //子线程输出线程变量的值
                System.out.println("thread:"+threadLocal.get());
            }
        });
        thread.start();
        //主线程输出线程变量的值
        System.out.println("main:"+threadLocal.get());

    }
}
//同一个ThreadLocal变量在父线程中被设置值后，在子线程中是获取不到的


/**
 * Unsafe类
 * JDK的rt.jar包中的Unsafe类提供了硬件级别的原子性操作
 */
public class TestUnsafe {
    //获取Unsafe实例
    static final Unsafe unsafe = Unsafe.getUnsafe();
    //记录变量state在类TestUnsafe中的偏移值
    static final long stateOffset;
    private volatile long state = 0;
    static {
        try{
            //获取state变量在类TestUnSafe中的偏移值
            stateOffset = unsafe.objectFieldOffset(TestUnsafe.class.getDeclaredField("state"));
        }catch(Exception ex){
            System.out.println(ex.getLocalizedMessage());
            throw new Error(ex);
        }
    }
    public static void main(String[] args){
        //创建实例，并且设置state值为1
        TestUnsafe test = new TestUnsafe();
        Boolean sucess = unsafe.compareAndSwapInt(test,stateOffset,0,1); //如果test对象中内存偏移量为stateOffset的state变量的值为0，则更新该值为1
        System.out.println(sucess);
    }
}


/**
 * Java并发包中ThreadLocalRandom类原理剖析
 * Random的缺点是多个线程会使用同一个原子性种子变量，从而导致对原子变量更新的竞争
 * 为了弥补多线程高并发情况下Random的缺陷，在JUC包下新增了ThreadLocalRandom类
 */
public class RandomTest{
    public static void main(String[] args) {
        //获取一个随机数生成器
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i=0;i<10;i++){
            System.out.println(random.nextInt(5));
        }
    }
}

//ThreadLocalRandom类似于ThreadLocal类，就是个工具类，当线程调用ThreadLocalRandom的current方法时
//ThreadLocalRandom负责初始化调用线程的threadLocalRandomSeed变量，也就是初始化种子(具体的种子存放在具体的调用线程的threadLoaclRandomSeed变量之中)
