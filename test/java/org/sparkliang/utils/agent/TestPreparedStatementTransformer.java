package org.sparkliang.utils.agent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.sparkliang.utils.agent.statementlogger.PreparedStatementRecord;
import org.sparkliang.utils.agent.statementlogger.PreparementdStatementLogger;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@RunWith(JUnit4.class)
public class TestPreparedStatementTransformer {

    private Connection conn = null;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/../data/TestDB", "sa", "123456");
    }

    @Test
    public void testCanLogSql()throws Exception {
        try {
            //given
            String sql = "select * from test.testtable";
            PreparedStatement stmt = conn.prepareStatement(sql);

            //when
            Map<PreparedStatement, PreparedStatementRecord> currentStatments = PreparementdStatementLogger.getCurrentStatementRecord();

            //then
            Assert.assertTrue(currentStatments.containsKey(stmt));
            PreparedStatementRecord record = PreparementdStatementLogger.getCurrentStatementRecord().get(stmt);
            Assert.assertEquals(sql, record.getSql());
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

    }

    @Test
    public void testCanLogParameter() throws Exception{
        try {
            //given
            String sql = "insert into test.testtable values(?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            int intVal = 1;
            String stringVal = "abcd";
            stmt.setInt(1, intVal);
            stmt.setString(2, stringVal);


            //when
            Map<PreparedStatement, PreparedStatementRecord> currentStatments = PreparementdStatementLogger.getCurrentStatementRecord();

            //then
            Assert.assertTrue(currentStatments.containsKey(stmt));
            PreparedStatementRecord record = PreparementdStatementLogger.getCurrentStatementRecord().get(stmt);
            Assert.assertArrayEquals(
                    new Object[]{intVal, stringVal}, record.getParamValues().toArray()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

    }

}