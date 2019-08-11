package org.sparkliang.utils.agent;

import org.apache.log4j.Logger;

import java.lang.instrument.Instrumentation;

public class PreparedStatementAgent {
    private static Logger LOGGER = Logger.getLogger(PreparedStatementAgent.class);

    public static void premain(String args, Instrumentation inst){
        LOGGER.info("run premain program");

//        try (InputStream configFile = PreparedStatementAgent.class.getClassLoader().getResourceAsStream("AgentConfig.properties")){
//            Properties config = new Properties();
//            config.load(configFile);
//            Boolean isNeedClassfileDump = config.contains("IsGenerateClassFileDump")?
//                    (Boolean) config.get("IsGenerateClassFileDump") : Boolean.FALSE;
//            if(isNeedClassfileDump && config.contains("GenerateClassFileDumpPath")){
//                String generateClassFileDumpPath = (String)config.get("GenerateClassFileDumpPath");
//                inst.ad
//            }
//
//        }catch (Exception e){}
        inst.addTransformer(new PreparedStatementTransformer());
    }


}
