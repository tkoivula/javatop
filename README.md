# javatop
Javatop is a command line Linux utility to collect and show JVM thread information of a Java application. Javatop is like [jstack](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jstack.html) command with steroids: javatop takes several thread samples and visualises waiting, blocked, running and I/O threads directly on command line. It can also be used to record thread information into a file to be later examined. 

It can be used directly on server to get instant view of threads. Instead of taking a single thread dump javatop can be used to take several samples and save them into a file to be later examined either remotely or in-place on server.

In short, javatop is:
  * tool to visualize what Java app is doing,
  * simple command that runs directly on server,
  * requires no changes or Java agent libraries in the server JVM,
  * requires no debug ports as it connects just like jstack command and
  * has appendable file format to log thread information.

Please note that javatop is not complete Java profiler. There are better desktop Java profiler software available. Javatop is more of a replacement of the jstack command to examine a Java application's concurrency and performance fast, in real-time. 



## Installation

Javatop requires JDK. If your server just has JRE installed, you can obtain just the tools.jar from the JDk and copy it to the server.

  1. Download [javatop](https://github.com/tkoivula/javatop/releases) binary.
  2. Try running it:
    ```$ ./javatop -h```
  4. If it doesn't find your Java JDK set JAVA_HOME environment variable to the correct JDK directory:
    ```$ export JAVA_HOME="/my/path/to/oracle1.8.0/"```

## Usage
```
Usage: javatop [options..] [pid]
       javatop -i [file] [options..]

Options:
    -t N     Take samples in N msec intervals (default: 100)
    -c N     Take total of N samples (default: 20)
    -w M     Print results using width N (default: 40)
    -j       Do not hide built-in JVM threads
    -b       Print black and white instead of ANSI colors
    -o FILE  Write samples into a file instead of printing results
    -a FILE  Take one sample and append it into a file. This option
             can be used from scripts to collect thread information
             of longer periods to be analyzed later
    -i FILE  Read samples from given file instead of connecting to a
             JVM instance and show results
    -r N-M   Print results from samples N-M (useful with -i option)
    -n N     Print a thread dump of sample N (useful with -i option)
    -s       Take pme sample and print thread dump (jstack)
    -d       Print debug information
```

## Examples

Get an overview of a java application running as service (user tomcat, process ID 7836) directly on server:

    $ sudo -u tomcat javatop 7836
  
Take samples from a longer period of time and save them into a file:

    $ javatop -t 200 -c 100 -o sampledata.dump
  
Take one sample and append it to an existing file (to be used for example peridiocally from a cron script):

    $ javatop -a logs/threads-$(date +%F).dump

Get an overview of the application from a dump file:

    $ javatop -i sampledata.dump
  
Show a thread dump of a specific sample (like jstack but from the past):

    $ javatop -i threads-2015-30-12.dump -n 15
  
Show a graph from a range of samples from larger dump file:

    $ javatop -i threads-2015-30-12.dump -r 3601-3660 -w 60
  
  

  
  
