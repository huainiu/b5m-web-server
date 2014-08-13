import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SpringApp {
	
	public static void main(String[] args) throws IOException {
		 ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"application-service.xml", "application-cache.xml"});
		 System.out.println(context);
		 System.in.read();
	}
	
}
