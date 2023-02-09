package org.Lv2example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sa.rpc.cli.proxy.ProxyGrpc;
import sa.rpc.cli.proxy.ProxyOuterClass;
import sa.rpc.entity.Entity;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

//建立一个线程类
class MyRunnable implements Runnable {
    private final ProxyGrpc.ProxyBlockingStub myStub;
    private final Function<ProxyGrpc.ProxyBlockingStub, Integer> myFunc;

    public MyRunnable(ProxyGrpc.ProxyBlockingStub stub, Function<ProxyGrpc.ProxyBlockingStub, Integer> func) {
        myStub = stub;
        myFunc = func;
    }

    public void run() {
        myFunc.apply(myStub);
    }
}


//建立主逻辑类
public class Lv2GrpcDemo {
    //查询订阅方法
    public static void GetSubscription(ProxyGrpc.ProxyBlockingStub stub) {
        //rep返回code为1代表成功,其余状态码可参考接入文档,data是返回订阅股票的情况
        Entity.Void request = Entity.Void.newBuilder().build();
        ProxyOuterClass.SubscriptionRsp response = stub.getSubscription(request);
        System.out.println(response);
    }

    //新增订阅
    public static void AddSubscription(ProxyGrpc.ProxyBlockingStub stub) {
        //修改协议的值
        //2:市场代码标识(1为上海证券,2为深圳证券)
        //000001:股票代码
        //15:订阅全部标识(1为逐笔成交,2为逐笔委托,4为委托队列,8为股票十档行情,如果想全部订阅可直接填入15,原理是1+2+4+8,如果想订阅某几个行情将几个行情标识相加即可)
        Entity.String request = Entity.String.newBuilder().setValue("2_000001_15").build();
        //批量订阅方法
        //Entity.String request = Entity.String.newBuilder().setValue("2_000001_15,2_000002_5,2_000003_12").build();
        //rep返回code为1代表成功,其余状态码可参考接入文档
        ProxyOuterClass.Rsp response = stub.addSubscription(request);
        System.out.println(response);
    }

    //取消订阅
    public static void DelSubscription(ProxyGrpc.ProxyBlockingStub stub) {
        //修改协议的值
        //2:市场代码标识(1为上海证券,2为深圳证券)
        //000001:股票代码
        //15:取消全部标识(1为逐笔成交,2为逐笔委托,4为委托队列,8为股票十档行情,如果想全部取消订阅可直接填入15,原理是1+2+4+8,如果想取消订阅某几个行情将几个行情标识相加即可)
        Entity.String request = Entity.String.newBuilder().setValue("2_000001_15").build();
        //批量取消方法
        //Entity.String request = Entity.String.newBuilder().setValue("2_000001_15,2_000002_5,2_000003_12").build();
        //rep返回code为1代表成功,其余状态码可参考接入文档
        ProxyOuterClass.Rsp response = stub.delSubscription(request);
        System.out.println(response);
    }

    //推送逐笔成交行情数据
    public static int TickRecordStream(ProxyGrpc.ProxyBlockingStub stub) {
        Entity.Void request = Entity.Void.newBuilder().build();
        java.util.Iterator<Entity.TickRecord> stream = stub.newTickRecordStream(request);
        //用while循环就可以不断消费数据
        while (stream.hasNext()) {
            System.out.println(stream.next());
        }
        return 0;
    }

    //推送逐笔委托行情数据
    public static int OrderRecordStream(ProxyGrpc.ProxyBlockingStub stub) {
        Entity.Void request = Entity.Void.newBuilder().build();
        java.util.Iterator<Entity.OrderRecord> stream = stub.newOrderRecordStream(request);
        //用while循环就可以不断消费数据
        while (stream.hasNext()) {
            System.out.println(stream.next());
        }

        return 0;
    }

    //推送委托队列行情数据
    public static int OrderQuoteRecordStream(ProxyGrpc.ProxyBlockingStub stub) {
        Entity.Void request = Entity.Void.newBuilder().build();
        java.util.Iterator<Entity.OrderQueueRecord> stream = stub.newOrderQueueRecordStream(request);
        //用while循环就可以不断消费数据
        while (stream.hasNext()) {
            System.out.println(stream.next());
        }

        return 0;
    }

    //推送股票十档行情行情数据
    public static int StockQuoteRecordStream(ProxyGrpc.ProxyBlockingStub stub) {
        Entity.Void request = Entity.Void.newBuilder().build();
        java.util.Iterator<Entity.StockQuoteRecord> stream = stub.newStockQuoteRecordStream(request);
        //用while循环就可以不断消费数据
        while (stream.hasNext()) {
            System.out.println(stream.next());
        }

        return 0;
    }


    public static void main(String[] args) {
        //代理服务器监听的地址和端口(注意监听IP和端口必须和配置文件的一致)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 5000)
                .usePlaintext()
                .build();
        ProxyGrpc.ProxyBlockingStub stub = ProxyGrpc.newBlockingStub(channel);

        //请先订阅再运行接收数据,如不订阅是接收不了数据
        AddSubscription(stub);

        //启动线程,订阅多少种行情就启动多少线程
        CountDownLatch latch = new CountDownLatch(4);

        new Thread(new MyRunnable(stub, Lv2GrpcDemo::TickRecordStream), "逐笔成交").start();
        new Thread(new MyRunnable(stub, Lv2GrpcDemo::OrderRecordStream), "逐笔委托").start();
        new Thread(new MyRunnable(stub, Lv2GrpcDemo::OrderQuoteRecordStream), "委托队列").start();
        new Thread(new MyRunnable(stub, Lv2GrpcDemo::StockQuoteRecordStream), "十档行情").start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
