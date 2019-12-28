# 1、配置项
要对tomcat调优，必须先了解有关tomcat参数配置项。tomcat配置在${tomcat}/conf/server.xml中，主要关注线程池(Executor)和连接器（Connector）配置。

## 1.1、线程池配置
```
<Executor name="tomcatThreadPool" namePrefix="catalina-exec-"
        maxThreads="150" minSpareThreads="4"/>
```
表示可组件之间Tomcat中共享的线程池。从历史上看，每个连接器（Connector）都会创建一个线程池，但是当配置为支持执行器时，您可以在（主要）连接器之间以及其他组件之间共享线程池。
主要配置项（更多配置说明参考tomcat官网：[http://tomcat.apache.org/tomcat-8.5-doc/config/executor.html](http://tomcat.apache.org/tomcat-8.5-doc/config/executor.html)）：

| **配置项**   | **用途**   | **默认值**   | **描述**   | 
|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|
| maxThreads   | 活动线程的最大数量   | 200，但配置文件中设置的为150   |    | 

## 1.2、连接器配置
```
<Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
```
除以上配置之外，还有些参数配置项（更多配置说明参考tomcat官网：[http://tomcat.apache.org/tomcat-8.5-doc/config/http.html](http://tomcat.apache.org/tomcat-8.5-doc/config/http.html)）：
| **配置项**   | **用途**   | **默认值**   | **描述**   | 
|:----:|:----:|:----:|:----:|:----:|:----:|:----:|:----:|
| executor   | 线程池配置   | 无   | 指定线程池配置项name的值，如果存在，则其他有关线程配置属性将失效；如果不设置，则连接器将使用内部线程池   | 
| maxThreads   | 请求处理线程的最大数量   | 200   |    | 
| connectionTimeout   | 连接超时时间   | 60s   | 但是配置文件中设置的是20000ms即20s，实际默认值为20s   | 
| acceptCount   | 等待接受accept的请求数量限制   | 100   |    | 
| maxConnections   | 最大连接数   | 1w   |    | 

# 2、参数调整
tomcat的最大能接收的请求数：

>总数 = acceptCount + maxConnections
* maxConnections：tomcat能够接收的请求数量限制
* acceptCount：超过tomcat请求数量限制后，将请求堆积到操作系统中的数量限制（Linux和windows系统略有不同）

它们的关系如下：

![图片](https://uploader.shimo.im/f/a3g1uOA9uHEVEUG6.png!thumbnail)

## 2.1、maxConnections的调整
>maxConnections = （1+0.2）*  maxThreads

当maxConnections（最大连接数）的值小于maxThreads的数量时，需要调大maxConnections的值，因为线程池线程数量大于最大连接数，会导致部分线程无法充分利用，浪费资源，最好的做法是调整maxConnections要比maxThreads的数量大20%，如果当前连接的线程数大于线程池线程数量时，将多余的连接堆积到tomcat的work处理线程池中（堆积占内存）。

## 2.2、acceptCount的调整
有时候想接收更多的请求，又不想堆积在tomcat中（占内存），可以利用操作系统来做高校的堆积，可以将acceptCount的数量调整为maxConnections的值。

acceptCount的值tomcat默认是100，Linux默认是128。

## 2.3、maxThreads的调整
并发线程数的量（maxThreads）是最难控制的，因为线程数太少，CPU利用率低，程序吞吐量变小，资源浪费，容易堆积；如果线程数太多，线程上下文频繁切换，性能降低。

```
理想的并发线程数量=(1+代码阻塞时间/代码执行时间)*cpu的数量
```
如：cpu的数量为2，收到请求，Java代码执行的时间时50ms，而等待数据返回需要50ms，那么：理想线程数=(1+50ms/50ms) * 2 = 4。
但是代码阻塞时间等不太好把握，实际情况需要不断的进行压测，不断调整线程数，将CPU的利用率达到80%~90%是最优的情况。

# 3、参数调优测试
本章节测试的代码是基于springboot的，版本是2.2.1.RELEASE。使用maven工具直接打包成可执行的jar包，调优参数可以在application.properties或者yml文件中配置，可以在执行jar文件时携带参数，为了方便测试以及修改相关参数，我们使用后者的方式。

其中本章所用到有关测试代码、相关工具下载地址以及学习资料：

* 本章测试代码（Git地址）：
* jmeter测试工具下载地址：[http://mirror.bit.edu.cn/apache//jmeter/binaries/apache-jmeter-5.2.1.zip](http://mirror.bit.edu.cn/apache//jmeter/binaries/apache-jmeter-5.2.1.zip)
* jmeter学习文档：[https://www.cnblogs.com/jessicaxu/p/7501770.html](https://www.cnblogs.com/jessicaxu/p/7501770.html)
## 3.1、数量测试
本次测试连接数量的控制以及验证总连接数与maxConnections、acceptCount的关系。

测试接口代码（部分）：

```
/**
 * 假设该方法需要执行3s
 * 用于测试连接数的控制
 * @return
 * @throws InterruptedException
 */
@RequestMapping("/testCount")
public String testCount() throws InterruptedException {
    Thread.sleep(3 * 1000);
    return "success";
}
```
将项目打成jar文件后，执行如下命令启动程序：

```
java -jar tomcat-adjust-0.0.1-SNAPSHOT.jar --server.tomcat.max-connections=2 --server.tomcat.max-thread=10 --server.tomcat.acceptCount=3
```
即acceptCount的数量为3个、maxConnections的数量为2个，最大并发数maxThread为10个，按第2节的能处理的最大请求数应该是3+2=5个，那么下面就来验证这个值。

### 3.1.1、linux系统
将jar文件上传到Linux系统中，然后执行上述命令启动程序

![图片](https://uploader.shimo.im/f/F00Not974yoo866L.png!thumbnail)

下面我们将使用jMeter性能测试工具来进行测试，配置如下：

![图片](https://uploader.shimo.im/f/0PI5Yo2PpWYERWrH.png!thumbnail)

其中192.168.0.135为我Linux服务器地址，线程属性：

![图片](https://uploader.shimo.im/f/6ta9NEiXXCULbvBn.png!thumbnail)

即配置了一秒钟请求10个线程，循环执行一次

参数配置好后，右击“数量测试”->启动，然后点击“查看结果树”，查看执行结果：

![图片](https://uploader.shimo.im/f/GUJJqG88rRAerT4H.gif)

可以看到10个线程都请求成功了，原则是应该只有5个线程被处理，其他5个线程执行失败，这是因为第2节讲的Linux的不仅在tcp握手成功后可以堆积请求，在握手的过程也可以堆积请求（这是操作系统方面决定的，不必纠结太多）。

### 3.1.2、Windows系统
同样的代码，我们在Windows系统中也执行上述jar命令：

![图片](https://uploader.shimo.im/f/SscXPWOsjyArrUVn.png!thumbnail)

修改jmeter测试用例上的ip地址为127.0.0.1，然后启动测试，结果如下：

![图片](https://uploader.shimo.im/f/s3LChTitjvstPBQl.gif)

可以看到结果，5个请求失败，5个成功，打开其中一个失败的请求：

![图片](https://uploader.shimo.im/f/Wf9cdMVuLBsjfZvL.png!thumbnail)

```
Thread Name:数量测试 1-6
Sample Start:2019-12-28 18:42:27 CST
Load time:2005
Connect Time:2005
Latency:0
Size in bytes:2705
Sent bytes:0
Headers size in bytes:0
Body size in bytes:2705
Sample Count:1
Error Count:1
Data type ("text"|"bin"|""):text
Response code:Non HTTP response code: org.apache.http.conn.HttpHostConnectException
Response message:Non HTTP response message: Connect to 127.0.0.1:8080 [/127.0.0.1] failed: Connection refused: connect

HTTPSampleResult fields:
ContentType: 
DataEncoding: null
```
错误：Connection refused: connect请求连接被拒绝。

## 3.2、参数调优测试
测试代码（部分）：

```
/**
 * 参数调优测试
 * @return
 * @throws InterruptedException
 */
@RequestMapping("/test")
public String test() throws InterruptedException {
    System.out.println("访问test：" + Thread.currentThread().getName());
    // 这段代码，一直运算。
    for (int i = 0; i < 200000; i++) {
        new Random().nextInt();
    }
    // 50毫秒的数据库等待，线程不干活
    Thread.sleep(50L);
    return "success";
}
```
在jmeter上添加线程组，用于参数调优测试，配置如下：

![图片](https://uploader.shimo.im/f/tUqQ2FJ126sCRgwL.png!thumbnail)

1秒钟发送1000个请求线程，连续发生10次，即10s钟内发生1万次请求。

