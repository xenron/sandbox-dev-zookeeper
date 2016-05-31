package dg.com.configManagement.web;

import java.util.Calendar;
import java.util.Date;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import dg.com.configManagement.web.service.OrderRepository;
import dg.com.configManagement.web.service.OrderService;
import dg.com.configManagement.web.service.ProductRepository;
import dg.com.configManagement.web.service.pojo.Order;
import dg.com.configManagement.web.service.pojo.Product;
import dg.com.configManagement.web.util.LockUtil;

public class Test {
    private static ApplicationContext ctx;  
    
//    static 
//    {  
//        ctx = new ClassPathXmlApplicationContext("applicationContext.xml");  
//    }        
//    
    public static void testpurchase(){
    	OrderService os = (OrderService)ctx.getBean("orderService"); 
    	
        Order order = new Order();
        order.setProductId(1L);
        order.setCreateTime(new Date());
        order.setPnum(1);
        os.doOrder(order);
    }
    
    public static void testMapper(){
    	ProductRepository mapper = (ProductRepository)ctx.getBean("productMapper"); 
        //����id=1���û���ѯ���������ݿ��е���������Ըĳ����Լ���.
        System.out.println("�õ��û�id=1���û���Ϣ");
        Product product = mapper.selectProductById(1L);
        System.out.println(product.getName()); 
        
        OrderRepository omapper = (OrderRepository)ctx.getBean("orderMapper"); 
        Order order = new Order();
        order.setProductId(1L);
        order.setCreateTime(new Date());
        order.setPnum(1);
        omapper.saveOrder(order);   	
    }
    
    public static void testShardLog(int type,String identity){
    	System.out.println("---------------��ʼ��ȡ��"+identity);
    	LockUtil.init("localhost:2181,localhost:2182");
    	LockUtil.getShardLock(type, identity);
    	System.out.println("---------------��ȡ������"+identity);
    }
    public static void main(String[] args)  
    {  

    	int type = Integer.parseInt(args[0]);
    	System.out.println("type="+type);
    	testShardLog(type,args[1]);
    	//testShardLog(0,"f6");
//    	try {
//    		LockUtil.init("localhost:2181");
//			//LockUtil.addChildWatcher("/LockService");
//    		LockUtil.getExclusiveLock();
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//    	long nowtime = Calendar.getInstance().getTimeInMillis();
//    	System.out.println("begin with "+nowtime);
//    	testpurchase();
//    	System.out.println("end with "+nowtime);
    //	String []names = ctx.getBeanDefinitionNames();
//    	for(String s: names){
//    		System.out.println(s);
//    	}
    	
    	try {
    		Thread.sleep(20000);
    		System.out.println("-------------��ʼ--�ͷ���");
    		LockUtil.unlockForShardLock();
    		System.out.println("-------------�ɹ�--�ͷ���");
			Thread.sleep(30000);
			System.out.println("---------------�����˳�");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } 
}
