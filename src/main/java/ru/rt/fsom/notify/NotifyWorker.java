package ru.rt.fsom.notify;

import com.comptel.mds.sas5.taskengine.rmi_fifo.RMIFifoClient;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import ru.rt.fsom.RmiFifoClient;
import ru.rt.fsom.conf.Conf;
import ru.rt.fsom.dict.Params;
import static ru.rt.fsom.dict.Params.LOGGER;
import ru.rt.oms.OrderStatusNotification;

@Stateless
public class NotifyWorker {
    private static final int ERR_MIN_LENGHT = 1024;
    
    @EJB private NotifyPersist notifyPersist;
    @EJB private RmiFifoClient rmiFifoClient;
    @EJB private Conf conf;
    
    public void doWork(Set<NotifyQueue> queues, 
	    ConcurrentHashMap<String, NotifyHelper> notifyHelpers, 
	    ConcurrentHashMap<String, String> sended, 
	    ConcurrentHashMap<String, String> noSended){
	LOGGER.log(Level.INFO, "В базе есть {0} не отправленных нотификаций. Для {1} из них нет связанных задач!", new Object[]{queues.size(), noSended.size()});
	RMIFifoClient client = conf.getRMIFifoClient();
	if (client == null){
	    Params.LOGGER.log(Level.SEVERE, "Нет подключения к InstantLink! RMIFIFOClient is null!");
	    return;
	}
	queues.parallelStream()
	.forEach(notifyQueue->{	    
	    final String orderId = notifyQueue.getOrderId();
	    final String omsId = notifyQueue.getOmsId();	 
	    final String helperKey = orderId + " " + omsId;
	    NotifyHelper notifyHelper = notifyHelpers.get(helperKey);
	    if (notifyHelper == null){
		notifyHelper = new NotifyHelper(orderId, omsId);
		//LOGGER.log(Level.INFO, "{0} {1} Создан новый обработчик ", new Object[]{orderId, omsId});
		notifyHelpers.put(helperKey, notifyHelper);
	    }
	    try {
		if (notifyHelper.isWork()) { //обработчик работает
		    LOGGER.log(Level.INFO, "{0} {1} Обработчик уже выполняется!", new Object[]{orderId, omsId});
		} else { //если обработчик не работает
		    notifyHelper.setRun();
		    Set<OrderStatusNotification> notifies = notifyPersist.loadNotCompletedNotifies(orderId, omsId);
		    notifies.forEach(notify->{
			rmiFifoClient.sendNotifToIL(orderId, omsId, notify, client, sended, noSended);
		    });
		    notifyHelper.setStop();
		    notifyHelpers.remove(helperKey);
		}		
	    } catch(Exception ex){
		LOGGER.log(Level.SEVERE, "{0} {1} ERROR occurred: {2}", new Object[]{orderId, omsId, getShortMsg(ex.getMessage())});
	    }
	});
	//LOGGER.log(Level.INFO, "------ doWork finish -----");
    }
    
    private String getShortMsg(String longMsg){
	if (longMsg == null) return "";
	return longMsg.substring(0, Math.min(ERR_MIN_LENGHT, longMsg.length()));
    }
}