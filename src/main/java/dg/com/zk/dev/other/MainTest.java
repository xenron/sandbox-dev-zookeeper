package dg.com.zk.dev.other;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainTest {

	public static void main(String[] args) throws IOException {
		InetSocketAddress address = new InetSocketAddress("www.baidu.com",80);
		InetAddress ia = address.getAddress();
	       InetAddress resolvedAddresses[] = InetAddress.getAllByName((ia!=null) ? ia.getHostAddress():
               address.getHostName());
           for (InetAddress resolvedAddress : resolvedAddresses) {
               // If hostName is null but the address is not, we can tell that
               // the hostName is an literal IP address. Then we can set the host string as the hostname
               // safely to avoid reverse DNS lookup.
               // As far as i know, the only way to check if the hostName is null is use toString().
               // Both the two implementations of InetAddress are final class, so we can trust the return value of
               // the toString() method.
        	   System.out.println(resolvedAddress.toString());
               if (resolvedAddress.toString().startsWith("/") 
                       && resolvedAddress.getAddress() != null) {
//                   this.serverAddresses.add(
//                           new InetSocketAddress(InetAddress.getByAddress(
//                                   address.getHostName(),
//                                   resolvedAddress.getAddress()), 
//                                   address.getPort()));
            	   System.out.println(resolvedAddress.getHostAddress());
               } else {
            	   System.out.println(resolvedAddress.getHostAddress());
                   //this.serverAddresses.add(new InetSocketAddress(resolvedAddress.getHostAddress(), address.getPort()));
               }  
           }		
		// TODO Auto-generated method stub
//		List<String> address = new ArrayList<String>();
//		address.add("aaa");
//		address.add("bbb");
//		address.add("ccc");
//		address.add("ddd");
//		address.add("eee");
//		Collections.shuffle(address);
//		for(String s : address){
//			System.out.println(s);
//		}
//		System.out.println("------------");
//		Collections.shuffle(address);
//		for(String s : address){
//			System.out.println(s);
//		}		
	}

}
