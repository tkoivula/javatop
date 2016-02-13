package org.koivula.javatop;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachNotSupportedException;
import sun.tools.attach.HotSpotVirtualMachine;

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
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

public class ThreadData {

    public List<JtdaCore> threads = new ArrayList<>();

    // calculated by calc() method:
    public SortedSet<String> ids;
    public SortedSet<String> names;
    public Map<String,Long> ages;
    public Map<String,Double[]> cpus;
 
    // Reserved thread names
    private static final Set<String> jvmThreadNames = new HashSet<>(Arrays.asList(
        "Finalizer", "Reference Handler", "Signal Dispatcher",
        "Attach Listener", "C1 CompilerThread", "C2 CompilerThread",
        "GC task thread", "Service Thread", "VM Periodic Task Thread",
        "VM Thread",
        "Gang worker", "Concurrent Mark-Sweep GC Thread",
        "Surrogate Locker Thread", "GC Daemon",
        "JDWP Event Helper Thread", "JDWP Transport Listener"
        ));

    public void select(int first, int last) {
        threads = threads.subList(first - 1, last);
    }

    public void calc(boolean hideJvmThreads) {
        ids = new TreeSet<String>();
        names = new TreeSet<String>();
        for (JtdaCore m: threads) {
            for (JtdaCore.StackTrace s: m.traces) {
                 ids.add(s.name);
                 names.add(s.name);
            }
        }
 
        if (hideJvmThreads) {
            List<String> toHide = new ArrayList<>();
            for (String name: names) {
                for (String n: jvmThreadNames) {
                    if (name.toLowerCase().startsWith(n.toLowerCase())) toHide.add(name);
                }
            }
            ids.removeAll(toHide);
            names.removeAll(toHide);
        }

        ages = new HashMap<String, Long>();
	for (String l: ids) {
           long c = 0L;
	   for (JtdaCore m: threads) {
                JtdaCore.StackTrace ti = findThread(m, l);
                if (ti != null) {
                    //names.add(ti.getThreadName());
                    ++c;
                }
           }
           ages.put(l, c);
        }

        cpus = new HashMap<String, Double[]>();
	for (String l: ids) {
           long c = 0L;
           long b = 0L;
           long io = 0L;
	   for (JtdaCore m: threads) {
                JtdaCore.StackTrace ti = findThread(m, l);
                if (ti != null && Thread.State.RUNNABLE.equals(ti.state)) {
                    if (isWaitingForIo(ti)) {
                        ++io;
                    } else {
                        ++c;
                    }
                }
                if (ti != null && Thread.State.BLOCKED.equals(ti.state)) {
                    ++b;
                }
           }
           cpus.put(l, new Double[] {
                 100.0 * c / threads.size(),
                 100.0 * b / threads.size(),
                 100.0 * io / threads.size()
                 });
        }
    }

    public static boolean isWaitingForIo(JtdaCore.StackTrace ti) {
        if (ti.stack.isEmpty()) return false;

        String method = ti.stack.get(0).toLowerCase().trim();
        if (method.startsWith("java.io") &&
            (method.contains("read") || method.contains("write")) &&
            method.contains("(native method)")) return true;

        if (method.startsWith("java.net") &&
            (method.contains("socket")) &&
            method.contains("(native method)")) return true;

        if (method.startsWith("sun.nio") &&
            (method.contains("socket")) &&
            method.contains("(native method)")) return true;

        return false; // not IO
    }

    public static JtdaCore.StackTrace findThread(JtdaCore m, String name) {
        for (JtdaCore.StackTrace s: m.traces) {
            if (s.name.equals(name)) return s;
        }
        return null;
    }

    public long countLessThan(long limit) {
        long c = 0L;
        for (Long age: ages.values()) {
            if (age < limit) ++c;
        }
        return c;
    }

    public List<Long> countStateEqual(String state) {
        List<Long> list = new ArrayList<>(threads.size());
        for (JtdaCore m: threads) { 
            long c = 0L;
            for (JtdaCore.StackTrace ti: m.traces) {
                if (!names.contains(ti.name)) continue; // hidden thread
                if (ti.state.equals(Thread.State.RUNNABLE) && isWaitingForIo(ti)) {
                    if (state.equals("IO")) c++;
                }
                else if (state.equals("WAITING")) {
                    if (ti.state.equals(Thread.State.WAITING) ||
                        ti.state.equals(Thread.State.TIMED_WAITING)) c++;
                }
                else if (state.equals(ti.state.toString())) c++;
            }
            list.add(c);
        }
        return list;
    }

    public static JtdaCore parseThreadData(String input, boolean ignoreLocks) throws Exception {
        if (input.contains("No such process")) {
            throw new Exception(input);
        }
        JtdaCore data = new JtdaCore();
        data.analyseSameThread(input, ignoreLocks);
        if (data.traces.isEmpty()) {
            throw new Exception("Could not read thread dump.");
        }
        return data;
    }

    // Attach to pid and perform a thread dump
    private static String runThreadDump(String pid) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(pid);

        // Cast to HotSpotVirtualMachine as this is implementation specific
        // method.
        InputStream in = ((HotSpotVirtualMachine)vm).remoteDataDump(new Object[0]);

        // read to EOF and just print output
        byte b[] = new byte[256];
        int n;
        StringBuffer text = new StringBuffer();
        do {
            n = in.read(b);
            if (n > 0) {
                text.append(new String(b, 0, n, "UTF-8"));
            }
        } while (n > 0);
        in.close();
        vm.detach();
        return text.toString();
    }

    public void collectSamples(String pid, int times, int interval,
            boolean debug) throws Exception {
        long a = System.currentTimeMillis();
        for (int i=0; i<times; ++i) {
            long t0 = System.currentTimeMillis();
            String outs = runThreadDump(pid);
            if (debug) {
                System.out.println(outs);
            }
            threads.add(parseThreadData(outs, true));
            long t1 = System.currentTimeMillis();

            if (i == 0 && times > 1) {
                System.out.print(String.format(
                    "Taking %d samples in %.3f sec intervals.",
                    times,
                    0.001 * interval));
            } else {
                System.out.print(".");
            }

            if (t1 - t0 < interval) {
                Thread.sleep(interval - (t1 - t0));
            }
        }
        long time = System.currentTimeMillis() - a;
    }

    /**
     * Read thread samples from a file. 
     */
    public static ThreadData readFrom(File inputFile) throws IOException {
        try(
            InputStream in = new FileInputStream(inputFile);
            GZIPInputStream gz = new GZIPInputStream(in);
            DataInputStream din = new DataInputStream(gz);
        ){
            //ThreadData data = (ThreadData) input.readObject();
            ThreadData data = new ThreadData();
            try {
              do {
                  // every record has the length of the object stream, and then
                  // the stream as bytes:
                  int len = din.readInt();
                  byte[] bytes = new byte[len];
                  din.readFully(bytes);
                  ObjectInput input = new ObjectInputStream(
                     new ByteArrayInputStream(bytes));
                  data.threads.add((JtdaCore) input.readObject());
                  input.close();
              } while (true);
            }catch (EOFException e) { }
            return data;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write this objects (thread samples) into a file.
     */
    public void writeInto(File outputFile, boolean append) throws IOException {
        try (
            OutputStream out = new FileOutputStream(outputFile, append);
            GZIPOutputStream gz = new GZIPOutputStream(out);
            DataOutputStream dout = new DataOutputStream(gz);
        ){
            //output.writeObject(this);
            for (JtdaCore m: threads) {
                 ByteArrayOutputStream bas = new ByteArrayOutputStream();
                 ObjectOutput output = new ObjectOutputStream(bas);
                 output.writeObject(m);
                 output.close();
                 byte[] bytes = bas.toByteArray();
                 // every record has the length of the object stream, and then
                 // the stream as bytes:
                 dout.writeInt(bytes.length);
                 dout.write(bytes, 0, bytes.length);
            }
        } 
    }

    /**
     * Print thread dump of given sample.
     */
    public void print(int sample, PrintStream out) {
        JtdaCore s = threads.get(sample);
        for (JtdaCore.StackTrace stack : s.traces) {
            out.println(stack);
        }
    }

}
