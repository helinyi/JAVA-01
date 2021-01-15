要求：使用jmap，jstat，jstack，以及可视化工具，查看jvm情况。 mac上可以用wrk，windows上可以按照superbenchmark压测 http://localhost:8088/api/hello 查看jvm



脱离场景谈性能都是耍流氓:

目前绝大部分 Java 应用系统，堆内存并不大比如 2G-4G 以内，而且对 10ms 这种低延迟的 GC 暂停不敏感，也就 是说处理一个业务步骤，大概几百毫秒都是可以接受的，GC 暂停 100ms 还是 10ms 没多大区别。另一方面，系统的 吞吐量反而往往是我们追求的重点，这时候就需要考虑采用并行 GC。

如果堆内存再大一些，可以考虑 G1 GC。如果内存非常大(比如超过 16G，甚至是 64G、128G)，或者是对延迟非 常敏感(比如高频量化交易系统)，就需要考虑使用本节提到的新 GC(ZGC/Shenandoah)。

## SerialGC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseSerialGC -jar target/gateway-server- 0.0.1-SNAPSHOT.jar

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
  0.00   0.96  45.03   4.12  94.21  91.32     95    0.209     2    0.045    0.254
  0.96   0.00  32.57   4.12  94.21  91.32     98    0.215     2    0.045    0.260
  0.00   0.96  40.89   4.12  94.21  91.32    101    0.221     2    0.045    0.265
  0.88   0.00  67.77   4.12  94.21  91.32    104    0.226     2    0.045    0.271
  0.00   0.88  93.83   4.12  94.21  91.32    107    0.232     2    0.045    0.277
  0.00   0.96   4.54   4.12  94.25  91.34    111    0.240     2    0.045    0.285
  0.88   0.00   1.98   4.12  94.25  91.34    114    0.246     2    0.045    0.291
  0.00   0.96   1.98   4.12  94.25  91.34    117    0.252     2    0.045    0.296
  0.88   0.00  58.89   4.12  94.25  91.34    118    0.254     2    0.045    0.298
  0.96   0.00  29.39   4.12  94.25  91.34    120    0.258     2    0.045    0.302
  0.96   0.00  83.41   4.12  94.25  91.34    122    0.261     2    0.045    0.306
  0.00   0.96  59.96   4.12  94.25  91.34    123    0.263     2    0.045    0.308
  ......
  ......
  0.00   0.88  65.64   4.23  94.41  91.35    727    1.394     2    0.045    1.439
  0.88   0.00  18.66   4.23  94.41  91.35    730    1.400     2    0.045    1.444
  0.96   0.00  60.23   4.23  94.41  91.35    732    1.404     2    0.045    1.448
  0.81   0.00  78.26   4.23  94.41  91.35    734    1.408     2    0.045    1.452
  0.96   0.00  80.56   4.23  94.41  91.35    736    1.412     2    0.045    1.456
  0.00   0.96   9.53   4.23  94.41  91.35    739    1.417     2    0.045    1.462
  0.00   0.88  85.89   4.24  94.41  91.35    741    1.421     2    0.045    1.466
  0.96   0.00   0.00   4.24  94.41  91.35    744    1.427     2    0.045    1.472
  0.96   0.00  16.25   4.24  94.41  91.35    746    1.431     2    0.045    1.475
  0.96   0.00  31.19   4.24  94.41  91.35    748    1.435     2    0.045    1.479
  0.00   0.96   9.52   4.24  94.42  91.35    751    1.441     2    0.045    1.485
  0.96   0.00  75.79   4.24  94.42  91.35    752    1.442     2    0.045    1.487
  0.96   0.00  75.79   4.24  94.42  91.35    752    1.442     2    0.045    1.487
  0.96   0.00  75.79   4.24  94.42  91.35    752    1.442     2    0.045    1.487
</pre>

计算可得，1.442/752=0.00191755319 于等于2ms

**为什么并行GC的次数反而比串行GC多呢？**



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
  0.00   0.16  23.69   2.76  94.04  91.32     57    0.139     2    0.022    0.161
  0.00   0.13  18.03   2.76  94.04  91.32     61    0.142     2    0.022    0.164
  0.00   0.10   7.72   2.76  94.04  91.32     65    0.145     2    0.022    0.168
  0.00   0.09  34.64   2.76  94.04  91.32     69    0.149     2    0.022    0.171
  0.00   0.12  55.42   2.76  94.04  91.32     73    0.152     2    0.022    0.174
  0.00   0.09  68.00   2.76  94.04  91.32     77    0.155     2    0.022    0.177
  0.12   0.00  20.63   2.76  94.05  91.32     82    0.159     2    0.022    0.181
  0.09   0.00   0.00   2.76  94.05  91.32     86    0.162     2    0.022    0.184
  0.00   0.13  70.50   2.76  94.05  91.32     89    0.164     2    0.022    0.187
  0.00   0.13  85.77   2.76  94.05  91.32     93    0.168     2    0.022    0.190
  0.00   0.13  72.26   2.76  94.06  91.34     97    0.171     2    0.022    0.193
  0.00   0.10  28.51   2.76  94.06  91.34    101    0.175     2    0.022    0.197
  0.00   0.14  32.02   2.76  94.06  91.34    105    0.178     2    0.022    0.200
  0.00   0.09  36.89   2.76  94.06  91.34    109    0.181     2    0.022    0.203
  0.00   0.08  67.12   2.76  94.06  91.34    113    0.184     2    0.022    0.206
  ......
  ......
  0.00   0.38  62.15   3.33  93.62  89.15    729    0.792     2    0.022    0.814
  0.37   0.00  10.94   3.34  93.63  89.15    732    0.795     2    0.022    0.817
  0.41   0.00  29.87   3.34  93.64  89.15    734    0.797     2    0.022    0.819
  0.39   0.00  51.19   3.34  93.64  89.15    736    0.799     2    0.022    0.822
  0.00   0.46   6.57   3.35  93.64  89.15    739    0.802     2    0.022    0.825
  0.44   0.00   0.00   3.35  93.64  89.15    742    0.805     2    0.022    0.827
  0.41   0.00   1.99   3.36  93.64  89.15    744    0.807     2    0.022    0.829
  0.36   0.00   0.00   3.36  93.64  89.15    746    0.809     2    0.022    0.832
  0.41   0.00  15.48   3.36  93.64  89.15    748    0.811     2    0.022    0.833
  0.00   0.42  16.72   3.37  93.64  89.15    751    0.814     2    0.022    0.836
  0.00   0.44  48.76   3.37  93.64  89.15    753    0.816     2    0.022    0.838
  0.37   0.00  37.15   3.37  93.64  89.15    754    0.817     2    0.022    0.839
  0.37   0.00  37.23   3.37  93.64  89.15    754    0.817     2    0.022    0.839
  0.37   0.00  37.30   3.37  93.64  89.15    754    0.817     2    0.022    0.839
</pre>

计算可得，0.817/754=0.00108355437 约等于1ms，比serialgc跟parallelgc都有很大的提升！



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
  0.00 100.00   9.78   5.18  94.09  90.12     36    0.141     0    0.000    0.141
  0.00 100.00  85.25   5.18  94.09  90.12     37    0.142     0    0.000    0.142
  0.00 100.00  68.32   5.18  94.09  90.12     39    0.144     0    0.000    0.144
  0.00 100.00  45.65   5.18  94.09  90.12     41    0.146     0    0.000    0.146
  0.00 100.00  18.32   5.18  94.09  90.12     43    0.148     0    0.000    0.148
  0.00 100.00  87.73   5.18  94.09  90.12     44    0.150     0    0.000    0.150
  0.00 100.00  74.53   5.18  94.09  90.12     46    0.152     0    0.000    0.152
  0.00 100.00  48.91   5.18  94.09  90.12     48    0.154     0    0.000    0.154
  0.00 100.00  33.85   5.19  94.09  90.12     50    0.157     0    0.000    0.157
  0.00 100.00  13.04   5.19  94.09  90.12     52    0.159     0    0.000    0.159
  0.00 100.00  87.58   5.19  94.10  90.14     53    0.160     0    0.000    0.160
  0.00 100.00  66.93   5.19  94.10  90.14     55    0.163     0    0.000    0.163
  0.00 100.00  47.98   5.19  94.14  90.14     57    0.165     0    0.000    0.165
  0.00 100.00  31.52   5.19  94.14  90.14     59    0.167     0    0.000    0.167
  ......
  ......
    0.00 100.00  72.20   6.57  92.98  89.67    328    0.550     0    0.000    0.550
  0.00 100.00  84.78   6.57  92.98  89.67    329    0.551     0    0.000    0.551
  0.00 100.00  57.14   6.57  92.98  89.67    330    0.553     0    0.000    0.553
  0.00 100.00  55.43   6.59  92.98  89.67    331    0.554     0    0.000    0.554
  0.00 100.00  71.27   6.59  92.98  89.67    332    0.556     0    0.000    0.556
  0.00 100.00  62.42   6.60  92.98  89.67    333    0.557     0    0.000    0.557
  0.00 100.00  40.68   6.60  92.98  89.67    334    0.558     0    0.000    0.558
  0.00 100.00  76.71   6.61  92.99  89.67    335    0.560     0    0.000    0.560
  0.00 100.00  91.61   6.62  92.99  89.67    336    0.561     0    0.000    0.561
  0.00 100.00  90.22   6.62  92.99  89.67    337    0.563     0    0.000    0.563
  0.00 100.00  69.10   6.63  92.99  89.67    338    0.565     0    0.000    0.565
  0.00 100.00  69.10   6.63  92.99  89.67    338    0.565     0    0.000    0.565
  0.00 100.00  69.10   6.63  92.99  89.67    338    0.565     0    0.000    0.565
  0.00 100.00  69.10   6.63  92.99  89.67    338    0.565     0    0.000    0.565
</pre>

计算可得，0.565/338=0.00167159763 约等于1ms。可以看见gc的次数更少

