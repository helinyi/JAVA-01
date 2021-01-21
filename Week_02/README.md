READ THIS!!!!

1. GCLogAnalysis/src/main/java/GCLogAnalysis.java
2. GCLogAnalysis/src/main/java/wrk.md
3. httpclient/src/main/java/httpclient.java

<hr>

GC log我们要关注什么？

1. GC暂停时间
2. GC之之后的内存使用量/使用率

Full GC：我们主要关注GC之后内存使用量是否下降，其次关注暂停时间。简单估算，GC后老年代使用量为220MB左右，耗时50ms。如果内存扩大10倍，GC后老年代内存使用量也扩大10倍，那耗时可能就是500ms甚至更高，系统就会有很明显地影响。

年轻代gc，我们可以关注暂停时间，以及gc后的内存使用率是否正常，但不用特别关注gc前的使用量，而且只要业务在运行，年轻代的对象分配就少不了，回收量就不会少
fullgc时我们更关注老年代的使用量有没有下降，以及下降了多少。如果fullgc后老年代内存不怎么下降，那就说明系统有问题了

JMX提供了GC时间的通知机制，监听GC时间的示例程序我们会在容器章节讲到。但很多情况下JMX通知时间中的GC数据并不完全，只是一个粗略的统计汇总。GC日志才是我们了解JVM和垃圾收集器最可靠和全面的信息，因为里面包含很多细节。再次强调，分析GC日志是一项很有价值的技能，能帮助我们更好的排查性能问题。

References:

1. https://tech.meituan.com/2017/12/29/jvm-optimize.html
2. https://tech.meituan.com/2020/11/12/java-9-cms-gc.html
3. https://tech.meituan.com/2020/08/06/new-zgc-practice-in-meituan.html
4. http://legendtkl.com/2017/04/28/golang-gc/
5. https://github.com/vipshop/vjtools
6. https://tech.meituan.com/2016/09/23/g1.html
7. https://blogs.oracle.com/poonam/understanding-g1-gc-logs
8. https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/G1GettingStarted/index.html
