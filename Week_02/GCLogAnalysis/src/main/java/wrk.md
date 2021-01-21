## Serial GC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseSerialGC -XX:MaxGCPauseMillis=50 -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -jar gateway-server-0.0.1-SNAPSHOT.jar

![image-20210120010609139](images/image-20210120010609139.png)

![image-20210120010835783](images/image-20210120010835783.png)

![image-20210120010922458](images/image-20210120010922458.png)



## Parallel GC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseParallelGC -XX:MaxGCPauseMillis=50 -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -jar gateway-server-0.0.1-SNAPSHOT.jar

![image-20210120011603045](images/image-20210120011603045.png)

![image-20210120011612341](images/image-20210120011612341.png)

![image-20210120011621958](images/image-20210120011621958.png)





## CMS GC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseConcMarkSweepGC -XX:MaxGCPauseMillis=50 -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -jar gateway-server-0.0.1-SNAPSHOT.jar

![image-20210120012727712](images/image-20210120012727712.png)

![image-20210120012751019](images/image-20210120012751019.png)

![image-20210120012800552](images/image-20210120012800552.png)



## G1 GC

java -Xmx1g -Xms1g -XX:-UseAdaptiveSizePolicy -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -jar gateway-server-0.0.1-SNAPSHOT.jar

![image-20210120005515931](images/image-20210120005515931.png)

![image-20210120005545080](images/image-20210120005545080.png)

![image-20210120005612568](images/image-20210120005612568.png)