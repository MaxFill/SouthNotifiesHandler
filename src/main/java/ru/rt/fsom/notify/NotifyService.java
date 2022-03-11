package ru.rt.fsom.notify;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import static ru.rt.fsom.dict.Params.LOGGER;

@Startup
@Singleton
public class NotifyService {
    private static final int DEFAULT_TIMER_LAUNCH_FREQUENCY = 6000;  //частота запуска таймера нотификаций в милисекундах

    private final ConcurrentHashMap<String, NotifyHelper> notifyHelpers = new ConcurrentHashMap<>(); 
    private final ConcurrentHashMap<String, String> sended = new ConcurrentHashMap<>(); 
    private final ConcurrentHashMap<String, String> noSended = new ConcurrentHashMap<>();
    
    @EJB private NotifyWorker notifyWorker;
    @EJB private NotifyPersist notifyPersist;
    
    @Resource 
    TimerService timerService;
    
    @PostConstruct
    private void init(){	
	int interval = DEFAULT_TIMER_LAUNCH_FREQUENCY;
	LOGGER.log(Level.INFO, "Запуск таймера.... ");
	TimerConfig timerConfig = new TimerConfig();
	timerConfig.setPersistent(false);
	Date startDate = new Date();
	timerService.createIntervalTimer(startDate, interval, timerConfig);		
	LOGGER.log(Level.INFO, "Таймер инициализирован! Частота повторов = {0} sec.", interval / 1000);
    }
    
    @Timeout
    public void doTimer(Timer timer) {
	Set<NotifyQueue> notifyQueues = notifyPersist.makeNotifyQueue();
	if (notifyQueues.size() > 0){	    
	    notifyWorker.doWork(notifyQueues, notifyHelpers, sended, noSended);
	}
    }
 
}