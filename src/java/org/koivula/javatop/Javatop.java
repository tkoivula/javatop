package org.koivula.javatop;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Collection;
import java.util.Arrays;
import sun.tools.jstack.JStack;
import java.io.*;
import java.util.regex.*;

public class Javatop {

    private static void printUsage() {
        System.out.println("\nUsage: javatop [options..] [pid]");
        System.out.println("       javatop -i [file] [options..]\n");

        System.out.println("Collect and show thread information of a Java application by taking");
        System.out.println("multiple thread dump samples by connecting to the JVM similarly than");
        System.out.println("jstack tool.\n");

        System.out.println("Options:");
        System.out.println("    -t N     Take samples in N msec intervals (default: 100)");
        System.out.println("    -c N     Take total of N samples (default: 20)");
        System.out.println("    -w M     Print results using width N (default: 40)");
        System.out.println("    -j       Do not hide built-in JVM threads");
        System.out.println("    -b       Print black and white instead of ANSI colors");
        System.out.println("    -o FILE  Write samples into a file instead of printing results");
        System.out.println("    -a FILE  Take one sample and append it into a file. This option");
        System.out.println("             can be used from scripts to collect thread information");
        System.out.println("             of longer periods to be analyzed later");
        System.out.println("    -i FILE  Read samples from given file instead of connecting to a");
        System.out.println("             JVM instance and show results");
        System.out.println("    -r N-M   Print results from samples N-M (useful with -i option)");
        System.out.println("    -n N     Print a thread dump of sample N (useful with -i option)");
        System.out.println("    -s       Take pme sample and print thread dump (jstack)");
        System.out.println("    -d       Print debug information");

        System.out.println("\nFor more details see <https://github.com/tkoivula/javatop>\n");
    }

    public static void main(String[] args) throws Exception {
        // Params and their defaults:
        int times = 20;
        int interval = 100;
        int width = 40;

        Integer min = null;
        Integer max = null;

        boolean hideJvmThreads = true;
        boolean append = false;
        File outputFile = null;
        File inputFile = null;
        boolean colors = true;
 
        boolean debug = false;
        boolean ok = true;
        boolean printStack = false;
        Integer printNthSample = null;

        String pid = null;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                ok = false;
            } else if (args[i].equals("-d")) {
                debug = true;
            } else if (args[i].equals("-t")) {
                interval = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-c")) {
                times = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-w")) {
                width = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-o")) {
                outputFile = new File(args[++i]);
            } else if (args[i].equals("-a")) {
                outputFile = new File(args[++i]);
                append = true;
                times = 1;
            } else if (args[i].equals("-i")) {
                inputFile = new File(args[++i]);
            } else if (args[i].equals("-j")) {
                hideJvmThreads = false;
            } else if (args[i].equals("-b")) {
                colors = false;
            } else if (args[i].equals("-r")) {
                String[] minMax = args[++i].split("-");
                min = Integer.parseInt(minMax[0]);                
                max = Integer.parseInt(minMax[1]);                
            } else if (args[i].equals("-n")) {
                printNthSample = Integer.parseInt(args[++i]) - 1;
            } else if (args[i].equals("-s")) {
                printNthSample = 0;
                times = 1;
            } else {
                pid = args[i];
            }
        }

        // check that we have PID
        if (ok && pid == null && inputFile == null) {
            pid = Processes.findProcess("java");
            if (pid != null) {
                System.out.println("Found java process with pid " + pid);
            } else {
                System.err.println("Could not automatically detect the java process. Specify PID of the monitored JVM.");
                ok = false;
            }
        }

        if (!ok) {
            printUsage();
            System.exit(1);
        }

        ThreadData dump;
        if (inputFile != null) {
            // Deserialize samples from file
            dump = ThreadData.readFrom(inputFile);

        } else {
           // Collect samples
           dump = new ThreadData();
           dump.collectSamples(pid, times, interval, debug);
        }
        if (min != null) { 
            dump.select(min, max);
        }

        // Calculate
        dump.calc(hideJvmThreads);

        System.out.println("\n");

        if (printNthSample != null) {
            dump.print(printNthSample, System.out);
            System.exit(0);
        }

        if (outputFile != null) {
            // Serialize samples into a file
            dump.writeInto(outputFile, append);
            
        } else {
            // Print results
            printResults(dump, times, colors, width);

            //JStack.main(args); 
        }
    }

    private static void printResults(ThreadData dump,
             int times, boolean colors, int width) {
        Long time1 = dump.threads.get(0).timestamp;
        Long time2 = dump.threads.get(dump.threads.size() - 1).timestamp;
        if (time1 != null && time2 != null) {
            System.out.println(String.format("Samples: %d (%tc - %tc)",
                  dump.threads.size(),
                  time1, time2));
        }
        System.out.println(String.format("Threads total: %d", dump.ids.size()));
        System.out.println(String.format("Short lived threads: %d",
            dump.countLessThan(times)));
        System.out.println("RUNNABLE threads: " + stat(dump.countStateEqual("RUNNABLE")));
        System.out.println("BLOCKED threads:  " + stat(dump.countStateEqual("BLOCKED")));
        System.out.println("I/O threads:      " + stat(dump.countStateEqual("IO")));

        ThreadTable table = new ThreadTable(dump);

        System.out.println("");
        System.out.println("");
        System.out.println(table.printLegend(colors));

        System.out.println("All threads:"); 
        Ascii.line(61);
        table.plot(width, false, colors);

        System.out.println("Grouped by names:"); 
        Ascii.line(61);
        table.plot(width, true, colors);
    }

    public static String stat(List<Long> values) {
        long min = values.get(0);
        long max = values.get(0);
        
        long sum = 0L;
        for (Long v: values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        Collections.sort(values);
        long mean = values.get(values.size() / 2);
        
        return String.format("min=%d, max=%d, average=%.2f, mean=%d",
                  min, max, 1.0 * sum / values.size(), mean);
    }

    public static String stat(List<Double> values, String prec) {
        double min = values.get(0);
        double max = values.get(0);
        
        double sum = 0.0;
        for (double v: values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        Collections.sort(values);
        double mean = values.get(values.size() / 2);
        
        return String.format("min="+prec+", max="+prec+", average="+prec+", mean="+prec,
                  min, max, 1.0 * sum / values.size(), mean);
    }


}
