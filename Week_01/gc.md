要求：使用jmap，jstat，jstack，以及可视化工具，查看jvm情况。 mac上可以用wrk，windows上可以按照superbenchmark压测 http://localhost:8088/api/hello 查看jvm



-XX:+UseAdaptiveSizePolicy： Automatically sizes the young generation and chooses an optimum survivor ratio to maximize performance.

https://www.oracle.com/technical-resources/articles/javame/garbagecollection2.html   A5.4



脱离场景谈性能都是耍流氓:

目前绝大部分 Java 应用系统，堆内存并不大比如 2G-4G 以内，而且对 10ms 这种低延迟的 GC 暂停不敏感，也就 是说处理一个业务步骤，大概几百毫秒都是可以接受的，GC 暂停 100ms 还是 10ms 没多大区别。另一方面，系统的 吞吐量反而往往是我们追求的重点，这时候就需要考虑采用并行 GC。

如果堆内存再大一些，可以考虑 G1 GC。如果内存非常大(比如超过 16G，甚至是 64G、128G)，或者是对延迟非 常敏感(比如高频量化交易系统)，就需要考虑使用本节提到的新 GC(ZGC/Shenandoah)。



| **Young**             | **Tenured**      | **JVM options**                                              |
| --------------------- | ---------------- | ------------------------------------------------------------ |
| **Serial**            | **Serial**       | **-XX:+UseSerialGC**                                         |
| **Parallel Scavenge** | **Parallel Old** | **-XX:+UseParallelGC -XX:+UseParallelOldGC**                 |
| **Parallel New**      | **CMS**          | **-XX:+UseParNewGC -XX:+UseConcMarkSweepGC **(or just -XX:+UseConcMarkSweepGC) |
| **G1**                | **-XX:+UseG1GC** |                                                              |



## SerialGC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseSerialGC -jar target/gateway-server- 0.0.1-SNAPSHOT.jar

**-XX:+UseSerialGC下，无论是young generation还是old generation都是用的serial**

串行GC：单线程，不能跟应用线程并行处理，会触发全线暂停

wrk -t6 -d300s http://localhost:8088/api/hello

<pre>
Running 5m test @ http://localhost:8088/api/hello
  6 threads and 10 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    31.11ms  122.92ms   2.00s    92.62%
    Req/Sec     6.14k     1.95k    8.67k    86.30%
  9151429 requests in 5.00m, 1.07GB read
  Socket errors: connect 0, read 0, write 0, timeout 12
Requests/sec:  30495.42
Transfer/sec:      3.64MB
</pre>



jconsole:

![image-20210114171944551](/Users/frank/Library/Application Support/typora-user-images/image-20210114171944551.png)

![image-20210114171959275](/Users/frank/Library/Application Support/typora-user-images/image-20210114171959275.png)

以下可以看到上面显示的堆内存使用波动大都发生在eden space，因为绝大部分的压测连接都是短时间的

![image-20210114172036715](/Users/frank/Library/Application Support/typora-user-images/image-20210114172036715.png)

![image-20210114172115030](/Users/frank/Library/Application Support/typora-user-images/image-20210114172115030.png)

老年区也有在稳步上升

![image-20210114172207324](/Users/frank/Library/Application Support/typora-user-images/image-20210114172207324.png)

jmap:

<pre>
Heap Usage:
New Generation (Eden + 1 Survivor Space):
   capacity = 322109440 (307.1875MB)
   used     = 28681936 (27.353225708007812MB)
   free     = 293427504 (279.8342742919922MB)
   8.904407148079857% used
Eden Space:
   capacity = 286326784 (273.0625MB)
   used     = 28482816 (27.163330078125MB)
   free     = 257843968 (245.899169921875MB)
   9.947660362783246% used
From Space:
   capacity = 35782656 (34.125MB)
   used     = 199120 (0.1898956298828125MB)
   free     = 35583536 (33.93510437011719MB)
   0.5564707102793041% used
To Space:
   capacity = 35782656 (34.125MB)
   used     = 0 (0.0MB)
   free     = 35782656 (34.125MB)
   0.0% used
tenured generation:
   capacity = 715849728 (682.6875MB)
   used     = 41253984 (39.342864990234375MB)
   free     = 674595744 (643.3446350097656MB)
   5.762939117859105% used  
</pre>

可以看到除了new generation 跟 eden space，其他几乎无变化


<pre>
Heap Usage:
New Generation (Eden + 1 Survivor Space):
   capacity = 322109440 (307.1875MB)
   used     = 24808136 (23.65888214111328MB)
   free     = 297301304 (283.5286178588867MB)
   7.701772416232197% used
Eden Space:
   capacity = 286326784 (273.0625MB)
   used     = 24614096 (23.473831176757812MB)
   free     = 261712688 (249.5886688232422MB)
   8.596504894212062% used
From Space:
   capacity = 35782656 (34.125MB)
   used     = 194040 (0.18505096435546875MB)
   free     = 35588616 (33.93994903564453MB)
   0.5422738882211539% used
To Space:
   capacity = 35782656 (34.125MB)
   used     = 0 (0.0MB)
   free     = 35782656 (34.125MB)
   0.0% used
tenured generation:
   capacity = 715849728 (682.6875MB)
   used     = 41458072 (39.537498474121094MB)
   free     = 674391656 (643.1500015258789MB)
   5.7914490120474% used
</pre>
jstat:

<pre>
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.05   0.00   0.00   4.77  94.06  90.11     48    0.259     2    0.108    0.367
  0.13   0.00  10.50   4.77  94.06  90.11     52    0.265     2    0.108    0.373
  0.12   0.00   5.75   4.77  94.06  90.11     56    0.271     2    0.108    0.379
  0.00   0.09  70.69   4.77  94.06  90.11     59    0.276     2    0.108    0.384
  0.00   0.08  26.64   4.77  94.06  90.11     63    0.282     2    0.108    0.390
  0.08   0.00  65.75   4.77  94.06  90.11     66    0.287     2    0.108    0.395
  0.08   0.00   3.96   4.77  94.06  90.11     70    0.294     2    0.108    0.402
  0.00   0.08  20.06   4.77  94.06  90.11     73    0.299     2    0.108    0.407
  0.12   0.00  26.65   4.77  94.06  90.11     76    0.305     2    0.108    0.413
  0.00   0.05  59.84   4.77  94.06  90.11     79    0.310     2    0.108    0.418
  0.04   0.00  64.12   4.77  94.06  90.11     82    0.316     2    0.108    0.423
  0.00   0.04  49.03   4.77  94.06  90.11     85    0.321     2    0.108    0.429
  0.13   0.00  28.43   4.77  94.06  90.11     88    0.327     2    0.108    0.435
  ......
  ......
  0.11   0.00  58.60   4.77  94.29  90.14    626    1.396     2    0.108    1.504
  0.05   0.00  38.00   4.77  94.29  90.14    628    1.401     2    0.108    1.509
  0.06   0.00  57.58   4.77  94.29  90.14    630    1.405     2    0.108    1.513
  0.07   0.00   2.76   4.77  94.29  90.14    632    1.409     2    0.108    1.517
  0.03   0.00   0.00   4.77  94.29  90.14    634    1.413     2    0.108    1.521
  0.09   0.00   8.73   4.77  94.29  90.14    636    1.418     2    0.108    1.526
  0.09   0.00  20.69   4.77  94.29  90.14    638    1.422     2    0.108    1.530
  0.07   0.00  38.32   4.77  94.29  90.14    640    1.427     2    0.108    1.534
  0.04   0.00  56.93   4.77  94.29  90.14    642    1.431     2    0.108    1.539
  0.04   0.00  89.13   4.77  94.29  90.14    644    1.435     2    0.108    1.543
  0.00   0.09   4.17   4.77  94.29  90.14    647    1.442     2    0.108    1.550
  0.00   0.09  23.30   4.77  94.29  90.14    647    1.442     2    0.108    1.550
  0.00   0.09  23.30   4.77  94.29  90.14    647    1.442     2    0.108    1.550
</pre>

计算可得，1.442/647=0.00222874806 约等于2ms



## ParallelGC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseParallelGC -jar gateway-server-0.0.1-SNAPSHOT.jar

**只指定-XX:+UseParallelGC 跟同时指定-XX:+UseParallelOldGC是相同的**

```
java -XX:+UseParallelGC com.mypackages.MyExecutableClass
java -XX:+UseParallelOldGC com.mypackages.MyExecutableClass
java -XX:+UseParallelGC -XX:+UseParallelOldGC com.mypackages.MyExecutableClass
```



并行GC：通过并行执行，使得GC时间大幅减少。

wrk -t6 -d300s http://localhost:8088/api/hello

Running 5m test @ http://localhost:8088/api/hello
6 threads and 10 connections
 Thread Stats  Avg   Stdev   Max  +/- Stdev
  Latency  30.29ms 110.50ms  1.72s  92.16%
  Req/Sec   7.04k   2.07k  9.34k  87.96%
 10211577 requests in 5.00m, 1.19GB read
 Socket errors: connect 0, read 0, write 0, timeout 23
Requests/sec: 34026.45
Transfer/sec:   4.06MB

jconsole:

![image-20210114173922278](/Users/frank/Library/Application Support/typora-user-images/image-20210114173922278.png)

![image-20210114173928695](/Users/frank/Library/Application Support/typora-user-images/image-20210114173928695.png)

![image-20210114173951847](/Users/frank/Library/Application Support/typora-user-images/image-20210114173951847.png)![image-20210114174004929](/Users/frank/Library/Application Support/typora-user-images/image-20210114174004929.png)

![image-20210114174017761](/Users/frank/Library/Application Support/typora-user-images/image-20210114174017761.png)

![image-20210114174206761](/Users/frank/Library/Application Support/typora-user-images/image-20210114174206761.png)

jstat：

<pre>
jstat -gcutil 6542 1000 1000     
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.96   0.00  55.22   4.08  94.15  89.67     46    0.122     2    0.046    0.168
  0.96   0.00  82.77   4.08  94.15  89.67     50    0.128     2    0.046    0.174
  0.00   0.96  31.16   4.09  94.15  89.67     55    0.135     2    0.046    0.181
  0.00   0.96  90.03   4.10  94.15  89.67     59    0.140     2    0.046    0.186
  0.88   0.00  27.27   4.11  94.15  89.67     64    0.147     2    0.046    0.193
  0.00   0.88   1.98   4.11  94.15  89.67     69    0.154     2    0.046    0.200
  0.00   0.88  71.28   4.12  94.15  89.67     73    0.159     2    0.046    0.205
  0.96   0.00  52.46   4.12  94.15  89.67     78    0.166     2    0.046    0.212
  0.00   0.81  21.69   4.12  94.15  89.67     83    0.173     2    0.046    0.218
  0.88   0.00   0.00   4.13  94.15  89.67     88    0.179     2    0.046    0.225
  0.88   0.00  64.66   4.13  94.16  89.67     92    0.185     2    0.046    0.231
  0.00   0.88  20.24   4.13  94.16  89.67     97    0.192     2    0.046    0.238
  ......
  ......
  0.88   0.00  44.12   4.24  93.83  89.70    846    1.369     2    0.046    1.415
  0.00   0.96  31.69   4.24  93.83  89.70    849    1.374     2    0.046    1.420
  0.00   0.88  43.33   4.24  93.83  89.70    851    1.377     2    0.046    1.423
  0.88   0.00  70.65   4.24  93.83  89.70    854    1.382     2    0.046    1.428
  0.59   0.00  18.23   4.25  93.83  89.70    858    1.388     2    0.046    1.434
  0.96   0.00  52.90   4.25  93.83  89.70    860    1.391     2    0.046    1.437
  0.00   0.88  39.78   4.25  93.83  89.70    863    1.396     2    0.046    1.442
  0.96   0.00  38.74   4.25  93.83  89.70    866    1.401     2    0.046    1.447
  0.00   0.96  79.96   4.25  93.83  89.70    869    1.406     2    0.046    1.452
  0.88   0.00  20.22   4.25  93.83  89.70    872    1.411     2    0.046    1.456
  0.00   0.96  44.84   4.25  93.83  89.70    873    1.412     2    0.046    1.458
  0.00   0.96  44.84   4.25  93.83  89.70    873    1.412     2    0.046    1.458
</pre>


计算可得，1.412/873=0.00161741122 于等于1.6ms，比serial gc快

jinfo:

<pre>
VM Flags:
-XX:CICompilerCount=12 -XX:InitialHeapSize=1073741824 -XX:MaxHeapSize=1073741824 -XX:MaxNewSize=357564416 -XX:MinHeapDeltaBytes=524288 -XX:NewSize=357564416 -XX:NonNMethodCodeHeapSize=7591728 -XX:NonProfiledCodeHeapSize=122033256 -XX:OldSize=716177408 -XX:ProfiledCodeHeapSize=122033256 -XX:ReservedCodeCacheSize=251658240 -XX:+SegmentedCodeCache -XX:-UseAOT -XX:-UseAdaptiveSizePolicy -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseFastUnorderedTimeStamps -XX:+UseParallelGC 
VM Arguments:
jvm_args: -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseParallelGC 
java_command: gateway-server-0.0.1-SNAPSHOT.jar
java_class_path (initial): gateway-server-0.0.1-SNAPSHOT.jar
Launcher Type: SUN_STANDARD
</pre>



## CMS

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseConcMarkSweepGC -jar gateway-server-0.0.1-SNAPSHOT.jar

wrk -t6 -d300s http://localhost:8088/api/hello

<pre>
  Running 5m test @ http://localhost:8088/api/hello
  6 threads and 10 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    32.11ms  106.32ms   1.55s    91.08%
    Req/Sec     6.87k     2.22k   21.26k    86.91%
  10047061 requests in 5.00m, 1.17GB read
  Socket errors: connect 0, read 0, write 0, timeout 12
Requests/sec:  33503.85
Transfer/sec:      4.00MB
</pre>



jinfo：用jinfo能看到一些默认的配置，虽然我们只指定了GC算法，但其实背后有很多

<pre>
VM Flags:
-XX:CICompilerCount=12 -XX:+FlightRecorder -XX:InitialHeapSize=1073741824 -XX:MaxHeapSize=1073741824 -XX:MaxNewSize=357892096 -XX:MaxTenuringThreshold=6 -XX:MinHeapDeltaBytes=196608 -XX:NewSize=357892096 -XX:NonNMethodCodeHeapSize=7591728 -XX:NonProfiledCodeHeapSize=122033256 -XX:OldSize=715849728 -XX:ProfiledCodeHeapSize=122033256 -XX:ReservedCodeCacheSize=251658240 -XX:+SegmentedCodeCache -XX:+UnlockCommercialFeatures -XX:-UseAOT -XX:-UseAdaptiveSizePolicy -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseFastUnorderedTimeStamps -XX:+UseParNewGC 
VM Arguments:
jvm_args: -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseConcMarkSweepGC 
java_command: gateway-server-0.0.1-SNAPSHOT.jar
java_class_path (initial): gateway-server-0.0.1-SNAPSHOT.jar
Launcher Type: SUN_STANDARD
</pre>

这里可以看到，只指定-XX:+UseConcMarkSweepGC 跟同时指定-XX:+UseParNewGC效果是一样的，在这种情况下，young generation用的是parallel new，old generation用的是CMS

JMC:

![image-20210114182319750](/Users/frank/Library/Application Support/typora-user-images/image-20210114182319750.png)



JMC flight recorder:

![image-20210114183215916](/Users/frank/Library/Application Support/typora-user-images/image-20210114183215916.png)

![image-20210114183257011](/Users/frank/Library/Application Support/typora-user-images/image-20210114183257011.png)

![image-20210114183317068](/Users/frank/Library/Application Support/typora-user-images/image-20210114183317068.png)

可以看到每次GC的时间跟原因，JMC flight recorder牛逼。可以看到每次pause的时间都在1-2ms之间徘徊

按照这个[stackoverflow](https://stackoverflow.com/questions/28342736/java-gc-allocation-failure)，"Allocation Failure" is a cause of GC cycle to kick in。Allocation Failure" means that no more space left in Eden to allocate object. So, it is normal cause of young GC. 意思是压测太猛了，Eden区分配对象没分配过来，就要gc清点空间。按照这个思路去优化的话，应该是提高eden space的空间，就能减少gc，对应于上节课讲的，就是**调-Xmn 跟 -XX:MaxNewSize到更大**，前提是-Xmx先增大

![image-20210114183405815](/Users/frank/Library/Application Support/typora-user-images/image-20210114183405815.png)

这里可以看到很明显地分出了young garbage collector 跟 old garbage collector，比jconsole更加直白；GC time ratio 是99（By default, the value of **-XX:GCTimeRatio** flag is set to 99 by the JVM, which means that the application will get 99 times more  working time compared to the garbage collection which is a good  trade-off for the server-side applications.），**这个应该可以较少一点**，因为我们的gc次数还是比较多的，我们的程序逻辑只是回个helloworld；New ratio是2，就是说年轻代：老年代=2，在我们这个简单的wrk压测用例下，**这个应该可以调更大**，5/6/7/8应该都可以；

![image-20210114184134313](/Users/frank/Library/Application Support/typora-user-images/image-20210114184134313.png)



jstat:

<pre>
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.09   0.00  10.89   2.74  93.97  90.22     36    0.110     2    0.025    0.135
  0.11   0.00  48.62   2.74  93.97  90.22     40    0.113     2    0.025    0.138
  0.00   0.16  14.52   2.74  93.98  90.24     45    0.117     2    0.025    0.141
  0.00   0.13  51.01   2.74  93.98  90.24     49    0.120     2    0.025    0.145
  0.00   0.13  71.67   2.74  93.98  90.24     53    0.123     2    0.025    0.148
  0.09   0.00   1.97   2.74  93.98  90.24     58    0.128     2    0.025    0.152
  0.13   0.00   0.00   2.74  93.98  90.24     62    0.131     2    0.025    0.155
  0.13   0.00  46.57   2.74  93.98  90.24     66    0.134     2    0.025    0.159
  0.15   0.00  78.09   2.74  93.98  90.24     70    0.137     2    0.025    0.162
  0.00   0.17   5.94   2.74  93.98  90.24     75    0.141     2    0.025    0.166
  ......
  ......
  0.00   0.10  90.74   2.74  94.19  90.27    817    0.808     2    0.025    0.833
  0.00   0.11  52.39   2.74  94.19  90.27    821    0.812     2    0.025    0.836
  0.13   0.00  57.07   2.74  94.19  90.27    824    0.815     2    0.025    0.839
  0.10   0.13 100.00   2.74  94.19  90.27    826    0.816     2    0.025    0.840
  0.00   0.13  59.09   2.74  94.19  90.27    829    0.819     2    0.025    0.844
  0.00   0.14  19.04   2.74  94.19  90.27    833    0.823     2    0.025    0.848
  0.00   0.16  65.20   2.74  94.19  90.27    835    0.825     2    0.025    0.849
  0.13   0.00   2.78   2.74  94.19  90.27    836    0.826     2    0.025    0.850
  0.00   0.10  25.26   2.74  94.19  90.27    839    0.829     2    0.025    0.853
  0.00   0.10  25.26   2.74  94.19  90.27    839    0.829     2    0.025    0.853
  0.00   0.10  25.26   2.74  94.19  90.27    839    0.829     2    0.025    0.853
</pre>


计算可得，0.829/839=0.000988081 少于1ms，比parallel gc更快，因为我们的wrk用例比较简单，大部分工作发生在eden space，大多是ygc，所以这里说明新生代的parallel new 算法是比parallel scavenge 更快的



## G1

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -jar gateway-server-0.0.1-SNAPSHOT.jar

wrk -t6 -d300s http://localhost:8088/api/hello

<pre>
Running 5m test @ http://localhost:8088/api/hello
  6 threads and 10 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    30.37ms  119.84ms   1.99s    92.69%
    Req/Sec     6.18k     1.89k    8.90k    80.65%
  9161990 requests in 5.00m, 1.07GB read
  Socket errors: connect 0, read 0, write 0, timeout 17
Requests/sec:  30532.04
Transfer/sec:      3.65MB
</pre>

jinfo:

<pre>
VM Flags:
-XX:CICompilerCount=12 -XX:ConcGCThreads=3 -XX:G1ConcRefinementThreads=13 -XX:G1HeapRegionSize=1048576 -XX:InitialHeapSize=1073741824 -XX:MarkStackSize=4194304 -XX:MaxGCPauseMillis=50 -XX:MaxHeapSize=1073741824 -XX:MaxNewSize=643825664 -XX:MinHeapDeltaBytes=1048576 -XX:NonNMethodCodeHeapSize=7591728 -XX:NonProfiledCodeHeapSize=122033256 -XX:ProfiledCodeHeapSize=122033256 -XX:ReservedCodeCacheSize=251658240 -XX:+SegmentedCodeCache -XX:-UseAOT -XX:-UseAdaptiveSizePolicy -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseFastUnorderedTimeStamps -XX:+UseG1GC 
VM Arguments:
jvm_args: -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseG1GC -XX:MaxGCPauseMillis=50 
java_command: gateway-server-0.0.1-SNAPSHOT.jar
java_class_path (initial): gateway-server-0.0.1-SNAPSHOT.jar
Launcher Type: SUN_STANDARD
</pre>

JMC flight recorder:

![image-20210114185843100](/Users/frank/Library/Application Support/typora-user-images/image-20210114185843100.png)

![image-20210114185921002](/Users/frank/Library/Application Support/typora-user-images/image-20210114185921002.png)

**可以看到虽然G1是软实时的，但总体来说对于每次GC，它的pause要比CMS要更耗时？？**



jstat：

<pre>
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.00 100.00  19.75   4.95  94.46  92.13     22    0.122     0    0.000    0.122
  0.00 100.00  14.13   5.17  94.46  92.13     24    0.124     0    0.000    0.124
  0.00 100.00  11.96   5.17  94.19  89.97     26    0.126     0    0.000    0.126
  0.00 100.00   6.06   5.16  94.19  89.97     28    0.128     0    0.000    0.128
  0.00 100.00  88.98   5.16  94.19  89.97     29    0.129     0    0.000    0.129
  0.00 100.00  78.73   5.17  94.19  89.97     31    0.132     0    0.000    0.132
  0.00 100.00  67.70   5.17  94.19  89.97     33    0.134     0    0.000    0.134
  0.00 100.00  54.19   5.16  94.19  89.97     35    0.136     0    0.000    0.136
  0.00 100.00  36.96   5.17  94.19  89.97     37    0.138     0    0.000    0.138
  0.00 100.00  28.88   5.18  94.19  89.97     39    0.140     0    0.000    0.140
  0.00 100.00  69.57   5.18  94.19  89.97     40    0.142     0    0.000    0.142
  ......
  ......
  0.00 100.00   2.02   5.21  93.87  90.00    337    0.520     0    0.000    0.520
  0.00 100.00  12.42   5.21  93.87  90.00    338    0.522     0    0.000    0.522
  0.00 100.00  18.32   5.21  93.87  90.00    339    0.523     0    0.000    0.523
  0.00 100.00  41.30   5.21  93.87  90.00    340    0.524     0    0.000    0.524
  0.00 100.00  44.88   5.21  93.87  90.00    341    0.526     0    0.000    0.526
  0.00 100.00  50.16   5.21  93.87  90.00    342    0.527     0    0.000    0.527
  0.00 100.00  38.35   5.21  93.87  90.00    343    0.528     0    0.000    0.528
  0.00 100.00  47.20   5.21  93.87  90.00    344    0.530     0    0.000    0.530
  0.00 100.00  71.89   5.21  93.87  90.00    345    0.531     0    0.000    0.531
  0.00 100.00  46.12   5.21  93.87  90.00    346    0.532     0    0.000    0.532
  0.00 100.00  46.12   5.21  93.87  90.00    346    0.532     0    0.000    0.532
  0.00 100.00  46.12   5.21  93.87  90.00    346    0.532     0    0.000    0.532
</pre>

计算可得，0.532/346=0.001537572 约等于1.5ms，跟教学资料讲的吻合。可能因为内存小的原因，g1算法在我们1g heap的用例下没有发挥出比CMS更大的威力



## playground: trigger more full gcs

java -Xmx20m -Xms20m -XX:-UseAdaptiveSizePolicy -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -jar gateway-server-0.0.1-SNAPSHOT.jar

wrk -t6 -c2000 -d300s http://localhost:8088/api/hello



<pre>
java -Xmx20m -Xms20m -XX:-UseAdaptiveSizePolicy -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -jar gateway-server-0.0.1-SNAPSHOT.jar
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.4.RELEASE)

WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.springframework.cglib.core.ReflectUtils$1 (jar:file:/Users/frank/Downloads/gateway-server-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-core-5.0.8.RELEASE.jar!/) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of org.springframework.cglib.core.ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-Acceptor-0"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-AsyncTimeout"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-exec-19"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-exec-6"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-ClientPoller-0"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-exec-10"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "http-nio-8088-ClientPoller-1"
......
xception in thread "http-nio-8088-exec-45" Exception in thread "http-nio-8088-exec-26" Exception in thread "http-nio-8088-exec-7" Exception in thread "http-nio-8088-exec-11" Exception in thread "http-nio-8088-exec-15" Exception in thread "http-nio-8088-exec-18" Exception in thread "http-nio-8088-exec-24" Exception in thread "http-nio-8088-exec-34" Exception in thread "http-nio-8088-exec-42" java.lang.OutOfMemoryError: Java heap space
Exception in thread "http-nio-8088-exec-14" java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
Exception in thread "http-nio-8088-exec-40" java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
Exception in thread "http-nio-8088-exec-29" Exception in thread "http-nio-8088-exec-5" Exception in thread "http-nio-8088-exec-12" java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
Exception in thread "http-nio-8088-exec-48" java.lang.OutOfMemoryError: Java heap space
</pre>



<pre>
Running 5m test @ http://localhost:8088/api/hello
  6 threads and 2000 connections
^C  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   223.27ms  223.25ms   1.23s    85.50%
    Req/Sec    45.19     54.68   510.00     83.51%
  1676 requests in 2.11m, 204.59KB read
  Socket errors: connect 0, read 82861, write 0, timeout 0
Requests/sec:     13.22
Transfer/sec:      1.61KB
</pre>

<pre>
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.00 100.00  50.00  81.86  94.07  90.45    225    0.267     6    0.332    0.599
  0.00 100.00   0.00  87.89  94.07  90.45    228    0.270     6    0.332    0.602
  0.00 100.00  50.00  82.08  94.07  90.45    233    0.276     6    0.332    0.608
  0.00   0.00   0.00  95.89  94.10  90.45    258    0.302    17    0.892    1.194
  0.00   0.00   0.00  95.88  94.10  90.45    276    0.317    35    1.873    2.190
  0.00   0.00   0.00  95.87  94.10  90.45    294    0.332    54    2.877    3.210
  0.00   0.00   0.00  95.87  94.10  90.45    312    0.347    72    3.835    4.182
  0.00   0.00   0.00  95.87  94.10  90.45    332    0.363    91    4.840    5.202
  0.00   0.00   0.00  95.87  94.10  90.45    350    0.377   110    5.826    6.203
  0.00   0.00   0.00  95.86  94.10  90.45    369    0.393   128    6.765    7.157
  0.00   0.00   0.00  95.85  94.10  90.45    390    0.409   147    7.761    8.170
  0.00   0.00   0.00  95.84  94.10  90.45    407    0.423   166    8.762    9.185
  0.00   0.00   0.00  95.84  94.10  90.45    423    0.435   184    9.704   10.139
  0.00   0.00   0.00  95.84  94.11  90.45    444    0.452   203   10.687   11.139
  0.00   0.00   0.00  95.83  94.11  90.45    461    0.466   222   11.685   12.150
  0.00   0.00   0.00  95.83  94.11  90.45    482    0.483   241   12.686   13.169
  0.00   0.00   0.00  95.83  94.11  90.45    501    0.498   259   13.661   14.160
  0.00   0.00   0.00  95.83  94.11  90.45    519    0.513   277   14.628   15.140
  0.00   0.00   0.00  95.83  94.11  90.45    536    0.526   296   15.623   16.149
  0.00   0.00   0.00  95.77  94.11  90.45    556    0.542   314   16.572   17.115
  0.00   0.00   0.00  95.74  94.11  90.45    576    0.558   333   17.560   18.117
  0.00   0.00   0.00  95.74  94.11  90.45    593    0.572   352   18.548   19.120
  0.00   0.00   0.00  95.74  94.11  90.45    612    0.587   371   19.546   20.133
  0.00   0.00   0.00  95.71  94.11  90.45    629    0.602   388   20.494   21.096
  0.00   0.00   0.00  95.71  94.11  90.45    649    0.618   407   21.480   22.099
  0.00   0.00   0.00  95.68  94.11  90.45    666    0.632   426   22.477   23.110
  0.00   0.00   0.00  95.60  94.11  90.45    683    0.647   444   23.468   24.115
  0.00   0.00   0.00  95.53  94.11  90.45    701    0.661   462   24.417   25.078
  0.00   0.00   0.00  95.53  94.11  90.45    721    0.677   481   25.408   26.086
  0.00   0.00   0.00  95.47  94.11  90.45    742    0.693   500   26.403   27.097
  0.00   0.00   0.00  95.39  94.11  90.45    762    0.708   519   27.388   28.097
  0.00   0.00   0.00  95.34  94.11  90.45    781    0.724   537   28.333   29.056
  0.00   0.00   0.00  95.33  94.11  90.45    800    0.738   556   29.363   30.101
  0.00   0.00   0.00  95.31  94.11  90.45    819    0.754   576   30.337   31.091
  0.00   0.00   0.00  95.30  94.11  90.45    837    0.769   594   31.277   32.047
</pre>



References：

1. [plumbr gc book](https://plumbr.io/handbook/garbage-collection-algorithms-implementations)



