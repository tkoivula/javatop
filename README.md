# javatop
Javatop command line Linux utility to collect and show JVM thread information of a Java application.

## Installation

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

Get an overview of the only java application directly on server (no need to specify PID is there is only one java process):

    $ javatop

Get an overview of a java application running as service (user tomcat, process ID 7836) directly on server:

    $ sudo -u tomcat javatop 7836
  
Take samples from a longer period of time and save them into a file:

    $ javatop -t 200 -c 100 -o sampledata.dump
  
Take one sample and append it to an existing file (to be used for example peridiocally from a cron script):

    $ javatop -a logs/threads-$(date +%F).dump

Get an overview of the application from a dump file:

    $ javatop -i sampledata.dump
  
Show wider graph from a larger number of samples:

    $ javatop -i threads-2015-30-12.dump -w 100
  
Show a thread dump of a specific sample:

    $ javatop -i threads-2015-30-12.dump -n 15
  
Show a graph from a range of samples from larger dump file:

    $ javatop -i threads-2015-30-12.dump -r 3601-3660 -w 60
  
  

  
  
