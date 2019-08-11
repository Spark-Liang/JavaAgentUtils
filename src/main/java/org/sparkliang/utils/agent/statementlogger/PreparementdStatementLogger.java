package org.sparkliang.utils.agent.statementlogger;

import org.sparkliang.utils.agent.exception.AgentException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreparementdStatementLogger {
    private static final String LOG_PATH = System.getProperty("user.home") + "/tmp/PreparedStatementLogAgent/SQL.log";;

    private static boolean IS_INITED = false;
    private static PrintWriter OUT = null;
    private static Map<PreparedStatement, PreparedStatementRecord> STMT_MAP
            = new HashMap<PreparedStatement, PreparedStatementRecord>();
    private final static Object  STMT_MAP_LOCK = new Object();



    public static void logSQL(PreparedStatement pstmt, String sql){
        PreparedStatementRecord record = getRecord(pstmt);
        record.logSql(pstmt,sql);
    }

    public static void logSetParemeter(PreparedStatement pstmt, int paramIndex,Object value){
        PreparedStatementRecord record = getRecord(pstmt);
        record.logParameter(pstmt,paramIndex,value);
    }

    private static PreparedStatementRecord getRecord(PreparedStatement pstmt){
        PreparedStatementRecord record = null;
        if(STMT_MAP.containsKey(pstmt)){
            record = STMT_MAP.get(pstmt);
        }else{
            synchronized (STMT_MAP_LOCK){
                if(STMT_MAP.containsKey(pstmt)){
                    record = STMT_MAP.get(pstmt);
                }else{
                    record = new PreparedStatementRecord(pstmt);
                    STMT_MAP.put(pstmt,record);
                }
            }
        }
        return record;
    }

    public static void logExecute(PreparedStatement pstmt) {
        Date current = new Date();
        PreparedStatementRecord record = getRecord(pstmt);
        OUT.println(String.format(
                "%s : %s;",current.toGMTString(),record.toString()
        ));
        OUT.flush();
    }

    public static Map<PreparedStatement, PreparedStatementRecord> getCurrentStatementRecord(){
        return new HashMap<>(STMT_MAP);
    }


    static {
        try {
            OUT = new PrintWriter(new OutputStreamWriter(new FileOutputStream(LOG_PATH)));
        } catch (FileNotFoundException e) {
            String msg = String.format("Failed to open the log file. Reason is '%s'", e.getMessage());
            throw new AgentException(msg);
        }
    }
}
