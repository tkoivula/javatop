package org.koivula.javatop;

import java.util.ArrayList;
import java.io.*;

public class Processes {

    public static String getCurrentPid() throws Exception {
        java.lang.management.RuntimeMXBean runtime = 
        java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        sun.management.VMManagement mgmt =  
              (sun.management.VMManagement) jvm.get(runtime);
        java.lang.reflect.Method pid_method =  
               mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);
        int pid = (Integer) pid_method.invoke(mgmt);     
        return "" + pid;
    }

    public static String findProcess(String name) {
        try {
            // current jvm is one java process as well. Do not monitor itself.
            String currentPid = getCurrentPid();

            Process p = Runtime.getRuntime().exec("ps -eo pid,comm");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String process;
            ArrayList<String> pids = new ArrayList<>();
            while ((process = input.readLine()) != null) {
                String[] pidCmd = process.trim().split(" ");
                if (pidCmd.length > 1 && pidCmd[1].equals(name) &&
                       !pidCmd[0].equals(currentPid)) {
                     pids.add(pidCmd[0]);
                }
            }
            input.close();

            if (pids.size() == 1) {
                // found exactly one, use that one
                return pids.get(0);
            }
            // found either zero or multiple, cannot know what the caller wants

         } catch (Throwable t) {
            // just return null
         }
         return null;
    }

}
