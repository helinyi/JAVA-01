
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class GCLogAnalysis {
    private static Random random = new Random();
    public static void main(String[] args) {
        // 当前毫秒时间戳
        long startMillis = System.currentTimeMillis();
        // 持续运行毫秒数; 可根据需要进行修改
        long timeoutMillis = TimeUnit.SECONDS.toMillis(30);
        // 结束时间戳
        long endMillis = startMillis + timeoutMillis;
        LongAdder counter = new LongAdder();
        System.out.println("running...");
        // 缓存一部分对象，进入老年代
        int cacheSize = 2000;
        Object[] cachedGarbage = new Object[cacheSize];
        // 在此时间范围内，持续循环
        while (System.currentTimeMillis() < endMillis) {
            // 生成垃圾对象
            Object garbage = generateGarbage(100*1024);
            counter.increment();
            int randomIndex = random.nextInt(2*cacheSize);
            // 这里为什么不是必然放进数组？应该是要模拟真实，不然数组两下就爆掉了
            if (randomIndex < cacheSize) {
                cachedGarbage[randomIndex] = garbage;
            }
        }
        System.out.println("执行结束！共生成对象次数：" + counter.longValue());
    }

    private static Object generateGarbage(int max) {
        int randomSize = random.nextInt(max);
        int type = randomSize % 4;
        Object result = null;
        switch (type) {
            case 0:
                result = new int[randomSize];
                break;
            case 1:
                result = new byte[randomSize];
                break;
            case 2:
                result = new double[randomSize];
                break;
            default:
                StringBuilder builder = new StringBuilder();
                String randomString = "randomString-Anything";
                while (builder.length() < randomSize) {
                    builder.append(randomString);
                    builder.append(max);
                    builder.append(randomSize);
                }
                result = builder.toString();
                break;
        }
        return result;
    }
}

// -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xms512m -Xmx512m
//OpenJDK 64-Bit Server VM (25.232-b09) for bsd-amd64 JRE (1.8.0_232-b09), built on Oct 19 2019 07:00:21 by "jenkins" with gcc 4.2.1 (Based on Apple Inc. build 5658) (LLVM build 2336.11.00)
//        Memory: 4k page, physical 67108864k(2114952k free)
//
//        /proc/meminfo:
//
//        CommandLine flags: -XX:InitialHeapSize=536870912 -XX:MaxHeapSize=536870912 -XX:+PrintGC -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseParallelGC
//        2021-01-19T12:38:19.333+0500: 0.163: [GC (Allocation Failure) [PSYoungGen: 131584K->21502K(153088K)] 131584K->42173K(502784K), 0.0145990 secs] [Times: user=0.02 sys=0.12, real=0.01 secs]
//        2021-01-19T12:38:19.366+0500: 0.197: [GC (Allocation Failure) [PSYoungGen: 153080K->21498K(153088K)] 173751K->82748K(502784K), 0.0214546 secs] [Times: user=0.04 sys=0.17, real=0.02 secs]
//        2021-01-19T12:38:19.404+0500: 0.235: [GC (Allocation Failure) [PSYoungGen: 153082K->21503K(153088K)] 214332K->121630K(502784K), 0.0157899 secs] [Times: user=0.05 sys=0.11, real=0.02 secs]
//        2021-01-19T12:38:19.436+0500: 0.266: [GC (Allocation Failure) [PSYoungGen: 152956K->21488K(153088K)] 253083K->158529K(502784K), 0.0145073 secs] [Times: user=0.04 sys=0.10, real=0.02 secs]
//        2021-01-19T12:38:19.466+0500: 0.297: [GC (Allocation Failure) [PSYoungGen: 152772K->21496K(153088K)] 289814K->191534K(502784K), 0.0137381 secs] [Times: user=0.04 sys=0.09, real=0.02 secs]
//        2021-01-19T12:38:19.495+0500: 0.325: [GC (Allocation Failure) [PSYoungGen: 152922K->21503K(80384K)] 322960K->235293K(430080K), 0.0174922 secs] [Times: user=0.04 sys=0.12, real=0.02 secs]
//        2021-01-19T12:38:19.520+0500: 0.351: [GC (Allocation Failure) [PSYoungGen: 80383K->37400K(116736K)] 294173K->257493K(466432K), 0.0047729 secs] [Times: user=0.03 sys=0.02, real=0.00 secs]
//        2021-01-19T12:38:19.535+0500: 0.365: [GC (Allocation Failure) [PSYoungGen: 95631K->49485K(116736K)] 315723K->274148K(466432K), 0.0059962 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]
//        2021-01-19T12:38:19.550+0500: 0.381: [GC (Allocation Failure) [PSYoungGen: 108254K->57839K(116736K)] 332917K->291573K(466432K), 0.0095829 secs] [Times: user=0.07 sys=0.02, real=0.01 secs]
//        2021-01-19T12:38:19.570+0500: 0.400: [GC (Allocation Failure) [PSYoungGen: 116719K->42503K(116736K)] 350453K->310036K(466432K), 0.0153412 secs] [Times: user=0.04 sys=0.11, real=0.01 secs]
//        2021-01-19T12:38:19.594+0500: 0.424: [GC (Allocation Failure) [PSYoungGen: 101383K->19162K(116736K)] 368916K->326107K(466432K), 0.0161746 secs] [Times: user=0.03 sys=0.13, real=0.02 secs]
//        2021-01-19T12:38:19.610+0500: 0.440: [Full GC (Ergonomics) [PSYoungGen: 19162K->0K(116736K)] [ParOldGen: 306945K->237718K(349696K)] 326107K->237718K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0274165 secs] [Times: user=0.27 sys=0.01, real=0.02 secs]
//        2021-01-19T12:38:19.650+0500: 0.480: [GC (Allocation Failure) [PSYoungGen: 58511K->18678K(116736K)] 296229K->256396K(466432K), 0.0025584 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.663+0500: 0.493: [GC (Allocation Failure) [PSYoungGen: 77558K->20848K(116736K)] 315276K->275345K(466432K), 0.0038658 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.678+0500: 0.508: [GC (Allocation Failure) [PSYoungGen: 79507K->16275K(116736K)] 334003K->289670K(466432K), 0.0038457 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
//        2021-01-19T12:38:19.692+0500: 0.522: [GC (Allocation Failure) [PSYoungGen: 75141K->20768K(116736K)] 348536K->309062K(466432K), 0.0036412 secs] [Times: user=0.04 sys=0.01, real=0.00 secs]
//        2021-01-19T12:38:19.704+0500: 0.535: [GC (Allocation Failure) [PSYoungGen: 79648K->20402K(116736K)] 367942K->327735K(466432K), 0.0039206 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.708+0500: 0.539: [Full GC (Ergonomics) [PSYoungGen: 20402K->0K(116736K)] [ParOldGen: 307332K->263292K(349696K)] 327735K->263292K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0263004 secs] [Times: user=0.28 sys=0.00, real=0.03 secs]
//        2021-01-19T12:38:19.746+0500: 0.576: [GC (Allocation Failure) [PSYoungGen: 58880K->25294K(116736K)] 322172K->288586K(466432K), 0.0028191 secs] [Times: user=0.03 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.760+0500: 0.591: [GC (Allocation Failure) [PSYoungGen: 84033K->19678K(116736K)] 347325K->306926K(466432K), 0.0041642 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.775+0500: 0.605: [GC (Allocation Failure) [PSYoungGen: 78558K->22162K(116736K)] 365806K->326782K(466432K), 0.0040756 secs] [Times: user=0.04 sys=0.01, real=0.00 secs]
//        2021-01-19T12:38:19.791+0500: 0.621: [GC (Allocation Failure) [PSYoungGen: 81042K->19339K(116736K)] 385662K->345572K(466432K), 0.0109337 secs] [Times: user=0.03 sys=0.07, real=0.01 secs]
//        2021-01-19T12:38:19.802+0500: 0.632: [Full GC (Ergonomics) [PSYoungGen: 19339K->0K(116736K)] [ParOldGen: 326232K->287078K(349696K)] 345572K->287078K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0282920 secs] [Times: user=0.28 sys=0.01, real=0.03 secs]
//        2021-01-19T12:38:19.842+0500: 0.673: [GC (Allocation Failure) [PSYoungGen: 58497K->15045K(116736K)] 345576K->302124K(466432K), 0.0024634 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.855+0500: 0.685: [GC (Allocation Failure) [PSYoungGen: 73925K->24346K(116736K)] 361004K->326123K(466432K), 0.0038523 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.870+0500: 0.700: [GC (Allocation Failure) [PSYoungGen: 83133K->20706K(116736K)] 384910K->345575K(466432K), 0.0044582 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.874+0500: 0.705: [Full GC (Ergonomics) [PSYoungGen: 20706K->0K(116736K)] [ParOldGen: 324869K->301876K(349696K)] 345575K->301876K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0308459 secs] [Times: user=0.26 sys=0.01, real=0.03 secs]
//        2021-01-19T12:38:19.917+0500: 0.747: [GC (Allocation Failure) [PSYoungGen: 58880K->20225K(116736K)] 360756K->322102K(466432K), 0.0028692 secs] [Times: user=0.03 sys=0.00, real=0.01 secs]
//        2021-01-19T12:38:19.930+0500: 0.761: [GC (Allocation Failure) [PSYoungGen: 78582K->18602K(116736K)] 380459K->339136K(466432K), 0.0043492 secs] [Times: user=0.04 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.935+0500: 0.765: [Full GC (Ergonomics) [PSYoungGen: 18602K->0K(116736K)] [ParOldGen: 320533K->301354K(349696K)] 339136K->301354K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0289628 secs] [Times: user=0.30 sys=0.00, real=0.03 secs]
//        2021-01-19T12:38:19.975+0500: 0.806: [GC (Allocation Failure) [PSYoungGen: 58842K->21412K(116736K)] 360196K->322766K(466432K), 0.0026171 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:19.989+0500: 0.820: [GC (Allocation Failure) [PSYoungGen: 79995K->23399K(116736K)] 381349K->345480K(466432K), 0.0042497 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
//        2021-01-19T12:38:19.993+0500: 0.824: [Full GC (Ergonomics) [PSYoungGen: 23399K->0K(116736K)] [ParOldGen: 322080K->312011K(349696K)] 345480K->312011K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0311278 secs] [Times: user=0.29 sys=0.01, real=0.03 secs]
//        2021-01-19T12:38:20.036+0500: 0.866: [GC (Allocation Failure) [PSYoungGen: 58803K->19441K(120320K)] 370814K->331452K(470016K), 0.0025738 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:20.051+0500: 0.881: [GC (Allocation Failure) [PSYoungGen: 83393K->39607K(103936K)] 395404K->351619K(453632K), 0.0046298 secs] [Times: user=0.05 sys=0.00, real=0.00 secs]
//        2021-01-19T12:38:20.068+0500: 0.899: [GC (Allocation Failure) [PSYoungGen: 103531K->56816K(115712K)] 415542K->369103K(465408K), 0.0058382 secs] [Times: user=0.06 sys=0.00, real=0.01 secs]
//        2021-01-19T12:38:20.086+0500: 0.916: [GC (Allocation Failure) [PSYoungGen: 115253K->55707K(116736K)] 427540K->384734K(466432K), 0.0082274 secs] [Times: user=0.08 sys=0.01, real=0.01 secs]
//        2021-01-19T12:38:20.094+0500: 0.924: [Full GC (Ergonomics) [PSYoungGen: 55707K->0K(116736K)] [ParOldGen: 329027K->319277K(349696K)] 384734K->319277K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0331782 secs] [Times: user=0.33 sys=0.01, real=0.03 secs]
//        2021-01-19T12:38:20.139+0500: 0.969: [Full GC (Ergonomics) [PSYoungGen: 58791K->0K(116736K)] [ParOldGen: 319277K->323778K(349696K)] 378069K->323778K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0354384 secs] [Times: user=0.33 sys=0.01, real=0.04 secs]
//        2021-01-19T12:38:20.186+0500: 1.016: [Full GC (Ergonomics) [PSYoungGen: 58880K->0K(116736K)] [ParOldGen: 323778K->323714K(349696K)] 382658K->323714K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0347110 secs] [Times: user=0.33 sys=0.00, real=0.04 secs]
//        2021-01-19T12:38:20.232+0500: 1.063: [Full GC (Ergonomics) [PSYoungGen: 58880K->0K(116736K)] [ParOldGen: 323714K->324810K(349696K)] 382594K->324810K(466432K), [Metaspace: 3195K->3195K(1056768K)], 0.0321030 secs] [Times: user=0.31 sys=0.00, real=0.03 secs]
//        Heap
//        PSYoungGen      total 116736K, used 2682K [0x00000007b5580000, 0x00000007c0000000, 0x00000007c0000000)
//        eden space 58880K, 4% used [0x00000007b5580000,0x00000007b581e900,0x00000007b8f00000)
//        from space 57856K, 0% used [0x00000007b8f00000,0x00000007b8f00000,0x00000007bc780000)
//        to   space 57856K, 0% used [0x00000007bc780000,0x00000007bc780000,0x00000007c0000000)
//        ParOldGen       total 349696K, used 324810K [0x00000007a0000000, 0x00000007b5580000, 0x00000007b5580000)
//        object space 349696K, 92% used [0x00000007a0000000,0x00000007b3d32960,0x00000007b5580000)
//        Metaspace       used 3201K, capacity 4500K, committed 4864K, reserved 1056768K
//class space    used 343K, capacity 388K, committed 512K, reserved 1048576K12K, reserved 1048576K

