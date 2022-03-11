package ru.rt.fsom.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rt.fsom.conf.Conf;
import ru.rt.fsom.dict.Params;
import ru.rt.fsom.utils.Utils;

/**
 * Модуль работы с задачами в базе данных
 * @author Maksim.Filatov
 */
@Stateless
@LocalBean
public class TaskPersist {
    private @EJB Conf conf;
    
    public int makeTaskAsCompleted(final String taskId, final String logInfo){
	Params.LOGGER.log(Level.INFO, "{0} Обновление статуса задачи для taskId={1} ", new Object[]{logInfo, taskId});
	int countUpdate = 0;
	String sql = "UPDATE ilink.task_persist SET \"IsComplete\" = true, \"DateComplete\" = ? WHERE \"TaskId\"= ?";
	try (Connection conn = conf.getJdbcConnection();
	    PreparedStatement ps = conn.prepareStatement(sql)
	){
	    ps.setTimestamp(1, Utils.getCurrentTimeStamp());
	    ps.setString(2, taskId);
	    countUpdate = ps.executeUpdate();
        } catch (SQLException ex) {
            Params.LOGGER.log(Level.SEVERE, "SQL State {0} error: {1}", new Object[]{ex.getSQLState(), ex.getMessage()});
        } catch (Exception ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex);
        }
	return countUpdate;
    }

    /* *** privates *** */	
    
    public TaskData loadTask(final String orderId, final String omsId, final String logInfo){
	String sql = "SELECT * FROM ilink.task_persist WHERE \"OmsId\" = ? AND \"OrderId\" = ? AND \"IsComplete\" = false";
        TaskData task = null;
	try (Connection conn = conf.getJdbcConnection();
	    PreparedStatement preparedStatement = conn.prepareStatement(sql)
	){
            if (conn != null) {	
		preparedStatement.setString(1, omsId);
		preparedStatement.setString(2, orderId);
		ResultSet resultSet = preparedStatement.executeQuery();
		if (resultSet.next()) {
		    task = new TaskData();
		    task.setTaskId(resultSet.getString(1));		    		    
		    task.setTaskType(resultSet.getString(4));
		    task.setNeType(resultSet.getString(5));
		    task.setOrderId(orderId);
		}
            } else {
                Params.LOGGER.log(Level.INFO, "{0} Failed to make jdbc connection!", new Object[]{logInfo});
            }	    
        } catch (SQLException ex) {
            Params.LOGGER.log(Level.SEVERE, "SQL state: {0} \nerror: {1}", new Object[]{ex.getSQLState(), ex.getMessage()});
        } catch (Exception ex) {
	    Params.LOGGER.log(Level.SEVERE, null, ex);
        }
	return task;
    }

}