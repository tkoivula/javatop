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

public class ThreadTable {

    protected ThreadData data;

    public ThreadTable(ThreadData data) {
        this.data = data;
    }

    public void plot(int w, boolean groupBy, boolean colors) {
        if (w > data.threads.size()) w = data.threads.size();
        double dx = 1.0 * w / data.threads.size();   

        ArrayList<String> names;
        if (groupBy) {
            names = new ArrayList<String>(groupByName(data.names));
        } else {
            names = new ArrayList<String>(data.names);
        }
        HashMap<Integer,Integer> sizes = new HashMap<>();
        Ascii out = new Ascii(w, names, colors);

        out.addColumn("CPU", "%.1f", "0.0");
        out.addColumn("BLO", "%.1f", "0.0");
        out.addColumn("I/O", "%.1f", "0.0");
        out.addColumn("#", "%d", "");

        for (Map.Entry<String,Double[]> e: data.cpus.entrySet()) {
            int y = names.indexOf(e.getKey());
            if (y < 0) {
                for (y = 0; y < names.size(); ++y) {
                   if (e.getKey().startsWith(names.get(y))) break;
                }
            }
            Integer size = sizes.get(y);
            sizes.put(y, size == null ? 1 : size + 1);

            if (y >= names.size()) continue; // filtered away
            Double cpu = (Double) out.getColumnValue(y, "CPU");
            if (cpu == null) cpu = 0.0;
            Double blo = (Double) out.getColumnValue(y, "BLO");
            if (blo == null) blo = 0.0;
            Double io = (Double) out.getColumnValue(y, "I/O");
            if (io == null) io = 0.0;

            out.setColumnValue(y, "CPU", cpu + e.getValue()[0]);
            out.setColumnValue(y, "BLO", blo + e.getValue()[1]);
            out.setColumnValue(y, "I/O", io + e.getValue()[2]);
        }
  
        if (groupBy) {
            // fill in sizes
            for (Map.Entry<Integer,Integer> e: sizes.entrySet()) {
                out.setColumnValue(e.getKey(), "#", e.getValue());
            }
        }

        // fill values:
        int x = 0;
        for (JtdaCore m: data.threads) {
            for (JtdaCore.StackTrace s: m.traces) {
                int y = names.indexOf(s.name);
                if (y < 0) {
                    for (y = 0; y < names.size(); ++y) {
                        if (s.name.startsWith(names.get(y))) break;
                    }
                }
                if (y >= names.size()) continue; // filtered away
                int xx = (int) Math.floor(dx * x);
                if (Thread.State.RUNNABLE.equals(s.state)) {
                    if (ThreadData.isWaitingForIo(s)) {
                        out.add(xx, y, "IO");
                    } else {
                        out.add(xx, y, "RUNNABLE");
                    }
                } 
                else if (Thread.State.WAITING.equals(s.state) ||
                         Thread.State.TIMED_WAITING.equals(s.state)) {
                    out.add(xx, y, "WAITING");
                }
                else if (Thread.State.BLOCKED.equals(s.state)) {
                    out.add(xx, y, "BLOCKED");
                }
            }
            ++x;
        }

        // fill colours:
        x = 0;
        for (JtdaCore m: data.threads) {
            for (JtdaCore.StackTrace s: m.traces) {
                int y = names.indexOf(s.name);
                if (y < 0) {
                    for (y = 0; y < names.size(); ++y) {
                        if (s.name.startsWith(names.get(y))) break;
                    }
                }
                if (y >= names.size()) continue; // filtered away
                int xx = (int) Math.floor(dx * x);
                Set<String> value = out.get(xx, y);
                if (value == null || value.isEmpty()) {
                    out.setColor(xx, y, ' ', Ascii.ANSI_RESET); 
                }
                else if (containsOnly(value, "WAITING")) {
                    out.setColor(xx, y, '.', Ascii.ANSI_BLACK, Ascii.ANSI_BG_WHITE);
                } 
                else if (containsOnly(value, "WAITING", "IO")) {
                    out.setColor(xx, y, 'I', Ascii.ANSI_BLACK, Ascii.ANSI_BG_BLUE);
                } 
                else if (containsOnly(value, "WAITING", "RUNNABLE")) {
                    out.setColor(xx, y, '=', Ascii.ANSI_WHITE, Ascii.ANSI_BG_GREEN);
                } 
                else if (containsOnly(value, "BLOCKED")) {
                    out.setColor(xx, y, 'B', Ascii.ANSI_WHITE, Ascii.ANSI_BG_RED);
                } else {
                    out.setColor(xx, y, '≃', Ascii.ANSI_WHITE, Ascii.ANSI_BG_YELLOW);
                }
            }
            ++x;
        }

        // Sort by CPU
        out.sort("CPU", false);

        System.out.println(out);
    }

    public String printLegend(boolean colors) {
        StringBuffer buf = new StringBuffer();
        buf.append("    ");
        printExp(buf, Ascii.ANSI_BLACK, Ascii.ANSI_BG_WHITE, ".", "Waiting", colors);
        buf.append("  ");
        printExp(buf, Ascii.ANSI_BLACK, Ascii.ANSI_BG_BLUE, "I", "IO", colors);
        buf.append("  ");
        printExp(buf, Ascii.ANSI_WHITE, Ascii.ANSI_BG_GREEN, "=", "Runnable", colors);
        buf.append("\n    ");
        printExp(buf, Ascii.ANSI_WHITE, Ascii.ANSI_BG_RED, "B", "Blocked", colors);
        buf.append("  ");
        printExp(buf, Ascii.ANSI_WHITE, Ascii.ANSI_BG_YELLOW, "≃", "Mixed", colors);
        buf.append("\n");
        return buf.toString();
    }

    private static void printExp(StringBuffer buf, String col1, String col2, String chr,
            String legend, boolean color) {
        if (color) buf.append(col1);
        if (color) buf.append(col2);
        buf.append(chr);
        if (color) buf.append(Ascii.ANSI_RESET);
        buf.append(" = ").append(legend);
    }
     
    private static boolean containsOnly(Set<String> set, String... values) {
        HashSet<String> vSet = new HashSet<String>(Arrays.asList(values));
        for (String s: set) {
            if (!vSet.contains(s)) return false;
        }
        return true;
    }

    private static Set<String> groupByName(Collection<String> names) {
        Set<String> arr = new HashSet<String>();
        Pattern p = Pattern.compile("^(.*\\D)\\d+$");
        for (String n: names) {
            Matcher m = p.matcher(n);
            if (m.matches()) {
                arr.add(m.group(1)); 
            } else {
                arr.add(n);
            }
        }
        return arr;
    }


}
