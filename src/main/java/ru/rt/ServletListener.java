package ru.rt;

import javax.ejb.EJB;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import ru.rt.fsom.conf.Conf;

public class ServletListener implements ServletContextListener {
    @EJB private Conf conf;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {	
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
	conf.rmiFifoClientUnregistr();
    }
}