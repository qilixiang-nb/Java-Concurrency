//主要方法源码解析

//1.初始化
public CopyOnWriteArrayList()
{
    //无参构造函数
    setArray(new Object[0]);
}

//有参构造函数：创建一个List,其内部元素是入参toCopyIn的副本
public CopyOnWriteArrayList(E[] toCopyIn)
{
    setArray(Arrays.copyOf(toCopyIn,toCopyIn.length,Object[].class);)
}

//入参为集合，将集合里面的元素复制到本List
public CopyOnWriteArrayList(Collection<? extends E> c)
{
    Object[] elements;
    if(c.getClass() == CopyOnWriteArrayList.class)
    {
        elements = ((CopyOnWriteArrayList<?>)c).getArray();
    }
    else 
    {
        elements = c.toArray();
        //c.toArray might (incorrectly) not return Object[]
        if(elements.getClass()!=Object[].class)
        {
            elements = Arrays.copyOf(elements,elements.length,Object[].class)
        }
        setArray(elements);
    }
}


//2.添加元素：add(E e) add(int index,E element) addIfAbsent(E e)  addAllAbsent(Collection<? extends E> c)
//由于加了锁，整个add过程是个原子性操作。
public boolean add(E e)
{
    //获取独占锁,如果多个线程都调用add方法则只有一个线程会获取到该锁，其他线程会被阻塞挂起直到锁被释放。
    final ReentrantLock lock = this.lock;
    lock.lock();
    try
    {
        //获取array
        Object[] elements = getArray();
        //复制array到新数组，添加元素到新数组
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements,len+1);
        newElements[len] = e;
        //使用新数组替换添加前的数组
        setArray(newElements);
        return true;
    }
    final{
        //释放独占锁
        lock.unlock();
    }
} 


//3.获取指定位置元素：E get(int index),如果元素不存在则抛出IndexOutOfBoundsException异常。
//整个过程没有进行加锁操作，写时复制策略会产生弱一致性问题
public E get(int index)
{
    return get(getArray(),index);
}

final Object[] getArray()
{
    return array;
}

private E get(Object[] a,int index)
{
    return (E) a[index];
}


//4.修改指定元素
public E set(int index,E element)
{
    final ReentrantLock lock = this.lock;
    lock.lock();
    try{
        Object[] elements = getArray();
        E oldValue = get(elements,index);
        if(oldValue != element)
        {
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements,len);
            newElements[index] = element;
            setArray(newElements);
        }
        else
        {
            //Not quite a no-op;ensures volatile write semantics
            setArray(elements);
        }
        return oldValue;
    }
    finally
    {
        lock.unlock();
    }
}


//5.删除元素
public E remove(int index)
{
    //获取独占锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try{
        //获取数组
        Object[] elements = getArray();
        int len = elements.length;
        //获取指定元素
        E oldValue = get(elements,index);
        int numMoved = len-index-1;
        //如果要删除的是最后一个元素
        if(numMoved == 0)
        {
            setArray(Arrays.copyOf(elements,len-1));
        }
        else
        {
            //分两次复制删除后剩余的元素到新数组
            Object[] newElements = new Object[len-1];
            System.arraycopy(elements,0,newElements,0,index);
            System.arraycopy(elements,index+1,newElements,index,numMoved);
            //使用新数组代替老数组
            setArray(newElements);
        }
        return oldValue;
    }
    final{
        lock.unlock();
    }
}


//6.弱一致性的迭代器：返回迭代器后，其他线程对list的增删改对迭代器是不可见的

//使用迭代器
public static void main(String[] args)
{
    CopyOnWriteArrayList<String> arrayList = new CopyOnWriteArrayList<>();
    arrayList.add("Hello");
    arrayList.add("alibaba");
    Iterator<String> itr = arrayList.iterator();
    while(itr.hasNext())
    {
        System.out.println(iter.next());
    }
}

//实现CopyOnWriteArrayList中迭代器的弱一致性
public Iterator<E> iterator()
{
    return new COWIterator<E>(getArray(),0);
}

static final class COWIterator<E> implements ListIterator<E>
{
    //array的快照版本,snapshot保存了当前list的内容。
    private final Object[] snapshot;
    //数组下标
    private int cursor;
    //构造函数
    private COWIterator(Object[] elements,int initialCursor)
    {
        cursor = initialCursor;
        snapshot = elements;
    }
    //是否遍历结束
    public boolean hasNext()
    {
        return cursor < snapshot.length;
    }
    //获取元素
    public E next()
    {
        if(!hasNext())
        {
            throw new NoSuchElementException();
        }
        return (E)snapshot[cursor++];
    }
}

//演示多线程下迭代器的弱一致性的效果
//主线程在子线程执行完毕后使用获取的迭代器遍历数组元素，从子线程里面进行的操作一个也没有生效，这就是
//迭代器弱一致性的体现，获取迭代器的操作必须在子线程操作之前进行。
public class CopyList 
{
    private static volatile CopyOnWriteArrayList<String> arrayList = new CopyOnWriteArrayList<>();
    public static void main(String[] args)
    {
        arrayList.add("hello");
        arrayList.add("NingBo");
        arrayList.add("welcome");
        arrayList.add("to");
        arrayList.add("Wuhan");

        Thread threadOne = new Thread(new Runnable(){
            @Override
            public void run(){
                //修改list中下标为1的元素为NB
                arrayList.set(1,"NB");
                //删除元素
                arrayList.remove(2);
                arrayList.remove(3);
            }
        });
        //保证在修改线程启动前获取迭代器
        Iterator<String> itr = arrayList.iterator();
        //启动线程
        threadOne.start();
        //等待子线程执行完毕
        threadOne.join();
        //迭代元素
        while(iter.hasNext())
        {
            System.out.println(iter.next());
        }
    }
}

//总结：CopyOnWriteArrayList使用写时复制策略来保证List的一致性，而获取——修改——写入三步操作并不是
//原子性的，所以在增删改的过程中都使用了独占锁，来保证在某个时间只有一个线程能对List数组进行修改。
//另外CopyOnWriteArrayList提供了弱一致性的迭代器，从而保证在获取迭代器后，其他线程对List的修改是不可见的
//迭代器遍历的数组是一个快照。