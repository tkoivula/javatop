package org.koivula.javatop;

import java.util.concurrent.*;
import java.io.*;

public class Sample {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);

        File file = new File("test.dump");

        int num = 0;
        while (true) {
            Thread.sleep(300);

            // Generate some CPU in current thread
            long a = 12L;
	    for (int i=0; i<1000; ++i) {
                for (int j=0; j<10000; ++j) {
                    a = a * (i+2) / (j+1);
                }
            }

            final Object mutex = new Object();

            // test with a pool
            for (int i=0; i<5; ++i) {
                if  (i % 3 != 0) {
                    pool.submit(new TestJob(mutex));
                } else {
                    pool.submit(new IoJob(file));
                }
            }

            // test with short lived thread
            new Thread(new TestJob(mutex), "mythreads-" + (++num)).start();
            if (num == 1024) num = 0;

            //System.out.print(".");
            if (a == 0) continue;
        }
    }

    public static class TestJob implements Runnable {

        private Object mutex;

        public TestJob(Object mutex) {
            this.mutex = mutex;
        }

        @Override
        public void run() {
            long a = 12L;
            for (int i=0; i<1000; ++i) { 
                synchronized (mutex) {
	            for (int j=0; j<100; ++j) {
		        a = a * (i+2) / (j+1);
	            }
                }
	        for (int j=0; j<10000; ++j) {
	            a = a * (i+2) / (j+1);
	        }
            }
        }
    }

    public static class IoJob implements Runnable {

        private File file;

        public IoJob(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            for (int i=0; i<10; ++i) { 
                try {
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader, 100000);
                    String line = null;
                    while((line = bufferedReader.readLine()) != null) {
                       // nop
                    }   
                    bufferedReader.close();         
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

} 
