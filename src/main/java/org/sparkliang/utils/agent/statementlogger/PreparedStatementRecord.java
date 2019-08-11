package org.sparkliang.utils.agent.statementlogger;

import org.sparkliang.utils.agent.exception.AgentException;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class PreparedStatementRecord {

    private PreparedStatement pstmt = null;
    private String sql;
    private List<Object> paramValues = new Vector<Object>();

    public PreparedStatementRecord(PreparedStatement pstmt) {
        this.pstmt = pstmt;
    }

    public void assertIsCorrespondingStmt(PreparedStatement pstmt) {
        if (!this.pstmt.equals(pstmt)) {
            throw new AgentException("Error! Is not the corresponding preparedStatement.");
        }
    }

    public void logSql(PreparedStatement pstmt, String sql) {
        assertIsCorrespondingStmt(pstmt);
        this.sql=sql;
    }

    public void logParameter(PreparedStatement pstmt, int paramIndex, Object value) {
        assertIsCorrespondingStmt(pstmt);
        paramValues.set(paramIndex - 1, value);
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParamValues() {
        return paramValues;
    }

    @Override
    public String toString() {
        List<Object> vals = new ArrayList<Object>(paramValues);
        StringBuilder paramString = new StringBuilder();
        int i = 0;
        final int maxI = vals.size();
        if (i < maxI) {
            paramString.append(vals.get(i));
            i++;
            while (i < maxI) {
                paramString.append(',');
                paramString.append(vals.get(i));
                i++;
            }
        }

        return String.format(
                "sql:'%s'\nparams:[%s]", sql, paramString.toString()
        );
    }
}
